package io.mosip.registration.processor.adjudication.util;


	import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.adjudication.dto.ManualVerificationStatus;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.packet.storage.entity.ManualVerificationEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;

	@Component
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public class ManualVerificationUpdateUtility {
		/** The logger. */
		private static Logger regProcLogger = RegProcessorLogger.getLogger(ManualVerificationUpdateUtility.class);

		/** The base packet repository. */
		@Autowired
		private BasePacketRepository<ManualVerificationEntity, String> basePacketRepository;

		/**
		 * Update manual verification entity once request is pushed to queue for a given
		 * RID
		 */
		public void updateManualVerificationEntityRID(List<ManualVerificationEntity> mves, String requestId) {
			mves.stream().forEach(mve -> {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						mve.getRegId(), "ManualVerificationUpdateUtility::updateManualVerificationEntityRID()::entry");
				mve.setStatusCode(ManualVerificationStatus.INQUEUE.name());
				mve.setStatusComment("Sent to manual adjudication queue");
				mve.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				mve.setRequestId(requestId);
				basePacketRepository.update(mve);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						mve.getRegId(), "ManualVerificationUpdateUtility::updateManualVerificationEntityRID()::exit");
			});
		}
	}

