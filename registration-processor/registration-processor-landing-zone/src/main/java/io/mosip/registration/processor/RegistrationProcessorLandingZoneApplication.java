package io.mosip.registration.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan( basePackages = { "io.mosip.registration.processor.status.*",
		"io.mosip.registration.processor.rest.client.*", "io.mosip.registration.processor.util",
		"io.mosip.registration.processor.core.config","${mosip.auth.adapter.impl.basepackage}"} )
public class RegistrationProcessorLandingZoneApplication {

	public static void main(String[] args) {
		SpringApplication.run(RegistrationProcessorLandingZoneApplication.class, args);
	}

}
