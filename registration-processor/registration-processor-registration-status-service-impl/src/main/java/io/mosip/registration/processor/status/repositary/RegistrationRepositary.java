/*
 * 
 */
package io.mosip.registration.processor.status.repositary;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.processor.status.entity.BaseRegistrationEntity;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;

/**
 * The Interface RegistrationRepositary.
 *
 * @param <T>
 *            the generic type
 * @param <E>
 *            the element type
 */
@Repository
public interface RegistrationRepositary<T extends BaseRegistrationEntity, E> extends BaseRepository<T, E> {
	
	@Query("SELECT registration.id FROM RegistrationStatusEntity registration WHERE registration.id in :regIds and registration.latestTransactionStatusCode =:statusCode")
	public List<String> getProcessedOrProcessingRegIds(@Param("regIds") List<String> regIds,
			@Param("statusCode") String statusCode);

	@Query("SELECT registration.id FROM RegistrationStatusEntity registration WHERE registration.id in :regIds and registration.statusCode !=:statusCode1 and registration.statusCode !=:statusCode2")
	public List<String> getWithoutStatusCodes(@Param("regIds") List<String> regIds,
													   @Param("statusCode1") String statusCode1, @Param("statusCode2") String statusCode2);
	
}
