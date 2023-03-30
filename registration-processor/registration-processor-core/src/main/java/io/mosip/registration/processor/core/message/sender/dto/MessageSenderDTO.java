package io.mosip.registration.processor.core.message.sender.dto;

import io.mosip.registration.processor.core.common.rest.dto.BaseRestRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessageSenderDTO extends BaseRestRequestDTO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MessageSenderRequestDTO request;
}
