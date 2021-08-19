package io.mosip.registration.processor.core.workflow.dto;

import lombok.Data;

@Data
public class WorkflowDetail {

	private static final long serialVersionUID = 6705845720255847210L;

	/** The registration id. */
	private String workflowId;

	/** The status code. */
	private String statusCode;

	/** The status comment. */
	private String statusComment;

	/** The created by. */
	private String createdBy;

	/** The create date time. */
	private String createDateTime;

	/** The updated by. */
	private String updatedBy;

	private String workflowType;

	/** The update date time. */
	private String updateDateTime;

	/** The registration stage name. */
	private String currentStageName;

	/** The resume time stamp. */
	private String resumeTimestamp;

	/** The default resume action. */
	private String defaultResumeAction;

	private String pauseRuleIds;
	
	private String lastSuccessStageName;

}
