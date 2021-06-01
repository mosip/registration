package io.mosip.registration.processor.workflow.manager;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.workflow.manager.stage.WorkflowActionApi;
import io.mosip.registration.processor.workflow.manager.stage.WorkflowActionJob;
import io.mosip.registration.processor.workflow.manager.stage.WorkflowInternalActionVerticle;


public class WorkflowManagerApplication 
{
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config", "io.registration.processor.workflow.manager.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.packet.storage.config");
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
