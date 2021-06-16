package io.mosip.registration.processor.notification.service;

import org.springframework.http.ResponseEntity;

import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;


public interface NotificationService {

	ResponseEntity<Void> process(WorkflowCompletedEventDTO object);

	ResponseEntity<Void> process(WorkflowPausedForAdditionalInfoEventDTO object);

}