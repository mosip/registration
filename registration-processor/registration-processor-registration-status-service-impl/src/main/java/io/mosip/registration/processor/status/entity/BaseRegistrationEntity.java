package io.mosip.registration.processor.status.entity;


import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

/**
 * The Class BaseRegistrationEntity.
 *
 * @author Girish Yarru
 */
// Common Entity where RegistrationStatusEntity,Transaction Enity and
// SyncRegistrationEntity extends this. This is created to implement common
// repository(RegistrationRepository)



@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class BaseRegistrationEntity<C>  {

	@EmbeddedId
	protected C id;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public C getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(C id) {
		this.id = id;
	}

}
