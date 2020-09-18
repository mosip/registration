package io.mosip.registration.processor.core.auth.dto;

import lombok.Data;

@Data
public class BaseAuthRequestDTO {

	/** The value for Id*/
	private String id;

	/** The value for version*/
	private String version;

	private String specVersion;

	private String thumbprint;

	private String domainUri;

	private String env;
	
}
