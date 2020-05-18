package io.mosip.registration.processor.core.exception;

public class PacketValidatorException extends Exception{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new apis resource access exception.
	 */
	public PacketValidatorException() {
		super();
	}


	/**
	 * Instantiates a new apis resource access exception.
	 *
	 * @param cause the cause
	 */
	public PacketValidatorException( Throwable cause) {
		super( cause);
	}
}
