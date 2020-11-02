package io.mosip.registration.processor.stages.app;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.stages.osivalidator.OSIValidatorStage;

/**
 * The Class OSIValidatorApplication.
 */

public class OSIValidatorApplication {
	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config", "io.mosip.registration.processor.stages.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.registration.processor.core.kernel.beans");
		ctx.refresh();
		OSIValidatorStage validatebean = ctx.getBean(OSIValidatorStage.class);
		validatebean.deployVerticle();

	}

}
