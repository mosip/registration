package io.mosip.registration.processor.core.exception;

import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class UnsupportedEventBusTypeException extends RegistrationProcessorCheckedException {

	/**
	 *	serialVersionUID for verfication while deserializing
	 */
	private static final long serialVersionUID = -561425277680618876L;

	/**
	 * Instantiates a Unsupported event type exception.
	 */
	public UnsupportedEventBusTypeException() {
		super();
	}

	/**
	 * Instantiates a Unsupported event type  exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 */
	public UnsupportedEventBusTypeException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Instantiates a Unsupported event type exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 * @param rootCause    the root cause
	 */
	public UnsupportedEventBusTypeException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Instantiates a Unsupported event type exception.
	 *
	 * @param exceptionConstant the exception constant
	 */
	public UnsupportedEventBusTypeException(PlatformErrorMessages exceptionConstant) {
		this(exceptionConstant.getCode(), exceptionConstant.getMessage());
	}

	/**
	 * Instantiates a Unsupported event type exception.
	 *
	 * @param exceptionConstant the exception constant
	 * @param rootCause         the root cause
	 */
	public UnsupportedEventBusTypeException(PlatformErrorMessages exceptionConstant, Throwable rootCause) {
		this(exceptionConstant.getCode(), exceptionConstant.getMessage(), rootCause);
	}

}
