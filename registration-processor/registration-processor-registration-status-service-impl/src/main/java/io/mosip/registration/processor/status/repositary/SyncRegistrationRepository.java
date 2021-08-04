package io.mosip.registration.processor.status.repositary;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.processor.status.entity.BaseSyncRegistrationEntity;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;

@Repository
public interface SyncRegistrationRepository<T extends BaseSyncRegistrationEntity, E> extends BaseRepository<T, E> {

	@Query("SELECT registrationList FROM SyncRegistrationEntity registrationList WHERE registrationList.registrationId =:regId and registrationList.registrationType =:regType")
	public List<SyncRegistrationEntity> getSyncRecordsByRegIdAndRegType(@Param("regId") String regId,
			@Param("regType") String regType);

}