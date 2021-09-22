package io.mosip.registration.processor.core.anonymous.dto;

import lombok.Data;

@Data
public class BiometricInfoDTO {

	private String type;
	private String subType;
	private Long qualityScore;
	private String attempts;
	private String digitalId;
}
