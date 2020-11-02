package io.mosip.registration.processor.manual.verification.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.packet.manager.exception.FileNotFoundInDestinationException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
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
import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.manual.verification.dto.MatchDetail;
import io.mosip.registration.processor.manual.verification.dto.UserDto;
import io.mosip.registration.processor.manual.verification.exception.InvalidFileNameException;
import io.mosip.registration.processor.manual.verification.exception.InvalidUpdateException;
import io.mosip.registration.processor.manual.verification.exception.MatchTypeNotFoundException;
import io.mosip.registration.processor.manual.verification.exception.NoRecordAssignedException;
import io.mosip.registration.processor.manual.verification.exception.UserIDNotPresentException;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

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
	
//	@Value("${registration.processor.datasharejson}")
//	private String dataShareJsonString;

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

	private String getDatashareUrl(String matchedRegId) throws JsonParseException, JsonMappingException, IOException, ApisResourceAccessException {
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
            JSONObject mappingJson = utilities.getRegistrationProcessorMappingJson();
            String individualBiometrics = JsonUtil
                    .getJSONValue(JsonUtil.getJSONObject(mappingJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);
            // get individual biometrics file name from id.json
			BiometricRecord biometricRecord = packetManagerService.getBiometrics(regId, individualBiometrics, null, source, process);
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

}
