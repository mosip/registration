package io.mosip.registration.processor.transaction.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RegistrationTransactionSwaggerConfig {
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components())
				.info(new Info().title("Registration Transaction Service API documentation").description(
						"Registration Transaction service ").version("3.0.1"));
	}
}
