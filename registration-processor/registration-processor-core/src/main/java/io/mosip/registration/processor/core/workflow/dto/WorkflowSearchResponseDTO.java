package io.mosip.registration.processor.core.workflow.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowSearchResponseDTO extends BaseRestResponseDTO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private PageResponseDTO<WorkflowDetail> response;

	/** The error. */
	private List<ErrorDTO> errors;
}
