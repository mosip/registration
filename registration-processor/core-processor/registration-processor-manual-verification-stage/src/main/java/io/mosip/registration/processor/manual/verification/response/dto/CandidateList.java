package io.mosip.registration.processor.manual.verification.response.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CandidateList {

	@NotNull
	private Integer count;
	/**
	 * Analytics will be dumped in the manual verification table hence its expected as
	 * key value pair of String and Object.
	 */
	private Map<String, Object> analytics;
	private List<Candidate> candidates;
	
	
}
