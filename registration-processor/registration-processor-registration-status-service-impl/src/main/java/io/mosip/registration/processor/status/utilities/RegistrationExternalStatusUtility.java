package io.mosip.registration.processor.status.utilities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.code.RegistrationExternalStatusCode;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;

/**
 * The Class RegistrationExternalStatusUtility.
 */
@Component
public class RegistrationExternalStatusUtility {
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(RegistrationExternalStatusUtility.class);

	/** The threshold time. */
	@Value("${registration.processor.max.retry}")
	private int thresholdTime;

	/** The elapsed time. */
	@Value("${registration.processor.reprocess.elapse.time}")
	private int elapsedTime;

	/** 
	 * The list of comma separated stages that should be successfully completed before packet 
	 * reaches the stage that uploads packets to the packet store 
	 * */
	@Value("#{'${mosip.registration.processor.registration.status.stages-before-reaching-packet-store:PacketReceiverStage,SecurezoneNotificationStage}'.split(',')}")
	private List<String> stagesBeforeReachingPacketStore;

	/**
	 * Instantiates a new registration external status utility.
	 */
	public RegistrationExternalStatusUtility() {
		super();
	}

	/**
	 * Gets the external status.
	 *
	 * @param entity
	 *            the entity
	 * @return the external status
	 */
	public RegistrationExternalStatusCode getExternalStatus(RegistrationStatusEntity entity) {

		RegistrationExternalStatusCode mappedValue = null;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"RegistrationStatusMapUtil::getExternalStatus()::entry");

		String status = entity.getStatusCode();
		if (status.equalsIgnoreCase(RegistrationStatusCode.PROCESSED.toString())) {
			mappedValue = RegistrationExternalStatusCode.UIN_GENERATED;
		} else if (status.equalsIgnoreCase(RegistrationStatusCode.PROCESSING.toString())
				|| status.equalsIgnoreCase(RegistrationStatusCode.PAUSED.toString())
				|| status.equalsIgnoreCase(RegistrationStatusCode.RESUMABLE.toString())
				|| status.equalsIgnoreCase(RegistrationStatusCode.REPROCESS.toString()) 
				|| status.equalsIgnoreCase(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString())) {
			mappedValue = checkStatusforPacketReceiver(entity);
		} else if (status.equalsIgnoreCase(RegistrationStatusCode.FAILED.toString())) {
			mappedValue = checkStatusforPacketUploader(entity);
		}else {
			mappedValue = RegistrationExternalStatusCode.REJECTED;
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				entity.getReferenceRegistrationId(), "RegistrationStatusMapUtil::getExternalStatus()::exit");
		return mappedValue;
	}

	/**
	 * Check statusfor packet receiver.
	 *
	 * @param entity
	 *            the entity
	 * @return the registration external status code
	 */
	private RegistrationExternalStatusCode checkStatusforPacketReceiver(RegistrationStatusEntity entity) {
		long timeElapsedinPacketreceiver = checkElapsedTime(entity);
		if ((entity.getLatestTransactionTypeCode()
				.equalsIgnoreCase(RegistrationTransactionTypeCode.PACKET_RECEIVER.toString()))
				&& (timeElapsedinPacketreceiver > elapsedTime)) {
			if ((entity.getRetryCount() < thresholdTime)) {
				return RegistrationExternalStatusCode.RESEND;
			}
			return RegistrationExternalStatusCode.REREGISTER;
		} else if(entity.getLastSuccessStageName() == null ||
				stagesBeforeReachingPacketStore.contains(entity.getLastSuccessStageName()))
			return RegistrationExternalStatusCode.PROCESSING;
		else
			return RegistrationExternalStatusCode.PROCESSED;
	}

	/**
	 * Check status for packet uploader.
	 *
	 * @param entity
	 *            the entity
	 * @return the registration external status code
	 */
	private RegistrationExternalStatusCode checkStatusforPacketUploader(RegistrationStatusEntity entity) {
		if ((entity.getLatestTransactionTypeCode()
				.equalsIgnoreCase(RegistrationTransactionTypeCode.PACKET_RECEIVER.toString())
				|| entity.getLatestTransactionTypeCode()
						.equalsIgnoreCase(RegistrationTransactionTypeCode.UPLOAD_PACKET.toString()))
				&& (entity.getRetryCount() < thresholdTime)) {
			return RegistrationExternalStatusCode.RESEND;
		} else
			return RegistrationExternalStatusCode.REREGISTER;
	}

	/**
	 * Check elapsed time.
	 *
	 * @param entity
	 *            the entity
	 * @return the long
	 */
	private Long checkElapsedTime(RegistrationStatusEntity entity) {
		LocalDateTime createdTime = entity.getLatestTransactionTimes();
		LocalDateTime currentTime = LocalDateTime.now();
		LocalDateTime tempDate = LocalDateTime.from(createdTime);
		long seconds = tempDate.until(currentTime, ChronoUnit.SECONDS);
		return seconds;
	}

}
