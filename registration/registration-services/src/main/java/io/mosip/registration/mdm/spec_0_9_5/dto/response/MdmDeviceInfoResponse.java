package io.mosip.registration.mdm.spec_0_9_5.dto.response;

import lombok.Data;

@Data
public class MdmDeviceInfoResponse {

	private Error error;
	private String deviceInfo;
}
