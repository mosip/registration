package io.mosip.registration.mdm.spec_0_9_2.dto.response;

import io.mosip.registration.mdm.dto.Error;
import lombok.Data;

@Data
public class DeviceInfoResponse {

	private String deviceInfo;

	private Error error;
	
	

}
