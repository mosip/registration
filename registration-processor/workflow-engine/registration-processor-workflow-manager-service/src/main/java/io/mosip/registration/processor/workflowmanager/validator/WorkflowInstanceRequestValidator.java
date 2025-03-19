package io.mosip.registration.processor.workflowmanager.validator;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.exception.WorkflowInstanceRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.WorkflowActionDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceDTO;


/**
 * The Class WorkflowInstanceRequestValidator.
 */
@Component
public class WorkflowInstanceRequestValidator {

    /** The Constant VER. */
    private static final String VER = "version";

    /** The Constant TIMESTAMP. */
    private static final String TIMESTAMP = "requesttime";

    /** The Constant ID_FIELD. */
    private static final String ID_FIELD = "id";

    /** The Constant WORKFLOW_INSTANCE_ID. */
    private static final String WORKFLOW_INSTANCE_ID = "mosip.regproc.workflow-manager.instance.api-id";

    /** The Constant WORKFLOW_INSTANCE_VERSION. */
    private static final String WORKFLOW_INSTANCE_VERSION = "mosip.regproc.workflow-manager.instance.version";

    Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowInstanceRequestValidator.class);

    /** The env. */
    @Autowired
    private Environment env;

    /**
     * Validate.
     *
     * @param workflowInstanceDTO the workflow instance DTO
     * @param errors            the errors
     * @return true, if successful
     * @throws WorkflowInstanceRequestValidationException
     */
    public void validate(WorkflowInstanceDTO workflowInstanceDTO)
            throws WorkflowInstanceRequestValidationException {
        regProcLogger.debug("WorkflowInstanceRequestValidator  validate entry");

        validateId(workflowInstanceDTO.getId());
        validateVersion(workflowInstanceDTO.getVersion());
        validateReqTime(workflowInstanceDTO.getRequesttime());

        regProcLogger.debug("WorkflowInstanceRequestValidator  validate exit");

    }

    /**
     * Validate version.
     *
     * @param version the version
     * @param errors  the errors
     * @return true, if successful
     * @throws WorkflowInstanceRequestValidationException
     */
    private void validateVersion(String version)
            throws WorkflowInstanceRequestValidationException {
        if (Objects.isNull(version)) {
            throw new WorkflowInstanceRequestValidationException(
                    PlatformErrorMessages.RPR_WIN_MISSING_INPUT_PARAMETER.getCode(),
                    String.format(PlatformErrorMessages.RPR_WIN_MISSING_INPUT_PARAMETER.getMessage(), VER));


        } else if (!version.equalsIgnoreCase(env.getProperty(WORKFLOW_INSTANCE_VERSION))) {
            throw new WorkflowInstanceRequestValidationException(
                    PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getCode(),
                    String.format(PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getMessage(), VER));


        }
    }

    /**
     * Validate id.
     *
     * @param id     the id
     * @param errors the errors
     * @return true, if successful
     * @throws WorkflowInstanceRequestValidationException
     */
    private void validateId(String id) throws WorkflowInstanceRequestValidationException {
        if (Objects.isNull(id)) {
            throw new WorkflowInstanceRequestValidationException(
                    PlatformErrorMessages.RPR_WIN_MISSING_INPUT_PARAMETER.getCode(),
                    String.format(PlatformErrorMessages.RPR_WIN_MISSING_INPUT_PARAMETER.getMessage(), ID_FIELD));

        } else if (!id.equalsIgnoreCase(env.getProperty(WORKFLOW_INSTANCE_ID))) {
            throw new WorkflowInstanceRequestValidationException(
                    PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getCode(),
                    String.format(PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getMessage(), ID_FIELD));

        }
    }

    /**
     * Validate req time.
     *
     * @param requesttime the requesttime
     * @param errors      the errors
     * @return true, if successful
     * @throws WorkflowInstanceRequestValidationException
     */
    private void validateReqTime(String requesttime)
            throws WorkflowInstanceRequestValidationException {

        if (Objects.isNull(requesttime)) {
            throw new WorkflowInstanceRequestValidationException(
                    PlatformErrorMessages.RPR_WIN_MISSING_INPUT_PARAMETER.getCode(),
                    String.format(PlatformErrorMessages.RPR_WIN_MISSING_INPUT_PARAMETER.getMessage(), TIMESTAMP));

        } else {
            try {
                DateUtils.parseToLocalDateTime(requesttime);


            } catch (Exception e) {
                regProcLogger.error("Exception while parsing date {}", ExceptionUtils.getStackTrace(e));
                throw new WorkflowInstanceRequestValidationException(
                        PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getCode(),
                        String.format(PlatformErrorMessages.RPR_WIN_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));

            }
        }

    }
}