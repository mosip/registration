package io.mosip.registration.processor.workflowmanager;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionApi;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionJob;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowInternalActionVerticle;


public class WorkflowManagerApplication 
{
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.workflowmanager.config",
				"io.mosip.registration.processor.status.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.kernel.websub.api.config.publisher");
		ctx.refresh();
		WorkflowInternalActionVerticle workflowInternalActionVerticle = ctx
				.getBean(WorkflowInternalActionVerticle.class);
		workflowInternalActionVerticle.deployVerticle();
		WorkflowActionApi workflowActionApi = ctx.getBean(WorkflowActionApi.class);
		workflowActionApi.deployVerticle();
		WorkflowActionJob workflowActionJob = ctx.getBean(WorkflowActionJob.class);
		workflowActionJob.deployVerticle();
	}
}
