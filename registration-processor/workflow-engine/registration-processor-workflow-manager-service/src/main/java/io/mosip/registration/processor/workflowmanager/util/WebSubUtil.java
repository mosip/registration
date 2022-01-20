package io.mosip.registration.processor.workflowmanager.util;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;


@Component
public class WebSubUtil {
	@Autowired
	private PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> workflowCompletedPublisher;

	@Autowired
	private PublisherClient<String, WorkflowPausedForAdditionalInfoEventDTO, HttpHeaders> workflowPausedForAdditionalInfoPublisher;
	
	@Value("${mosip.regproc.workflow.complete.topic}")
	private String workflowCompleteTopic;

	@Value("${websub.publish.url}")
	private String webSubPublishUrl;

	@Value("${mosip.regproc.workflow.pausedforadditionalinfo.topic}")
	private String workflowPausedforadditionalinfoTopic;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WebSubUtil.class);
	

	@PostConstruct
	private void registerTopic() {
		try {
			workflowCompletedPublisher.registerTopic(workflowCompleteTopic, webSubPublishUrl);

		} catch (WebSubClientException exception) {
			regProcLogger.warn(exception.getMessage());
		}
		try {
			workflowPausedForAdditionalInfoPublisher.registerTopic(workflowPausedforadditionalinfoTopic,
					webSubPublishUrl);
		} catch (WebSubClientException exception) {
			regProcLogger.warn(exception.getMessage());
		}
	}

	public void publishEvent(WorkflowCompletedEventDTO workflowCompletedEventDTO) throws WebSubClientException {
		String rid = workflowCompletedEventDTO.getInstanceId();
		HttpHeaders httpHeaders = new HttpHeaders();
		workflowCompletedPublisher.publishUpdate(workflowCompleteTopic, workflowCompletedEventDTO,
				MediaType.APPLICATION_JSON_UTF8_VALUE,
				httpHeaders, webSubPublishUrl);
		regProcLogger.info("Publish the update successfully  for registration id {}", rid);

	}

	public void publishEvent(WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO)
			throws WebSubClientException {
		String rid = workflowPausedForAdditionalInfoEventDTO.getInstanceId();
		HttpHeaders httpHeaders = new HttpHeaders();
		workflowPausedForAdditionalInfoPublisher.publishUpdate(workflowPausedforadditionalinfoTopic,
				workflowPausedForAdditionalInfoEventDTO,
				MediaType.APPLICATION_JSON_UTF8_VALUE,
				httpHeaders, webSubPublishUrl);
		regProcLogger.info("Publish the update successfully  for registration id {}", rid);

	}
	
	
}
