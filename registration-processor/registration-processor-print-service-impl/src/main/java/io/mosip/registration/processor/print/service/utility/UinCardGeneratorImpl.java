package io.mosip.registration.processor.print.service.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.kernel.core.pdfgenerator.spi.PDFGenerator;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.pdfgenerator.itext.constant.PDFGeneratorExceptionCodeConstant;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.UinCardType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.spi.uincardgenerator.UinCardGenerator;
import io.mosip.registration.processor.print.service.dto.PDFSignatureRequestDto;
import io.mosip.registration.processor.print.service.dto.SignatureResponseDto;
import io.mosip.registration.processor.print.service.exception.PDFSignatureException;

/**
 * The Class UinCardGeneratorImpl.
 * 
 * @author M1048358 Alok
 */
@Component
public class UinCardGeneratorImpl implements UinCardGenerator<byte[]> {

	/** The pdf generator. */
	@Autowired
	private PDFGenerator pdfGenerator;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UinCardGeneratorImpl.class);

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";


	@Value("${mosip.registration.processor.print.service.uincard.lowerleftx}")
	private int lowerLeftX;

	@Value("${mosip.registration.processor.print.service.uincard.lowerlefty}")
	private int lowerLeftY;

	@Value("${mosip.registration.processor.print.service.uincard.upperrightx}")
	private int upperRightX;

	@Value("${mosip.registration.processor.print.service.uincard.upperrighty}")
	private int upperRightY;

	@Value("${mosip.registration.processor.print.service.uincard.signature.reason}")
	private String reason;




	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private Environment env;


	ObjectMapper mapper = new ObjectMapper();
	
	
	@Autowired
	private RestApiClient restApiClient;


	@Override
	public byte[] generateUinCard(InputStream in, UinCardType type, String password)
			throws ApisResourceAccessException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"UinCardGeneratorImpl::generateUinCard()::entry");
        byte[] pdfSignatured=null;
		ByteArrayOutputStream out = null;
		try {
			out = (ByteArrayOutputStream) pdfGenerator.generate(in);
			PDFSignatureRequestDto request = new PDFSignatureRequestDto(lowerLeftX, lowerLeftY, upperRightX,
					upperRightY, reason, 1, password);
			request.setApplicationId("KERNEL");
		  	request.setReferenceId("SIGN");
		  	request.setData(CryptoUtil.encodeBase64String(out.toByteArray()));
		  	DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

		  	request.setTimeStamp(DateUtils.getUTCCurrentDateTimeString());
			RequestWrapper<PDFSignatureRequestDto> requestWrapper = new RequestWrapper<>();

			requestWrapper.setRequest(request);
			requestWrapper.setRequesttime(localdatetime);
			ResponseWrapper<?> responseWrapper;
			SignatureResponseDto signatureResponseDto;

			 responseWrapper= (ResponseWrapper<?>)restClientService.postApi(ApiName.PDFSIGN, null, null,
					requestWrapper, ResponseWrapper.class,MediaType.APPLICATION_JSON);


			if (responseWrapper.getErrors() != null && !responseWrapper.getErrors().isEmpty()) {
				ErrorDTO error = responseWrapper.getErrors().get(0);
			    throw new PDFSignatureException(error.getMessage());
			}
			signatureResponseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
					SignatureResponseDto.class);

			 pdfSignatured = CryptoUtil.decodeBase64(signatureResponseDto.getData());

		} catch (IOException | PDFGeneratorException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", PlatformErrorMessages.RPR_PRT_PDF_NOT_GENERATED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getMessage() + ExceptionUtils.getStackTrace(e));
		} 
			  catch (ApisResourceAccessException e) {
			 
			 regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
			 LoggerFileConstant.REGISTRATIONID.toString(), "",
			 PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.name() + e.getMessage()
			 + ExceptionUtils.getStackTrace(e)); throw new PDFSignatureException(
			 e.getMessage() + ExceptionUtils.getStackTrace(e)); }
			 
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"UinCardGeneratorImpl::generateUinCard()::exit");

		return pdfSignatured;
	}

}
