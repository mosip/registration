package io.mosip.registration.processor.stages.createdraft;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.stages.createdraft.stage.CreateDraftStage;

public class CreateDraftApplication {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
		configApplicationContext.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.stages.createdraft.config",
				"io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.stages.config",
				"io.mosip.kernel.packetmanager.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.registration.processor.core.kernel.beans");

		configApplicationContext.refresh();

		CreateDraftStage createDraftStage = configApplicationContext.getBean(CreateDraftStage.class);
		createDraftStage.deployVerticle();

	}
}
