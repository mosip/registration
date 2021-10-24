package io.mosip.registration.processor.biometric.authentication.stage;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.biometric.authentication.constants.BiometricAuthenticationConstants;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.BioTypeException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.AuthUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BiometricAuthenticationStage extends MosipVerticleAPIManager {
	private static Logger regProcLogger = RegProcessorLogger.getLogger(BiometricAuthenticationStage.class);
	private static final String FILE_NOT_PRESENT_ERROR = "KER-PUT-007";

	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private Utilities utility;



	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	private AuthUtil authUtil;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	@Autowired
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	@Value("${mosip.kernel.applicant.type.age.limit}")
	private String ageLimit;

	/** server port number. */
	@Value("${server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.biometric.authentication.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationservice;

	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.BIOMETRIC_AUTHENTICATION_BUS_IN,
				MessageBusAddress.BIOMETRIC_AUTHENTICATION_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.BIOMETRIC_AUTHENTICATION_BUS_IN,
				MessageBusAddress.BIOMETRIC_AUTHENTICATION_BUS_OUT));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"BiometricAuthenticationStage::BiometricAuthenticationStage::entry");
		String registrationId = object.getRid();
		object.setMessageBusAddress(MessageBusAddress.BIOMETRIC_AUTHENTICATION_BUS_IN);
		object.setIsValid(Boolean.FALSE);
		object.setInternalError(Boolean.FALSE);
		InternalRegistrationStatusDto registrationStatusDto=registrationStatusService
				.getRegistrationStatus(registrationId);

		registrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.BIOMETRIC_AUTHENTICATION.toString());
		registrationStatusDto.setRegistrationStageName(this.getClass().getSimpleName());
		SyncRegistrationEntity regEntity = syncRegistrationservice.findByRegistrationId(registrationId);
		String description = "";
		String code = "";
		boolean isTransactionSuccessful = false;

		try {
	        String process = registrationStatusDto.getRegistrationType();
			String registartionType = regEntity.getRegistrationType();
			int applicantAge = utility.getApplicantAge(registrationId, process, ProviderStageName.BIO_AUTH);
			int childAgeLimit = Integer.parseInt(ageLimit);
			String applicantType = BiometricAuthenticationConstants.ADULT;
			if (applicantAge <= childAgeLimit && applicantAge > 0) {
				applicantType = BiometricAuthenticationConstants.CHILD;
			}
			if (isUpdateAdultPacket(registartionType, applicantType)) {

				String biometricsLabel = packetManagerService.getFieldByMappingJsonKey(
						registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, registrationStatusDto.getRegistrationType(), ProviderStageName.BIO_AUTH);
				if (StringUtils.isEmpty(biometricsLabel)) {
					isTransactionSuccessful = checkIndividualAuthentication(registrationId, process,
							registrationStatusDto);
					description = isTransactionSuccessful
							? PlatformSuccessMessages.RPR_PKR_BIOMETRIC_AUTHENTICATION.getMessage()
							: PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_FAILED.getMessage();
				} else {
					String individualBiometricsFileName = JsonUtil
							.getJSONValue(JsonUtil.readValueWithUnknownProperties(biometricsLabel, JSONObject.class),
									MappingJsonConstants.VALUE);
					if (individualBiometricsFileName != null && !individualBiometricsFileName.isEmpty()) {
						BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
								registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.BIO_AUTH);
						if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().isEmpty()) {
							isTransactionSuccessful = false;
							description = StatusUtil.BIOMETRIC_FILE_NOT_FOUND.getMessage();
							regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
									LoggerFileConstant.REGISTRATIONID.toString(), registrationId, description);
							registrationStatusDto.setStatusComment(description);
							registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_FILE_NOT_FOUND.getCode());
						} else {
							isTransactionSuccessful = true;
						}
					} else {
						isTransactionSuccessful = true;
						description = PlatformSuccessMessages.RPR_PKR_BIOMETRIC_AUTHENTICATION.getMessage();
					}
				}
			}

			else {
				object.setIsValid(true);
				object.setInternalError(false);
				isTransactionSuccessful = true;
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"BiometricAuthenticationStage::success");
				if (SyncTypeDto.NEW.toString().equalsIgnoreCase(registartionType)) {
					description = BiometricAuthenticationConstants.NEW_PACKET_DESCRIPTION + registrationId;
				} else
					description = BiometricAuthenticationConstants.CHILD_PACKET_DESCRIPTION + registrationId;
			}
			if (isTransactionSuccessful) {
				object.setIsValid(Boolean.TRUE);
				object.setInternalError(Boolean.FALSE);
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_AUTHENTICATION_SUCCESS.getCode());
				registrationStatusDto.setStatusComment(StatusUtil.BIOMETRIC_AUTHENTICATION_SUCCESS.getMessage());
			} else {
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				registrationStatusDto.setStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
			}

		} catch (IOException | NoSuchAlgorithmException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			object.setIsValid(false);
			object.setInternalError(true);
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			code = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_IOEXCEPTION.getCode();
			description = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_IOEXCEPTION.getMessage();
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), code, registrationId,
					description + e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (JsonProcessingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION));
			isTransactionSuccessful = false;
			description = (PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
			code = (PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			e.printStackTrace();
		} catch (PacketManagerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			description = (PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
			code = (PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (ApisResourceAccessException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage()+ e.getMessage()));
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			code = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_API_RESOURCE_EXCEPTION.getCode();
			description = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_API_RESOURCE_EXCEPTION.getMessage();
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), code, registrationId,
					description + e.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
		} 
		catch (AuthSystemException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setSubStatusCode(StatusUtil.AUTH_SYSTEM_EXCEPTION.getCode());
			registrationStatusDto.setStatusComment(
					trimExceptionMessage.trimExceptionMessage(StatusUtil.AUTH_SYSTEM_EXCEPTION.getMessage()+ e.getMessage()));
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.AUTH_SYSTEM_EXCEPTION));
			code = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_AUTH_SYSTEM_EXCEPTION.getCode();
			description = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_AUTH_SYSTEM_EXCEPTION.getMessage();
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), code, registrationId,
					description + e.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);
			
		} 
		catch (Exception ex) {
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			code = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_FAILED.getCode();
			description = PlatformErrorMessages.BIOMETRIC_AUTHENTICATION_FAILED.getMessage();
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), code, registrationId,
					description + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(Boolean.FALSE);

		} finally {

			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_PKR_BIOMETRIC_AUTHENTICATION.getCode()
					: code;
			String moduleName = ModuleName.BIOMETRIC_AUTHENTICATION.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			description = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_PKR_BIOMETRIC_AUTHENTICATION.getMessage()
					: description;
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description, eventId, eventName, eventType, moduleId,
					moduleName, registrationId);
		}

		return object;
	}

	private boolean isUpdateAdultPacket(String registartionType, String applicantType) {
		return (registartionType.equalsIgnoreCase(RegistrationType.UPDATE.name())
				|| registartionType.equalsIgnoreCase(RegistrationType.RES_UPDATE.name()))
				&& applicantType.equalsIgnoreCase(BiometricAuthenticationConstants.ADULT);
	}

	private boolean idaAuthenticate(List<BIR> segments, String uin, InternalRegistrationStatusDto registrationStatusDto)
			throws IOException, ApisResourceAccessException, BioTypeException, AuthSystemException, CertificateException, NoSuchAlgorithmException {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();

		boolean idaAuth = false;
		AuthResponseDTO authResponseDTO = authUtil.authByIdAuthentication(uin,
				BiometricAuthenticationConstants.INDIVIDUAL_TYPE_USERID, segments);
		if ((authResponseDTO.getErrors() == null || authResponseDTO.getErrors().isEmpty())
				&& authResponseDTO.getResponse().isAuthStatus()) {
			idaAuth = true;
		} else {
			List<io.mosip.registration.processor.core.auth.dto.ErrorDTO> errors = authResponseDTO.getErrors();
			if (errors != null) {
			if (errors.stream().anyMatch(error -> error.getErrorCode().equalsIgnoreCase("IDA-MLC-007"))) {
				throw new AuthSystemException(PlatformErrorMessages.RPR_AUTH_SYSTEM_EXCEPTION.getMessage());
			} else {
			String result = errors.stream().map(s -> s.getErrorMessage() + " ").collect(Collectors.joining());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setSubStatusCode(StatusUtil.INDIVIDUAL_BIOMETRIC_AUTHENTICATION_FAILED.getCode());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.INDIVIDUAL_BIOMETRIC_AUTHENTICATION_FAILED.getMessage()
							+ result));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), "", registrationStatusDto.getRegistrationId(),
					"IDA Authentiacation failed - " + authResponseDTO.getErrors());
			idaAuth = false;
				}
			}

		}
		return idaAuth;
	}

	private boolean checkIndividualAuthentication(String registrationId, String process,
			InternalRegistrationStatusDto registrationStatusDto) throws IOException, BioTypeException,
			AuthSystemException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, CertificateException, NoSuchAlgorithmException {

		BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(registrationId,
                MappingJsonConstants.AUTHENTICATION_BIOMETRICS, process, ProviderStageName.BIO_AUTH);
		if (biometricRecord == null || CollectionUtils.isEmpty(biometricRecord.getSegments())) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto
					.setStatusComment(StatusUtil.BIOMETRIC_AUTHENTICATION_FAILED_FILE_NOT_FOUND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_AUTHENTICATION_FAILED_FILE_NOT_FOUND.getCode());
			return false;
		}
		String uin = utility.getUIn(registrationId, process, ProviderStageName.BIO_AUTH);

		return idaAuthenticate(biometricRecord.getSegments(), uin, registrationStatusDto);

	}

}
