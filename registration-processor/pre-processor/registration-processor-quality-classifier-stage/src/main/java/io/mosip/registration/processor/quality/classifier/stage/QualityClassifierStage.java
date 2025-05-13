package io.mosip.registration.processor.quality.classifier.stage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
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
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.quality.classifier.exception.FileMissingException;
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
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.quality.classifier.config", "io.mosip.registration.processor.stages.config",
		"io.mosip.registrationprocessor.stages.config", "io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", "io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", "io.mosip.kernel.idobjectvalidator.config",
		"io.mosip.registration.processor.core.kernel.beans", "io.mosip.kernel.biosdk.provider.impl" })
public class QualityClassifierStage extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.quality.classifier.";


	/** The Constant UTF_8. */
	public static final String UTF_8 = "UTF-8";

	/** The Constant VALUE. */
	public static final String VALUE = "value";

	/** The Constant TRUE. */
	public static final String TRUE = "true";

	/** The Constant EXCEPTION. */
	public static final String EXCEPTION = "EXCEPTION";

	private TrimExceptionMessage trimExceptionMsg = new TrimExceptionMessage();

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/**
	 * After this time intervel, message should be considered as expired (In
	 * seconds).
	 */
	@Value("${mosip.regproc.quality.classifier.message.expiry-time-limit}")
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
	private PriorityBasedPacketManagerService basedPacketManagerService;

	@Autowired
	private PacketManagerService packetManagerService;

	/** The registration status mapper util. */
	@Autowired
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	/**
	 * Below quality classifications map should contain proper quality
	 * classification name and quality range, any overlap of the quality range will
	 * result in a random behaviour of tagging. In range, upper and lower values are
	 * inclusive.
	 */
	
	@Value("#{${mosip.regproc.quality.classifier.tagging.quality.ranges:{'level-1':'0-10','level-2':'10-20','level-3':'20-30','level-4':'30-40','level-5':'40-50','level-6':'50-60','level-7':'60-70','level-8':'70-80','level-9':'80-90','level-10':'90-101',}}}")
	private Map<String, String> qualityClassificationRangeMap;

	/** Quality Tag Prefix */
	@Value("${mosip.regproc.quality.classifier.tagging.quality.prefix:Biometric_Quality-}")
	private String qualityTagPrefix;

    /** The tag value that will be used by default when the packet does not have value for the biometric tag field */
    @Value("${mosip.regproc.quality.classifier.tagging.quality.biometric-not-available-tag-value}")
    private String biometricNotAvailableTagValue;

    /** modality arrays that needs to be tagged */
    @Value("#{'${mosip.regproc.quality.classifier.tagging.quality.modalities}'.split(',')}")
    private List<String> modalities;

	private static String RANGE_DELIMITER = "-";

	/**
	 * Filter qualityClassficaticationsRangeMap using delimiter and store into
	 * parsedQualityRangemap using @PostConstruct
	 */
	private Map<String, int[]> parsedQualityRangeMap;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(QualityClassifierStage.class);

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	private MosipEventBus mosipEventBus = null;

	private TrimExceptionMessage trimExpMessage = new TrimExceptionMessage();

	@Autowired
	private BioAPIFactory bioApiFactory;

	@PostConstruct
	private void generateParsedQualityRangeMap() {
		parsedQualityRangeMap = new HashMap<>();
		for (Map.Entry<String, String> entry : qualityClassificationRangeMap.entrySet()) {
			String[] range = entry.getValue().split(RANGE_DELIMITER);
			int[] rangeArray = new int[2];
			rangeArray[0] = Integer.parseInt(range[0]);
			rangeArray[1] = Integer.parseInt(range[1]);
			parsedQualityRangeMap.put(entry.getKey(), rangeArray);
		}
	}

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.QUALITY_CLASSIFIER_BUS_IN,
				MessageBusAddress.QUALITY_CLASSIFIER_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.QUALITY_CLASSIFIER_BUS_IN,
				MessageBusAddress.QUALITY_CLASSIFIER_BUS_OUT));
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
		object.setMessageBusAddress(MessageBusAddress.QUALITY_CLASSIFIER_BUS_IN);
		String regId = object.getRid();
		LogDescription description = new LogDescription();
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.FALSE);
		Boolean isTransactionSuccessful = Boolean.FALSE;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
				"QualityCheckerStage::process()::entry");

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(regId,
				object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());

		try {
			String individualBiometricsObject = basedPacketManagerService.getFieldByMappingJsonKey(regId,
					MappingJsonConstants.INDIVIDUAL_BIOMETRICS, registrationStatusDto.getRegistrationType(),
					ProviderStageName.QUALITY_CHECKER);
			if (StringUtils.isEmpty(individualBiometricsObject)) {
				packetManagerService.addOrUpdateTags(regId, getQualityTags(null));
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
				BiometricRecord biometricRecord = basedPacketManagerService.getBiometricsByMappingJsonKey(regId,
						MappingJsonConstants.INDIVIDUAL_BIOMETRICS, registrationStatusDto.getRegistrationType(),
						ProviderStageName.QUALITY_CHECKER);

				if (biometricRecord == null || CollectionUtils.isEmpty(biometricRecord.getSegments())) {
					biometricRecord = basedPacketManagerService.getBiometricsByMappingJsonKey(regId,
							MappingJsonConstants.AUTHENTICATION_BIOMETRICS, registrationStatusDto.getRegistrationType(),
							ProviderStageName.QUALITY_CHECKER);
				}

				if (biometricRecord == null || biometricRecord.getSegments() == null
						|| biometricRecord.getSegments().size() == 0) {
					description.setCode(PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getCode());
					description.setMessage(PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getMessage());
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), regId,
							PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getMessage());
					throw new FileMissingException(PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getCode(),
							PlatformErrorMessages.RPR_QCR_BIO_FILE_MISSING.getMessage());
				}
				

				packetManagerService.addOrUpdateTags(regId, getQualityTags(biometricRecord.getSegments()));

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
						"UpdatingTags::success ");

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

		} catch (ApisResourceAccessException e) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.APIS_RESOURCE_ACCESS_EXCEPTION));
			registrationStatusDto.setStatusComment(trimExpMessage
					.trimExceptionMessage(StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			object.setInternalError(Boolean.TRUE);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PUM_NGINX_ACCESS_FAILED.name() + ExceptionUtils.getStackTrace(e));

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
			description.setCode(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getMessage());

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
			description.setCode(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_QCR_BIOMETRIC_EXCEPTION.getMessage());
		} catch (JsonProcessingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
							+ org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(trimExceptionMsg
					.trimExceptionMessage(StatusUtil.JSON_PARSING_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(registrationStatusMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.JSON_PROCESSING_EXCEPTION));
			description.setMessage(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
							+ org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto.setStatusComment(
					trimExceptionMsg.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
		}catch (PacketManagerNonRecoverableException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
							+ org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMsg
					.trimExceptionMessage(StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION));
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (PacketManagerException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, RegistrationStatusCode.FAILED.toString() + e.getMessage()
							+ org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.name());
			registrationStatusDto.setStatusComment(trimExceptionMsg
					.trimExceptionMessage(StatusUtil.PACKET_MANAGER_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.PACKET_MANAGER_EXCEPTION));
			description.setMessage(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.PACKET_MANAGER_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
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
			description.setCode(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.getMessage());
		} finally {
			if(object.getInternalError()) {
				updateErrorFlags(registrationStatusDto, object);
			}
			object.setRid(registrationStatusDto.getRegistrationId());
			registrationStatusDto.setRegistrationStageName(getStageName());
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.QUALITY_CLASSIFIER.toString());
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_QUALITY_CHECK_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.QUALITY_CLASSIFIER.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, regId);

		}

		return object;
	}


	private iBioProviderApi getBioSdkInstance(BiometricType biometricType) throws BiometricException {
		iBioProviderApi bioProvider = bioApiFactory.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK);
		return bioProvider;
	}
	
	private Map<String, String> getQualityTags(List<BIR> birs) throws BiometricException{
		
		Map<String, String> tags = new HashMap<String, String>();

		// setting biometricNotAvailableTagValue for each modality in case biometrics are not available
		if (birs == null) {
			modalities.forEach(modality -> {
				tags.put(qualityTagPrefix.concat(modality), biometricNotAvailableTagValue);
			});
			return tags;
		}

		HashMap<String, Float> bioTypeMinScoreMap = new HashMap<String, Float>();

		// get individual biometrics file name from id.json
		for (BIR bir : birs) {

			if (bir.getOthers() != null) {
				HashMap<String, String> othersInfo = bir.getOthers();
				boolean exceptionValue = false;
				for (Map.Entry<String, String> other : othersInfo.entrySet()) {
					if (other.getKey().equals(EXCEPTION)) {
						if (other.getValue().equals(TRUE)) {
							exceptionValue = true;
						}
						break;
					}
				}

				if (exceptionValue) {
					continue;
				}
			}

			BiometricType biometricType = bir.getBdbInfo().getType().get(0);
			BIR[] birArray = new BIR[1];
			birArray[0] = bir;
			if(!biometricType.name().equalsIgnoreCase(BiometricType.EXCEPTION_PHOTO.name())) {
			float[] qualityScoreresponse = getBioSdkInstance(biometricType).getSegmentQuality(birArray, null);

			float score = qualityScoreresponse[0];
			String bioType = bir.getBdbInfo().getType().get(0).value();

			// Check for entry
			Float storedMinScore = bioTypeMinScoreMap.get(bioType);

			bioTypeMinScoreMap.put(bioType,
					storedMinScore == null ? score : storedMinScore > score ? score : storedMinScore);
			}
		}

		for (Entry<String, Float> bioTypeMinEntry : bioTypeMinScoreMap.entrySet()) {

			for (Entry<String, int[]> qualityRangeEntry : parsedQualityRangeMap.entrySet()) {

				if (bioTypeMinEntry.getValue() >= qualityRangeEntry.getValue()[0]
						&& bioTypeMinEntry.getValue() < qualityRangeEntry.getValue()[1]) {

					tags.put( qualityTagPrefix.concat(bioTypeMinEntry.getKey()), qualityRangeEntry.getKey());
					break;
				}

			}
		}
		
		// setting biometricNotAvailableTagValue for modalities those are not available in BIRs
		modalities.forEach(modality -> {
			if (!tags.containsKey(qualityTagPrefix.concat(modality))) {
				tags.put(qualityTagPrefix.concat(modality), biometricNotAvailableTagValue);
			}
		});

		return tags;
	}

	private void updateErrorFlags(InternalRegistrationStatusDto registrationStatusDto, MessageDTO object) {
		object.setInternalError(true);
		if (registrationStatusDto.getLatestTransactionStatusCode()
				.equalsIgnoreCase(RegistrationTransactionStatusCode.REPROCESS.toString())) {
			object.setIsValid(true);
		} else {
			object.setIsValid(false);
		}
	}
}
