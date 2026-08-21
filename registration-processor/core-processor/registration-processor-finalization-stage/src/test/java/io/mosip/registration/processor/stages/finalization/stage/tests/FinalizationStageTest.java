package io.mosip.registration.processor.stages.finalization.stage.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.utils.StaleCheckResult;
import io.mosip.registration.processor.packet.storage.utils.Utility;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.stages.finalization.stage.FinalizationStage;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class FinalizationStageTest {
	/** The registration status service. */
	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** Mosip router for APIs */
	@Mock
	private MosipRouter router;
	
	/** registration status mapper util */
	@Mock
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;
	
	@Mock
	private IdrepoDraftService idrepoDraftService;

	@Mock
	private Utility utility;

	/** The core audit request builder. */
	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;
	
	/** The dto. */
	InternalRegistrationStatusDto statusDto;
	
	/** The dto. */
	MessageDTO dto = new MessageDTO();
	
	/** The BiometricAuthenticationStage stage. */
	@InjectMocks
	private FinalizationStage finalizationStage = new FinalizationStage() {
		@Override
		public MosipEventBus getEventBus(Object verticleName, String url, int instanceNumber) {
			vertx = Vertx.vertx();

			return new MosipEventBus() {

				@Override
				public Vertx getEventbus() {
					return vertx;
				}

				@Override
				public void consume(MessageBusAddress fromAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress,
						EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {

				}

				@Override
				public void send(MessageBusAddress toAddress, MessageDTO message) {

				}

				@Override
				public void consumerHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}

				@Override
				public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
					// TODO Auto-generated method stub

				}


			};
		}

		@Override
		public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
				MessageBusAddress toAddress, long messageExpiryTimeLimit) {
		}
		
		@Override
		public Integer getPort() {
			return 8080;
		}
	};
	
	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(finalizationStage, "environment", new StandardEnvironment());
		ReflectionTestUtils.setField(finalizationStage, "defaultWorkerPoolSize", 10);
		ReflectionTestUtils.setField(finalizationStage, "messageExpiryTimeLimit", Long.valueOf(0));
		ReflectionTestUtils.setField(finalizationStage, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(finalizationStage, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

		dto.setRid("2018701130000410092018110735");
		dto.setReg_type("UPDATE");

		MockitoAnnotations.initMocks(this);


		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		Mockito.doReturn(responseWrapper).when(auditLogRequestBuilder).createAuditRequestBuilder(
				"test case description", EventId.RPR_405.toString(), EventName.UPDATE.toString(),
				EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);

		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setStatusCode("");
		registrationStatusDto.setRegistrationType("NEW");

		when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(registrationStatusDto);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("");

		when(idrepoDraftService.idrepoHasDraft(anyString())).thenReturn(true);

		// resolveUinForFinalization fetches draft demographics to get the UIN for stale check
		java.util.Map<String, Object> identityWithUin = new java.util.HashMap<>();
		identityWithUin.put("UIN", "9876543210");
		ResponseDTO draftWithUin = new ResponseDTO();
		draftWithUin.setIdentity(identityWithUin);
		when(idrepoDraftService.idrepoGetDraft(anyString(), eq("demographics"))).thenReturn(draftWithUin);

		when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.NOT_STALE);
		IdResponseDTO idResponseDTO = new IdResponseDTO();
		ResponseDTO responseDTO = new ResponseDTO();
		responseDTO.setAnonymousProfile("aa");
		responseDTO.setStatus("ACTIVATED");
		idResponseDTO.setErrors(null);
		idResponseDTO.setId("mosip.id.read");
		idResponseDTO.setResponse(responseDTO);
		idResponseDTO.setResponsetime("2019-01-17T06:29:01.940Z");
		idResponseDTO.setVersion("1.0");
		when(idrepoDraftService.idrepoPublishDraft(anyString())).thenReturn(idResponseDTO);
	}
	@Test
	public void testFinalizationSuccess() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		MessageDTO result = finalizationStage.process(messageDTO);
		Mockito.verify(idrepoDraftService).idrepoPublishDraft("27847657360002520181210094052");
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testFinalizationDraftUnavailable() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		when(idrepoDraftService.idrepoHasDraft(anyString())).thenReturn(false);
		MessageDTO result = finalizationStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testFinalizationDraftException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		when(idrepoDraftService.idrepoHasDraft(anyString())).thenThrow(IdrepoDraftException.class);
		MessageDTO result = finalizationStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testFinalizationAPIException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		when(idrepoDraftService.idrepoHasDraft(anyString())).thenThrow(ApisResourceAccessException.class);
		MessageDTO result = finalizationStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testFinalizationUnknownException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		when(idrepoDraftService.idrepoHasDraft(anyString())).thenThrow(NullPointerException.class);
		MessageDTO result = finalizationStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}
	
	@Test
	public void testStaleReprocess_UnavailableTriggersReprocess() throws Exception {
		when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.UNAVAILABLE);

		MessageDTO result = finalizationStage.process(dto);

		Mockito.verify(idrepoDraftService, Mockito.never()).idrepoDiscardDraft(anyString());
		Mockito.verify(idrepoDraftService, Mockito.never()).idrepoPublishDraft(anyString());
		assertTrue(result.getInternalError());
	}

	@Test
	public void testDeployVerticle() {
		finalizationStage.deployVerticle();
	}

	@Test
	public void testIdrepoDraftReprocessableException() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);
		when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION))
				.thenReturn("REPROCESS");
		when(idrepoDraftService.idrepoPublishDraft(anyString())).thenThrow(IdrepoDraftReprocessableException.class);
		MessageDTO result = finalizationStage.process(messageDTO);
		assertTrue(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testStaleReprocess_DiscardDraftAndSkipPublish() throws Exception {
		when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

		MessageDTO result = finalizationStage.process(dto);

		Mockito.verify(idrepoDraftService).idrepoDiscardDraft(dto.getRid());
		Mockito.verify(idrepoDraftService, Mockito.never()).idrepoPublishDraft(anyString());
		assertFalse(result.getIsValid());
		assertFalse(result.getInternalError());
	}

	@Test
	public void testProcess_ActivatedPacket_DraftPublishedSuccessfully() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.ACTIVATED.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		MessageDTO result = finalizationStage.process(messageDTO);

		Mockito.verify(idrepoDraftService).idrepoPublishDraft(anyString());
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testProcess_DeactivatedPacket_DraftPublishedSuccessfully() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.DEACTIVATED.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		MessageDTO result = finalizationStage.process(messageDTO);

		Mockito.verify(idrepoDraftService).idrepoPublishDraft(anyString());
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	@Test
	public void testProcess_DeactivatedUinStatus_RejectsWithoutInternalError() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		// Ensure the status DTO explicitly reports NEW type (immune to mock-ordering side-effects)
		InternalRegistrationStatusDto newStatusDto = new InternalRegistrationStatusDto();
		newStatusDto.setRegistrationId("27847657360002520181210094052");
		newStatusDto.setStatusCode("");
		newStatusDto.setRegistrationType(RegistrationType.NEW.name());
		when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(newStatusDto);

		// Draft has no identity field → resolveUinFromDraft returns null → triggers deactivated-identity path
		ResponseDTO draftNoUin = new ResponseDTO();
		when(idrepoDraftService.idrepoGetDraft(anyString(), eq("demographics"))).thenReturn(draftNoUin);

		MessageDTO result = finalizationStage.process(messageDTO);

		Mockito.verify(idrepoDraftService, Mockito.never()).idrepoPublishDraft(anyString());
		assertFalse(result.getInternalError());
		assertFalse(result.getIsValid());
	}

	@Test
	public void testProcess_UinNotInDraft_TriggersReprocess() throws Exception {
		// UPDATE packet where draft exists but identity has no UIN → must schedule reprocess, not publish
		InternalRegistrationStatusDto updateStatusDto = new InternalRegistrationStatusDto();
		updateStatusDto.setRegistrationId("2018701130000410092018110735");
		updateStatusDto.setStatusCode("");
		updateStatusDto.setRegistrationType("UPDATE");
		when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any())).thenReturn(updateStatusDto);

		ResponseDTO draftNoUin = new ResponseDTO();
		draftNoUin.setIdentity(new java.util.HashMap<>());
		when(idrepoDraftService.idrepoGetDraft(anyString(), eq("demographics"))).thenReturn(draftNoUin);

		MessageDTO result = finalizationStage.process(dto);

		Mockito.verify(idrepoDraftService, Mockito.never()).idrepoPublishDraft(anyString());
		assertTrue(result.getInternalError());
	}

	// TC_44473_15: UPDATE packet success path — draft must be published
	@Test
	public void testProcess_UpdatePacket_DraftPublishedSuccessfully() throws Exception {
		// setUp stubs: draft has UIN="9876543210", isLatestPacket→NOT_STALE, publishDraft returns ACTIVATED
		MessageDTO result = finalizationStage.process(dto); // dto is UPDATE type from setUp

		Mockito.verify(idrepoDraftService).idrepoPublishDraft(dto.getRid());
		assertFalse(result.getInternalError());
		assertTrue(result.getIsValid());
	}

	// TC_44473_14 error path: non-reprocessable publishDraft failure → FAILED, no requeue
	@Test
	public void testProcess_PublishDraftIdrepoException_MarksAsFailed() throws Exception {
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setRid("27847657360002520181210094052");
		messageDTO.setReg_type(RegistrationType.NEW.name());
		messageDTO.setWorkflowInstanceId("123er");
		messageDTO.setIteration(1);

		when(idrepoDraftService.idrepoPublishDraft(anyString()))
				.thenThrow(new IdrepoDraftException("IDR-IDC-001", "publish failed"));

		MessageDTO result = finalizationStage.process(messageDTO);

		assertTrue(result.getInternalError());
		assertFalse(result.getIsValid());
	}
}
