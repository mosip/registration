package io.mosip.registration.processor.reprocessor.validator;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.WorkflowActionDTO;


/**
 * The Class WorkflowActionRequestValidator.
 */
@Component
public class WorkflowActionRequestValidator {

	/** The Constant VER. */
	private static final String VER = "version";
	
	/** The Constant TIMESTAMP. */
	private static final String TIMESTAMP = "requesttime";

	/** The Constant ID_FIELD. */
	private static final String ID_FIELD = "id";

	/** The Constant WORKFLOW_ACTION_ID. */
	private static final String WORKFLOW_ACTION_ID = "mosip.regproc.workflow.action.api-id";

	/** The Constant WORKFLOW_ACTION_VERSION. */
	private static final String WORKFLOW_ACTION_VERSION = "mosip.regproc.workflow.action.version";

	Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowActionRequestValidator.class);

	/** The env. */
	@Autowired
	private Environment env;

	/**
	 * Validate.
	 *
	 * @param workflowActionDTO the workflow action DTO
	 * @param errors            the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	public void validate(WorkflowActionDTO workflowActionDTO)
			throws WorkflowActionRequestValidationException {
		regProcLogger.debug("WorkflowActionRequestValidator  validate entry");

		validateId(workflowActionDTO.getId());
		validateVersion(workflowActionDTO.getVersion());
		validateReqTime(workflowActionDTO.getRequesttime());

		regProcLogger.debug("WorkflowActionRequestValidator  validate exit");

	}

	/**
	 * Validate version.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validateVersion(String version)
			throws WorkflowActionRequestValidationException {
		if (Objects.isNull(version)) {
			throw new WorkflowActionRequestValidationException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), VER));


		} else if (!version.equalsIgnoreCase(env.getProperty(WORKFLOW_ACTION_VERSION))) {
			throw new WorkflowActionRequestValidationException(
					PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), VER));


		}
	}

	/**
	 * Validate id.
	 *
	 * @param id     the id
	 * @param errors the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validateId(String id) throws WorkflowActionRequestValidationException {
		if (Objects.isNull(id)) {
			throw new WorkflowActionRequestValidationException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getMessage(), ID_FIELD));

		} else if (!id.equalsIgnoreCase(env.getProperty(WORKFLOW_ACTION_ID))) {
			throw new WorkflowActionRequestValidationException(
					PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), ID_FIELD));

		}
	}

	/**
	 * Validate req time.
	 *
	 * @param requesttime the requesttime
	 * @param errors      the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validateReqTime(String requesttime)
			throws WorkflowActionRequestValidationException {

		if (Objects.isNull(requesttime)) {
			throw new WorkflowActionRequestValidationException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getMessage(), TIMESTAMP));

		} else {
		try {
				DateUtils.parseToLocalDateTime(requesttime);


		} catch (Exception e) {
				regProcLogger.error("Exception while parsing date {}", ExceptionUtils.getStackTrace(e));
				throw new WorkflowActionRequestValidationException(
						PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));

		}
		}

	}
}
