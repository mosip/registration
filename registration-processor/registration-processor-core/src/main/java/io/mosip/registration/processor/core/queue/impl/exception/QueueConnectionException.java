package io.mosip.registration.processor.core.queue.impl.exception;

import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class QueueConnectionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private  static String EMPTY_SPACE=" ";

	public  QueueConnectionException() {
		super();
	}

	public  QueueConnectionException(String errorMsg) {
		super(PlatformErrorMessages.RPR_SYS_QUEUE_CONNECTION_EXCEPTION.getCode() + EMPTY_SPACE +errorMsg);
	}

}
