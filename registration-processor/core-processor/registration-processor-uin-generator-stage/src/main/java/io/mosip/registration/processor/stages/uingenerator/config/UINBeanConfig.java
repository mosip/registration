package io.mosip.registration.processor.stages.uingenerator.config;

import io.mosip.registration.processor.stages.uingenerator.util.IdSchemaUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.mosip.registration.processor.stages.uingenerator.stage.UinGeneratorStage;

@Configuration
public class UINBeanConfig {
	
	@Bean 
	public UinGeneratorStage getUinGeneratorStage() {
		return new UinGeneratorStage();
	}

	@Bean
	public IdSchemaUtil idSchemaUtil() {
		return new IdSchemaUtil();
	}
}
