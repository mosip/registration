package io.mosip.registration.processor.core.packet.dto.packetvalidator;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class PacketValidationDto {
	private boolean isValid = false;
	private boolean isTransactionSuccessful;
	private String packetValidatonStatusCode="";
	private String packetValidaionFailureMessage = "";
}
