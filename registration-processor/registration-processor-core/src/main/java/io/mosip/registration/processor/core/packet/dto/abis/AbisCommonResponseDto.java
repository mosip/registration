package io.mosip.registration.processor.core.packet.dto.abis;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.apache.htrace.shaded.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class AbisCommonResponseDto implements Serializable{
	
	private static final long serialVersionUID = -7080424253600088998L;
	
	private String id;

	private String requestId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	private LocalDateTime responsetime;
	
	private Integer returnValue;
	
	private Integer failureReason;

}
