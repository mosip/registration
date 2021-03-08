package io.mosip.registration.processor.reprocessor.validator;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
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
	private static final String WORKFLOW_ACTION_ID = "mosip.registration.processor.workflow.action.id";

	/** The Constant WORKFLOW_ACTION_VERSION. */
	private static final String WORKFLOW_ACTION_VERSION = "mosip.registration.processor.workflow.action.version";

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
	 */
	public boolean validate(WorkflowActionDTO workflowActionDTO, List<ErrorDTO> errors) {
		regProcLogger.debug("WorkflowActionRequestValidator  validate entry");
		boolean isValid = false;
		if(validateReqTime(workflowActionDTO.getRequesttime(),errors) && validateId(workflowActionDTO.getId(), errors)
				&& validateVersion(workflowActionDTO.getVersion(), errors)) {
			isValid = true;
		}
		regProcLogger.debug("WorkflowActionRequestValidator  validate exit");
		return isValid;
	}

	/**
	 * Validate version.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 */
	private boolean validateVersion(String version, List<ErrorDTO> errors) {
		if (Objects.isNull(version)) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode());
			error.setMessage(
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), VER));
			errors.add(error);
			return false;

		} else if (!version.equalsIgnoreCase(env.getProperty(WORKFLOW_ACTION_VERSION))) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode());
			error.setMessage(
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), VER));
			errors.add(error);
			return false;

		} else {
			return true;
		}
	}

	/**
	 * Validate id.
	 *
	 * @param id     the id
	 * @param errors the errors
	 * @return true, if successful
	 */
	private boolean validateId(String id, List<ErrorDTO> errors) {
		if (Objects.isNull(id)) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode());
			error.setMessage(
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), ID_FIELD));
			errors.add(error);
			return false;

		} else if (!id.equalsIgnoreCase(env.getProperty(WORKFLOW_ACTION_ID))) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode());
			error.setMessage(
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), ID_FIELD));
			errors.add(error);
			return false;

		} else {
			return true;
		}
	}

	/**
	 * Validate req time.
	 *
	 * @param requesttime the requesttime
	 * @param errors      the errors
	 * @return true, if successful
	 */
	private boolean validateReqTime(String requesttime, List<ErrorDTO> errors) {
		boolean isValid = false;
		if (Objects.isNull(requesttime)) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode());
			error.setMessage(
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), TIMESTAMP));
			errors.add(error);
			return isValid;

		} else {
		try {
				DateUtils.parseToLocalDateTime(requesttime);
				isValid = true;

		} catch (Exception e) {
				regProcLogger.error("Exception while parsing date {}", ExceptionUtils.getStackTrace(e));
				ErrorDTO error = new ErrorDTO();
				error.setErrorCode(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode());
				error.setMessage(
						String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));
				errors.add(error);
		}
		}
		return isValid;
	}
}
