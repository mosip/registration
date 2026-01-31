package io.mosip.registration.processor.core.spi.packet.validator;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketValidatorException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;

import java.io.IOException;
import java.util.Map;

public interface PacketValidator {

	/**
	 * Validate packet. Default implementation delegates to {@link #validate(String, String, PacketValidationDto, Map)} with null metaInfo.
	 */
	default boolean validate(String registrationId, String process, PacketValidationDto packetValidationDto) throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException, JsonProcessingException, PacketManagerException {
		return validate(registrationId, process, packetValidationDto, null);
	}

	/**
	 * Validate packet with optional pre-fetched metaInfo to avoid redundant packet-manager getMetaInfo calls.
	 * When metaInfo is null, implementors fetch it internally.
	 */
	boolean validate(String registrationId, String process, PacketValidationDto packetValidationDto, Map<String, String> metaInfo) throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException, JsonProcessingException, PacketManagerException;
}
