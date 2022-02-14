/*
 * 
 */
package io.mosip.registration.processor.status.repositary;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.processor.status.entity.BaseRegistrationEntity;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
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
	
	@Query("SELECT registration.regId FROM RegistrationStatusEntity registration WHERE registration.regId in :regIds AND registration.latestTransactionStatusCode =:statusCode")
	public List<String> getProcessedOrProcessingRegIds(@Param("regIds") List<String> regIds,
			@Param("statusCode") String statusCode);

	@Query("SELECT registration.regId FROM RegistrationStatusEntity registration WHERE registration.regId in :regIds AND registration.statusCode !=:statusCode1 AND registration.statusCode !=:statusCode2")
	public List<String> getWithoutStatusCodes(@Param("regIds") List<String> regIds,
													   @Param("statusCode1") String statusCode1, @Param("statusCode2") String statusCode2);

	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId = :regId AND registration.isDeleted =false AND registration.isActive=true")
	public List<RegistrationStatusEntity> findByRegId(@Param("regId") String regId);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId IN :regIds AND registration.isDeleted =false AND registration.isActive=true")
	public List<RegistrationStatusEntity> findByRegIds(@Param("regIds") List<String> regIds);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId IN :regIds AND registration.isDeleted =false AND registration.isActive=true order by registration.createDateTime")
	public List<RegistrationStatusEntity> findByRegIdsOrderbyCreatedDateTime(@Param("regIds") List<String> regIds);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.statusCode = :statusCode AND registration.isDeleted =false AND registration.isActive=true")
	public List<RegistrationStatusEntity> findByStatusCode(@Param("statusCode") String statusCode);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId = :regId AND registration.statusCode = :statusCode ")
	public List<RegistrationStatusEntity> findByRegIdANDByStatusCode(@Param("regId") String regId,@Param("statusCode") String statusCode);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.id.workflowInstanceId = :workflowInstanceId AND registration.isDeleted =false AND registration.isActive=true")
	public List<RegistrationStatusEntity> findByWorkflowInstanceId(@Param("workflowInstanceId") String workflowInstanceId);
	
	@Query(value ="SELECT * FROM registration r WHERE r.latest_trn_status_code IN :status AND r.reg_process_retry_count<=:reprocessCount AND r.latest_trn_dtimes <:timeDifference AND r.status_code NOT IN :statusCodes LIMIT :fetchSize ", nativeQuery = true)
	public List<RegistrationStatusEntity> getUnProcessedPackets(@Param("status") List<String> status,@Param("reprocessCount") Integer reprocessCount,@Param("timeDifference") LocalDateTime timeDifference,@Param("statusCodes") List<String> statusCodes,@Param("fetchSize") Integer fetchSize );
	
	@Query("SELECT COUNT(*) FROM RegistrationStatusEntity registration WHERE registration.latestTransactionStatusCode IN :status AND registration.regProcessRetryCount<=:reprocessCount AND registration.latestTransactionTimes<:timeDifference AND registration.statusCode  NOT IN :statusCodes ")
	public int getUnProcessedPacketsCount(@Param("status") List<String> status,@Param("reprocessCount") Integer reprocessCount,@Param("timeDifference") LocalDateTime timeDifference,@Param("statusCodes") List<String> statusCodes );
	
	@Query(value ="SELECT * FROM registration r WHERE r.status_code IN :statusCodes AND r.resume_timestamp < now() AND r.default_resume_action is NOT NULL order by r.upd_dtimes LIMIT :fetchSize ", nativeQuery = true)
	public List<RegistrationStatusEntity> getActionablePausedPackets(@Param("statusCodes") List<String> statusCodes,@Param("fetchSize") Integer fetchSize);

	@Query(value ="SELECT * FROM registration r WHERE r.status_code =:statusCode  order by r.upd_dtimes LIMIT :fetchSize ", nativeQuery = true)
	public List<RegistrationStatusEntity> getResumablePackets(@Param("statusCode") String statusCode,@Param("fetchSize") Integer fetchSize);
}

