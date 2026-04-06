package io.mosip.registration.processor.rest.client.audit.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

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
 * The Class AuditRequestBuilder.
 *
 * @author Rishabh Keshari
 */
@Component
public class AuditLogRequestBuilder {

	/** The logger. */
	private final Logger regProcLogger = RegProcessorLogger.getLogger(AuditLogRequestBuilder.class);

	@Value("${mosip.regproc.audit.sender.pool.size:20}")
	private int auditSenderPoolSize = 20;

	private ExecutorService auditExecutor;

	@PostConstruct
	public void init() {
		auditExecutor = Executors.newFixedThreadPool(auditSenderPoolSize, r -> {
			Thread t = new Thread(r, "audit-sender");
			t.setDaemon(true);
			return t;
		});
	}

	/** The registration processor rest service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;

	private static final String AUDIT_SERVICE_ID = "mosip.registration.processor.audit.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/**
	 * Creates the audit request builder.
	 *
	 * @param description
	 *            the description
	 * @param eventId
	 *            the event id
	 * @param eventName
	 *            the event name
	 * @param eventType
	 *            the event type
	 * @param registrationId
	 *            the registration id
	 * @return the audit response dto
	 */
	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String registrationId, ApiName apiname) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::entry");

		AuditRequestDto auditRequestDto = new AuditRequestDto();
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
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
		auditExecutor.execute(() -> {
			try {
				registrationProcessorRestService.postApi(apiname, "", "", requestWrapper, ResponseWrapper.class);
			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(arae.getMessage());
			}
		});
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,\r\n"
						+ "			String registrationId, ApiName apiname)::exit");

		return new ResponseWrapper<>();
	}

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId,AuditLogConstant auditLogConstant) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		AuditRequestDto auditRequestDto = new AuditRequestDto();
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
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
		auditExecutor.execute(() -> {
			try {
				registrationProcessorRestService.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);
			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(arae.getMessage());
			}
		});
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");

		return new ResponseWrapper<>();
	}

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId,
																	   String userId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::entry");

		AuditRequestDto auditRequestDto = new AuditRequestDto();
		RequestWrapper<AuditRequestDto> requestWrapper = new RequestWrapper<>();
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
		auditExecutor.execute(() -> {
			try {
				registrationProcessorRestService.postApi(ApiName.AUDIT, "", "", requestWrapper, ResponseWrapper.class);
			} catch (ApisResourceAccessException arae) {
				regProcLogger.error(arae.getMessage());
			}
		});
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId,
				"AuditLogRequestBuilder:: createAuditRequestBuilder(String description, String eventId, String eventName, String eventType,String moduleId,String moduleName,\r\n"
						+ "			String registrationId)::exit");

		return new ResponseWrapper<>();
	}

	public ResponseWrapper<AuditResponseDto> createAuditRequestBuilder(String description, String eventId,
																	   String eventName, String eventType, String moduleId, String moduleName, String registrationId) {
		return createAuditRequestBuilder(description, eventId, eventName, eventType, moduleId, moduleName, registrationId, AuditLogConstant.REGISTRATION_ID);
	}
}