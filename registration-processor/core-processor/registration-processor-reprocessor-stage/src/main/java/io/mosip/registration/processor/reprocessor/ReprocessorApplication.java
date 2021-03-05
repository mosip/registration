package io.mosip.registration.processor.reprocessor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.reprocessor.stage.ReprocessorStage;
import io.mosip.registration.processor.reprocessor.stage.WorkflowEventUpdateVerticle;

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
		WorkflowEventUpdateVerticle workFlowEventUpdateVerticle = ctx.getBean(WorkflowEventUpdateVerticle.class);
		workFlowEventUpdateVerticle.deployVerticle();
	}

}
