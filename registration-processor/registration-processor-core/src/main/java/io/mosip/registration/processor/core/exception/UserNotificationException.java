package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class UserNotificationException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new UserNotificationException.
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 */
	public UserNotificationException(String errorCode, String message) {
        super(errorCode, message);
    }

	/**
	 * Instantiates a new UserNotificationException.
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 * @param t         the t
	 */
	public UserNotificationException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
