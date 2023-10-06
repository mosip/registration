package io.mosip.registration.processor.workflowmanager.verticle;

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
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.UserNotificationException;
import io.mosip.registration.processor.core.exception.UserNotificationRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationDTO;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationRequestDTO;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationResponse;
import io.mosip.registration.processor.core.user.notification.dto.ResponseDTO;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;
import io.mosip.registration.processor.workflowmanager.validator.UserNotificationRequestValidator;
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
public class UserNotificationApi extends MosipVerticleAPIManager {

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.user.notification.api.server.port}")
	private String port;

	@Autowired
	UserNotificationApi UserNotificationApi;
	
	/** The web sub util. */
	@Autowired
	private WebSubUtil webSubUtil;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.user.notification.api.eventbus.port}")
	private String eventBusPort;

	@Value("${mosip.registration.processor.datetime.pattern}")
	private String dateTimePattern;

	@Value("${mosip.regproc.user.notification.api.id}")
	private String id;

	@Value("${mosip.regproc.user.notification.api.version}")
	private String version;
	
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.reprocessor.";

	@Autowired
	MosipRouter router;

	@Value("${server.servlet.path}")
	private String contextPath;

	private MosipEventBus mosipEventBus = null;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UserNotificationApi.class);

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The module id. */
	public static String MODULE_ID = PlatformSuccessMessages.RPR_USER_NOTIFICATION_API_SUCCESS.getCode();

	/** The module name. */
	public static String MODULE_NAME = ModuleName.USER_NOTIFICATION_API.toString();

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	UserNotificationRequestValidator UserNotificationRequestValidator;

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
		router.post(contextPath + "/userNotification");
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
			UserNotificationDTO UserNotificationDTO = JsonUtil.readValueWithUnknownProperties(obj.toString(),
					UserNotificationDTO.class);
			UserNotificationRequestDTO UserNotificationRequestDTO = UserNotificationDTO.getRequest();
			rid = UserNotificationRequestDTO.getRid();
			regProcLogger.debug("UserNotificationApi:processURL called for registration id {}",
					UserNotificationDTO.getRequest().getRid());
			UserNotificationRequestValidator.validate(UserNotificationDTO);
			String user = getUser(ctx);

			registrationStatusDto = registrationStatusService.getRegistrationStatus(rid,UserNotificationRequestDTO.getRegType(),1,null);
			if (registrationStatusDto == null) {
				description.setMessage(PlatformErrorMessages.RPR_UNA_RID_NOT_FOUND.getMessage());
				description.setCode(PlatformErrorMessages.RPR_UNA_RID_NOT_FOUND.getCode());
				updateAudit(description, UserNotificationRequestDTO.getRid(), false, user);
				throw new UserNotificationException(PlatformErrorMessages.RPR_UNA_RID_NOT_FOUND.getCode(),
						String.format(PlatformErrorMessages.RPR_UNA_RID_NOT_FOUND.getMessage(), rid));
			}
			if (!registrationStatusDto.getRegistrationType().equals(UserNotificationRequestDTO.getRegType())) {
				description.setMessage(PlatformErrorMessages.RPR_UNA_REGTYPE_NOT_MATCHING.getMessage());
				description.setCode(PlatformErrorMessages.RPR_UNA_REGTYPE_NOT_MATCHING.getCode());
				updateAudit(description, UserNotificationRequestDTO.getRid(), false, user);
				throw new UserNotificationException(PlatformErrorMessages.RPR_UNA_REGTYPE_NOT_MATCHING.getCode(),
						String.format(PlatformErrorMessages.RPR_UNA_REGTYPE_NOT_MATCHING.getMessage(), rid));
				}
				
				sendWorkflowCompletedWebSubEvent(registrationStatusDto);
				regProcLogger.info("Request added to queue succesfully  for rid {}", rid);
				description.setMessage(PlatformSuccessMessages.RPR_USER_NOTIFICATION_API_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_USER_NOTIFICATION_API_SUCCESS.getCode());
				updateAudit(description, rid, true, user);
				buildResponse(ctx, description.getMessage(), null);
				regProcLogger.debug("UserNotificationApi:processURL called ended for registration id {}", rid);

		} catch (UserNotificationRequestValidationException e) {
			logError(rid, e.getErrorCode(), e.getMessage(), e, ctx);
		} catch (UserNotificationException e) {
			logError(rid, e.getErrorCode(), e.getMessage(), e, ctx);
		} catch (Exception e) {
			logError(rid, PlatformErrorMessages.RPR_UNA_UNKNOWN_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_UNA_UNKNOWN_EXCEPTION.getMessage(), e, ctx);
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
	 * @param routingContext
	 * @param message
	 * @param errors
	 */
	private void buildResponse(RoutingContext routingContext, String message, List<ErrorDTO> errors) {
		UserNotificationResponse UserNotificationResponse = new UserNotificationResponse();
		UserNotificationResponse.setId(id);
		UserNotificationResponse.setVersion(version);
		UserNotificationResponse.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
		if (message == null) {
			UserNotificationResponse.setErrors(errors);
		} else {
			ResponseDTO responseDTO = new ResponseDTO();
			responseDTO.setStatusMessage(message);
			UserNotificationResponse.setResponse(responseDTO);
		}
		this.setResponse(routingContext, UserNotificationResponse);

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
					"Error in  UserNotificationApi:processURL  for registration id {} {} {} {}", rid, errorMessage,
					e.getMessage(), ExceptionUtils.getStackTrace(e));
		}

		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setErrorCode(errorCode);
		errorDTO.setMessage(errorMessage);
		errors.add(errorDTO);
		buildResponse(ctx, null, errors);
	}
	
	private void sendWorkflowCompletedWebSubEvent(InternalRegistrationStatusDto registrationStatusDto) {
		WorkflowCompletedEventDTO workflowCompletedEventDTO = new WorkflowCompletedEventDTO();
		workflowCompletedEventDTO.setInstanceId(registrationStatusDto.getRegistrationId());
		workflowCompletedEventDTO.setResultCode(registrationStatusDto.getStatusCode());
		workflowCompletedEventDTO.setWorkflowType(registrationStatusDto.getRegistrationType());
		if (registrationStatusDto.getStatusCode().equalsIgnoreCase(RegistrationStatusCode.REJECTED.toString())) {
			workflowCompletedEventDTO.setErrorCode(RegistrationExceptionTypeCode.PACKET_REJECTED.name());
		}
		if (registrationStatusDto.getStatusCode().equalsIgnoreCase(RegistrationStatusCode.FAILED.toString())) {
			workflowCompletedEventDTO.setErrorCode(RegistrationExceptionTypeCode.PACKET_FAILED.name());
		}

		webSubUtil.publishEvent(workflowCompletedEventDTO);

	}
	
	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}
}
