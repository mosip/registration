package io.mosip.registration.processor.status.repositary;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.processor.status.entity.BasePacketEntity;
import io.mosip.registration.processor.status.entity.AdditionalInfoRequestEntity;

@Repository
public interface BaseRegProcRepository<T extends BasePacketEntity, E> extends BaseRepository<T, E> {

    @Query("SELECT additionalInfoRequest FROM AdditionalInfoRequestEntity additionalInfoRequest WHERE additionalInfoRequest.id.additionalInfoReqId =:additionalInfoReqId")
    public List<AdditionalInfoRequestEntity> getAdditionalInfoRequestByReqId(@Param("additionalInfoReqId") String additionalInfoReqId);

    @Query("SELECT additionalInfoRequest FROM AdditionalInfoRequestEntity additionalInfoRequest WHERE additionalInfoRequest.regId =:regId AND additionalInfoRequest.additionalInfoProcess =:additionalInfoProcess AND additionalInfoRequest.additionalInfoIteration=:additionalInfoIteration")
    public List<AdditionalInfoRequestEntity> getAdditionalInfoRequestByRegIdAndProcessAndIteration(@Param("regId") String regId, @Param("additionalInfoProcess") String additionalInfoProcess, @Param("additionalInfoIteration")  int additionalInfoIteration);

	@Query("SELECT additionalInfoRequest FROM AdditionalInfoRequestEntity additionalInfoRequest WHERE additionalInfoRequest.regId =:regId AND additionalInfoRequest.additionalInfoProcess =:additionalInfoProcess order by additionalInfoRequest.additionalInfoIteration desc")
	public List<AdditionalInfoRequestEntity> getAdditionalInfoRequestByRegIdAndProcess(@Param("regId") String regId,
			@Param("additionalInfoProcess") String additionalInfoProcess);

    @Query("SELECT additionalInfoRequest FROM AdditionalInfoRequestEntity additionalInfoRequest WHERE additionalInfoRequest.regId =:regId")
    public List<AdditionalInfoRequestEntity> getAdditionalInfoByRegId(@Param("regId") String regId);
}
