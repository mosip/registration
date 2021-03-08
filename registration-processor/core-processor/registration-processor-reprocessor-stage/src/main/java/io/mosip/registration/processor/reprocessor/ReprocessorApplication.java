package io.mosip.registration.processor.reprocessor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.reprocessor.stage.ReprocessorStage;
import io.mosip.registration.processor.reprocessor.stage.WorkFlowEventUpdateVerticle;
import io.mosip.registration.processor.reprocessor.stage.WorkflowActionApi;

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
		ctx.scan("io.mosip.registration.processor.core.config", "io.mosip.registration.processor.reprocessor.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.core.kernel.beans");
		ctx.refresh();
		ReprocessorStage reprocessorStage = ctx.getBean(ReprocessorStage.class);
		reprocessorStage.deployVerticle();
		WorkFlowEventUpdateVerticle workFlowEventUpdateVerticle = ctx.getBean(WorkFlowEventUpdateVerticle.class);
		workFlowEventUpdateVerticle.deployVerticle();
		WorkflowActionApi workflowActionApi = ctx.getBean(WorkflowActionApi.class);
		workflowActionApi.deployVerticle();
	}

}
