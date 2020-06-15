package io.mosip.registration.mdm.dto;

import lombok.Data;

@Data
public class MDMRequestDto {

	private String modality;
	private String[] exceptions;
	private String mosipProcess;
	private String environment;
	private int timeout;
	private int count;
	private int requestedScore;

	public MDMRequestDto(String modality, String[] exceptions, String mosipProcess, String environment, int timeout,
			int count, int requestedScore) {
		super();
		this.modality = modality;
		this.exceptions = exceptions;
		this.mosipProcess = mosipProcess;
		this.environment = environment;
		this.timeout = timeout;
		this.count = count;
		this.requestedScore = requestedScore;
	}

}
