package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class MessageSenderException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new MessageSenderException.
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 */
	public MessageSenderException(String errorCode, String message) {
        super(errorCode, message);
    }

	/**
	 * Instantiates a new MessageSenderException.
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 * @param t         the t
	 */
	public MessageSenderException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
