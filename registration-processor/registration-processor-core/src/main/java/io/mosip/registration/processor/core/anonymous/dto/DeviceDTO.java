package io.mosip.registration.processor.core.anonymous.dto;

import lombok.Data;

@Data
public class DeviceDTO {

	private String os;
	private String browser;
	private String browserVersion;
	private String software;
	private String version;
	private String year;
	private String id;
}
