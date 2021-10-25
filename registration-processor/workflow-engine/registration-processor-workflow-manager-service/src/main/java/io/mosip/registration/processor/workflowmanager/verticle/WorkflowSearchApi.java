package io.mosip.registration.processor.workflowmanager.verticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.mosip.registration.processor.core.util.JsonUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

import io.mosip.kernel.core.exception.IOException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
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
import io.mosip.registration.processor.core.workflow.dto.PageResponseDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowDetail;
import io.mosip.registration.processor.core.workflow.dto.WorkflowSearchRequestDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowSearchResponseDTO;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.workflowmanager.service.WorkflowSearchService;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowSearchRequestValidator;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WorkflowSearchApi extends MosipRouter {


	@Value("${mosip.registration.processor.datetime.pattern}")
	private String dateTimePattern;

	@Value("${mosip.regproc.workflow-manager.search.api-id}")
	private String id;

	@Value("${mosip.regproc.workflow-manager.search.version}")
	private String version;

	/**
	 * The context path.
	 */
	@Value("${server.servlet.path}")
	private String contextPath;


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
		String user = null;
		PageResponseDTO<WorkflowDetail> pageResponseDto = new PageResponseDTO<>();

		try {
			JsonObject obj = ctx.getBodyAsJson();
			user = getUser(ctx);
			WorkflowSearchRequestDTO searchRequestDTO = (WorkflowSearchRequestDTO) JsonUtil
					.readValueWithUnknownProperties(obj.toString(), WorkflowSearchRequestDTO.class);
			regProcLogger.debug("WorkflowActionApi:processURL called");
			workflowSearchRequestValidator.validate(searchRequestDTO);
			Page<WorkflowDetail> pageDtos = workflowSearchService
					.searchRegistrationDetails(searchRequestDTO.getRequest());
			pageResponseDto = buildPageReponse(pageDtos);
			pageResponseDto.setData(pageDtos.getContent());
			isTransactionSuccessful = true;
			description.setCode(PlatformSuccessMessages.RPR_WORKFLOW_SEARCH_API_SUCCESS.getCode());
			description.setMessage(PlatformSuccessMessages.RPR_WORKFLOW_SEARCH_API_SUCCESS.getMessage());
			updateAudit(description, "", isTransactionSuccessful, user);
			regProcLogger.info("Process the workflowSearch successfully");
			buildResponse(ctx, pageResponseDto, null);
			regProcLogger.debug("WorkflowSearchApi:processURL ended");

		} catch (WorkFlowSearchException e) {
			description.setMessage(e.getMessage());
			description.setCode(e.getErrorCode());
			updateAudit(description, "", isTransactionSuccessful, user);
			logError(e.getErrorCode(), e.getMessage(), e, ctx);
		} catch (Exception e) {
			description.setMessage(PlatformErrorMessages.RPR_WAA_UNKNOWN_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_WAA_UNKNOWN_EXCEPTION.getCode());
			updateAudit(description, "", isTransactionSuccessful, user);
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

	private void buildResponse(RoutingContext routingContext, PageResponseDTO<WorkflowDetail> wfs,
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

	public static <T, D> PageResponseDTO<D> buildPageReponse(Page<T> page) {
		PageResponseDTO<D> pageResponse = new PageResponseDTO<>();
		if (page != null) {
			long totalItem = page.getTotalElements();
			int pageSize = page.getSize();
			int start = (page.getNumber() * pageSize) + 1;
			pageResponse.setFromRecord(start);
			pageResponse.setToRecord((long) (start - 1) + page.getNumberOfElements());
			pageResponse.setTotalRecord(totalItem);
		}
		return pageResponse;
	}

	public void setResponse(RoutingContext ctx, Object object) {
		ctx.response().putHeader("content-type", "text/plain").putHeader("Access-Control-Allow-Origin", "*")
				.putHeader("Access-Control-Allow-Methods", "GET, POST").setStatusCode(200)
				.end(Json.encodePrettily(object));
	};

	public void setApiRoute(Router router) {
		setRoute(router);
		routes(this);
	}

}
