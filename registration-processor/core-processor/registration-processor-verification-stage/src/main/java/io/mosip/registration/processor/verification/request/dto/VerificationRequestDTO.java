package io.mosip.registration.processor.verification.request.dto;

import java.util.List;

import lombok.Data;

@Data
public class VerificationRequestDTO {
	
	private String id;
	
	private String version;

	private String requestId;

	private String referenceId;

	private String requesttime;
	
	private String referenceURL;
	
	private List<Addtional> addtional;
	

}
