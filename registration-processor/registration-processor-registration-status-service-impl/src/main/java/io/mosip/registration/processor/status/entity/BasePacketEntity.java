package io.mosip.registration.processor.status.entity;
	
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

/**
 * The Class BasePacketEntity.
 *
 * @author Girish Yarru
 * @param <C> the generic type
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class BasePacketEntity<C> {

	/** The id. */
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
