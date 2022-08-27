package io.mosip.registration.processor.workflowmanager.verticle;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowActionCode;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
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

	@Mock
	private AdditionalInfoRequestService additionalInfoRequestService;

	@Mock
	WorkflowActionService workflowActionService;

	@Mock
	private PacketManagerService packetManagerService;
	
	@Mock
	private AnonymousProfileService anonymousProfileService;
	
	@Mock
	private IdSchemaUtil idSchemaUtil;
	
	@Mock
	private Utilities utility;

	@Mock
	private Environment env;


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
		
		public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
			
		};
	};

	@Before
	public void setUp() throws Exception {
		ReflectionTestUtils.setField(workflowInternalActionVerticle, "anonymousProfileBusAddress", "anonymous-profile-bus-in");
	}
	
	@Test
	public void testDeployVerticle() {

		ReflectionTestUtils.setField(workflowInternalActionVerticle, "workerPoolSize", 10);
		ReflectionTestUtils.setField(workflowInternalActionVerticle, "clusterManagerUrl", "/dummyPath");
		ReflectionTestUtils.setField(workflowInternalActionVerticle, "defaultMaxAllowedIteration", 5);
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
		List<String> matchedRuleIds=new ArrayList<String>();
		matchedRuleIds.add("NON_RESIDENT_CHILD_APPLICANT");
		workflowInternalActionDTO.setMatchedRuleIds(matchedRuleIds);
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
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
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
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
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
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
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(null);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
	}

	@Test
	public void testProcessSuccessForCompleteAsProcessed() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as processed");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
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
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
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
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
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
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any())).thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.REPROCESS.toString(), argument.getAllValues().get(0).getStatusCode());
	}

	@Test
	public void testProcessSuccessForPauseAndRequestAdditionalInfo() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.PAUSE_AND_REQUEST_ADDITIONAL_INFO.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused for Additional Info");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction(WorkflowActionCode.STOP_PROCESSING.toString());
		workflowInternalActionDTO.setIteration(1);
		workflowInternalActionDTO.setReg_type("NEW");
		workflowInternalActionDTO.setAdditionalInfoProcess("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(
				additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcess(anyString(), anyString()))
				.thenReturn(null);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString(),
				argument.getAllValues().get(0).getStatusCode());
		ArgumentCaptor<AdditionalInfoRequestDto> argument1 = ArgumentCaptor.forClass(AdditionalInfoRequestDto.class);

		verify(additionalInfoRequestService, atLeastOnce()).addAdditionalInfoRequest(argument1.capture());
		assertEquals(workflowInternalActionDTO.getAdditionalInfoProcess(),
				argument1.getAllValues().get(0).getAdditionalInfoProcess());
		ArgumentCaptor<WorkflowPausedForAdditionalInfoEventDTO> argument2 = ArgumentCaptor
				.forClass(WorkflowPausedForAdditionalInfoEventDTO.class);

		verify(webSubUtil, atLeastOnce()).publishEvent(argument2.capture());
		assertEquals(workflowInternalActionDTO.getAdditionalInfoProcess(),
				argument2.getAllValues().get(0).getAdditionalInfoProcess());

	}
	@Test
	public void testProcessSuccessForRestartParentFlow() throws WorkflowActionException, ApisResourceAccessException,
			PacketManagerException, JsonProcessingException, IOException {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.RESTART_PARENT_FLOW.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_RESTART_PARENT_FLOW.getMessage());
		workflowInternalActionDTO.setReg_type("CORRECTION");
		workflowInternalActionDTO.setIteration(1);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setRegId("10006100390000920200603070407");
		additionalInfoRequestDto.setAdditionalInfoIteration(1);
		additionalInfoRequestDto.setWorkflowInstanceId("Workflow123");
		additionalInfoRequestDto.setAdditionalInfoReqId("additionalRequestId");
		Mockito.when(additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyInt())).thenReturn(additionalInfoRequestDto);
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		verify(workflowActionService, times(1)).processWorkflowAction(Mockito.any(), Mockito.anyString());
		verify(packetManagerService, times(1)).addOrUpdateTags(Mockito.anyString(), Mockito.any());
	}

	@Test
	public void testProcessSuccessForCompleteAsProcessedForAdditionalInfoWorkflow()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
			WorkflowActionException {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as processed");
		workflowInternalActionDTO.setReg_type("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setRegId("10006100390000920200603070407");
		additionalInfoRequestDto.setAdditionalInfoIteration(1);
		additionalInfoRequestDto.setWorkflowInstanceId("Workflow123");
		additionalInfoRequestDto.setAdditionalInfoReqId("additionalRequestId");
		Mockito.when(additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyInt())).thenReturn(additionalInfoRequestDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.PROCESSED.toString(), argument.getAllValues().get(0).getStatusCode());
		verify(workflowActionService, times(1)).processWorkflowAction(Mockito.any(), Mockito.anyString());
		verify(packetManagerService, times(1)).addOrUpdateTags(Mockito.anyString(), Mockito.any());
	}

	@Test
	public void testProcessSuccessForCompleteAsRejectedForAdditionalInfoWorkflow()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
			WorkflowActionException {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as rejected");
		workflowInternalActionDTO.setReg_type("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setRegId("10006100390000920200603070407");
		additionalInfoRequestDto.setAdditionalInfoIteration(1);
		additionalInfoRequestDto.setWorkflowInstanceId("Workflow123");
		additionalInfoRequestDto.setAdditionalInfoReqId("additionalRequestId");
		Mockito.when(additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyInt())).thenReturn(additionalInfoRequestDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.REJECTED.toString(), argument.getAllValues().get(0).getStatusCode());
		verify(workflowActionService, times(1)).processWorkflowAction(Mockito.any(), Mockito.anyString());
		verify(packetManagerService, times(1)).addOrUpdateTags(Mockito.anyString(), Mockito.any());
	}

	@Test
	public void testProcessSuccessForCompleteAsFailedForAdditionalInfoWorkflow()
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
			WorkflowActionException {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_FAILED.toString());
		workflowInternalActionDTO.setActionMessage("packet is complete as failed");
		workflowInternalActionDTO.setReg_type("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setRegId("10006100390000920200603070407");
		additionalInfoRequestDto.setAdditionalInfoIteration(1);
		additionalInfoRequestDto.setAdditionalInfoProcess("CORRECTION");
		additionalInfoRequestDto.setWorkflowInstanceId("Workflow123");
		additionalInfoRequestDto.setAdditionalInfoReqId("additionalRequestId");
		Mockito.when(additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyInt())).thenReturn(additionalInfoRequestDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.FAILED.toString(), argument.getAllValues().get(0).getStatusCode());
		verify(workflowActionService, times(1)).processWorkflowAction(Mockito.any(), Mockito.anyString());
		verify(packetManagerService, times(1)).addOrUpdateTags(Mockito.anyString(), Mockito.any());
	}

	@Test
	public void testProcessSuccessForPauseAndRequestAdditionalInfoForAdditionalInfoWorkflow() throws WorkflowActionException,
			ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO
				.setActionCode(WorkflowInternalActionCode.PAUSE_AND_REQUEST_ADDITIONAL_INFO.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused for Additional Info");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction(WorkflowActionCode.STOP_PROCESSING.toString());
		workflowInternalActionDTO.setIteration(1);
		workflowInternalActionDTO.setReg_type("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setRegId("10006100390000920200603070407");
		additionalInfoRequestDto.setAdditionalInfoIteration(1);
		additionalInfoRequestDto.setWorkflowInstanceId("Workflow123");
		additionalInfoRequestDto.setAdditionalInfoReqId("additionalRequestId");
		Mockito.when(additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcessAndIteration(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyInt())).thenReturn(additionalInfoRequestDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.FAILED.toString(),
				argument.getAllValues().get(0).getStatusCode());
		verify(workflowActionService, times(1)).processWorkflowAction(Mockito.any(), Mockito.anyString());
		verify(packetManagerService, times(1)).addOrUpdateTags(Mockito.anyString(), Mockito.any());
	}

	@Test
	public void testProcessSuccessForCompleteAsRejectedWithoutParentFlow() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO
				.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW.toString());
		workflowInternalActionDTO
				.setActionMessage("Packet processing completed with reject status without Parent flow");
		workflowInternalActionDTO.setReg_type("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.REJECTED.toString(), argument.getAllValues().get(0).getStatusCode());
	}
	@Test
	public void testProcessSuccessForPauseAndRequestAdditionalInfoWithMaxAllowedIterationExceed() {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.PAUSE_AND_REQUEST_ADDITIONAL_INFO.toString());
		workflowInternalActionDTO.setActionMessage("packet is paused for Additional Info");
		workflowInternalActionDTO.setResumeTimestamp("2021-03-02T08:24:29.526Z");
		workflowInternalActionDTO.setDefaultResumeAction(WorkflowActionCode.STOP_PROCESSING.toString());
		workflowInternalActionDTO.setIteration(6);
		workflowInternalActionDTO.setReg_type("NEW");
		workflowInternalActionDTO.setAdditionalInfoProcess("CORRECTION");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(), any());
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setRegId("10006100390000920200603070407");
		additionalInfoRequestDto.setAdditionalInfoIteration(5);
		additionalInfoRequestDto.setWorkflowInstanceId("Workflow123");
		additionalInfoRequestDto.setAdditionalInfoReqId("additionalRequestId");
		List<AdditionalInfoRequestDto> additionalInfoRequestDtoList=new ArrayList<AdditionalInfoRequestDto>();
		additionalInfoRequestDtoList.add(additionalInfoRequestDto);
		Mockito.when(
				additionalInfoRequestService.getAdditionalInfoRequestByRegIdAndProcess(anyString(), anyString()))
				.thenReturn(additionalInfoRequestDtoList);
		workflowInternalActionVerticle.process(workflowInternalActionDTO);
		ArgumentCaptor<InternalRegistrationStatusDto> argument = ArgumentCaptor
				.forClass(InternalRegistrationStatusDto.class);

		verify(registrationStatusService, atLeastOnce()).updateRegistrationStatusForWorkflowEngine(argument.capture(), Mockito.any(),
				Mockito.any());
		assertEquals(RegistrationStatusCode.REJECTED.toString(), argument.getAllValues().get(0).getStatusCode());
		ArgumentCaptor<WorkflowCompletedEventDTO> argument1 = ArgumentCaptor.forClass(WorkflowCompletedEventDTO.class);
		verify(webSubUtil, atLeastOnce()).publishEvent(argument1.capture());
		assertEquals(RegistrationStatusCode.REJECTED.toString(), argument1.getAllValues().get(0).getResultCode());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessSuccessForAnonymousProfile() throws IOException, JSONException, BaseCheckedException {
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid("10006100390000920200603070407");
		workflowInternalActionDTO.setReg_type("NEW");
		workflowInternalActionDTO.setIsValid(true);
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.ANONYMOUS_PROFILE.toString());
		workflowInternalActionDTO.setActionMessage("anonymous profile event");

		Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
				.thenReturn("1.0");
		Mockito.when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(Arrays.asList(""));

		Map<String, String> fieldTypeMap = new HashedMap();
		fieldTypeMap.put("postalCode", "string");
		fieldTypeMap.put("zone", "simpleType");
		Mockito.when(idSchemaUtil.getIdSchemaFieldTypes(anyDouble())).thenReturn(fieldTypeMap);

		Map<String, String> fieldMap = new HashedMap();
		fieldMap.put("postalCode", "14022");
		fieldMap.put("dateOfBirth", "1998/01/01");
		fieldMap.put("phone", "6666666666");
		Mockito.when(packetManagerService.getFields(anyString(), any(), anyString(), any())).thenReturn(fieldMap);

		Map<String, String> metaInfoMap = new HashedMap();
		metaInfoMap.put("documents", "[{\"documentType\" : \"CIN\"},{\"documentType\" : \"RNC\"})]");
		metaInfoMap.put("operationsData",
				"[{\"label\" : \"officerId\",\"value\" : \"110024\"},{\"label\" : \"officerBiometricFileName\",\"value\" : \"null\"})]");
		metaInfoMap.put("creationDate", "2021-09-01T03:48:49.193Z");
		Mockito.when(packetManagerService.getMetaInfo(anyString(), anyString(), any())).thenReturn(metaInfoMap);

		BiometricRecord biometricRecord = new BiometricRecord();
		BIR bir = new BIR();
		HashMap<String, String> entry = new HashMap<>();
		bir.setSb("eyJ4NWMiOlsiTUlJRGtEQ0NBbmlnQXdJQkFnSUVwNzo".getBytes());
		bir.setBdb("SUlSADAyMAAAACc6AAEAAQAAJyoH5AoJECYh//8Bc18wBgAAAQIDCgABlwExCA".getBytes());
		entry.put("PAYLOAD", "{\"deviceServiceVersion\":\"0.9.5\",\"bioValue\":\"<bioValue>\",\"qualityScore\":\"80\",\"bioType\":\"Iris\"}");
		bir.setOthers(entry);
		biometricRecord.setSegments(Arrays.asList(bir));
		Mockito.when(packetManagerService.getBiometrics(anyString(), anyString(), anyString(), any()))
				.thenReturn(biometricRecord);
		
		org.json.simple.JSONObject identity = new org.json.simple.JSONObject();
		LinkedHashMap IDSchemaVersion = new LinkedHashMap();
		IDSchemaVersion.put("value", "1.0");
		identity.put("IDSchemaVersion", IDSchemaVersion);
		
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10006100390000920200603070407");
		Mockito.when(registrationStatusService.getRegistrationStatus(any(), any(), any(), any()))
				.thenReturn(registrationStatusDto);
		Mockito.when(utility.getRegistrationProcessorMappingJson(any()))
		.thenReturn(identity);
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Mockito.when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString())).thenReturn("jsonProfile");
		Mockito.doNothing().when(anonymousProfileService).saveAnonymousProfile(anyString(), anyString(), anyString());
		MessageDTO object = workflowInternalActionVerticle.process(workflowInternalActionDTO);
		assertEquals(true, object.getIsValid());
	}
}
