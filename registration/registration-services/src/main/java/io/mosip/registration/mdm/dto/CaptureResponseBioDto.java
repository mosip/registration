package io.mosip.registration.mdm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaptureResponseBioDto {

	
	private String specVersion;
	@JsonIgnore
	private CaptureResponsBioDataDto captureResponseData;
	@JsonProperty("data")
	private String captureBioData;
	private String hash;
}
