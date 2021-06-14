package io.mosip.registration.processor.core.workflow.dto;

import java.io.Serializable;

import lombok.Data;


/* (non-Javadoc)
 * @see java.lang.Object#toString()
 */
@Data
public class WorkflowPausedForAdditionalInfoEventDTO implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new workflow paused for additional info event DTO.
	 */
	public WorkflowPausedForAdditionalInfoEventDTO() {
		super();
	}


	/** The instance id. */
	private String instanceId;

	/** The result code. */
	private String resultCode;

	/** The workflow type. */
	private String workflowType;

	/** The sub process. */
	private String subProcess;

	/** The additional info request id. */
	private String additionalInfoRequestId;

}
