package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * The primary key class for the reg_manual_verification database table.
 * 
 * @author Shuchita
 * @since 0.0.1
 */
@Embeddable
public class ManualVerificationPKEntity implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The matched ref id. */
	@Column(name = "matched_ref_id")
	private String matchedRefId;

	/** The matched ref type. */
	@Column(name = "matched_ref_type")
	private String matchedRefType;

	@Column(name = "workflow_instance_id")
	private String workflowInstanceId;

	/**
	 * Gets the matched ref id.
	 *
	 * @return the matchedRefId
	 */
	public String getMatchedRefId() {
		return matchedRefId;
	}

	/**
	 * Sets the matched ref id.
	 *
	 * @param matchedRefId            the matchedRefId to set
	 */
	public void setMatchedRefId(String matchedRefId) {
		this.matchedRefId = matchedRefId;
	}

	/**
	 * Gets the matched ref type.
	 *
	 * @return the matchedRefType
	 */
	public String getMatchedRefType() {
		return matchedRefType;
	}

	/**
	 * Sets the matched ref type.
	 *
	 * @param matchedRefType            the matchedRefType to set
	 */
	public void setMatchedRefType(String matchedRefType) {
		this.matchedRefType = matchedRefType;
	}

	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	public void setWorkflowInstanceId(String workflowInstanceId) {
		this.workflowInstanceId = workflowInstanceId;
	}

	/**
	 * Gets the serialversionuid.
	 *
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}