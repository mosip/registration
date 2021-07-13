package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;


/**
 * The Class WorkflowInternalActionException.
 */
public class WorkflowInternalActionException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new workflow internal action exception.
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 */
	public WorkflowInternalActionException(String errorCode, String message) {
        super(errorCode, message);
    }

	/**
	 * Instantiates a new workflow internal action exception.
	 *
	 * @param errorCode the error code
	 * @param message   the message
	 * @param t         the t
	 */
	public WorkflowInternalActionException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
