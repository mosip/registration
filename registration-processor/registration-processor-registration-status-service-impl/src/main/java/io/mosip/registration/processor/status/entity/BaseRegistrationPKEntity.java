package io.mosip.registration.processor.status.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;


@Embeddable
public class BaseRegistrationPKEntity implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The id. */
	@Column
	private String id;
	
	/** The registration type. */
	@Column(name="reg_type")
	private String registrationType;

	@Column
	private Integer iteration=1;

	@Column(name = "workflow_instance_id")
	protected String workflowInstanceId;
	
	public  BaseRegistrationPKEntity() {
		
	}
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param baseId the new id
	 */
	public void setId(String baseId) {
		this.id = baseId;
	}

	/**
	 * Gets the registration type.
	 *
	 * @return the registration type
	 */
	public String getRegistrationType() {
		return registrationType;
	}

	/**
	 * Sets the registration type.
	 *
	 * @param registrationType
	 *            the new registration type
	 */
	public void setRegistrationType(String registrationType) {
		this.registrationType = registrationType;
	}
	
	public Integer getIteration() {
		return iteration;
	}
	public void setIteration(Integer iteration) {
		this.iteration = iteration;
	}

	public String getWorkflowInstanceId() {
		return workflowInstanceId;
	}

	public void setWorkflowInstanceId(String workflowInstanceId) {
		this.workflowInstanceId = workflowInstanceId;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BaseRegistrationPKEntity)) {
			return false;
		}
		BaseRegistrationPKEntity castOther = (BaseRegistrationPKEntity) other;
		return this.id.equals(castOther.id) && this.registrationType.equals(castOther.registrationType) && this.iteration.equals(castOther.iteration);
	}
	
	public int hashCode() {
		final int prime = 31;
		int hash = 17;
		hash = hash * prime + this.id.hashCode();
		hash = hash * prime + this.registrationType.hashCode();
		hash = hash * prime + this.iteration.hashCode();
		return hash;
	}
	
}
