package io.mosip.registration.processor.manual.verification.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.FileNotFoundInDestinationException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import io.mosip.registration.processor.core.exception.util.PacketStructure;
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
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationStatus;
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

	/** The utilities. */
	@Autowired
	private Utilities utilities;

    @Autowired
    private IdSchemaUtils idSchemaUtils;

	/** The audit log request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The filesystem ceph adapter impl. */
	@Autowired
	private PacketReaderService packetReaderService;

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
		ManualVerificationEntity manualVerificationEntity;

		if (!entities.isEmpty()) {
			manualVerificationEntity = entities.get(0);
			manualVerificationDTO.setRegId(manualVerificationEntity.getId().getRegId());
			manualVerificationDTO.setMatchedRefId(manualVerificationEntity.getId().getMatchedRefId());
			manualVerificationDTO.setMatchedRefType(manualVerificationEntity.getId().getMatchedRefType());
			manualVerificationDTO.setMvUsrId(manualVerificationEntity.getMvUsrId());
			manualVerificationDTO.setStatusCode(manualVerificationEntity.getStatusCode());
			manualVerificationDTO.setReasonCode(manualVerificationEntity.getReasonCode());
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
				manualVerificationEntity = entities.get(0);
				manualVerificationEntity.setStatusCode(ManualVerificationStatus.ASSIGNED.name());
				manualVerificationEntity.setMvUsrId(dto.getUserId());
				ManualVerificationEntity updatedManualVerificationEntity = basePacketRepository
						.update(manualVerificationEntity);
				if (updatedManualVerificationEntity != null) {
					manualVerificationDTO.setRegId(updatedManualVerificationEntity.getId().getRegId());
					manualVerificationDTO.setMatchedRefId(updatedManualVerificationEntity.getId().getMatchedRefId());
					manualVerificationDTO
							.setMatchedRefType(updatedManualVerificationEntity.getId().getMatchedRefType());
					manualVerificationDTO.setMvUsrId(updatedManualVerificationEntity.getMvUsrId());
					manualVerificationDTO.setStatusCode(updatedManualVerificationEntity.getStatusCode());
				}
			}

		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				dto.getUserId(), "ManualVerificationServiceImpl::assignApplicant()::exit");
		return manualVerificationDTO;

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
	public byte[] getApplicantFile(String regId, String fileName, String source) throws
			ApiNotAccessibleException, io.mosip.kernel.core.exception.IOException, IOException, PacketDecryptionFailureException {

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
		if (PacketFiles.BIOMETRIC.name().equals(fileName)) {
            JSONObject mappingJson = utilities.getRegistrationProcessorMappingJson();
            String individualBiometrics = JsonUtil
                    .getJSONValue(JsonUtil.getJSONObject(mappingJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);
            // get individual biometrics file name from id.json
            String fileSource = idSchemaUtils.getSource(individualBiometrics, packetReaderService.getIdSchemaVersionFromPacket(regId));
            InputStream idJsonStream = null;
            if (fileSource != null) {
                idJsonStream = packetReaderService.getFile(regId,
                        PacketFiles.ID.name(), fileSource);
            }
            if (idJsonStream == null)
				throw new FileNotFoundInDestinationException("Identity json not present inside packet");
            String idJsonString = IOUtils.toString(idJsonStream, "UTF-8");
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
					PlatformErrorMessages.RPR_MVS_INVALID_FILE_REQUEST.getMessage());
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

	/**
	 * Gets the applicant biometric file.
	 *
	 * @param regId
	 *            the reg id
	 * @param fileName
	 *            the file name
	 * @return the applicant biometric file
	 * @throws IOException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws ApisResourceAccessException
	 * @throws PacketDecryptionFailureException
	 */
	private InputStream getApplicantBiometricFile(String regId, String fileName, String source)
			throws ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, IOException, PacketDecryptionFailureException {
		return packetReaderService.getFile(regId, fileName, source != null ? source : defaultSource);
	}

	/**
	 * Gets the applicant demographic file.
	 *
	 * @param regId
	 *            the reg id
	 * @param fileName
	 *            the file name
	 * @return the applicant demographic file
	 * @throws IOException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws ApisResourceAccessException
	 * @throws PacketDecryptionFailureException
	 */
	private InputStream getApplicantDemographicFile(String regId, String fileName, String source)
			throws PacketDecryptionFailureException, ApiNotAccessibleException,
			io.mosip.kernel.core.exception.IOException, IOException {
		return packetReaderService.getFile(regId, fileName, source != null ? source : defaultSource);
	}

	private InputStream getApplicantMetaInfoFile(String regId, String fileName, String source) throws PacketDecryptionFailureException,
			ApiNotAccessibleException, io.mosip.kernel.core.exception.IOException, IOException {
		return packetReaderService.getFile(regId, PacketStructure.PACKETMETAINFO, source != null ? source : defaultSource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.manual.adjudication.service.
	 * ManualAdjudicationService#updatePacketStatus(io.mosip.registration.processor.
	 * manual.adjudication.dto.ManualVerificationDTO)
	 */
	@Override
	public ManualVerificationDTO updatePacketStatus(ManualVerificationDTO manualVerificationDTO, String stageName) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		String registrationId = manualVerificationDTO.getRegId();
		String matchedRefId = manualVerificationDTO.getMatchedRefId();
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		messageDTO.setRid(manualVerificationDTO.getRegId());
		validateRegAndMactedRefIdEmpty(registrationId, matchedRefId);

		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		ManualVerificationEntity manualVerificationEntity;
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
		List<ManualVerificationEntity> entities = basePacketRepository.getSingleAssignedRecord(
				manualVerificationDTO.getRegId(), manualVerificationDTO.getMatchedRefId(),
				manualVerificationDTO.getMvUsrId(), ManualVerificationStatus.ASSIGNED.name());

		if (entities.isEmpty()) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "ManualVerificationServiceImpl::updatePacketStatus()"
							+ PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
			throw new NoRecordAssignedException(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
		} else {
			manualVerificationEntity = entities.get(0);
			manualVerificationEntity.setStatusCode(manualVerificationDTO.getStatusCode());
			manualVerificationEntity.setReasonCode(manualVerificationDTO.getReasonCode());
		}
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(registrationId);
		messageDTO.setReg_type(RegistrationType.valueOf(registrationStatusDto.getRegistrationType()));
		try {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.MANUAL_VERIFICATION.toString());
			registrationStatusDto.setRegistrationStageName(stageName);

			if (manualVerificationDTO.getStatusCode().equalsIgnoreCase(ManualVerificationStatus.APPROVED.name())) {
				if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(RegistrationType.LOST.toString()))
					packetInfoManager.saveRegLostUinDet(registrationId, manualVerificationDTO.getMatchedRefId(),
							PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode(),
							ModuleName.MANUAL_VERIFICATION.toString());
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
			ManualVerificationEntity maVerificationEntity = basePacketRepository.update(manualVerificationEntity);
			manualVerificationDTO.setStatusCode(maVerificationEntity.getStatusCode());
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

	private void validateRegAndMactedRefIdEmpty(String registrationId, String matchedRefId) {

		if (registrationId == null || registrationId.isEmpty() || matchedRefId == null || matchedRefId.isEmpty()) {

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
