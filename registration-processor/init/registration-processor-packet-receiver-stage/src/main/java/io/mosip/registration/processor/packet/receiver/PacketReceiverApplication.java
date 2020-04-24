package io.mosip.registration.processor.packet.receiver;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.packet.receiver.stage.PacketReceiverStage;

/**
 * The Class PacketReceiverApplication.
 */
public class PacketReceiverApplication {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
		configApplicationContext.scan(
				"io.mosip.registration.processor.status.config",
				  "io.mosip.registration.processor.packet.receiver.config",
				  "io.mosip.registration.processor.core.config",
				  "io.mosip.registration.processor.rest.client.config");
		configApplicationContext.refresh();
		PacketReceiverStage packetReceiverStage = configApplicationContext.getBean(PacketReceiverStage.class);
		packetReceiverStage.deployVerticle();
	}
}
