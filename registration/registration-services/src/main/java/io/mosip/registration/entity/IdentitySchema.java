package io.mosip.registration.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

/**
 * The Class IdentitySchema.
 */
@Data
@Entity
public class IdentitySchema implements Serializable {

	private static final long serialVersionUID = 933141370563068589L;

	/** The id. */
	@Id
	@Column(name = "id")
	private String id;

	/** The schema version. */
	@Column(name = "schema_version")
	private double schemaVersion;

	/** The id attr json. */
	@Column(name = "id_attr_json")
	private String idAttrJson;

	/** The schema json. */
	@Column(name = "schema_json")
	private String schemaJson;

	/** The effective from. */
	@Column(name = "effective_from")
	private String effectiveFrom;

	/** The is active. */
	@Column(name = "is_active")
	private boolean isActive;

}
