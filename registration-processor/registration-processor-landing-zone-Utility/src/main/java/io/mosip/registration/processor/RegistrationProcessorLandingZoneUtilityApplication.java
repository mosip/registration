package io.mosip.registration.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RegistrationProcessorLandingZoneUtilityApplication {

	public static void main(String[] args) {
		SpringApplication.run(RegistrationProcessorLandingZoneUtilityApplication.class, args);
	}

}
