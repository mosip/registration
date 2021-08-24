package io.mosip.registration.processor.workflowmanager.verticle;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.rest.client.audit.dto.AuditResponseDto;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowActionJobTest {
	MessageDTO dto = new MessageDTO();
	@InjectMocks
	private WorkflowActionJob workflowActionJob = new WorkflowActionJob() {
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
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		}
	};

	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Mock
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Mock
	private LogDescription description;

	@Mock
	WorkflowActionService workflowActionService;

	@Before
	public void setup() throws Exception {

		ReflectionTestUtils.setField(workflowActionJob, "fetchSize", 2);
		Field auditLog = AuditLogRequestBuilder.class.getDeclaredField("registrationProcessorRestService");
		auditLog.setAccessible(true);
		@SuppressWarnings("unchecked")
		RegistrationProcessorRestClientService<Object> mockObj = Mockito
				.mock(RegistrationProcessorRestClientService.class);
		auditLog.set(auditLogRequestBuilder, mockObj);
		AuditResponseDto auditResponseDto = new AuditResponseDto();
		ResponseWrapper<AuditResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setResponse(auditResponseDto);
		auditLogRequestBuilder.createAuditRequestBuilder("test case description", EventId.RPR_401.toString(),
				EventName.ADD.toString(), EventType.BUSINESS.toString(), "1234testcase", ApiName.AUDIT);
	}

	@Test
	public void testProcessSuccess() throws TablenotAccessibleException, WorkflowActionException {

		List<InternalRegistrationStatusDto> actionablePausedPacketList = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto.setDefaultResumeAction("RESUME_PROCESSING");
		registrationStatusDto.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto.setReProcessRetryCount(0);
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		registrationStatusDto.setStatusCode("PAUSED");

		actionablePausedPacketList.add(registrationStatusDto);
		InternalRegistrationStatusDto registrationStatusDto2 = new InternalRegistrationStatusDto();

		registrationStatusDto2.setRegistrationId("2018701130000410092018110734");
		registrationStatusDto2.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto2.setDefaultResumeAction("RESUME_FROM_BEGINNING");
		registrationStatusDto2.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto2.setReProcessRetryCount(0);
		registrationStatusDto2.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		registrationStatusDto2.setStatusCode("PAUSED");

		actionablePausedPacketList.add(registrationStatusDto2);

		InternalRegistrationStatusDto registrationStatusDto3 = new InternalRegistrationStatusDto();

		registrationStatusDto3.setRegistrationId("2018701130000410092018110736");
		registrationStatusDto3.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto3.setDefaultResumeAction("STOP_PROCESSING");
		registrationStatusDto3.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto3.setReProcessRetryCount(0);
		registrationStatusDto3.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
		registrationStatusDto3.setStatusCode("PAUSED");

		actionablePausedPacketList.add(registrationStatusDto3);
		Mockito.when(registrationStatusService.getActionablePausedPackets(anyInt()))
				.thenReturn(actionablePausedPacketList);
		dto = workflowActionJob.process(dto);
		verify(workflowActionService, times(3)).processWorkflowAction(Mockito.any(), Mockito.anyString());
	}

	@Test
	public void TablenotAccessibleExceptionTest() throws Exception {
		Mockito.when(registrationStatusService.getActionablePausedPackets(anyInt()))
				.thenThrow(new TablenotAccessibleException("") {
				});

		dto = workflowActionJob.process(dto);
		assertEquals(true, dto.getInternalError());

	}

	@Test
	public void testWorkflowActionException() throws TablenotAccessibleException, WorkflowActionException {

		List<InternalRegistrationStatusDto> actionablePausedPacketList = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto.setDefaultResumeAction("RESUME_PROCESSING");
		registrationStatusDto.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto.setReProcessRetryCount(0);
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());

		actionablePausedPacketList.add(registrationStatusDto);
		Mockito.when(registrationStatusService.getActionablePausedPackets(anyInt()))
				.thenReturn(actionablePausedPacketList);
		Mockito.doThrow(new WorkflowActionException("", "")).when(workflowActionService)
				.processWorkflowAction(anyList(),
				Mockito.anyString());
		dto = workflowActionJob.process(dto);
		assertEquals(true, dto.getInternalError());
	}

	@Test
	public void testException() throws TablenotAccessibleException, WorkflowActionException {

		List<InternalRegistrationStatusDto> actionablePausedPacketList = new ArrayList<>();
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();

		registrationStatusDto.setRegistrationId("2018701130000410092018110735");
		registrationStatusDto.setRegistrationStageName("PacketValidatorStage");
		registrationStatusDto.setDefaultResumeAction("RESUME_PROCESSING");
		registrationStatusDto.setResumeTimeStamp(LocalDateTime.now());
		registrationStatusDto.setReProcessRetryCount(0);
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());

		actionablePausedPacketList.add(registrationStatusDto);
		Mockito.when(registrationStatusService.getActionablePausedPackets(anyInt()))
				.thenReturn(null);
		dto = workflowActionJob.process(dto);
		assertEquals(true, dto.getInternalError());
	}
}
