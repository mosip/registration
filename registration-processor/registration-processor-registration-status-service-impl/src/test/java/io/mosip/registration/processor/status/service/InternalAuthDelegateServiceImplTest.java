package io.mosip.registration.processor.status.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.NoSuchAlgorithmException;
import io.mosip.registration.processor.core.auth.dto.AuthRequestDTO;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.IndividualIdDto;
import io.mosip.registration.processor.core.auth.dto.ResponseDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.status.service.impl.InternalAuthDelegateServiceImpl;

@RunWith(SpringJUnit4ClassRunner.class)
public class InternalAuthDelegateServiceImplTest {

	@InjectMocks
	InternalAuthDelegateServiceImpl internalAuthDelegateServiceImpl = new InternalAuthDelegateServiceImpl();

	@Mock
	RegistrationProcessorRestClientService<Object> restClientService;

	@Mock
	private RestApiClient restApiClient;

	@Mock
	RestTemplate restTemplate;

	@Mock
	ObjectMapper mapper;

	AuthRequestDTO authRequestDTO = new AuthRequestDTO();
	AuthResponseDTO authResponse = new AuthResponseDTO();
	ResponseDTO responseDto = new ResponseDTO();

	IndividualIdDto individualIdDto = new IndividualIdDto();
	ResponseWrapper<IndividualIdDto> response = new ResponseWrapper<IndividualIdDto>();

	@Before
	public void setup() throws Exception {

		ReflectionTestUtils.setField(internalAuthDelegateServiceImpl, "internalAuthUri",
				"https://dev.mosip.net/idauthentication/v1/internal/auth");
		ReflectionTestUtils.setField(internalAuthDelegateServiceImpl, "getCertificateUri",
				"https://dev.mosip.net/idauthentication/v1/internal/getCertificate");
		authRequestDTO.setEnv("Staging");
		authRequestDTO.setIndividualId("45128164920495");
		authRequestDTO.setIndividualIdType("UIN");
		authRequestDTO.setRequest("BFijjscahGoaaol");

		responseDto.setAuthStatus(true);
		authResponse.setId("");
		authResponse.setResponse(responseDto);
		ResponseEntity<AuthResponseDTO> entity = new ResponseEntity<AuthResponseDTO>(authResponse, HttpStatus.OK);
		Mockito.when(restApiClient.getRestTemplate()).thenReturn(restTemplate);
		Mockito.when(restTemplate.exchange(anyString(), any(), any(), eq(AuthResponseDTO.class))).thenReturn(entity);
		Mockito.when(mapper.writeValueAsString(any())).thenReturn("");
		Mockito.when(mapper.readValue(anyString(), eq(IndividualIdDto.class))).thenReturn(individualIdDto);
	}

	@Test
	public void authenticateSuccessTest() throws Exception {

		individualIdDto.setIndividualId("84953741281492");
		response.setResponse(individualIdDto);
		response.setErrors(null);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), anyString(), any())).thenReturn(response);
		AuthResponseDTO result = internalAuthDelegateServiceImpl.authenticate(authRequestDTO, new HttpHeaders());
		assertEquals(result.getResponse().isAuthStatus(), true);
	}

	@Test(expected = ApisResourceAccessException.class)
	public void authenticateIndividualIdNotFoundFailureTest() throws Exception {

		individualIdDto.setIndividualId("84953741281491");
		response.setResponse(individualIdDto);
		Mockito.when(restClientService.getApi(any(), any(), anyString(), anyString(), any())).thenReturn(response);
		internalAuthDelegateServiceImpl.authenticate(authRequestDTO, new HttpHeaders());
	}

	@Test
	public void getCertificateSuccessTest() throws Exception {

		Optional<String> referenceId = Optional.of("PROCESSOR");
		Mockito.when(restApiClient.getApi(any(), eq(Object.class))).thenReturn(new Object());
		Object result = internalAuthDelegateServiceImpl.getCertificate("REGISTRATIONCLIENT",
				referenceId, new HttpHeaders());
		assertNotNull(result);
	}

}