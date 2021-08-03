package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class RegBioRefPKEntity implements Serializable {

	@Column(name = "workflow_instance_id")
	private String workflowInstanceId;

	@Column(name = "bio_ref_id")
	private String bioRefId;

	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	public void setWorkflowInstanceId(String workflowInstanceId) {
		this.workflowInstanceId = workflowInstanceId;
	}

	public String getBioRefId() {
		return this.bioRefId;
	}

	public void setBioRefId(String bioRefId) {
		this.bioRefId = bioRefId;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}
