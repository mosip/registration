package io.mosip.registration.processor.status.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.entity.AnonymousProfileEntity;
import io.mosip.registration.processor.status.entity.AnonymousProfilePKEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
@Component
public class AnonymousProfileServiceImpl implements AnonymousProfileService {
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(AnonymousProfileServiceImpl.class);
	/** The Anonymus Profile repository. */
	@Autowired
	private BaseRegProcRepository<AnonymousProfileEntity, String> anonymousProfileRepository;


	@Override
	public void saveAnonymousProfile(String regId, String processStage, String profileJson) {
		try {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
					"PacketInfoManagerImpl::saveAnonymousProfile()::entry");
			AnonymousProfileEntity anonymousProfileEntity=new AnonymousProfileEntity();
			AnonymousProfilePKEntity anonymousProfilePKEntity=new AnonymousProfilePKEntity();
			anonymousProfilePKEntity.setId(regId);
			anonymousProfileEntity.setId(anonymousProfilePKEntity);
			anonymousProfileEntity.setProfile(profileJson);
			anonymousProfileEntity.setProcessStage(processStage);
			anonymousProfileEntity.setCreatedBy("SYSTEM");
			anonymousProfileEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			anonymousProfileEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			anonymousProfileEntity.setIsDeleted(false);
			
			anonymousProfileRepository.save(anonymousProfileEntity);
			} catch (DataAccessLayerException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						"", e.getMessage() + ExceptionUtils.getStackTrace(e));

				throw new TablenotAccessibleException(
						PlatformErrorMessages.RPR_RGS_ANONYMOUS_PROFILE_TABLE_NOT_ACCESSIBLE.getMessage(), e);
			} 
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
					"PacketInfoManagerImpl::saveRegLostUinDetData()::exit");

	}

}
