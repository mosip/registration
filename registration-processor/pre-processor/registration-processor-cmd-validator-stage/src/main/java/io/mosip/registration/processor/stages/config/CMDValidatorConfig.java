package io.mosip.registration.processor.stages.config;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.stages.app.CMDValidationProcessor;

@Configuration
@RefreshScope
public class CMDValidatorConfig {
	
	@Bean
	public CMDValidationProcessor getCMDValidationProcessor() {
		return new CMDValidationProcessor();
	}

	@Bean
	public RestApiClient getRestApiClient() {
		return new RestApiClient();
	}

}
