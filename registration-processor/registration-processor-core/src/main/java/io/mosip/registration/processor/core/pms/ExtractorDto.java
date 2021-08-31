package io.mosip.registration.processor.core.pms;

import lombok.Data;

@Data
public class ExtractorDto {
	
	private String attributeName;
	
	private String biometric;
	
	private ExtractorProviderDto extractor;
}