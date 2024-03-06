package io.mosip.registration.processor.manual.adjudication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.mosip.registration.processor.manual.verification.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.manual.verification.response.builder.ManualVerificationResponseBuilder;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.service.impl.ManualVerificationServiceImpl;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
import io.mosip.registration.processor.adjudication.util.ManualVerificationRequestValidator;
import io.mosip.registration.processor.adjudication.util.ManualVerificationUpdateUtility;

@Configuration
public class ManualVerificationConfigBean {


	@Bean ManualVerificationService getManualVerificationService() {
		return new ManualVerificationServiceImpl();
	}
	@Bean
	ManualVerificationUpdateUtility getManualVerificationUpdateUtility() {
		return new ManualVerificationUpdateUtility();
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