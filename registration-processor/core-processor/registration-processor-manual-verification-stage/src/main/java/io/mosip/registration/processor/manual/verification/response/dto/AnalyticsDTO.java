package io.mosip.registration.processor.manual.verification.response.dto;

import lombok.Data;

@Data
public class AnalyticsDTO {

	private String primaryOperatorID;
	
	private String primaryOperatorComments;
	
	private String secondaryOperatorID;
	
	private String secondaryOperatorComments;
	
	private String key1="value1";
	
	private String key2="value2";
}
