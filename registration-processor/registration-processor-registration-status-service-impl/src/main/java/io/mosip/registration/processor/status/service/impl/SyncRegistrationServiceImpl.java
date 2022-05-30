/**
 * 
 */
package io.mosip.registration.processor.status.service.impl;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.mosip.kernel.core.util.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.idvalidator.rid.constant.RidExceptionProperty;
import io.mosip.registration.processor.core.anonymous.dto.AnonymousProfileDTO;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.AuditLogConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ResponseStatusCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.code.SupervisorStatus;
import io.mosip.registration.processor.status.dao.SyncRegistrationDao;
import io.mosip.registration.processor.status.decryptor.Decryptor;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.LostRidDto;
import io.mosip.registration.processor.status.dto.RegistrationAdditionalInfoDTO;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusSubRequestDto;
import io.mosip.registration.processor.status.dto.RegistrationSyncRequestDTO;
import io.mosip.registration.processor.status.dto.SearchInfo;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailureDto;
import io.mosip.registration.processor.status.dto.SyncResponseFailureV2Dto;
import io.mosip.registration.processor.status.dto.SyncResponseSuccessDto;
import io.mosip.registration.processor.status.dto.SyncResponseSuccessV2Dto;
import io.mosip.registration.processor.status.encryptor.Encryptor;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.EncryptionFailureException;
import io.mosip.registration.processor.status.exception.LostRidValidationException;
import io.mosip.registration.processor.status.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.utilities.RegistrationUtility;

/**
 * The Class SyncRegistrationServiceImpl.
 *
 * @author M1048399
 * @author M1048219
 * @author M1047487
 */
@Component
public class SyncRegistrationServiceImpl implements SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> {

	/** The Constant CREATED_BY. */
	private static final String CREATED_BY = "MOSIP";

	/** The event id. */
	private String eventId = "";

	/** The event name. */
	private String eventName = "";

	@Value("${mosip.registration.processor.postalcode.req.url}")
	private String locationCodeReqUrl;

	@Value("${mosip.registration.processor.lostrid.iteration.max.count:10000}")
	private int iteration;

	@Value("${registration.processor.lostrid.max.registrationid:5}")
	private int maxSearchResult;

	/** The event type. */
	private String eventType = "";

	/** The sync registration dao. */
	@Autowired
	private SyncRegistrationDao syncRegistrationDao;
	/** The sync AnonymousProfileService . */
	@Autowired
	private AnonymousProfileService anonymousProfileService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The rid validator. */
	@Autowired
	private RidValidator<String> ridValidator;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private RegistrationUtility registrationUtility;

	/** The lancode length. */
	private int LANCODE_LENGTH = 3;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(SyncRegistrationServiceImpl.class);

	/** The decryptor. */
	@Autowired
	private Decryptor decryptor;
	
	/** The encryptor. */
	@Autowired
	private Encryptor encryptor;

	@Value("#{'${registration.processor.main-processes}'.split(',')}")
	private List<String> mainProcesses;

	@Value("#{'${registration.processor.sub-processes}'.split(',')}")
	private List<String> subProcesses;
	
	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;
	
	/** The get reg processor identity json. */
	@Value("${registration.processor.identityjson}")
	private String getRegProcessorIdentityJson;

	@Autowired
	RestApiClient restApiClient;
	/**
	 * Instantiates a new sync registration service impl.
	 */
	public SyncRegistrationServiceImpl() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.status.service.SyncRegistrationService#sync(
	 * java.util.List)
	 */
	public List<SyncResponseDto> sync(List<SyncRegistrationDto> resgistrationDtos, String referenceId,
			String timeStamp) {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::sync()::entry");
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		try {
			for (SyncRegistrationDto registrationDto : resgistrationDtos) {
				syncResponseList = validateSync(registrationDto, syncResponseList, referenceId, timeStamp);
			}
			isTransactionSuccessful = true;
			description.setMessage("Registartion Id's are successfully synched in Sync Registration table");

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "");
		} catch (DataAccessLayerException e) {
			description.setMessage(PlatformErrorMessages.RPR_RGS_DATA_ACCESS_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_DATA_ACCESS_EXCEPTION.getCode());
			description.setMessage("DataAccessLayerException while syncing Registartion Id's" + "::" + e.getMessage());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		} finally {
			if (isTransactionSuccessful) {
				eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
						: EventName.ADD.toString();
				eventType = EventType.BUSINESS.toString();
			} else {
				description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_SYNC_SERVICE_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_SYNC_SERVICE_FAILED.getCode());
				eventId = EventId.RPR_405.toString();
				eventName = EventName.EXCEPTION.toString();
				eventType = EventType.SYSTEM.toString();
			}
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_SYNC_REGISTRATION_SERVICE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.SYNC_REGISTRATION_SERVICE.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, AuditLogConstant.MULTIPLE_ID.toString());

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::sync()::exit");
		return syncResponseList;

	}

	public List<SyncResponseDto> syncV2(List<SyncRegistrationDto> resgistrationDtos, String referenceId,
										String timeStamp) {
		List<SyncResponseDto> syncResponseList = new ArrayList<>();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::sync()::entry");
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		try {
			for (SyncRegistrationDto registrationDto : resgistrationDtos) {
				if(registrationDto.getPacketId()!=null && !registrationDto.getPacketId().isBlank()){
					syncResponseList = validateSyncV2(registrationDto, syncResponseList, referenceId, timeStamp);
				}
				else {
					SyncResponseFailDto syncResponseFailureDto = new SyncResponseFailDto();
					syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
					syncResponseFailureDto.setMessage("Missing Request Value -  packetId");
					syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getCode());
					syncResponseList.add(syncResponseFailureDto);
				}
			}
			isTransactionSuccessful = true;
			description.setMessage("Registartion Id's are successfully synched in Sync Registration table");

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "");
		} catch (DataAccessLayerException e) {
			description.setMessage(PlatformErrorMessages.RPR_RGS_DATA_ACCESS_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_DATA_ACCESS_EXCEPTION.getCode());
			description.setMessage("DataAccessLayerException while syncing Registartion Id's" + "::" + e.getMessage());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		} finally {
			if (isTransactionSuccessful) {
				eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
						: EventName.ADD.toString();
				eventType = EventType.BUSINESS.toString();
			} else {
				description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_SYNC_SERVICE_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_SYNC_SERVICE_FAILED.getCode());
				eventId = EventId.RPR_405.toString();
				eventName = EventName.EXCEPTION.toString();
				eventType = EventType.SYSTEM.toString();
			}
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_SYNC_REGISTRATION_SERVICE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.SYNC_REGISTRATION_SERVICE.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, AuditLogConstant.MULTIPLE_ID.toString());

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::sync()::exit");
		return syncResponseList;

	}

	/**
	 * Validate RegiId with Kernel RidValiator.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return the list
	 */
	private List<SyncResponseDto> validateSync(SyncRegistrationDto registrationDto,
			List<SyncResponseDto> syncResponseList, String referenceId,
			String timeStamp) {
		if (validateLanguageCode(registrationDto, syncResponseList)
				&& validateRegistrationType(registrationDto, syncResponseList)
				&& validateHashValue(registrationDto, syncResponseList)
				&& validateSupervisorStatus(registrationDto, syncResponseList)) {
			if (validateRegistrationID(registrationDto, syncResponseList)) {
				SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
				try {
					if (ridValidator.validateId(registrationDto.getRegistrationId())) {
						syncResponseList = syncRegistrationRecord(registrationDto, syncResponseList, referenceId, timeStamp);
					}
				} catch (InvalidIDException e) {
					syncResponseFailureDto.setRegistrationId(registrationDto.getRegistrationId());

					syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
					if (e.getErrorCode().equals(RidExceptionProperty.INVALID_RID_LENGTH.getErrorCode())) {
						syncResponseFailureDto
								.setMessage(PlatformErrorMessages.RPR_RGS_INVALID_REGISTRATIONID_LENGTH.getMessage());
						syncResponseFailureDto
								.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_REGISTRATIONID_LENGTH.getCode());
					} else if (e.getErrorCode().equals(RidExceptionProperty.INVALID_RID.getErrorCode())) {
						syncResponseFailureDto
								.setMessage(PlatformErrorMessages.RPR_RGS_INVALID_REGISTRATIONID.getMessage());
						syncResponseFailureDto
								.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_REGISTRATIONID.getCode());
					} else if (e.getErrorCode().equals(RidExceptionProperty.INVALID_RID_TIMESTAMP.getErrorCode())) {
						syncResponseFailureDto.setMessage(
								PlatformErrorMessages.RPR_RGS_INVALID_REGISTRATIONID_TIMESTAMP.getMessage());
						syncResponseFailureDto
								.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_REGISTRATIONID_TIMESTAMP.getCode());
					}
					syncResponseList.add(syncResponseFailureDto);
				}
			}
		}
		return syncResponseList;
	}


	private List<SyncResponseDto> validateSyncV2(SyncRegistrationDto registrationDto,
											   List<SyncResponseDto> syncResponseList, String referenceId,
											   String timeStamp) {
		if (validateLanguageCode(registrationDto, syncResponseList)
				&& validateRegistrationType(registrationDto, syncResponseList)
				&& validateHashValue(registrationDto, syncResponseList)
				&& validateSupervisorStatus(registrationDto, syncResponseList)) {
			if (validateRegistrationID(registrationDto, syncResponseList)) {
				syncResponseList = syncRegistrationRecord(registrationDto, syncResponseList, referenceId, timeStamp);
			}
		}
		List<SyncResponseDto> syncResponseV2List=new ArrayList<>();
		for(SyncResponseDto dto:syncResponseList) {
			if(dto instanceof SyncResponseFailureDto) {
				SyncResponseFailureV2Dto v2Dto=new SyncResponseFailureV2Dto(dto.getRegistrationId(),dto.getStatus(),
						((SyncResponseFailureDto) dto).getErrorCode(),((SyncResponseFailureDto) dto).getMessage(),
						registrationDto.getPacketId());
				syncResponseV2List.add(v2Dto);
			}
			if(dto instanceof SyncResponseSuccessDto || dto instanceof SyncResponseDto) {
				SyncResponseSuccessV2Dto v2Dto=new SyncResponseSuccessV2Dto(dto.getRegistrationId(),dto.getStatus(),
						registrationDto.getPacketId());
				syncResponseV2List.add(v2Dto);
			}
			if(dto instanceof SyncResponseFailDto) {
				SyncResponseFailureV2Dto v2Dto=new SyncResponseFailureV2Dto(dto.getRegistrationId(),dto.getStatus(),
						((SyncResponseFailDto) dto).getErrorCode(),((SyncResponseFailDto) dto).getMessage(),
						registrationDto.getPacketId());
				syncResponseV2List.add(v2Dto);
			}
		}
		
		return syncResponseV2List;
	}

	/**
	 * Validate supervisor status.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return true, if successful
	 */
	private boolean validateSupervisorStatus(SyncRegistrationDto registrationDto,
			List<SyncResponseDto> syncResponseList) {
		String value = registrationDto.getSupervisorStatus();
		if (SupervisorStatus.APPROVED.toString().equals(value)) {
			return true;
		} else if (SupervisorStatus.REJECTED.toString().equals(value)) {
			return true;

		} else {
			SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
			syncResponseFailureDto.setRegistrationId(registrationDto.getRegistrationId());

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_INVALID_SUPERVISOR_STATUS.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_SUPERVISOR_STATUS.getCode());
			syncResponseList.add(syncResponseFailureDto);
			return false;
		}

	}

	/**
	 * Validate hash value.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return true, if successful
	 */
	private boolean validateHashValue(SyncRegistrationDto registrationDto, List<SyncResponseDto> syncResponseList) {

		if (registrationDto.getPacketHashValue() == null) {
			SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
			syncResponseFailureDto.setRegistrationId(registrationDto.getRegistrationId());

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_INVALID_HASHVALUE.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_HASHVALUE.getCode());
			syncResponseList.add(syncResponseFailureDto);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Validate status code.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return true, if successful
	 */
	private boolean validateRegistrationType(SyncRegistrationDto registrationDto,
			List<SyncResponseDto> syncResponseList) {
		List<String> processes=new ArrayList<>();
		processes.addAll(mainProcesses);
		processes.addAll(subProcesses);
		String value = registrationDto.getRegistrationType();
		if(processes.contains(value)) {
			return true;
		}else {
			SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
			syncResponseFailureDto.setRegistrationId(registrationDto.getRegistrationId());

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_INVALID_SYNCTYPE.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_SYNCTYPE.getCode());
			syncResponseList.add(syncResponseFailureDto);
			return false;
		}
	}

	/**
	 * Validate language code.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return true, if successful
	 */
	private boolean validateLanguageCode(SyncRegistrationDto registrationDto, List<SyncResponseDto> syncResponseList) {
		if (registrationDto.getLangCode().length() == LANCODE_LENGTH) {
			return true;
		} else {
			SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
			syncResponseFailureDto.setRegistrationId(registrationDto.getRegistrationId());

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_INVALID_LANGUAGECODE.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_INVALID_LANGUAGECODE.getCode());
			syncResponseList.add(syncResponseFailureDto);
			return false;
		}
	}

	/**
	 * Validate registration ID.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return true, if successful
	 */
	private boolean validateRegistrationID(SyncRegistrationDto registrationDto,
			List<SyncResponseDto> syncResponseList) {
		if (registrationDto.getRegistrationId() != null) {
			return true;
		} else {
			SyncResponseFailureDto syncResponseFailureDto = new SyncResponseFailureDto();
			syncResponseFailureDto.setRegistrationId(registrationDto.getRegistrationId());
			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_EMPTY_REGISTRATIONID.getCode());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_EMPTY_REGISTRATIONID.getMessage());
			syncResponseList.add(syncResponseFailureDto);
			return false;
		}
	}

	/**
	 * Validate reg id.
	 *
	 * @param registrationDto
	 *            the registration dto
	 * @param syncResponseList
	 *            the sync response list
	 * @return the list
	 */
	public List<SyncResponseDto> syncRegistrationRecord(SyncRegistrationDto registrationDto,
														List<SyncResponseDto> syncResponseList, String referenceId,
														String timeStamp) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationDto.getRegistrationId(), "SyncRegistrationServiceImpl::validateRegId()::entry");
		SyncResponseSuccessDto syncResponseDto = new SyncResponseSuccessDto();
		SyncRegistrationEntity existingSyncRegistration = findByPacketId(registrationDto.getPacketId());
		SyncRegistrationEntity syncRegistration;
		if (existingSyncRegistration != null) {
			// update sync registration record
			syncRegistration = convertDtoToEntity(registrationDto, referenceId, timeStamp);
			syncRegistration.setWorkflowInstanceId(existingSyncRegistration.getWorkflowInstanceId());
			syncRegistration.setCreateDateTime(existingSyncRegistration.getCreateDateTime());
			if(syncRegistration.getCreateDateTime()!=null) {
				syncRegistration.setRegistrationDate(syncRegistration.getCreateDateTime().toLocalDate());
			}
			syncRegistrationDao.update(syncRegistration);
			syncResponseDto.setRegistrationId(registrationDto.getRegistrationId());

			eventId = EventId.RPR_402.toString();
		} else {
			// first time sync registration

			syncRegistration = convertDtoToEntity(registrationDto, referenceId, timeStamp);
			syncRegistration.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			syncRegistration.setWorkflowInstanceId(RegistrationUtility.generateId());
			if(syncRegistration.getCreateDateTime()!=null) {
				syncRegistration.setRegistrationDate(syncRegistration.getCreateDateTime().toLocalDate());
			}
			syncRegistrationDao.save(syncRegistration);
			syncResponseDto.setRegistrationId(registrationDto.getRegistrationId());
			
			eventId = EventId.RPR_407.toString();
		}
		syncResponseDto.setStatus(ResponseStatusCode.SUCCESS.toString());
		syncResponseList.add(syncResponseDto);
		saveAnonymousProfile( registrationDto,  referenceId,  timeStamp);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationDto.getRegistrationId(), "SyncRegistrationServiceImpl::validateRegId()::exit");
		return syncResponseList;
	}

	private void saveAnonymousProfile(SyncRegistrationDto registrationDto, String referenceId, String timeStamp)  {
		AnonymousProfileDTO dto=new AnonymousProfileDTO();
		try{
			dto.setProcessName(registrationDto.getRegistrationType());
			dto.setStatus("REGISTERED");
			dto.setStartDateTime(timeStamp);
			dto.setDate(LocalDate.now(ZoneId.of("UTC")).toString());
			dto.setProcessStage("SYNC");
			List<String> channel=new ArrayList<>(); 
			String mappingJsonString = registrationUtility.getMappingJson();
			org.json.simple.JSONObject mappingJsonObject = objectMapper.readValue(mappingJsonString, org.json.simple.JSONObject.class);
			org.json.simple.JSONObject regProcessorIdentityJson =JsonUtil.getJSONObject(mappingJsonObject, MappingJsonConstants.IDENTITY);
			
			channel.add( registrationDto.getEmail() != null ? JsonUtil.getJSONValue(
	                JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL),
	                MappingJsonConstants.VALUE) : null);
			channel.add( registrationDto.getPhone() != null ? JsonUtil.getJSONValue(
	                JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE),
	                MappingJsonConstants.VALUE) : null);
			dto.setChannel(channel);
			dto.setEnrollmentCenterId(referenceId.split("_")[0]);
			anonymousProfileService.saveAnonymousProfile(registrationDto.getRegistrationId(),
					"SYNC", JsonUtil.objectMapperObjectToJson(dto));
			} catch (java.io.IOException exception) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"", exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.status.service.SyncRegistrationService#
	 * isPresent(java.lang.String)
	 */
	@Override
	public boolean isPresent(String registrationId) {
		return findByRegistrationId(registrationId) != null;
	}

	/**
	 * Find by registration id.
	 *
	 * @param registrationId
	 *            the registration id
	 * @return the sync registration entity
	 */
	@Override
	public List<SyncRegistrationEntity> findByRegistrationId(String registrationId) {
		return syncRegistrationDao.findById(registrationId);
	}

	@Override
	public SyncRegistrationEntity findByWorkflowInstanceId(String workflowInstanceId) {
		return syncRegistrationDao.findByWorkflowInstanceId(workflowInstanceId);
	}

	 /**
	   * Find by registration id and additional info req id.
	   * @param registrationId
	   *            the registration id
	   * @param additionalInfoRequestId
	   *            the additional info req id
	   * @return the sync registration entity
	   */
	@Override
	public SyncRegistrationEntity findByRegistrationIdAndAdditionalInfoReqId(String registrationId, String additionalInfoRequestId) {
		return syncRegistrationDao.findByRegistrationIdIdAndAdditionalInfoReqId(registrationId,additionalInfoRequestId);
	}

	@Override
	public SyncRegistrationEntity findByPacketId(String packetId) {
		return syncRegistrationDao.findByPacketId(packetId);
	}

	@Override
	public List<SyncRegistrationEntity> findByAdditionalInfoReqId(String additionalInfoReqId) {
		return syncRegistrationDao.findByAdditionalInfoReqId(additionalInfoReqId);
	}

	/**
	 * Convert dto to entity.
	 *
	 * @param dto
	 *            the dto
	 * @return the sync registration entity
	 */
	private SyncRegistrationEntity convertDtoToEntity(SyncRegistrationDto dto, String referenceId,
			String timeStamp) {
		SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
		syncRegistrationEntity.setRegistrationId(dto.getRegistrationId().trim());
		syncRegistrationEntity.setIsDeleted(dto.getIsDeleted() != null ? dto.getIsDeleted() : Boolean.FALSE);
		syncRegistrationEntity.setLangCode(dto.getLangCode());
		syncRegistrationEntity.setRegistrationType(dto.getRegistrationType());
		syncRegistrationEntity.setPacketHashValue(dto.getPacketHashValue());
		syncRegistrationEntity.setPacketSize(dto.getPacketSize());
		syncRegistrationEntity.setSupervisorStatus(dto.getSupervisorStatus());
		syncRegistrationEntity.setSupervisorComment(dto.getSupervisorComment());
		syncRegistrationEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		syncRegistrationEntity.setAdditionalInfoReqId(dto.getAdditionalInfoReqId());
		syncRegistrationEntity.setPacketId(dto.getPacketId() != null ? dto.getPacketId() : dto.getRegistrationId());
		syncRegistrationEntity.setReferenceId(referenceId);
		try {
			RegistrationAdditionalInfoDTO regAdditionalInfo = new RegistrationAdditionalInfoDTO();
			regAdditionalInfo.setName(dto.getName());
			regAdditionalInfo.setEmail(dto.getEmail());
			regAdditionalInfo.setPhone(dto.getPhone());
			
			String additionalInfo = JsonUtils.javaObjectToJsonString(regAdditionalInfo);
			byte[] encryptedInfo = encryptor.encrypt(additionalInfo, referenceId, timeStamp);
			syncRegistrationEntity.setOptionalValues(encryptedInfo);
			if (dto.getName() != null) {
				syncRegistrationEntity.setName(dto.getName() != null ?
						getHashCode(dto.getName().replaceAll("\\s", "").toLowerCase()) : null);
			}
			syncRegistrationEntity.setEmail(dto.getEmail() != null ? getHashCode(dto.getEmail()) : null);
			syncRegistrationEntity.setCenterId(getHashCode(referenceId.split("_")[0]));
			syncRegistrationEntity.setPhone(dto.getPhone() != null ? getHashCode(dto.getPhone()) : null);
			syncRegistrationEntity
					.setLocationCode(getHashCode(getLocationCode(referenceId.split("_")[0], dto.getLangCode())));
		} catch (JsonProcessingException | RegStatusAppException | EncryptionFailureException
				| ApisResourceAccessException exception) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}		

		syncRegistrationEntity.setCreatedBy(CREATED_BY);
		syncRegistrationEntity.setUpdatedBy(CREATED_BY);
		if (syncRegistrationEntity.getIsDeleted() != null && syncRegistrationEntity.getIsDeleted()) {
			syncRegistrationEntity.setDeletedDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		} else {
			syncRegistrationEntity.setDeletedDateTime(null);
		}

		return syncRegistrationEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.status.service.SyncRegistrationService#
	 * decryptAndGetSyncRequest(java.lang.Object, java.lang.String,
	 * java.lang.String, java.util.List)
	 */
	@Override
	public RegistrationSyncRequestDTO decryptAndGetSyncRequest(Object encryptedSyncMetaInfo, String referenceId,
			String timeStamp, List<SyncResponseDto> syncResponseList) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::decryptAndGetSyncRequest()::entry");

		RegistrationSyncRequestDTO registrationSyncRequestDTO = null;
		try {
			String decryptedSyncMetaData = decryptor.decrypt(encryptedSyncMetaInfo, referenceId, timeStamp);
			registrationSyncRequestDTO = (RegistrationSyncRequestDTO) JsonUtils
					.jsonStringToJavaObject(RegistrationSyncRequestDTO.class, decryptedSyncMetaData);

		} catch (PacketDecryptionFailureException | ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			SyncResponseFailDto syncResponseFailureDto = new SyncResponseFailDto();

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_DECRYPTION_FAILED.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_DECRYPTION_FAILED.getCode());
			syncResponseList.add(syncResponseFailureDto);
		} catch (JsonParseException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			SyncResponseFailDto syncResponseFailureDto = new SyncResponseFailDto();

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_JSON_PARSING_EXCEPTION.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_JSON_PARSING_EXCEPTION.getCode());
			syncResponseList.add(syncResponseFailureDto);

		} catch (JsonMappingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			SyncResponseFailDto syncResponseFailureDto = new SyncResponseFailDto();

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_RGS_JSON_MAPPING_EXCEPTION.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_RGS_JSON_MAPPING_EXCEPTION.getCode());
			syncResponseList.add(syncResponseFailureDto);
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			SyncResponseFailDto syncResponseFailureDto = new SyncResponseFailDto();

			syncResponseFailureDto.setStatus(ResponseStatusCode.FAILURE.toString());
			syncResponseFailureDto.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			syncResponseFailureDto.setErrorCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
			syncResponseList.add(syncResponseFailureDto);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::decryptAndGetSyncRequest()::exit");

		return registrationSyncRequestDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.status.service.SyncRegistrationService#
	 * getByIds(java.util.List)
	 */
	@Override
	public List<RegistrationStatusDto> getByIds(List<RegistrationStatusSubRequestDto> requestIds) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::getByIds()::entry");

		try {
			List<String> registrationIds = new ArrayList<>();

			for (RegistrationStatusSubRequestDto registrationStatusSubRequestDto : requestIds) {
				registrationIds.add(registrationStatusSubRequestDto.getRegistrationId());
			}
			if (!registrationIds.isEmpty()) {
				List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationDao.getByIds(registrationIds);

				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"SyncRegistrationServiceImpl::getByIds()::exit");
				return convertEntityListToDtoListAndGetExternalStatus(syncRegistrationEntityList);
			}
			return null;
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}

	}

	@Override
	public List<LostRidDto> searchLostRid(SearchInfo searchInfo) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"SyncRegistrationServiceImpl::getByIds()::entry");
		try {
			updateFiltersWithHashedValues(searchInfo);
			List<SyncRegistrationEntity> syncRegistrationEntities = syncRegistrationDao.getSearchResults(
					searchInfo.getFilters(),
					searchInfo.getSort());
			List<LostRidDto> lostRidDtos = entityToDtoMapper(syncRegistrationEntities);
			validateRegistrationIds(lostRidDtos);
			return lostRidDtos;
		} catch (DataAccessLayerException | NoSuchAlgorithmException | RegStatusAppException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	@Override
	public List<RegistrationStatusDto> getExternalStatusByIds(List<String> requestIds) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::getExternalStatusByIds()::entry");

		try {
			if (!requestIds.isEmpty()) {
				List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationDao.getByIds(requestIds);

				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"SyncRegistrationServiceImpl::getExternalStatusByIds()::exit");
				return convertEntityListToDtoListAndGetExternalStatus(syncRegistrationEntityList);
			}
			return null;
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}

	}

	private List<LostRidDto> entityToDtoMapper(List<SyncRegistrationEntity> syncRegistrationEntities) {
		List<LostRidDto> lostRidDtos = new ArrayList<LostRidDto>();
		syncRegistrationEntities.forEach(syncEntity -> {
			LostRidDto lostRidDto = new LostRidDto();
			lostRidDto.setRegistrationId(syncEntity.getRegistrationId());
			lostRidDto.setRegistartionDate(null!=syncEntity.getRegistrationDate()?syncEntity.getRegistrationDate().toString():null);
			lostRidDto.setSyncDateTime(null!=syncEntity.getCreateDateTime()?syncEntity.getCreateDateTime().toString():null);
			if(syncEntity.getOptionalValues()!=null) {
				getAdditionalInfo(syncEntity.getReferenceId(), syncEntity.getOptionalValues(), lostRidDto.getAdditionalInfo());
			}
			lostRidDtos.add(lostRidDto);
		});
		return lostRidDtos.stream().distinct().collect(Collectors.toList());
	}

	private void getAdditionalInfo(String referenceId, byte[] optionalValues, Map<String, String> additionalInfo)  {
		String name=null;
		try {
			String decryptedData=decryptor.decrypt(CryptoUtil.encodeBase64String(optionalValues),referenceId, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
			JSONObject jsonObject=new JSONObject(decryptedData);
			name=jsonObject.getString("name");
			additionalInfo.put("name",name);

		} catch (PacketDecryptionFailureException | ApisResourceAccessException |JSONException  e) {
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_DECRYPTION_FAILED.getMessage(),e);		}
	}

	private void validateRegistrationIds(List<LostRidDto> lostRidDtos) throws RegStatusAppException {
		LostRidValidationException exception = new LostRidValidationException();
		if (lostRidDtos.size() >= maxSearchResult) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_SEARCH, exception);
		}

	}

	private void updateFiltersWithHashedValues(SearchInfo searchInfo)
			throws NoSuchAlgorithmException, RegStatusAppException {
		for (FilterInfo filterInfo : searchInfo.getFilters()) {
			if (filterInfo.getColumnName().equals("email") || filterInfo.getColumnName().equals("phone")
					|| filterInfo.getColumnName().equals("centerId")
					|| filterInfo.getColumnName().equals("locationCode")) {

				filterInfo.setValue(getHashCode(filterInfo.getValue()));
			} else if (filterInfo.getColumnName().equalsIgnoreCase("name")) {
				filterInfo.setValue(getHashCode(filterInfo.getValue().replaceAll("\\s", "").toLowerCase()));
			}
		}
	}

	/**
	 * Convert entity list to dto list and get external status.
	 *
	 * @param syncRegistrationEntityList
	 *            the sync registration entity list
	 * @return the list
	 */
	private List<RegistrationStatusDto> convertEntityListToDtoListAndGetExternalStatus(
			List<SyncRegistrationEntity> syncRegistrationEntityList) {
		List<RegistrationStatusDto> list = new ArrayList<>();
		if (syncRegistrationEntityList != null) {
			for (SyncRegistrationEntity entity : syncRegistrationEntityList) {
				list.add(convertEntityToDtoAndGetExternalStatus(entity));
			}

		}
		return list;
	}

	/**
	 * Convert entity to dto and get external status.
	 *
	 * @param entity
	 *            the entity
	 * @return the registration status dto
	 */
	private RegistrationStatusDto convertEntityToDtoAndGetExternalStatus(SyncRegistrationEntity entity) {
		RegistrationStatusDto registrationStatusDto = new RegistrationStatusDto();
		registrationStatusDto.setRegistrationId(entity.getRegistrationId());
		registrationStatusDto.setAdditionalInfoReqId(entity.getAdditionalInfoReqId());
		registrationStatusDto.setStatusCode(RegistrationExternalStatusCode.UPLOAD_PENDING.toString());
		return registrationStatusDto;
	}

	private String getLocationCode(String centerId, String langCode) {
		String requestUrl = locationCodeReqUrl + "/" + centerId + "/" + langCode;
		URI requestUri = URI.create(requestUrl);
		String locationCode = null;
		try {
			String response = restApiClient.getApi(requestUri, String.class);
			JSONObject jsonObjects = new JSONObject(response);
			locationCode = jsonObjects.getJSONObject("response") != null ? ((JSONObject) jsonObjects.getJSONObject("response").getJSONArray("registrationCenters").get(0))
					.getString("locationCode") : null;
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
		return locationCode;
	}

	@Override
	public boolean deleteAdditionalInfo(SyncRegistrationEntity syncEntity) {
		return syncRegistrationDao.deleteAdditionalInfo(syncEntity);
	}

	@Override
	public List<SyncRegistrationEntity> getByPacketIds(List<String> packetIdList) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"SyncRegistrationServiceImpl::getByPacketIds()::entry");
		try {
			if (!packetIdList.isEmpty()) {
				List<SyncRegistrationEntity> syncRegistrationEntityList = syncRegistrationDao
						.getByPacketIds(packetIdList);

				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"SyncRegistrationServiceImpl::getByPacketIds()::exit");
				return syncRegistrationEntityList;
			}
			return null;
		} catch (DataAccessLayerException e) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new TablenotAccessibleException(
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e);
		}
	}

	private static byte[] getHMACHash(String value) throws java.security.NoSuchAlgorithmException {
		if (value == null)
			return null;
		return HMACUtils2.generateHash(value.getBytes());
	}

	private static byte[] getHMACHashWithSalt(byte[] valueBytes, byte[] saltBytes)
			throws java.security.NoSuchAlgorithmException {
		if (valueBytes == null)
			return null;
		return HMACUtils2.digestAsPlainTextWithSalt(valueBytes, saltBytes).getBytes();
	}

	private String getHashCode(String value) throws RegStatusAppException {
		String encodedHash = null;
		if (value == null) {
			return null;
		}
		try {
			byte[] hashCode = getHMACHash(value);
			byte[] nonce = Arrays.copyOfRange(hashCode, hashCode.length - 2, hashCode.length);
			String result = convertBytesToHex(nonce);
			Long hashValue = Long.parseLong(result, 16);
			Long saltIndex = hashValue % 10000;
			String salt = syncRegistrationDao.getSaltValue(saltIndex);
			byte[] saltBytes=null;
			try {
				saltBytes= CryptoUtil.decodeURLSafeBase64(salt);
			} catch (IllegalArgumentException exception) {
				saltBytes = CryptoUtil.decodePlainBase64(salt);
			}
			byte[] hashBytes = value.getBytes();
			for (int i = 0; i <= iteration; i++) {
				hashBytes = getHMACHashWithSalt(hashBytes, saltBytes);
			}
			encodedHash = CryptoUtil.encodeToURLSafeBase64(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_SEARCH, e);
		}
		return encodedHash;
	}

	private static String convertBytesToHex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte temp : bytes) {
			result.append(String.format("%02x", temp));
		}
		return result.toString();
	}

}
