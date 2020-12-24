package io.mosip.registration.processor.manual.verification.request.dto;

import java.util.List;

import lombok.Data;

@Data
public class Source {
	public String attribute;

	public List<Filter> filter;
}
