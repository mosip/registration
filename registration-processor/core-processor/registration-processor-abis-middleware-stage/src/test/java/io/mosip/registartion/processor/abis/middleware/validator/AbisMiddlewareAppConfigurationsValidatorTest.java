package io.mosip.registartion.processor.abis.middleware.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registartion.processor.abis.middleware.validators.AbisMiddlewareAppConfigurationsValidator;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AbisMiddlewareAppConfigurationsValidatorTest.MyConfig.class })
public class AbisMiddlewareAppConfigurationsValidatorTest {

	@InjectMocks
	AbisMiddlewareAppConfigurationsValidator abisMiddlewareAppConfigurationsValidator = new AbisMiddlewareAppConfigurationsValidator();

	@Mock
	private Utilities utility;

	@Autowired
	private MyListener listener;

	List<AbisQueueDetails> abisQueueDetails = new ArrayList<AbisQueueDetails>();

	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(abisMiddlewareAppConfigurationsValidator, "reprocessorElapseTime", 7200);
		ReflectionTestUtils.setField(abisMiddlewareAppConfigurationsValidator, "reprocessBufferTime", 900);
	}

	@Test
	public void validateConfigurationsSuccessTest() throws RegistrationProcessorCheckedException {

		AbisQueueDetails details = new AbisQueueDetails();
		details.setInboundMessageTTL(100);
		abisQueueDetails.add(details);
		Mockito.when(utility.getAbisQueueDetails()).thenReturn(abisQueueDetails);
		abisMiddlewareAppConfigurationsValidator.validateConfigurations(listener.events.peek());
	}
	
	@Test
	public void validateConfigurationsElapseTimeTest() throws RegistrationProcessorCheckedException {

		ReflectionTestUtils.setField(abisMiddlewareAppConfigurationsValidator, "reprocessorElapseTime", 1000);
		AbisQueueDetails details = new AbisQueueDetails();
		details.setInboundMessageTTL(100);
		abisQueueDetails.add(details);
		Mockito.when(utility.getAbisQueueDetails()).thenReturn(abisQueueDetails);
		abisMiddlewareAppConfigurationsValidator.validateConfigurations(listener.events.peek());
	}

	@Test
	public void validateConfigurationsFailureTest() throws RegistrationProcessorCheckedException {

		Mockito.when(utility.getAbisQueueDetails()).thenThrow(RegistrationProcessorCheckedException.class);
		abisMiddlewareAppConfigurationsValidator.validateConfigurations(listener.events.peek());
	}

	public static class MyConfig {
		@Bean
		public MyListener listener() {
			return new MyListener();
		}
	}

	public static class MyListener implements ApplicationListener<ContextRefreshedEvent> {
		LinkedBlockingQueue<ContextRefreshedEvent> events = new LinkedBlockingQueue<ContextRefreshedEvent>();

		public void onApplicationEvent(ContextRefreshedEvent event) {
			events.add(event);
		}
	}
}