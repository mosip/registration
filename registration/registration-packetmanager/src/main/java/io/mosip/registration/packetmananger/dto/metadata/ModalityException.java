package io.mosip.registration.packetmananger.dto.metadata;

import lombok.Data;

@Data
public class ModalityException {
	private String type;
	private String missingBiometric;
	private String reason;
	private String exceptionType;
	private String individualType;
}
