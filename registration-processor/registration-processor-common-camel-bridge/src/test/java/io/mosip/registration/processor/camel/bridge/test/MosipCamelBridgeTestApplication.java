package io.mosip.registration.processor.camel.bridge.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is main class for Vertx Camel Bridge
 *
 * @author Pranav Kumar
 * @since 0.0.1
 *
 */
@SpringBootApplication(scanBasePackages = {"io.mosip.registration.processor.camel.bridge.config" })
public class MosipCamelBridgeTestApplication {

	
	
		/**
		 * Main method to run spring boot application
		 * 
		 * @param args args
		 */
		public static void main(String[] args) {
			SpringApplication.run(MosipCamelBridgeTestApplication.class, args);
		}

}
