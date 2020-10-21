package io.mosip.registration.processor.packet.manager.decryptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.registration.processor.packet.manager.constant.CryptomanagerConstant;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.dto.CryptomanagerRequestDto;
import io.mosip.registration.processor.packet.manager.dto.CryptomanagerResponseDto;
import io.mosip.registration.processor.packet.manager.exception.PacketDecryptionFailureExceptionConstant;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;

/**
 * Decryptor class for packet decryption.
 *
 * @author Girish Yarru
 * @author Abhishek Kumar
 * @since 1.0.0
 */
@Component
public class DecryptorImpl implements Decryptor {
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DecryptorImpl.class);

	@Value("${registration.processor.application.id}")
	private String applicationId;

	@Value("${mosip.kernel.machineid.length}")
	private int machineIdLength;

	@Value("${mosip.kernel.registrationcenterid.length}")
	private int centerIdLength;

	@Value("${registration.processor.rid.machineidsubstring}")
	private int machineIdSubStringLength;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private Environment env;

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
	public InputStream decrypt(InputStream packetStream, String registrationId)
			throws PacketDecryptionFailureException, ApisResourceAccessException {
		InputStream outstream = null;
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "Decryptor::decrypt()::entry");
		try {
			byte[] packet = IOUtils.toByteArray(packetStream);
			String centerId = registrationId.substring(0, centerIdLength);
			String machineId = registrationId.substring(centerIdLength, machineIdSubStringLength);
			String refId = centerId + "_" + machineId;
			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			io.mosip.kernel.core.http.RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setReferenceId(refId);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
					"Size = " + packet.length);
			byte[] nonce = Arrays.copyOfRange(packet, 0, CryptomanagerConstant.GCM_NONCE_LENGTH);
			byte[] aad = Arrays.copyOfRange(packet, CryptomanagerConstant.GCM_NONCE_LENGTH,
					CryptomanagerConstant.GCM_NONCE_LENGTH + CryptomanagerConstant.GCM_AAD_LENGTH);
			byte[] encryptedData = Arrays.copyOfRange(packet, CryptomanagerConstant.GCM_NONCE_LENGTH + CryptomanagerConstant.GCM_AAD_LENGTH,
					packet.length);
			cryptomanagerRequestDto.setAad(CryptoUtil.encodeBase64String(aad));
			cryptomanagerRequestDto.setSalt(CryptoUtil.encodeBase64String(nonce));
			cryptomanagerRequestDto.setData(CryptoUtil.encodeBase64String(encryptedData));
			// setLocal Date Time
			if (registrationId.length() > 14) {
				String packetCreatedDateTime = registrationId.substring(registrationId.length() - 14);
				String formattedDate = packetCreatedDateTime.substring(0, 8) + "T"
						+ packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6);

				cryptomanagerRequestDto.setTimeStamp(
						LocalDateTime.parse(formattedDate, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")));
			} else {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"Packet DecryptionFailed-Invalid Packet format");

				throw new PacketDecryptionFailureException(
						PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE
								.getErrorCode(),
						"Packet DecryptionFailed-Invalid Packet format");
			}
			request.setId(env.getProperty(DECRYPT_SERVICE_ID));
			request.setMetadata(null);
			request.setRequest(cryptomanagerRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
			CryptomanagerResponseDto response;
			response = (CryptomanagerResponseDto) restClientService.postApi(ApiName.CRYPTOMANAGERDECRYPT, "", "",
					request, CryptomanagerResponseDto.class);
			if (response.getErrors() != null && !response.getErrors().isEmpty()) {
				ServiceError error = response.getErrors().get(0);
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, DECRYPTION_FAILURE);
				description.setMessage(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getCode());
				throw new PacketDecryptionFailureException(error.getErrorCode(), error.getMessage());
			}
			byte[] decryptedPacket = CryptoUtil.decodeBase64(response.getResponse().getData());
			outstream = new ByteArrayInputStream(decryptedPacket);
			isTransactionSuccessful = true;
			description.setMessage(PlatformSuccessMessages.RPR_DECRYPTION_SUCCESS.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_DECRYPTION_SUCCESS.getCode());

			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Decryptor::decrypt()::exit");
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, description.getMessage());
		} catch (IOException e) {
			description.setMessage(PlatformErrorMessages.RPR_PDS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PDS_IO_EXCEPTION.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, description.getMessage());
			throw new PacketDecryptionFailureException(
					PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
					IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			description.setMessage(PlatformErrorMessages.RPR_PDS_DATE_TIME_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PDS_DATE_TIME_EXCEPTION.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, description.getMessage());
			throw new PacketDecryptionFailureException(
					PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
					DATE_TIME_EXCEPTION);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Internal Error occurred ");
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();

				description.setMessage(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getMessage()
						+ httpClientException.getResponseBodyAsString());
				description.setCode(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getCode());

				throw new PacketDecryptionFailureException(
						PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE
								.getErrorCode(),
						httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				description.setMessage(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getMessage()
						+ httpServerException.getResponseBodyAsString());
				description.setCode(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getCode());

				throw new PacketDecryptionFailureException(
						PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE
								.getErrorCode(),
						httpServerException.getResponseBodyAsString());
			} else {
				description.setMessage(
						PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getMessage() + e.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PDS_PACKET_DECRYPTION_FAILURE.getCode());

				throw e;
			}

		} finally {
			String eventId = "";
			String eventName = "";
			String eventType = "";
			eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();

			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_DECRYPTION_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.DECRYPTOR.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, DECRYPTION_SUCCESS);
		return outstream;
	}

}