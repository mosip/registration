package io.mosip.registration.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import io.mosip.registration.entity.IdentitySchema;

/**
 * The Interface IdentitySchemaRepository.
 */
public interface IdentitySchemaRepository extends JpaRepository<IdentitySchema, String> {

	/**
	 * Find by schema version and is active.
	 *
	 * @param name the name
	 * @param active the active
	 * @return the identity schema
	 */
	IdentitySchema findBySchemaVersionAndIsActive(double name, boolean active);

}
