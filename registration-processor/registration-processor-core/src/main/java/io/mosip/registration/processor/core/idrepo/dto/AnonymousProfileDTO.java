package io.mosip.registration.processor.core.idrepo.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;



@Data
public class AnonymousProfileDTO {

	private String yearOfBirth;
	
	private String gender;
	
	private List<String> factors;
	
	private List<String> exceptions;
	
	private String locationData;
	
	private String dateOfEnrollment;
	
	private List<String> preferredLanguages;
	
	private Map<String, Boolean> verificationStatus;
}
