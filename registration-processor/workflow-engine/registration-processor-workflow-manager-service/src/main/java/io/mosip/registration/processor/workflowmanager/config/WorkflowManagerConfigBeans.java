package io.mosip.registration.processor.workflowmanager.config;

import io.mosip.registration.processor.workflowmanager.service.WorkflowInstanceService;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowInstanceRequestValidator;
import io.mosip.registration.processor.workflowmanager.verticle.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;

import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.client.PublisherClientImpl;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.rest.client.service.impl.RegistrationProcessorRestClientServiceImpl;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowSearchService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowActionRequestValidator;
import io.mosip.registration.processor.workflowmanager.validator.WorkflowSearchRequestValidator;

@PropertySource("classpath:bootstrap.properties")
@Configuration
public class WorkflowManagerConfigBeans {
	@Bean
	public WorkflowInternalActionVerticle getWorkflowEventUpdateVerticle() {
		return new WorkflowInternalActionVerticle();
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
    public WorkflowInstanceApi getWorkFlowInstanceApi() {
        return new WorkflowInstanceApi();
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
    public WorkflowInstanceRequestValidator getWorkflowInstanceRequestValidator() {
        return new WorkflowInstanceRequestValidator();
    }


	@Bean
	public WorkflowActionService getWorkflowActionService() {
		return new WorkflowActionService();
	}


    @Bean
    public WorkflowInstanceService getWorkFlowInstanceService() {
        return new WorkflowInstanceService();
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
	public PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> getWebPublisherClientForWorkflowCompletedEvent() {
		return new PublisherClientImpl<WorkflowCompletedEventDTO>();
	}

	@Bean
	public RegistrationProcessorRestClientService<Object> getRegistrationProcessorRestClientService() {
		return new RegistrationProcessorRestClientServiceImpl();
	}

	@Bean
	public WorkflowActionJob getWorkflowActionJob() {
		return new WorkflowActionJob();
	}

	@Bean
	public PublisherClient<String, WorkflowPausedForAdditionalInfoEventDTO, HttpHeaders> getWebPublisherClientForWorkflowPausedForAdditionalInfoEvent() {
		return new PublisherClientImpl<WorkflowPausedForAdditionalInfoEventDTO>();
	}
}
