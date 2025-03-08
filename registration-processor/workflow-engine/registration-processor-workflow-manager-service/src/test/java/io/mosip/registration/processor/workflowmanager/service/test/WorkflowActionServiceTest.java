package io.mosip.registration.processor.workflowmanager.service.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.workflowmanager.verticle.WorkflowInstanceApi;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;


@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class WorkflowActionServiceTest {
	/** The registration status service. */
	@Mock
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The packet manager service. */
	@Mock
	private PacketManagerService packetManagerService;

	/** The core audit request builder. */
	@Mock
	AuditLogRequestBuilder auditLogRequestBuilder;

	/** The web sub util. */
	@Mock
	WebSubUtil webSubUtil;

	@InjectMocks
	WorkflowActionService workflowActionService;


	private InternalRegistrationStatusDto registrationStatusDto;
	

	@Before
	public void setUp()
			throws Exception {
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10003100030001520190422074511");
		registrationStatusDto.setRegistrationType("NEW");
		registrationStatusDto.setRegistrationStageName("SecurezoneNotificationStage");
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.name());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PAUSED.name());
		registrationStatusDto.setPauseRuleIds("testhotlisted,test1hotlisted");
		ReflectionTestUtils.setField(workflowActionService, "resumeFromBeginningStage", "SecurezoneNotificationStage");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatusForWorkflowEngine(any(), any(),
				Mockito.any());
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);
		Map<String, String> tags = new HashedMap();
		tags.put("PAUSE_IMMUNITY_RULE_IDS", "100,101");
		Mockito.when(packetManagerService.getTags(any(), any())).thenReturn(tags);
	}

	@Test
	public void testResumeProcessing() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");
	}
	
	@Test
	public void testResumeProcessingStatusDtoEmpty() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");
	}

	@Test(expected = WorkflowActionException.class)
	public void testResumeProcessingUnknownWorkflowAction() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING_AND_REMOVE_HOTLISTED_TAG");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testResumeProcessingTablenotAccessibleException() throws WorkflowActionException {
		TablenotAccessibleException tablenotAccessibleException = new TablenotAccessibleException(
				PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
		Mockito.doThrow(tablenotAccessibleException).when(registrationStatusService)
				.updateRegistrationStatusForWorkflowEngine(any(),
				anyString(), anyString());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testResumeProcessingPacketManagerException() throws WorkflowActionException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		PacketManagerException packetManagerException = new PacketManagerException("ERR-001", "exception occured");
		Mockito.doThrow(packetManagerException).when(packetManagerService).getTags(anyString(), any());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testResumeProcessingIOException() throws WorkflowActionException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		IOException exception = new IOException("exception occured");
		Mockito.doThrow(exception).when(packetManagerService).getTags(anyString(), any());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");
	}

	@Test
	public void testResumeProcessingWithReprocessFailed() throws WorkflowActionException {
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS_FAILED.name());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");
	}
	
	@Test
	public void testResumeFromBeginning() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testResumeFromBeginningPacketManagerException() throws WorkflowActionException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		PacketManagerException packetManagerException = new PacketManagerException("ERR-001", "exception occured");
		Mockito.doThrow(packetManagerException).when(packetManagerService).getTags(anyString(), any());
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testResumeFromBeginningIOException() throws WorkflowActionException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		IOException exception = new IOException("exception occured");
		Mockito.doThrow(exception).when(packetManagerService).getTags(anyString(), any());
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}
	
	@Test
	public void testResumeFromBeginningStatusReprocesFailed() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		registrationStatusDto.setLatestTransactionStatusCode("REPROCESS_FAILED");
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}
	
	@Test
	public void testResumeFromBeginningStatusDtoEmpty() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}
	
	@Test
	public void testStopProcessingPausedRuleIdsNull() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		registrationStatusDto.setPauseRuleIds(null);
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testResumeFromBeginningTablenotAccessibleException() throws WorkflowActionException {
		TablenotAccessibleException tablenotAccessibleException = new TablenotAccessibleException(
				PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
		Mockito.doThrow(tablenotAccessibleException).when(registrationStatusService)
				.updateRegistrationStatusForWorkflowEngine(any(),
				anyString(), anyString());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");
	}

	@Test
	public void testStopProcessingStatusDtoEmpty() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "STOP_PROCESSING");
	}

	@Test
	public void testStopProcessing() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "STOP_PROCESSING");
	}

	@Test(expected = WorkflowActionException.class)
	public void testStopProcessingTablenotAccessibleException() throws WorkflowActionException {
		TablenotAccessibleException tablenotAccessibleException = new TablenotAccessibleException(
				PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
		Mockito.doThrow(tablenotAccessibleException).when(registrationStatusService)
				.updateRegistrationStatusForWorkflowEngine(any(),
				anyString(), anyString());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "STOP_PROCESSING");
	}
	
	@Test(expected = WorkflowActionException.class)
	public void testStopProcessingWebsubException() throws WorkflowActionException {
		WebSubClientException webSubClientException = new WebSubClientException("ERR-001","exception occured");
		Mockito.doThrow(webSubClientException).when(webSubUtil).publishEvent(any(WorkflowCompletedEventDTO.class));
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "STOP_PROCESSING");
	}

	@Test(expected = WorkflowActionException.class)
	public void testUnknownWorkflow() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "test");
	}
	
}
