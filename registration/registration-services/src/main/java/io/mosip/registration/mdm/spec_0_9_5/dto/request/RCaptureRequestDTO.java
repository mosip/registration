package io.mosip.registration.mdm.spec_0_9_5.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class RCaptureRequestDTO {

	private String env;
	private String purpose;
	private String specVersion;
	private String timeout;
	private String captureTime;
	private String transactionId;
	private List<RCaptureRequestBioDTO> bio;
	private Object customOpts;

	public RCaptureRequestDTO(String env, String purpose, String specVersion, String timeout, String captureTime,
			String transactionId, List<RCaptureRequestBioDTO> bio, Object customOpts) {
		super();
		this.env = env;
		this.purpose = purpose;
		this.specVersion = specVersion;
		this.timeout = timeout;
		this.captureTime = captureTime;
		this.transactionId = transactionId;
		this.bio = bio;
		this.customOpts = customOpts;
	}

}
