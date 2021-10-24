package io.mosip.registration.processor.manual.verification.dto;

import java.util.List;

import lombok.Data;

@Data
public class Filter {
	public String language;
	public String type;
	public List<String> subType;
}
