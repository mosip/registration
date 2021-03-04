package io.mosip.registration.mdm.dto;

public enum MDMError {
	
	PARSE_ERROR("REG-MDM-101", "JSON parsing error"),
	DEVICE_NOT_REGISTERED("REG-MDM-102", "Device not registered"),
	UNSUPPORTED_SPEC("REG-MDM-103", "Unsupported SpecVersion"),
	DEVICE_NOT_FOUND("REG-MDM-104", "Device not found"),
	MDM_REQUEST_FAILED("REG-MDM-105", "MDM request Failed : ");
	
	MDMError(String errorCode, String errorMessage) {
		this.setErrorCode(errorCode);
		this.setErrorMessage(errorMessage);
	}
	
	private String errorCode;
	private String errorMessage;
	
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
