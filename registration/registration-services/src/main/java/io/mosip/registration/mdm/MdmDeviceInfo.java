package io.mosip.registration.mdm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MdmDeviceInfo {
	private String[] specVersion;
	private String deviceStatus;
	private String deviceId;
	private String firmware;
	private String certification;
	private String serviceVersion;
	private int[] deviceSubId;
	private String callbackId;
	private String digitalId;
	private String deviceCode;
	private String purpose;

}
