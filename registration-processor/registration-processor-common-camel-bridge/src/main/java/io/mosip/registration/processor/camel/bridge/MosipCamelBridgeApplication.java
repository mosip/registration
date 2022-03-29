package io.mosip.registration.processor.camel.bridge;

import io.mosip.registration.processor.core.config.reader.ConfigPropertyReader;
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
				ConfigPropertyReader.getConfig("mosip.auth.adapter.impl.basepackage"),
				"io.mosip.registration.processor.camel.bridge.config",
				"io.mosip.registration.processor.rest.client.config");
		configApplicationContext.refresh();
		MosipBridgeFactory mosipBridgeFactory = configApplicationContext.getBean(MosipBridgeFactory.class);
		mosipBridgeFactory.getEventBus();
		mosipBridgeFactory.startCamelBridge();
	}

}
