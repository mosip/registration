package io.mosip.registration.processor.verification.response.builder;

import java.util.Objects;

import io.mosip.registration.processor.verification.response.dto.ManualVerificationAssignResponseDTO;
import io.mosip.registration.processor.verification.response.dto.ManualVerificationBioDemoResponseDTO;
import io.mosip.registration.processor.verification.response.dto.ManualVerificationDecisionResponseDTO;
import io.mosip.registration.processor.verification.response.dto.ManualVerificationPacketResponseDTO;
import io.mosip.registration.processor.verification.response.dto.Response;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.verification.dto.VerificationDecisionDto;

@Component
public class ManualVerificationResponseBuilder {

	public static BaseRestResponseDTO buildManualVerificationSuccessResponse(Object classType, String id,
			String version, String dateTimePattern) {

		if (classType.getClass() == ManualVerificationDTO.class) {
			ManualVerificationAssignResponseDTO response = new ManualVerificationAssignResponseDTO();
			if (Objects.isNull(response.getId())) {
				response.setId(id);
			}
			response.setErrors(null);
			response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
			response.setVersion(version);
			response.setResponse((ManualVerificationDTO) classType);
			return response;

		}
		else if (classType.getClass() == VerificationDecisionDto.class) {
			ManualVerificationDecisionResponseDTO response = new ManualVerificationDecisionResponseDTO();
			if (Objects.isNull(response.getId())) {
				response.setId(id);
			}
			response.setErrors(null);
			response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
			response.setVersion(version);
			response.setResponse((VerificationDecisionDto) classType);
			return response;

		}
		else if (classType.getClass() == String.class) {
			ManualVerificationBioDemoResponseDTO manualVerificationBioDemoResponseDTO = new ManualVerificationBioDemoResponseDTO();
			if (Objects.isNull(manualVerificationBioDemoResponseDTO.getId())) {
				manualVerificationBioDemoResponseDTO.setId(id);
			}
			manualVerificationBioDemoResponseDTO.setErrors(null);
			manualVerificationBioDemoResponseDTO
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
			manualVerificationBioDemoResponseDTO.setVersion(version);
			Response response = new Response();
			response.setFile((String) classType);
			manualVerificationBioDemoResponseDTO.setResponse(response);
			return manualVerificationBioDemoResponseDTO;
		} else {

			ManualVerificationPacketResponseDTO response = new ManualVerificationPacketResponseDTO();
			if (Objects.isNull(response.getId())) {
				response.setId(id);
			}
			response.setErrors(null);
			response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(dateTimePattern));
			response.setVersion(version);
			response.setResponse((PacketMetaInfo) classType);
			return response;
		}
	}

}
