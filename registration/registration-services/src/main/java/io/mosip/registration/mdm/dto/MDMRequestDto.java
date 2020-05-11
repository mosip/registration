package io.mosip.registration.mdm.dto;

import lombok.Data;

@Data
public class MDMRequestDto {
	
	private String modality;
	private String[] bioAttributes;
	private String[] exceptions;
	
	private String mosipProcess;
	private String environment;
	private int timeout;
	private int count;
	private int requestedScore;

}
