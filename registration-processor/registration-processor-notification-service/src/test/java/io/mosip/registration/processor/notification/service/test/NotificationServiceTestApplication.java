package io.mosip.registration.processor.notification.service.test;

import javax.persistence.EntityManagerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;

/**
 * Audit manager application
 * 
 * @author Dharmesh Khandelwal
 * @since 1.0.0
 *
 */
@SpringBootApplication(scanBasePackages =  { "io.mosip.registration.processor.notification.service.*"},exclude = {HibernateDaoConfig.class} )
public class NotificationServiceTestApplication {

	/**
	 * Main method to run spring boot application
	 * 
	 * @param args args
	 */
	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceTestApplication.class, args);
	}
}
