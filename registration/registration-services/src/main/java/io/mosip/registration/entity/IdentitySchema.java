package io.mosip.registration.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * The Class IdentitySchema.
 */
@Data
@Entity
@Table(name = "identity_schema", schema = "reg")
public class IdentitySchema implements Serializable {

	private static final long serialVersionUID = 933141370563068589L;

	/** The id. */
	@Id
	@Column(name = "id")
	private String id;

	/** The schema version. */
	@Column(name = "id_version")
	private double idVersion;

	/** file to store UI json and schema json */
	@Column(name = "file_name")
	private String fileName;

	/** MD5 hash of the file */
	@Column(name = "file_hash")
	private String fileHash;

	/** The effective from. */
	@Column(name = "effective_from")
	private Timestamp effectiveFrom;	

}
