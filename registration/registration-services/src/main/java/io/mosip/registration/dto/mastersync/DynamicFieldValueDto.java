package io.mosip.registration.dto.mastersync;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicFieldValueDto {

	private String code;
	private String value;
	private boolean active;
}
