package io.mosip.registration.processor.packet.storage.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * The primary key class for the reg_verification database table.
 * 
 * @author Monobikash
 * @since 1.2.0
 */
@Embeddable
public class VerificationPKEntity implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	@Column(name = "workflow_instance_id")
	private String workflowInstanceId;

	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	public void setWorkflowInstanceId(String workflowInstanceId) {
		this.workflowInstanceId = workflowInstanceId;
	}

}