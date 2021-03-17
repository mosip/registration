package io.mosip.registration.processor.reprocessor.util.test;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.reprocessor.util.WebSubUtil;

@RunWith(SpringRunner.class)
public class WebSubUtilTest {
	@Mock
	private PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> pb;

	@InjectMocks
	WebSubUtil webSubUtil;

	@Mock
	private Environment env;

	@Test
	public void testPublishEventSuccess() throws WebSubClientException {
		WorkflowCompletedEventDTO workflowCompletedEventDTO = new WorkflowCompletedEventDTO();
		when(env.getProperty("websub.publish.url")).thenReturn("/websubdummypublishurl");
		webSubUtil.publishEvent(workflowCompletedEventDTO);
	}

}
