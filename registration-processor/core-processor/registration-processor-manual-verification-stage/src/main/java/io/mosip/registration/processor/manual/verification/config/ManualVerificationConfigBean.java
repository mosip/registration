package io.mosip.registration.processor.manual.verification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.mosip.registration.processor.manual.verification.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.manual.verification.response.builder.ManualVerificationResponseBuilder;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.service.impl.ManualVerificationServiceImpl;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
import io.mosip.registration.processor.manual.verification.util.ManualVerificationRequestValidator;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.service.impl.PacketInfoManagerImpl;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.manual.verification.Listener;

@Configuration
public class ManualVerificationConfigBean {
	
	
	@Bean ManualVerificationService getManualVerificationService() {
		return new ManualVerificationServiceImpl();
	}
	
	@Bean Listener getListener() {
		return new Listener();
	}
	
	@Bean
	public ManualVerificationStage getManualVerificationStage() {
		return new ManualVerificationStage();
	}

	@Bean
	ManualVerificationRequestValidator getManualVerificationRequestValidator() {
		return new ManualVerificationRequestValidator();
	}
	
	@Bean
	ManualVerificationExceptionHandler getManualVerificationExceptionHandler() {
		return new ManualVerificationExceptionHandler();
	}
	
	@Bean
	ManualVerificationResponseBuilder getManualVerificationResponseBuilder() {
		return new ManualVerificationResponseBuilder();
	}

}