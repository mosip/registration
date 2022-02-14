package io.mosip.registration.processor.workflowmanager.util.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;


@RunWith(SpringRunner.class)
public class WebSubUtilTest {
	@Mock
	private PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> pb;

	@Mock
	private PublisherClient<String, WorkflowPausedForAdditionalInfoEventDTO, HttpHeaders> workflowPausedForAdditionalInfoPublisher;

	@InjectMocks
	WebSubUtil webSubUtil;

	@Test
	public void testPublishEventSuccess() throws WebSubClientException {
		ReflectionTestUtils.setField(webSubUtil, "webSubPublishUrl", "/websubdummypublishurl");
		ReflectionTestUtils.setField(webSubUtil, "workflowCompleteTopic",
				"registration_processor_workflow_completed_event");
		WorkflowCompletedEventDTO workflowCompletedEventDTO = new WorkflowCompletedEventDTO();
		webSubUtil.publishEvent(workflowCompletedEventDTO);
		verify(pb, times(1)).publishUpdate(any(), any(WorkflowCompletedEventDTO.class), any(), any(), any());		
	}

	@Test
	public void testPublishPausedForAdditionalInfoEventSuccess() throws WebSubClientException {
		ReflectionTestUtils.setField(webSubUtil, "webSubPublishUrl", "/websubdummypublishurl");
		ReflectionTestUtils.setField(webSubUtil, "workflowPausedforadditionalinfoTopic",
				"registration_processor_workflow_paused_for_additional_info_event");
		WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO = new WorkflowPausedForAdditionalInfoEventDTO();
		webSubUtil.publishEvent(workflowPausedForAdditionalInfoEventDTO);
		verify(workflowPausedForAdditionalInfoPublisher, times(1)).publishUpdate(any(),
				any(WorkflowPausedForAdditionalInfoEventDTO.class), any(), any(),
				any());
	}
}
