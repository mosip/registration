package io.mosip.registration.processor.biodedupe;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.biodedupe.stage.BioDedupeStage;

public class BioDedupeApplication {

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
		configApplicationContext.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.biodedupe.config", "io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.kernel.packetmanager.config");

		configApplicationContext.refresh();
		BioDedupeStage bioDedupeStage = configApplicationContext.getBean(BioDedupeStage.class);
		bioDedupeStage.deployVerticle();

	}

}