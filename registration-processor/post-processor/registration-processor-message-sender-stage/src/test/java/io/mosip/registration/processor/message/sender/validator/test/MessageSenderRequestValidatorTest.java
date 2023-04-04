package io.mosip.registration.processor.message.sender.validator.test;

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
import io.mosip.registration.processor.core.exception.MessageSenderRequestValidationException;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderDTO;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderRequestDTO;
import io.mosip.registration.processor.message.sender.validator.MessageSenderRequestValidator;

@RunWith(SpringRunner.class)
public class MessageSenderRequestValidatorTest {
	@Mock
	private Environment env;

	@InjectMocks
	MessageSenderRequestValidator messageSenderRequestValidator;

	@Before
	public void setup() {
		when(env.getProperty("mosip.regproc.message.sender.api.id"))
				.thenReturn("mosip.registration.processor.message.sender");
		when(env.getProperty("mosip.regproc.message.sender.api.version")).thenReturn("1.0");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		when(env.getProperty("mosip.registration.processor.timezone"))
		.thenReturn("GMT");
		ReflectionTestUtils.setField(messageSenderRequestValidator, "gracePeriod", 10800);
	
	}

	@Test
	public void testValidateSuccess() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderDTO.setVersion("1.0");
		messageSenderDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		MessageSenderRequestDTO messageSenderRequestDTO=new MessageSenderRequestDTO();
		messageSenderRequestDTO.setRegType("NEW");
		messageSenderRequestDTO.setRid("10003100030001520190422074511");
		messageSenderDTO.setRequest(messageSenderRequestDTO);
		messageSenderRequestValidator.validate(messageSenderDTO);
	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testMissingId() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderRequestValidator.validate(messageSenderDTO);
	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testMissingVersion() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderRequestValidator.validate(messageSenderDTO);
	
	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testMissingRequesttime() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderDTO.setVersion("1.0");
		messageSenderRequestValidator.validate(messageSenderDTO);
	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testInValidId() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender.test");
		messageSenderRequestValidator.validate(messageSenderDTO);

	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testInValidVersion() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderDTO.setVersion("1.1");
		messageSenderRequestValidator.validate(messageSenderDTO);

	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testInValidRequestTime() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderDTO.setVersion("1.0");
		messageSenderDTO.setRequesttime("2023-04-02T12:08:00.158Z");

		messageSenderRequestValidator.validate(messageSenderDTO);
	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testMissingRid() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderDTO.setVersion("1.0");
		messageSenderDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		MessageSenderRequestDTO messageSenderRequestDTO = new MessageSenderRequestDTO();
		messageSenderDTO.setRequest(messageSenderRequestDTO);
		messageSenderRequestValidator.validate(messageSenderDTO);
	}

	@Test(expected = MessageSenderRequestValidationException.class)
	public void testMissingRegType() throws MessageSenderRequestValidationException {
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		messageSenderDTO.setId("mosip.registration.processor.message.sender");
		messageSenderDTO.setVersion("1.0");
		messageSenderDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		MessageSenderRequestDTO messageSenderRequestDTO = new MessageSenderRequestDTO();
		messageSenderRequestDTO.setRid("10003100030001520190422074511");
		messageSenderDTO.setRequest(messageSenderRequestDTO);
		messageSenderRequestValidator.validate(messageSenderDTO);
	}
}
