package io.mosip.registration.dto.response;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UiScreenDTO {

	private int order;
	private String name;
	private HashMap<String, String> label;
	private HashMap<String, String> caption;
	private List<String> fields;
	private String layoutTemplate;
	private boolean isActive;
	private boolean preRegFetchRequired;
	
}
