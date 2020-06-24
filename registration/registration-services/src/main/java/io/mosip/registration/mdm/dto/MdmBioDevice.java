package io.mosip.registration.mdm.dto;

import lombok.Data;

@Data
public class MdmBioDevice {

	private String deviceType;
	private String deviceSubType;
	private String status;
	private String providerName;
	private String providerId;
	private String serialVersion;
	private String certification;
	private String callbackId;
	private String deviceModel;
	private String deviceMake;
	private String firmWare;
	private String deviceExpiry;
	private String deviceId;
	private String deviceCode;
	private String serialNumber;
	private String[] deviceSubId;
	private String deviceProviderName;
	private String deviceProviderId;
	private String purpose;
	private String specVersion;
	private String timestamp;
	private int port;

}
