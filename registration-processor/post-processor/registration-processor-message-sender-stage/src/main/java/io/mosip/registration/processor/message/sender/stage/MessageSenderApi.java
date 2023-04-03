package io.mosip.registration.processor.message.sender.stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.MessageSenderException;
import io.mosip.registration.processor.core.exception.MessageSenderRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderDTO;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderRequestDTO;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderResponse;
import io.mosip.registration.processor.core.message.sender.dto.ResponseDTO;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.message.sender.validator.MessageSenderRequestValidator;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Sowmya
 *
 */
/**
 * @author M1022006
 *
 */
@Component
public class MessageSenderApi extends MosipVerticleAPIManager {

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.message.sender.api.server.port}")
	private String port;

	@Autowired
	MessageSenderApi messageSenderApi;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.message.sender.api.eventbus.port}")
	private String eventBusPort;

	@Value("${mosip.registration.processor.datetime.pattern}")
	private String dateTimePattern;

	@Value("${mosip.regproc.message.sender.api.id}")
	private String id;

	@Value("${mosip.regproc.message.sender.api.version}")
	private String version;

	@Autowired
	MosipRouter router;
	
	@Value("${server.servlet.path}")
	private String contextPath;

	private MosipEventBus mosipEventBus = null;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(MessageSenderApi.class);

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The module id. */
	public static String MODULE_ID = PlatformSuccessMessages.RPR_MESSAGE_SENDER_API_SUCCESS.getCode();

	/** The module name. */
	public static String MODULE_NAME = ModuleName.MESSAGE_SENDER_API.toString();

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	MessageSenderRequestValidator messageSenderRequestValidator;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), null, null));
		this.routes(router);
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	@Override
	public Integer getEventBusPort() {
		return Integer.parseInt(eventBusPort);
	}

	/**
	 * contains all the routes in this stage
	 *
	 * @param router
	 */
	private void routes(MosipRouter router) {
		router.post(contextPath + "/messagesender");
		router.handler(this::processURL, this::failure);
	}

	/**
	 * method to process the context received.
	 *
	 * @param ctx the ctx
	 */
	public void processURL(RoutingContext ctx) {
		LogDescription description = new LogDescription();
		InternalRegistrationStatusDto registrationStatusDto;
		JsonObject obj = ctx.getBodyAsJson();
		String rid = null;
		try {
			MessageSenderDTO messageSenderDTO = JsonUtil.readValueWithUnknownProperties(obj.toString(),
					MessageSenderDTO.class);
			MessageSenderRequestDTO messageSenderRequestDTO = messageSenderDTO.getRequest();
			rid = messageSenderRequestDTO.getRid();
			regProcLogger.debug("MessageSenderApi:processURL called for registration id {}",
					messageSenderDTO.getRequest().getRid());
			messageSenderRequestValidator.validate(messageSenderDTO);
			String user = getUser(ctx);

			registrationStatusDto = registrationStatusService.getRegistrationStatus(rid);
			if (registrationStatusDto == null) {
				description.setMessage(PlatformErrorMessages.RPR_MAS_RID_NOT_FOUND.getMessage());
				description.setCode(PlatformErrorMessages.RPR_MAS_RID_NOT_FOUND.getCode());
				updateAudit(description, messageSenderRequestDTO.getRid(), false, user);
				throw new MessageSenderException(PlatformErrorMessages.RPR_MAS_RID_NOT_FOUND.getCode(),
						String.format(PlatformErrorMessages.RPR_MAS_RID_NOT_FOUND.getMessage(), rid));
			}
			if (!registrationStatusDto.getRegistrationType().equals(messageSenderRequestDTO.getRegType())) {
				description.setMessage(PlatformErrorMessages.RPR_MAS_REGTYPE_NOT_MATCHING.getMessage());
				description.setCode(PlatformErrorMessages.RPR_MAS_REGTYPE_NOT_MATCHING.getCode());
				updateAudit(description, messageSenderRequestDTO.getRid(), false, user);
				throw new MessageSenderException(PlatformErrorMessages.RPR_MAS_REGTYPE_NOT_MATCHING.getCode(),
						String.format(PlatformErrorMessages.RPR_MAS_REGTYPE_NOT_MATCHING.getMessage(), rid));
				}
				MessageDTO messageDTO = new MessageDTO();
				messageDTO.setRid(messageSenderRequestDTO.getRid());
				messageDTO.setReg_type(RegistrationType.valueOf(messageSenderRequestDTO.getRegType()));
				messageDTO.setIsValid(true);
				messageDTO.setInternalError(false);
				messageDTO.setMessageBusAddress(MessageBusAddress.MESSAGE_SENDER_BUS);
				sendMessage(messageDTO, MessageBusAddress.MESSAGE_SENDER_BUS);
				regProcLogger.info("Request added to queue succesfully  for rid {}", rid);
				description.setMessage(PlatformSuccessMessages.RPR_MESSAGE_SENDER_API_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_API_SUCCESS.getCode());
				updateAudit(description, rid, true, user);
				buildResponse(ctx, description.getMessage(), null);
				regProcLogger.debug("MessageSenderApi:processURL called ended for registration id {}", rid);

		} catch (MessageSenderRequestValidationException e) {
			logError(rid, e.getErrorCode(), e.getMessage(), e, ctx);
		} catch (MessageSenderException e) {
			logError(rid, e.getErrorCode(), e.getMessage(), e, ctx);
		} catch (Exception e) {
			logError(rid, PlatformErrorMessages.RPR_MAS_UNKNOWN_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_MAS_UNKNOWN_EXCEPTION.getMessage(), e, ctx);
		}
		
	}

	/**
	 * @param routingContext
	 */
	private void failure(RoutingContext routingContext) {
		this.setResponse(routingContext, routingContext.failure().getMessage());
	}
	
	/**
	 *
	 */
	@Override
	public MessageDTO process(MessageDTO object) {
		return null;
	}

	/**
	 * @param ctx
	 * @return
	 */
	private String getUser(RoutingContext ctx) {
		String user = "";
		if (Objects.nonNull(ctx.user()) && Objects.nonNull(ctx.user().principal()))
			user = ctx.user().principal().getString("username");
		return user;
	}

	/**
	 * Update audit.
	 *
	 * @param description             the description
	 * @param registrationId          the registration id
	 * @param isTransactionSuccessful the is transaction successful
	 */
	private void updateAudit(LogDescription description, String registrationId, boolean isTransactionSuccessful,
			String user) {

		String moduleId = isTransactionSuccessful ? MODULE_ID : description.getCode();

		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
		String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
		String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId,
				eventName, eventType,
				moduleId, MODULE_NAME, registrationId, user);
	}

	/**
	 * @param message
	 * @param toAddress
	 */
	public void sendMessage(MessageDTO message, MessageBusAddress toAddress) {
		this.send(this.mosipEventBus, toAddress, message);
	}

	/**
	 * @param routingContext
	 * @param message
	 * @param errors
	 */
	private void buildResponse(RoutingContext routingContext, String message, List<ErrorDTO> errors) {
		MessageSenderResponse messageSenderResponse = new MessageSenderResponse();
		messageSenderResponse.setId(id);
		messageSenderResponse.setVersion(version);
		messageSenderResponse.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
		if (message == null) {
			messageSenderResponse.setErrors(errors);
		} else {
			ResponseDTO responseDTO = new ResponseDTO();
			responseDTO.setStatusMessage(message);
			messageSenderResponse.setResponse(responseDTO);
		}
		this.setResponse(routingContext, messageSenderResponse);

	}

	/**
	 * @param rid
	 * @param errorCode
	 * @param errorMessage
	 * @param e
	 * @param ctx
	 */
	private void logError(String rid, String errorCode, String errorMessage,
			Exception e, RoutingContext ctx) {
		if (e != null) {
			regProcLogger.error(
					"Error in  MessageSenderApi:processURL  for registration id {} {} {} {}", rid, errorMessage,
					e.getMessage(), ExceptionUtils.getStackTrace(e));
		}

		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setErrorCode(errorCode);
		errorDTO.setMessage(errorMessage);
		errors.add(errorDTO);
		buildResponse(ctx, null, errors);
	}
}
