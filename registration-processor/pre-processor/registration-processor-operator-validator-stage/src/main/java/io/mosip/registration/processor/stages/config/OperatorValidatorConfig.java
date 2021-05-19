package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.packet.storage.utils.OSIUtils;
import io.mosip.registration.processor.stages.operatorvalidator.OperatorValidationProcessor;
import io.mosip.registration.processor.stages.operatorvalidator.OperatorValidator;

@Configuration
public class OperatorValidatorConfig {

	@Bean
	public OperatorValidationProcessor getOperatorValidationProcessor() {
		return new OperatorValidationProcessor();
	}

	@Bean
	public OperatorValidator getOperatorValidator() {
		return new OperatorValidator();
	}

	@Bean
	public OSIUtils getOSIUtils() {
		return new OSIUtils();
	}
}
