package io.mosip.registration.processor.stages.osivalidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.AuthSystemException;
import io.mosip.registration.processor.core.exception.ParentOnHoldException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class OSIValidatorStageTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JsonUtil.class, IOUtils.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class OSIValidatorStageTest {

	/** The osi validator stage. */
	@InjectMocks
	private OSIValidatorStage osiValidatorStage = new OSIValidatorStage() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			return null;
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress) {
		}
	};

	@Mock
	private Utilities utility;

	/**
	 * Test deploy verticle.
	 */
	@Test
	public void testDeployVerticle() {
		osiValidatorStage.deployVerticle();
	}

	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The audit log request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The o SI validator. */
	@Mock
	private OSIValidator oSIValidator;

	/** The umc validator. */
	@Mock
	UMCValidator umcValidator;

	/** The dto. */
	MessageDTO dto = new MessageDTO();

	/** The registration status dto. */
	InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

	@Mock
	private PacketManagerService packetManagerService;

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {

		Mockito.when(utility.getDefaultSource()).thenReturn("reg-client");
		Mockito.when(packetManagerService.getMetaInfo(anyString(),anyString(),anyString())).thenReturn(new HashMap<>());

		ReflectionTestUtils.setField(osiValidatorStage, "workerPoolSize", 10);
		ReflectionTestUtils.setField(osiValidatorStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(osiValidatorStage, "validateUMC", true);

		@SuppressWarnings("unchecked")
		RegistrationProcessorRestClientService<Object> mockObj = Mockito
				.mock(RegistrationProcessorRestClientService.class);

		Field auditLog = AuditLogRequestBuilder.class.getDeclaredField("registrationProcessorRestService");
		auditLog.setAccessible(true);
		auditLog.set(auditLogRequestBuilder, mockObj);
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_401.toString(), EventName.ADD.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);

		dto.setRid("reg1234");
		registrationStatusDto.setRegistrationId("reg1234");
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);

	}

	/**
	 * Testis valid OSI success.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testisValidOSISuccess() throws Exception {

		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		MessageDTO messageDto = osiValidatorStage.process(dto);

		assertTrue(messageDto.getIsValid());

	}

	/**
	 * Testis valid OSI failure.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testisValidOSIFailure() throws Exception {
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.FALSE);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);

		MessageDTO messageDto = osiValidatorStage.process(dto);

		assertFalse(messageDto.getIsValid());
	}

	/**
	 * IO exception test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void IOExceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new IOException());
		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());
	}

	@Test
	public void fSAdapterExceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new FSAdapterException("", ""));

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void apiResourceExceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new ApisResourceAccessException(""));

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}
	
	@Test
	public void ParentOnHoldExceptionTest() throws Exception {
		InternalRegistrationStatusDto regStatusDto = new InternalRegistrationStatusDto();
		regStatusDto.setStatusCode(StatusUtil.UIN_RID_NOT_FOUND.getCode());
		regStatusDto.setStatusComment("ParentOnHold");
		regStatusDto.setSubStatusCode(StatusUtil.UIN_RID_NOT_FOUND.getCode());
		
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(regStatusDto);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new ParentOnHoldException(StatusUtil.UIN_RID_NOT_FOUND.getCode(),StatusUtil.UIN_RID_NOT_FOUND.getMessage()));

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void exceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new NullPointerException(""));

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void aPIResourceServerExceptionTest() throws Exception {
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpServerErrorException httpServerErrorException = new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpServerErrorException);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(apisResourceAccessException);

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void aPIResourceClientExceptionTest() throws Exception {
		ApisResourceAccessException apisResourceAccessException = Mockito.mock(ApisResourceAccessException.class);
		HttpClientErrorException httpClientErrorException = new HttpClientErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "KER-FSE-004:encrypted data is corrupted or not base64 encoded");
		Mockito.when(apisResourceAccessException.getCause()).thenReturn(httpClientErrorException);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(apisResourceAccessException);

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	/**
	 * Data access exception test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void dataAccessExceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new DataAccessException("") {
		});
		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	/**
	 * Testis valid OSI failure.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testisValidOSIFailureWithRetryCount() throws Exception {

		registrationStatusDto.setRetryCount(1);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.FALSE);

		MessageDTO messageDto = osiValidatorStage.process(dto);

		assertFalse(messageDto.getIsValid());
	}
	/**
	 * Testis valid OSI failure.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testisValidUMCFailureWithRetryCount() throws Exception {

		registrationStatusDto.setRetryCount(1);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.FALSE);

		MessageDTO messageDto = osiValidatorStage.process(dto);

		assertFalse(messageDto.getIsValid());
	}
	@Test
	public void testAuthSystemException() throws Exception {
		InternalRegistrationStatusDto regStatusDto = new InternalRegistrationStatusDto();
		regStatusDto.setStatusCode(StatusUtil.AUTH_SYSTEM_EXCEPTION.getCode());
		regStatusDto.setStatusComment("Auth system exception");
		regStatusDto.setSubStatusCode(StatusUtil.AUTH_SYSTEM_EXCEPTION.getCode());
		
		
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(regStatusDto);
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(oSIValidator.isValidOSI(anyString(), anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenThrow(new AuthSystemException(StatusUtil.AUTH_SYSTEM_EXCEPTION.getMessage()));

		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());

	}

	@Test
	public void jsonProcessingExceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(packetManagerService.getMetaInfo(any(), any(), any())).thenThrow(new JsonProcessingException("message"));
		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());
	}

	@Test
	public void packetManagerExceptionTest() throws Exception {
		Mockito.when(umcValidator.isValidUMC(anyString(), any(InternalRegistrationStatusDto.class), anyMap())).thenReturn(Boolean.TRUE);
		Mockito.when(packetManagerService.getMetaInfo(any(),any(),any())).thenThrow(new PacketManagerException("id", "message"));
		MessageDTO messageDto = osiValidatorStage.process(dto);
		assertEquals(true, messageDto.getInternalError());
	}
}
