package io.mosip.registration.util.advice;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.registration.repositories.UserTokenRepository;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.LoginMode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.MachineMappingDAO;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.LoginUserDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

/**
 * The Class RestClientAuthAdvice checks whether the invoking REST service
 * should required authentication. If required then the auth service is invoked
 * to get the token.
 * 
 * @author Balaji Sridharan
 * @author Mahesh Kumar
 */
@Aspect
@Component
public class RestClientAuthAdvice {

	private static final String INVALID_TOKEN_STRING = "Invalid Token";
	private static final String TOKEN_EXPIRED = "Token expired";
	private static final boolean HAVE_TO_SAVE_AUTH_TOKEN = true;

	private static final Logger LOGGER = AppConfig.getLogger(RestClientAuthAdvice.class);
	@Autowired
	private ServiceDelegateUtil serviceDelegateUtil;
	@Autowired
	private MachineMappingDAO machineMappingDAO;
	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private AuthTokenUtilService authTokenUtilService;

	@Autowired
	private UserTokenRepository userTokenRepository;

	/**
	 * The {@link Around} advice method which be invoked for all web services. This
	 * advice adds the Authorization Token to the Web-Service Request Header, if
	 * authorization is required. If Authorization Token had expired, a new token
	 * will be requested.
	 * 
	 * @param joinPoint
	 *            the join point of the advice
	 * @return the response from the web-service
	 * @throws RegBaseCheckedException
	 *             - generalized exception with errorCode and errorMessage
	 */
	@Around("execution(* io.mosip.registration.util.restclient.RestClientUtil.invoke(..))")
	public Object addAuthZToken(ProceedingJoinPoint joinPoint) throws RegBaseCheckedException {
		try {
			LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
					"Adding authZ token to web service request header if required");

			RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) joinPoint.getArgs()[0];

			if (requestHTTPDTO.isRequestSignRequired()) {
				addRequestSignature(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getRequestBody());
			}

			if (requestHTTPDTO.isAuthRequired()) {
				String authZToken = getAuthZToken(requestHTTPDTO);
				setAuthHeaders(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getAuthZHeader(), authZToken);
			}

			requestHTTPDTO.setHttpEntity(new HttpEntity<>(requestHTTPDTO.getRequestBody(), requestHTTPDTO.getHttpHeaders()));
			Object response = joinPoint.proceed(joinPoint.getArgs());

			if (handleInvalidTokenFromResponse(response, joinPoint)) {
				LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
						"Found invalid token error, retrying with new token");
				return joinPoint.proceed(joinPoint.getArgs());
			}

			LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME, "completed");
			return response;

		} catch (HttpClientErrorException httpClientErrorException) {
			String errorResponseBody = httpClientErrorException.getResponseBodyAsString();

			if (errorResponseBody != null && StringUtils.containsIgnoreCase(errorResponseBody, INVALID_TOKEN_STRING)
					|| 401 == httpClientErrorException.getRawStatusCode()) {
				try {
					RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) joinPoint.getArgs()[0];
					getAuthZToken(requestHTTPDTO);
					return joinPoint.proceed(joinPoint.getArgs());
				} catch (RegBaseCheckedException regBaseCheckedException) {
					throw regBaseCheckedException;
				} catch (Throwable throwableError) {
					throw new RegBaseCheckedException(
							RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
							RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage(), throwableError);
				}

			}
			throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage(),
					httpClientErrorException);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			throw regBaseCheckedException;
		} catch (Throwable throwable) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage(), throwable);
		}
	}


	/*private void getNewAuthZToken(RequestHTTPDTO requestHTTPDTO) throws RegBaseCheckedException {
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Entering into the new auth token generation ");
		String authZToken = RegistrationConstants.EMPTY;
		boolean haveToAuthZByClientId = false;
		LoginUserDTO loginUserDTO = (LoginUserDTO) ApplicationContext.map().get(RegistrationConstants.USER_DTO);

		if (RegistrationConstants.JOB_TRIGGER_POINT_USER.equals(requestHTTPDTO.getTriggerPoint())) {
			if (loginUserDTO == null || loginUserDTO.getPassword() == null
					|| isLoginModeOTP(loginUserDTO) || !SessionContext.isSessionContextAvailable()) {
				haveToAuthZByClientId = true;
				LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
						"Application context or Session Context with OTP ");
			} else {
				serviceDelegateUtil.getAuthToken(LoginMode.PASSWORD, HAVE_TO_SAVE_AUTH_TOKEN);
				authZToken = SessionContext.authTokenDTO().getCookie();
				LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
						"Session Context with password auth token generated " + authZToken);
			}
		}

		// Get the AuthZ Token By Client ID and Secret Key if
		if ((haveToAuthZByClientId
				|| RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM.equals(requestHTTPDTO.getTriggerPoint()))) {
			serviceDelegateUtil.getAuthToken(LoginMode.CLIENTID, HAVE_TO_SAVE_AUTH_TOKEN);
			authZToken = ApplicationContext.authTokenDTO().getCookie();
			SessionContext.authTokenDTO().setCookie(authZToken);
			
			LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
					"Application context or Session Context with OTP generated " + authZToken);
		}

		setAuthHeaders(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getAuthZHeader(), authZToken);
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Completed the new auth token generation ");
	}*/

	private String getAuthZToken(RequestHTTPDTO requestHTTPDTO)
			throws RegBaseCheckedException {
		AuthTokenDTO authZToken = authTokenUtilService.fetchAuthToken(requestHTTPDTO.getTriggerPoint());
		return authZToken.getCookie();
	}

	/*private String getAuthZToken(RequestHTTPDTO requestHTTPDTO, boolean haveToAuthZByClientId)
			throws RegBaseCheckedException {
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME, "Getting authZ token");
		String authZToken = null;

		// Get the AuthZ Token from AuthZ Web-Service only if Job is triggered by User
		// and existing AuthZ Token had expired
		if (RegistrationConstants.JOB_TRIGGER_POINT_USER.equals(requestHTTPDTO.getTriggerPoint())) {
			if (SessionContext.isSessionContextAvailable() && null != SessionContext.authTokenDTO()
					&& cookieAvailable()) {
				authZToken = SessionContext.authTokenDTO().getCookie();
				LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
						"Session Context Auth token " + authZToken);
			} else {
				LoginUserDTO loginUserDTO = (LoginUserDTO) ApplicationContext.map().get(RegistrationConstants.USER_DTO);
				if (loginUserDTO == null || loginUserDTO.getPassword() == null || !SessionContext.isSessionContextAvailable()) {
					haveToAuthZByClientId = true;
					LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
							"Session Context null and user id and password are from applicaiton context ");
				} else {
					SessionContext.setAuthTokenDTO(serviceDelegateUtil.getAuthToken(LoginMode.PASSWORD, HAVE_TO_SAVE_AUTH_TOKEN));
					authZToken = SessionContext.authTokenDTO().getCookie();
					LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
							"Session Context with password Auth token " + authZToken);
				}
			}
		}

		// Get the AuthZ Token By Client ID and Secret Key if
		if ((haveToAuthZByClientId
				|| RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM.equals(requestHTTPDTO.getTriggerPoint()))) {
			if (null == ApplicationContext.authTokenDTO() || null == ApplicationContext.authTokenDTO().getCookie()) {
				ApplicationContext.setAuthTokenDTO(serviceDelegateUtil.getAuthToken(LoginMode.CLIENTID, HAVE_TO_SAVE_AUTH_TOKEN));
			}
			authZToken = ApplicationContext.authTokenDTO().getCookie();
			LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
					"Application Context with Auth token " + authZToken);
		}

		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME, "Getting of authZ token completed");

		return authZToken;
	}*/

	/**
	 * Setup of Auth Headers.
	 *
	 * @param httpHeaders
	 *            http headers
	 * @param authHeader
	 *            auth header
	 * @param authZCookie
	 *            the Authorization Token or Cookie
	 */
	private void setAuthHeaders(HttpHeaders httpHeaders, String authHeader, String authZCookie) {
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Adding authZ token to request header");

		String[] arrayAuthHeaders = null;

		if (authHeader != null) {
			arrayAuthHeaders = authHeader.split(":");
			if (arrayAuthHeaders[1].equalsIgnoreCase(RegistrationConstants.REST_OAUTH)) {
				httpHeaders.add(RegistrationConstants.COOKIE, authZCookie);
			} else if (arrayAuthHeaders[1].equalsIgnoreCase(RegistrationConstants.AUTH_TYPE)) {
				httpHeaders.add(arrayAuthHeaders[0], arrayAuthHeaders[1]);
			}
		}

		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Adding of authZ token to request header completed");
	}

	/**
	 * Add request signature to the request header
	 * 
	 * @param httpHeaders
	 *            the HTTP headers for the web-service request
	 * @param requestBody
	 *            the request body
	 * @throws RegBaseCheckedException
	 *             exception while generating request signature
	 */
	private void addRequestSignature(HttpHeaders httpHeaders, Object requestBody) throws RegBaseCheckedException {
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Adding request signature to request header");

		try {
			httpHeaders.add("request-signature", String.format("Authorization:%s", CryptoUtil
					.encodeBase64(clientCryptoFacade.getClientSecurity().signData(JsonUtils.javaObjectToJsonString(requestBody).getBytes()))));
			httpHeaders.add(RegistrationConstants.KEY_INDEX, CryptoUtil.encodeBase64String(String
					.valueOf(machineMappingDAO.getKeyIndexByMachineName(RegistrationSystemPropertiesChecker.getMachineId()))
					.getBytes()));
		} catch (JsonProcessingException jsonProcessingException) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTHZ_ADDING_REQUEST_SIGN.getErrorCode(),
					RegistrationExceptionConstants.AUTHZ_ADDING_REQUEST_SIGN.getErrorMessage(),
					jsonProcessingException);
		}

		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Completed adding request signature to request header completed");
	}

	private boolean handleInvalidTokenFromResponse(Object response, ProceedingJoinPoint joinPoint)
			throws RegBaseCheckedException {
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Entering into the invalid token check");
		if (response != null && (StringUtils.containsIgnoreCase(response.toString(), TOKEN_EXPIRED) || 
				StringUtils.containsIgnoreCase(response.toString(), INVALID_TOKEN_STRING))) {
			LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
					"Old Token got expired for the token  " +  response);
			RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) joinPoint.getArgs()[0];
			LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
					"Creating the new token ");
			getAuthZToken(requestHTTPDTO);
			return true;
		}
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Completed the invalid token check");
		return false;
	}
	
	private boolean isLoginModeOTP(LoginUserDTO loginUserDTO) {
		LOGGER.info(LoggerConstants.AUTHZ_ADVICE, APPLICATION_ID, APPLICATION_NAME,
				"Checking for the Session Context with OTP is available :: " + (SessionContext.isSessionContextAvailable() && 
						loginUserDTO != null && loginUserDTO.getOtp() != null));
		return SessionContext.isSessionContextAvailable() && loginUserDTO != null && loginUserDTO.getOtp() != null;
	}
	
	/*private boolean cookieAvailable() {
		return null != SessionContext.authTokenDTO().getCookie() && 
				SessionContext.authTokenDTO().getCookie().trim().length() > 10;
	}*/
}
