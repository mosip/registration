package io.mosip.registration.processor.status.exception;

import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

/**
 * 
 * @author Dhanendra
 *
 */
public class LostRidValidationException extends RegStatusAppException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -2685586188884469940L;

	/**
	 * Instantiates a new id repo data validation exception.
	 */
	public LostRidValidationException() {
		super();
	}

	/**
	 * Instantiates a new id repo data validation exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 */
	public LostRidValidationException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Instantiates a new id repo data validation exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 * @param rootCause    the root cause
	 */
	public LostRidValidationException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Instantiates a new id repo data validation exception.
	 *
	 * @param exceptionConstant the exception constant
	 */
	public LostRidValidationException(PlatformErrorMessages exceptionConstant) {
		super(exceptionConstant);
	}

	/**
	 * Instantiates a new id repo data validation exception.
	 *
	 * @param exceptionConstant the exception constant
	 * @param rootCause         the root cause
	 */
	public LostRidValidationException(PlatformErrorMessages exceptionConstant, Throwable rootCause) {
		super(exceptionConstant, rootCause);
	}
}
