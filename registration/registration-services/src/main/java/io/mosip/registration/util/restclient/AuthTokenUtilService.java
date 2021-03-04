package io.mosip.registration.util.restclient;


import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoginMode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.LoginUserDTO;
import io.mosip.registration.entity.UserToken;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.UserTokenRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;


/**
 * @author Anusha Sunkada
 * @since 1.1.3
 */
@Service
public class AuthTokenUtilService {

    private static final String AUTH_REFRESH_TOKEN_UTIL = "AUTH_REFRESH_TOKEN_UTIL";
    private static final Logger LOGGER = AppConfig.getLogger(AuthTokenUtilService.class);

    @Autowired
    private ClientCryptoFacade clientCryptoFacade;

    @Autowired
    private RestClientUtil restClientUtil;

    @Autowired
    private Environment environment;

    @Autowired
    private UserDetailDAO userDetailDAO;

    @Autowired
    private UserTokenRepository userTokenRepository;

    public boolean hasAnyValidToken() {
        UserToken userToken = userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(System.currentTimeMillis()/1000);
        if(userToken != null) {
            return true;
        }
        userToken = userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(System.currentTimeMillis()/1000);
        if(userToken != null) {
            return true;
        }

        LOGGER.error(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID, "No valid auth token found! Needs new token to be fetched");
        return false;
    }

    public AuthTokenDTO fetchAuthToken(String triggerPoint) throws RegBaseCheckedException {
        LOGGER.info(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                "fetchAuthToken invoked for triggerPoint >>>>> " + triggerPoint);

        if(SessionContext.isSessionContextAvailable()) {
            UserToken userToken = userTokenRepository.findByUsrIdAndUserDetailIsActiveTrue(SessionContext.userId());
            if(userToken != null && userToken.getTokenExpiry() > (System.currentTimeMillis()/1000)) {
                AuthTokenDTO authTokenDTO = new AuthTokenDTO();
                authTokenDTO.setCookie(String.format("Authorization=%s", userToken.getToken()));
                return authTokenDTO;
            }

            if(userToken != null && userToken.getRtokenExpiry() > (System.currentTimeMillis()/1000)) {
                AuthTokenDTO authTokenDTO = new AuthTokenDTO();
                authTokenDTO.setCookie(String.format("Authorization=%s",
                        refreshAuthToken(userToken.getUsrId(), userToken.getRefreshToken())));
                return authTokenDTO;
            }
        }
        else {
            UserToken userToken = userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(System.currentTimeMillis()/1000);
            if(userToken != null) {
                AuthTokenDTO authTokenDTO = new AuthTokenDTO();
                authTokenDTO.setCookie(String.format("Authorization=%s", userToken.getToken()));
                return authTokenDTO;
            }

            userToken = userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(System.currentTimeMillis()/1000);
            if(userToken != null) {
                AuthTokenDTO authTokenDTO = new AuthTokenDTO();
                authTokenDTO.setCookie(String.format("Authorization=%s",
                        refreshAuthToken(userToken.getUsrId(), userToken.getRefreshToken())));
                return authTokenDTO;
            }
        }

        LoginUserDTO loginUserDTO = (LoginUserDTO) ApplicationContext.map().get(RegistrationConstants.USER_DTO);
        if(loginUserDTO != null && loginUserDTO.getPassword() != null) {
            return getAuthTokenAndRefreshToken(LoginMode.PASSWORD);
        }

        throw new RegBaseCheckedException(
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage());
    }



    private String refreshAuthToken(String userId, String refreshToken) throws RegBaseCheckedException {
        LOGGER.info(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                "refreshAuthToken invoked for userId >>>>> " + userId);
        try {
            String timestamp = DateUtils.formatToISOString(LocalDateTime.now(ZoneOffset.UTC));
            String header = String.format("{\"kid\" : \"%s\"}", CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getSigningPublicPart(), null));
            String payload = String.format("{\"refreshToken\" : \"%s\", \"authType\":\"%s\", \"timestamp\" : \"%s\"}",
                    refreshToken, "REFRESH", timestamp);
            byte[] signature = clientCryptoFacade.getClientSecurity().signData(payload.getBytes());
            String data = String.format("%s.%s.%s", Base64.getUrlEncoder().encodeToString(header.getBytes()),
                    Base64.getUrlEncoder().encodeToString(payload.getBytes()), Base64.getUrlEncoder().encodeToString(signature));

            RequestHTTPDTO requestHTTPDTO = getRequestHTTPDTO(data, timestamp);
            setTimeout(requestHTTPDTO);
            setURI(requestHTTPDTO, new HashMap<>(), getEnvironmentProperty("auth_by_password", RegistrationConstants.SERVICE_URL));
            Map<String, Object> responseMap = restClientUtil.invokeForToken(requestHTTPDTO);

            long currentTimeInSeconds = System.currentTimeMillis()/1000;
            JSONObject jsonObject = getAuthTokenResponse(responseMap);
            userDetailDAO.updateAuthTokens(userId, jsonObject.getString("token"),
                    jsonObject.getString("refreshToken"),
                    currentTimeInSeconds + jsonObject.getLong("expiryTime"),
                    currentTimeInSeconds + jsonObject.getLong("refreshExpiryTime"));
            return jsonObject.getString("token");
        } catch (Exception exception) {
            LOGGER.error(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                    ExceptionUtils.getStackTrace(exception));
        }
        throw new RegBaseCheckedException(
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage());
    }

    public AuthTokenDTO getAuthTokenAndRefreshToken(LoginMode loginMode) throws RegBaseCheckedException {
        LoginUserDTO loginUserDTO = (LoginUserDTO) ApplicationContext.map().get(RegistrationConstants.USER_DTO);
        return getAuthTokenAndRefreshToken(loginMode, loginUserDTO);
    }


    public AuthTokenDTO getAuthTokenAndRefreshToken(LoginMode loginMode, LoginUserDTO loginUserDTO) throws RegBaseCheckedException {
        LOGGER.info(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                "Fetching Auth Token and refresh token based on Login Mode >>> " + loginMode);
        try {
            String timestamp = DateUtils.formatToISOString(LocalDateTime.now(ZoneOffset.UTC));
            String header = String.format("{\"kid\" : \"%s\"}", CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getSigningPublicPart(), null));

            String payload = "";
            switch (loginMode) {
                case PASSWORD:
                    payload = String.format("{\"userId\" : \"%s\", \"password\": \"%s\", \"authType\":\"%s\", \"timestamp\" : \"%s\"}",
                        loginUserDTO.getUserId(), loginUserDTO.getPassword(), "NEW", timestamp);
                    break;
                case OTP:
                    payload = String.format("{\"userId\" : \"%s\", \"otp\": \"%s\", \"authType\":\"%s\", \"timestamp\" : \"%s\"}",
                            loginUserDTO.getUserId(), loginUserDTO.getOtp(), "OTP", timestamp);
                    break;
            }

            byte[] signature = clientCryptoFacade.getClientSecurity().signData(payload.getBytes());
            String data = String.format("%s.%s.%s", Base64.getUrlEncoder().encodeToString(header.getBytes()),
                    Base64.getUrlEncoder().encodeToString(payload.getBytes()), Base64.getUrlEncoder().encodeToString(signature));

            RequestHTTPDTO requestHTTPDTO = getRequestHTTPDTO(data, timestamp);
            setTimeout(requestHTTPDTO);
            setURI(requestHTTPDTO, new HashMap<>(), getEnvironmentProperty("auth_by_password", RegistrationConstants.SERVICE_URL));
            Map<String, Object> responseMap = restClientUtil.invokeForToken(requestHTTPDTO);

            long currentTimeInSeconds = System.currentTimeMillis()/1000;
            JSONObject jsonObject = getAuthTokenResponse(responseMap);
            AuthTokenDTO authTokenDTO = new AuthTokenDTO();
            authTokenDTO.setCookie(String.format("Authorization=%s", jsonObject.getString("token")));
            authTokenDTO.setLoginMode(loginMode.getCode());

            ApplicationContext.setAuthTokenDTO(authTokenDTO);
            if(SessionContext.isSessionContextAvailable())
                SessionContext.setAuthTokenDTO(authTokenDTO);

            if(loginUserDTO.getPassword() != null)
                userDetailDAO.updateUserPwd(loginUserDTO.getUserId(), loginUserDTO.getPassword());

            userDetailDAO.updateAuthTokens(loginUserDTO.getUserId(), jsonObject.getString("token"),
                    jsonObject.getString("refreshToken"),
                    currentTimeInSeconds + jsonObject.getLong("expiryTime"),
                    currentTimeInSeconds + jsonObject.getLong("refreshExpiryTime"));

            return authTokenDTO;

        } catch (Exception exception) {
            LOGGER.error(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                    ExceptionUtils.getStackTrace(exception));
        }
        throw new RegBaseCheckedException(
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage());
    }

    private JSONObject getAuthTokenResponse(Map<String, Object> responseMap) throws RegBaseCheckedException {
        if(responseMap.get(RegistrationConstants.REST_RESPONSE_BODY) != null) {
            Map<String, Object> respBody = (Map<String, Object>) responseMap.get(RegistrationConstants.REST_RESPONSE_BODY);
            if (respBody.get("response") != null) {
                byte[] decryptedData = clientCryptoFacade.decrypt(CryptoUtil.decodeBase64((String)respBody.get("response")));
                return new JSONObject(new String(decryptedData));
            }

            if(respBody.get("errors") != null) {
                List<LinkedHashMap<String, Object>> errorMap = (List<LinkedHashMap<String, Object>>) respBody
                        .get(RegistrationConstants.ERRORS);
                if(!errorMap.isEmpty()) {
                    throw new RegBaseCheckedException((String)errorMap.get(0).get("errorCode"),
                            (String)errorMap.get(0).get("message"));
                }
            }
        }
        throw new RegBaseCheckedException(
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(),
                RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage());
    }

    private RequestHTTPDTO getRequestHTTPDTO(String data, String timestamp) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("id", "");
        requestBody.put("version", "");
        requestBody.put("request", data);
        requestBody.put("requesttime", timestamp);

        RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
        requestHTTPDTO.setClazz(Object.class);
        requestHTTPDTO.setRequestBody(requestBody);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        requestHTTPDTO.setHttpHeaders(headers);
        requestHTTPDTO.setIsSignRequired(false);
        requestHTTPDTO.setRequestSignRequired(false);
        requestHTTPDTO.setHttpMethod(HttpMethod.POST);
        return requestHTTPDTO;
    }

    private String getEnvironmentProperty(String serviceName, String serviceComponent) {
        return environment.getProperty(serviceName.concat(RegistrationConstants.DOT).concat(serviceComponent));
    }

    private void setURI(RequestHTTPDTO requestHTTPDTO, Map<String, String> requestParams, String url) {
        LOGGER.info(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                "Preparing URI for web-service");
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
        if (requestParams != null) {
            Set<String> set = requestParams.keySet();
            for (String queryParamName : set) {
                uriComponentsBuilder.queryParam(queryParamName, requestParams.get(queryParamName));
            }
        }
        URI uri = uriComponentsBuilder.build().toUri();
        requestHTTPDTO.setUri(uri);
        LOGGER.info(AUTH_REFRESH_TOKEN_UTIL, APPLICATION_NAME, APPLICATION_ID,
                "Completed preparing URI for web-service >>>>>>> " + uri);
    }

    private void setTimeout(RequestHTTPDTO requestHTTPDTO) {
        // Timeout in milli second
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(
                Integer.parseInt((String) ApplicationContext.map().get(RegistrationConstants.HTTP_API_READ_TIMEOUT)));
        requestFactory.setConnectTimeout(
                Integer.parseInt((String) ApplicationContext.map().get(RegistrationConstants.HTTP_API_WRITE_TIMEOUT)));
        requestHTTPDTO.setSimpleClientHttpRequestFactory(requestFactory);
    }
}
