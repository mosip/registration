package io.mosip.registration.dto.json.metadata;

import lombok.Data;

@Data
public class CustomDigitalId {

	private String serialNo;
	private String make;
	private String model;
	private String type;
	private String subType;
	private String dpId;
	private String dp;
	private String dateTime;
}
