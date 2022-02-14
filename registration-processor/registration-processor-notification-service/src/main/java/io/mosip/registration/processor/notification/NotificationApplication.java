package io.mosip.registration.processor.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * Init class for Notification service.
 *
 * @author Urvil Joshi
 * @since 1.0.0
 *
 */
@SpringBootApplication(scanBasePackages = { "io.mosip.registration.processor.notification.*",
		"io.mosip.kernel.websub.api.*","${mosip.auth.adapter.impl.basepackage}", "io.mosip.registration.processor.message.sender.config",
		"io.mosip.registration.processor.rest.client.config", "io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.core.config", "io.mosip.registration.processor.packet.storage.dao" ,"io.mosip.registration.processor.status.config"})
@EnableScheduling
public class NotificationApplication {

	/**
	 * Main method to run spring boot application
	 * 
	 * @param args args
	 */
	public static void main(String[] args) {
		SpringApplication.run(NotificationApplication.class, args);
	}

}
