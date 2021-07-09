package io.mosip.registration.processor.status.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "packetId", "errorCode", "errorMessage" })
public class PacketStatusErrorDTO extends ErrorDTO {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -5261464773892046294L;


	public PacketStatusErrorDTO(String errorcode, String message) {
		super(errorcode, message);
	}


	private String packetId;

}
