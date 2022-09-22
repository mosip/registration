package io.mosip.registration.processor.verification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.verification.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.verification.service.VerificationService;
import io.mosip.registration.processor.verification.service.impl.VerificationServiceImpl;
import io.mosip.registration.processor.verification.util.ManualVerificationRequestValidator;
import io.mosip.registration.processor.verification.util.SaveVerificationRecordUtility;

@Configuration
public class VerificationConfigBean {
	
	
	@Bean
    VerificationService getManualVerificationService() {
		return new VerificationServiceImpl();
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
	SaveVerificationRecordUtility getsSaveVerificationRecordUtility() {
		return new SaveVerificationRecordUtility();
	}

}