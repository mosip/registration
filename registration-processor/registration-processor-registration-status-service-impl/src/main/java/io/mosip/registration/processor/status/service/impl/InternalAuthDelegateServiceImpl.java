package io.mosip.registration.processor.status.service.impl;

import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.status.service.InternalAuthDelegateService;

/**
 * The Class InternalAuthDelegateServiceImpl - The implementation that delegates
 * the calls to ID-Authentication's internal auth APIs.
 *
 * @author Loganathan.Sekar
 */
@Component
public class InternalAuthDelegateServiceImpl implements InternalAuthDelegateService {
	
	private final Logger logger = RegProcessorLogger.getLogger(InternalAuthDelegateServiceImpl.class);

	private static final String REFERENCE_ID = "referenceId";

	private static final String APPLICATION_ID = "applicationId";

	/** The rest api client. */
	@Autowired
	private RestApiClient restApiClient;
	
	/** The internal auth uri. */
	@Value("${ida-internal-auth-uri}")
	private String internalAuthUri;
	
	/** The get certificate uri. */
	@Value("${ida-internal-get-certificate-uri}")
	private String getCertificateUri;
	
	/**
	 * Authenticate.
	 *
	 * @param authRequestDTO the auth request DTO
	 * @param headers the headers
	 * @return the object
	 * @throws Exception 
	 */
	@Override
	public HttpEntity<Object> authenticate(Object authRequestDTO, HttpHeaders headers) throws Exception {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(internalAuthUri);
		HttpEntity<?> httpRequestEntity = new HttpEntity<Object>(authRequestDTO, headers);
		return postApi(builder.toUriString(), MediaType.APPLICATION_JSON, httpRequestEntity, Object.class);
	}

	/**
	 * Gets the certificate.
	 *
	 * @param applicationId the application id
	 * @param referenceId   the reference id
	 * @param headers the headers
	 * @return the certificate
	 * @throws Exception 
	 */
	@Override
	public Object getCertificate(String applicationId, Optional<String> referenceId, HttpHeaders headers) throws Exception {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCertificateUri);
		builder.queryParam(APPLICATION_ID, applicationId);
		referenceId.ifPresent(refId -> builder.queryParam(REFERENCE_ID, refId));
		return restApiClient.getApi(builder.build().toUri(), Object.class);
	}
	
	public <T> HttpEntity<T> postApi(String uri, MediaType mediaType, HttpEntity<?> requestEntity, Class<T> responseClass) throws Exception {
		RestTemplate restTemplate;
		try {
			restTemplate = restApiClient.getRestTemplate();
			logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), uri);
			return restTemplate.exchange(uri, HttpMethod.POST, requestEntity, responseClass);
		} catch (Exception e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			restApiClient.tokenExceptionHandler(e);
			throw e;
		}
	}

}
