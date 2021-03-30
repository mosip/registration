package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

/**
 * The Class MessageExpiredException.
 */
public class MessageExpiredException extends BaseUncheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new message expired exception.
	 */
	public MessageExpiredException() {
		super();
	}

	/**
	 * Instantiates a new message expired exception.
	 *
	 * @param message
	 *            the message
	 */
	public MessageExpiredException(String message) {
		super(PlatformErrorMessages.RPR_SYS_MESSAGE_EXPIRED.getCode(), 
			PlatformErrorMessages.RPR_SYS_MESSAGE_EXPIRED.getMessage() + " " + message);
	}

	/**
	 * Instantiates a new message expired exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public MessageExpiredException(String message, Throwable cause) {
		super(PlatformErrorMessages.RPR_SYS_MESSAGE_EXPIRED.getCode() + EMPTY_SPACE, message, cause);
	}

}
