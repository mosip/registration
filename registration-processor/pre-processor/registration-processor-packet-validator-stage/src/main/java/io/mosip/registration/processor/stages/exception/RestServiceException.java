package io.mosip.registration.processor.stages.exception;

import java.util.Optional;

import io.mosip.registration.processor.core.exception.ApisResourceAccessException;

/*
 * The class RestServiceException
 */

public class RestServiceException extends ApisResourceAccessException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 372518972095526748L;

	/** The response body. */
	private transient String responseBodyAsString;

	private transient Object responseBody;

	/**
	 * Instantiates a new rest service exception.
	 */
	public RestServiceException() {
		super();
	}

	/**
	 * Instantiates a new rest client exception.
	 *
	 * @param exceptionConstant
	 *            the exception constant
	 */
	public RestServiceException(String exceptionConstant) {
		super(exceptionConstant);
	}

	/**
	 * Instantiates a new rest client exception.
	 *
	 * @param exceptionConstant
	 *            the exception constant
	 * @param rootCause
	 *            the root cause
	 */
	public RestServiceException(String exceptionConstant, Throwable rootCause) {
		super(exceptionConstant, rootCause);
	}

	/**
	 * Instantiates a new rest service exception.
	 *
	 * @param exceptionConstant
	 *            the exception constant
	 * @param responseBodyAsString
	 *            the response body as string
	 * @param responseBody
	 *            the response body
	 */
	public RestServiceException(String exceptionConstant, String responseBodyAsString, Object responseBody) {
		super(exceptionConstant);
		this.responseBody = responseBody;
		this.responseBodyAsString = responseBodyAsString;
	}

	/**
	 * Gets the response body.
	 *
	 * @return the response body
	 */
	public Optional<Object> getResponseBody() {
		return Optional.ofNullable(responseBody);
	}

	public Optional<String> getResponseBodyAsString() {
		return Optional.ofNullable(responseBodyAsString);
	}
}
