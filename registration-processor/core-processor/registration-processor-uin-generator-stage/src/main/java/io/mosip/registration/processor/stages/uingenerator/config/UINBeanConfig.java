package io.mosip.registration.processor.stages.uingenerator.config;

import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.idvalidator.vid.impl.VidValidatorImpl;
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
	public VidValidator<String> vidValidator(){return new VidValidatorImpl();}
}
