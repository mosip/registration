package io.mosip.registration.processor.workflowmanager.validator.test;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.UserNotificationRequestValidationException;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationDTO;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationRequestDTO;
import io.mosip.registration.processor.workflowmanager.validator.UserNotificationRequestValidator;

@RunWith(SpringRunner.class)
public class UserNotificationRequestValidatorTest {
	@Mock
	private Environment env;

	@InjectMocks
	UserNotificationRequestValidator userNotificationRequestValidator;

	@Before
	public void setup() {
		when(env.getProperty("mosip.regproc.user.notification.api.id"))
				.thenReturn("mosip.registration.processor.user.notification");
		when(env.getProperty("mosip.regproc.user.notification.api.version")).thenReturn("1.0");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.timezone"))
		.thenReturn("GMT");
		ReflectionTestUtils.setField(userNotificationRequestValidator, "gracePeriod", 10800);

	}

	@Test
	public void testValidateSuccess() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationDTO.setVersion("1.0");
		userNotificationDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		UserNotificationRequestDTO userNotificationRequestDTO=new UserNotificationRequestDTO();
		userNotificationRequestDTO.setRegType("NEW");
		userNotificationRequestDTO.setRid("10003100030001520190422074511");
		userNotificationDTO.setRequest(userNotificationRequestDTO);
		userNotificationRequestValidator.validate(userNotificationDTO);
	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testMissingId() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationRequestValidator.validate(userNotificationDTO);
	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testMissingVersion() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationRequestValidator.validate(userNotificationDTO);

	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testMissingRequesttime() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationDTO.setVersion("1.0");
		userNotificationRequestValidator.validate(userNotificationDTO);
	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testInValidId() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification.test");
		userNotificationRequestValidator.validate(userNotificationDTO);

	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testInValidVersion() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationDTO.setVersion("1.1");
		userNotificationRequestValidator.validate(userNotificationDTO);

	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testInValidRequestTime() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationDTO.setVersion("1.0");
		userNotificationDTO.setRequesttime("2023-04-02T12:08:00.158Z");

		userNotificationRequestValidator.validate(userNotificationDTO);
	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testMissingRid() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationDTO.setVersion("1.0");
		userNotificationDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		UserNotificationRequestDTO userNotificationRequestDTO = new UserNotificationRequestDTO();
		userNotificationDTO.setRequest(userNotificationRequestDTO);
		userNotificationRequestValidator.validate(userNotificationDTO);
	}

	@Test(expected = UserNotificationRequestValidationException.class)
	public void testMissingRegType() throws UserNotificationRequestValidationException {
		UserNotificationDTO userNotificationDTO = new UserNotificationDTO();
		userNotificationDTO.setId("mosip.registration.processor.user.notification");
		userNotificationDTO.setVersion("1.0");
		userNotificationDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		UserNotificationRequestDTO userNotificationRequestDTO = new UserNotificationRequestDTO();
		userNotificationRequestDTO.setRid("10003100030001520190422074511");
		userNotificationDTO.setRequest(userNotificationRequestDTO);
		userNotificationRequestValidator.validate(userNotificationDTO);
	}
}
