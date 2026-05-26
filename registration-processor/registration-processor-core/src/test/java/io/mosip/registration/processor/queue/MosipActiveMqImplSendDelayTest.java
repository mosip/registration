package io.mosip.registration.processor.queue;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.activemq.ScheduledMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.queue.impl.MosipActiveMqImpl;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

@RunWith(MockitoJUnitRunner.class)
public class MosipActiveMqImplSendDelayTest {

	@InjectMocks
	private MosipActiveMqImpl mosipActiveMqImpl;

	@Mock
	private TextMessage textMessage;

	@Test
	public void applyScheduledDelaySetsAmqScheduledDelayProperty() throws JMSException {
		ReflectionTestUtils.invokeMethod(mosipActiveMqImpl, "applyScheduledDelay", textMessage, 500L,
				"abis-inbound-queue");

		verify(textMessage).setLongProperty(eq(ScheduledMessage.AMQ_SCHEDULED_DELAY), eq(500L));
	}

	@Test
	public void applyZeroDelaySkipsScheduledProperty() throws JMSException {
		ReflectionTestUtils.invokeMethod(mosipActiveMqImpl, "applyScheduledDelay", textMessage, 0L,
				"abis-inbound-queue");

		verify(textMessage, never()).setLongProperty(eq(ScheduledMessage.AMQ_SCHEDULED_DELAY), anyLong());
	}

}
