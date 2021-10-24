package io.mosip.registration.processor.manual.verification.response.dto;

import lombok.Data;
import org.json.simple.JSONObject;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class Candidate {

	@NotNull
	@NotBlank
	private String referenceId;
	
	private JSONObject analytics;

}
