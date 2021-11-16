package io.mosip.registration.processor.verification.config;

import io.mosip.registration.processor.verification.stage.ManualAdjudicationStage;
import io.mosip.registration.processor.verification.util.ManualVerificationRequestValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.verification.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.verification.response.builder.ManualVerificationResponseBuilder;
import io.mosip.registration.processor.verification.service.ManualAdjudicationService;
import io.mosip.registration.processor.verification.service.impl.ManualAdjudicationServiceImpl;

@Configuration
public class ManualAdjudicationConfigBean {
	
	
	@Bean
    ManualAdjudicationService getManualVerificationService() {
		return new ManualAdjudicationServiceImpl();
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

	@Bean
	public ManualAdjudicationStage manualAdjudicationStage() {
		return new ManualAdjudicationStage();
	}

}