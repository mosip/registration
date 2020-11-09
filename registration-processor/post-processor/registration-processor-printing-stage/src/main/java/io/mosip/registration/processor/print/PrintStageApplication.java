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
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.status.config");
		ctx.refresh();

		PrintStage printStage = ctx.getBean(PrintStage.class);
		printStage.deployVerticle();

	}
}
