package io.mosip.registration.mdm.spec_0_9_2.dto.request;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class StreamRequestDTO {

	private String env;
	private String mosipProcess;
	private String version;
	private int timeout;
	@JsonIgnore
	private String captureTime;
	@JsonIgnore
	private String registrationID;

	@JsonProperty("bio")
	private List<StreamBioRequestDTO> mosipBioRequest;

	private List<Map<String, String>> customOpts;
}
