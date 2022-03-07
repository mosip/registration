package io.mosip.registration.processor.reprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import io.mosip.registration.processor.reprocessor.verticle.ReprocessorVerticle;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
/**
 * Main class for Reprocessor Application
 * 
 * @author Pranav kumar
 * @since 0.10.0
 *
 */
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.reprocessor.config",
            "io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.packet.storage.config" })
public class ReprocessorApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ReprocessorApplication.class);

		ReprocessorVerticle reprocessorVerticle = ctx.getBean(ReprocessorVerticle.class);
		reprocessorVerticle.deployVerticle();
	}

}
