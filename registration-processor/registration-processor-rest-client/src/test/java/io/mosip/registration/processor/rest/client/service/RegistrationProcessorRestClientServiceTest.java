package io.mosip.registration.processor.rest.client.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.web.client.ResourceAccessException;

import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.audit.dto.AuditRequestDto;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.rest.client.service.impl.RegistrationProcessorRestClientServiceImpl;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class RegistrationProcessorRestClientServiceTest {

	@InjectMocks
	RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService = new RegistrationProcessorRestClientServiceImpl();
	/** The rest api client. */
	@Mock
	private RestApiClient restApiClient;

	/** The env. */
	@Mock
	private Environment env;
	private AuditResponseDto auditResponseDto;

	@Before
	public void setUp() {
		auditResponseDto = new AuditResponseDto();
		auditResponseDto.setStatus(true);

	}

	@Test
	public void getObjecSuccessTest() throws Exception {

		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.getApi(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(auditResponseDto);
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.getApi(ApiName.AUDIT,
				null, "query1", "12345", AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}

	@Test
	public void postObjecSuccessTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(auditResponseDto);
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.postApi(ApiName.AUDIT,
				"query1", "12345", auditRequestDto, AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}

	@Test(expected = ApisResourceAccessException.class)
	public void getObjecTestFailureTest() throws Exception {
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		Mockito.when(restApiClient.getApi(ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(exp);

		registrationProcessorRestClientService.getApi(ApiName.AUDIT, Arrays.asList("abc", "def"), "query1", "12345",
				AuditResponseDto.class);
	}

	@Test(expected = ApisResourceAccessException.class)
	public void postObjecTestFailureTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenThrow(exp);
		registrationProcessorRestClientService.postApi(ApiName.AUDIT, "query1", "12345", auditRequestDto,
				AuditResponseDto.class);
	}
	
	@Test
	public void getObjectForArgListSuccessTest() throws Exception {

		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.getApi(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(auditResponseDto);
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.getApi(ApiName.AUDIT,
				null, Arrays.asList("query1"), Arrays.asList("12345"), AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}

	@Test(expected = ApisResourceAccessException.class)
	public void getObjectForArgListTestFailureTest() throws Exception {
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		Mockito.when(restApiClient.getApi(ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(exp);

		registrationProcessorRestClientService.getApi(ApiName.AUDIT, Arrays.asList("abc", "def"),
				Arrays.asList("query1"), Arrays.asList("12345"), AuditResponseDto.class);
	}

	@Test
	public void postObjectSuccessTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(auditResponseDto);
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.postApi(ApiName.AUDIT,
				pathSegments, "query1", "12345", auditRequestDto, AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}

	@Test(expected = ApisResourceAccessException.class)
	public void postObjectTestFailureTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenThrow(exp);
		registrationProcessorRestClientService.postApi(ApiName.AUDIT, pathSegments, "query1", "12345", auditRequestDto,
				AuditResponseDto.class);
	}
	
	@Test
	public void postObjectForArgListSuccessTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(auditResponseDto);
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.postApi(ApiName.AUDIT,
				null, pathSegments, Arrays.asList("query1"), Arrays.asList("12345"), auditRequestDto,
				AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}

	@Test(expected = ApisResourceAccessException.class)
	public void postObjectForArgListTestFailureTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenThrow(exp);
		registrationProcessorRestClientService.postApi(ApiName.AUDIT, null, pathSegments, Arrays.asList("query1"),
				Arrays.asList("12345"), auditRequestDto, AuditResponseDto.class);
	}
	
	@Test
	public void patchObjectSuccessTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.patchApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(auditResponseDto);
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.patchApi(ApiName.AUDIT,
				pathSegments, "query1", "12345", auditRequestDto, AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}
	
	@Test(expected = ApisResourceAccessException.class)
	public void patchObjectTestFailureTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		Mockito.when(restApiClient.patchApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenThrow(exp);
		registrationProcessorRestClientService.patchApi(ApiName.AUDIT, pathSegments, "query1", "12345", auditRequestDto,
				AuditResponseDto.class);
	}
	
	@Test
	public void putObjectSuccessTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		Mockito.when(restApiClient.putApi(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(auditResponseDto);
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.putApi(ApiName.AUDIT,
				pathSegments, "query1", "12345", auditRequestDto, AuditResponseDto.class,null);
		assertEquals(true, resultDto.isStatus());
	}
	
	@Test(expected = ApisResourceAccessException.class)
	public void putObjectTestFailureTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(env.getProperty(ArgumentMatchers.any())).thenReturn("AUDIT");
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		Mockito.when(restApiClient.putApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenThrow(exp);
		registrationProcessorRestClientService.putApi(ApiName.AUDIT, pathSegments, "query1", "12345", auditRequestDto,
				AuditResponseDto.class,null);
	}
	
	@Test
	public void postObjectForUrlSuccessTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(auditResponseDto);
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		AuditResponseDto resultDto = (AuditResponseDto) registrationProcessorRestClientService.postApi("AUDIT",
				null, pathSegments, Arrays.asList("query1"), Arrays.asList("12345"), auditRequestDto,
				AuditResponseDto.class);
		assertEquals(true, resultDto.isStatus());
	}

	@Test(expected = ApisResourceAccessException.class)
	public void postObjectForUrlTestFailureTest() throws Exception {
		AuditRequestDto auditRequestDto = new AuditRequestDto();
		ResourceAccessException exp = new ResourceAccessException("errorMessage");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("test");
		Mockito.when(restApiClient.postApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenThrow(exp);
		registrationProcessorRestClientService.postApi("AUDIT", null, pathSegments, Arrays.asList("query1"),
				Arrays.asList("12345"), auditRequestDto, AuditResponseDto.class);
	}

}
