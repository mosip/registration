package io.mosip.registration.processor.quality.checker.stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
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
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.quality.checker.exception.FileMissingException;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class QualityCheckerStage.
 * 
 * @author M1048358 Alok Ranjan
 */
@Component
@Configuration
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.quality.checker.config",
		"io.mosip.registration.processor.stages.config", 
		"io.mosip.registrationprocessor.stages.config", 
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", 
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", 
		"io.mosip.kernel.idobjectvalidator.config",
		"io.mosip.registration.processor.core.kernel.beans", "io.mosip.kernel.biosdk.provider.impl" })
public class QualityCheckerStage extends MosipVerticleAPIManager {
	
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.quality.checker.";

	/** The Constant FINGER. */
	private static final String FINGER = "FINGER";

	/** The Constant THUMB. */
	private static final String THUMB = "Thumb";

	/** The Constant RIGHT. */
	private static final String RIGHT = "Right";

	/** The Constant LEFT. */
	private static final String LEFT = "Left";

	/** The Constant IRIS. */
	private static final String IRIS = "IRIS";

	/** The Constant FACE. */
	private static final String FACE = "FACE";

	/** The Constant UTF_8. */
	public static final String UTF_8 = "UTF-8";

	/** The Constant VALUE. */
	public static final String VALUE = "value";

	private TrimExceptionMessage trimExceptionMsg = new TrimExceptionMessage();

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The iris threshold. */
	@Value("${mosip.iris_threshold}")
	private Integer irisThreshold;

	/** The left finger threshold. */
	@Value("${mosip.leftslap_fingerprint_threshold}")
	private Integer leftFingerThreshold;

	/** The right finger threshold. */
	@Value("${mosip.rightslap_fingerprint_threshold}")
	private Integer rightFingerThreshold;

	/** The thumb finger threshold. */
	@Value("${mosip.thumbs_fingerprint_threshold}")
	private Integer thumbFingerThreshold;

	/** The face threshold. */
	@Value("${mosip.facequalitythreshold}")
	private Integer faceThreshold;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.quality.checker.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** Mosip router for APIs */
	@Autowired
	private MosipRouter router;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/** The registration status mapper util. */
	@Autowired
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(QualityCheckerStage.class);

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	private MosipEventBus mosipEventBus = null;

	private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

	@Autowired
	private BioAPIFactory bioApiFactory;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.QUALITY_CHECKER_BUS_IN,
				MessageBusAddress.QUALITY_CHECKER_BUS_OUT, messageExpiryTimeLimit);
	}
	
	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	@Override
	public void start() {
		router.setRoute(
				this.postUrl(getVertx(), MessageBusAddress.OSI_BUS_IN, MessageBusAddress.OSI_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#process(
	 * java.lang.Object)
	 */
	@Override
	public MessageDTO process(MessageDTO object) {
		object.setMessageBusAddress(MessageBusAddress.QUALITY_CHECKER_BUS_IN);
		String regId = object.getRid();
		LogDescription description = new LogDescription();
		Boolean isTransactionSuccessful = Boolean.FALSE;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"QualityCheckerStage::process()::entry");

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(regId);

		try {
			String individualBiometricsObject = packetManagerService.getFieldByMappingJsonKey(
					regId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, registrationStatusDto.getRegistrationType(), ProviderStageName.QUALITY_CHECKER);
			if (StringUtils.isEmpty(individualBiometricsObject)) {
				description.setCode(PlatformErrorMessages.INDIVIDUAL_BIOMETRIC_NOT_FOUND.getCode());
				description.setMessage(PlatformErrorMessages.INDIVIDUAL_BIOMETRIC_NOT_FOUND.getMessage());
				object.setIsValid(Boolean.TRUE);
				isTransactionSuccessful = Boolean.TRUE;
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				registrationStatusDto.setStatusComment(StatusUtil.INDIVIDUAL_BIOMETRIC_NOT_FOUND.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.INDIVIDUAL_BIOMETRIC_NOT_FOUND.getCode());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
						"Individual Biometric parameter is not present in ID Json");
			} else {
				BiometricRecord biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
						regId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, registrationStatusDto.getRegistrationType(), ProviderStageName.QUALITY_CHECKER);

				if (biometricRecord == null || CollectionUtils.isEmpty(biometricRecord.getSegments())) {
					biometricRecord = packetManagerService.getBiometricsByMappingJsonKey(
							regId, MappingJsonConstants.AUTHENTICATION_BIOMETRICS, registrationStatusDto.getRegistrationType(), ProviderStageName.QUALITY_CHECKER);
				}

				if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0) {
					description.setCode(PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getCode());
					description.setMessage(PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getMessage());
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), regId,
							PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getMessage());
					throw new FileMissingException(PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getCode(),
							PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getMessage());
				}
				List<BIR> birList = biometricRecord.getSegments();
				// get individual biometrics file name from id.json
				int scoreCounter = 0;
				for (BIR bir : birList) {
					BiometricType biometricType = bir.getBdbInfo().getType().get(0);
					List<String> subtype = bir.getBdbInfo().getSubtype();
					Integer threshold = getThresholdBasedOnType(biometricType, subtype);
					float[] qualityScoreresponse;
					BIR[] birArray = new BIR[1];
					birArray[0]=bir;
					qualityScoreresponse = getBioSdkInstance(biometricType).getSegmentQuality(birArray, null);
					int qualityScore = Float.valueOf(qualityScoreresponse[0]).intValue();
					if (qualityScore < threshold) {
						object.setIsValid(Boolean.FALSE);
						isTransactionSuccessful = Boolean.FALSE;
						registrationStatusDto
								.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
						registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
						registrationStatusDto.setStatusComment(StatusUtil.BIOMETRIC_QUALITY_CHECK_FAILED.getMessage());
						registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_QUALITY_CHECK_FAILED.getCode());
						description.setCode(PlatformErrorMessages.BIOMETRIC_QUALITY_CHECK_FAILED.getCode());
						description.setMessage(PlatformErrorMessages.BIOMETRIC_QUALITY_CHECK_FAILED.getMessage());
						break;
					} else {
						scoreCounter++;
					}
				}
				if (scoreCounter == birList.size()) {
					object.setIsValid(Boolean.TRUE);
					description.setCode(PlatformSuccessMessages.RPR_QUALITY_CHECK_SUCCESS.getCode());
					description.setMessage(PlatformSuccessMessages.RPR_QUALITY_CHECK_SUCCESS.getMessage());
					isTransactionSuccessful = Boolean.TRUE;
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
					registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					registrationStatusDto.setStatusComment(StatusUtil.BIOMETRIC_QUALITY_CHECK_SUCCESS.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.BIOMETRIC_QUALITY_CHECK_SUCCESS.getCode());
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), regId, "QualityCheckerImpl::success");
				}
			}

		} catch (ApisResourceAccessException e) {
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			registrationStatusDto.setStatusComment(trimExpMessage
					.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			object.setInternalError(true);
			object.setIsValid(false);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.name() + ExceptionUtils.getStackTrace(e));

			description.setMessage(PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.getCode());
		} catch (FileMissingException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(StatusUtil.BIO_METRIC_FILE_MISSING.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_METRIC_FILE_MISSING.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXCEPTION));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			object.setIsValid(false);
			isTransactionSuccessful = false;
			description.setCode(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getMessage());
			object.setRid(regId);

		} catch (BiometricException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimExceptionMsg
					.trimExceptionMessage(StatusUtil.BIO_METRIC_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.BIO_METRIC_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXCEPTION));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getMessage() + ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			isTransactionSuccessful = false;
			description.setCode(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getMessage());
			object.setRid(regId);
		}
		catch (JsonProcessingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimExceptionMsg.trimExceptionMessage(StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION));
			isTransactionSuccessful = false;
			description.setMessage(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
			e.printStackTrace();
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimExceptionMsg.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			isTransactionSuccessful = false;
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (PacketManagerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					RegistrationStatusCode.FAILED.toString() + e.getMessage() + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(
					trimExceptionMsg.trimExceptionMessage(StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
			isTransactionSuccessful = false;
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
			object.setIsValid(Boolean.FALSE);
			object.setInternalError(Boolean.TRUE);
			object.setRid(registrationStatusDto.getRegistrationId());
		} catch (Exception ex) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMsg
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					RegistrationStatusCode.FAILED.toString() + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			object.setInternalError(Boolean.TRUE);
			isTransactionSuccessful = Boolean.FALSE;
			description.setCode(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getMessage());
		} finally {
			registrationStatusDto.setRegistrationStageName(getStageName());
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.QUALITY_CHECK.toString());
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_QUALITY_CHECK_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.QUALITY_CHECK.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, regId);

		}

		return object;
	}

	/**
	 * Gets the threshold based on type.
	 *
	 * @param singleType the single type
	 * @param subtype    the subtype
	 * @return the threshold based on type
	 */
	private Integer getThresholdBasedOnType(BiometricType biometricType, List<String> subtype) {
		if (biometricType.value().equalsIgnoreCase(FINGER)) {
			if (subtype.contains(THUMB)) {
				return thumbFingerThreshold;
			} else if (subtype.contains(RIGHT)) {
				return rightFingerThreshold;
			} else if (subtype.contains(LEFT)) {
				return leftFingerThreshold;
			}
		} else if (biometricType.value().equalsIgnoreCase(IRIS)) {
			return irisThreshold;
		} else if (biometricType.value().equalsIgnoreCase(FACE)) {
			return faceThreshold;
		}
		return 0;
	}

	private iBioProviderApi getBioSdkInstance(BiometricType biometricType) throws BiometricException {
		iBioProviderApi bioProvider = bioApiFactory.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK);
		return bioProvider;
	}
}
