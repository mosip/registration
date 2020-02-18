package io.mosip.registration.dto.json.metadata;

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
