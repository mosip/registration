package io.mosip.registration.processor.credentialrequestor.stage.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class VidNotAvailableException extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VidNotAvailableException() {
		super();
	}

	/**
	 * Instantiates a new reg proc checked exception.
	 *
	 * @param errorCode
	 *            the error code
	 * @param errorMessage
	 *            the error message
	 */
	public VidNotAvailableException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
