package io.mosip.registration.processor.reprocessor.stage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.Test;
import org.junit.runner.RunWith;
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
import io.mosip.registration.processor.core.abstractverticle.WorkflowEventDTO;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@RunWith(SpringRunner.class)
public class WorkflowEventUpdateVerticleTest {
	@Mock
	private MosipRouter router;
	@Mock
	MosipEventBus mosipEventBus;

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	private InternalRegistrationStatusDto registrationStatusDto;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;


	@InjectMocks
	private WorkflowEventUpdateVerticle workFlowEventUpdateVerticle = new WorkflowEventUpdateVerticle() {
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
				MessageBusAddress addressbus2) {
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

		ReflectionTestUtils.setField(workFlowEventUpdateVerticle, "workerPoolSize", 10);
		ReflectionTestUtils.setField(workFlowEventUpdateVerticle, "clusterManagerUrl", "/dummyPath");
		workFlowEventUpdateVerticle.deployVerticle();
	}

	@Test
	public void testStart() {
		ReflectionTestUtils.setField(workFlowEventUpdateVerticle, "port", "2333");
		Mockito.doNothing().when(router).setRoute(any());
		workFlowEventUpdateVerticle.start();
	}

	@Test
	public void testProcessSuccess() {
		WorkflowEventDTO workFlowEventDto = new WorkflowEventDTO();
		workFlowEventDto.setRid("10006100390000920200603070407");
		workFlowEventDto.setStatusCode("PAUSED");
		workFlowEventDto.setStatusComment("packet is paused");
		workFlowEventDto.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workFlowEventDto.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workFlowEventDto.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workFlowEventUpdateVerticle.process(workFlowEventDto);
	}

	@Test
	public void testTablenotAccessibleException() {
		WorkflowEventDTO workFlowEventDto = new WorkflowEventDTO();
		workFlowEventDto.setRid("10006100390000920200603070407");
		workFlowEventDto.setStatusCode("PAUSED");
		workFlowEventDto.setStatusComment("packet is paused");
		workFlowEventDto.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workFlowEventDto.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workFlowEventDto.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString()))
				.thenThrow(TablenotAccessibleException.class);

		workFlowEventUpdateVerticle.process(workFlowEventDto);
	}

	@Test
	public void testDateTimeParseException() {
		WorkflowEventDTO workFlowEventDto = new WorkflowEventDTO();
		workFlowEventDto.setRid("10006100390000920200603070407");
		workFlowEventDto.setStatusCode("PAUSED");
		workFlowEventDto.setStatusComment("packet is paused");
		workFlowEventDto.setResumeTimestamp("2021-03-02T08:24:29.5Z");
		workFlowEventDto.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workFlowEventDto.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(registrationStatusDto);
		workFlowEventUpdateVerticle.process(workFlowEventDto);
	}

	@Test
	public void testException() {
		WorkflowEventDTO workFlowEventDto = new WorkflowEventDTO();
		workFlowEventDto.setRid("10006100390000920200603070407");
		workFlowEventDto.setStatusCode("PAUSED");
		workFlowEventDto.setStatusComment("packet is paused");
		workFlowEventDto.setResumeTimestamp("2021-03-02T08:24:29.5Z");
		workFlowEventDto.setEventTimestamp("2021-03-02T08:24:29.526Z");
		workFlowEventDto.setDefaultResumeAction("ResumeProcessing");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(anyString())).thenReturn(null);
		workFlowEventUpdateVerticle.process(workFlowEventDto);
	}
}
