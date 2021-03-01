package io.mosip.registration.mdm.spec_0_9_2.dto.response;

import lombok.Data;

@Data
public class DeviceDiscoveryMDSResponse {

	private String deviceId;
	private String deviceStatus;
	private String certification;
	private String serviceVersion;
	private String[] deviceSubId;
	private String callbackId;
	private String digitalId;
	private String deviceCode;
	private String[] specVersion;
	private String purpose;
	private Error error;
}
