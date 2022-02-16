package io.mosip.registration.processor.adjudication.request.dto;

import lombok.Data;

@Data
public class ReferenceURL {
	private String source;
	private String status;
	private String URL;
}
