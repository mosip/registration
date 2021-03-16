package io.mosip.registration.processor.reprocessor.service;

import java.util.List;

import org.springframework.stereotype.Component;

import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.WorkflowActionCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
@Component
public class WorkflowActionService {
	
	public void processWorkflowAction(List<String> workflowIds, String workflowAction,
			MosipEventBus mosipEventBus) {
		WorkflowActionCode workflowActionCode = WorkflowActionCode.valueOf(workflowAction);
		switch (workflowActionCode) {
		case RESUME_PROCESSING:
			processResumeProcessing(workflowIds, mosipEventBus);
		case RESUME_PROCESSING_AND_REMOVE_HOTLISTED_TAG:
			processResumeProcessingAndRemoveHotlistedTag(workflowIds, mosipEventBus);
		case RESUME_FROM_BEGINNING:
			processResumeFromBeginning(workflowIds, mosipEventBus);
		case RESUME_FROM_BEGINNING_AND_REMOVE_HOTLISTED_TAG:
			processResumeFromBeginningAndRemoveHotlistedTag(workflowIds, mosipEventBus);
		case STOP_PROCESSING:
			processStopProcessing(workflowIds, mosipEventBus);
		}
	}

	private void processStopProcessing(List<String> workflowIds, MosipEventBus mosipEventBus) {
		// TODO Auto-generated method stub

	}

	private void processResumeFromBeginningAndRemoveHotlistedTag(List<String> workflowIds,
			MosipEventBus mosipEventBus) {
		// TODO Auto-generated method stub

	}

	private void processResumeFromBeginning(List<String> workflowIds,
			MosipEventBus mosipEventBus) {
		// TODO Auto-generated method stub

	}

	private void processResumeProcessingAndRemoveHotlistedTag(List<String> workflowIds,
			MosipEventBus mosipEventBus) {
		// TODO Auto-generated method stub

	}

	private void processResumeProcessing(List<String> workflowIds,
			MosipEventBus mosipEventBus) {
		// TODO Auto-generated method stub

	}

	private void removeHotlistedTag(String rid) {
		// TODO Auto-generated method stub

	}
}
