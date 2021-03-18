package io.mosip.registration.processor.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Init class for Notification service.
 *
 * @author Urvil Joshi
 * @since 1.0.0
 *
 */
@SpringBootApplication(scanBasePackages = { "io.mosip.registration.processor.notification.*","io.mosip.registration.processor.message.sender.*","io.mosip.kernel.websub.api.*","io.mosip.kernel.packetmanager.config"})
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