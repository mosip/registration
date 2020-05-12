package io.mosip.registration.processor.print;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.print.stage.PrintStage;

/**
 * @author M1048399
 *
 */
public class PrintStageApplication {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config", "io.mosip.registration.processor.print.config",
				"io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.filesystem.ceph.adapter.impl.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.message.sender.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.print.service.config",
				"io.mosip.registration.processor.message.sender.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.registration.processor.packet.utility.config");
		ctx.refresh();

		PrintStage printStage = ctx.getBean(PrintStage.class);
		printStage.deployVerticle();
	}
}
