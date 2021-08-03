package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class RegLostUinDetPKEntity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Column(name = "workflow_instance_id")
	private String workflowInstanceId;

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
		RegLostUinDetPKEntity that = (RegLostUinDetPKEntity) o;
		return Objects.equals(workflowInstanceId, that.workflowInstanceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(workflowInstanceId);
	}
}
