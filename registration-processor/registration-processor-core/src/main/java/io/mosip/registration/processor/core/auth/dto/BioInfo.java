package io.mosip.registration.processor.core.auth.dto;

import lombok.Data;

@Data
public class BioInfo {

	/** The Value for data */
	private String data;

	/** The Value for hash */
	private String hash;

	/** The Value for sessionKey */
	private String sessionKey;

	private String specVersion;

	/** The Value for signature */
	private String thumbprint;

}
