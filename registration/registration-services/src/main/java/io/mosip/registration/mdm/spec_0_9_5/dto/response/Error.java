package io.mosip.registration.mdm.spec_0_9_5.dto.response;

import lombok.Data;

@Data
public class Error {

	// According to spec errorcode,errorinfo
	private String errorCode;
	private String errorInfo;

}
