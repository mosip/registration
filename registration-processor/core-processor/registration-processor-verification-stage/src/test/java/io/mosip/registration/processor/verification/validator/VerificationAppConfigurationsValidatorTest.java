package io.mosip.registration.processor.verification.validator;

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
import io.mosip.registration.processor.verification.validators.VerificationAppConfigurationsValidator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { VerificationAppConfigurationsValidatorTest.MyConfig.class })
public class VerificationAppConfigurationsValidatorTest {

	@InjectMocks
	VerificationAppConfigurationsValidator verificationAppConfigurationsValidator = new VerificationAppConfigurationsValidator();

	@Autowired
	private MyListener listener;

	@Before
	public void setup() throws Exception {
		ReflectionTestUtils.setField(verificationAppConfigurationsValidator, "reprocessorElapseTime", 7200);
		ReflectionTestUtils.setField(verificationAppConfigurationsValidator, "reprocessBufferTime", 900);
		ReflectionTestUtils.setField(verificationAppConfigurationsValidator, "mvRequestMessageTTL", 5400);
	}

	@Test
	public void validateConfigurationsSuccessTest() throws RegistrationProcessorCheckedException {

		verificationAppConfigurationsValidator.validateConfigurations(listener.events.peek());
	}
	
	@Test
	public void validateConfigurationsElapseTimeTest() throws RegistrationProcessorCheckedException {

		ReflectionTestUtils.setField(verificationAppConfigurationsValidator, "reprocessorElapseTime", 1000);
		verificationAppConfigurationsValidator.validateConfigurations(listener.events.peek());
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