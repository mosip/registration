package io.mosip.registration.processor.status.service;

import java.util.Optional;

import org.springframework.http.HttpHeaders;

import io.mosip.registration.processor.core.auth.dto.AuthRequestDTO;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;

/**
 * The Interface InternalAuthDelegateService - the service that delegates
 * the calls to ID-Authentication's internal auth APIs
 * 
 * @author Loganathan Sekar
 */
public interface InternalAuthDelegateService {
	
	/**
	 * Authenticate.
	 *
	 * @param authRequestDTO the auth request DTO
	 * @param headers 
	 * @return the AuthResponseDTO
	 * @throws Exception 
	 */
	public AuthResponseDTO authenticate(AuthRequestDTO authRequestDTO, HttpHeaders headers) throws Exception;

	/**
	 * Gets the certificate.
	 *
	 * @param applicationId the application id
	 * @param referenceId the reference id
	 * @param headers the headers
	 * @return the certificate
	 * @throws Exception 
	 */
	Object getCertificate(String applicationId, Optional<String> referenceId, HttpHeaders headers) throws Exception;

}
