package io.mosip.registration.processor.rest.client.utils;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;

import io.mosip.registration.processor.core.tracing.ContextualData;
import io.mosip.registration.processor.core.tracing.TracingConstant;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.rest.client.audit.dto.Metadata;
import io.mosip.registration.processor.rest.client.audit.dto.PasswordRequest;
import io.mosip.registration.processor.rest.client.audit.dto.SecretKeyRequest;
import io.mosip.registration.processor.rest.client.audit.dto.TokenRequestDTO;
import io.mosip.registration.processor.rest.client.exception.TokenGenerationFailedException;

/**
 * The Class RestApiClient.
 *
 * @author Rishabh Keshari
 */
@Component
public class RestApiClient {

	/** The logger. */
	private final Logger logger = RegProcessorLogger.getLogger(RestApiClient.class);

	/** The builder. */
	@Autowired
	RestTemplateBuilder builder;

	@Autowired
	Environment environment;

	private static final String AUTHORIZATION = "Authorization=";

	RestTemplate localRestTemplate;

	@PostConstruct
	private void loadRestTemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		localRestTemplate = getRestTemplate();
		logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				LoggerFileConstant.APPLICATIONID.toString(), "loadRestTemplate completed successfully");
	}


	/**
	 * Gets the api. *
	 * 
	 * @param              <T> the generic type
	 * @param uri       the get URI
	 * @param responseType the response type
	 * @return the api
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T getApi(URI uri, Class<?> responseType) throws Exception {
		T result = null;
		try {
			result = (T) localRestTemplate.exchange(uri, HttpMethod.GET, setRequestHeader(null, null), responseType)
					.getBody();
		} catch (Exception e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			tokenExceptionHandler(e);
			throw e;
		}
		return result;
	}

	/**
	 * Post api.
	 *
	 * @param <T>
	 *            the generic type
	 * @param uri
	 *            the uri
	 * @param requestType
	 *            the request type
	 * @param responseClass
	 *            the response class
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public <T> T postApi(String uri, MediaType mediaType, Object requestType, Class<?> responseClass) throws Exception {

		T result = null;
		try {
			logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), uri);
			result = (T) localRestTemplate.postForObject(uri, setRequestHeader(requestType, mediaType), responseClass);

		} catch (Exception e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			tokenExceptionHandler(e);
			throw e;
		}
		return result;
	}

	/**
	 * Patch api.
	 *
	 * @param <T>
	 *            the generic type
	 * @param uri
	 *            the uri
	 * @param requestType
	 *            the request type
	 * @param responseClass
	 *            the response class
	 * @return the t
	 */
	@SuppressWarnings("unchecked")
	public <T> T patchApi(String uri, MediaType mediaType, Object requestType, Class<?> responseClass)
			throws Exception {

		T result = null;
		try {
			logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), uri);
			result = (T) localRestTemplate.patchForObject(uri, setRequestHeader(requestType, mediaType), responseClass);

		} catch (Exception e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			tokenExceptionHandler(e);
			throw e;
		}
		return result;
	}

	public <T> T patchApi(String uri, Object requestType, Class<?> responseClass) throws Exception {
		return patchApi(uri, null, requestType, responseClass);
	}

	/**
	 * Put api.
	 *
	 * @param <T>
	 *            the generic type
	 * @param uri
	 *            the uri
	 * @param requestType
	 *            the request type
	 * @param responseClass
	 *            the response class
	 * @param mediaType
	 * @return the t
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T putApi(String uri, Object requestType, Class<?> responseClass, MediaType mediaType) throws Exception {

		T result = null;
		ResponseEntity<T> response = null;
		try {
			logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), uri);

			response = (ResponseEntity<T>) localRestTemplate.exchange(uri, HttpMethod.PUT,
					setRequestHeader(requestType.toString(), mediaType), responseClass);
			result = response.getBody();
		} catch (Exception e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			tokenExceptionHandler(e);
			throw e;
		}
		return result;
	}

	public RestTemplate getRestTemplate() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		if(localRestTemplate != null)
			return localRestTemplate;

		logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				LoggerFileConstant.APPLICATIONID.toString(), Arrays.asList(environment.getActiveProfiles()).toString());
		if (Arrays.stream(environment.getActiveProfiles()).anyMatch("dev-k8"::equals)) {
			logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(),
					Arrays.asList(environment.getActiveProfiles()).toString());
			return new RestTemplate();
		} else {
			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

			SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy).build();

			SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

			CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

			requestFactory.setHttpClient(httpClient);
			return new RestTemplate(requestFactory);
		}

	}

	/**
	 * this method sets token to header of the request
	 *
	 * @param requestType
	 * @param mediaType
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private HttpEntity<Object> setRequestHeader(Object requestType, MediaType mediaType) throws IOException {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Cookie", getToken());
		headers.add(TracingConstant.TRACE_HEADER, (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY));
		if (mediaType != null) {
			headers.add("Content-Type", mediaType.toString());
		}
		if (requestType != null) {
			try {
				HttpEntity<Object> httpEntity = (HttpEntity<Object>) requestType;
				HttpHeaders httpHeader = httpEntity.getHeaders();
				Iterator<String> iterator = httpHeader.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					if (!(headers.containsKey("Content-Type") && key == "Content-Type"))
						headers.add(key, httpHeader.get(key).get(0));
				}
				return new HttpEntity<Object>(httpEntity.getBody(), headers);
			} catch (ClassCastException e) {
				return new HttpEntity<Object>(requestType, headers);
			}
		} else
			return new HttpEntity<Object>(headers);
	}

	/**
	 * This method gets the token for the user details present in config server.
	 *
	 * @return
	 * @throws IOException
	 */
	public String getToken() throws IOException {
		String token = System.getProperty("token");
		boolean isValid = false;

		if (StringUtils.isNotEmpty(token)) {

			isValid = TokenHandlerUtil.isValidBearerToken(token, environment.getProperty("token.request.issuerUrl"),
					environment.getProperty("token.request.clientId"));


		}
		if (!isValid) {
		TokenRequestDTO<SecretKeyRequest> tokenRequestDTO = new TokenRequestDTO<SecretKeyRequest>();
		tokenRequestDTO.setId(environment.getProperty("token.request.id"));
		tokenRequestDTO.setMetadata(new Metadata());

		tokenRequestDTO.setRequesttime(DateUtils.getUTCCurrentDateTimeString());
		// tokenRequestDTO.setRequest(setPasswordRequestDTO());
		tokenRequestDTO.setRequest(setSecretKeyRequestDTO());
		tokenRequestDTO.setVersion(environment.getProperty("token.request.version"));

		Gson gson = new Gson();
		HttpClient httpClient = HttpClientBuilder.create().build();
		// HttpPost post = new
		// HttpPost(environment.getProperty("PASSWORDBASEDTOKENAPI"));
		HttpPost post = new HttpPost(environment.getProperty("KEYBASEDTOKENAPI"));
		try {
			StringEntity postingString = new StringEntity(gson.toJson(tokenRequestDTO));
			post.setEntity(postingString);
			post.setHeader("Content-type", "application/json");
			post.setHeader(TracingConstant.TRACE_HEADER, (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY));
			HttpResponse response = httpClient.execute(post);
			org.apache.http.HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity, "UTF-8");
			Header[] cookie = response.getHeaders("Set-Cookie");
			if (cookie.length == 0)
				throw new TokenGenerationFailedException();
			token = response.getHeaders("Set-Cookie")[0].getValue();
				System.setProperty("token", token.substring(14, token.indexOf(';')));
			return token.substring(0, token.indexOf(';'));
		} catch (IOException e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
			}
		}
		return AUTHORIZATION + token;
	}

	private SecretKeyRequest setSecretKeyRequestDTO() {
		SecretKeyRequest request = new SecretKeyRequest();
		request.setAppId(environment.getProperty("token.request.appid"));
		request.setClientId(environment.getProperty("token.request.clientId"));
		request.setSecretKey(environment.getProperty("token.request.secretKey"));
		return request;
	}

	private PasswordRequest setPasswordRequestDTO() {

		PasswordRequest request = new PasswordRequest();
		request.setAppId(environment.getProperty("token.request.appid"));
		request.setPassword(environment.getProperty("token.request.password"));
		request.setUserName(environment.getProperty("token.request.username"));
		return request;
	}

	public void tokenExceptionHandler(Exception e) {
		if (e instanceof HttpStatusCodeException) {
			HttpStatusCodeException ex = (HttpStatusCodeException) e;
			if (ex.getRawStatusCode() == 401) {
				logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
						LoggerFileConstant.APPLICATIONID.toString(), "Authentication failed. Resetting auth token.");
				System.setProperty("token", "");
			}
		}
	}


}
