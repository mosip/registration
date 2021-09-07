package io.mosip.registration.processor.manual.verification.validator;

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.manual.verification.validators.ManualVerificationAppConfigurationsValidator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ManualVerificationAppConfigurationsValidatorTest.MyConfig.class })
public class ManualVerificationAppConfigurationsValidatorTest {

	@InjectMocks
	ManualVerificationAppConfigurationsValidator manualVerificationAppConfigurationsValidator = new ManualVerificationAppConfigurationsValidator();

	@Autowired
	private MyListener listener;

	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(manualVerificationAppConfigurationsValidator, "reprocessorElapseTime", 7200);
		ReflectionTestUtils.setField(manualVerificationAppConfigurationsValidator, "reprocessBufferTime", 900);
		ReflectionTestUtils.setField(manualVerificationAppConfigurationsValidator, "mvRequestMessageTTL", 5400);
	}

	@Test
	public void validateConfigurationsSuccessTest() throws RegistrationProcessorCheckedException {

		manualVerificationAppConfigurationsValidator.validateConfigurations(listener.events.peek());
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
