package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class IntroducerOnHoldException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new IntroducerValidationException .
	 */
	public IntroducerOnHoldException() {
		super();
	}

	/**
	 * 
	 * @param message
	 */
	public IntroducerOnHoldException(String code,String message) {
		super(code, message);
	}

}
