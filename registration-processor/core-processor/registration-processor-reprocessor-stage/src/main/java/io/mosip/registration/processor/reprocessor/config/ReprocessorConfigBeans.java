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
import io.mosip.registration.processor.reprocessor.service.WorkflowSearchService;
import io.mosip.registration.processor.reprocessor.stage.ReprocessorStage;
import io.mosip.registration.processor.reprocessor.stage.WorkflowActionApi;
import io.mosip.registration.processor.reprocessor.stage.WorkflowEventUpdateVerticle;
import io.mosip.registration.processor.reprocessor.stage.WorkflowSearchApi;
import io.mosip.registration.processor.reprocessor.util.WebSubUtil;
import io.mosip.registration.processor.reprocessor.validator.WorkflowActionRequestValidator;
import io.mosip.registration.processor.reprocessor.validator.WorkflowSearchRequestValidator;
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
	public WorkflowSearchApi getWorkflowSearchApi() {
		return new WorkflowSearchApi();
	}

	@Bean
	public WorkflowActionRequestValidator getWorkflowActionRequestValidator() {
		return new WorkflowActionRequestValidator();
	}

	@Bean
	public WorkflowSearchRequestValidator getWorkflowSearchRequestValidator() {
		return new WorkflowSearchRequestValidator();
	}

	@Bean
	public WorkflowActionService getWorkflowActionService() {
		return new WorkflowActionService();
	}

	@Bean
	public WorkflowSearchService getWorkflowSearchService() {
		return new WorkflowSearchService();
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
