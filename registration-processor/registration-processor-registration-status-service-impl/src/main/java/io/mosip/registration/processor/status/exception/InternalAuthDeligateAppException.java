package io.mosip.registration.processor.status.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;


public class InternalAuthDeligateAppException extends BaseUncheckedException{
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6748760277721155095L;

	/** The id. */
	private String id;

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Instantiates a new id repo app exception.
	 */
	public InternalAuthDeligateAppException() {
		super();
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param errorCode the error code
	 * @param errorMessage the error message
	 */
	public InternalAuthDeligateAppException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param errorCode the error code
	 * @param errorMessage the error message
	 * @param rootCause the root cause
	 */
	public InternalAuthDeligateAppException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}
	
	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param errorCode the error code
	 * @param errorMessage the error message
	 * @param rootCause the root cause
	 * @param id the id
	 */
	public InternalAuthDeligateAppException(String errorCode, String errorMessage, Throwable rootCause, String id) {
		super(errorCode, errorMessage, rootCause);
		this.id = id;
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param exceptionConstant the exception constant
	 */
	public InternalAuthDeligateAppException(PlatformErrorMessages exceptionConstant) {
		this(exceptionConstant.getCode(), exceptionConstant.getMessage());
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param exceptionConstant the exception constant
	 * @param rootCause the root cause
	 */
	public InternalAuthDeligateAppException(PlatformErrorMessages exceptionConstant, Throwable rootCause) {
		this(exceptionConstant.getCode(), exceptionConstant.getMessage(), rootCause);
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param exceptionConstant the exception constant
	 * @param id the id
	 */
	public InternalAuthDeligateAppException(PlatformErrorMessages exceptionConstant, String id) {
		this(exceptionConstant.getCode(), exceptionConstant.getMessage());
		this.id = id;
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param exceptionConstant the exception constant
	 * @param rootCause the root cause
	 * @param id the id
	 */
	public InternalAuthDeligateAppException(PlatformErrorMessages exceptionConstant, Throwable rootCause, String id) {
		this(exceptionConstant.getCode(), exceptionConstant.getMessage(), rootCause);
		this.id = id;
	}
}
