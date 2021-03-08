package io.mosip.registration.processor.abis.handler.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.abis.handler.constant.AbisHandlerStageConstant;
import io.mosip.registration.processor.abis.handler.dto.DataShareResponseDto;
import io.mosip.registration.processor.abis.handler.dto.Filter;
import io.mosip.registration.processor.abis.handler.dto.ShareableAttributes;
import io.mosip.registration.processor.abis.handler.dto.Source;
import io.mosip.registration.processor.abis.handler.exception.AbisHandlerException;
import io.mosip.registration.processor.abis.handler.exception.DataShareException;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.AbisStatusCode;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyRequestGalleryDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.Flag;
import io.mosip.registration.processor.core.packet.dto.abis.ReferenceIdDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegBioRefDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegDemoDedupeListDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.utils.BIRConverter;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class AbisHandlerStage.
 * 
 * @author M1048358 Alok
 */
@Service
public class AbisHandlerStage extends MosipVerticleAPIManager {

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The max results. */
	@Value("${registration.processor.abis.maxResults}")
	private String maxResults;

	/** The target FPIR. */
	@Value("${registration.processor.abis.targetFPIR}")
	private String targetFPIR;

	/** server port number. */
	@Value("${server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${registration.processor.policy.id}")
	private String policyId;

	@Value("${registration.processor.subscriber.id}")
	private String subscriberId;

	@Autowired
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(AbisHandlerStage.class);

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Autowired
	private Utilities utility;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	@Autowired
	private CbeffUtil cbeffutil;

	@Autowired
	private Environment env;

	@Autowired
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	private static final String DATASHARECREATEURL = "DATASHARECREATEURL";

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.ABIS_HANDLER_BUS_IN,
				MessageBusAddress.ABIS_HANDLER_BUS_OUT);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), MessageBusAddress.ABIS_HANDLER_BUS_IN,
				MessageBusAddress.ABIS_HANDLER_BUS_OUT));
		this.createServer(router.getRouter(), Integer.parseInt(port));
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
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		object.setMessageBusAddress(MessageBusAddress.ABIS_HANDLER_BUS_IN);
		Boolean isTransactionSuccessful = false;
		String regId = object.getRid();
		InternalRegistrationStatusDto registrationStatusDto = null;
		String transactionTypeCode = null;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "AbisHandlerStage::process()::entry");
		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(regId);
			transactionTypeCode = registrationStatusDto.getLatestTransactionTypeCode();
			String transactionId = registrationStatusDto.getLatestRegistrationTransactionId();

			Boolean isIdentifyRequestPresent = packetInfoManager.getIdentifyByTransactionId(transactionId,
					AbisHandlerStageConstant.IDENTIFY);

			if (!isIdentifyRequestPresent) {
				List<AbisQueueDetails> abisQueueDetails = utility.getAbisQueueDetails();
				if (abisQueueDetails.isEmpty()) {
					description.setStatusComment(AbisHandlerStageConstant.DETAILS_NOT_FOUND);
					description.setMessage(PlatformErrorMessages.RPR_DETAILS_NOT_FOUND.getMessage());
					description.setCode(PlatformErrorMessages.RPR_DETAILS_NOT_FOUND.getCode());
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "",
							AbisHandlerStageConstant.DETAILS_NOT_FOUND);
					throw new AbisHandlerException(PlatformErrorMessages.RPR_ABIS_INTERNAL_ERROR.getCode());
				}
				createRequest(regId, abisQueueDetails, transactionId, registrationStatusDto.getRegistrationType(), description, transactionTypeCode);
				object.setMessageBusAddress(MessageBusAddress.ABIS_MIDDLEWARE_BUS_IN);
			} else {
				if (transactionTypeCode.equalsIgnoreCase(AbisHandlerStageConstant.DEMOGRAPHIC_VERIFICATION)) {
					object.setMessageBusAddress(MessageBusAddress.DEMO_DEDUPE_BUS_IN);
				} else if (transactionTypeCode.equalsIgnoreCase(AbisHandlerStageConstant.BIOGRAPHIC_VERIFICATION)) {
					object.setMessageBusAddress(MessageBusAddress.BIO_DEDUPE_BUS_IN);
				}
			}
			description.setStatusComment(AbisHandlerStageConstant.ABIS_HANDLER_SUCCESS);
			description.setMessage(PlatformSuccessMessages.RPR_ABIS_HANDLER_STAGE_SUCCESS.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_ABIS_HANDLER_STAGE_SUCCESS.getCode());
			isTransactionSuccessful = true;
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, description.getMessage());
		} catch (Exception e) {
			description.setStatusComment(AbisHandlerStageConstant.ERROR_IN_ABIS_HANDLER);
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			registrationStatusDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
			if (transactionTypeCode.equalsIgnoreCase(AbisHandlerStageConstant.DEMOGRAPHIC_VERIFICATION)) {
				registrationStatusDto.setRegistrationStageName(AbisHandlerStageConstant.DEMO_DEDUPE_STAGE);
			} else if (transactionTypeCode.equalsIgnoreCase(AbisHandlerStageConstant.BIOGRAPHIC_VERIFICATION)) {
				registrationStatusDto.setRegistrationStageName(AbisHandlerStageConstant.BIO_DEDUPE_STAGE);
			}
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			String moduleId = description.getCode();
			String moduleName = ModuleName.ABIS_HANDLER.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
		} finally {
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_ABIS_HANDLER_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.ABIS_HANDLER.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, regId);

		}

		return object;
	}

	private void createRequest(String regId, List<AbisQueueDetails> abisQueueDetails, String transactionId, String process,
			LogDescription description, String transactionTypeCode) throws Exception {
		List<RegBioRefDto> bioRefDtos = packetInfoManager.getBioRefIdByRegId(regId);
		String bioRefId;
		if (bioRefDtos.isEmpty()) {
			bioRefId = getUUID();
			insertInBioRef(regId, bioRefId);
		} else {
			bioRefId = bioRefDtos.get(0).getBioRefId();
		}
		createInsertRequest(abisQueueDetails, transactionId, bioRefId, regId, process, description);
		createIdentifyRequest(abisQueueDetails, transactionId, bioRefId, transactionTypeCode, description);

	}

	/**
	 * Creates the identify request.
	 *
	 * @param abisQueueDetails
	 *            the abis application dto list
	 * @param transactionId
	 *            the transaction id
	 * @param bioRefId
	 *            the bio ref id
	 * @param transactionTypeCode
	 *            the transaction type code
	 * @param description
	 */
	private void createIdentifyRequest(List<AbisQueueDetails> abisQueueDetails, String transactionId, String bioRefId,
			String transactionTypeCode, LogDescription description) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisHandlerStage::createIdentifyRequest()::entry");
		String batchId = getUUID();
		for (AbisQueueDetails abisQueue : abisQueueDetails) {
			AbisRequestDto abisRequestDto = new AbisRequestDto();
			String id = getUUID();
			abisRequestDto.setId(id);
			abisRequestDto.setAbisAppCode(abisQueue.getName());
			abisRequestDto.setBioRefId(bioRefId);
			abisRequestDto.setRequestType(AbisHandlerStageConstant.IDENTIFY);
			abisRequestDto.setReqBatchId(batchId);
			abisRequestDto.setRefRegtrnId(transactionId);

			byte[] abisIdentifyRequestBytes = getIdentifyRequestBytes(transactionId, bioRefId, transactionTypeCode, id,
					description);
			abisRequestDto.setReqText(abisIdentifyRequestBytes);

			abisRequestDto.setStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
			abisRequestDto.setStatusComment(null);
			abisRequestDto.setLangCode(AbisHandlerStageConstant.ENG);
			abisRequestDto.setCrBy(AbisHandlerStageConstant.USER);
			abisRequestDto.setUpdBy(null);
			abisRequestDto.setIsDeleted(Boolean.FALSE);

			String moduleId = PlatformSuccessMessages.RPR_ABIS_HANDLER_STAGE_SUCCESS.getCode();
			String moduleName = ModuleName.ABIS_HANDLER.toString();
			packetInfoManager.saveAbisRequest(abisRequestDto, moduleId, moduleName);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisHandlerStage::createIdentifyRequest()::exit");
	}

	/**
	 * Gets the identify request bytes.
	 *
	 * @param transactionId
	 *            the transaction id
	 * @param bioRefId
	 *            the bio ref id
	 * @param transactionTypeCode
	 *            the transaction type code
	 * @param id
	 *            the id
	 * @param description
	 * @return the identify request bytes
	 */
	private byte[] getIdentifyRequestBytes(String transactionId, String bioRefId, String transactionTypeCode, String id,
			LogDescription description) {
		AbisIdentifyRequestDto abisIdentifyRequestDto = new AbisIdentifyRequestDto();
		Flag flag = new Flag();
		abisIdentifyRequestDto.setId(AbisHandlerStageConstant.MOSIP_ABIS_IDENTIFY);
		abisIdentifyRequestDto.setVersion(AbisHandlerStageConstant.VERSION);
		abisIdentifyRequestDto.setRequestId(id);
		abisIdentifyRequestDto.setReferenceId(bioRefId);
		abisIdentifyRequestDto.setReferenceUrl("");
		abisIdentifyRequestDto.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		flag.setMaxResults(maxResults);
		flag.setTargetFPIR(targetFPIR);
		abisIdentifyRequestDto.setFlags(flag);

		// Added Gallery data for demo dedupe
		if (transactionTypeCode.equalsIgnoreCase(AbisHandlerStageConstant.DEMOGRAPHIC_VERIFICATION)) {
			List<RegDemoDedupeListDto> regDemoDedupeListDtoList = packetInfoManager
					.getDemoListByTransactionId(transactionId);
			if (regDemoDedupeListDtoList.isEmpty()) {
				description.setStatusComment(AbisHandlerStageConstant.NO_RECORD_FOUND);
				description.setMessage(PlatformErrorMessages.RPR_NO_RECORD_FOUND.getMessage());
				description.setCode(PlatformErrorMessages.RPR_NO_RECORD_FOUND.getCode());
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"Potential Match Records are Not Found for Demo Dedupe Potential Match");
				throw new AbisHandlerException(description.getMessage());
			}
			List<ReferenceIdDto> referenceIdDtos = new ArrayList<>();

			for (RegDemoDedupeListDto dedupeListDto : regDemoDedupeListDtoList) {
				ReferenceIdDto dto = new ReferenceIdDto();
				List<RegBioRefDto> regBioRefDto = packetInfoManager.getBioRefIdByRegId(dedupeListDto.getMatchedRegId());
				if (!CollectionUtils.isEmpty(regBioRefDto)) {
					dto.setReferenceId(regBioRefDto.get(0).getBioRefId());
				}

				referenceIdDtos.add(dto);
			}
			AbisIdentifyRequestGalleryDto galleryDto = new AbisIdentifyRequestGalleryDto();
			galleryDto.setReferenceIds(referenceIdDtos);
			abisIdentifyRequestDto.setGallery(galleryDto);
		}

		try {
			String jsonString = JsonUtils.javaObjectToJsonString(abisIdentifyRequestDto);
			return jsonString.getBytes();
		} catch (JsonProcessingException e) {
			description.setStatusComment(AbisHandlerStageConstant.ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST);
			description.setMessage(PlatformErrorMessages.RPR_ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST.getMessage());
			description.setCode(PlatformErrorMessages.RPR_ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", AbisHandlerStageConstant.ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST);
			throw new AbisHandlerException(PlatformErrorMessages.RPR_ABIS_INTERNAL_ERROR.getCode(), e);
		}
	}

	/**
	 * Insert in bio ref.
	 *
	 * @param regId
	 *            the reg id
	 * @param bioRefId
	 *            the bio ref id
	 */
	private void insertInBioRef(String regId, String bioRefId) {
		RegBioRefDto regBioRefDto = new RegBioRefDto();
		regBioRefDto.setBioRefId(bioRefId);
		regBioRefDto.setCrBy(AbisHandlerStageConstant.USER);
		regBioRefDto.setIsActive(Boolean.TRUE);
		regBioRefDto.setIsDeleted(Boolean.FALSE);
		regBioRefDto.setRegId(regId);
		regBioRefDto.setUpdBy(null);
		String moduleId = PlatformSuccessMessages.RPR_ABIS_HANDLER_STAGE_SUCCESS.getCode();
		String moduleName = ModuleName.ABIS_HANDLER.toString();
		packetInfoManager.saveBioRef(regBioRefDto, moduleId, moduleName);
	}

	/**
	 * Creates the insert request.
	 *
	 * @param abisQueueDetails
	 *            the abis application dto list
	 * @param transactionId
	 *            the transaction id
	 * @param bioRefId
	 *            the bio ref id
	 * @param regId
	 *            the reg id
	 * @param description
	 */
	private void createInsertRequest(List<AbisQueueDetails> abisQueueDetails, String transactionId, String bioRefId,
			String regId, String process, LogDescription description) throws Exception {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "AbisHandlerStage::createInsertRequest()::entry");
		String batchId = getUUID();
		List<String> abisProcessedInsertAppCodeList = packetInfoManager.getAbisProcessedRequestsAppCodeByBioRefId(
				bioRefId, AbisStatusCode.INSERT.toString(), AbisStatusCode.PROCESSED.toString());
		List<String> abisAppCodeList = new ArrayList<>();
		for (AbisQueueDetails abisQueue : abisQueueDetails) {
			abisAppCodeList.add(abisQueue.getName());
		}

		for (String appCode : abisAppCodeList) {

			AbisRequestDto abisRequestDto = new AbisRequestDto();
			String id = getUUID();
			abisRequestDto.setId(id);
			abisRequestDto.setAbisAppCode(appCode);
			abisRequestDto.setBioRefId(bioRefId);
			abisRequestDto.setRequestType(AbisHandlerStageConstant.INSERT);
			abisRequestDto.setReqBatchId(batchId);
			abisRequestDto.setRefRegtrnId(transactionId);

			byte[] abisInsertRequestBytes = getInsertRequestBytes(regId, id, process, bioRefId, description);
			abisRequestDto.setReqText(abisInsertRequestBytes);

			abisRequestDto.setStatusCode(AbisStatusCode.IN_PROGRESS.toString());
			abisRequestDto.setStatusComment(null);
			abisRequestDto.setLangCode(AbisHandlerStageConstant.ENG);
			abisRequestDto.setCrBy(AbisHandlerStageConstant.USER);
			abisRequestDto.setUpdBy(null);
			abisRequestDto.setIsDeleted(Boolean.FALSE);
			String moduleId = PlatformSuccessMessages.RPR_ABIS_HANDLER_STAGE_SUCCESS.getCode();
			String moduleName = ModuleName.ABIS_HANDLER.toString();
			if (abisProcessedInsertAppCodeList != null && abisProcessedInsertAppCodeList.contains(appCode)) {
				abisRequestDto.setStatusCode(AbisStatusCode.ALREADY_PROCESSED.toString());
				packetInfoManager.saveAbisRequest(abisRequestDto, moduleId, moduleName);
			} else {
				abisRequestDto.setStatusCode(AbisStatusCode.IN_PROGRESS.toString());
				packetInfoManager.saveAbisRequest(abisRequestDto, moduleId, moduleName);
			}

		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisHandlerStage::createInsertRequest()::exit");
	}

	/**
	 * Gets the insert request bytes.
	 *
	 * @param regId
	 *            the reg id
	 * @param id
	 *            the id
	 * @param bioRefId
	 *            the bio ref id
	 * @param description
	 * @return the insert request bytes
	 */
	private byte[] getInsertRequestBytes(String regId, String id, String process, String bioRefId, LogDescription description) throws Exception {
		AbisInsertRequestDto abisInsertRequestDto = new AbisInsertRequestDto();
		abisInsertRequestDto.setId(AbisHandlerStageConstant.MOSIP_ABIS_INSERT);
		abisInsertRequestDto.setReferenceId(bioRefId);
		abisInsertRequestDto.setReferenceURL(getDataShareUrl(regId, process));
		abisInsertRequestDto.setRequestId(id);
		abisInsertRequestDto.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		abisInsertRequestDto.setVersion(AbisHandlerStageConstant.VERSION);
		try {
			String jsonString = JsonUtils.javaObjectToJsonString(abisInsertRequestDto);
			return jsonString.getBytes();
		} catch (JsonProcessingException e) {
			description.setStatusComment(AbisHandlerStageConstant.ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST);
			description.setMessage(PlatformErrorMessages.RPR_ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST.getMessage());
			description.setCode(PlatformErrorMessages.RPR_ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", AbisHandlerStageConstant.ERROR_IN_ABIS_HANDLER_IDENTIFY_REQUEST);
			throw new AbisHandlerException(PlatformErrorMessages.RPR_ABIS_INTERNAL_ERROR.getCode(), e);
		}
	}

	/**
	 * Gets the uuid.
	 *
	 * @return the uuid
	 */
	private String getUUID() {
		return UUID.randomUUID().toString();
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
		BiometricRecord biometricRecord = priorityBasedPacketManagerService.getBiometrics(
				id, individualBiometricsLabel, modalities, process, ProviderStageName.BIO_DEDUPE);
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

		return response.getDataShare().getUrl();
	}
	public Map<String, List<String>> createTypeSubtypeMapping() throws ApisResourceAccessException, DataShareException, JsonParseException, JsonMappingException, com.fasterxml.jackson.core.JsonProcessingException, IOException{
		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
				ApiName.PMS, Lists.newArrayList(policyId, PolicyConstant.PARTNER_ID, subscriberId), "", "", ResponseWrapper.class);
		if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() >0)) {
			throw new DataShareException(policyResponse == null ? "Policy Response response is null" : policyResponse.getErrors().get(0).getMessage());
			
		} else {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
			LinkedHashMap<String, Object> policies = (LinkedHashMap<String, Object>) responseMap.get(PolicyConstant.POLICIES);
			List<?> attributes = (List<?>) policies.get(PolicyConstant.SHAREABLE_ATTRIBUTES);
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
}
