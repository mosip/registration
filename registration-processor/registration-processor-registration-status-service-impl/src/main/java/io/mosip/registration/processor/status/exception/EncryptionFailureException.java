package io.mosip.registration.processor.status.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class EncryptionFailureException extends BaseCheckedException{

	/** Serializable version Id. */
	private static final long serialVersionUID = 1L;

	/**
	 * @param code
	 *            Error Code Corresponds to Particular Exception
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public EncryptionFailureException(String code, String message, Throwable cause) {
		super(code, message, cause);

	}

	public EncryptionFailureException(String errorCode, String errorMessage) {
		super(errorCode,errorMessage);
	}

}