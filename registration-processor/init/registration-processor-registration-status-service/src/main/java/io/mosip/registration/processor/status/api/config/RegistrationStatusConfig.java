package io.mosip.registration.processor.status.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class RegistrationStatusConfig.
 */
@Configuration
public class RegistrationStatusConfig {

	/**
	 * Registration status bean.
	 */
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components())
				.info(new Info().title("Registration status API").description(
						"This is a registration status service using springdoc-openapi and OpenAPI 3.").version("3.0.1"));
	}
}

