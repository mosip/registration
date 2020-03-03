package io.mosip.registration.processor.print.service.util.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.kernel.core.pdfgenerator.spi.PDFGenerator;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.UinCardType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.print.service.dto.SignatureResponseDto;
import io.mosip.registration.processor.print.service.exception.PDFSignatureException;
import io.mosip.registration.processor.print.service.utility.UinCardGeneratorImpl;

@RunWith(MockitoJUnitRunner.class)
public class UinCardGeneratorImplTest {

	@Mock
	private PDFGenerator pdfGenerator;

	@InjectMocks
	private UinCardGeneratorImpl cardGeneratorImpl;
	
	@Mock
	private Environment env;
	
	@Mock
	private RegistrationProcessorRestClientService<Object> restClientService;
	
	
	@Before
	public void setUp() {
		when(env.getProperty("mosip.registration.processor.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ReflectionTestUtils.setField(cardGeneratorImpl, "lowerLeftX", 73);
		ReflectionTestUtils.setField(cardGeneratorImpl, "lowerLeftY", 100);
		ReflectionTestUtils.setField(cardGeneratorImpl, "upperRightX", 300);
		ReflectionTestUtils.setField(cardGeneratorImpl, "upperRightY", 300);
		ReflectionTestUtils.setField(cardGeneratorImpl, "reason", "signing");
		
	}
	

	@Test
	public void testCardGenerationSuccess() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFile = classLoader.getResource("csshtml.html").getFile();
		InputStream is = new FileInputStream(inputFile);

		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		Mockito.when(pdfGenerator.generate(is)).thenReturn(outputStream);
		ResponseWrapper<SignatureResponseDto> responseWrapper=new ResponseWrapper<>();
		SignatureResponseDto signatureResponseDto=new SignatureResponseDto();
		signatureResponseDto.setData(buffer.toString());
		responseWrapper.setResponse(signatureResponseDto);
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(),any(MediaType.class)))
		.thenReturn(responseWrapper);
		byte[] bos = (byte[]) cardGeneratorImpl.generateUinCard(is, UinCardType.PDF,
				null);

		String outputPath = System.getProperty("user.dir");
		String fileSepetator = System.getProperty("file.separator");
		File OutPutPdfFile = new File(outputPath + fileSepetator + "csshtml.pdf");
		FileOutputStream op = new FileOutputStream(OutPutPdfFile);
		op.write(bos);
		op.flush();
		assertTrue(OutPutPdfFile.exists());
		if (op != null) {
			op.close();
		}
		OutPutPdfFile.delete();
	}

	@Test(expected = PDFGeneratorException.class)
	public void testPdfGeneratorException() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFileName = classLoader.getResource("emptyFile.html").getFile();
		File inputFile = new File(inputFileName);
		InputStream inputStream = new FileInputStream(inputFile);
		PDFGeneratorException e = new PDFGeneratorException(null, null);
		Mockito.doThrow(e).when(pdfGenerator).generate(inputStream);
		cardGeneratorImpl.generateUinCard(inputStream, UinCardType.PDF, null);
	}

	@Test(expected = PDFSignatureException.class)
	public void testPDFSignatureException() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFile = classLoader.getResource("csshtml.html").getFile();
		InputStream is = new FileInputStream(inputFile);

		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		ApisResourceAccessException e = new ApisResourceAccessException(null, null);
		Mockito.doThrow(e).when(restClientService).postApi(any(), any(), any(), any(), any(),any(MediaType.class));
		
		Mockito.when(pdfGenerator.generate(is)).thenReturn(outputStream);
		
		
		cardGeneratorImpl.generateUinCard(is, UinCardType.PDF, null);
	}
	@Test(expected = PDFSignatureException.class)
	public void testCardGenerationFailure() throws IOException, ApisResourceAccessException {
		ClassLoader classLoader = getClass().getClassLoader();
		String inputFile = classLoader.getResource("csshtml.html").getFile();
		InputStream is = new FileInputStream(inputFile);

		byte[] buffer = new byte[8192];
		int bytesRead;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		Mockito.when(pdfGenerator.generate(is)).thenReturn(outputStream);
		ResponseWrapper<SignatureResponseDto> responseWrapper=new ResponseWrapper<>();
		List<ErrorDTO> errors=new ArrayList<ErrorDTO>();
		ErrorDTO error=new ErrorDTO();
		error.setErrorCode("KER-001");
		error.setMessage("error in digital signature");
		errors.add(error);
		responseWrapper.setErrors(errors);
	
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any(),any(MediaType.class)))
		.thenReturn(responseWrapper);
		cardGeneratorImpl.generateUinCard(is, UinCardType.PDF, null);
	}
}
