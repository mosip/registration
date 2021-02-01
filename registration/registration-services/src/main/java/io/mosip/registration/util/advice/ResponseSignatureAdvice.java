package io.mosip.registration.util.advice;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.mosip.commons.packet.constants.CryptomanagerConstant;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.keymanager.exception.KeystoreProcessingException;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.exception.KeymanagerServiceException;
import io.mosip.kernel.keymanagerservice.exception.NoUniqueAliasException;
import io.mosip.kernel.signature.dto.TimestampRequestDto;
import io.mosip.kernel.signature.service.SignatureService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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

	private static final String CERTIFICATE_API_PATH = "/v1/syncdata/getCertificate";

	@Value("${mosip.utc-datetime-pattern:yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}")
	private String DATETIME_PATTERN;

	@Value("${mosip.sign.refid:SIGN}")
	private String signRefId;

	@Autowired
    private SignatureService signatureService;
	
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
	 * The above three values are passed to the {@link SignatureService} where the
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

			LinkedHashMap<String, Object> responseBodyMap = (LinkedHashMap<String, Object>) restClientResponse
					.get(RegistrationConstants.REST_RESPONSE_BODY);

			if (null != requestDto && requestDto.getIsSignRequired() && null != responseBodyMap && responseBodyMap.size() > 0
					&& null != responseBodyMap.get(RegistrationConstants.RESPONSE)) {
				if (responseBodyMap.get(RegistrationConstants.RESPONSE) instanceof LinkedHashMap){
					LinkedHashMap<String, Object> resp = (LinkedHashMap<String, Object>) responseBodyMap
							.get(RegistrationConstants.RESPONSE);
					checkAndUploadCertificate(resp, joinPoint);
				}

				responseHeader = (HttpHeaders) restClientResponse.get(RegistrationConstants.REST_RESPONSE_HEADERS);

				DateTimeFormatter format = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
				LocalDateTime localdatetime = LocalDateTime.parse(DateUtils.getUTCCurrentDateTimeString(DATETIME_PATTERN), format);
				TimestampRequestDto timestampRequestDto = new TimestampRequestDto();
				timestampRequestDto.setSignature(responseHeader.get(RegistrationConstants.RESPONSE_SIGNATURE).get(0));
				timestampRequestDto.setData(new ObjectMapper().writeValueAsString(responseBodyMap));
				timestampRequestDto.setTimestamp(localdatetime);

				//TODO - Change it to JWT signature verification
				if (signatureService.validate(timestampRequestDto).getStatus().equalsIgnoreCase(CryptomanagerConstant.SIGNATURES_SUCCESS)) {
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
		} catch (RuntimeException | JsonProcessingException regBaseCheckedException) {
			LOGGER.error(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
			throw new RegBaseCheckedException("Exception in response signature", regBaseCheckedException.getMessage());
		}

		LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
				"successfully leaving response signature method...");

		return restClientResponse;

	}

	/**
	 * Checks if this is Sign certificate fetch API call
	 * if yes, it saves the certificate in keystore
	 * @param resp
	 * @param joinPoint
	 */
	private void checkAndUploadCertificate(LinkedHashMap<String, Object> resp, JoinPoint joinPoint) {
		if(joinPoint.getArgs() != null && joinPoint.getArgs() instanceof Object[] && joinPoint.getArgs()[0] != null) {
			RequestHTTPDTO requestDto = (RequestHTTPDTO) joinPoint.getArgs()[0];

			UriComponents uriComponents = UriComponentsBuilder.fromUri(requestDto.getUri()).build();
			if(!(uriComponents.getPath().equals(CERTIFICATE_API_PATH) &&
					uriComponents.getQueryParams().containsKey(RegistrationConstants.GET_CERT_APP_ID) &&
					uriComponents.getQueryParams().getFirst(RegistrationConstants.GET_CERT_APP_ID).equals(RegistrationConstants.KERNEL_APP_ID) &&
					uriComponents.getQueryParams().containsKey(RegistrationConstants.REF_ID) &&
					uriComponents.getQueryParams().getFirst(RegistrationConstants.REF_ID).equals(signRefId))) {
				return;
			}

			try {
				KeyPairGenerateResponseDto keyPairGenerateResponseDto = keymanagerService.getCertificate(RegistrationConstants.KERNEL_APP_ID,
						Optional.of(signRefId));
			} catch (BaseUncheckedException exception) {
				//TODO - Need to get exact exception on expire and revocation
				if(!exception.getErrorCode().equals("KER-KMS-012")) //Key Generation Process is not completed.
				{
					LOGGER.error(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
							ExceptionUtils.getStackTrace(exception));
					return;
				}

				UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
				uploadCertRequestDto.setApplicationId(uriComponents.getQueryParams().getFirst(RegistrationConstants.GET_CERT_APP_ID));
				uploadCertRequestDto.setCertificateData(resp.get(RegistrationConstants.CERTIFICATE).toString());
				uploadCertRequestDto.setReferenceId(signRefId);
				keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);
				LOGGER.info(LoggerConstants.RESPONSE_SIGNATURE_VALIDATION, APPLICATION_ID, APPLICATION_NAME,
						"Uploaded certificate with request..." + uploadCertRequestDto);
			}
		}
	}

}
