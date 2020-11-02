package io.mosip.registration.processor.stages.validator.impl;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
    public boolean validate(String id, String source, String process, PacketValidationDto packetValidationDto) throws PacketValidatorException, ApisResourceAccessException, RegistrationProcessorCheckedException, IOException, JsonProcessingException, PacketManagerException {
        boolean isValid = packetValidatorImpl.validate(id, source, process, packetValidationDto);
        if (isValid)
            isValid = referenceValidatorImpl.validate(id, source, process, packetValidationDto);
        return isValid;
    }
}
