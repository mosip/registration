package io.mosip.registration.processor.status.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class PacketStatusSubRequestDTO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6952445372902508898L;
	private String packetId;
}
