package io.mosip.registration.processor.status.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * Instantiates a new packet external status sub request DTO.
 */
@Data
public class PacketExternalStatusSubRequestDTO implements Serializable{
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6952445372902508898L;
	
	/** The packet id. */
	private String packetId;
}
