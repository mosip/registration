package io.mosip.registration.repositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.mosip.registration.entity.IdentitySchema;

/**
 * The Interface IdentitySchemaRepository.
 */
public interface IdentitySchemaRepository extends JpaRepository<IdentitySchema, String> {

	@Query(value = "SELECT max(id_version) FROM reg.identity_schema WHERE effective_from <= ?1", nativeQuery = true)
	Double findLatestEffectiveIdVersion(LocalDateTime effectiveFrom);
	
	@Query(value = "SELECT * FROM reg.identity_schema WHERE id_version = "
			+ "( SELECT max(id_version) FROM reg.identity_schema WHERE effective_from <= ?1 )", nativeQuery = true)
	IdentitySchema findLatestEffectiveIdentitySchema(LocalDateTime effectiveFrom);
	
	
	IdentitySchema findByIdVersion(double idVersion);

}
