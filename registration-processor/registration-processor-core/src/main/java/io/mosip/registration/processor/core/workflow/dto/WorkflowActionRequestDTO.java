package io.mosip.registration.processor.core.workflow.dto;

import java.util.List;

import lombok.Data;
@Data
public class WorkflowActionRequestDTO {

	private List<String> workflowIds;

	private String workflowAction;

}
