package io.mosip.registration.mdm.spec_0_9_2.dto.request;

import lombok.Data;

@Data
public class StreamBioRequestDTO {

	private String type;
	private int count;
	private String[] exception;
	private int requestedScore;
	private String deviceId;
	private int deviceSubId;
	private String previousHash;

}
