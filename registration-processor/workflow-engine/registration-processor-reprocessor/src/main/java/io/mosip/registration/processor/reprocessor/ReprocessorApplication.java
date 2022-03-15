package io.mosip.registration.processor.reprocessor;

import io.mosip.registration.processor.core.config.reader.ConfigPropertyReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.reprocessor.verticle.ReprocessorVerticle;

/**
 * Main class for Reprocessor Application
 * 
 * @author Pranav kumar
 * @since 0.10.0
 *
 */
public class ReprocessorApplication {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config",
				ConfigPropertyReader.getConfig("mosip.auth.adapter.impl.basepackage"),
				"io.mosip.registration.processor.reprocessor.config",
				"io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.packet.storage.config");
		ctx.refresh();
		ReprocessorVerticle reprocessorVerticle = ctx.getBean(ReprocessorVerticle.class);
		reprocessorVerticle.deployVerticle();
	}

}
