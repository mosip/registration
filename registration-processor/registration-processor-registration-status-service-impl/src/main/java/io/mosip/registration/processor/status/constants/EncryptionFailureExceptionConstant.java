package io.mosip.registration.processor.status.constants;

/**
 * The Enum EncryptionFailureExceptionConstant.
 */
public enum EncryptionFailureExceptionConstant {
	
	/** The mosip encryption failure error code. */
	MOSIP_ENCRYPTION_FAILURE_ERROR_CODE("RPR-PKD-004", "The Encryption for the Data has failed");

	/** The error code. */
	public final String errorCode;

	/** The error message. */
	public final String errorMessage;

	/**
	 * Instantiates a new encryption failure exception constant.
	 *
	 * @param string1 the string 1
	 * @param string2 the string 2
	 */
	EncryptionFailureExceptionConstant(String string1,String string2) {
		this.errorCode = string1;
		this.errorMessage = string2;
	}

	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Gets the error message.
	 *
	 * @return the error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

}