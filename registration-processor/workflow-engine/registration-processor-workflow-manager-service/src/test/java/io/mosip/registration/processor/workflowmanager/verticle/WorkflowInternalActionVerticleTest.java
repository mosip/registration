package io.mosip.registration.processor.workflowmanager.verticle;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@RunWith(SpringRunner.class)
public class WorkflowInternalActionVerticleTest {
	@Mock
	private MosipRouter router;
	@Mock
	MosipEventBus mosipEventBus;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	WebSubUtil webSubUtil;


	@InjectMocks
	private WorkflowInternalActionVerticle workflowInternalActionVerticle = new WorkflowInternalActionVerticle() {
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
			};
		}

		@Override
		public void consumeAndSend(MosipEventBus eventbus, MessageBusAddress addressbus1,
				MessageBusAddress addressbus2, long messageExpiryTimeLimit) {
		}

		@Override
		public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
			return null;

		}

		@Override
		public void createServer(Router router, int port) {

		}
	};

	@Test
	public void testDeployVerticle() {

		ReflectionTestUtils.setField(workflowInternalActionVerticle, "workerPoolSize", 10);
		ReflectionTestUtils.setField(workflowInternalActionVerticle, "clusterManagerUrl", "/dummyPath");
		workflowInternalActionVerticle.deployVerticle();
	}

	@Test
	public void testStart() {
		ReflectionTestUtils.setField(workflowInternalActionVerticle, "port", "2333");
		Mockito.doNothing().when(router).setRoute(any());
		workflowInternalActionVerticle.start();
	}

	@Test
	public void testProcessSuccessForMarkAsPaused() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_PAUSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.PAUSED.toString(), argument.getAllValues().get(0).getStatusCode());

	}

	@Test
	public void testTablenotAccessibleException() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_PAUSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow(TablenotAccessibleException.class);

		workflowInternalActionVerticle.process(workflowInternalActionDTO);
	}

	@Test
	public void testDateTimeParseException() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_PAUSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.5Z");
		workflowInternalActionDTO.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
	}

	@Test
	public void testException() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_PAUSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.5Z");
		workflowInternalActionDTO.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(null);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
	}

	@Test
	public void testProcessSuccessForCompleteAsProcessed() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as processed");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.PROCESSED.toString(), argument.getAllValues().get(0).getStatusCode());
		ArgumentCaptor<WorkflowCompletedEventDTO> argument1 = ArgumentCaptor.forClass(WorkflowCompletedEventDTO.class);

		verify(webSubUtil, atLeastOnce()).publishEvent(argument1.capture());
		assertEquals(RegistrationStatusCode.PROCESSED.toString(), argument1.getAllValues().get(0).getResultCode());

	}

	@Test
	public void testProcessSuccessForCompleteAsRejected() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as rejected");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.REJECTED.toString(), argument.getAllValues().get(0).getStatusCode());
		ArgumentCaptor<WorkflowCompletedEventDTO> argument1 = ArgumentCaptor.forClass(WorkflowCompletedEventDTO.class);
		verify(webSubUtil, atLeastOnce()).publishEvent(argument1.capture());
		assertEquals(RegistrationStatusCode.REJECTED.toString(), argument1.getAllValues().get(0).getResultCode());

	}

	@Test
	public void testProcessSuccessForCompleteAsFailed() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_FAILED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as failed");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.FAILED.toString(), argument.getAllValues().get(0).getStatusCode());
		ArgumentCaptor<WorkflowCompletedEventDTO> argument1 = ArgumentCaptor.forClass(WorkflowCompletedEventDTO.class);
		verify(webSubUtil, atLeastOnce()).publishEvent(argument1.capture());
		assertEquals(RegistrationStatusCode.FAILED.toString(), argument1.getAllValues().get(0).getResultCode());
	}

	@Test
	public void testProcessSuccessForMarkAsReprocess()
	{
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_REPROCESS.toString());
		workflowInternalActionDTO.setActionMessage("packet is marked as reprocess");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatus(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.REPROCESS.toString(), argument.getAllValues().get(0).getStatusCode());
	}
}
