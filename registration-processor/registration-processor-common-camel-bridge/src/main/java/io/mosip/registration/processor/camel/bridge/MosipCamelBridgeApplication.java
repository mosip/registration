package io.mosip.registration.processor.camel.bridge;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This is main class for Vertx Camel Bridge
 *
 * @author Pranav Kumar
 * @since 0.0.1
 *
 */
public class MosipCamelBridgeApplication {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
		configApplicationContext.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.camel.bridge.config",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.kernel.core.logger.config");
		configApplicationContext.refresh();
		MosipBridgeFactory mosipBridgeFactory = configApplicationContext.getBean(MosipBridgeFactory.class);
		mosipBridgeFactory.getEventBus();
		mosipBridgeFactory.startCamelBridge();
	}

}
