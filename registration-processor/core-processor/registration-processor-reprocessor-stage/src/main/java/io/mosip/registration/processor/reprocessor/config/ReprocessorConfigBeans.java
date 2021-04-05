package io.mosip.registration.processor.reprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.client.PublisherClientImpl;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.reprocessor.service.WorkflowActionService;
import io.mosip.registration.processor.reprocessor.stage.ReprocessorStage;
import io.mosip.registration.processor.reprocessor.stage.WorkflowActionApi;
import io.mosip.registration.processor.reprocessor.stage.WorkflowEventUpdateVerticle;
import io.mosip.registration.processor.reprocessor.util.WebSubUtil;
import io.mosip.registration.processor.reprocessor.validator.WorkflowActionRequestValidator;
import io.mosip.registration.processor.rest.client.service.impl.RegistrationProcessorRestClientServiceImpl;

/**
 * Config class to get configurations and beans for Reprocessor stage
 * 
 * @author Pranav Kumar
 * @since 0.10.0
 *
 */
@PropertySource("classpath:bootstrap.properties")
@Configuration
public class ReprocessorConfigBeans {

	@Bean
	public ReprocessorStage getReprocessorStage() {
		return new ReprocessorStage();
	}
	@Bean
	public RegistrationProcessorRestClientService<Object> getRegistrationProcessorRestClientService() {
		return new RegistrationProcessorRestClientServiceImpl();
	}

	@Bean
	public WorkflowEventUpdateVerticle getWorkflowEventUpdateVerticle() {
		return new WorkflowEventUpdateVerticle();
	}

	@Bean
	public WorkflowActionApi getWorkFlowActionApi() {
		return new WorkflowActionApi();
	}

	@Bean
	public WorkflowActionRequestValidator getWorkflowActionRequestValidator() {
		return new WorkflowActionRequestValidator();
	}

	@Bean
	public WorkflowActionService getWorkflowActionService() {
		return new WorkflowActionService();
	}

	@Bean
	public PacketManagerService getPacketManagerService() {
		return new PacketManagerService();
	}

	@Bean
	public WebSubUtil getWebSubUtil() {
		return new WebSubUtil();
	}

	@Bean
	public PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> getWebPublisherClient() {
		return new PublisherClientImpl<WorkflowCompletedEventDTO>();
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}
}
