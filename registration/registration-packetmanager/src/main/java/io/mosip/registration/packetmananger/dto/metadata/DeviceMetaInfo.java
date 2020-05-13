package io.mosip.registration.packetmananger.dto.metadata;

import lombok.Data;

@Data
public class DeviceMetaInfo {

	private String deviceCode;
	private String deviceServiceVersion;
	private DigitalId digitalId;
}
