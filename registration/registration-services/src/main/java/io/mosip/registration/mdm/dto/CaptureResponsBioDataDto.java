package io.mosip.registration.mdm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.mosip.registration.dto.json.metadata.DigitalId;
import lombok.Getter;
import lombok.Setter;

/**
 * Holds the captured biometric value from the device
 * 
 * @author balamurugan.ramamoorthy
 *
 */
@Getter
@Setter
public class CaptureResponsBioDataDto {
	@JsonIgnore
	private DigitalId digitalIdDecoded;
	private String digitalId;
	private String deviceCode;
	private String deviceServiceVersion;
	private String bioSubType;
	private String purpose;
	private String env;
	private String bioValue;
	private String bioExtract;
	private String transactionId;
	private String timestamp;
	private String requestedScore;
	private String qualityScore;

}
