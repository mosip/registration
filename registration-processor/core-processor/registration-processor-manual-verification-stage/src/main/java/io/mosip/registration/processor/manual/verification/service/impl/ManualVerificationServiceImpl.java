package io.mosip.registration.processor.manual.verification.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.DedupeSourceName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTOWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.manual.verification.Listener;
import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import io.mosip.registration.processor.manual.verification.dto.DataShareResponseDto;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.manual.verification.dto.MatchDetail;
import io.mosip.registration.processor.manual.verification.dto.UserDto;
import io.mosip.registration.processor.manual.verification.exception.DataShareException;
import io.mosip.registration.processor.manual.verification.exception.InvalidFileNameException;
import io.mosip.registration.processor.manual.verification.exception.InvalidRidException;
import io.mosip.registration.processor.manual.verification.exception.InvalidUpdateException;
import io.mosip.registration.processor.manual.verification.exception.MatchTypeNotFoundException;
import io.mosip.registration.processor.manual.verification.exception.MatchedRefNotExistsException;
import io.mosip.registration.processor.manual.verification.exception.NoRecordAssignedException;
import io.mosip.registration.processor.manual.verification.exception.UserIDNotPresentException;
import io.mosip.registration.processor.manual.verification.request.dto.Gallery;
import io.mosip.registration.processor.manual.verification.request.dto.ManualAdjudicationRequestDTO;
import io.mosip.registration.processor.manual.verification.request.dto.ReferenceIds;
import io.mosip.registration.processor.manual.verification.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
import io.mosip.registration.processor.packet.manager.exception.FileNotFoundInDestinationException;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.BIRConverter;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.manual.verification.request.dto.ShareableAttributes;
import io.mosip.registration.processor.manual.verification.request.dto.Source;
import io.mosip.registration.processor.manual.verification.request.dto.Filter;
/**
 * The Class ManualVerificationServiceImpl.
 */
@Component
@Transactional
public class ManualVerificationServiceImpl implements ManualVerificationService {

	/** The logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ManualVerificationServiceImpl.class);

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	@Value("${packet.default.source}")
	private String defaultSource;

	@Value("${registration.processor.manual.adjudication.policy.id}")
	private String policyId;

	@Value("${registration.processor.manual.adjudication.subscriber.id}")
	private String subscriberId;

	@Autowired
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	@Autowired
	private CbeffUtil cbeffutil;

	@Autowired
	private Utilities utility;

//	@Value("${registration.processor.datasharejson}")
//	private String dataShareJsonString;

	@Autowired
	private Listener l;
	
	@Autowired
	private PacketManagerService packetManagerService;

	/** The utilities. */
	@Autowired
	private Utilities utilities;

	/** The audit log request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;
	/** The base packet repository. */
	@Autowired
	private BasePacketRepository<ManualVerificationEntity, String> basePacketRepository;

	/** The manual verification stage. */
	@Autowired
	private ManualVerificationStage manualVerificationStage;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	/*
	 * * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.manual.adjudication.service.
	 * ManualAdjudicationService#assignStatus(io.mosip.registration.processor.manual
	 * .adjudication.dto.UserDto)
	 */

	@Override
	public ManualVerificationDTO assignApplicant(UserDto dto) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				dto.getUserId(), "ManualVerificationServiceImpl::assignApplicant()::entry");
		ManualVerificationDTO manualVerificationDTO = new ManualVerificationDTO();
		List<ManualVerificationEntity> entities;
		String matchType = dto.getMatchType();
		try {
		
		if (dto.getUserId() == null || dto.getUserId().isEmpty()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					dto.getUserId(), "ManualVerificationServiceImpl::assignApplicant()::UserIDNotPresentException"
							+ PlatformErrorMessages.RPR_MVS_NO_USER_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			throw new UserIDNotPresentException(
					PlatformErrorMessages.RPR_MVS_NO_USER_ID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_USER_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
		}
		checkUserIDExistsInMasterList(dto);
		entities = basePacketRepository.getAssignedApplicantDetails(dto.getUserId(),
				ManualVerificationStatus.ASSIGNED.name());

		if (!(matchType.equalsIgnoreCase(DedupeSourceName.ALL.toString()) || isMatchTypeDemoOrBio(matchType))) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					dto.getUserId(), "ManualVerificationServiceImpl::assignApplicant()"
							+ PlatformErrorMessages.RPR_MVS_NO_MATCH_TYPE_PRESENT.getMessage());
			throw new MatchTypeNotFoundException(PlatformErrorMessages.RPR_MVS_NO_MATCH_TYPE_PRESENT.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_MATCH_TYPE_PRESENT.getMessage());
		}
		

		if (!entities.isEmpty()) {
			
			manualVerificationDTO.setRegId(entities.get(0).getId().getRegId());
			manualVerificationDTO.setUrl(getDatashareUrl(entities.get(0).getId().getRegId()));
			List<MatchDetail> gallery=new ArrayList<>();
			List<ManualVerificationEntity> mentities=entities.stream().filter(entity -> entity.getId()
					.getRegId().equals(manualVerificationDTO.getRegId())).collect(Collectors.toList());
			for(ManualVerificationEntity entity: mentities) {
				MatchDetail detail=new MatchDetail();
				detail.setMatchedRegId(entity.getId().getMatchedRefId());
				detail.setMatchedRefType(entity.getId().getMatchedRefType());
				detail.setReasonCode(entity.getReasonCode());
					detail.setUrl(getDatashareUrl(entity.getId().getMatchedRefId()));
					gallery.add(detail);
			}
			manualVerificationDTO.setGallery(gallery);
			
			manualVerificationDTO.setMvUsrId(entities.get(0).getMvUsrId());
			manualVerificationDTO.setStatusCode(entities.get(0).getStatusCode());
			
		} else {
			if (matchType.equalsIgnoreCase(DedupeSourceName.ALL.toString())) {
				entities = basePacketRepository.getFirstApplicantDetailsForAll(ManualVerificationStatus.PENDING.name());
			} else if (isMatchTypeDemoOrBio(matchType)) {
				entities = basePacketRepository.getFirstApplicantDetails(ManualVerificationStatus.PENDING.name(),
						matchType);
			}
			if (entities.isEmpty()) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
						dto.getUserId(), "ManualVerificationServiceImpl::assignApplicant()"
								+ PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
				throw new NoRecordAssignedException(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode(),
						PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
			} else {
				
				manualVerificationDTO.setMvUsrId(dto.getUserId());
				manualVerificationDTO.setStatusCode(ManualVerificationStatus.ASSIGNED.name());
				manualVerificationDTO.setUrl(getDatashareUrl(entities.get(0).getId().getRegId()));
				manualVerificationDTO.setRegId(entities.get(0).getId().getRegId());
				List<MatchDetail> gallery=new ArrayList<>();
				List<ManualVerificationEntity> mentities=entities.stream().filter(entity -> entity.getId()
						.getRegId().equals(manualVerificationDTO.getRegId())).collect(Collectors.toList());
				for(ManualVerificationEntity manualVerificationEntity: mentities) {
					manualVerificationEntity.setStatusCode(ManualVerificationStatus.ASSIGNED.name());
					manualVerificationEntity.setMvUsrId(dto.getUserId());
					ManualVerificationEntity updatedManualVerificationEntity = basePacketRepository
							.update(manualVerificationEntity);
					if (updatedManualVerificationEntity != null) {
						MatchDetail detail=new MatchDetail();
						detail.setMatchedRegId(updatedManualVerificationEntity.getId().getMatchedRefId());
						detail.setMatchedRefType(updatedManualVerificationEntity.getId().getMatchedRefType());
						detail.setReasonCode(updatedManualVerificationEntity.getReasonCode());
						detail.setUrl(getDatashareUrl(updatedManualVerificationEntity.getId().getMatchedRefId()));
						gallery.add(detail);
				}
			}
				manualVerificationDTO.setGallery(gallery);
			}
		}
		} catch (IOException | ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					dto.getUserId(), PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
		
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				dto.getUserId(), "ManualVerificationServiceImpl::assignApplicant()::exit");
		return manualVerificationDTO;

	}

	private String getDatashareUrl(String matchedRegId)
			throws JsonParseException, JsonMappingException, IOException, ApisResourceAccessException {
//		JSONObject dataShareJson=mapper.readValue(dataShareJsonString, JSONObject.class);
//		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
//				.getRegistrationStatus(matchedRegId);
//		File dataShareJsonFile=new File("");
//		String policyId="";
//		String subscriberId="";
//		List<String> pathSegments=new ArrayList<>();
//		pathSegments.add(policyId);
//		pathSegments.add(subscriberId);
//		ResponseWrapper<?> responseWrapper=(ResponseWrapper<?>) restClientService.postApi(ApiName.DATASHARECREATEURL,
//				MediaType.MULTIPART_FORM_DATA,pathSegments, null, null, dataShareJsonFile, ResponseWrapper.class);
//		DataShareResponseDto responsedto=mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
//				DataShareResponseDto.class);
		return null;// responsedto.getUrl();
	}

	private boolean isMatchTypeDemoOrBio(String matchType) {
		return matchType.equalsIgnoreCase(DedupeSourceName.DEMO.toString())
				|| matchType.equalsIgnoreCase(DedupeSourceName.BIO.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.manual.adjudication.service.
	 * ManualAdjudicationService#getApplicantFile(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public byte[] getApplicantFile(String regId, String fileName, String source) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

		byte[] file = null;
		InputStream fileInStream = null;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "ManualVerificationServiceImpl::getApplicantFile()::entry");
		if (regId == null || regId.isEmpty()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "ManualVerificationServiceImpl::getApplicantFile()"
							+ PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			throw new InvalidFileNameException(PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
					PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
		}
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(regId);
		if (registrationStatusDto == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "ManualVerificationServiceImpl::getApplicantFile()"
							+ PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getMessage());
			throw new InvalidFileNameException(PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getCode(),
					PlatformErrorMessages.TRANSACTIONS_NOT_AVAILABLE.getMessage());
		}
		String process = registrationStatusDto.getRegistrationType();
		if (PacketFiles.BIOMETRIC.name().equals(fileName)) {
            // get individual biometrics file name from id.json
			BiometricRecord biometricRecord = packetManagerService.getBiometrics(regId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, null, process);
            /*if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().isEmpty())
				throw new FileNotFoundInDestinationException("Identity json not present inside packet");
            JSONObject idJsonObject = JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class);
            JSONObject identity = JsonUtil.getJSONObject(idJsonObject,
                    utilities.getGetRegProcessorDemographicIdentity());
            if (identity == null)
				throw new FileNotFoundInDestinationException("Identity json not present inside packet");
            JSONObject individualBiometricsObject = JsonUtil.getJSONObject(identity, individualBiometrics);
			if (individualBiometricsObject == null)
				throw new FileNotFoundInDestinationException("Individual biometrics field not present inside identity");
            String biometricFileName = JsonUtil.getJSONValue(individualBiometricsObject, MappingJsonConstants.VALUE);
			fileInStream = getApplicantBiometricFile(regId, biometricFileName, fileSource);
		} else if (PacketFiles.DEMOGRAPHIC.name().equals(fileName)) {
			fileInStream = getApplicantDemographicFile(regId, PacketFiles.ID.name(), source);
		} else if (PacketFiles.PACKET_META_INFO.name().equals(fileName)) {
			fileInStream = getApplicantMetaInfoFile(regId, PacketFiles.PACKET_META_INFO.name(), source);
		} else {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "ManualVerificationServiceImpl::getApplicantFile()"
							+ PlatformErrorMessages.RPR_MVS_INVALID_FILE_REQUEST.getMessage());
			throw new InvalidFileNameException(PlatformErrorMessages.RPR_MVS_INVALID_FILE_REQUEST.getCode(),
					PlatformErrorMessages.RPR_MVS_INVALID_FILE_REQUEST.getMessage());*/
		}
		if (fileInStream == null)
			throw new FileNotFoundInDestinationException("File not found inside packet");
		try {
			file = IOUtils.toByteArray(fileInStream);
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "ManualVerificationServiceImpl::getApplicantFile()::exit");
		return file;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.manual.adjudication.service.
	 * ManualAdjudicationService#updatePacketStatus(io.mosip.registration.processor.
	 * manual.adjudication.dto.ManualVerificationDTO)
	 */
	@Override
	public ManualVerificationDecisionDto updatePacketStatus(ManualVerificationDecisionDto manualVerificationDTO, String stageName) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		String registrationId = manualVerificationDTO.getRegId();
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		messageDTO.setRid(manualVerificationDTO.getRegId());
		validateRegAndMactedRefIdEmpty(registrationId);

		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				manualVerificationDTO.getRegId(), "ManualVerificationServiceImpl::updatePacketStatus()::entry");
		if (!manualVerificationDTO.getStatusCode().equalsIgnoreCase(ManualVerificationStatus.REJECTED.name())
				&& !manualVerificationDTO.getStatusCode().equalsIgnoreCase(ManualVerificationStatus.APPROVED.name())) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "ManualVerificationServiceImpl::updatePacketStatus()"
							+ PlatformErrorMessages.RPR_MVS_INVALID_STATUS_UPDATE.getMessage());
			throw new InvalidUpdateException(PlatformErrorMessages.RPR_MVS_INVALID_STATUS_UPDATE.getCode(),
					PlatformErrorMessages.RPR_MVS_INVALID_STATUS_UPDATE.getMessage());
		}
		List<ManualVerificationEntity> entities=new ArrayList<>();
		
		 entities.addAll(basePacketRepository.getAllAssignedRecord(
				manualVerificationDTO.getRegId(), 
				manualVerificationDTO.getMvUsrId(), ManualVerificationStatus.ASSIGNED.name()));
		
		if (entities.isEmpty()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "ManualVerificationServiceImpl::updatePacketStatus()"
							+ PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
			throw new NoRecordAssignedException(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
		} else {
			for (int i = 0; i < entities.size(); i++) {
				ManualVerificationEntity manualVerificationEntity=entities.get(i);
				manualVerificationEntity.setStatusCode(manualVerificationDTO.getStatusCode());
				manualVerificationEntity.setReasonCode(manualVerificationDTO.getReasonCode());
				entities.set(i, manualVerificationEntity);
				
			}
			
		}
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(registrationId);
		messageDTO.setReg_type(RegistrationType.valueOf(registrationStatusDto.getRegistrationType()));
		try {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.MANUAL_VERIFICATION.toString());
			registrationStatusDto.setRegistrationStageName(stageName);

			if (manualVerificationDTO.getStatusCode().equalsIgnoreCase(ManualVerificationStatus.APPROVED.name())) {
				if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(RegistrationType.LOST.toString())) {
					for(ManualVerificationEntity detail: entities) {
						packetInfoManager.saveRegLostUinDet(registrationId, detail.getId().getMatchedRefId(),
							PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode(),
							ModuleName.MANUAL_VERIFICATION.toString());
					}
				}
				messageDTO.setIsValid(true);
				manualVerificationStage.sendMessage(messageDTO);
				registrationStatusDto.setStatusComment(StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());

				isTransactionSuccessful = true;
				description.setMessage(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode());

			} else {
				registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
				registrationStatusDto.setStatusComment(StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getCode());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

				description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getCode());
				messageDTO.setIsValid(Boolean.FALSE);
				manualVerificationStage.sendMessage(messageDTO);
			}
			List<ManualVerificationEntity> maVerificationEntity = new ArrayList<>();
			for(ManualVerificationEntity manualVerificationEntity: entities) {
			 maVerificationEntity.add( basePacketRepository.update(manualVerificationEntity));
			}
			manualVerificationDTO.setStatusCode(maVerificationEntity.get(0).getStatusCode());
			registrationStatusDto.setUpdatedBy(USER);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					manualVerificationDTO.getRegId(), description.getMessage());

		} catch (TablenotAccessibleException e) {

			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());

			description.setMessage(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					manualVerificationDTO.getRegId(), e.getMessage() + ExceptionUtils.getStackTrace(e));
		}

		finally {
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode()
					: description.getCode();
			String moduleName = ModuleName.MANUAL_VERIFICATION.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

			String eventId = "";
			String eventName = "";
			String eventType = "";
			eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				manualVerificationDTO.getRegId(), "ManualVerificationServiceImpl::updatePacketStatus()::exit");
		return manualVerificationDTO;

	}

	
	private void validateRegAndMactedRefIdEmpty(String registrationId) {
		if (registrationId == null || registrationId.isEmpty() ) {

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "ManualVerificationServiceImpl::updatePacketStatus()::InvalidFileNameException"
							+ PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			throw new InvalidFileNameException(PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
					PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
		}
	}


	@SuppressWarnings("unchecked")
	private void checkUserIDExistsInMasterList(UserDto dto) {
		ResponseWrapper<UserResponseDTOWrapper> responseWrapper;
		UserResponseDTOWrapper userResponseDTOWrapper;
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(ManualVerificationConstants.USERS);
		pathSegments.add(dto.getUserId());
		Date date = Calendar.getInstance().getTime();
		DateFormat dateFormat = new SimpleDateFormat(ManualVerificationConstants.TIME_FORMAT);
		String effectiveDate = dateFormat.format(date);
		// pathSegments.add("2019-05-16T06:12:52.994Z");
		pathSegments.add(effectiveDate);
		try {

			responseWrapper = (ResponseWrapper<UserResponseDTOWrapper>) restClientService.getApi(ApiName.MASTER,
					pathSegments, "", "", ResponseWrapper.class);

			if (responseWrapper.getResponse() != null) {
				userResponseDTOWrapper = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
						UserResponseDTOWrapper.class);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
						dto.getUserId(),
						"ManualVerificationServiceImpl::checkUserIDExistsInMasterList()::get MASTER USERS service call ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(userResponseDTOWrapper));
				if (!userResponseDTOWrapper.getUserResponseDto().get(0).getStatusCode()
						.equals(ManualVerificationConstants.ACT)) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), null,
							PlatformErrorMessages.RPR_MVS_USER_STATUS_NOT_ACTIVE.getCode(),
							PlatformErrorMessages.RPR_MVS_USER_STATUS_NOT_ACTIVE.getMessage() + dto.getUserId());
					throw new UserIDNotPresentException(PlatformErrorMessages.RPR_MVS_USER_STATUS_NOT_ACTIVE.getCode(),
							PlatformErrorMessages.RPR_MVS_USER_STATUS_NOT_ACTIVE.getMessage());
				}
			} else {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), null,
						PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getCode(),
						PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getMessage());
				throw new UserIDNotPresentException(PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getCode(),
						PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getMessage());

			}
		} catch (ApisResourceAccessException | IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), null,
					PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getMessage() + e);
			throw new UserIDNotPresentException(PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_USER_ID_PRESENT.getMessage());

		}
	}

	/*
	 * Get matched ref id for given RID and form request ,push to queue
	 */
	public void pushRequestToQueue(String refId) throws Exception {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				refId, "ManualVerificationServiceImpl::pushRequestToQueue()::entry");
		List<ManualVerificationEntity> mves = getMatchingEntitiesforRefId(refId);
		if (mves.size() == 0 || null == mves)
			throw new MatchedRefNotExistsException(
					PlatformErrorMessages.RPR_MVS_NO_MATCHEDRID_FOUND_FOR_GIVEN_RID.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_MATCHEDRID_FOUND_FOR_GIVEN_RID.getMessage());

		ManualAdjudicationRequestDTO mar = formAdjudicationRequest(mves);
		System.out.println("Request===>"+mar);
		
		l.sendAdjudicationRequest(mar);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				refId, "ManualVerificationServiceImpl::pushRequestToQueue()::entry");

	}

	
	private String getDataShareUrl(String id, String process) throws Exception {
		Map<String,List<String>> typeAndSubtypMap=createTypeSubtypeMapping();
		List<String> modalities=new ArrayList<>();
		for(Map.Entry<String,List<String>> entry:typeAndSubtypMap.entrySet()) {
			if(entry.getValue()==null) {
				modalities.add(entry.getKey());
			} else {
				modalities.addAll(entry.getValue());
			}
		}
		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);
		BiometricRecord biometricRecord = packetManagerService.getBiometrics(id, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, modalities, process);
		byte[] content = cbeffutil.createXML(BIRConverter.convertSegmentsToBIRList(biometricRecord.getSegments()));

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("name", individualBiometricsLabel);
		map.add("filename", individualBiometricsLabel);

		ByteArrayResource contentsAsResource = new ByteArrayResource(content) {
			@Override
			public String getFilename() {
				return individualBiometricsLabel;
			}
		};
		map.add("file", contentsAsResource);

		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(policyId);
		pathSegments.add(subscriberId);

		DataShareResponseDto response = (DataShareResponseDto) registrationProcessorRestClientService.postApi(ApiName.DATASHARECREATEURL, MediaType.MULTIPART_FORM_DATA, pathSegments, null, null, map, DataShareResponseDto.class);
		if (response == null || (response.getErrors() != null && response.getErrors().size() >0))
			throw new DataShareException(response == null ? "Datashare response is null" : response.getErrors().get(0).getMessage());

		return response.getUrl();
	}
	
	
	public Map<String, List<String>> createTypeSubtypeMapping() throws ApisResourceAccessException, DataShareException, JsonParseException, JsonMappingException, com.fasterxml.jackson.core.JsonProcessingException, IOException{
		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
				ApiName.PMS, Lists.newArrayList(subscriberId, ManualVerificationConstants.POLICY_ID, policyId), "", "", ResponseWrapper.class);
		if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() >0)) {
			throw new DataShareException(policyResponse == null ? "Policy Response response is null" : policyResponse.getErrors().get(0).getMessage());
			
		} else {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
			LinkedHashMap<String, Object> policies = (LinkedHashMap<String, Object>) responseMap.get(ManualVerificationConstants.POLICIES);
			List<?> attributes = (List<?>) policies.get(ManualVerificationConstants.SHAREABLE_ATTRIBUTES);
			ObjectMapper mapper = new ObjectMapper();
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(attributes.get(0)),
					ShareableAttributes.class);
			for (Source source : shareableAttributes.getSource()) {
				List<Filter> filterList = source.getFilter();
				if (filterList != null && !filterList.isEmpty()) {

					filterList.forEach(filter -> {
						if (filter.getSubType() != null && !filter.getSubType().isEmpty()) {
							typeAndSubTypeMap.put(filter.getType(), filter.getSubType());
						} else {
							typeAndSubTypeMap.put(filter.getType(), null);
						}
					});
				}
			}
		}
		return typeAndSubTypeMap;
		
	}

	/*
	 * get matched ref id for a given registration id
	 */
	private List<ManualVerificationEntity> getMatchingEntitiesforRefId(String rid) {
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"ManualVerificationServiceImpl::getMatchingEntitiesforRefId()::entry");

		List<ManualVerificationEntity> matchedEntities = basePacketRepository.getMatchedIds(rid);

		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"ManualVerificationServiceImpl::getMatchingEntitiesforRefId()::entry");

		return matchedEntities;
	}

	/*
	 * Form manual adjudication request
	 */
	private ManualAdjudicationRequestDTO formAdjudicationRequest(List<ManualVerificationEntity> mve) throws Exception {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ManualVerificationServiceImpl::formAdjudicationRequest()::entry");

		ManualAdjudicationRequestDTO req = new ManualAdjudicationRequestDTO();
		req.setId(ManualVerificationConstants.MANUAL_ADJUDICATION_ID);
		req.setVersion(ManualVerificationConstants.VERSION);
		req.setRequestId(mve.get(0).getRequestId());
		req.setReferenceId(mve.get(0).getId().getRegId());
		InternalRegistrationStatusDto registrationStatusDto = null;
		registrationStatusDto = registrationStatusService.getRegistrationStatus(mve.get(0).getId().getRegId());
		try {

			req.setReferenceURL(
					getDataShareUrl(mve.get(0).getId().getRegId(), registrationStatusDto.getRegistrationType()));

		} catch (PacketManagerException | ApisResourceAccessException ex) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					ex.getErrorCode(), ex.getErrorText());
			req.setReferenceURL(null);

		}

		List<ReferenceIds> referenceIds = new ArrayList<>();
		mve.forEach(e -> {
			ReferenceIds r = new ReferenceIds();
			InternalRegistrationStatusDto registrationStatusDto1 = null;
			registrationStatusDto1 = registrationStatusService.getRegistrationStatus(e.getId().getMatchedRefId());
		
			try {
				r.setReferenceId(e.getId().getMatchedRefId());
				r.setReferenceURL(getDataShareUrl(e.getId().getMatchedRefId(),registrationStatusDto1.getRegistrationType()));
				referenceIds.add(r);
			} catch (PacketManagerException | ApisResourceAccessException ex) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), ex.getErrorCode(), ex.getErrorText());
				r.setReferenceURL(null);
				referenceIds.add(r);
			} catch (Exception exp) {

				exp.printStackTrace();
			}

		});
		Gallery g = new Gallery();
		g.setReferenceIds(referenceIds);
		req.setGallery(g);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ManualVerificationServiceImpl::formAdjudicationRequest()::entry");

		return req;
	}

	/*
	 * Once response is obtained from queue it is saved in manual verification
	 * entity
	 */
	public void saveToDB(ManualAdjudicationResponseDTO res) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				res.getId(), "ManualVerificationServiceImpl::saveToDB()::entry");

		if (res.getCandidateList().getCount() > 0) {
			res.getCandidateList().getCandidates().forEach(candidate -> {
				ManualVerificationEntity mve = basePacketRepository.getManualVerificationEntitty(res.getRequestId(),
				 candidate.getReferenceId());
				mve.setReponseText(res.toString().getBytes());
				basePacketRepository.update(mve);
			});

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				res.getId(), "ManualVerificationServiceImpl::saveToDB()::entry");

	}

	/*
	 * This method will be called from the event bus passing messageDTO object
	 * containing rid Based o Rid fetch match reference Id and form request which is
	 * pushed to queue and update Manual verification entity
	 */

	@Override
	public MessageDTO process(MessageDTO object) {
		try {
			object.setInternalError(false);
			object.setIsValid(false);
			object.setMessageBusAddress(MessageBusAddress.MANUAL_VERIFICATION_BUS_IN);

			if (null == object.getRid() || object.getRid().isEmpty())
				throw new InvalidRidException(PlatformErrorMessages.RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
						PlatformErrorMessages.RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					object.getRid(), "ManualVerificationServiceImpl::process()::entry");
			pushRequestToQueue(object.getRid());
			updateManualVerificationEntityRID(object.getRid());

		} catch (DataShareException de) {
			object.setInternalError(true);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					de.getErrorCode(), de.getErrorText());

		} catch (InvalidRidException exp) {
			object.setInternalError(true);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), null, exp.getErrorCode(), exp.getErrorText());
		} catch (MatchedRefNotExistsException exp) {
			object.setInternalError(true);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					exp.getErrorCode(), exp.getErrorText());

		} catch (Exception e) {
			object.setInternalError(true);
			e.printStackTrace();
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					e.getMessage(), e.getMessage());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				object.getRid(), "ManualVerificationServiceImpl::process()::entry");

		return object;
	}

	/*
	 * Update manual verification entity once request is pushed to queue for a given
	 * RID
	 */
	private void updateManualVerificationEntityRID(String rid) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"ManualVerificationServiceImpl::updateManualVerificationEntityRID()::entry");
		ManualVerificationEntity mve = basePacketRepository.getManualVerificationEntityForRID(rid);
		mve.setStatusCode("Inqueue");
		mve.setStatusComment("Sent to manual adjudication queue");
		mve.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
		basePacketRepository.update(mve);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"ManualVerificationServiceImpl::updateManualVerificationEntityRID()::exit");

	}

}
