package io.mosip.registration.processor.stages.validator;

import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.stages.dto.PacketValidationDto;
import io.mosip.registration.processor.stages.exception.PacketValidatorException;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

public interface PacketValidator {

	boolean validate(InternalRegistrationStatusDto registrationStatusDto, PacketMetaInfo packetMetaInfo,
			MessageDTO object, IdentityIteratorUtil identityIteratorUtil, PacketValidationDto packetValidationDto,
			TrimExceptionMessage trimMessage, LogDescription description) throws PacketValidatorException;
}
