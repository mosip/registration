package io.mosip.registration.processor.status.repositary;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.mosip.registration.processor.status.entity.SaltEntity;


/**
 * The Interface SaltRepository.
 *
 * @author Manoj SP
 */
public interface SaltRepository extends JpaRepository<SaltEntity, Long> {

	/**
	 * Count by id in list of ids.
	 *
	 * @param ids the ids
	 * @return the long
	 */
	Long countByIdIn(List<Long> ids);

	@Query("FROM SaltEntity s where s.id = ?1")
	SaltEntity findSaltById(Long id);
}
