package io.mosip.registration.processor.workflowmanager.verticle;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.exception.WorkflowInstanceException;
import io.mosip.registration.processor.core.exception.WorkflowInstanceRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceResponse;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceResponseDTO;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowInstanceService;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowInstanceRequestValidator;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WorkflowInstanceApi extends MosipRouter {

    @Value("${mosip.registration.processor.datetime.pattern}")
    private String dateTimePattern;

    @Value("${mosip.regproc.workflow-manager.instance.api-id}")
    private String id;

    @Value("${mosip.regproc.workflow-manager.instance.version}")
    private String version;

    @Autowired
    private WorkflowInstanceRequestValidator validator;

    @Autowired
    private WorkflowInstanceService workflowInstanceService;
    /**
     * The context path.
     */
    @Value("${server.servlet.path}")
    private String contextPath;

    /** The reg proc logger. */
    private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowInstanceApi.class);

    /** The registration status service. */
    @Autowired
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    /** The module id. */
    public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_INSTANCE_API_SUCCESS.getCode();

    /** The module name. */
    public static String MODULE_NAME = ModuleName.WORKFLOW_INSTANCE_API.toString();

    /** The core audit request builder. */
    @Autowired
    AuditLogRequestBuilder auditLogRequestBuilder;

    public void setApiRoute(Router router) {
        setRoute(router);
        routes(this);
    }

    /**
     * contains all the routes in this stage
     *
     * @param router
     */
    private void routes(MosipRouter router) {
        router.post(contextPath + "/workflowinstance");
        router.handler(this::processURL, this::failure);
    }

    /**
     * method to process the context received.
     *
     * @param ctx the ctx
     */
    public void processURL(RoutingContext ctx) {
        String regId = null;
        boolean isTransactionSuccessful = false;
        LogDescription description = new LogDescription();
        String user = null;
        try {
            JsonObject obj = ctx.getBodyAsJson();
            WorkflowInstanceDTO workflowInstanceDTO = JsonUtil.readValueWithUnknownProperties(obj.toString(),
                    WorkflowInstanceDTO.class);
            regId = workflowInstanceDTO.getRequest().getRegistrationId();
            regProcLogger.debug("WorkflowInstanceApi:processURL called for registration id {}", regId);
            validator.validate(workflowInstanceDTO);
            user = getUser(ctx);

            InternalRegistrationStatusDto dto = workflowInstanceService
                    .createWorkflowInstance(workflowInstanceDTO.getRequest(), user);

            isTransactionSuccessful = true;
            description.setMessage(PlatformErrorMessages.RPR_WIN_VALIDATION_SUCCESS.getMessage());
            updateAudit(description, regId, isTransactionSuccessful,
                    user);

            regProcLogger.info("Process the WorkflowInstance successfully  for registration id {}", regId);
            buildResponse(ctx, dto.getWorkflowInstanceId(), null);

            regProcLogger.debug("WorkflowInstanceApi:processURL ended for registration id {}", regId);

        } catch (WorkflowInstanceException e) {
            description.setMessage(e.getMessage());
            description.setCode(e.getErrorCode());
            updateAudit(description, "", isTransactionSuccessful, user);
            logError(regId,e.getErrorCode(), e.getMessage(), e, ctx);

        } catch (WorkflowInstanceRequestValidationException e) {
            description.setMessage(PlatformErrorMessages.RPR_WAA_UNKNOWN_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_WAA_UNKNOWN_EXCEPTION.getCode());
            updateAudit(description, "", isTransactionSuccessful, user);
            logError(regId, e.getErrorCode(), e.getMessage(), e, ctx);
        } catch (Exception e) {
            description.setMessage(PlatformErrorMessages.RPR_WIN_UNKNOWN_EXCEPTION.getMessage());
            description.setCode(PlatformErrorMessages.RPR_WIN_UNKNOWN_EXCEPTION.getCode());
            updateAudit(description, "", isTransactionSuccessful, user);
            logError(regId, PlatformErrorMessages.RPR_WIN_UNKNOWN_EXCEPTION.getCode(),
                    PlatformErrorMessages.RPR_WIN_UNKNOWN_EXCEPTION.getMessage(), e, ctx);
        }
    }

    private String getUser(RoutingContext ctx) {
        String user = "";
        if (Objects.nonNull(ctx.user()) && Objects.nonNull(ctx.user().principal()))
            user = ctx.user().principal().getString("username");
        return user;
    }

    private void logError(String regId, String errorCode, String errorMessage, Exception e, RoutingContext ctx) {
        if (e != null) {
            regProcLogger.error("Error in  WorkflowInstanceApi:processURL  for registration id {} {} {} {}", regId,
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

    public void setResponse(RoutingContext ctx, Object object) {
        ctx.response().putHeader("content-type", "application/json").putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST").setStatusCode(200)
                .end(Json.encodePrettily(object));
    };

    private void buildResponse(RoutingContext routingContext, String workflowInstanceId, List<ErrorDTO> errors) {
        WorkflowInstanceResponseDTO workflowInstanceResponseDTO = new WorkflowInstanceResponseDTO();
        workflowInstanceResponseDTO.setId(id);
        workflowInstanceResponseDTO.setVersion(version);
        workflowInstanceResponseDTO.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
        if (workflowInstanceId == null) {
            workflowInstanceResponseDTO.setErrors(errors);
        } else {
            WorkflowInstanceResponse responseDTO = new WorkflowInstanceResponse();
            responseDTO.setWorkflowInstanceId(workflowInstanceId);
            workflowInstanceResponseDTO.setResponse(responseDTO);
        }
        this.setResponse(routingContext, workflowInstanceResponseDTO);

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

        auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
                moduleId, MODULE_NAME, registrationId, user);
    }

}