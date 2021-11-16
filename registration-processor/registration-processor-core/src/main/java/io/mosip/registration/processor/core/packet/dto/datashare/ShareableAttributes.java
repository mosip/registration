package io.mosip.registration.processor.core.packet.dto.datashare;

import java.util.List;

import lombok.Data;

@Data
public class ShareableAttributes {
	public boolean encrypted;

	public String format;

	public String attributeName;

	public List<Source> source;

	public String group;
}
