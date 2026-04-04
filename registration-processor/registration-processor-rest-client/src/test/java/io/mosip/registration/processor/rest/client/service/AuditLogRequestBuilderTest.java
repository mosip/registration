package io.mosip.registration.processor.rest.client.service;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class AuditLogRequestBuilderTest {

	@InjectMocks
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Mock
	private Environment env;

	AuditResponseDto dto;
	private Logger fooLogger;

	private ListAppender<ILoggingEvent> listAppender;

	private static final String AUDIT_SERVICE_ID = "mosip.registration.processor.audit.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	@Before
	public void setup() {
		Mockito.when(env.getProperty(AUDIT_SERVICE_ID)).thenReturn("mosip.applicanttype.getApplicantType");
		Mockito.when(env.getProperty(REG_PROC_APPLICATION_VERSION)).thenReturn("1.0");
		Mockito.when(env.getProperty(DATETIME_PATTERN)).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		auditLogRequestBuilder.init();
	}

	@Test
	public void createAuditRequestBuilderTest() throws ApisResourceAccessException {
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		dto = new AuditResponseDto();
		dto.setStatus(true);
		responseWrapper.setResponse(dto);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		auditLogRequestBuilder.createAuditRequestBuilder("abcde", "200", "ADD", "ADD", "123456789", ApiName.AUDIT);
		verify(registrationProcessorRestService, timeout(2000)).postApi(any(), any(), any(), any(), any());
	}

	@Test
	public void createAuditRequestBuilderForModuleTest() throws ApisResourceAccessException {
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		dto = new AuditResponseDto();
		dto.setStatus(true);
		responseWrapper.setResponse(dto);
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any())).thenReturn(responseWrapper);
		auditLogRequestBuilder.createAuditRequestBuilder("abcde", "200", "ADD", "ADD", "moduleID", "moduleName", "123456789");
		verify(registrationProcessorRestService, timeout(2000)).postApi(any(), any(), any(), any(), any());
	}

	@Test
	public void createAuditRequestBuilderFailureTest() throws ApisResourceAccessException {
		fooLogger = (Logger) LoggerFactory.getLogger(AuditLogRequestBuilder.class);
		listAppender = new ListAppender<>();
		listAppender.start();
		fooLogger.addAppender(listAppender);
		ApisResourceAccessException exp = new ApisResourceAccessException("errorMessage");
		Mockito.when(registrationProcessorRestService.postApi(any(), any(), any(), any(), any())).thenThrow(exp);

		auditLogRequestBuilder.createAuditRequestBuilder("abcde", "200", "ADD", "ADD", "123456789", ApiName.AUDIT);
		auditLogRequestBuilder.createAuditRequestBuilder("abcde", "200", "ADD", "ADD", "moduleID", "moduleName", "123456789");

		// Wait for both async audit calls to complete before asserting logs
		verify(registrationProcessorRestService, timeout(2000).times(2)).postApi(any(), any(), any(), any(), any());

		Assertions.assertThat(listAppender.list)
			.extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
			.contains(Tuple.tuple(Level.ERROR, "RPR-RCT-001 --> errorMessage"));
	}
}
