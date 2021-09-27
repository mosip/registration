package io.mosip.registration.processor.stages.helper;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.stages.dto.AsyncRequestDTO;
import io.mosip.registration.processor.stages.exception.RestServiceException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.core.publisher.Mono;


@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@WebMvcTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ WebClient.class, SslContextBuilder.class, Mock.class })
public class RestHelperTest {

	/** The rest helper. */
	@InjectMocks
	RestHelperImpl restHelper;
	
	/** The mapper. */
	@Autowired
	ObjectMapper mapper;
	
	/** The environment. */
	@Autowired
	Environment environment;
	
	@MockBean
	private RestApiClient restApiClient;
	
	@Before
	public void before() throws IOException {
		PowerMockito.mockStatic(SslContextBuilder.class);
		SslContextBuilder sslContextBuilder = PowerMockito.mock(SslContextBuilder.class);
		PowerMockito.when(SslContextBuilder.forClient()).thenReturn(sslContextBuilder);
		PowerMockito.when(sslContextBuilder.trustManager(Mockito.any(TrustManagerFactory.class)))
				.thenReturn(sslContextBuilder);
		PowerMockito.when(sslContextBuilder.build()).thenReturn(Mockito.mock(SslContext.class));
		Mockito.when(restApiClient.getToken()).thenReturn("Gjnaiuhs==");
	}
	
	/**
	 * Test get auth token.
	 *
	 * @throws JsonParseException the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@Ignore
	public void testGetAuthToken() throws IOException {
		//ReflectionTestUtils.setField(restHelper, "authToken", null);
		PowerMockito.mockStatic(WebClient.class);
		WebClient webClient = PowerMockito.mock(WebClient.class);
		PowerMockito.when(WebClient.create(Mockito.any())).thenReturn(webClient);
		RequestBodyUriSpec requestBodyUriSpec = PowerMockito.mock(RequestBodyUriSpec.class);
		PowerMockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
		RequestHeadersSpec requestHeadersSpec = PowerMockito.mock(RequestHeadersSpec.class);
		PowerMockito.when(requestBodyUriSpec.syncBody(Mockito.any())).thenReturn(requestHeadersSpec);
		ClientResponse clientResponse = PowerMockito.mock(ClientResponse.class);
		PowerMockito.when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
		MultiValueMap<String, ResponseCookie> map = new LinkedMultiValueMap<>();
		map.add("Authorization", ResponseCookie.from("Authorization", "1234").build());
		PowerMockito.when(clientResponse.cookies()).thenReturn(map);
		PowerMockito.when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
		String response = "{\"response\":{\"status\":\"success\"}}";
		PowerMockito.when(clientResponse.bodyToMono(Mockito.any(Class.class)))
				.thenReturn(Mono.just(mapper.readValue(response.getBytes(), ObjectNode.class)));
		ReflectionTestUtils.invokeMethod(restHelper, "getAuthToken");
	}
	
	/**
	 * Test get auth token invalid.
	 *
	 * @throws JsonParseException the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@Ignore
	public void testGetAuthTokenInvalid() throws JsonParseException, JsonMappingException, IOException {
		//ReflectionTestUtils.setField(restHelper, "authToken", null);
		PowerMockito.mockStatic(WebClient.class);
		WebClient webClient = PowerMockito.mock(WebClient.class);
		PowerMockito.when(WebClient.create(Mockito.any())).thenReturn(webClient);
		RequestBodyUriSpec requestBodyUriSpec = PowerMockito.mock(RequestBodyUriSpec.class);
		PowerMockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
		RequestHeadersSpec requestHeadersSpec = PowerMockito.mock(RequestHeadersSpec.class);
		PowerMockito.when(requestBodyUriSpec.syncBody(Mockito.any())).thenReturn(requestHeadersSpec);
		ClientResponse clientResponse = PowerMockito.mock(ClientResponse.class);
		PowerMockito.when(clientResponse.toEntity(Mockito.any(Class.class))).thenReturn(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
		//PowerMockito.when(clientResponse.b(Mockito.any(Class.class))).thenReturn(clientResponse);
		PowerMockito.when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
		MultiValueMap<String, ResponseCookie> map = new LinkedMultiValueMap<>();
		map.add("Authorization", ResponseCookie.from("Authorization", "1234").build());
		PowerMockito.when(clientResponse.cookies()).thenReturn(map);
		PowerMockito.when(clientResponse.statusCode()).thenReturn(HttpStatus.CREATED);
		String response = "{\"response\":{\"status\":\"success\"}}";
		PowerMockito.when(clientResponse.bodyToMono(Mockito.any(Class.class)))
				.thenReturn(Mono.just(mapper.readValue(response.getBytes(), ObjectNode.class)));
		ReflectionTestUtils.invokeMethod(restHelper, "getAuthToken");
	}
	
	/**
	 * Test request async.
	 *
	 * @throws IDDataValidationException             the ID data validation exception
	 * @throws RestServiceException             the rest service exception
	 * @throws JsonParseException the json parse exception
	 * @throws JsonMappingException the json mapping exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testRequestAsync() throws JsonParseException, JsonMappingException, IOException {
		PowerMockito.mockStatic(WebClient.class);
		ResponseSpec responseSpec=PowerMockito.mock(ResponseSpec.class);
		PowerMockito.mock(ClientResponse.class);
		//PowerMockito.when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
		String response = "{\"response\":{\"status\":\"success\"}}";
		//Mono<? extends ObjectNode> monoResponse= Mono.just(mapper.readValue(response.getBytes(), ObjectNode.class));
		AsyncRequestDTO restReqDTO=new AsyncRequestDTO();
		Map<String, String> pathVariables=new HashMap<>();;
		restReqDTO.setPathVariables(pathVariables);
		//restReqDTO.setResponseType(Mockito.any(Class.class));
		WebClient webClient = PowerMockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = PowerMockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = PowerMockito.mock(RequestBodySpec.class);
		Builder mockBuilder = PowerMockito.mock(WebClient.Builder.class);
		PowerMockito.when(WebClient.builder()).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.build()).thenReturn(webClient);
		PowerMockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Function<UriBuilder, URI> uriFunction=Mockito.any();
		PowerMockito.when(requestBodyUriSpec.uri(uriFunction)).thenReturn(requestBodySpec);
		PowerMockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		PowerMockito.when(responseSpec.bodyToMono((Class)null)).thenReturn( Mono.<Object>just(mapper.readValue(response.getBytes(), ObjectNode.class)));
		restHelper.requestAsync(restReqDTO);
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testRequestAsyncWithHeaders() throws JsonParseException, JsonMappingException, IOException {
		PowerMockito.mockStatic(WebClient.class);
		ResponseSpec responseSpec=PowerMockito.mock(ResponseSpec.class);
		PowerMockito.mock(ClientResponse.class);
		//PowerMockito.when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
		String response = "{\"response\":{\"status\":\"success\"}}";
		//Mono<? extends ObjectNode> monoResponse= Mono.just(mapper.readValue(response.getBytes(), ObjectNode.class));
		AsyncRequestDTO restReqDTO=new AsyncRequestDTO();
		Map<String, String> pathVariables=new HashMap<>();;
		restReqDTO.setPathVariables(pathVariables);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		restReqDTO.setHeaders(headers);
		//restReqDTO.setResponseType(Mockito.any(Class.class));
		WebClient webClient = PowerMockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = PowerMockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = PowerMockito.mock(RequestBodySpec.class);
		Builder mockBuilder = PowerMockito.mock(WebClient.Builder.class);
		PowerMockito.when(WebClient.builder()).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.build()).thenReturn(webClient);
		PowerMockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Function<UriBuilder, URI> uriFunction=Mockito.any();
		PowerMockito.when(requestBodyUriSpec.uri(uriFunction)).thenReturn(requestBodySpec);
		PowerMockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		PowerMockito.when(responseSpec.bodyToMono((Class)null)).thenReturn( Mono.<Object>just(mapper.readValue(response.getBytes(), ObjectNode.class)));
		restHelper.requestAsync(restReqDTO);
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testRequestAsyncWithParam() throws IOException {
		PowerMockito.mockStatic(WebClient.class);
		ResponseSpec responseSpec=PowerMockito.mock(ResponseSpec.class);
		PowerMockito.mock(ClientResponse.class);
		//PowerMockito.when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
		String response = "{\"response\":{\"status\":\"success\"}}";
		//Mono<? extends ObjectNode> monoResponse= Mono.just(mapper.readValue(response.getBytes(), ObjectNode.class));
		AsyncRequestDTO restReqDTO=new AsyncRequestDTO();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		restReqDTO.setHeaders(headers);
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
		String name="Tapaswini";
		paramMap.add("name", name);
		restReqDTO.setParams(paramMap);
		WebClient webClient = PowerMockito.mock(WebClient.class);
		RequestBodyUriSpec requestBodyUriSpec = PowerMockito.mock(RequestBodyUriSpec.class);
		RequestBodySpec requestBodySpec = PowerMockito.mock(RequestBodySpec.class);
		Builder mockBuilder = PowerMockito.mock(WebClient.Builder.class);
		PowerMockito.when(WebClient.builder()).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.clientConnector(Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.baseUrl(Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.defaultHeader(Mockito.any(), Mockito.any())).thenReturn(mockBuilder);
		PowerMockito.when(mockBuilder.build()).thenReturn(webClient);
		PowerMockito.when(webClient.method(Mockito.any())).thenReturn(requestBodyUriSpec);
		Function<UriBuilder, URI> uriFunction=Mockito.any();
		PowerMockito.when(requestBodyUriSpec.uri(uriFunction)).thenReturn(requestBodySpec);
		PowerMockito.when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		PowerMockito.when(responseSpec.bodyToMono((Class)null)).thenReturn( Mono.<Object>just(mapper.readValue(response.getBytes(), ObjectNode.class)));
		restHelper.requestAsync(restReqDTO);
	}
	
	
	/**
	 * Test request async and return.
	 *
	 * @throws IDDataValidationException             the ID data validation exception
	 * @throws RestServiceException             the rest service exception
	 * @throws SSLException the SSL exception
	 */
	@Test(expected=RestServiceException.class)
	public void testRequestAsyncAndReturn() throws RestServiceException, SSLException {
		PowerMockito.mockStatic(SslContextBuilder.class);
		SslContextBuilder sslContextBuilder = PowerMockito.mock(SslContextBuilder.class);
		PowerMockito.when(SslContextBuilder.forClient()).thenReturn(sslContextBuilder);
		PowerMockito.when(sslContextBuilder.trustManager(Mockito.any(TrustManagerFactory.class)))
				.thenReturn(sslContextBuilder);
		PowerMockito.when(sslContextBuilder.build()).thenThrow(new SSLException(""));
		Object object = restHelper.requestAsync(new AsyncRequestDTO()).get();
		if(object instanceof RestServiceException) {
			RestServiceException restServiceException = (RestServiceException) object;
			throw restServiceException;
			
		}
	}


}


