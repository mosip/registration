package io.mosip.registration.processor.adjudication.response.dto;

import lombok.Data;
import org.json.simple.JSONObject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class Candidate {

	@NotNull
	@NotBlank
	private String referenceId;
	
	private JSONObject analytics;

}
