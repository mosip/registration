package io.mosip.registration.processor.eventhandler;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.eventhandler.stage.EventHandlerStage;

/**
 * @author M1048399
 *
 */
public class EventHandlerStageApplication {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config", "io.mosip.registration.processor.print.config",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.packet.storage.config");
		ctx.refresh();

		EventHandlerStage printStage = ctx.getBean(EventHandlerStage.class);
		printStage.deployVerticle();

	}
}
