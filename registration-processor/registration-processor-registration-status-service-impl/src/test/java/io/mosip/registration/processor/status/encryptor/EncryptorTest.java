package io.mosip.registration.processor.status.encryptor;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;

import io.mosip.registration.processor.status.dto.DecryptResponseDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.CryptomanagerResponseDto;
import io.mosip.registration.processor.status.exception.EncryptionFailureException;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ EncryptorTest.class })
public class EncryptorTest {

	@InjectMocks
	private Encryptor encryptor;

	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;
	
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;
	
	@Mock
	private Environment env;
	
	private DecryptResponseDto cryptomanagerResponseDto;
	private String data;

	@Before
	public void setup() throws FileNotFoundException {
		data = "bW9zaXA";
		cryptomanagerResponseDto = new DecryptResponseDto();
		cryptomanagerResponseDto.setData(data);
		when(env.getProperty("mosip.registration.processor.crypto.decrypt.id"))
				.thenReturn("mosip.cryptomanager.decrypt");
		when(env.getProperty("mosip.registration.processor.application.version")).thenReturn("1.0");
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
				.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	}

	@Test
	public void encryptTest() throws EncryptionFailureException, ApisResourceAccessException, IOException {
		DecryptResponseDto cryptomanagerResponseDto = new DecryptResponseDto();
		cryptomanagerResponseDto.setData("mosip");
		ResponseWrapper<DecryptResponseDto> response = new ResponseWrapper<>();
		response.setResponse(cryptomanagerResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any())).thenReturn(response);
		byte[] encryptedData = encryptor.encrypt(data, "10011", "2019-05-07T05:13:55.704Z");

		assertNotNull(encryptedData);
	}

	@Test(expected = EncryptionFailureException.class)
	public void httpClientErrorExceptionTest()
			throws FileNotFoundException, ApisResourceAccessException, EncryptionFailureException {
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.BAD_REQUEST,
				"Invalid request");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		encryptor.encrypt(data, "10011", "2019-05-07T05:13:55.704Z");
	}

	@Test(expected = EncryptionFailureException.class)
	public void httpServerErrorExceptionTest()
			throws FileNotFoundException, ApisResourceAccessException, EncryptionFailureException {

		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:Data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);

		encryptor.encrypt(data, "10011", "2019-05-07T05:13:55.704Z");
	}

	@Test(expected = ApisResourceAccessException.class)
	public void encryptionFailureExceptionTest()
			throws FileNotFoundException, ApisResourceAccessException, EncryptionFailureException {

		ApisResourceAccessException apisResourceAccessException = new ApisResourceAccessException(
				"Encryption failure");
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenThrow(apisResourceAccessException);
		encryptor.encrypt(data, "10011", "2019-05-07T05:13:55.704Z");
	}

}
