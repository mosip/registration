package io.mosip.registration.processor.core.pms.dto;

import lombok.Data;

@Data
public class ExtractorDto {
	
	private String attributeName;
	
	private String biometric;
	
	private ExtractorProviderDto extractor;
}