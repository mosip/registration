package io.mosip.registration.processor.core.workflow.dto;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class WorkflowSearchRequestDTO extends BaseRestRequestDTO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private SearchInfo request;
}
