package io.mosip.registration.processor.notification.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.mosip.registration.processor.notification.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.notification.service.NotificationService;

@RestController
public class NotificationController {

	@Autowired
	private NotificationService notificationService;

	@PostMapping(value = "/callback", consumes = "application/json")
	@PreAuthenticateContentAndVerifyIntent(secret = "${registration.processor.notification_service_subscriber_secret}", callback = "/registrationprocessor/v1/notification/callback", topic = "${registration.processor.notification_service_subscriber_topic}")
	public ResponseEntity<Void> process(@RequestBody WorkflowCompletedEventDTO object) {
		{
			return notificationService.process(object);
		}
	}

}
