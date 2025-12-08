package io.mosip.registration.processor.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.registration.processor.core.digital.signature.dto.JWTSignatureRequestDto;
import io.mosip.registration.processor.core.digital.signature.dto.JWTSignatureResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.digital.signature.dto.SignRequestDto;
import io.mosip.registration.processor.core.digital.signature.dto.SignResponseDto;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.exception.DigitalSignatureException;

@Component
public class DigitalSignatureUtility {

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DigitalSignatureUtility.class);

	@Autowired
	private Environment env;

	@Autowired
	ObjectMapper mapper;

    @Value("${mosip.sign-certificate-refid:SIGN}")
    private String signRefID;

    /** The sign applicationid. */
    @Value("${mosip.sign.applicationid:KERNEL}")
    private String signApplicationid;

	private static final String DIGITAL_SIGNATURE_ID = "mosip.registration.processor.digital.signature.id";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";

	public String getDigitalSignature(String data) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"DigitalSignatureUtility::getDigitalSignature()::entry");

        String encodedData = CryptoUtil.encodeToURLSafeBase64(data.getBytes(StandardCharsets.UTF_8));
        regProcLogger.info("DigitalSignatureUtility::getDigitalSignature() :: Encoded data: " + encodedData);
        RequestWrapper<JWTSignatureRequestDto> request = new RequestWrapper<>();
        JWTSignatureRequestDto jwtSignatureRequestDto = new JWTSignatureRequestDto();
        jwtSignatureRequestDto.setApplicationId(signApplicationid);
        jwtSignatureRequestDto.setReferenceId(signRefID);
        jwtSignatureRequestDto.setDataToSign(CryptoUtil.encodeToURLSafeBase64(data.getBytes(StandardCharsets.UTF_8)));
        request.setRequest(jwtSignatureRequestDto);
        request.setId(env.getProperty(DIGITAL_SIGNATURE_ID));
        request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
        DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
        LocalDateTime localdatetime = LocalDateTime
                .parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
        request.setRequesttime(localdatetime);
        request.setMetadata(null);

		try {
			ResponseWrapper<JWTSignatureResponseDto> response = (ResponseWrapper) registrationProcessorRestService.postApi(ApiName.DIGITALSIGNATURE, "", "", request, ResponseWrapper.class);

			if (response.getErrors() != null && response.getErrors().size() > 0) {
				response.getErrors().stream().forEach(r -> {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
							"DigitalSignatureUtility::getDigitalSignature():: error with error message " + r.getMessage());
				});
			}

			JWTSignatureResponseDto jwtSignatureResponseDto = mapper.readValue(mapper.writeValueAsString(response.getResponse()), JWTSignatureResponseDto.class);
			
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"DigitalSignatureUtility::getDigitalSignature()::exit");

            String signature = jwtSignatureResponseDto.getJwtSignedData();
            regProcLogger.info("DigitalSignatureUtility::getDigitalSignature():: Signature data : " + signature);
            return jwtSignatureResponseDto.getJwtSignedData();
		} catch (ApisResourceAccessException | IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"DigitalSignatureUtility::getDigitalSignature():: error with error message " + e.getMessage());
			throw new DigitalSignatureException(e.getMessage(), e);
		}

	}
}

