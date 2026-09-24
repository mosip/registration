package io.mosip.registration.processor.status.repositary;

import io.mosip.registration.processor.status.entity.SaltEntity;
import io.mosip.registration.processor.status.entity.TrackerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


/**
 * The Interface SaltRepository.
 *
 * @author Manoj SP
 */
public interface TrackerRepository extends JpaRepository<TrackerEntity, String> {

	@Query("SELECT trn FROM TrackerEntity trn WHERE trn.registrationId=:regId and trn.transactionId=:transactionId and trn.transactionFlowId=:transactionFlowid")
	TrackerEntity findByRegIdAndTransactionIdAndFlowId(@Param("regId") String regId, @Param("transactionId") String transactionId, @Param("transactionFlowid") String transactionFlowid);

	@Query("SELECT trn FROM TrackerEntity trn WHERE trn.transactionId=:transactionId")
	TrackerEntity findByTransactionId(@Param("transactionId") String transactionId);

}
