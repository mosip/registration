package io.mosip.registration.processor.reprocessor.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;

@Component
public class WebSubUtil {
	@Autowired
	private PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> publisher;

	@Autowired
	private Environment env;

	@Value("${mosip.regproc.workflow.complete.topic}")
	private String workflowCompleteTopic;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WebSubUtil.class);

	public void publishSuccess(WorkflowCompletedEventDTO workflowCompletedEventDTO)
			throws WebSubClientException {
		String rid = workflowCompletedEventDTO.getInstanceId();
		registerTopic(rid);
		HttpHeaders httpHeaders = new HttpHeaders();
		publisher.publishUpdate(workflowCompleteTopic, workflowCompletedEventDTO,
				MediaType.APPLICATION_JSON_UTF8_VALUE, httpHeaders, env.getProperty("websub.publish.url"));
		regProcLogger.info("Publish the update successfully  for registration id {}", rid);


	}

	private void registerTopic(String rid) {
		try {
			publisher.registerTopic(workflowCompleteTopic,
					env.getProperty("websub.publish.url"));
		} catch (WebSubClientException e) {
			regProcLogger.error("Topic already registered for registration id {}", rid);

		}

	}

}
