package io.mosip.registration.processor.packet.utility.constants;

public enum PacketUtilityErrorCodes {

	UNKNOWN_RESOURCE_EXCEPTION("KER-PUT-001",
			"Unknown resource provided"),
	FILE_NOT_FOUND_IN_DESTINATION("KER-PUT-002", "Unable to Find File in Destination Folder"),
	SYS_IO_EXCEPTION("KER-PUT-002", "Unable to Find File in Destination Folder");


	private final String errorCode;
	private final String errorMessage;

	private PacketUtilityErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
