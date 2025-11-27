package io.mosip.registration.processor.rest.client.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.tracing.ContextualData;
import io.mosip.registration.processor.core.tracing.TracingConstant;
import io.mosip.registration.processor.rest.client.audit.dto.Metadata;
import io.mosip.registration.processor.rest.client.audit.dto.SecretKeyRequest;
import io.mosip.registration.processor.rest.client.audit.dto.TokenRequestDTO;
import io.mosip.registration.processor.rest.client.exception.TokenGenerationFailedException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;

/**
 * The Class WebApiClient.
 *
 * @author Rishabh Keshari
 */
@Component
public class WebApiClient {

    /** The logger. */
    private final Logger logger = RegProcessorLogger.getLogger(WebApiClient.class);

    /** The builder. */
    @Autowired
    RestTemplateBuilder builder;

    @Autowired
    Environment environment;

    private static final String AUTHORIZATION = "Authorization=";

    @Autowired
    @Qualifier("selfTokenWebClient")
    WebClient webClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    ObjectMapper objMp;

    /**
     * Gets the api. *
     *
     * @param <T>          the generic type
     * @param uri          the get URI
     * @param responseType the response type
     * @return the api
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <T> T getApi(URI uri, Class<?> responseType) throws Exception {
        try {
            return (T) webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block(TIMEOUT);
        } catch (Exception e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            tokenExceptionHandler(e);
            throw e;
        }
    }

    /**
     * Post api.
     *
     * @param <T>           the generic type
     * @param uri           the uri
     * @param mediaType     the mediaType
     * @param requestType   the request type
     * @param responseClass the response class
     * @return the t
     */
    @SuppressWarnings("unchecked")
    public <T> T postApi(String uri, MediaType mediaType, Object requestType, Class<?> responseClass) throws Exception {

        T result = null;
        try {
            return (T) webClient.post()
                    .uri(uri)
                    .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_JSON)
                    .header(TracingConstant.TRACE_HEADER,
                            (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY))
                    .bodyValue(requestType)
                    .retrieve()
                    .onStatus(HttpStatusCode::is2xxSuccessful, clientResponse -> {
                        // Log the status, headers, and body for 2xx codes (like 200 OK)
                        // This is where you would inspect the problematic 200 response
                        return clientResponse.bodyToMono(String.class) // Read the body as a String
                                .flatMap(body -> {
                                    logger.error(LoggerFileConstant.SESSIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            "200 OK received, but body deserialization failed. Raw Body: " + body);
                                    // Return an error to stop processing
                                    return Mono.error(new RuntimeException("WebClient 200 OK Deserialization Error. See logs for raw body."));
                                });
                    })
                    .bodyToMono(responseClass)
                    .block(TIMEOUT);
        } catch (Exception e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            tokenExceptionHandler(e);
            throw e;
        }
    }

    /**
     * Post api.
     *
     * @param <T>           the generic type
     * @param uri           the uri
     * @param requestType   the request type
     * @param responseClass the response class
     * @return the t
     */
    public <T> T postApi(String uri, Object requestType, Class<?> responseClass) throws Exception {
        return postApi(uri, null, requestType, responseClass);
    }

    /**
     * Patch api.
     *
     * @param <T>           the generic type
     * @param uri           the uri
     * @param requestType   the request type
     * @param responseClass the response class
     * @return the t
     */
    @SuppressWarnings("unchecked")
    public <T> T patchApi(String uri, MediaType mediaType, Object requestType, Class<?> responseClass)
            throws Exception {
        try {
            return (T) webClient.patch()
                    .uri(uri)
                    .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_JSON)
                    .header(TracingConstant.TRACE_HEADER,
                            (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY))
                    .bodyValue(requestType)
                    .retrieve()
                    .onStatus(HttpStatusCode::is2xxSuccessful, clientResponse -> {
                        // Log the status, headers, and body for 2xx codes (like 200 OK)
                        // This is where you would inspect the problematic 200 response
                        return clientResponse.bodyToMono(String.class) // Read the body as a String
                                .flatMap(body -> {
                                    logger.error(LoggerFileConstant.SESSIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            "200 OK received, but body deserialization failed. Raw Body: " + body);
                                    // Return an error to stop processing
                                    return Mono.error(new RuntimeException("WebClient 200 OK Deserialization Error. See logs for raw body."));
                                });
                    })
                    .bodyToMono(responseClass)
                    .block(TIMEOUT);
        } catch (Exception e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            tokenExceptionHandler(e);
            throw e;
        }
    }

    public <T> T patchApi(String uri, Object requestType, Class<?> responseClass) throws Exception {
        return patchApi(uri, null, requestType, responseClass);
    }

    /**
     * Put api.
     *
     * @param <T>           the generic type
     * @param uri           the uri
     * @param requestType   the request type
     * @param responseClass the response class
     * @param mediaType
     * @return the t
     * @throws Exception the exception
     */
    @SuppressWarnings("unchecked")
    public <T> T putApi(String uri, Object requestType, Class<?> responseClass, MediaType mediaType) throws Exception {

        try {
            return (T) webClient.put()
                    .uri(uri)
                    .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_JSON)
                    .header(TracingConstant.TRACE_HEADER,
                            (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY))
                    .bodyValue(requestType)
                    .retrieve()
                    .onStatus(HttpStatusCode::is2xxSuccessful, clientResponse -> {
                        // Log the status, headers, and body for 2xx codes (like 200 OK)
                        // This is where you would inspect the problematic 200 response
                        return clientResponse.bodyToMono(String.class) // Read the body as a String
                                .flatMap(body -> {
                                    logger.error(LoggerFileConstant.SESSIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            "200 OK received, but body deserialization failed. Raw Body: " + body);
                                    // Return an error to stop processing
                                    return Mono.error(new RuntimeException("WebClient 200 OK Deserialization Error. See logs for raw body."));
                                });
                    })
                    .bodyToMono(responseClass)
                    .block(TIMEOUT);
        } catch (Exception e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            tokenExceptionHandler(e);
            throw e;
        }
    }

    public int headApi(URI uri) throws Exception {
        try {
            return webClient.head()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity()
                    .map(resp -> resp.getStatusCode().value())
                    .block(TIMEOUT);
        } catch (Exception e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            tokenExceptionHandler(e);
            throw e;
        }
    }

    public <T> T deleteApi(URI uri, Class<?> responseType) throws Exception {
        try {
            return (T) webClient.delete()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is2xxSuccessful, clientResponse -> {
                        // Log the status, headers, and body for 2xx codes (like 200 OK)
                        // This is where you would inspect the problematic 200 response
                        return clientResponse.bodyToMono(String.class) // Read the body as a String
                                .flatMap(body -> {
                                    logger.error(LoggerFileConstant.SESSIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            LoggerFileConstant.APPLICATIONID.toString(),
                                            "200 OK received, but body deserialization failed. Raw Body: " + body);
                                    // Return an error to stop processing
                                    return Mono.error(new RuntimeException("WebClient 200 OK Deserialization Error. See logs for raw body."));
                                });
                    })
                    .bodyToMono(responseType)
                    .block(TIMEOUT);
        } catch (Exception e) {
            logger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    LoggerFileConstant.APPLICATIONID.toString(),
                    e.getMessage() + ExceptionUtils.getStackTrace(e));
            tokenExceptionHandler(e);
            throw e;
        }
    }

    public WebClient getRestTemplate() {
        return webClient;
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
        // headers.add("Cookie", getToken());
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
                    List<String> collection = httpHeader.get(key);
                    if ((collection != null && !collection.isEmpty())
                            && !(headers.containsKey("Content-Type") && key.equalsIgnoreCase("Content-Type")))
                        headers.add(key, collection.get(0));

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
     * @throws ParseException
     */
    public String getToken() throws IOException, ParseException {
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

            tokenRequestDTO.setRequesttime(DateUtils2.getUTCCurrentDateTimeString());
            // tokenRequestDTO.setRequest(setPasswordRequestDTO());
            tokenRequestDTO.setRequest(setSecretKeyRequestDTO());
            tokenRequestDTO.setVersion(environment.getProperty("token.request.version"));

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(environment.getProperty("KEYBASEDTOKENAPI"));
            try {
                StringEntity postingString = new StringEntity(objMp.writeValueAsString(tokenRequestDTO));
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");
                post.setHeader(TracingConstant.TRACE_HEADER,
                        (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY));
                CloseableHttpResponse response = httpClient.execute(post);
                org.apache.hc.core5.http.HttpEntity entity = response.getEntity();
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
            } catch (ParseException e) {
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

    public void tokenExceptionHandler(Exception e) {
        if (e.getCause() instanceof HttpStatusCodeException) {
            HttpStatusCodeException ex = (HttpStatusCodeException) e.getCause();
            if (ex.getRawStatusCode() == 401) {
                logger.error(LoggerFileConstant.SESSIONID.toString(),
                        LoggerFileConstant.APPLICATIONID.toString(),
                        LoggerFileConstant.APPLICATIONID.toString(),
                        "Authentication failed. Resetting auth token.");
                System.setProperty("token", "");
            }
        }
    }
}