package io.mosip.registration.processor.stages.validator;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.stages.validator.impl.CompositePacketValidator;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;

@RunWith(SpringJUnit4ClassRunner.class)
public class CompositePacketValidatorTest {

	@InjectMocks
	CompositePacketValidator compositePacketValidator = new CompositePacketValidator();

	@Mock
	private PacketValidatorImpl packetValidatorImpl;

	@Mock
	private PacketValidator referenceValidatorImpl;

	PacketValidationDto packetValidationDto = new PacketValidationDto();

	@Test
	public void validateSuccessTest() throws ApisResourceAccessException, RegistrationProcessorCheckedException,
			JsonProcessingException, PacketManagerException, IOException {

		Mockito.when(packetValidatorImpl.validate(anyString(), anyString(), any())).thenReturn(true);
		Mockito.when(referenceValidatorImpl.validate(anyString(), anyString(), any())).thenReturn(true);
		boolean result = compositePacketValidator.validate("10011100120000620210727102631", "NEW", packetValidationDto);
		assertEquals(result, true);
	}

	@Test
	public void validateFailureTest() throws ApisResourceAccessException, RegistrationProcessorCheckedException,
			JsonProcessingException, PacketManagerException, IOException {

		Mockito.when(packetValidatorImpl.validate(anyString(), anyString(), any())).thenReturn(false);
		boolean result = compositePacketValidator.validate("10011100120000620210727102631", "NEW", packetValidationDto);
		assertEquals(result, false);
	}

}