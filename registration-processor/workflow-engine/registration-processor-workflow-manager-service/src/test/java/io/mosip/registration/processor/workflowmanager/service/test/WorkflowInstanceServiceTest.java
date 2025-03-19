package io.mosip.registration.processor.workflowmanager.service.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Arrays;

import io.mosip.registration.processor.status.service.SyncRegistrationService;
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

import io.mosip.registration.processor.core.exception.WorkflowInstanceException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.workflow.dto.NotificationInfoDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowInstanceRequestDTO;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dao.SyncRegistrationDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.encryptor.Encryptor;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowInstanceService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;


@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class WorkflowInstanceServiceTest {
    /** The registration status service. */
    @Mock
    RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    /** The core audit request builder. */
    @Mock
    AuditLogRequestBuilder auditLogRequestBuilder;

    @Mock
    private SyncRegistrationDao syncRegistrationDao;

    @InjectMocks
    WorkflowInstanceService workflowInstanceService;

    @Mock
    private Encryptor encryptor;

    @Mock
    private Utilities utility;

    @Mock
    private SyncRegistrationService syncRegistrationService;


    private WorkflowInstanceRequestDTO workflowInstanceRequestDto;


    @Before
    public void setUp()
            throws Exception {
        workflowInstanceRequestDto = new WorkflowInstanceRequestDTO();
        workflowInstanceRequestDto.setRegistrationId("10003100030001520190422074511");
        workflowInstanceRequestDto.setProcess("NEW");
        workflowInstanceRequestDto.setSource("REGISTRATION_CLIENT");
        workflowInstanceRequestDto.setAdditionalInfoReqId("");
        NotificationInfoDTO notificationInfoDto = new NotificationInfoDTO();
        notificationInfoDto.setName("testName");
        notificationInfoDto.setEmail("Email");
        notificationInfoDto.setPhone("123456789");
        workflowInstanceRequestDto.setNotificationInfo(notificationInfoDto);
        Mockito.when(syncRegistrationDao.save(any())).thenReturn(null);
        Mockito.when(syncRegistrationDao.findByRegistrationIdIdAndRegType(anyString(),any())).thenReturn(null);
        Mockito.when(encryptor.encrypt(anyString(),anyString(),anyString())).thenReturn(null);
        Mockito.when(utility.getRefId(anyString(),anyString())).thenReturn("");
        ReflectionTestUtils.setField(workflowInstanceService, "beginningStage", "PacketValidatorStage");
        Mockito.when(utility.getInternalProcess(any())).thenReturn("NEW");
        Mockito.doNothing().when(registrationStatusService).addRegistrationStatus(any(), anyString(),
                anyString());
        Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
    }

    @Test
    public void testAddRegistrationProcess() throws Exception {
        workflowInstanceService.createWorkflowInstance(workflowInstanceRequestDto, "USER");
    }

    @Test(expected = WorkflowInstanceException.class)
    public void testAddRegistrationProcessTablenotAccessibleException() throws Exception {
        TablenotAccessibleException tablenotAccessibleException = new TablenotAccessibleException(
                PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
        Mockito.doThrow(tablenotAccessibleException).when(registrationStatusService)
                .addRegistrationStatus(any(), anyString(),
                        anyString());
        workflowInstanceService.createWorkflowInstance(workflowInstanceRequestDto, "USER");
    }

    @Test(expected = Exception.class)
    public void testAddRegistrationProcessException() throws Exception {
        Exception exp=new Exception(PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage());
        Mockito.doThrow(exp).when(registrationStatusService)
                .addRegistrationStatus(any(), anyString(),
                        anyString());
        workflowInstanceService.createWorkflowInstance(workflowInstanceRequestDto, "USER");
    }

}