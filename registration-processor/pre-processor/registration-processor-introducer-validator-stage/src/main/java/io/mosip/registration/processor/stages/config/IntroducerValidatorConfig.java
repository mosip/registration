package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.stages.app.IntroducerValidationProcessor;

@Configuration
public class IntroducerValidatorConfig {

	@Bean
	public IntroducerValidationProcessor getSupervisorValidationProcessor() {
		return new IntroducerValidationProcessor();
	}
}
