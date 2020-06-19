package io.mosip.registration.mdm.spec_0_9_2.dto.response;

import lombok.Data;

@Data
public class DigitalId {
	private String serialNo;
	private String make;
	private String model;
	private String type;
	private String subType;
	private String deviceProviderId;
	private String deviceProvider;
	private String dateTime;

}
