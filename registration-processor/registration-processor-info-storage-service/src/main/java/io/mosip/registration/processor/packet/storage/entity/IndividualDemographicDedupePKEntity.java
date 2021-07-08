package io.mosip.registration.processor.packet.storage.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * The Class IndividualDemographicDedupePKEntity.
 *
 * @author Girish Yarru
 */
@Embeddable
public class IndividualDemographicDedupePKEntity implements Serializable {
	
	/** The Constant serialVersionUID. */
	// default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;

	/** The lang code. */
	@Column(name = "lang_code", nullable = false)
	private String langCode;

	@Column(name = "workflow_instance_id")
	private String workflowInstanceId;

	/**
	 * Instantiates a new individual demographic dedupe PK entity.
	 */
	public IndividualDemographicDedupePKEntity() {
		super();
	}

	/**
	 * Gets the lang code.
	 *
	 * @return the lang code
	 */
	public String getLangCode() {
		return this.langCode;
	}

	/**
	 * Sets the lang code.
	 *
	 * @param langCode the new lang code
	 */
	public void setLangCode(String langCode) {
		this.langCode = langCode;
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
		IndividualDemographicDedupePKEntity that = (IndividualDemographicDedupePKEntity) o;
		return Objects.equals(langCode, that.langCode) &&
				Objects.equals(workflowInstanceId, that.workflowInstanceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(langCode, workflowInstanceId);
	}
}