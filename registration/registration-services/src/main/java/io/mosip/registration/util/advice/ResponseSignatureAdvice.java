package io.mosip.registration.util.advice;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.packet.spi.IPacketCryptoService;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.restclient.RequestHTTPDTO;

/**
 * All the responses of the rest call services which are invoking from the
 * reg-client will get signed from this class.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 *
 */
@Aspect
@Component
public class ResponseSignatureAdvice {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = AppConfig.getLogger(ResponseSignatureAdvice.class);

	@Autowired
    @Qualifier("OfflinePacketCryptoServiceImpl")
    private IPacketCryptoService offlinePacketCryptoServiceImpl;
	
	@Autowired
	private KeymanagerService keymanagerService;

	/**
	 * <p>
	 * It is an after returning method in which for each and everytime after
	 * successfully invoking the
	 * "io.mosip.registration.util.restclient.RestClientUtil.invoke()" method, this
	 * method will be called.
	 * </p>
	 * 
	 * Here we are passing three arguments as parameters
	 * <ol>
	 * <li>SignIn Key - Public Key from Kernel</li>
	 * <li>Response - Signature from response header</li>
	 * <li>Response Body - Getting from the Service response</li>
	 * </ol>
	 * 
	 * The above three values are passed to the {@link SignatureUtil} where the
	 * validation will happen for the response that we send
	 * 
	 * @param joinPoint - the JointPoint
	 * @param result    - the object result
	 * @return the rest client response as {@link Map}
	 * @throws RegBaseCheckedException - the exception class that handles all the
	 *                                 checked exceptions
	 */
	@SuppressWarnings("unchecked")
	@AfterReturning(pointcut = "execution(* io.mosip.registration.util.restclient.RestClientUtil.invoke(..))", returning = "result")
	public synchronized Map<String, Object> responseSignatureValidation(JoinPoint joinPoint, Object result)
			throws RegBaseCheckedException {

		LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
				"Entering into response signature method");

		HttpHeaders responseHeader = null;
		Object[] requestHTTPDTO = joinPoint.getArgs();
		RequestHTTPDTO requestDto = (RequestHTTPDTO) requestHTTPDTO[0];
		LinkedHashMap<String, Object> restClientResponse = null;
		try {
			restClientResponse = (LinkedHashMap<String, Object>) result;

			LinkedHashMap<String, Object> keyResponse = (LinkedHashMap<String, Object>) restClientResponse
					.get(RegistrationConstants.REST_RESPONSE_BODY);

			if (null != requestDto && requestDto.getIsSignRequired() && null != keyResponse && keyResponse.size() > 0
					&& null != keyResponse.get(RegistrationConstants.RESPONSE)) {
				if (keyResponse.get(RegistrationConstants.RESPONSE) instanceof LinkedHashMap){
					LinkedHashMap<String, Object> resp = (LinkedHashMap<String, Object>) keyResponse
							.get(RegistrationConstants.RESPONSE);
					if (resp.containsKey(RegistrationConstants.CERTIFICATE) && resp.get(RegistrationConstants.CERTIFICATE) != null) {
						if(joinPoint.getArgs() != null && joinPoint.getArgs() instanceof Object[] && joinPoint.getArgs()[0] != null) {
							RequestHTTPDTO request = (RequestHTTPDTO) joinPoint.getArgs()[0];
							Map<String, String> queryPairs = new LinkedHashMap<String, String>();
						    String query = request.getUri().getQuery();
						    String[] pairs = query.split("&");
						    for (String pair : pairs) {
						        int index = pair.indexOf("=");
						        queryPairs.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"), URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
						    }
						    LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
									"Extracted query params from the request to upload certificate..." + queryPairs);
						    
							if (queryPairs.get(RegistrationConstants.REF_ID).equals(RegistrationConstants.KER)) {
								UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
								uploadCertRequestDto.setApplicationId(queryPairs.get(RegistrationConstants.GET_CERT_APP_ID));
								uploadCertRequestDto.setCertificateData(resp.get(RegistrationConstants.CERTIFICATE).toString());
								uploadCertRequestDto.setReferenceId(RegistrationConstants.KERNEL_REF_ID);
								keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);
								
								LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
										"Uploaded certificate with request..." + uploadCertRequestDto);
							}
						}
					}
				}

				LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
						requestDto.getUri().getPath().replaceAll("/", "====>"));

				LinkedHashMap<String, Object> responseBodyMap = (LinkedHashMap<String, Object>) restClientResponse
						.get(RegistrationConstants.REST_RESPONSE_BODY);

				responseHeader = (HttpHeaders) restClientResponse.get(RegistrationConstants.REST_RESPONSE_HEADERS);
				
				if (offlinePacketCryptoServiceImpl.verify(
						new ObjectMapper().writeValueAsString(responseBodyMap).getBytes(),
						responseHeader.get(RegistrationConstants.RESPONSE_SIGNATURE).get(0).getBytes())) {
					LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
							"response signature is valid...");
					return restClientResponse;
				} else {
					LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
							"response signature is Invalid...");
					restClientResponse.put(RegistrationConstants.REST_RESPONSE_BODY, new LinkedHashMap<>());
					restClientResponse.put(RegistrationConstants.REST_RESPONSE_HEADERS, new LinkedHashMap<>());
				}
			}
		} catch (RuntimeException | JsonProcessingException | UnsupportedEncodingException regBaseCheckedException) {
			LOGGER.error(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
			throw new RegBaseCheckedException("Exception in response signature", regBaseCheckedException.getMessage());
		}

		LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
				"successfully leaving response signature method...");

		return restClientResponse;

	}

}
