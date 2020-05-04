package io.mosip.registration.repositories;

import java.sql.Timestamp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.mosip.registration.entity.IdentitySchema;

/**
 * The Interface IdentitySchemaRepository.
 */
public interface IdentitySchemaRepository extends JpaRepository<IdentitySchema, String> {

	@Query(value = "SELECT max(id_version) FROM reg.identity_schema", nativeQuery = true)
	Double findLatestEffectiveIdVersion(Timestamp effectiveFrom);
	
	@Query(value = "SELECT * FROM reg.identity_schema WHERE id_version = "
			+ "( SELECT max(id_version) FROM reg.identity_schema )", nativeQuery = true)
	IdentitySchema findLatestEffectiveIdentitySchema(Timestamp effectiveFrom);
		
	IdentitySchema findByIdVersion(double idVersion);

}
