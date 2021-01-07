package io.mosip.registration.processor.manual.verification.request.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import lombok.Data;

@Data
public class ManualAdjudicationRequestDTO {
	
	private String id;
	
	private String version;

	private String requestId;

	private String referenceId;

	private String requesttime;
	
	private String referenceURL;
	
	private List<Addtional> addtional;
	
	private Gallery gallery;
	

}
