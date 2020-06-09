package io.mosip.registration.mdm.spec_0_9_5.dto.response;

import lombok.Data;

@Data
public class RCaptureResponseBiometricsDTO {

	private String specVersion;
	private String data;
	private String hash;
	private Error error;
}
