package io.mosip.registration.processor.reprocessor.stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.PageResponseDto;
import io.mosip.registration.processor.core.workflow.dto.WorkflowDetail;
import io.mosip.registration.processor.core.workflow.dto.WorkflowSearchRequestDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowSearchResponseDTO;
import io.mosip.registration.processor.reprocessor.service.WorkflowSearchService;
import io.mosip.registration.processor.reprocessor.validator.WorkflowSearchRequestValidator;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class WorkflowSearchApi extends MosipVerticleAPIManager {


	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.workflowsearch.server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.reprocessor.";

	@Value("${mosip.regproc.workflowsearch.eventbus.port}")
	private String eventBusPort;

	@Value("${mosip.registration.processor.datetime.pattern}")
	private String dateTimePattern;

	@Value("${mosip.regproc.workflow.search.api-id}")
	private String id;

	@Value("${mosip.regproc.workflow.search.version}")
	private String version;

	@Autowired
	MosipRouter router;

	/**
	 * The context path.
	 */
	@Value("${server.servlet.path}")
	private String contextPath;

	private MosipEventBus mosipEventBus = null;


	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowSearchApi.class);

	/** The module id. */
	public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_ACTION_API_SUCCESS.getCode();

	/** The module name. */
	public static String MODULE_NAME = ModuleName.WORKFLOW_ACTION_API.toString();

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	WorkflowSearchService workflowSearchService;

	@Autowired
	WorkflowSearchRequestValidator workflowSearchRequestValidator;

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
		router.post(contextPath + "/workflow/search");
		router.handler(this::processURL, this::failure);
	}
	/**
	 * method to process the context received.
	 *
	 * @param ctx the ctx
	 */
	public void processURL(RoutingContext ctx) {

		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		try {
			JsonObject obj = ctx.getBodyAsJson();
			String user = getUser(ctx);
			WorkflowSearchRequestDTO searchRequestDTO = (WorkflowSearchRequestDTO) JsonUtils
					.jsonStringToJavaObject(WorkflowSearchRequestDTO.class, obj.toString());
			regProcLogger.debug("WorkflowActionApi:processURL called");
			workflowSearchRequestValidator.validate(searchRequestDTO);
			PageResponseDto<WorkflowDetail> pageResponeDto = workflowSearchService
					.searchRegistrationDetails(searchRequestDTO.getRequest());
			isTransactionSuccessful = true;
			description.setMessage("workflowSearch api fetch the record successfully");
			updateAudit(description, "", isTransactionSuccessful, user);
			regProcLogger.info("Process the workflowSearch successfully");
			buildResponse(ctx, pageResponeDto, null);
			regProcLogger.debug("WorkflowSearchApi:processURL ended");

		} catch (IOException e) {
			logError(
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e,ctx);
		} catch (WorkFlowSearchException e) {
			logError(e.getErrorCode(), e.getMessage(), e, ctx);
		} catch (Exception e) {
			logError(
					PlatformErrorMessages.RPR_WAA_UNKNOWN_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_WAA_UNKNOWN_EXCEPTION.getMessage(), e, ctx);
		}
	}

	private void logError(
			String errorCode, String errorMessage, Exception e, RoutingContext ctx) {
		if (e != null) {
			regProcLogger.error(
					"Error in  WorkflowSearchApi:processURL  for workflowSearch {} {} {}",
					errorMessage, e.getMessage(), ExceptionUtils.getStackTrace(e));
		}

		List<ErrorDTO> errors = new ArrayList<ErrorDTO>();
		ErrorDTO errorDTO = new ErrorDTO();
		errorDTO.setErrorCode(errorCode);
		errorDTO.setMessage(errorMessage);
		errors.add(errorDTO);
		buildResponse(ctx, null, errors);
	}

	private void failure(RoutingContext routingContext) {
		this.setResponse(routingContext, routingContext.failure().getMessage());
	}
	@Override
	public MessageDTO process(MessageDTO object) {
		return null;
	}

	private void buildResponse(RoutingContext routingContext, PageResponseDto<WorkflowDetail> wfs,
			List<ErrorDTO> errors) {

		WorkflowSearchResponseDTO workflowSearchResponseDTO = new WorkflowSearchResponseDTO();
		workflowSearchResponseDTO.setId(id);
		workflowSearchResponseDTO.setVersion(version);
		workflowSearchResponseDTO.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
		if (wfs == null) {
			workflowSearchResponseDTO.setErrors(errors);
		} else {
			workflowSearchResponseDTO.setResponse(wfs);
		}
		this.setResponse(routingContext, workflowSearchResponseDTO);
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

	private String getUser(RoutingContext ctx) {
		String user = "";
		if (Objects.nonNull(ctx.user()) && Objects.nonNull(ctx.user().principal()))
			user = ctx.user().principal().getString("username");
		return user;
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

}
