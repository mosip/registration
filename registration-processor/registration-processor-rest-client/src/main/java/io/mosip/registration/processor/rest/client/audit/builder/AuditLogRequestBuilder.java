package io.mosip.registration.processor.rest.client.audit.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.AuditLogConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.ServerUtil;
import io.mosip.registration.processor.rest.client.audit.dto.AuditRequestDto;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;

/**
 * The Class AuditRequestBuilder - Virtual Thread based Async Version
 * BACKWARD COMPATIBLE - All original method signatures preserved
 *
 * @author Rishabh Keshari
 */
@Component
public class AuditLogRequestBuilder {

	private final Logger regProcLogger = RegProcessorLogger.getLogger(AuditLogRequestBuilder.class);

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;

	@Autowired(required = false)
	@Qualifier("auditExecutor")
	private Executor auditExecutor;

	private static final Executor VIRTUAL_THREAD_EXECUTOR =
			Executors.newVirtualThreadPerTaskExecutor();

	private static final String AUDIT_SERVICE_ID = "mosip.registration.processor.audit.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	// Cached at startup — these never change at runtime
	private String dateTimePattern;
	private DateTimeFormatter dateTimeFormatter;
	private String auditServiceId;
	private String appVersion;
	private String serverIp;
	private String serverName;

	@PostConstruct
	public void init() {
		dateTimePattern = env.getProperty(DATETIME_PATTERN);
		dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
		auditServiceId = env.getProperty(AUDIT_SERVICE_ID);
		appVersion = env.getProperty(REG_PROC_APPLICATION_VERSION);
		serverIp = ServerUtil.getServerUtilInstance().getServerIp();
		serverName = ServerUtil.getServerUtilInstance().getServerName();
	}

	private Executor getExecutor() {
		return auditExecutor != null ? auditExecutor : VIRTUAL_THREAD_EXECUTOR;
	}

	/**
	 * Unwraps ExecutionException from CompletableFuture.get() and rethrows
	 * the original cause — so sync catch blocks see the real exception type.
	 */
	private ResponseWrapper<AuditResponseDto> getFromFuture(
			CompletableFuture<ResponseWrapper<AuditResponseDto>> future)
			throws ApisResourceAccessException, Exception {
		try {
			return future.orTimeout(5, java.util.concurrent.TimeUnit.SECONDS).get();
		} catch (ExecutionException ee) {
			Throwable cause = ee.getCause();
			if (cause instanceof ApisResourceAccessException) {
				throw (ApisResourceAccessException) cause;
			}
			throw new Exception(cause != null ? cause.getMessage() : ee.getMessage(), cause);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new Exception("Audit interrupted: " + ie.getMessage(), ie);
		}
	}

	// =========================================================================
	// SYNC METHOD 1 — calls async internally, original error logging preserved
	// =========================================================================

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String registrationId, ApiName apiname) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::entry");

		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		try {
			responseWrapper = getFromFuture(
					createAuditRequestBuilderAsync(description, eventId, eventName, eventType, registrationId, apiname));
		} catch (ApisResourceAccessException arae) {
			regProcLogger.error(arae.getMessage()); // short format — test expects this
		} catch (Exception e) {
			regProcLogger.error("Error in createAuditRequestBuilder: " + e.getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::exit");
		return responseWrapper;
	}

	// =========================================================================
	// ASYNC METHOD 1 — NO .exceptionally() so exception propagates to sync
	// =========================================================================

	public CompletableFuture<ResponseWrapper<AuditResponseDto>> createAuditRequestBuilderAsync(String description,
																							   String eventId, String eventName, String eventType, String registrationId, ApiName apiname) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				AuditRequestDto auditRequestDto = new AuditRequestDto();
				auditRequestDto.setDescription(description);
				auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(dateTimePattern));
				auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
				auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
				auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setEventId(eventId);
				auditRequestDto.setEventName(eventName);
				auditRequestDto.setEventType(eventType);
				auditRequestDto.setHostIp(serverIp);
				auditRequestDto.setHostName(serverName);
				auditRequestDto.setId(registrationId);
				auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
				auditRequestDto.setModuleId(null);
				auditRequestDto.setModuleName(null);
				auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setSessionUserName(null);

				RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
				requestWrapper.setId(auditServiceId);
				requestWrapper.setMetadata(null);
				requestWrapper.setRequest(auditRequestDto);
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils2.getUTCCurrentDateTimeString(dateTimePattern), dateTimeFormatter);
				requestWrapper.setRequesttime(localdatetime);
				requestWrapper.setVersion(appVersion);

				@SuppressWarnings("unchecked")
				ResponseWrapper<AuditResponseDto> response = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(apiname, "", "", requestWrapper, ResponseWrapper.class);

				ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
				if (response != null) {
					responseWrapper = response;
				}
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Async audit log created successfully");
				return responseWrapper;

			} catch (ApisResourceAccessException arae) {
				// log long format for async callers, then rethrow so getFromFuture can unwrap
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Audit API Error: " + arae.getMessage());
				throw new java.util.concurrent.CompletionException(arae); // preserves original type
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Unexpected error in async audit logging: " + e.getMessage());
				throw new java.util.concurrent.CompletionException(e);
			}
		}, getExecutor());
	}

	// =========================================================================
	// SYNC METHOD 2 — calls async internally, original error logging preserved
	// =========================================================================

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId,
																	   AuditLogConstant auditLogConstant) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		try {
			responseWrapper = getFromFuture(
					createAuditRequestBuilderAsync(description, eventId, eventName, eventType,
							moduleId, moduleName, registrationId, auditLogConstant));
		} catch (ApisResourceAccessException arae) {
			regProcLogger.error(arae.getMessage()); // short format — test expects this
		} catch (Exception e) {
			regProcLogger.error("Error in createAuditRequestBuilder: " + e.getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");
		return responseWrapper;
	}

	// =========================================================================
	// ASYNC METHOD 2 — NO .exceptionally() so exception propagates to sync
	// =========================================================================

	public CompletableFuture<ResponseWrapper<AuditResponseDto>> createAuditRequestBuilderAsync(String description,
																							   String eventId, String eventName, String eventType, String moduleId, String moduleName,
																							   String registrationId, AuditLogConstant auditLogConstant) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				AuditRequestDto auditRequestDto = new AuditRequestDto();
				auditRequestDto.setDescription(description);
				auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(dateTimePattern));
				auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
				auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
				auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setEventId(eventId);
				auditRequestDto.setEventName(eventName);
				auditRequestDto.setEventType(eventType);
				auditRequestDto.setHostIp(serverIp);
				auditRequestDto.setHostName(serverName);
				auditRequestDto.setId(registrationId);
				auditRequestDto.setIdType(auditLogConstant.toString());
				auditRequestDto.setModuleId(moduleId);
				auditRequestDto.setModuleName(moduleName);
				auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setSessionUserName(null);

				RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
				requestWrapper.setId(auditServiceId);
				requestWrapper.setMetadata(null);
				requestWrapper.setRequest(auditRequestDto);
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils2.getUTCCurrentDateTimeString(dateTimePattern), dateTimeFormatter);
				requestWrapper.setRequesttime(localdatetime);
				requestWrapper.setVersion(appVersion);

				@SuppressWarnings("unchecked")
				ResponseWrapper<AuditResponseDto> response = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);

				ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
				if (response != null) {
					responseWrapper = response;
				}
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Async audit log with modules created successfully");
				return responseWrapper;

			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Audit API Error: " + arae.getMessage());
				throw new java.util.concurrent.CompletionException(arae);
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Unexpected error in async audit logging: " + e.getMessage());
				throw new java.util.concurrent.CompletionException(e);
			}
		}, getExecutor());
	}

	// =========================================================================
	// SYNC METHOD 3 — calls async internally, original error logging preserved
	// =========================================================================

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId,
																	   String userId) {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		try {
			responseWrapper = getFromFuture(
					createAuditRequestBuilderAsync(description, eventId, eventName, eventType,
							moduleId, moduleName, registrationId, userId));
		} catch (ApisResourceAccessException arae) {
			regProcLogger.error(arae.getMessage()); // short format — test expects this
		} catch (Exception e) {
			regProcLogger.error("Error in createAuditRequestBuilder: " + e.getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");
		return responseWrapper;
	}

	// =========================================================================
	// ASYNC METHOD 3 — NO .exceptionally() so exception propagates to sync
	// =========================================================================

	public CompletableFuture<ResponseWrapper<AuditResponseDto>> createAuditRequestBuilderAsync(String description,
																							   String eventId, String eventName, String eventType, String moduleId, String moduleName,
																							   String registrationId, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				AuditRequestDto auditRequestDto = new AuditRequestDto();
				auditRequestDto.setDescription(description);
				auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(dateTimePattern));
				auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
				auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
				auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setEventId(eventId);
				auditRequestDto.setEventName(eventName);
				auditRequestDto.setEventType(eventType);
				auditRequestDto.setHostIp(serverIp);
				auditRequestDto.setHostName(serverName);
				auditRequestDto.setId(registrationId);
				auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
				auditRequestDto.setModuleId(moduleId);
				auditRequestDto.setModuleName(moduleName);
				auditRequestDto.setSessionUserId(userId);
				auditRequestDto.setSessionUserName(null);

				RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
				requestWrapper.setId(auditServiceId);
				requestWrapper.setMetadata(null);
				requestWrapper.setRequest(auditRequestDto);
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils2.getUTCCurrentDateTimeString(dateTimePattern), dateTimeFormatter);
				requestWrapper.setRequesttime(localdatetime);
				requestWrapper.setVersion(appVersion);

				@SuppressWarnings("unchecked")
				ResponseWrapper<AuditResponseDto> response = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);

				ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
				if (response != null) {
					responseWrapper = response;
				}
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Async audit log with userId created successfully");
				return responseWrapper;

			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Audit API Error: " + arae.getMessage());
				throw new java.util.concurrent.CompletionException(arae);
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Unexpected error in async audit logging: " + e.getMessage());
				throw new java.util.concurrent.CompletionException(e);
			}
		}, getExecutor());
	}

	// =========================================================================
	// CONVENIENCE OVERLOAD — original signature preserved
	// =========================================================================

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId) {
		return createAuditRequestBuilder(description, eventId, eventName, eventType, moduleId, moduleName,
				registrationId, AuditLogConstant.REGISTRATION_ID);
	}
}