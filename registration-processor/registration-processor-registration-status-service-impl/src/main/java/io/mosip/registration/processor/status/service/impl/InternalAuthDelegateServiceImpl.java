package io.mosip.registration.processor.status.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.auth.dto.AuthRequestDTO;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.IndividualIdDto;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
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

	private static final String APPID = "regproc";

	/** The rest api client. */
	@Autowired
	private RestApiClient restApiClient;

	@Autowired
	RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	@Qualifier("selfTokenRestTemplate")
	private RestTemplate restTemplate;

	/** The internal auth uri. */
	@Value("${ida-internal-auth-uri}")
	private String internalAuthUri;

	/** The get certificate uri. */
	@Value("${ida-internal-get-certificate-uri}")
	private String getCertificateUri;

	@Autowired
	ObjectMapper mapper;

	/**
	 * Authenticate.
	 *
	 * @param authRequestDTO the auth request DTO
	 * @param headers        the headers
	 * @return the AuthResponseDTO
	 * @throws Exception
	 */
	@Override
	public AuthResponseDTO authenticate(AuthRequestDTO authRequestDTO, HttpHeaders headers) throws Exception {

		// get individualId from userId
		String individualId = getIndividualIdByUserId(authRequestDTO.getIndividualId());

		authRequestDTO.setIndividualId(individualId);
		authRequestDTO.setIndividualIdType(null);

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(internalAuthUri);
		HttpEntity<?> httpRequestEntity = new HttpEntity<Object>(authRequestDTO, headers);
		return postApi(builder.toUriString(), MediaType.APPLICATION_JSON, httpRequestEntity, AuthResponseDTO.class)
				.getBody();
	}

	/**
	 * Gets the certificate.
	 *
	 * @param applicationId the application id
	 * @param referenceId   the reference id
	 * @param headers       the headers
	 * @return the certificate
	 * @throws Exception
	 */
	@Override
	public Object getCertificate(String applicationId, Optional<String> referenceId, HttpHeaders headers)
			throws Exception {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getCertificateUri);
		builder.queryParam(APPLICATION_ID, applicationId);
		referenceId.ifPresent(refId -> builder.queryParam(REFERENCE_ID, refId));
		return restApiClient.getApi(builder.build().toUri(), Object.class);
	}

	public <T> HttpEntity<T> postApi(String uri, MediaType mediaType, HttpEntity<?> requestEntity,
			Class<T> responseClass) throws Exception {
		try {
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

	/**
	 * get the individualId by userid
	 * 
	 * @param userid
	 * @return individualId
	 * @throws ApisResourceAccessException
	 * @throws IOException
	 */
	private String getIndividualIdByUserId(String userid) throws ApisResourceAccessException, IOException {

		logger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), userid,
				"InternalAuthDelegateServiceImpl::getIndividualIdByUserId()::entry");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(APPID);
		pathSegments.add(userid);
		String individualId = null;
		ResponseWrapper<?> response = null;
		try {
			response = (ResponseWrapper<?>) restClientService.getApi(ApiName.GETINDIVIDUALIDFROMUSERID, pathSegments, "",
					"", ResponseWrapper.class);
			logger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"getIndividualIdByUserId called for with GETINDIVIDUALIDFROMUSERID GET service call ended successfully");
			if (response.getErrors() != null) {
				throw new ApisResourceAccessException(
						PlatformErrorMessages.LINK_FOR_USERID_INDIVIDUALID_FAILED_STATUS_EXCEPTION.getMessage());
			} else {
				IndividualIdDto readValue = mapper.readValue(mapper.writeValueAsString(response.getResponse()),
						IndividualIdDto.class);
				individualId = readValue.getIndividualId();
			}
		} catch (ApisResourceAccessException e) {
			throw new ApisResourceAccessException(
					PlatformErrorMessages.UNABLE_TO_ACCESS_API.getMessage());
		} catch (IOException e) {
			throw new IOException(PlatformErrorMessages.RPR_RGS_IOEXCEPTION.getMessage());
		}

		logger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), userid,
				"InternalAuthDelegateServiceImpl::getIndividualIdByUserId()::exit");
		return individualId;
	}

}
