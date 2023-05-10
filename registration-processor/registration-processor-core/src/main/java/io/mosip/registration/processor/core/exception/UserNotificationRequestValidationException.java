package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;

public class UserNotificationRequestValidationException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new UserNotificationRequestValidationException
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 */
	public UserNotificationRequestValidationException(String errorCode, String message) {
        super(errorCode, message);
    }

	/**
	 * Instantiates a new UserNotificationRequestValidationException
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 * @param t         the t
	 */
	public UserNotificationRequestValidationException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
