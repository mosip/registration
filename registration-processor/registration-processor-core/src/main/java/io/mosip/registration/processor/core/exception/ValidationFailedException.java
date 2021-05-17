package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class ValidationFailedException extends BaseCheckedException {

	private static final long serialVersionUID = 1L;

	public ValidationFailedException(String errorCode, String message) {
		super(errorCode, message);
	}

	public ValidationFailedException(String errorCode, String message, Throwable t) {
		super(errorCode, message, t);
	}

}
