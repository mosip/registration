package io.mosip.registration.processor.stages.uingenerator.config;

import io.mosip.registration.processor.stages.uingenerator.service.IdrepoDraftService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;

@Configuration
public class UINBeanConfig {
	
	@Bean
	public IdSchemaUtil idSchemaUtil() {
		return new IdSchemaUtil();
	}

	@Bean
	public IdrepoDraftService idrepoDraftService() {
		return new IdrepoDraftService();
	}
}
