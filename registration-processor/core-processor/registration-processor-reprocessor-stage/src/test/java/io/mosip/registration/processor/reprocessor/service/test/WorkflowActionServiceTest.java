package io.mosip.registration.processor.reprocessor.service.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;

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

import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.reprocessor.service.WorkflowActionService;
import io.mosip.registration.processor.reprocessor.stage.ReprocessorStage;
import io.mosip.registration.processor.reprocessor.util.WebSubUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

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
	


	@Mock
	ReprocessorStage reprocessorStage;

	@Before
	public void setUp()
			throws Exception {
		registrationStatusDto = new InternalRegistrationStatusDto();
		registrationStatusDto.setRegistrationId("10003100030001520190422074511");
		registrationStatusDto.setRegistrationType("NEW");
		registrationStatusDto.setRegistrationStageName("SecurezoneNotificationStage");
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.name());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PAUSED.name());
		registrationStatusDto.setResumeRemoveTags("testhotlisted,test1hotlisted");
		ReflectionTestUtils.setField(workflowActionService, "resumeFromBeginningStage", "SecurezoneNotificationStage");
		Mockito.doNothing().when(registrationStatusService).updateRegistrationStatus(any(), any(),
				Mockito.any());
		Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(null);

	}

	@Test
	public void testResumeProcessing() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");

	}

	@Test(expected = WorkflowActionException.class)
	public void testResumeProcessingTablenotAccessibleException() throws WorkflowActionException {
		TablenotAccessibleException tablenotAccessibleException = new TablenotAccessibleException(
				PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
		Mockito.doThrow(tablenotAccessibleException).when(registrationStatusService)
				.updateRegistrationStatus(any(),
				anyString(), anyString());
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
	public void testResumeFromBeginningTablenotAccessibleException() throws WorkflowActionException {
		TablenotAccessibleException tablenotAccessibleException = new TablenotAccessibleException(
				PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
		Mockito.doThrow(tablenotAccessibleException).when(registrationStatusService)
				.updateRegistrationStatus(any(),
				anyString(), anyString());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_FROM_BEGINNING");

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
				.updateRegistrationStatus(any(),
				anyString(), anyString());
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



	@Test
	public void testResumeProcessingWithReprocessFailed() throws WorkflowActionException {
		registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS_FAILED.name());
		List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
		internalRegistrationStatusDtos.add(registrationStatusDto);
		workflowActionService.processWorkflowAction(internalRegistrationStatusDtos, "RESUME_PROCESSING");

	}
}
