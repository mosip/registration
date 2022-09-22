package io.mosip.registration.processor.verification.util;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.packet.storage.entity.VerificationEntity;
import io.mosip.registration.processor.packet.storage.entity.VerificationPKEntity;
import io.mosip.registration.processor.packet.storage.exception.UnableToInsertData;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.verification.dto.ManualVerificationStatus;


@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class SaveVerificationRecordUtility {
	
	private static Logger regProcLogger = RegProcessorLogger.getLogger(SaveVerificationRecordUtility.class);
	private static final String VERIFICATION_COMMENT = "Packet marked for verification";
	
	@Autowired
	private BasePacketRepository<VerificationEntity, String> basePacketRepository;
	
	
	public boolean saveVerificationRecord(MessageDTO messageDTO, String requestId, LogDescription description) {
		String registrationId = messageDTO.getRid();
		boolean isTransactionSuccessful = false;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "saveVerificationRecord::entry");

		try {
			List<VerificationEntity> existingRecords = basePacketRepository
					.getVerificationRecordByWorkflowInstanceId(messageDTO.getWorkflowInstanceId());

			if (CollectionUtils.isEmpty(existingRecords)) {
				VerificationEntity verificationEntity = new VerificationEntity();
				VerificationPKEntity verificationPKEntity = new VerificationPKEntity();
				verificationPKEntity.setWorkflowInstanceId(messageDTO.getWorkflowInstanceId());

				verificationEntity.setRegId(registrationId);
				verificationEntity.setId(verificationPKEntity);
				verificationEntity.setReponseText(null);
				verificationEntity.setRequestId(requestId);
				verificationEntity.setVerificationUsrId(null);
				verificationEntity.setReasonCode(VERIFICATION_COMMENT);
				verificationEntity.setStatusComment(VERIFICATION_COMMENT);
				verificationEntity.setStatusCode(ManualVerificationStatus.INQUEUE.name());
				verificationEntity.setActive(true);
				verificationEntity.setDeleted(false);
				verificationEntity.setCrBy("SYSTEM");
				verificationEntity.setCrDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
				verificationEntity.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
				basePacketRepository.save(verificationEntity);
			} else {
				VerificationEntity existingRecord = existingRecords.iterator().next();
				existingRecord.setReponseText(null);
				existingRecord.setVerificationUsrId(null);
				existingRecord.setReasonCode(VERIFICATION_COMMENT);
				existingRecord.setStatusComment(VERIFICATION_COMMENT);
				existingRecord.setStatusCode(ManualVerificationStatus.INQUEUE.name());
				existingRecord.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
				basePacketRepository.update(existingRecord);
			}
			description.setMessage("Packet marked for Verification saved successfully");
			isTransactionSuccessful = true;

		} catch (DataAccessLayerException e) {
			isTransactionSuccessful = false;
			description.setMessage("DataAccessLayerException while saving Verification data for rid" + registrationId
					+ "::" + e.getMessage());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, e.getMessage() + ExceptionUtils.getStackTrace(e));

			throw new UnableToInsertData(
					PlatformErrorMessages.RPR_PIS_UNABLE_TO_INSERT_DATA.getMessage() + registrationId, e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "saveVerificationRecord::exit");

		return isTransactionSuccessful;
	}

}
