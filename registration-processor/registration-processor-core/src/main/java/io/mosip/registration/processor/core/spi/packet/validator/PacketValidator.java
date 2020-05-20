package io.mosip.registration.processor.core.spi.packet.validator;

import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;

public interface PacketValidator {

	boolean validate(String registrationId, String registrationType, PacketValidationDto packetValidationDto) throws PacketValidatorException;
}
