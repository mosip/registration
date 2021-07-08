package io.mosip.registration.processor.status.sync.response.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.status.dto.ErrorDTO;
import io.mosip.registration.processor.status.dto.PacketExternalStatusDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Instantiates a new packet external status response DTO.
 */
@Data

/* (non-Javadoc)
 * @see io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO#hashCode()
 */
@EqualsAndHashCode(callSuper = true)
public class PacketExternalStatusResponseDTO extends BaseRestResponseDTO {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 9096621207505473503L;

	/** The response. */
	private List<PacketExternalStatusDTO> response;

	/** The error. */
	private List<ErrorDTO> errors;

}
