package io.mosip.registration.dto.mastersync;

import lombok.Data;

@Data
public class DynamicFieldValueJsonDto {

	private String code;
	private String value;
	private boolean isActive;
}
