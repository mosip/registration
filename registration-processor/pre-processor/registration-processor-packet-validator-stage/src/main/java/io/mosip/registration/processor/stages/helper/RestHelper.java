package io.mosip.registration.processor.stages.helper;

import java.util.function.Supplier;

import io.mosip.registration.processor.stages.dto.RestRequestDTO;

/*
 * The class RestHelper
 */
public interface RestHelper {

	/**
	 * Request to send/receive HTTP requests and return the response asynchronously.
	 *
	 * @param request
	 *            the request
	 * @return the supplier
	 */

	Supplier<Object> requestAsync(RestRequestDTO request);
}
