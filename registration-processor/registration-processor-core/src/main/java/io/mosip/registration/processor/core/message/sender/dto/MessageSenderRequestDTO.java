package io.mosip.registration.processor.core.message.sender.dto;

import lombok.Data;

@Data
public class MessageSenderRequestDTO {
	
	private String rid;
	private String regType;
}
