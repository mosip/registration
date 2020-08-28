package io.mosip.registration.processor.status.encryptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.kernel.core.exception.ExceptionUtils;
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
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.constants.EncryptionFailureExceptionConstant;
import io.mosip.registration.processor.status.dto.CryptomanagerRequestDto;
import io.mosip.registration.processor.status.dto.CryptomanagerResponseDto;
import io.mosip.registration.processor.status.exception.EncryptionFailureException;

@Component
public class Encryptor {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(Encryptor.class);

	@Value("${registration.processor.application.id}")
	private String applicationId;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private Environment env;

	private static final String DECRYPT_SERVICE_ID = "mosip.registration.processor.crypto.decrypt.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	private static final String IO_EXCEPTION = "Exception Converting encrypted packet inputStream to string";

	private static final String ENCRYPTION_SUCCESS = "Encryption success";

	@SuppressWarnings("unchecked")
	public byte[] encrypt(Object syncMetaInfo, String referenceId, String timeStamp)
			throws EncryptionFailureException, ApisResourceAccessException {
		byte[] encryptedData = null;
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"Encryptor::encrypt()::entry");
		try {
			ObjectMapper mapper = new ObjectMapper();

			Gson syncInfoGson = new GsonBuilder().create();
	        String syncInfo = syncInfoGson.toJson(syncMetaInfo);
	        
			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			ResponseWrapper<CryptomanagerResponseDto> response;
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(syncInfo);
			cryptomanagerRequestDto.setReferenceId(referenceId);
			CryptomanagerResponseDto cryptomanagerResponseDto;

			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime time = LocalDateTime.parse(timeStamp, format);
			cryptomanagerRequestDto.setTimeStamp(time);

			request.setId(env.getProperty(DECRYPT_SERVICE_ID));
			request.setMetadata(null);
			request.setRequest(cryptomanagerRequestDto);

			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);
			request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

			response = (ResponseWrapper<CryptomanagerResponseDto>) restClientService.postApi(ApiName.ENCRYPTURL, "", "",
					request, ResponseWrapper.class);
			if (response.getResponse() != null) {
				cryptomanagerResponseDto = mapper.readValue(mapper.writeValueAsString(response.getResponse()),
						CryptomanagerResponseDto.class);
				encryptedData = (CryptoUtil.encodeBase64(cryptomanagerResponseDto.getData().getBytes())).getBytes();
			} else {
				description.setMessage(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getCode());
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "", IO_EXCEPTION);
				throw new EncryptionFailureException(response.getErrors().get(0).getErrorCode(),
						response.getErrors().get(0).getMessage());
			}

			isTransactionSuccessful = true;
			description.setMessage(PlatformSuccessMessages.RPR_ENCRYPTION_SUCCESS.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_ENCRYPTION_SUCCESS.getCode());
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "Encryptor::encrypt()::exit");
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", description.getMessage());
		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", IO_EXCEPTION);
			description.setMessage(PlatformErrorMessages.RPR_PDS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PDS_IO_EXCEPTION.getCode());

			throw new EncryptionFailureException(
					EncryptionFailureExceptionConstant.MOSIP_ENCRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
					IO_EXCEPTION);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "Internal Error occurred " + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				description.setMessage(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getMessage()
						+ httpClientException.getResponseBodyAsString());
				description.setCode(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getCode());
				throw new EncryptionFailureException(
						EncryptionFailureExceptionConstant.MOSIP_ENCRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
						httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				description.setMessage(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getMessage()
						+ httpServerException.getResponseBodyAsString());
				description.setCode(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getCode());

				throw new EncryptionFailureException(
						EncryptionFailureExceptionConstant.MOSIP_ENCRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
						httpServerException.getResponseBodyAsString());
			} else {
				description.setMessage(
						PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getMessage() + e.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PGS_ENCRYPTOR_INVLAID_DATA_EXCEPTION.getCode());

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
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_ENCRYPTION_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.ENCRYPTOR.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, "");
		}
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				ENCRYPTION_SUCCESS);
		return encryptedData;
	}

}