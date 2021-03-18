package io.mosip.registration.processor.notification.dto;
import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains parameters for communication sent to WorkflowCompletedEvent.
 *
 * @since 1.1.5
 */
@Setter
@Getter
public class WorkflowCompletedEventDTO implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new message DTO.
	 */
	public WorkflowCompletedEventDTO() {
		super();
	}
	
	/** The registration id. */
	private String instanceId;
	
	private String resultCode;	
	
	private String workflowType;
	
	private String errorCode;

}
