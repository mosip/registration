package io.mosip.registration.processor.workflowmanager.service.test;

import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
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
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dao.SyncRegistrationDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.encryptor.Encryptor;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowInstanceService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;


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
    private RegistrationStatusDao registrationStatusDao;


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
        ReflectionTestUtils.setField(workflowInstanceService, "beginningStage", "PacketValidatorStage");
        Mockito.when(utility.getIterationForSyncRecord(anyMap(),any(),any())).thenReturn(1);
        Mockito.doNothing().when(registrationStatusService).addRegistrationStatus(any(), anyString(),
                anyString());
        Mockito.when(auditLogRequestBuilder.createAuditRequestBuilder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        Mockito.when(registrationStatusDao.findByIdAndProcessAndIteration(any(),any(),anyInt())).thenReturn(new ArrayList<RegistrationStatusEntity>());
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

    @Test(expected = WorkflowInstanceException.class)
    public void testValidateWorkflowInstanceAlreadyAvailableForRegistrationStatusEntity() throws Exception {
        RegistrationStatusEntity  registrationStatusEntity=new RegistrationStatusEntity();
        registrationStatusEntity.setRegId("10007100070014420250319152546");
        List<RegistrationStatusEntity> registrationStatusEntities=new ArrayList<RegistrationStatusEntity>();
        registrationStatusEntities.add(registrationStatusEntity);
        SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
        Mockito.when(registrationStatusDao.findByIdAndProcessAndIteration(any(),any(),anyInt())).thenReturn(registrationStatusEntities);
        Mockito.when(syncRegistrationDao.findByRegistrationIdAndRegTypeAndAdditionalInfoReqId(anyString(),any(),any())).thenReturn(syncRegistrationEntity);
        workflowInstanceService.createWorkflowInstance(workflowInstanceRequestDto, "USER");
    }

    @Test(expected = WorkflowInstanceException.class)
    public void testValidateWorkflowInstanceAlreadyAvailableForSyncRegistrationEntity() throws Exception {
        List<RegistrationStatusEntity> registrationStatusEntities=new ArrayList<RegistrationStatusEntity>();
        SyncRegistrationEntity syncRegistrationEntity = new SyncRegistrationEntity();
        syncRegistrationEntity.setRegistrationId("10007100070014420250319152546");
        Mockito.when(registrationStatusDao.findByIdAndProcessAndIteration(any(),any(),anyInt())).thenReturn(registrationStatusEntities);
        Mockito.when(syncRegistrationDao.findByRegistrationIdAndRegTypeAndAdditionalInfoReqId(anyString(),any(), anyString())).thenReturn(syncRegistrationEntity);
        workflowInstanceService.createWorkflowInstance(workflowInstanceRequestDto, "USER");
    }

}