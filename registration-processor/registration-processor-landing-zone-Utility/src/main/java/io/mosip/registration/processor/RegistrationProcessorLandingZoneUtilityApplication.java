package io.mosip.registration.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan( basePackages = { "io.mosip.registration.processor.status.*",
		"io.mosip.registration.processor.rest.client.*", "io.mosip.registration.processor.util",
		"io.mosip.registration.processor.core.config","${mosip.auth.adapter.impl.basepackage}"} )
public class RegistrationProcessorLandingZoneUtilityApplication {

	public static void main(String[] args) {
		SpringApplication.run(RegistrationProcessorLandingZoneUtilityApplication.class, args);
	}

}
