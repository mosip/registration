package io.mosip.registration.mdm.spec_0_9_5.dto.response;

import java.util.Base64;

import lombok.Data;

@Data
public class RCaptureResponseDataDTO {

	private String deviceCode;
	private String bioType;
	private String digitalId;
	private String deviceServiceVersion;
	private String bioSubType;
	private String purpose;
	private String env;
	private String bioValue;
	private String transactionId;
	private String timestamp;
	private String requestedScore;
	private String qualityScore;

	public byte[] getDecodedBioValue() {
		return Base64.getUrlDecoder().decode(bioValue);
	}

}
