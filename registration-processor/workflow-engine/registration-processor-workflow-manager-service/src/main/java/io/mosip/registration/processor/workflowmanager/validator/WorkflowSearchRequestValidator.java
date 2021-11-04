package io.mosip.registration.processor.workflowmanager.validator;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.WorkflowActionRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.FilterInfo;
import io.mosip.registration.processor.core.workflow.dto.PaginationInfo;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.core.workflow.dto.WorkflowSearchRequestDTO;

@Component
public class WorkflowSearchRequestValidator {

	/** The Constant VER. */
	private static final String VER = "version";

	/** The Constant TIMESTAMP. */
	private static final String TIMESTAMP = "requesttime";

	/** The Constant ID_FIELD. */
	private static final String ID_FIELD = "id";

	/** The Constant WORKFLOW_ACTION_ID. */
	private static final String WORKFLOW_SEARCH_ID = "mosip.regproc.workflow-manager.search.api-id";

	/** The Constant WORKFLOW_ACTION_VERSION. */
	private static final String WORKFLOW_SEARCH_VERSION = "mosip.regproc.workflow-manager.search.version";

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
	public void validate(WorkflowSearchRequestDTO workflowSearchDTO) throws WorkFlowSearchException {
		regProcLogger.debug("WorkflowActionRequestValidator  validate entry");
		validateId(workflowSearchDTO.getId());
		validateVersion(workflowSearchDTO.getVersion());
		validateReqTime(workflowSearchDTO.getRequesttime());
		validateFilter(workflowSearchDTO.getRequest().getFilters());
		validatePagination(workflowSearchDTO.getRequest().getPagination());
		validateSort(workflowSearchDTO.getRequest().getSort());

		regProcLogger.debug("WorkflowSearchRequestValidator  validate exit");

	}

	/**
	 * Validate version.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws WorkFlowSearchException
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validateVersion(String version) throws WorkFlowSearchException {
		if (Objects.isNull(version)) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), VER));
		} else if (!version.equalsIgnoreCase(env.getProperty(WORKFLOW_SEARCH_VERSION))) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), VER));
		}
	}

	/**
	 * Validate pagination.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validatePagination(PaginationInfo pagination)
			throws WorkFlowSearchException {
		if (Objects.isNull(pagination)) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), "pagination"));


		} else if (pagination.getPageFetch() == 0) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), "pagination"));


		}

	}

	/**
	 * Validate sortInfo.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validateSort(SortInfo sortInfo) throws WorkFlowSearchException {
		if (!Objects.isNull(sortInfo)) {
			if (sortInfo.getSortType().equals("asc") || sortInfo.getSortType().equals("desc")) {
				validateSortField(sortInfo);
			} else {
				throw new WorkFlowSearchException(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), "sort"));
			}
		}
	}

	/**
	 * Validate sortInfo.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 */
	private void validateSortField(SortInfo sortInfo) throws WorkFlowSearchException {
		if (sortInfo.getSortField() == null || sortInfo.getSortField().isBlank()) {
			throw new WorkFlowSearchException(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), "sortField"));

		} else if (sortInfo.getSortField().equals("createDateTime") || sortInfo.getSortField().equals("updateDateTime")
				|| sortInfo.getSortField().equals("resumeTimestamp")) {
			
		} else {
			throw new WorkFlowSearchException(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), "sortField"));
		}

	}


	/**
	 * Validate filter.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws WorkflowActionRequestValidationException
	 * 
	 */
	private void validateFilter(List<FilterInfo> filterInfos) throws WorkFlowSearchException {
		if (Objects.isNull(filterInfos)) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), "filter"));

		}
		for (FilterInfo filter : filterInfos) {
			if (filter.getColumnName().equals("workflowType") || filter.getColumnName().equals("statusCode")
					|| filter.getColumnName().equals("regId")) {

			} else {
				throw new WorkFlowSearchException(
						PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER.getMessage(), "filter"));
			}
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
	private void validateId(String id) throws WorkFlowSearchException {
		if (Objects.isNull(id)) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getMessage(), ID_FIELD));

		} else if (!id.equalsIgnoreCase(env.getProperty(WORKFLOW_SEARCH_ID))) {
			throw new WorkFlowSearchException(
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
	private void validateReqTime(String requesttime) throws WorkFlowSearchException {

		if (Objects.isNull(requesttime)) {
			throw new WorkFlowSearchException(
					PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_WAA_MISSING_INPUT_PARAMETER.getMessage(), TIMESTAMP));

		} else {
			try {
				DateUtils.parseToLocalDateTime(requesttime);

			} catch (Exception e) {
				regProcLogger.error("Exception while parsing date {}", ExceptionUtils.getStackTrace(e));
				throw new WorkFlowSearchException(
						PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_WAA_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));

			}
		}

	}
}
