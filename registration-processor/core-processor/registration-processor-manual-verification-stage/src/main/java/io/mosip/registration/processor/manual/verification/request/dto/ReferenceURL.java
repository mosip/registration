package io.mosip.registration.processor.manual.verification.request.dto;

import lombok.Data;

@Data
public class ReferenceURL {
	private String source;
	private String status;
	private String URL;
}
