package io.mosip.registration.processor.core.packet.dto.datashare;

import java.util.List;

import lombok.Data;

@Data
public class Source {
	public String attribute;

	public List<Filter> filter;
}
