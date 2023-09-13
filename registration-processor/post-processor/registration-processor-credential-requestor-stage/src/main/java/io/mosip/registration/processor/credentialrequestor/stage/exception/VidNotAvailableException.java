<<<<<<<< HEAD:registration-processor/post-processor/registration-processor-credential-requestor-stage/src/main/java/io/mosip/registration/processor/credentialrequestor/stage/exception/VidNotAvailableException.java
package io.mosip.registration.processor.credentialrequestor.stage.exception;
========
package io.mosip.registration.processor.eventhandler.stage.exception;
>>>>>>>> df41852ca05 (MOSIP-28121 : renamed print stage to event handler stage):registration-processor/post-processor/registration-processor-event-handler-stage/src/main/java/io/mosip/registration/processor/eventhandler/stage/exception/VidNotAvailableException.java

import io.mosip.kernel.core.exception.BaseCheckedException;

public class VidNotAvailableException extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public VidNotAvailableException() {
		super();
	}

	/**
	 * Instantiates a new reg proc checked exception.
	 *
	 * @param errorCode
	 *            the error code
	 * @param errorMessage
	 *            the error message
	 */
	public VidNotAvailableException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
