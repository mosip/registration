package io.mosip.registration.test.util.restclient;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import io.mosip.registration.constants.ActiveProfiles;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.util.advice.LoggerAdvice;
import io.mosip.registration.util.restclient.RequestHTTPDTO;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ApplicationContext.class })
public class LoggerAdviceTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private LoggerAdvice loggerAdvice;
	
	@Mock
	private JoinPoint joinPointMock;
	
	@Before
	public void init() throws Exception {
		Map<String, Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.SERVER_ACTIVE_PROFILE, ActiveProfiles.DEV.getCode());
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
	}
	
	@Test
	public void beforeLog() throws Throwable {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setUri(new URI("dev.mosip.io"));
		requestHTTPDTO.setHttpMethod(HttpMethod.GET);
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);

		loggerAdvice.requestLogging(joinPointMock);
	}
	
	@Test
	public void afterLog() throws Throwable {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setUri(new URI("dev.mosip.io"));
		requestHTTPDTO.setHttpMethod(HttpMethod.GET);
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);

		Map<String , Object> responseMap=new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, RegistrationConstants.REST_RESPONSE_BODY);
		responseMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, RegistrationConstants.REST_RESPONSE_BODY);
		loggerAdvice.responseLogging(joinPointMock, responseMap);
	}

	@Test
	public void entityLog() throws Throwable {

		Map<String , Object> responseMap=new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, RegistrationConstants.REST_RESPONSE_BODY);
		responseMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, RegistrationConstants.REST_RESPONSE_BODY);
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setUri(new URI("dev.mosip.io"));
		requestHTTPDTO.setHttpMethod(HttpMethod.GET);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		requestHTTPDTO.setHttpEntity(new HttpEntity<>(httpHeaders));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		
		loggerAdvice.entityLogging(joinPointMock, responseMap);
	}
}
