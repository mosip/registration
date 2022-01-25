package io.mosip.registration.processor.stages.validator;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.stages.validator.impl.CompositePacketValidator;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;
@RunWith(MockitoJUnitRunner.class)
public class CompositeValidatorTest {

	@InjectMocks
	CompositePacketValidator compositePacketValidator=new CompositePacketValidator();
	 @Mock
	 private PacketValidatorImpl packetValidatorImpl;

	    /** The reference validator. */
	 @Mock
	 @Qualifier("referenceValidatorImpl")
	 private PacketValidator referenceValidatorImpl;
	 
	 @Test
	 public void validateTest() throws ApisResourceAccessException, RegistrationProcessorCheckedException, JsonProcessingException, PacketManagerException, IOException {
		 Mockito.when(packetValidatorImpl.validate(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		 Mockito.when(referenceValidatorImpl.validate(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
		 assertTrue(compositePacketValidator.validate("1234", "NEW", new PacketValidationDto()));
	 }
	 	
}
