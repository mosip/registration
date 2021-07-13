package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.packet.storage.utils.OSIUtils;
import io.mosip.registration.processor.stages.supervisorvalidator.SupervisorValidationProcessor;
import io.mosip.registration.processor.stages.supervisorvalidator.SupervisorValidator;

@Configuration
public class SupervisorValidatorConfig {

	@Bean
	public SupervisorValidationProcessor getSupervisorValidationProcessor() {
		return new SupervisorValidationProcessor();
	}

	@Bean
	public SupervisorValidator getSupervisorValidator() {
		return new SupervisorValidator();
	}
	
	@Bean
	public OSIUtils getOSIUtils() {
		return new OSIUtils();
	}

}
