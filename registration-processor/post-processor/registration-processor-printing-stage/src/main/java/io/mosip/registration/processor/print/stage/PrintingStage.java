package io.mosip.registration.processor.print.stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.VidType;
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
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.print.stage.exception.VidNotAvailableException;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class PrintStage.
 * 
 * @author M1048358 Alok
 * @author Ranjitha Siddegowda
 * @author Sowmya
 */
@RefreshScope
@Service
public class PrintingStage extends MosipVerticleAPIManager {

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;


	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PrintingStage.class);

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;


	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;



	/** The port. */
	@Value("${server.port}")
	private String port;

	/** The mosip event bus. */
	private MosipEventBus mosipEventBus;

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.printing.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;


	@Value("${mosip.registration.processor.encrypt:false}")
	private boolean encrypt;


	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	boolean isConnection = false;

	private static final String SEPERATOR = "::";

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Environment env;

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";


	@Autowired
	private Utilities utilities;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {

		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.PRINTING_BUS, messageExpiryTimeLimit);


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
		object.setMessageBusAddress(MessageBusAddress.PRINTING_BUS);
		object.setInternalError(Boolean.FALSE);
		LogDescription description = new LogDescription();

		boolean isTransactionSuccessful = false;
		String uin = null;
		String regId = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "PrintStage::process()::entry");

		InternalRegistrationStatusDto registrationStatusDto = null;
		RequestWrapper<CredentialRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<?> responseWrapper;
		CredentialResponseDto credentialResponseDto;
		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(regId);
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());
			registrationStatusDto.setRegistrationStageName(this.getClass().getSimpleName());
			JSONObject jsonObject = utilities.retrieveUIN(regId);
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

			}
			else {
			String vid = getVid(uin);
			CredentialRequestDto credentialRequestDto = getCredentialRequestDto(vid);
			requestWrapper.setId(env.getProperty("mosip.registration.processor.credential.request.service.id"));
			requestWrapper.setRequest(credentialRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setVersion("1.0");
			responseWrapper = (ResponseWrapper<?>) restClientService.postApi(ApiName.CREDENTIALREQUEST, null, null,
					requestWrapper, ResponseWrapper.class, MediaType.APPLICATION_JSON);
			if (responseWrapper.getErrors() != null && !responseWrapper.getErrors().isEmpty()) {
				ErrorDTO error = responseWrapper.getErrors().get(0);
				object.setIsValid(Boolean.FALSE);
				isTransactionSuccessful = false;
				description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.getCode());

				registrationStatusDto.setStatusComment(
						StatusUtil.PRINT_REQUEST_FAILED.getMessage() + SEPERATOR + error.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.PRINT_REQUEST_FAILED.getCode());
				registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				registrationStatusDto
						.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());
			} else {
				credentialResponseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
						CredentialResponseDto.class);

				registrationStatusDto.setRefId(credentialResponseDto.getRequestId());
				object.setIsValid(Boolean.TRUE);
				isTransactionSuccessful = true;
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
		} catch (VidNotAvailableException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PRINT_REQUEST_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			registrationStatusDto.setStatusComment(
					trimeExpMessage.trimExceptionMessage(StatusUtil.VID_NOT_AVAILABLE.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.VID_NOT_AVAILABLE.getCode());
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

	private CredentialRequestDto getCredentialRequestDto(String regId) {
		CredentialRequestDto credentialRequestDto = new CredentialRequestDto();

		credentialRequestDto.setCredentialType(env.getProperty("mosip.registration.processor.credentialtype"));
		credentialRequestDto.setEncrypt(encrypt);

		credentialRequestDto.setId(regId);

		credentialRequestDto.setIssuer(env.getProperty("mosip.registration.processor.issuer"));

		credentialRequestDto.setEncryptionKey(generatePin());

		return credentialRequestDto;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() {
		router.setRoute(this.postUrl(vertx, MessageBusAddress.PRINTING_BUS, null));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	public String generatePin() {
		return RandomStringUtils.randomNumeric(6);
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
}
