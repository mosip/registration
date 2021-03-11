package io.mosip.registration.processor.notification.service.dto;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * This class contains parameters for communication between MOSIP stages.
 *
 * @author Pranav Kumar
 * @since 0.0.1
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
	
	private String resultType;	
	
	private String workflowType;
	
	private String errorCode;

}
