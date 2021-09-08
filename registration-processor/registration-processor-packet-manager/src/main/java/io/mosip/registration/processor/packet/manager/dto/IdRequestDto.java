package io.mosip.registration.processor.packet.manager.dto;

import lombok.Data;

/**
 * The Class IdRequestDTO.
 *
 * @author Ranjitha Siddegowda
 */
@Data
public class IdRequestDto {
	
	/** The id. */
	private String id;
	
	/** The request. */
	private RequestDto request;
	
	/** The time stamp. */
	private String requesttime;
	
	/** The version. */
	private String version;
	
	private Object metadata;
	
}
