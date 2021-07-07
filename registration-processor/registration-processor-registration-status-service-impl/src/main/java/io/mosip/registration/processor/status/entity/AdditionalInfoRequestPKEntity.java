package io.mosip.registration.processor.status.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AdditionalInfoRequestPKEntity implements Serializable {
	// default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;

	@Column(name = "workflow_instance_id")
	private String workflowInstanceId;

	@Column(name = "additional_info_req_id")
	private String additionalInfoReqId;

	public AdditionalInfoRequestPKEntity() {
	}

	public String getAdditionalInfoReqId() {
		return additionalInfoReqId;
	}

	public void setAdditionalInfoReqId(String additionalInfoReqId) {
		this.additionalInfoReqId = additionalInfoReqId;
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
		AdditionalInfoRequestPKEntity that = (AdditionalInfoRequestPKEntity) o;
		return workflowInstanceId.equals(that.workflowInstanceId) &&
				additionalInfoReqId.equals(that.additionalInfoReqId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(workflowInstanceId, additionalInfoReqId);
	}
}