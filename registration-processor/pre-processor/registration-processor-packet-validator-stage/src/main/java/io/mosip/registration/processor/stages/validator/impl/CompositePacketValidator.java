package io.mosip.registration.processor.stages.validator.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;

@Component
@Primary
public class CompositePacketValidator implements PacketValidator {

    @Autowired
    private PacketValidatorImpl packetValidatorImpl;

    /** The reference validator. */
    @Autowired(required = false)
    @Qualifier("referenceValidatorImpl")
    @Lazy
    private PacketValidator referenceValidatorImpl;

    @Override
    public boolean validate(String id, String process, PacketValidationDto packetValidationDto) throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException, JsonProcessingException, PacketManagerException {
        boolean isValid = packetValidatorImpl.validate(id, process, packetValidationDto);
        if (isValid)
            isValid = referenceValidatorImpl.validate(id, process, packetValidationDto);
        return isValid;
    }
}
