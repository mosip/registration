package io.mosip.registration.processor.core.message.sender.dto;

import java.util.List;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import lombok.Data;

@Data
public class MessageSenderResponse extends BaseRestResponseDTO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ResponseDTO response;

	/** The error. */
	private List<ErrorDTO> errors;
}
