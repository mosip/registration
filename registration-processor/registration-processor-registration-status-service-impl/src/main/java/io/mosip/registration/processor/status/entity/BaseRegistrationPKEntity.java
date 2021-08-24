package io.mosip.registration.processor.status.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;


@Embeddable
public class BaseRegistrationPKEntity implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "workflow_instance_id")
	protected String workflowInstanceId;
	
	public  BaseRegistrationPKEntity() {
		super();
	}

	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	public void setWorkflowInstanceId(String workflowInstanceId) {
		this.workflowInstanceId = workflowInstanceId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseRegistrationPKEntity that = (BaseRegistrationPKEntity) o;
		return Objects.equals(workflowInstanceId, that.workflowInstanceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(workflowInstanceId);
	}
}
