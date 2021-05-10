package io.mosip.registration.processor.stages;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.stages.app.SupervisorValidatorStage;

public class SupervisorValidatorStageApplication {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config", "io.mosip.registration.processor.stages.*",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.registration.processor.core.kernel.beans");
		ctx.refresh();
		SupervisorValidatorStage validatebean = ctx.getBean(SupervisorValidatorStage.class);
		validatebean.deployVerticle();

	}

}
