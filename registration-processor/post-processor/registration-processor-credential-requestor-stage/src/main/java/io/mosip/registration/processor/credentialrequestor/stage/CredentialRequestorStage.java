package io.mosip.registration.processor.credentialrequestor.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.*;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.*;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.*;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.CredentialRequestDto;
import io.mosip.registration.processor.core.idrepo.dto.CredentialResponseDto;
import io.mosip.registration.processor.core.idrepo.dto.VidInfoDTO;
import io.mosip.registration.processor.core.idrepo.dto.VidsInfosDTO;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.credentialrequestor.dto.CredentialPartner;
import io.mosip.registration.processor.credentialrequestor.stage.exception.VidNotAvailableException;
import io.mosip.registration.processor.credentialrequestor.util.CredentialPartnerUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Class PrintStage.
 * 
 * @author M1048358 Alok
 * @author Ranjitha Siddegowda
 * @author Sowmya
 */
@RefreshScope
@Service
@Configuration
@EnableScheduling
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.config", 
		"io.mosip.registration.processor.credentialrequestor.config",
		"io.mosip.registrationprocessor.stages.config",
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", 
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", 
		"io.mosip.kernel.idobjectvalidator.config",
		"io.mosip.registration.processor.core.kernel.beans" })
public class CredentialRequestorStage extends MosipVerticleAPIManager {
	
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.credentialrequestor.";
	private Random sr = null;
	private static final int max = 999999;
	private static final int min = 100000;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(CredentialRequestorStage.class);

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;


	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The mosip event bus. */
	private MosipEventBus mosipEventBus;

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.credentialrequestor.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	@Value("${mosip.registration.processor.encrypt:false}")
	private boolean encrypt;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	private static final String SEPERATOR = "::";

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Environment env;

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	private static final String ISSUERS = "mosip.registration.processor.issuer";

	@Value("#{T(java.util.Arrays).asList('${mosip.registration.processor.credential.default.partner-ids:}')}")
	private List<String> defaultPartners;

	private static String COMMA = ",";
	private static String HASH_DELIMITER = "#";

	@Autowired
	private Utilities utilities;

	@Autowired
	private CredentialPartnerUtil credentialPartnerUtil;

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.PRINTING_BUS_IN, MessageBusAddress.PRINTING_BUS_OUT,
				messageExpiryTimeLimit);
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
		TrimExceptionMessage trimeExpMessage = new TrimExceptionMessage();
		object.setMessageBusAddress(MessageBusAddress.PRINTING_BUS_IN);
		object.setInternalError(Boolean.FALSE);
		object.setIsValid(Boolean.FALSE);
		LogDescription description = new LogDescription();

		boolean isTransactionSuccessful = false;
		String uin = null;
		String refIds = null;
		String regId = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "PrintStage::process()::entry");

		InternalRegistrationStatusDto registrationStatusDto = null;
		RequestWrapper<CredentialRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<?> responseWrapper = null;
		CredentialResponseDto credentialResponseDto;
		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(
					regId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());
			registrationStatusDto.setRegistrationStageName(getStageName());
			JSONObject jsonObject = utilities.idrepoRetrieveIdentityByRid(regId);
			uin = JsonUtil.getJSONValue(jsonObject, IdType.UIN.toString());
			if (uin == null) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), null,
						PlatformErrorMessages.RPR_PRT_UIN_NOT_FOUND_IN_DATABASE.name());
				object.setIsValid(Boolean.FALSE);
				isTransactionSuccessful = false;
				description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getCode());

				registrationStatusDto.setStatusComment(
						StatusUtil.UIN_NOT_FOUND_IN_DATABASE.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.UIN_NOT_FOUND_IN_DATABASE.getCode());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				registrationStatusDto
						.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());

			} else {
				requestWrapper.setId(env.getProperty("mosip.registration.processor.credential.request.service.id"));
				DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
				requestWrapper.setVersion("1.0");
				List<CredentialPartner> allIssuerList = credentialPartnerUtil.getAllCredentialPartners().getPartners();
				// filtering with default partner ids and process
				List<CredentialPartner> filteredPartners = allIssuerList.stream()
						.filter(issuer -> defaultPartners.contains(issuer.getId()))
						.filter(issuer -> (issuer.getProcess() == null) || (issuer.getProcess().contains(object.getReg_type())))
						.collect(Collectors.toList());
				filteredPartners.addAll(credentialPartnerUtil.getCredentialPartners(
						regId, registrationStatusDto.getRegistrationType(), jsonObject));
				for (CredentialPartner key : filteredPartners) {
					CredentialRequestDto credentialRequestDto = getCredentialRequestDto(regId, registrationStatusDto.getRegistrationType(), key);
					LocalDateTime localdatetime = LocalDateTime.parse(
							DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
					requestWrapper.setRequesttime(localdatetime);
					requestWrapper.setRequest(credentialRequestDto);
					// issuers with appIdBasedCredentialIdSuffix is calling v1 api and for others stage is calling v2 api for credential
					if (StringUtils.isNotEmpty(key.getAppIdBasedCredentialIdSuffix())) {
						List<String> pathsegments = new ArrayList<>();
						pathsegments.add(regId + key.getAppIdBasedCredentialIdSuffix()); //  #PDF suffix is added to identify the requested credential via rid
						responseWrapper = (ResponseWrapper<?>) restClientService.postApi(ApiName.CREDENTIALREQUESTV2, MediaType.APPLICATION_JSON, pathsegments, null,
									null, requestWrapper, ResponseWrapper.class);
					} else {
						responseWrapper = (ResponseWrapper<?>) restClientService.postApi(ApiName.CREDENTIALREQUEST, null, null,
								requestWrapper, ResponseWrapper.class, MediaType.APPLICATION_JSON);
					}
					if (responseWrapper.getErrors() != null && !responseWrapper.getErrors().isEmpty()) {
						ErrorDTO error = responseWrapper.getErrors().get(0);
						object.setIsValid(Boolean.FALSE);
						isTransactionSuccessful = false;
						registrationStatusDto.setRefId(refIds);
						description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getMessage());
						description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getCode());

						registrationStatusDto.setStatusComment(
								StatusUtil.PRINT_REQUEST_FAILED.getMessage() + SEPERATOR + error.getMessage());
						registrationStatusDto.setSubStatusCode(StatusUtil.PRINT_REQUEST_FAILED.getCode());
						registrationStatusDto
								.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
						registrationStatusDto
								.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());
						break;
					} else {
						credentialResponseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
								CredentialResponseDto.class);
						refIds = credentialResponseDto.getRequestId();
						isTransactionSuccessful = true;
					}
				}
				if (isTransactionSuccessful) {
					registrationStatusDto.setRefId(refIds);
					object.setIsValid(Boolean.TRUE);
					description.setMessage(PlatformSuccessMessages.RPR_PRINT_STAGE_REQUEST_SUCCESS.getMessage());
					description.setCode(PlatformSuccessMessages.RPR_PRINT_STAGE_REQUEST_SUCCESS.getCode());
					registrationStatusDto.setStatusComment(
							trimeExpMessage.trimExceptionMessage(StatusUtil.PRINT_REQUEST_SUCCESS.getMessage()));
					registrationStatusDto.setSubStatusCode(StatusUtil.PRINT_REQUEST_SUCCESS.getCode());
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
					registrationStatusDto
							.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());

					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), regId, "PrintStage::process()::exit");
				}
			}
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			registrationStatusDto.setStatusComment(trimeExpMessage.trimExceptionMessage(
					StatusUtil.API_RESOUCE_ACCESS_FAILED.getMessage() + SEPERATOR + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			registrationStatusDto.setStatusComment(
					trimeExpMessage.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			registrationStatusDto.setStatusComment(
					trimeExpMessage.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getCode());
			object.setInternalError(Boolean.TRUE);
		}
		finally {
			if (object.getInternalError()) {
				updateErrorFlags(registrationStatusDto, object);
			}
			String eventId = "";
			String eventName = "";
			String eventType = "";
			eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_PRINT_STAGE_REQUEST_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.PRINT_STAGE.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, regId);

		}
		return object;
	}

	private CredentialRequestDto getCredentialRequestDto(String regId, String process, CredentialPartner key) {
		CredentialRequestDto credentialRequestDto = new CredentialRequestDto();
		Map<String, Object> additionalAttributes=new HashMap<>();

		credentialRequestDto.setCredentialType(key.getCredentialType());
		credentialRequestDto.setEncrypt(encrypt);

		credentialRequestDto.setId(regId);

		credentialRequestDto.setIssuer(key.getPartnerId());

		credentialRequestDto.setEncryptionKey(generatePin());
		additionalAttributes.put("templateTypeCode", key.getTemplate());
		additionalAttributes.put("registrationId", regId);
		if (CollectionUtils.isNotEmpty(key.getMetaInfoFields()))
			getAdditionalCredentialFields(regId, process, key.getMetaInfoFields(), additionalAttributes);
		credentialRequestDto.setAdditionalData(additionalAttributes);

		return credentialRequestDto;
	}

	private void getAdditionalCredentialFields(String regId, String process,
											   List<String> metaInfoFields,
											   Map<String, Object> additionalAttributes) {
		try {
			Map<String,String> metaInfo = utilities.getPacketManagerService().getMetaInfo(regId, process, ProviderStageName.CREDENTIAL_REQUESTOR);
			JSONArray metadata = new JSONArray(metaInfo.get(JsonConstant.METADATA));
			for(int i=0; i<metadata.length(); i++){
				org.json.JSONObject jsonObject = metadata.getJSONObject(i);
				if (metaInfoFields.contains(jsonObject.getString("label"))){
					additionalAttributes.put(jsonObject.getString("label"), jsonObject.getString("value"));
				}
			}

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, RegistrationStatusCode.FAILED + e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new BaseUncheckedException(PlatformErrorMessages.RPR_PRT_PARSING_ADDITIONAL_CRED_CONFIG.getCode(),
					PlatformErrorMessages.RPR_PRT_PARSING_ADDITIONAL_CRED_CONFIG.getMessage(), e);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), 
				MessageBusAddress.PRINTING_BUS_IN, MessageBusAddress.PRINTING_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}

	public String generatePin() {
		if (sr == null)
			instantiate();
		int randomInteger = sr.nextInt(max - min) + min;
		return String.valueOf(randomInteger);
	}

	@SuppressWarnings("unchecked")
	private String getVid(String uin) throws ApisResourceAccessException, VidNotAvailableException {
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(uin);
		String vid = null;

		VidsInfosDTO vidsInfosDTO;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getVid():: get GETVIDSBYUIN service call started with request data : "
		);

		vidsInfosDTO =  (VidsInfosDTO) restClientService.getApi(ApiName.GETVIDSBYUIN,
				pathsegments, "", "", VidsInfosDTO.class);
	
		if (vidsInfosDTO.getErrors() != null && !vidsInfosDTO.getErrors().isEmpty()) {
			ServiceError error = vidsInfosDTO.getErrors().get(0);
			throw new VidNotAvailableException(PlatformErrorMessages.RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION.getCode(),
					error.getMessage());

		} else {
			if(vidsInfosDTO.getResponse()!=null && !vidsInfosDTO.getResponse().isEmpty()) {
				for (VidInfoDTO VidInfoDTO : vidsInfosDTO.getResponse()) {
					if (VidType.PERPETUAL.name().equalsIgnoreCase(VidInfoDTO.getVidType())) {
						vid = VidInfoDTO.getVid();
						break;
					}
				}
				if (vid == null) {
					throw new VidNotAvailableException(
							PlatformErrorMessages.RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION.getCode(),
							PlatformErrorMessages.RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION.getMessage());
				}
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"PrintServiceImpl::getVid():: get GETVIDSBYUIN service call ended successfully");

			}else {
				throw new VidNotAvailableException(PlatformErrorMessages.RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION.getMessage());
			}
			
		}

		return vid;
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

	@Scheduled(fixedDelayString = "${mosip.regproc.printstage.pingeneration.refresh.millisecs:1800000}",
			initialDelayString = "${mosip.regproc.printstage.pingeneration.refresh.delay-on-startup.millisecs:5000}")
	private void instantiate() {
		regProcLogger.debug("Instantiating SecureRandom for credential pin generation............");
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			regProcLogger.error("Could not instantiate SecureRandom for credential pin generation", e);
		}
	}
}