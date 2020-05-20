package io.mosip.registration.processor.stages.validator.impl;

import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
    public boolean validate(String registrationId, String registrationType, PacketValidationDto packetValidationDto) throws PacketValidatorException {
        boolean isValid = packetValidatorImpl.validate(registrationId, registrationType, packetValidationDto);
        if (isValid)
            isValid = referenceValidatorImpl.validate(registrationId, registrationType, packetValidationDto);
        return isValid;
    }
}
