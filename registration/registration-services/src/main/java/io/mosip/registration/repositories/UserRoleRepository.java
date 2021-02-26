package io.mosip.registration.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.id.UserRoleId;

/**
 * Interface for {@link UserRole}
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 *
 */
public interface UserRoleRepository extends BaseRepository<UserRole, UserRoleId> {

	List<UserRole> findByUserRoleIdUsrId(String usrId);

	@Modifying
	@Query(value = "delete from reg.user_role u where u.usr_id = :userId", nativeQuery = true)
	void delete(@Param("userId") String userId);
	
}
