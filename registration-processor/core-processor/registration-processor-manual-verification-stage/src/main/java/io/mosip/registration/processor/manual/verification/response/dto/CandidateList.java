package io.mosip.registration.processor.manual.verification.response.dto;

import java.util.List;

import lombok.Data;

@Data
public class CandidateList {

	private Integer count;
	
	private List<Candidates> candidates;
	
	
}
