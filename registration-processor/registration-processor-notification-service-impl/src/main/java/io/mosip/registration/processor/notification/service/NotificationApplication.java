package io.mosip.registration.processor.notification.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Class MessageSenderApplication.
 *
 * @author Urvil Joshi
 * @since 1.0.0
 *
 */
@SpringBootApplication(scanBasePackages = { "io.mosip.registration.processor.*","io.mosip.kernel.packetmanager.config"})
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