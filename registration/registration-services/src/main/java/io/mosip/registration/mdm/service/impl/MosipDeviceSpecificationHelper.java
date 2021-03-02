package io.mosip.registration.mdm.service.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.DeviceException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.MDMError;
import io.mosip.registration.mdm.dto.MdmDeviceInfo;

/**
 * All helper methods commons to spec implementations
 *
 * @author anusha
 */
@Component
public class MosipDeviceSpecificationHelper {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecificationHelper.class);

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private SignatureService signatureService;

	@Value("${mosip.registration.mdm.validate.trust:true}")
	private boolean validateTrust;

	private final String CONTENT_LENGTH = "Content-Length:";

	public String getPayLoad(String data) throws RegBaseCheckedException {
		if (data == null || data.isEmpty()) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorCode(),
					RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage());
		}
		Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_PAYLOAD_EMPTY.getErrorCode(),
				RegistrationExceptionConstants.MDS_PAYLOAD_EMPTY.getErrorMessage());
	}

	public MdmDeviceInfo getDeviceInfoDecoded(String deviceInfo) {
		try {
			validateJWTResponse(deviceInfo);
			String result = new String(Base64.getUrlDecoder().decode(getPayLoad(deviceInfo)));
			return mapper.readValue(result, MdmDeviceInfo.class);
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_ID, APPLICATION_NAME, "Failed to decode device info",
					ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}

	public void validateJWTResponse(final String signedData) throws DeviceException {
		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
		jwtSignatureVerifyRequestDto.setValidateTrust(validateTrust);
		jwtSignatureVerifyRequestDto.setJwtSignatureData(signedData);
		JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = signatureService
				.jwtVerify(jwtSignatureVerifyRequestDto);
		if (!jwtSignatureVerifyResponseDto.isSignatureValid())
			throw new DeviceException(MDMError.MDM_INVALID_SIGNATURE.getErrorCode(),
					MDMError.MDM_INVALID_SIGNATURE.getErrorMessage());

		if (jwtSignatureVerifyRequestDto.getValidateTrust()
				&& !jwtSignatureVerifyResponseDto.getTrustValid().equals(SignatureConstant.TRUST_VALID)) {
			throw new DeviceException(MDMError.MDM_CERT_PATH_TRUST_FAILED.getErrorCode(),
					MDMError.MDM_CERT_PATH_TRUST_FAILED.getErrorMessage());
		}
	}

	public String generateMDMTransactionId() {
		return UUID.randomUUID().toString();
	}

	public String buildUrl(int port, String endPoint) {
		return String.format("%s:%s/%s", getRunningurl(), port, endPoint);
	}

	private String getRunningurl() {
		return "http://127.0.0.1";
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Using the urlStream get the next JPEG image as a byte[]
	 *
	 * @return byte[] of the JPEG
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	public byte[] getJPEGByteArray(InputStream urlStream, long maxTimeLimit)
			throws IOException, RegBaseCheckedException {

		int currByte = -1;

		boolean captureContentLength = false;
		StringWriter contentLengthStringWriter = new StringWriter(128);
		StringWriter headerWriter = new StringWriter(128);

		int contentLength = 0;

		while ((currByte = urlStream.read()) > -1) {
			if (captureContentLength) {
				if (currByte == 10 || currByte == 13) {
					contentLength = Integer.parseInt(contentLengthStringWriter.toString().replace(" ", ""));
					break;
				}
				contentLengthStringWriter.write(currByte);

			} else {
				headerWriter.write(currByte);
				String tempString = headerWriter.toString();
				int indexOf = tempString.indexOf(CONTENT_LENGTH);
				if (indexOf > 0) {
					captureContentLength = true;
				}
			}
			timeOutCheck(maxTimeLimit);
		}

		// 255 indicates the start of the jpeg image
		while (urlStream.read() != 255) {

			timeOutCheck(maxTimeLimit);
		}

		// rest is the buffer
		byte[] imageBytes = new byte[contentLength + 1];
		// since we ate the original 255 , shove it back in
		imageBytes[0] = (byte) 255;
		int offset = 1;
		int numRead = 0;
		while (offset < imageBytes.length
				&& (numRead = urlStream.read(imageBytes, offset, imageBytes.length - offset)) >= 0) {
			timeOutCheck(maxTimeLimit);
			offset += numRead;
		}

		return imageBytes;
	}

	private void timeOutCheck(long maxTimeLimit) throws RegBaseCheckedException {

		if (System.currentTimeMillis() > maxTimeLimit) {

			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_TIMEOUT.getErrorCode(),
					RegistrationExceptionConstants.MDS_STREAM_TIMEOUT.getErrorMessage());
		}
	}
}
