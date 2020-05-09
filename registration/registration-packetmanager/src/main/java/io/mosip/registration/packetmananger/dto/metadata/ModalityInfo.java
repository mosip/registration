package io.mosip.registration.packetmananger.dto.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModalityInfo {	
	@JsonProperty("BIRIndex")
	private String birIndex;
	private int numRetry;
	private boolean forceCaptured;
}
