package io.mosip.registration.processor.stages.uingenerator.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class VidServiceFailedException extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VidServiceFailedException() {
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
	public VidServiceFailedException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
