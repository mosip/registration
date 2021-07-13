package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.registration.processor.stages.introducervalidator.IntroducerValidationProcessor;
import io.mosip.registration.processor.stages.introducervalidator.IntroducerValidator;

@Configuration
public class IntroducerValidatorConfig {
	
	@Bean
	public IntroducerValidationProcessor getIntroducerValidationProcessor() {
		return new IntroducerValidationProcessor();
	}
	
	@Bean
	public IntroducerValidator getIntroducerValidator() {
		return new IntroducerValidator();
	}
	
	@Bean
	public BioAPIFactory getBioAPIFactory() {
		return new BioAPIFactory();
	}

}
