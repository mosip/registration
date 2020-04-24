package io.mosip.registration.dto.mastersync;

import java.util.List;

import lombok.Data;

@Data
public class DynamicFieldDto {

	private String id;
	private String name;
	private String dataType;
	private List<DynamicFieldValueDto> fieldVal;
	private String langCode;	
}
