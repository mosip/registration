package io.mosip.registration.processor.packet.utility.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.packet.utility.constants.LoggerFileConstant;
import io.mosip.registration.processor.packet.utility.dto.CryptomanagerRequestDto;
import io.mosip.registration.processor.packet.utility.dto.CryptomanagerResponseDto;
import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.packet.utility.logger.PacketUtilityLogger;
import io.mosip.registration.processor.packet.utility.service.PacketDecryptor;

/**
 * Decryptor class for packet decryption.
 *
 * @author Sowmya
 * 
 * @since 1.0.0
 */
@Component
public class PacketDecryptorImpl implements PacketDecryptor {
	private static Logger packetUtilityLogger = PacketUtilityLogger.getLogger(PacketDecryptorImpl.class);

	@Value("${registration.processor.application.id}")
	private String applicationId;

	@Value("${mosip.kernel.machineid.length}")
	private int machineIdLength;

	@Value("${mosip.kernel.registrationcenterid.length}")
	private int centerIdLength;

	@Value("${registration.processor.rid.machineidsubstring}")
	private int machineIdSubStringLength;


	@Autowired
	private Environment env;

	// @Autowired
	// private RestTemplate restTemplate;

	@Autowired
	ObjectMapper mapper;

	private static final String DECRYPT_SERVICE_ID = "mosip.registration.processor.crypto.decrypt.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	private static final String DECRYPTION_SUCCESS = "Decryption success";
	private static final String DECRYPTION_FAILURE = "Virus scan decryption failed for registrationId ";
	private static final String IO_EXCEPTION = "Exception while reading packet inputStream";
	private static final String DATE_TIME_EXCEPTION = "Error while parsing packet timestamp";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.decryptor.Decryptor#decrypt(java.io.
	 * InputStream, java.lang.String)
	 */
	@Override
	public InputStream decrypt(InputStream encryptedPacket, String registrationId)
			throws PacketDecryptionFailureException {
		InputStream outstream = null;
		boolean isTransactionSuccessful = false;

		try {
			String centerId = registrationId.substring(0, centerIdLength);
			String machineId = registrationId.substring(centerIdLength, machineIdSubStringLength);
			String refId = centerId + "_" + machineId;
			String encryptedPacketString = IOUtils.toString(encryptedPacket, "UTF-8");
			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(encryptedPacketString);
			cryptomanagerRequestDto.setReferenceId(refId);
			// setLocal Date Time
			if (registrationId.length() > 14) {
				String packetCreatedDateTime = registrationId.substring(registrationId.length() - 14);
				String formattedDate = packetCreatedDateTime.substring(0, 8) + "T"
						+ packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);

				cryptomanagerRequestDto.setTimeStamp(
						LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")));
			} else {
				packetUtilityLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"Packet DecryptionFailed-Invalid Packet format");

				throw new PacketDecryptionFailureException("Packet DecryptionFailed-Invalid Packet format");
			}
			request.setId(env.getProperty(DECRYPT_SERVICE_ID));
			request.setMetadata(null);
			request.setRequest(cryptomanagerRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
			HttpEntity<RequestWrapper<CryptomanagerRequestDto>> httpEntity = new HttpEntity<>(request,
					new HttpHeaders());
			CryptomanagerResponseDto response;
			ResponseEntity<String> responseEntity = null;
			/*
			 * /restTemplate.exchange(env.getProperty("DMZCRYPTOMANAGERDECRYPT"),
			 * HttpMethod.POST, httpEntity, String.class);
			 */
			ResponseWrapper<CryptomanagerResponseDto> responseObject;

				responseObject = mapper.readValue(responseEntity.getBody(), ResponseWrapper.class);
				response = mapper.readValue(mapper.writeValueAsString(responseObject.getResponse()),
						CryptomanagerResponseDto.class);

			if (response.getErrors() != null && !response.getErrors().isEmpty()) {
				ServiceError error = response.getErrors().get(0);
				packetUtilityLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, DECRYPTION_FAILURE);

				throw new PacketDecryptionFailureException(error.getMessage());
			}
			byte[] decryptedPacket = CryptoUtil.decodeBase64(response.getResponse().getData());
			outstream = new ByteArrayInputStream(decryptedPacket);
			isTransactionSuccessful = true;

		} catch (IOException e) {

			packetUtilityLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, e.getMessage());
			throw new PacketDecryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {

			packetUtilityLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, e.getMessage());
			throw new PacketDecryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			packetUtilityLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Internal Error occurred ");
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();



				throw new PacketDecryptionFailureException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();

				throw new PacketDecryptionFailureException(httpServerException.getResponseBodyAsString());
			} else {


				throw e;
			}

		}
		packetUtilityLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, DECRYPTION_SUCCESS);
		return outstream;
	}

}