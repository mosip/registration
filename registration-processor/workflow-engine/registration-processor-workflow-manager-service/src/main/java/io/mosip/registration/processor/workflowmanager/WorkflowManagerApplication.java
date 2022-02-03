package io.mosip.registration.processor.workflowmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionApi;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionJob;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowInternalActionVerticle;

@Configuration
@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.workflowmanager.config",
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.kernel.websub.api.config.publisher" })
public class WorkflowManagerApplication 
{
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(WorkflowManagerApplication.class, args);
		
		
		WorkflowInternalActionVerticle workflowInternalActionVerticle = ctx
				.getBean(WorkflowInternalActionVerticle.class);
		workflowInternalActionVerticle.deployVerticle();
		WorkflowActionApi workflowActionApi = ctx.getBean(WorkflowActionApi.class);
		workflowActionApi.deployVerticle();
		WorkflowActionJob workflowActionJob = ctx.getBean(WorkflowActionJob.class);
		workflowActionJob.deployVerticle();
	}
}
