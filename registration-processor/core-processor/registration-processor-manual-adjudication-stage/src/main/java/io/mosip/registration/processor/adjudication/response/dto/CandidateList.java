package io.mosip.registration.processor.adjudication.response.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

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
