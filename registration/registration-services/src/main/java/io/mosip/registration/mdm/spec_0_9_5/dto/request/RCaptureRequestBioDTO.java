package io.mosip.registration.mdm.spec_0_9_5.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class RCaptureRequestBioDTO {

	private String type;
	private String count;
	private String[] bioSubType;
	private String[] exception;
	private String requestedScore;
	private String deviceId;
	private String deviceSubId;
	private String previousHash;
	public RCaptureRequestBioDTO(String type, String count, String[] bioSubType, String[] exception,
			String requestedScore, String deviceId, String deviceSubId, String previousHash) {
		super();
		this.type = type;
		this.count = count;
		this.bioSubType = bioSubType;
		this.exception = exception;
		this.requestedScore = requestedScore;
		this.deviceId = deviceId;
		this.deviceSubId = deviceSubId;
		this.previousHash = previousHash;
	}

	

}
