package io.mosip.registration.processor.workflowmanager.util;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.TokenHandlerUtil;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.tracing.ContextualData;
import io.mosip.registration.processor.core.tracing.TracingConstant;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.rest.client.audit.dto.Metadata;
import io.mosip.registration.processor.rest.client.audit.dto.SecretKeyRequest;
import io.mosip.registration.processor.rest.client.audit.dto.TokenRequestDTO;
import io.mosip.registration.processor.rest.client.exception.TokenGenerationFailedException;


@Component
public class WebSubUtil {
	@Autowired
	private PublisherClient<String, WorkflowCompletedEventDTO, HttpHeaders> workflowCompletedPublisher;

	@Autowired
	private PublisherClient<String, WorkflowPausedForAdditionalInfoEventDTO, HttpHeaders> workflowPausedForAdditionalInfoPublisher;

	@Autowired
	Environment environment;
	
	@Value("${mosip.regproc.workflow.complete.topic}")
	private String workflowCompleteTopic;

	@Value("${websub.publish.url}")
	private String webSubPublishUrl;

	@Value("${mosip.regproc.workflow.pausedforadditionalinfo.topic}")
	private String workflowPausedforadditionalinfoTopic;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WebSubUtil.class);
	
	private static final String AUTHORIZATION = "Authorization=";

	@PostConstruct
	private void registerTopic() {
		try {
			workflowCompletedPublisher.registerTopic(workflowCompleteTopic, webSubPublishUrl);

		} catch (WebSubClientException exception) {
			regProcLogger.warn(exception.getMessage());
		}
		try {
			workflowPausedForAdditionalInfoPublisher.registerTopic(workflowPausedforadditionalinfoTopic,
					webSubPublishUrl);
		} catch (WebSubClientException exception) {
			regProcLogger.warn(exception.getMessage());
		}
	}

	public void publishEvent(WorkflowCompletedEventDTO workflowCompletedEventDTO) throws WebSubClientException {
		String rid = workflowCompletedEventDTO.getInstanceId();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Cookie", getToken());
		workflowCompletedPublisher.publishUpdate(workflowCompleteTopic, workflowCompletedEventDTO,
				MediaType.APPLICATION_JSON_UTF8_VALUE,
				httpHeaders, webSubPublishUrl);
		regProcLogger.info("Publish the update successfully  for registration id {}", rid);

	}

	public void publishEvent(WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO)
			throws WebSubClientException {
		String rid = workflowPausedForAdditionalInfoEventDTO.getInstanceId();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Cookie", getToken());
		workflowPausedForAdditionalInfoPublisher.publishUpdate(workflowPausedforadditionalinfoTopic,
				workflowPausedForAdditionalInfoEventDTO,
				MediaType.APPLICATION_JSON_UTF8_VALUE,
				httpHeaders, webSubPublishUrl);
		regProcLogger.info("Publish the update successfully  for registration id {}", rid);

	}
	
	/**
	 * This method gets the token for the user details present in config server.
	 *
	 * @return
	 * @throws IOException
	 */
	public String getToken()  {
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
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
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
}
