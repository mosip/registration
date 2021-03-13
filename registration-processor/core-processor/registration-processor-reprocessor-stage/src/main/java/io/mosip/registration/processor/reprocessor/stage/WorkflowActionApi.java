package io.mosip.registration.processor.reprocessor.stage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.WorkflowActionDTO;
import io.mosip.registration.processor.reprocessor.service.WorkflowActionService;
import io.mosip.registration.processor.reprocessor.validator.WorkflowActionRequestValidator;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class WorkflowActionApi extends MosipVerticleAPIManager {
	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";


	/** The audit log request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.workflowaction.server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.workflowaction.eventbus.port}")
	private String eventBusPort;

	@Autowired
	MosipRouter router;

	@Autowired
	private WorkflowActionRequestValidator validator;

	@Autowired
	private WorkflowActionService workflowActionService;
	/**
	 * The context path.
	 */
	@Value("${server.servlet.path}")
	private String contextPath;

	private MosipEventBus mosipEventBus = null;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowActionApi.class);

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);


	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), null, null));
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
		router.post(contextPath + "/workflowaction");
		router.handler(this::processURL, this::failure);
	}

	/**
	 * method to process the context received.
	 *
	 * @param ctx the ctx
	 */
	public void processURL(RoutingContext ctx) {
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		List<String> workflowIds = null;
		String workflowAction = null;
		try {
		JsonObject obj = ctx.getBodyAsJson();

			WorkflowActionDTO workflowActionDTO = (WorkflowActionDTO) JsonUtils
					.jsonStringToJavaObject(WorkflowActionDTO.class, obj.toString());
			workflowIds = workflowActionDTO.getRequest().getWorkflowId();
			regProcLogger.debug("WorkflowActionApi:processURL called for registration ids {}",
					workflowIds);
			List<ErrorDTO> errorList = new ArrayList<ErrorDTO>();
			boolean isValid = validator.validate(workflowActionDTO, errorList);

			if (isValid) {
				workflowActionService.processWorkflowAction(workflowIds,
						workflowActionDTO.getRequest().getWorkflowAction(), mosipEventBus);
				isTransactionSuccessful = true;
				regProcLogger.info("Process the workflowAction successfully  for workflow ids and workflowaction {} {}",
						workflowIds,
						workflowAction);

				this.setResponse(ctx,
						"Process the workflowIds '" + workflowIds + "' successfully");
			}
			regProcLogger.debug("WorkflowActionApi:processURL ended for registration ids {}",
					workflowActionDTO.getRequest().getWorkflowId());
		} catch (JsonParseException e) {
			updateDTOsAndLogError(description, workflowIds, workflowAction, e.getErrorCode(), e.getMessage(), e,ctx);
		} catch (JsonMappingException e) {
			updateDTOsAndLogError(description, workflowIds, workflowAction, e.getErrorCode(), e.getMessage(), e,ctx);
		} catch (IOException e) {
			updateDTOsAndLogError(description, workflowIds, workflowAction,
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e,ctx);
		} catch (WorkflowActionException e) {
			updateDTOsAndLogError(description, workflowIds, workflowAction, e.getErrorCode(), e.getMessage(), e,ctx);
		} catch(WorkflowActionRequestValidationException e) {
			updateDTOsAndLogError(description, workflowIds, workflowAction, e.getErrorCode(), e.getMessage(), e,ctx);
		}catch (Exception e) {
			updateDTOsAndLogError(description, workflowIds, workflowAction,
					PlatformErrorMessages.UNKNOWN_EXCEPTION.getCode(),
					PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage(), e,ctx);
		}
	}

	private void updateAudit(LogDescription description, boolean isTransactionSuccessful, String registrationId) {
		if (isTransactionSuccessful)
			description.setMessage(PlatformSuccessMessages.RPR_WORKFLOW_ACTION_API_SUCCESS.getMessage());
		String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_WORKFLOW_ACTION_API_SUCCESS.getCode()
				: description.getCode();

		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
		String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
		String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
				moduleId, ModuleName.WORKFLOW_ACTION_API.toString(), registrationId);
	}

	private void updateDTOsAndLogError(LogDescription description, List<String> workflowIds, String workflowAction,
			String errorCode, String errorMessage, Exception e, RoutingContext ctx) {
		description.setMessage(errorMessage);
		description.setCode(errorCode);
		regProcLogger.error(
				"Error in  WorkflowActionApi:processURL  for registration ids  and workflowAction {} {} {} {} {}",
				workflowIds, workflowAction,
				errorMessage, e.getMessage(), ExceptionUtils.getStackTrace(e));
		ctx.fail(e);
	}

	private void failure(RoutingContext routingContext) {
		this.setResponse(routingContext, routingContext.failure().getMessage());
	}
	@Override
	public MessageDTO process(MessageDTO object) {
		return null;
	}

}
