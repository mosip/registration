package io.mosip.registration.processor.notification.service;

import org.springframework.http.ResponseEntity;

import io.mosip.registration.processor.notification.dto.WorkflowCompletedEventDTO;

public interface NotificationService {

	ResponseEntity<Void> process(WorkflowCompletedEventDTO object);

}