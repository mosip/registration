package io.mosip.registration.processor.status.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * Instantiates a new packet external status DTO.
 */
@Data
public class PacketExternalStatusDTO  implements Serializable {
/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 7190330336993883907L;

	/** The packet id. */
	private String packetId;
	
	/** The status code. */
	private String statusCode;
}
