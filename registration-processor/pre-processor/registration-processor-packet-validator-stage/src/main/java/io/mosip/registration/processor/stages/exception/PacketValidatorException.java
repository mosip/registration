package io.mosip.registration.processor.stages.exception;

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
	 * @param message the message
	 * @param cause the cause
	 */
	public PacketValidatorException( Throwable cause) {
		super( cause);
	}
}
