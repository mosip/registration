package io.mosip.registration.processor.manual.verification.service.impl;

import static io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants.DATETIME_PATTERN;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
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
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTOWrapper;
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
import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import io.mosip.registration.processor.manual.verification.dto.DataShareRequestDto;
import io.mosip.registration.processor.manual.verification.dto.DataShareResponseDto;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationStatus;
import io.mosip.registration.processor.manual.verification.dto.UserDto;
import io.mosip.registration.processor.manual.verification.exception.DataShareException;
import io.mosip.registration.processor.manual.verification.exception.InvalidFileNameException;
import io.mosip.registration.processor.manual.verification.exception.InvalidRidException;
import io.mosip.registration.processor.manual.verification.exception.MatchedRefNotExistsException;
import io.mosip.registration.processor.manual.verification.exception.NoRecordAssignedException;
import io.mosip.registration.processor.manual.verification.exception.UserIDNotPresentException;
import io.mosip.registration.processor.manual.verification.request.dto.Filter;
import io.mosip.registration.processor.manual.verification.request.dto.Gallery;
import io.mosip.registration.processor.manual.verification.request.dto.ManualAdjudicationRequestDTO;
import io.mosip.registration.processor.manual.verification.request.dto.ReferenceIds;
import io.mosip.registration.processor.manual.verification.request.dto.ShareableAttributes;
import io.mosip.registration.processor.manual.verification.request.dto.Source;
import io.mosip.registration.processor.manual.verification.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
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
	private LinkedHashMap<String, Object> policies = null;
	private static final String MANUAL_VERIFICATION = "manualverification";

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
	@Value("${registration.processor.queue.manualverification.request:mosip-to-mv}")
	private String mvRequestAddress;

	/**Manual verification queue message expiry in seconds, if given 0 then message will never expire*/
	@Value("${registration.processor.queue.manualverification.request.messageTTL}")
	private int mvRequestMessageTTL;

	@Value("${registration.processor.manual.adjudication.policy.id:mpolicy-default-adjudication}")
	private String policyId;

	@Value("${registration.processor.manual.adjudication.subscriber.id:mpartner-default-adjudication}")
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

	/** The Constant PROTOCOL. */
	public static final String PROTOCOL = "https";

	private boolean isMatchTypeDemoOrBio(String matchType) {
		return matchType.equalsIgnoreCase(DedupeSourceName.DEMO.toString())
				|| matchType.equalsIgnoreCase(DedupeSourceName.BIO.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.manual.adjudication.service.
	 * ManualAdjudicationService#updatePacketStatus(io.mosip.registration.processor.
	 * manual.adjudication.dto.ManualVerificationDTO)
	 */
	@Override
	public boolean updatePacketStatus(ManualAdjudicationResponseDTO manualVerificationDTO, String stageName,MosipQueue queue) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(),
				manualVerificationDTO.getRequestId(), "ManualVerificationServiceImpl::updatePacketStatus()::entry");

		ManualVerificationEntity entity = validateRequestIdAndReturnRid(manualVerificationDTO.getRequestId());
		String regId = entity.getRegId();

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(entity.getRegId(), null, null, entity.getId().getWorkflowInstanceId());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.MANUAL_VERIFICATION.name());
		registrationStatusDto.setRegistrationStageName(stageName);

		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		messageDTO.setRid(regId);
		messageDTO.setReg_type(registrationStatusDto.getRegistrationType());

		try {

			List<ManualVerificationEntity> entities = retrieveInqueuedRecordsByRid(regId);

			// check if response is marked for resend
			if (isResendFlow(regId, manualVerificationDTO, entities)) {
				registrationStatusDto.setStatusComment(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				description.setMessage(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode());
				messageDTO.setInternalError(true);
				messageDTO.setIsValid(isTransactionSuccessful);
				manualVerificationStage.sendMessage(messageDTO);
			} else {
				// call success flow and process the response received from manual verification system
				isTransactionSuccessful = successFlow(
						entity, manualVerificationDTO, entities, registrationStatusDto, messageDTO, description);

				registrationStatusDto.setUpdatedBy(USER);
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						regId, description.getMessage());
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
		} catch (IOException e) {
			messageDTO.setInternalError(true);
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());

			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			messageDTO.setInternalError(true);
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());

			description.setMessage(PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.UNKNOWN_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} finally {
			if(messageDTO.getInternalError()) {
				updateErrorFlags(registrationStatusDto, messageDTO);
			}
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode()
					: description.getCode();
			String moduleName = ModuleName.MANUAL_VERIFICATION.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, regId);

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "ManualVerificationServiceImpl::updatePacketStatus()::exit");
		return isTransactionSuccessful;

	}

	/**
	 * Basic validation of requestId received against the rid present in manual-verification table
	 * Returns the correct rid after successful validation
	 * @param reqId : the request id
	 * @return rid : the registration id
	 */
	private ManualVerificationEntity validateRequestIdAndReturnRid(String reqId) {
		List<ManualVerificationEntity> entities = basePacketRepository.getRegistrationIdbyRequestId(reqId);

		if (CollectionUtils.isEmpty(entities) || new HashSet<>(entities.stream().map(e -> e.getRegId()).collect(Collectors.toList())).size() != 1) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					entities != null ? entities.stream().map(e -> e.getRegId()).collect(Collectors.toList()) : null,
					"Multiple rids found against request id : " + reqId);
			throw new InvalidRidException(
					PlatformErrorMessages.RPR_INVALID_RID_FOUND.getCode(), PlatformErrorMessages.RPR_INVALID_RID_FOUND.getCode());
		}

		ManualVerificationEntity entity = entities.iterator().next();

		if (entity != null && StringUtils.isEmpty(entity.getRegId())) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					entity.getRegId(), "ManualVerificationServiceImpl::updatePacketStatus()::InvalidFileNameException"
							+ PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			throw new InvalidFileNameException(PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
					PlatformErrorMessages.RPR_MVS_REG_ID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
		}
		return entity;
	}

	private List<ManualVerificationEntity> retrieveInqueuedRecordsByRid(String regId) {

		List<ManualVerificationEntity> entities = basePacketRepository.getAllAssignedRecord(
				regId, ManualVerificationStatus.INQUEUE.name());

		if (CollectionUtils.isEmpty(entities)) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "ManualVerificationServiceImpl::updatePacketStatus()"
							+ PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
			throw new NoRecordAssignedException(PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_ASSIGNED_RECORD.getMessage());
		}

		return entities;
	}


	@SuppressWarnings({ "unchecked", "unused" })
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
	private void pushRequestToQueue(MessageDTO messageDTO, MosipQueue queue) throws Exception {
		String refId = messageDTO.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				refId, "ManualVerificationServiceImpl::pushRequestToQueue()::entry");
		List<ManualVerificationEntity> mves = getMatchingEntitiesforRefId(refId);
		if (mves.size() == 0 || null == mves)
			throw new MatchedRefNotExistsException(
					PlatformErrorMessages.RPR_MVS_NO_MATCHEDRID_FOUND_FOR_GIVEN_RID.getCode(),
					PlatformErrorMessages.RPR_MVS_NO_MATCHEDRID_FOUND_FOR_GIVEN_RID.getMessage());

		ManualAdjudicationRequestDTO mar = prepareManualAdjudicationRequest(messageDTO, mves);
		String requestId = UUID.randomUUID().toString();
		mar.setRequestId(requestId);
		regProcLogger.info("Request : " + JsonUtils.javaObjectToJsonString(mar));
		if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
			mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar), mvRequestAddress, mvRequestMessageTTL);
		else
			mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar).getBytes(), mvRequestAddress, mvRequestMessageTTL);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				refId, "ManualVerificationServiceImpl::pushRequestToQueue()::entry");

		updateManualVerificationEntityRID(mves, requestId);
	}

	private String getDataShareUrl(String id, String process) throws Exception {
		DataShareRequestDto requestDto = new DataShareRequestDto();

		LinkedHashMap<String, Object> policy = getPolicy();

		Map<String, String> policyMap = getPolicyMap(policy);

		// set demographic
		Map<String, String> demographicMap = policyMap.entrySet().stream().filter(e-> e.getValue() != null &&
				(!META_INFO.equalsIgnoreCase(e.getValue()) && !AUDITS.equalsIgnoreCase(e.getValue())))
				.collect(Collectors.toMap(e-> e.getKey(),e -> e.getValue()));
		requestDto.setIdentity(packetManagerService.getFields(id, demographicMap.values().stream().collect(Collectors.toList()), process, ProviderStageName.MANUAL_VERIFICATION));

		// set documents
		JSONObject docJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
		for (Object doc : docJson.keySet()) {
			if (doc != null) {
				HashMap docmap = (HashMap) docJson.get(doc.toString());
				String docName = docmap != null && docmap.get(MappingJsonConstants.VALUE)!= null ? docmap.get(MappingJsonConstants.VALUE).toString() : null;
				if (policyMap.containsValue(docName)) {
					Document document = packetManagerService.getDocument(id, doc.toString(), process, ProviderStageName.MANUAL_VERIFICATION);
					if (document != null) {
						if (requestDto.getDocuments() != null)
							requestDto.getDocuments().put(docmap.get(MappingJsonConstants.VALUE).toString(), CryptoUtil.encodeBase64String(document.getDocument()));
						else {
							Map<String, String> docMap = new HashMap<>();
							docMap.put(docmap.get(MappingJsonConstants.VALUE).toString(), CryptoUtil.encodeBase64String(document.getDocument()));
							requestDto.setDocuments(docMap);
						}
					}
				}
			}
		}

		// set audits
		if (policyMap.containsValue(AUDITS))
			requestDto.setAudits(JsonUtils.javaObjectToJsonString(packetManagerService.getAudits(id, process, ProviderStageName.MANUAL_VERIFICATION)));

		// set metainfo
		if (policyMap.containsValue(META_INFO))
			requestDto.setMetaInfo(JsonUtils.javaObjectToJsonString(packetManagerService.getMetaInfo(id, process, ProviderStageName.MANUAL_VERIFICATION)));


		// set biometrics
		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);

		if (policyMap.containsValue(individualBiometricsLabel)) {
			List<String> modalities = getModalities(policy);
			BiometricRecord biometricRecord = packetManagerService.getBiometrics(
					id, individualBiometricsLabel, modalities, process, ProviderStageName.MANUAL_VERIFICATION);
			byte[] content = cbeffutil.createXML(biometricRecord.getSegments());
			requestDto.setBiometrics(content != null ? CryptoUtil.encodeBase64(content) : null);
		}


		String req = JsonUtils.javaObjectToJsonString(requestDto);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("name", MANUAL_VERIFICATION);
		map.add("filename", MANUAL_VERIFICATION);

		ByteArrayResource contentsAsResource = new ByteArrayResource(req.getBytes()) {
			@Override
			public String getFilename() {
				return MANUAL_VERIFICATION;
			}
		};
		map.add("file", contentsAsResource);

		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(policyId);
		pathSegments.add(subscriberId);
		URL dataShareUrl = null;
		String protocol = PROTOCOL;
		String url = null;

		if (httpProtocol != null && !httpProtocol.isEmpty()) {
			protocol = httpProtocol;
		}

		dataShareUrl = new URL(protocol, internalDomainName, env.getProperty(ApiName.DATASHARECREATEURL.name()));
		url = dataShareUrl.toString();
		url = url.replaceAll("[\\[\\]]", "");
		io.mosip.kernel.core.http.ResponseWrapper<DataShareResponseDto> resp = new io.mosip.kernel.core.http.ResponseWrapper<>();

		LinkedHashMap response = (LinkedHashMap) registrationProcessorRestClientService.postApi(url, MediaType.MULTIPART_FORM_DATA, pathSegments, null, null, map, LinkedHashMap.class);
		if (response == null || (response.get(ERRORS) != null))
			throw new DataShareException(response == null ? "Datashare response is null" : response.get(ERRORS).toString());

		LinkedHashMap datashare = (LinkedHashMap) response.get(DATASHARE);
		return datashare.get(URL) != null ? datashare.get(URL).toString() : null;
	}

	private Map<String, String> getPolicyMap(LinkedHashMap<String, Object> policies) throws DataShareException, IOException, ApisResourceAccessException {
		Map<String, String> policyMap = new HashMap<>();
		List<LinkedHashMap> attributes = (List<LinkedHashMap>) policies.get(ManualVerificationConstants.SHAREABLE_ATTRIBUTES);
		ObjectMapper mapper = new ObjectMapper();
		for (LinkedHashMap map : attributes) {
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(map),
					ShareableAttributes.class);
			policyMap.put(shareableAttributes.getAttributeName(), shareableAttributes.getSource().iterator().next().getAttribute());
		}
		return policyMap;

	}

	private LinkedHashMap<String, Object> getPolicy() throws DataShareException, ApisResourceAccessException {
		if (policies != null && policies.size() > 0)
			return policies;

		ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
				ApiName.PMS, Lists.newArrayList(policyId, PolicyConstant.PARTNER_ID, subscriberId), "", "", ResponseWrapper.class);
		if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() >0)) {
			throw new DataShareException(policyResponse == null ? "Policy Response response is null" : policyResponse.getErrors().get(0).getMessage());

		} else {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
			policies = (LinkedHashMap<String, Object>) responseMap.get(ManualVerificationConstants.POLICIES);
		}
		return policies;

	}


	public List<String> getModalities(LinkedHashMap<String, Object> policy) throws IOException{
		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		List<LinkedHashMap> attributes = (List<LinkedHashMap>) policy.get(ManualVerificationConstants.SHAREABLE_ATTRIBUTES);
		ObjectMapper mapper = new ObjectMapper();
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
		List<String> modalities=new ArrayList<>();
		for(Map.Entry<String, List<String>> entry : typeAndSubTypeMap.entrySet()) {
			if(entry.getValue() == null) {
				modalities.add(entry.getKey());
			} else {
				modalities.addAll(entry.getValue());
			}
		}

		return modalities;

	}

	/*
	 * get matched ref id for a given registration id
	 */
	private List<ManualVerificationEntity> getMatchingEntitiesforRefId(String rid) {
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"ManualVerificationServiceImpl::getMatchingEntitiesforRefId()::entry");

		List<ManualVerificationEntity> matchedEntities = basePacketRepository.getMatchedIds(rid, ManualVerificationStatus.PENDING.name());

		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"ManualVerificationServiceImpl::getMatchingEntitiesforRefId()::entry");

		return matchedEntities;
	}

	/*
	 * Form manual adjudication request
	 */
	private ManualAdjudicationRequestDTO prepareManualAdjudicationRequest(MessageDTO messageDTO, List<ManualVerificationEntity> mve) throws Exception {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ManualVerificationServiceImpl::formAdjudicationRequest()::entry");

		ManualAdjudicationRequestDTO req = new ManualAdjudicationRequestDTO();
		req.setId(ManualVerificationConstants.MANUAL_ADJUDICATION_ID);
		req.setVersion(ManualVerificationConstants.VERSION);
		req.setRequestId(mve.get(0).getRequestId());
		req.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		req.setReferenceId(mve.get(0).getRegId());
		InternalRegistrationStatusDto registrationStatusDto = null;
		registrationStatusDto = registrationStatusService.getRegistrationStatus(
				mve.get(0).getRegId(), messageDTO.getReg_type(), messageDTO.getIteration(), mve.get(0).getId().getWorkflowInstanceId());
		try {
			req.setReferenceURL(
					getDataShareUrl(mve.get(0).getRegId(), registrationStatusDto.getRegistrationType()));

		} catch (PacketManagerException | ApisResourceAccessException ex) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					ex.getErrorCode(), ex.getErrorText());
			throw ex;
		}

		List<ReferenceIds> referenceIds = new ArrayList<>();
		mve.forEach(e -> {
			ReferenceIds r = new ReferenceIds();
			InternalRegistrationStatusDto registrationStatusDto1 = null;
			registrationStatusDto1 = registrationStatusService.getRegistrationStatus(
					e.getId().getMatchedRefId(),messageDTO.getReg_type(), messageDTO.getIteration(), e.getId().getWorkflowInstanceId());

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
				regProcLogger.error(ExceptionUtils.getStackTrace(exp));
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
	public MessageDTO process(MessageDTO object, MosipQueue queue) {
		try {
			object.setInternalError(false);
			object.setIsValid(false);
			object.setMessageBusAddress(MessageBusAddress.MANUAL_VERIFICATION_BUS_IN);

			if (null == object.getRid() || object.getRid().isEmpty())
				throw new InvalidRidException(PlatformErrorMessages.RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL.getCode(),
						PlatformErrorMessages.RPR_MVS_NO_RID_SHOULD_NOT_EMPTY_OR_NULL.getMessage());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					object.getRid(), "ManualVerificationServiceImpl::process()::entry");
			pushRequestToQueue(object, queue);

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
			regProcLogger.error(ExceptionUtils.getStackTrace(e));
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
	private void updateManualVerificationEntityRID(List<ManualVerificationEntity> mves, String requestId) {
		mves.stream().forEach(mve -> {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					mve.getRegId(), "ManualVerificationServiceImpl::updateManualVerificationEntityRID()::entry");
			mve.setStatusCode(ManualVerificationStatus.INQUEUE.name());
			mve.setStatusComment("Sent to manual adjudication queue");
			mve.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
			mve.setRequestId(requestId);
			basePacketRepository.update(mve);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					mve.getRegId(), "ManualVerificationServiceImpl::updateManualVerificationEntityRID()::exit");
		});
	}

	/**
	 * Process response for success flow.
	 *
	 * @param entity
	 * @param manualVerificationDTO
	 * @param entities
	 * @param registrationStatusDto
	 * @param messageDTO
	 * @param description
	 * @return boolean
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 */
	private boolean successFlow(ManualVerificationEntity entity, ManualAdjudicationResponseDTO manualVerificationDTO,
								List<ManualVerificationEntity> entities,
								InternalRegistrationStatusDto registrationStatusDto, MessageDTO messageDTO,
								LogDescription description) throws com.fasterxml.jackson.core.JsonProcessingException {

		boolean isTransactionSuccessful = false;
		String statusCode = manualVerificationDTO.getReturnValue() == 1 &&
				CollectionUtils.isEmpty(manualVerificationDTO.getCandidateList().getCandidates()) ?
				ManualVerificationStatus.APPROVED.name() : ManualVerificationStatus.REJECTED.name();

		for (int i = 0; i < entities.size(); i++) {
			ObjectMapper objectMapper = new ObjectMapper();
			byte[] responsetext = objectMapper.writeValueAsBytes(manualVerificationDTO);

			ManualVerificationEntity manualVerificationEntity=entities.get(i);
			manualVerificationEntity.setStatusCode(statusCode);
			manualVerificationEntity.setReponseText(responsetext);
			manualVerificationEntity.setStatusComment(statusCode.equalsIgnoreCase(ManualVerificationStatus.APPROVED.name()) ?
					StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getMessage() :
					StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getMessage());
			entities.set(i, manualVerificationEntity);
		}
		isTransactionSuccessful = true;
		registrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.MANUAL_VERIFICATION.toString());
		registrationStatusDto.setRegistrationStageName(registrationStatusDto.getRegistrationStageName());

		if (statusCode != null && statusCode.equalsIgnoreCase(ManualVerificationStatus.APPROVED.name())) {
			if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(RegistrationType.LOST.toString())) {
				for(ManualVerificationEntity detail: entities) {
					packetInfoManager.saveRegLostUinDet(entity.getRegId(),
							entity.getId().getWorkflowInstanceId(), detail.getId().getMatchedRefId(),
							PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode(),
							ModuleName.MANUAL_VERIFICATION.toString());
				}
			}
			messageDTO.setIsValid(isTransactionSuccessful);
			manualVerificationStage.sendMessage(messageDTO);
			registrationStatusDto.setStatusComment(StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.MANUAL_VERIFIER_APPROVED_PACKET.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());

			description.setMessage(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_APPROVED.getCode());

		} else if (statusCode != null && statusCode.equalsIgnoreCase(ManualVerificationStatus.REJECTED.name())) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
			registrationStatusDto.setStatusComment(StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.MANUAL_VERIFIER_REJECTED_PACKET.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

			description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_REJECTED.getCode());
			messageDTO.setIsValid(Boolean.FALSE);
			manualVerificationStage.sendMessage(messageDTO);
		} else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.RPR_MANUAL_VERIFICATION_RESEND.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());

			description.setMessage(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_RESEND.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode());
			messageDTO.setIsValid(Boolean.FALSE);
			manualVerificationStage.sendMessage(messageDTO);
		}
		List<ManualVerificationEntity> maVerificationEntity = new ArrayList<>();
		for(ManualVerificationEntity manualVerificationEntity: entities) {
			maVerificationEntity.add(basePacketRepository.update(manualVerificationEntity));
		}

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
	 * Validate the response received from manual verification system.
	 *
	 * @param regId
	 * @param manualVerificationDTO
	 * @param entities
	 * @return boolean
	 * @throws JsonProcessingException
	 */
	private boolean isResponseValidationSuccess(String regId, ManualAdjudicationResponseDTO manualVerificationDTO, List<ManualVerificationEntity> entities) throws JsonProcessingException {
		boolean isValidationSuccess = true;
		// if candidate count is a positive number
		if (manualVerificationDTO.getReturnValue() == 1
				&& manualVerificationDTO.getCandidateList() != null
				&& manualVerificationDTO.getCandidateList().getCount() > 0) {

			// get the reference ids from response candidates.
			List<String> refIdsFromResponse = !CollectionUtils.isEmpty(manualVerificationDTO.getCandidateList().getCandidates()) ?
					manualVerificationDTO.getCandidateList().getCandidates().stream().map(c -> c.getReferenceId()).collect(Collectors.toList())
					: Collections.emptyList();

			// get the reference ids from manual verification table entities.
			List<String> refIdsFromEntities = entities.stream().map(e -> e.getId().getMatchedRefId()).collect(Collectors.toList());

			if (!manualVerificationDTO.getCandidateList().getCount().equals(refIdsFromResponse.size())) {
				String errorMessage = "Validation error - Candidate count does not match reference ids count.";
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						regId, errorMessage);
				auditLogRequestBuilder.createAuditRequestBuilder(
						errorMessage + " Response received : "
								+JsonUtils.javaObjectToJsonString(manualVerificationDTO), EventId.RPR_405.toString(),
						EventName.EXCEPTION.name(), EventType.BUSINESS.name(),
						PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode(), ModuleName.MANUAL_VERIFICATION.toString(), regId);
				isValidationSuccess = false;

			} else if (!refIdsFromEntities.containsAll(refIdsFromResponse)) {
				String errorMessage = "Validation error - Received ReferenceIds does not match reference ids in manual verification table.";
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						regId, errorMessage);
				auditLogRequestBuilder.createAuditRequestBuilder(
						errorMessage + " Response received : "
								+JsonUtils.javaObjectToJsonString(manualVerificationDTO), EventId.RPR_405.toString(),
						EventName.EXCEPTION.name(), EventType.BUSINESS.name(),
						PlatformSuccessMessages.RPR_MANUAL_VERIFICATION_RESEND.getCode(), ModuleName.MANUAL_VERIFICATION.toString(), regId);
				isValidationSuccess = false;
			}
		}
		return isValidationSuccess;
	}

	/**
	 * This method would validate response and on failure it will mark the response for reprocessing.
	 *
	 * @param regId
	 * @param manualVerificationDTO
	 * @param entities
	 * @return boolean
	 * @throws JsonProcessingException
	 */
	public boolean isResendFlow(String regId, ManualAdjudicationResponseDTO manualVerificationDTO, List<ManualVerificationEntity> entities) throws JsonProcessingException {
		boolean isResendFlow = false;
		if(manualVerificationDTO.getReturnValue() == 2 || !isResponseValidationSuccess(regId, manualVerificationDTO, entities)) {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "Received resend request from manual verification application. This will be marked for reprocessing.");

			// updating status code to pending so that it can be marked for manual verification again
			entities.forEach(e -> {
				e.setStatusCode(ManualVerificationStatus.PENDING.name());
				basePacketRepository.update(e);
			});
			isResendFlow = true;
		}
		return isResendFlow;
	}


}
