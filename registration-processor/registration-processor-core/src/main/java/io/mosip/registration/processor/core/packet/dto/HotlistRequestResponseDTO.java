package io.mosip.registration.processor.core.packet.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class HotlistRequestResponseDTO {

	private String id;
	
	private String idType;
	
	private String status;
	
	private LocalDateTime expiryTimestamp;
}
