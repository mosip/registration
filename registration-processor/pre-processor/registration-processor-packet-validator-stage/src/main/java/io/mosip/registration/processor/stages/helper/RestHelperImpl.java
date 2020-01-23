package io.mosip.registration.processor.stages.helper;

import java.util.function.Supplier;

import javax.net.ssl.SSLException;
import javax.validation.Valid;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.EmptyCheckUtils;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.stages.dto.RestRequestDTO;
import io.mosip.registration.processor.stages.exception.RestServiceException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@NoArgsConstructor
public class RestHelperImpl implements RestHelper {

	private static Logger mosipLogger = RegProcessorLogger.getLogger(RestHelperImpl.class);

	private String authToken;
	@Autowired
	private Environment env;
	@Autowired
	private ObjectMapper mapper;

	@Override
	public Supplier<Object> requestAsync(@Valid RestRequestDTO request) {
		try {
			Mono<?> sendRequest = request(request, getSslContext());
			sendRequest.subscribe();
			return () -> sendRequest.block();
		} catch (RestServiceException e) {
			System.out.println(ExceptionUtils.getStackTrace(e));
			return () -> new RestServiceException("UNABLE_TO_PROCESS", e);
		}
	}

	private SslContext getSslContext() throws RestServiceException {
		try {
			return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} catch (SSLException e) {
			System.out.println(ExceptionUtils.getStackTrace(e));
			throw new RestServiceException("UNKNOWN_ERROR", e);
		}
	}

	private Mono<?> request(RestRequestDTO request, SslContext sslContext) {
		WebClient webClient;
		Mono<?> monoResponse;
		RequestBodySpec uri;
		ResponseSpec exchange;
		RequestBodyUriSpec method;

		if (request.getHeaders() != null) {
			webClient = WebClient.builder()
					.clientConnector(new ReactorClientHttpConnector(builder -> builder.sslContext(sslContext)))
					.baseUrl(request.getUri())
					.defaultHeader(HttpHeaders.CONTENT_TYPE, request.getHeaders().getContentType().toString()).build();
		} else {
			webClient = WebClient.builder()
					.clientConnector(new ReactorClientHttpConnector(builder -> builder.sslContext(sslContext)))
					.baseUrl(request.getUri()).build();
		}

		method = webClient.method(request.getHttpMethod());
		if (request.getParams() != null && request.getPathVariables() == null) {
			uri = method.uri(builder -> builder.queryParams(request.getParams()).build());
		} else if (request.getParams() == null && request.getPathVariables() != null) {
			uri = method.uri(builder -> builder.build(request.getPathVariables()));
		} else {
			uri = method.uri(builder -> builder.build());
		}

		uri.cookie("Authorization", getAuthToken());

		if (request.getRequestBody() != null) {
			exchange = uri.syncBody(request.getRequestBody()).retrieve();
		} else {
			exchange = uri.retrieve();
		}

		monoResponse = exchange.bodyToMono(request.getResponseType());

		return monoResponse;
	}

	private String getAuthToken() {
		if (EmptyCheckUtils.isNullEmpty(authToken)) {
			generateAuthToken();
			return authToken;
		} else {
			return authToken;
		}
	}

	private void generateAuthToken() {
		ObjectNode requestBody = mapper.createObjectNode();
		requestBody.put("clientId", env.getProperty("token.request.clientId"));
		requestBody.put("secretKey", env.getProperty("token.request.secretKey"));
		requestBody.put("appId", env.getProperty("token.request.appid"));
		RequestWrapper<ObjectNode> request = new RequestWrapper<>();
		request.setRequesttime(DateUtils.getUTCCurrentDateTime());
		request.setRequest(requestBody);
		ClientResponse response = WebClient.create(env.getProperty("KEYBASEDTOKENAPI")).post().syncBody(request)
				.exchange().block();
		if (response.statusCode() == HttpStatus.OK) {
			ObjectNode responseBody = response.bodyToMono(ObjectNode.class).block();
			if (responseBody != null
					&& responseBody.get("response").get("status").asText().equalsIgnoreCase("success")) {
				ResponseCookie responseCookie = response.cookies().get("Authorization").get(0);
				authToken = responseCookie.getValue();

			} else {
				mosipLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						"", "Auth token generation failed: " + response);
			}
		} else {
			mosipLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
					"AuthResponse : status-" + response.statusCode() + " :\n"
							+ response.toEntity(String.class).block().getBody());
		}
	}

}
