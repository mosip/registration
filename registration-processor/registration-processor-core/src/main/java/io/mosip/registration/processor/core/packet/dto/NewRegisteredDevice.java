package io.mosip.registration.processor.core.packet.dto;

import lombok.Data;

@Data
public class NewRegisteredDevice {

	private String deviceCode;
	private String deviceServiceVersion;
	private NewDigitalId digitalId;
}
