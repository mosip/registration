package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class PacketValidatorException extends BaseCheckedException {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	public PacketValidatorException(String errorCode, String message, Throwable t) {
		super(errorCode, message, t);
	}
}
