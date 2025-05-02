package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

/**
 * The Class MessageExpiredException.
 */
public class DuplicateTransactionException extends BaseUncheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new message expired exception.
	 */
	public DuplicateTransactionException() {
		super();
	}

	/**
	 * Instantiates a new message expired exception.
	 *
	 * @param message
	 *            the message
	 */
	public DuplicateTransactionException(String message) {
		super(PlatformErrorMessages.RPR_SYS_DUPLICATE_TRANSACTION.getCode(),
			PlatformErrorMessages.RPR_SYS_DUPLICATE_TRANSACTION.getMessage() + " " + message);
	}

	/**
	 * Instantiates a new message expired exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public DuplicateTransactionException(String message, Throwable cause) {
		super(PlatformErrorMessages.RPR_SYS_DUPLICATE_TRANSACTION.getCode() + EMPTY_SPACE, message, cause);
	}

}
