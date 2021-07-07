package io.mosip.registration.processor.status.sync.response.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.status.dto.ErrorDTO;
import io.mosip.registration.processor.status.dto.PacketStatusDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PacketStatusResponseDTO extends BaseRestResponseDTO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9096621207505473503L;

	/** The response. */
	private List<PacketStatusDTO> response;

	/** The error. */
	private List<ErrorDTO> errors;

}
