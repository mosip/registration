package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.stages.supervisorvalidator.SupervisorValidationProcessor;

@Configuration
public class SupervisorValidatorConfig {

	@Bean
	public SupervisorValidationProcessor getSupervisorValidationProcessor() {
		return new SupervisorValidationProcessor();
	}

}
