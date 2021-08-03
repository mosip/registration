package io.mosip.registration.processor.core.workflow.dto;

import java.io.Serializable;

import lombok.Data;
/**
 * The Class WorkflowPausedForAdditionalInfoEventDTO.
 */
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

	/** The workflow type. */
	private String workflowType;


	/** The additional info process. */
	private String additionalInfoProcess;

	/** The additional info request id. */
	private String additionalInfoRequestId;

}
