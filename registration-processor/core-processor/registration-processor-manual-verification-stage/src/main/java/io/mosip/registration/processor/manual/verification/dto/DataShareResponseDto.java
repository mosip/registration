package io.mosip.registration.processor.manual.verification.dto;

import lombok.Data;

@Data
public class DataShareResponseDto {

	private String policyId;
	private String signature;
	private String subscriberId;
	private Integer transactionsAllowed;
	private String url;
	private Integer validForInMinutes;
}
