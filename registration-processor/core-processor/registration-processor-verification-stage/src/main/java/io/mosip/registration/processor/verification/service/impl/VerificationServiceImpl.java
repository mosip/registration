package io.mosip.registration.processor.verification.service.impl;

import static io.mosip.registration.processor.verification.constants.VerificationConstants.DATETIME_PATTERN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.VerificationEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.verification.constants.VerificationConstants;
import io.mosip.registration.processor.verification.dto.DataShareRequestDto;
import io.mosip.registration.processor.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.verification.exception.DataShareException;
import io.mosip.registration.processor.verification.exception.InvalidFileNameException;
import io.mosip.registration.processor.verification.exception.InvalidRidException;
import io.mosip.registration.processor.verification.exception.NoRecordAssignedException;
import io.mosip.registration.processor.verification.request.dto.Filter;
import io.mosip.registration.processor.verification.request.dto.ShareableAttributes;
import io.mosip.registration.processor.verification.request.dto.Source;
import io.mosip.registration.processor.verification.request.dto.VerificationRequestDTO;
import io.mosip.registration.processor.verification.response.dto.VerificationResponseDTO;
import io.mosip.registration.processor.verification.service.VerificationService;
import io.mosip.registration.processor.verification.stage.VerificationStage;
import io.mosip.registration.processor.verification.util.SaveVerificationRecordUtility;

/**
 * The Class ManualAdjudicationServiceImpl.
 */
@Component
@Transactional
public class VerificationServiceImpl implements VerificationService {

	/** The logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(VerificationServiceImpl.class);
	private LinkedHashMap<String, Object> policies = null;
	private static final String VERIFICATION = "verification";
	private static final String VERIFICATION_COMMENT = "Packet marked for verification";

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	private static final String TEXT_MESSAGE = "text";
	private static final String DATASHARE = "dataShare";
	private static final String ERRORS = "errors";
	private static final String URL = "url";
	private static final String META_INFO = "meta_info";
	private static final String AUDITS = "audits";

	@Autowired
	private Environment env;

	/** The address. */
	@Value("${registration.processor.queue.verification.request:mosip-to-verification}")
	private String mvRequestAddress;

	/**
	 * Manual adjudication queue message expiry in seconds, if given 0 then message
	 * will never expire
	 */
	@Value("${registration.processor.queue.verification.request.messageTTL}")
	private int mvRequestMessageTTL;

	@Value("${registration.processor.verification.policy.id:mpolicy-default-adjudication}")
	private String policyId;

	@Value("${registration.processor.verification.subscriber.id:mpartner-default-adjudication}")
	private String subscriberId;

	@Value("${activemq.message.format}")
	private String messageFormat;

	@Value("${mosip.regproc.data.share.protocol}")
	private String httpProtocol;

	@Value("${mosip.regproc.data.share.internal.domain.name}")
	private String internalDomainName;

	@Autowired
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	@Autowired
	private CbeffUtil cbeffutil;

	@Autowired
	private Utilities utility;

	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/** The audit log request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;
	/** The base packet repository. */
	@Autowired
	private BasePacketRepository<VerificationEntity, String> basePacketRepository;

	/** The manual verification stage. */
	@Autowired
	private VerificationStage verificationStage;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Autowired
	SaveVerificationRecordUtility saveVerificationRecordUtility;

	/** The Constant PROTOCOL. */
	public static final String PROTOCOL = "https";

	/*
	 * This method will be called from the event bus passing messageDTO object
	 * containing rid Based o Rid fetch match reference Id and form request which is
	 * pushed to queue and update Manual verification entity
	 */
	@Override
	public MessageDTO process(MessageDTO messageDTO, MosipQueue queue, String stageName) {
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		messageDTO.setMessageBusAddress(MessageBusAddress.VERIFICATION_BUS_IN);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				messageDTO.getRid(), "VerificationServiceImpl::process()::entry");

		boolean isTransactionSuccessful = true;
		LogDescription description = new LogDescription();

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				messageDTO.getRid(), messageDTO.getReg_type(), messageDTO.getIteration(),
				messageDTO.getWorkflowInstanceId());
		try {
			if (null == messageDTO.getRid() || messageDTO.getRid().isEmpty())
				throw new InvalidRidException(PlatformErrorMessages.RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
						PlatformErrorMessages.RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			VerificationRequestDTO mar = prepareVerificationRequest(messageDTO, registrationStatusDto);
			saveVerificationRecordUtility.saveVerificationRecord(messageDTO, mar.getRequestId(), description);
			regProcLogger.debug("Request : " + JsonUtils.javaObjectToJsonString(mar));

			if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
				mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar), mvRequestAddress,
						mvRequestMessageTTL);
			else
				mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar).getBytes(), mvRequestAddress,
						mvRequestMessageTTL);

			regProcLogger.info("ID : " + messageDTO.getRid() + " has been successfully sent for verification.");

			if (isTransactionSuccessful) {
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_SENT.getCode());
				registrationStatusDto.setStatusComment(StatusUtil.VERIFICATION_SENT.getMessage());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
			} else {
				registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
				registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_FAILED.getCode());
				registrationStatusDto.setStatusComment(StatusUtil.VERIFICATION_FAILED.getMessage());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.ERROR.toString());
			}
			registrationStatusDto.setRegistrationStageName(stageName);

		} catch (DataShareException de) {
			messageDTO.setInternalError(true);
			isTransactionSuccessful = false;
			description.setCode(de.getErrorCode());
			description.setMessage(de.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					de.getErrorCode(), de.getErrorText());

		} catch (InvalidRidException exp) {
			isTransactionSuccessful = false;
			description.setCode(exp.getErrorCode());
			description.setMessage(exp.getMessage());
			messageDTO.setInternalError(true);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), null, exp.getErrorCode(), exp.getErrorText());

		} catch (Exception e) {
			isTransactionSuccessful = false;
			description.setCode(PlatformSuccessMessages.RPR_VERIFICATION_SENT.getCode());
			description.setMessage(e.getMessage());
			messageDTO.setInternalError(true);
			regProcLogger.error(ExceptionUtils.getStackTrace(e));
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					e.getMessage(), e.getMessage());
		} finally {
			if (isTransactionSuccessful) {
				messageDTO.setIsValid(true);
				description.setCode(PlatformSuccessMessages.RPR_VERIFICATION_SUCCESS.getCode());
				description.setMessage(PlatformSuccessMessages.RPR_VERIFICATION_SUCCESS.getMessage());
			} else
				registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_FAILED.getCode());
			updateStatus(messageDTO, registrationStatusDto, isTransactionSuccessful, description,
					PlatformSuccessMessages.RPR_VERIFICATION_SENT);
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				messageDTO.getRid(), "VerificationServiceImpl::process()::entry");

		return messageDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.verification.service.
	 * ManualAdjudicationService#updatePacketStatus(io.mosip.registration.processor.
	 * verification.dto.ManualVerificationDTO)
	 */
	@Override
	public boolean updatePacketStatus(VerificationResponseDTO manualVerificationDTO, String stageName,
			MosipQueue queue) {

		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(),
				manualVerificationDTO.getRequestId(), "VerificationServiceImpl::updatePacketStatus()::entry");

		VerificationEntity entity = validateRequestIdAndReturnRid(manualVerificationDTO.getRequestId());
		String regId = entity != null ? entity.getRegId() : null;

		MessageDTO messageDTO = new MessageDTO();
		InternalRegistrationStatusDto registrationStatusDto = null;
		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(
					(entity != null ? entity.getRegId() : null), null, null,
					(entity != null ? (entity.getId() != null ? entity.getId().getWorkflowInstanceId() : null) : null));
			registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.VERIFICATION.name());
			registrationStatusDto.setRegistrationStageName(stageName);
			messageDTO.setInternalError(false);
			messageDTO.setIsValid(false);
			messageDTO.setRid(regId);
			messageDTO.setReg_type(registrationStatusDto.getRegistrationType());

			List<VerificationEntity> entities = retrieveInqueuedRecordsByRid(regId);

			// check if response is marked for resend
			if (isResendFlow(registrationStatusDto, manualVerificationDTO, entity)) {
				registrationStatusDto.setStatusComment(StatusUtil.VERIFICATION_RESEND.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_RESEND.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				description.setMessage(StatusUtil.VERIFICATION_RESEND.getMessage());
				description.setCode(StatusUtil.VERIFICATION_RESEND.getCode());
				messageDTO.setInternalError(true);
				messageDTO.setIsValid(isTransactionSuccessful);

			} else {
				// call success flow and process the response received from manual verification
				// system
				isTransactionSuccessful = successFlow(manualVerificationDTO, entity, registrationStatusDto, messageDTO,
						description);

				registrationStatusDto.setUpdatedBy(USER);
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), regId, description.getMessage());
			}

		} catch (TablenotAccessibleException e) {
			messageDTO.setInternalError(true);
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());

			description.setMessage(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (NoRecordAssignedException e) {
			messageDTO.setIsValid(false);
			messageDTO.setInternalError(false);
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.NO_RECORDS_ASSIGNED));
			registrationStatusDto.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode());

			description.setMessage(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			messageDTO.setInternalError(true);
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationExceptionMapperUtil.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());

			description.setMessage(PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.UNKNOWN_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} finally {
			// TODO structure to not return in finally,ignore for sonar
			updateStatus(messageDTO, registrationStatusDto, isTransactionSuccessful, description,
					PlatformSuccessMessages.RPR_VERIFICATION_SUCCESS);
			verificationStage.sendMessage(messageDTO);
		}
		return isTransactionSuccessful;
	}

	private void updateStatus(MessageDTO messageDTO, InternalRegistrationStatusDto registrationStatusDto,
			boolean isTransactionSuccessful, LogDescription description,
			PlatformSuccessMessages platformSuccessMessages) {
		if (messageDTO.getInternalError()) {
			updateErrorFlags(registrationStatusDto, messageDTO);
		}
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.VERIFICATION.toString());
		String regId = messageDTO.getRid();
		/** Module-Id can be Both Success/Error code */
		String moduleId = isTransactionSuccessful ? platformSuccessMessages.getCode() : description.getCode();
		String moduleName = ModuleName.VERIFICATION.toString();
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
		String eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
				: EventName.EXCEPTION.toString();
		String eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
				: EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
				moduleId, moduleName, regId);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "VerificationServiceImpl::updatePacketStatus()::exit");

	}

	/**
	 * Basic validation of requestId received against the rid present in
	 * manual-adjudication table Returns the correct rid after successful validation
	 * 
	 * @param reqId : the request id
	 * @return rid : the registration id
	 */
	private VerificationEntity validateRequestIdAndReturnRid(String reqId) {
		List<VerificationEntity> entities = basePacketRepository.getVerificationRecordByRequestId(reqId);

		if (CollectionUtils.isEmpty(entities)
				|| new HashSet<>(entities.stream().map(e -> e.getRegId()).collect(Collectors.toList())).size() != 1) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					entities != null ? entities.stream().map(e -> e.getRegId()).collect(Collectors.toList()) : null,
					"Multiple rids found against request id : " + reqId);
			throw new InvalidRidException(PlatformErrorMessages.RPR_INVALID_RID_FOUND.getCode(),
					PlatformErrorMessages.RPR_INVALID_RID_FOUND.getCode());
		}

		VerificationEntity entity = entities.iterator().next();

		if (entity != null && StringUtils.isEmpty(entity.getRegId())) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					entity.getRegId(), "VerificationServiceImpl::updatePacketStatus()::InvalidFileNameException"
							+ PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			throw new InvalidFileNameException(PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
					PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
		}
		return entity;
	}

	private List<VerificationEntity> retrieveInqueuedRecordsByRid(String regId) {

		List<VerificationEntity> entities = basePacketRepository.getAssignedVerificationRecord(regId,
				ManualVerificationStatus.INQUEUE.name());

		if (CollectionUtils.isEmpty(entities)) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "VerificationServiceImpl::updatePacketStatus()"
							+ PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
			throw new NoRecordAssignedException(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
		}

		return entities;
	}

	private String getDataShareUrl(String id, String process) throws Exception {
		DataShareRequestDto requestDto = new DataShareRequestDto();

		LinkedHashMap<String, Object> policy = getPolicy();

		Map<String, String> policyMap = getPolicyMap(policy);

		// set demographic
		Map<String, String> demographicMap = policyMap.entrySet().stream()
				.filter(e -> e.getValue() != null
						&& (!META_INFO.equalsIgnoreCase(e.getValue()) && !AUDITS.equalsIgnoreCase(e.getValue())))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		requestDto.setIdentity(
				packetManagerService.getFields(id, demographicMap.values().stream().collect(Collectors.toList()),
						process, ProviderStageName.VERIFICATION));

		// set documents
		JSONObject docJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
		for (Object doc : docJson.keySet()) {
			if (doc != null) {
				HashMap docmap = (HashMap) docJson.get(doc.toString());
				String docName = docmap != null && docmap.get(MappingJsonConstants.VALUE) != null
						? docmap.get(MappingJsonConstants.VALUE).toString()
						: null;
				if (policyMap.containsValue(docName)) {
					Document document = packetManagerService.getDocument(id, docName, process,
							ProviderStageName.VERIFICATION);
					if (document != null) {
						if (requestDto.getDocuments() != null)
							requestDto.getDocuments().put(docmap.get(MappingJsonConstants.VALUE).toString(),
									CryptoUtil.encodeToURLSafeBase64(document.getDocument()));
						else {
							Map<String, String> docMap = new HashMap<>();
							docMap.put(docmap.get(MappingJsonConstants.VALUE).toString(),
									CryptoUtil.encodeToURLSafeBase64(document.getDocument()));
							requestDto.setDocuments(docMap);
						}
					}
				}
			}
		}

		// set audits
		if (policyMap.containsValue(AUDITS))
			requestDto.setAudits(JsonUtils.javaObjectToJsonString(
					packetManagerService.getAudits(id, process, ProviderStageName.VERIFICATION)));

		// set metainfo
		if (policyMap.containsValue(META_INFO))
			requestDto.setMetaInfo(JsonUtils.javaObjectToJsonString(
					packetManagerService.getMetaInfo(id, process, ProviderStageName.VERIFICATION)));

		// set biometrics
		JSONObject regProcessorIdentityJson = utility
				.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);

		if (policyMap.containsValue(individualBiometricsLabel)) {
			List<String> modalities = getModalities(policy);
			BiometricRecord biometricRecord = packetManagerService.getBiometrics(id, individualBiometricsLabel,
					modalities, process, ProviderStageName.VERIFICATION);
			byte[] content = cbeffutil.createXML(biometricRecord.getSegments());
			requestDto.setBiometrics(content != null ? CryptoUtil.encodeToURLSafeBase64(content) : null);
		}

		String req = JsonUtils.javaObjectToJsonString(requestDto);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("name", VERIFICATION);
		map.add("filename", VERIFICATION);

		ByteArrayResource contentsAsResource = new ByteArrayResource(req.getBytes()) {
			@Override
			public String getFilename() {
				return VERIFICATION;
			}
		};
		map.add("file", contentsAsResource);

		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(policyId);
		pathSegments.add(subscriberId);
		String protocol = StringUtils.isNotEmpty(httpProtocol) ? PolicyConstant.HTTP_PROTOCOL
				: PolicyConstant.HTTPS_PROTOCOL;
		String url = null;

		if (policy.get(PolicyConstant.DATASHARE_POLICIES) != null) {
			LinkedHashMap<String, String> datasharePolicies = (LinkedHashMap<String, String>) policies
					.get(PolicyConstant.DATASHARE_POLICIES);
			if (!CollectionUtils.isEmpty(datasharePolicies)
					&& datasharePolicies.get(PolicyConstant.SHAREDOMAIN_WRITE) != null)
				url = datasharePolicies.get(PolicyConstant.SHAREDOMAIN_WRITE)
						+ env.getProperty(ApiName.DATASHARECREATEURL.name());
		}
		if (StringUtils.isEmpty(url))
			url = protocol + internalDomainName + env.getProperty(ApiName.DATASHARECREATEURL.name());
		url = url.replaceAll("[\\[\\]]", "");

		LinkedHashMap response = (LinkedHashMap) registrationProcessorRestClientService.postApi(url,
				MediaType.MULTIPART_FORM_DATA, pathSegments, null, null, map, LinkedHashMap.class);
		if (response == null || (response.get(ERRORS) != null))
			throw new DataShareException(
					response == null ? "Datashare response is null" : response.get(ERRORS).toString());

		LinkedHashMap datashare = (LinkedHashMap) response.get(DATASHARE);
		return datashare.get(URL) != null ? datashare.get(URL).toString() : null;
	}

	private Map<String, String> getPolicyMap(LinkedHashMap<String, Object> policies) throws IOException {
		Map<String, String> policyMap = new HashMap<>();
		List<LinkedHashMap> attributes = (List<LinkedHashMap>) policies.get(VerificationConstants.SHAREABLE_ATTRIBUTES);
		for (LinkedHashMap map : attributes) {
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(map),
					ShareableAttributes.class);
			policyMap.put(shareableAttributes.getAttributeName(),
					shareableAttributes.getSource().iterator().next().getAttribute());
		}
		return policyMap;

	}

	private LinkedHashMap<String, Object> getPolicy() throws DataShareException, ApisResourceAccessException {
		if (policies != null && policies.size() > 0)
			return policies;

		ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
				ApiName.PMS, Lists.newArrayList(policyId, PolicyConstant.PARTNER_ID, subscriberId), "", "",
				ResponseWrapper.class);
		if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() > 0)) {
			throw new DataShareException(policyResponse == null ? "Policy Response response is null"
					: policyResponse.getErrors().get(0).getMessage());

		} else {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
			policies = (LinkedHashMap<String, Object>) responseMap.get(VerificationConstants.POLICIES);
		}
		return policies;

	}

	public List<String> getModalities(LinkedHashMap<String, Object> policy) throws IOException {
		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		List<LinkedHashMap> attributes = (List<LinkedHashMap>) policy.get(VerificationConstants.SHAREABLE_ATTRIBUTES);
		for (LinkedHashMap map : attributes) {
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(map),
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
		List<String> modalities = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : typeAndSubTypeMap.entrySet()) {
			if (entry.getValue() == null) {
				modalities.add(entry.getKey());
			} else {
				modalities.addAll(entry.getValue());
			}
		}

		return modalities;

	}

	/*
	 * Form manual adjudication request
	 */
	private VerificationRequestDTO prepareVerificationRequest(MessageDTO messageDTO,
			InternalRegistrationStatusDto registrationStatusDto) throws Exception {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"VerificationServiceImpl::formAdjudicationRequest()::entry");

		VerificationRequestDTO req = new VerificationRequestDTO();
		List<VerificationEntity> entities = basePacketRepository
				.getVerificationRecordByWorkflowInstanceId(messageDTO.getWorkflowInstanceId());
		if (!CollectionUtils.isEmpty(entities))
			req.setRequestId(entities.get(0).getRequestId());
		else
			req.setRequestId(UUID.randomUUID().toString());

		req.setId(VerificationConstants.MANUAL_ADJUDICATION_ID);
		req.setVersion(VerificationConstants.VERSION);
		req.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		req.setReferenceId(messageDTO.getRid());
		try {
			req.setReferenceURL(getDataShareUrl(messageDTO.getRid(), registrationStatusDto.getRegistrationType()));

		} catch(PacketManagerNonRecoverableException ex){
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					ex.getErrorCode(), ex.getErrorText());
			throw ex;
		}catch (PacketManagerException | ApisResourceAccessException ex) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					ex.getErrorCode(), ex.getErrorText());
			throw ex;
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"VerificationServiceImpl::formAdjudicationRequest()::entry");

		return req;
	}

	/**
	 * Process response for success flow.
	 *
	 * @param entity
	 * @param manualVerificationDTO
	 * @param registrationStatusDto
	 * @param messageDTO
	 * @param description
	 * @return boolean
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 */
	private boolean successFlow(VerificationResponseDTO manualVerificationDTO, VerificationEntity entity,
			InternalRegistrationStatusDto registrationStatusDto, MessageDTO messageDTO, LogDescription description)
			throws JsonProcessingException {

		boolean isTransactionSuccessful = false;
		String statusCode = manualVerificationDTO.getReturnValue() == 1 ? ManualVerificationStatus.APPROVED.name()
				: ManualVerificationStatus.REJECTED.name();

		String responsetext = JsonUtils.javaObjectToJsonString(manualVerificationDTO);
		responsetext = StringUtils.isNotEmpty(responsetext) ? responsetext.replaceAll("\\s+", "") : responsetext;

		entity.setStatusCode(statusCode);
		entity.setReponseText(responsetext);
		entity.setStatusComment(statusCode.equalsIgnoreCase(ManualVerificationStatus.APPROVED.name())
				? StatusUtil.VERIFICATION_SUCCESS.getMessage()
				: StatusUtil.VERIFICATION_FAILED.getMessage());

		isTransactionSuccessful = true;
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.VERIFICATION.toString());
		registrationStatusDto.setRegistrationStageName(registrationStatusDto.getRegistrationStageName());

		if (statusCode.equalsIgnoreCase(ManualVerificationStatus.APPROVED.name())) {
			messageDTO.setIsValid(isTransactionSuccessful);
			registrationStatusDto.setStatusComment(StatusUtil.VERIFICATION_SUCCESS.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_SUCCESS.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());

			description.setMessage(PlatformSuccessMessages.RPR_VERIFICATION_SUCCESS.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_VERIFICATION_SUCCESS.getCode());

		} else if (statusCode.equalsIgnoreCase(ManualVerificationStatus.REJECTED.name())) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
			registrationStatusDto.setStatusComment(StatusUtil.VERIFICATION_FAILED.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

			description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getCode());
			messageDTO.setIsValid(Boolean.FALSE);
			messageDTO.setInternalError(Boolean.FALSE);
		} else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(StatusUtil.VERIFICATION_RESEND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.VERIFICATION_RESEND.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());

			description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode());
			messageDTO.setIsValid(Boolean.FALSE);
		}
		basePacketRepository.update(entity);

		return isTransactionSuccessful;
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

	/**
	 * This method would validate response and on failure it will mark the response
	 * for reprocessing.
	 *
	 * @param registrationStatusDto
	 * @param manualVerificationDTO
	 * @return boolean
	 * @throws JsonProcessingException
	 */
	public boolean isResendFlow(InternalRegistrationStatusDto registrationStatusDto,
			VerificationResponseDTO manualVerificationDTO, VerificationEntity entity) throws JsonProcessingException {
		boolean isResendFlow = false;
		if (manualVerificationDTO.getReturnValue() == 2) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationStatusDto.getRegistrationId(),
					"Received resend request from manual verification application. This will be marked for reprocessing.");

			// updating status code to pending so that it can be marked for manual
			// verification again
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.name());
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, ModuleName.VERIFICATION.name(),
					ModuleName.VERIFICATION.name());
			isResendFlow = true;
		}
		return isResendFlow;
	}


}
