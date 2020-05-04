package io.mosip.registration.util.advice;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.LinkedHashMap;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.ActiveProfiles;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.util.restclient.RequestHTTPDTO;

@Aspect
@Component
public class LoggerAdvice {

	private static final Logger LOGGER = AppConfig.getLogger(LoggerAdvice.class);
	private static final String beforelog = "REGISTRATION - LOGGER-BEFORE-ADVICE- INVOKE";
	private static final String afteReturnlog = "REGISTRATION - LOGGER-AFTERRETURN-ADVICE- INVOKE";

	/**
	 * Request logging.
	 *
	 * @param joinPoint the join point
	 * @throws Throwable the throwable
	 */
	@Before(value = "execution(* io.mosip.registration.util.restclient.RestClientUtil.invoke(..))")
	public void requestLogging(JoinPoint joinPoint) throws RegBaseUncheckedException {

		Object[] jointPointResponse = joinPoint.getArgs();
		RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) jointPointResponse[0];

		if (ActiveProfiles.DEV.getCode().equalsIgnoreCase(
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE)))
				|| ActiveProfiles.QA.getCode().equalsIgnoreCase(
						String.valueOf(ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE)))) {

			LOGGER.info(beforelog, APPLICATION_NAME, APPLICATION_ID, "Request URL======>" + requestHTTPDTO.getUri());
			LOGGER.info(beforelog, APPLICATION_NAME, APPLICATION_ID,
					"Request Method======>" + requestHTTPDTO.getHttpMethod());

			if (!StringUtils.containsIgnoreCase(requestHTTPDTO.getUri().toString(),
					RegistrationConstants.AUTH_SERVICE_URL)) {
				LOGGER.info(beforelog, APPLICATION_NAME, APPLICATION_ID,
						"Request Class======>" + requestHTTPDTO.getClazz());
			}
		}

	}

	@AfterReturning(pointcut = "execution(* io.mosip.registration.util.restclient.RestClientUtil.invoke(..))", returning = "result")
	public void responseLogging(JoinPoint joinPoint, Object result) throws RegBaseUncheckedException {

		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) result;

		if (ActiveProfiles.DEV.getCode().equalsIgnoreCase(
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE)))
				|| ActiveProfiles.QA.getCode().equalsIgnoreCase(
						String.valueOf(ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE)))) {

			if (responseMap != null && !responseMap.isEmpty()) {

				LOGGER.info(afteReturnlog, APPLICATION_NAME, APPLICATION_ID,
						"Response Body======>" + responseMap.get(RegistrationConstants.REST_RESPONSE_BODY));
				LOGGER.info(afteReturnlog, APPLICATION_NAME, APPLICATION_ID,
						"Response Header======>" + responseMap.get(RegistrationConstants.REST_RESPONSE_HEADERS));

			}
		}
	}

	@AfterReturning(pointcut = "execution(* io.mosip.registration.util.restclient.RestClientUtil.invokeForToken(..))", returning = "result")
	public void entityLogging(JoinPoint joinPoint, Object result) throws RegBaseUncheckedException {

		Object[] requestHTTPDTO = joinPoint.getArgs();
		RequestHTTPDTO requestDto = (RequestHTTPDTO) requestHTTPDTO[0];
		if (ActiveProfiles.DEV.getCode().equalsIgnoreCase(
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE)))
				|| ActiveProfiles.QA.getCode().equalsIgnoreCase(
						String.valueOf(ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE)))) {
			if (!StringUtils.containsIgnoreCase(requestDto.getUri().toString(),
					RegistrationConstants.AUTH_SERVICE_URL)) {
				LOGGER.info(afteReturnlog, APPLICATION_NAME, APPLICATION_ID,
						"Request Entity======>" + requestDto.getHttpEntity());
			}
		}
	}
}