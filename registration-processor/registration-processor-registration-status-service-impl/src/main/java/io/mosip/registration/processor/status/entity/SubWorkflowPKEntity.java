package io.mosip.registration.processor.status.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SubWorkflowPKEntity implements Serializable {
	// default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;

	@Column(name = "reg_id")
	private String regId;

	@Column(name = "additional_info_req_id")
	private String additionalInfoReqId;

	public SubWorkflowPKEntity() {
	}

	public String getRegId() {
		return regId;
	}

	public void setRegId(String regId) {
		this.regId = regId;
	}

	public String getAdditionalInfoReqId() {
		return additionalInfoReqId;
	}

	public void setAdditionalInfoReqId(String additionalInfoReqId) {
		this.additionalInfoReqId = additionalInfoReqId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SubWorkflowPKEntity that = (SubWorkflowPKEntity) o;
		return regId.equals(that.regId) &&
				additionalInfoReqId.equals(that.additionalInfoReqId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(regId, additionalInfoReqId);
	}
} 
