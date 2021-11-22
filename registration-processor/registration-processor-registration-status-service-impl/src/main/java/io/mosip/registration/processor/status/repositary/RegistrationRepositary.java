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
	
	@Query("SELECT registration.regId FROM RegistrationStatusEntity registration WHERE registration.regId in :regIds and registration.latestTransactionStatusCode =:statusCode")
	public List<String> getProcessedOrProcessingRegIds(@Param("regIds") List<String> regIds,
			@Param("statusCode") String statusCode);

	@Query("SELECT registration.regId FROM RegistrationStatusEntity registration WHERE registration.regId in :regIds and registration.statusCode !=:statusCode1 and registration.statusCode !=:statusCode2")
	public List<String> getWithoutStatusCodes(@Param("regIds") List<String> regIds,
													   @Param("statusCode1") String statusCode1, @Param("statusCode2") String statusCode2);

	@Query("SELECT COUNT(registration.id.workflowInstanceId)  FROM RegistrationStatusEntity registration WHERE registration.statusCode=:statusCode AND registration.latestTransactionStatusCode =:latestTransactionStatusCode AND registration.latestTransactionTimes < :timeDifference")
	long getInReprocessPacketsCount(@Param("statusCode")String statusCode,@Param("latestTransactionStatusCode")String latestTransactionStatusCode,@Param("timeDifference")LocalDateTime timeDifference);

	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId = :regId and registration.isDeleted =false and registration.isActive=true")
	public List<RegistrationStatusEntity> findByRegId(@Param("regId") String regId);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId IN :regIds and registration.isDeleted =false and registration.isActive=true")
	public List<RegistrationStatusEntity> findByRegIds(@Param("regIds") List<String> regIds);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId IN :regIds and registration.isDeleted =false and registration.isActive=true order by registration.createDateTime")
	public List<RegistrationStatusEntity> findByRegIdsOrderbyCreatedDateTime(@Param("regIds") List<String> regIds);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.statusCode = :statusCode order by registration.updateDateTime limit :fetchSize")
	public List<RegistrationStatusEntity> findByStatusCodeOrderbyUpdatedDateTime(@Param("statusCode") String statusCode,@Param("fetchSize") Integer fetchSize);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.statusCode = :statusCode and registration.isDeleted =false and registration.isActive=true")
	public List<RegistrationStatusEntity> findByStatusCode(@Param("statusCode") String statusCode);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.regId = :regId and registration.statusCode = :statusCode ")
	public List<RegistrationStatusEntity> findByRegIdANDByStatusCode(@Param("regId") String regId,@Param("statusCode") String statusCode);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.id.workflowInstanceId = :workflowInstanceId and registration.isDeleted =false and registration.isActive=true")
	public List<RegistrationStatusEntity> findByWorkflowInstanceId(@Param("workflowInstanceId") String workflowInstanceId);
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.latestTransactionStatusCode IN :status and registration.regProcessRetryCount<=:reprocessCount and registration.latestTransactionTimes<:timeDifference and registration.statusCode  NOT IN :statusCodes limit :fetchSize")
	public List<RegistrationStatusEntity> getUnProcessedPackets(@Param("status") List<String> status,@Param("reprocessCount") Integer reprocessCount,@Param("timeDifference") LocalDateTime timeDifference,@Param("statusCodes") List<String> statusCodes,@Param("fetchSize") Integer fetchSize );
	
	@Query("SELECT COUNT(*) FROM RegistrationStatusEntity registration WHERE registration.latestTransactionStatusCode IN :status and registration.regProcessRetryCount<=:reprocessCount and registration.latestTransactionTimes<:timeDifference and registration.statusCode  NOT IN :statusCodes ")
	public int getUnProcessedPacketsCount(@Param("status") List<String> status,@Param("reprocessCount") Integer reprocessCount,@Param("timeDifference") LocalDateTime timeDifference,@Param("statusCodes") List<String> statusCodes );
	
	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.statusCodes IN :statusCodes  and registration.resumeTimeStamp<:now() and registration.defaultResumeAction is not null order by registration.updateDateTime limit :fetchSize")
	public List<RegistrationStatusEntity> getActionablePausedPackets(@Param("statusCodes") List<String> statusCodes,@Param("fetchSize") Integer fetchSize);

	@Query("SELECT registration FROM RegistrationStatusEntity registration WHERE registration.statusCode=:statusCode   order by registration.updateDateTime limit :fetchSize")
	public List<RegistrationStatusEntity> getResumablePackets(@Param("statusCode") String statusCode,@Param("fetchSize") Integer fetchSize);
}

