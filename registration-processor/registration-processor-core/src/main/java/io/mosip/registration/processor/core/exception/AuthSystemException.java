/**
 * 
 */
package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

/**
 * @author M1022006
 *
 */
public class AuthSystemException extends BaseCheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	
	public AuthSystemException() {
		super();
	}

	
	public AuthSystemException(String message) {
		super(PlatformErrorMessages.RPR_AUTH_SYSTEM_EXCEPTION.getCode(), message);
	}

	
	public AuthSystemException(String message, Throwable cause) {
		super(PlatformErrorMessages.RPR_AUTH_SYSTEM_EXCEPTION.getCode(), message, cause);
	}
	
	
	
}
