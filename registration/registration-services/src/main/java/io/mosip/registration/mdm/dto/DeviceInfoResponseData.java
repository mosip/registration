package io.mosip.registration.mdm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds the dEvice details response from the MDM service
 * 
 * @author balamurugan.ramamoorthy
 *
 */
@Getter
@Setter
public class DeviceInfoResponseData {

	@JsonIgnore
	private DeviceInfo deviceInfoDecoded;
	private String deviceInfo;
	private Error error;

}
