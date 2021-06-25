package io.mosip.registration.processor.manual.verification.response.dto;

import lombok.Data;
import org.json.simple.JSONObject;

@Data
public class Candidate {
	
	private String referenceId;
	
	private JSONObject analytics;

}
