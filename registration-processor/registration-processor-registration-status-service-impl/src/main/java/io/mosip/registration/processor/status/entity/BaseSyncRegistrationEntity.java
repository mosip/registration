package io.mosip.registration.processor.status.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * The Class BaseSyncRegistrationEntity.
 *
 * @author Mamta Andhe
 */
// Common Entity where Transaction Enity and
// SyncRegistrationEntity extends this. This is created to implement common
// repository(RegistrationRepository)

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseSyncRegistrationEntity {

	/**
	 * Instantiates a new base registration entity.
	 */
	public BaseSyncRegistrationEntity() {
		super();
	}

	/** The id. */
	@Column(name = "workflow_instance_id", nullable = false)
	@Id
	protected String workflowInstanceId;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	/**
	 * Sets the id.
	 *
	 * @param baseId the new id
	 */
	public void setWorkflowInstanceId(String baseId) {
		this.workflowInstanceId = baseId;
	}

}
