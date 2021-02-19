package io.mosip.registration.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class UiScreenDTO {

	private String order;
	private String label;
	private List<String> fields;
	
}
