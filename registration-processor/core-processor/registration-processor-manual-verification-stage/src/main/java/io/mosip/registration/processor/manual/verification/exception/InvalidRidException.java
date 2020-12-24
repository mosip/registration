package io.mosip.registration.processor.manual.verification.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class InvalidRidException  extends BaseUncheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new invalid rid exception.
	 *
	 * @param code the code
	 * @param message the message
	 */
	public InvalidRidException(String code, String message){
		super(code, message);
	}



}
