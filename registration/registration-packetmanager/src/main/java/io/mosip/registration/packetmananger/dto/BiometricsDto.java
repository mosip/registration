package io.mosip.registration.packetmananger.dto;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import lombok.Data;

@Data
public class BiometricsDto {
	
	private byte[] modalityISO;
	private String modalityName;
	private double qualityScore;
	private boolean isForceCaptured;
	private int numOfRetries;
	
	private long formatType;
	private SingleType type;
	private String subType;
}
