package io.mosip.registration.processor.rest.client.audit.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
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
 * The Class AuditRequestBuilder - Async Version for High Performance
 * BACKWARD COMPATIBLE - All original method signatures preserved
 *
 * @author Rishabh Keshari
 */
@Component
public class AuditLogRequestBuilder {

	/** The logger. */
	private final Logger regProcLogger = RegProcessorLogger.getLogger(AuditLogRequestBuilder.class);

	/** The registration processor rest service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;

	@Autowired(required = false)
	private Executor auditExecutor;

	private static final String AUDIT_SERVICE_ID = "mosip.registration.processor.audit.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/**
	 * ORIGINAL METHOD - UNCHANGED SIGNATURE
	 * Creates audit request with event details - synchronous call
	 * Internally uses async processing for better performance
	 */
	@SuppressWarnings("unchecked")
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String registrationId, ApiName apiname) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::entry");

		AuditRequestDto auditRequestDto = new AuditRequestDto();
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		try {
			auditRequestDto.setDescription(description);
			auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
			auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
			auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setEventId(eventId);
			auditRequestDto.setEventName(eventName);
			auditRequestDto.setEventType(eventType);
			auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
			auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
			auditRequestDto.setId(registrationId);
			auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
			auditRequestDto.setModuleId(null);
			auditRequestDto.setModuleName(null);
			auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setSessionUserName(null);
			requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
			requestWrapper.setMetadata(null);
			requestWrapper.setRequest(auditRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

			// Call async version internally but block for backward compatibility
			if (auditExecutor != null) {
				responseWrapper = createAuditRequestBuilderAsync(description, eventId, eventName, eventType, registrationId, apiname)
						.orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
						.get();
			} else {
				// Fallback to synchronous if executor not available
				responseWrapper = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService.postApi(apiname, "",
						"", requestWrapper, ResponseWrapper.class);
			}
		} catch (ApisResourceAccessException arae) {
			regProcLogger.error(arae.getMessage());
		} catch (Exception e) {
			regProcLogger.error("Error in createAuditRequestBuilder: " + e.getMessage());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::exit");

		return responseWrapper;
	}

	/**
	 * NEW ASYNC METHOD (non-breaking) - For high-performance scenarios
	 * Returns CompletableFuture for non-blocking async operations
	 */
	@Async("auditExecutor")
	public CompletableFuture<ResponseWrapper<AuditResponseDto>> createAuditRequestBuilderAsync(String description,
																							   String eventId, String eventName, String eventType, String registrationId, ApiName apiname) {
		return CompletableFuture.supplyAsync(() -> {
			ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
			try {
				AuditRequestDto auditRequestDto = new AuditRequestDto();
				auditRequestDto.setDescription(description);
				auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
				auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
				auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
				auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setEventId(eventId);
				auditRequestDto.setEventName(eventName);
				auditRequestDto.setEventType(eventType);
				auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
				auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
				auditRequestDto.setId(registrationId);
				auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
				auditRequestDto.setModuleId(null);
				auditRequestDto.setModuleName(null);
				auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setSessionUserName(null);

				RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
				requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
				requestWrapper.setMetadata(null);
				requestWrapper.setRequest(auditRequestDto);
				DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
				requestWrapper.setRequesttime(localdatetime);
				requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

				@SuppressWarnings("unchecked")
				ResponseWrapper<AuditResponseDto> response = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(apiname, "", "", requestWrapper, ResponseWrapper.class);

				if (response != null) {
					responseWrapper = response;
				}
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Async audit log created successfully");
			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Audit API Error: " + arae.getMessage());
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Unexpected error in async audit logging: " + e.getMessage());
			}
			return responseWrapper;
		}, auditExecutor).exceptionally(ex -> {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Async audit logging failed: " + ex.getMessage());
			return new ResponseWrapper<>();
		});
	}


	/**
	 * ORIGINAL METHOD - UNCHANGED SIGNATURE
	 * Overloaded with module details
	 */
	@SuppressWarnings("unchecked")
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId,
																	   AuditLogConstant auditLogConstant) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		AuditRequestDto auditRequestDto;
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();

		try {

			auditRequestDto = new AuditRequestDto();
			auditRequestDto.setDescription(description);
			auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
			auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
			auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setEventId(eventId);
			auditRequestDto.setEventName(eventName);
			auditRequestDto.setEventType(eventType);
			auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
			auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
			auditRequestDto.setId(registrationId);
			auditRequestDto.setIdType(auditLogConstant.toString());
			auditRequestDto.setModuleId(moduleId);
			auditRequestDto.setModuleName(moduleName);
			auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setSessionUserName(null);
			requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
			requestWrapper.setMetadata(null);
			requestWrapper.setRequest(auditRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

			// Call async version internally but block for backward compatibility
			if (auditExecutor != null) {
				responseWrapper = createAuditRequestBuilderAsync(description, eventId, eventName, eventType, moduleId,
						moduleName, registrationId, auditLogConstant)
						.orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
						.get();
			} else {
				// Fallback to synchronous if executor not available
				responseWrapper = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);
			}

		} catch (ApisResourceAccessException arae) {

			regProcLogger.error(arae.getMessage());

		} catch (Exception e) {
			regProcLogger.error("Error in createAuditRequestBuilder: " + e.getMessage());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");

		return responseWrapper;
	}

	/**
	 * NEW ASYNC METHOD (non-breaking) - With module details
	 */
	@Async("auditExecutor")
	public CompletableFuture<ResponseWrapper<AuditResponseDto>> createAuditRequestBuilderAsync(String description,
																							   String eventId, String eventName, String eventType, String moduleId, String moduleName,
																							   String registrationId, AuditLogConstant auditLogConstant) {
		return CompletableFuture.supplyAsync(() -> {
			ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
			try {
				AuditRequestDto auditRequestDto = new AuditRequestDto();
				auditRequestDto.setDescription(description);
				auditRequestDto.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
				auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
				auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
				auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setEventId(eventId);
				auditRequestDto.setEventName(eventName);
				auditRequestDto.setEventType(eventType);
				auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
				auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
				auditRequestDto.setId(registrationId);
				auditRequestDto.setIdType(auditLogConstant.toString());
				auditRequestDto.setModuleId(moduleId);
				auditRequestDto.setModuleName(moduleName);
				auditRequestDto.setSessionUserId(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setSessionUserName(null);

				RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
				requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
				requestWrapper.setMetadata(null);
				requestWrapper.setRequest(auditRequestDto);
				DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
				requestWrapper.setRequesttime(localdatetime);
				requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

				@SuppressWarnings("unchecked")
				ResponseWrapper<AuditResponseDto> response = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);

				if (response != null) {
					responseWrapper = response;
				}
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Async audit log with modules created successfully");
			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Audit API Error: " + arae.getMessage());
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Unexpected error in async audit logging: " + e.getMessage());
			}
			return responseWrapper;
		}, auditExecutor).exceptionally(ex -> {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Async audit logging failed: " + ex.getMessage());
			return new ResponseWrapper<>();
		});
	}


	/**
	 * ORIGINAL METHOD - UNCHANGED SIGNATURE
	 * Overloaded with userId
	 */
	@SuppressWarnings("unchecked")
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId,
																	   String userId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		AuditRequestDto auditRequestDto;
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();

		try {

			auditRequestDto = new AuditRequestDto();
			auditRequestDto.setDescription(description);
			auditRequestDto
					.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
			auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
			auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
			auditRequestDto.setEventId(eventId);
			auditRequestDto.setEventName(eventName);
			auditRequestDto.setEventType(eventType);
			auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
			auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
			auditRequestDto.setId(registrationId);
			auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
			auditRequestDto.setModuleId(moduleId);
			auditRequestDto.setModuleName(moduleName);
			auditRequestDto.setSessionUserId(userId);
			auditRequestDto.setSessionUserName(null);
			requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
			requestWrapper.setMetadata(null);
			requestWrapper.setRequest(auditRequestDto);
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

			// Call async version internally but block for backward compatibility
			if (auditExecutor != null) {
				responseWrapper = createAuditRequestBuilderAsync(description, eventId, eventName, eventType, moduleId,
						moduleName, registrationId,userId)
						.orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
						.get();
			} else {
				// Fallback to synchronous if executor not available
				responseWrapper = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);
			}

		} catch (ApisResourceAccessException arae) {

			regProcLogger.error(arae.getMessage());

		} catch (Exception e) {
			regProcLogger.error("Error in createAuditRequestBuilder: " + e.getMessage());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");

		return responseWrapper;
	}

	/**
	 * NEW ASYNC METHOD (non-breaking) - With userId
	 */
	@Async("auditExecutor")
	public CompletableFuture<ResponseWrapper<AuditResponseDto>> createAuditRequestBuilderAsync(String description,
																							   String eventId, String eventName, String eventType, String moduleId, String moduleName,
																							   String registrationId,String userId) {
		return CompletableFuture.supplyAsync(() -> {
			ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
			try {
				AuditRequestDto auditRequestDto = new AuditRequestDto();
				auditRequestDto.setDescription(description);
				auditRequestDto
						.setActionTimeStamp(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
				auditRequestDto.setApplicationId(AuditLogConstant.MOSIP_4.toString());
				auditRequestDto.setApplicationName(AuditLogConstant.REGISTRATION_PROCESSOR.toString());
				auditRequestDto.setCreatedBy(AuditLogConstant.SYSTEM.toString());
				auditRequestDto.setEventId(eventId);
				auditRequestDto.setEventName(eventName);
				auditRequestDto.setEventType(eventType);
				auditRequestDto.setHostIp(ServerUtil.getServerUtilInstance().getServerIp());
				auditRequestDto.setHostName(ServerUtil.getServerUtilInstance().getServerName());
				auditRequestDto.setId(registrationId);
				auditRequestDto.setIdType(AuditLogConstant.REGISTRATION_ID.toString());
				auditRequestDto.setModuleId(moduleId);
				auditRequestDto.setModuleName(moduleName);
				auditRequestDto.setSessionUserId(userId);
				auditRequestDto.setSessionUserName(null);

				RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
				requestWrapper.setId(env.getProperty(AUDIT_SERVICE_ID));
				requestWrapper.setMetadata(null);
				requestWrapper.setRequest(auditRequestDto);
				DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
				LocalDateTime localdatetime = LocalDateTime
						.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
				requestWrapper.setRequesttime(localdatetime);
				requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

				@SuppressWarnings("unchecked")
				ResponseWrapper<AuditResponseDto> response = (ResponseWrapper<AuditResponseDto>) registrationProcessorRestService
						.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);

				if (response != null) {
					responseWrapper = response;
				}
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Async audit log with userId created successfully");
			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Audit API Error: " + arae.getMessage());
			} catch (Exception e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						registrationId, "Unexpected error in async audit logging: " + e.getMessage());
			}
			return responseWrapper;
		}, auditExecutor).exceptionally(ex -> {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "Async audit logging failed: " + ex.getMessage());
			return new ResponseWrapper<>();
		});
	}

	/**
	 * ORIGINAL METHOD - Convenience overload
	 */
	@SuppressWarnings("unchecked")
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId) {
		return createAuditRequestBuilder(description, eventId, eventName, eventType, moduleId, moduleName, registrationId, AuditLogConstant.REGISTRATION_ID);
	}
}