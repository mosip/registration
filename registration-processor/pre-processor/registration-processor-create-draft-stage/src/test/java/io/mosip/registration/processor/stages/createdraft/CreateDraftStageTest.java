package io.mosip.registration.processor.stages.createdraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.StaleCheckResult;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.storage.utils.Utility;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.createdraft.stage.CreateDraftStage;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * Unit tests for {@link CreateDraftStage}, aligned with {@code UinGeneratorStageTest}
 * scenarios now that draft create/update is handled via {@code idrepoUpdateDraftV2}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateDraftStageTest {

    private static final String REG_ID = "10001100770000320200720095022";
    private static final String EXISTING_UIN = "9876543210";
    private static final String DRAFTED_STATUS = "DRAFTED";

    @InjectMocks
    private CreateDraftStage createDraftStage;

    @Mock
    private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

    @Mock
    private IdrepoDraftService idrepoDraftService;

    @Mock
    private Utility utility;

    @Mock
    private AuditLogRequestBuilder auditLogRequestBuilder;

    @Spy
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil = new RegistrationExceptionMapperUtil();

    @Mock
    private CbeffUtil cbeffutil;

    @Mock
    private PriorityBasedPacketManagerService packetManagerService;

    @Mock
    private IdSchemaUtil idSchemaUtil;

    @Mock
    private Utilities utilities;

    @Mock
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    @Captor
    private ArgumentCaptor<InternalRegistrationStatusDto> statusCaptor;

    private MessageDTO messageDTO;
    private InternalRegistrationStatusDto registrationStatusDto;
    private JSONObject identityMappingJson;
    private JSONObject documentMappingJson;

    @Before
    public void setUp() throws Exception {
        messageDTO = new MessageDTO();
        messageDTO.setRid(REG_ID);
        messageDTO.setIteration(1);
        messageDTO.setWorkflowInstanceId("wf-001");

        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId(REG_ID);
        registrationStatusDto.setRegistrationType("NEW");
        registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());

        identityMappingJson = buildSchemaVersionMappingJson();
        documentMappingJson = buildEmptyDocumentMappingJson();

        when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(registrationStatusDto);

        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(null);
        when(utility.isLatestPacket(nullable(String.class), nullable(String.class), anyString()))
                .thenReturn(StaleCheckResult.NOT_STALE);
        when(idrepoDraftService.idrepoHasDraft(anyString())).thenReturn(false);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenReturn(idResponseWithStatus(DRAFTED_STATUS));

        when(utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY))
                .thenReturn(identityMappingJson);
        when(utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT))
                .thenReturn(documentMappingJson);

        ReflectionTestUtils.setField(createDraftStage, "idRepoUpdate", "mosip.id.update");
        ReflectionTestUtils.setField(createDraftStage, "convertIdSchemaToDouble", true);
        ReflectionTestUtils.setField(createDraftStage, "trimWhitespaces", false);
        ReflectionTestUtils.setField(createDraftStage, "updateInfo", null);

        when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any()))
                .thenReturn("0.1");
        when(packetManagerService.getFields(anyString(), any(), any(), any()))
                .thenReturn(new HashMap<>());
        when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(new ArrayList<>());
    }

    // -----------------------------------------------------------------------
    // NEW packet
    // -----------------------------------------------------------------------

    @Test
    public void testNewPacketDraftSuccess() throws Exception {
        messageDTO.setReg_type("NEW");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
        verify(idrepoDraftService, never()).idrepoDiscardDraft(anyString());
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_SUCCESS.getCode());
        assertLastUpdatedStatusCode(RegistrationStatusCode.PROCESSING.toString());
    }

    @Test
    public void testNewPacketDiscardExistingDraftBeforeRecreate() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoDiscardDraft(REG_ID);
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
    }

    @Test
    public void testNewPacketNullIdRepoResponse_FailsWithReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenReturn(failedIdResponseWithErrorCode("IDR-IDC-004"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_FAILED.getCode());
    }

    @Test
    public void testNewPacketIdrepoDraftException_InternalError() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftException("CDS-005", "Draft update failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
    }

    @Test
    public void testNewPacketApisResourceAccessException_InternalError() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new ApisResourceAccessException("ID Repo unreachable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
    }

    @Test
    public void testNewPacketDiscardThrowsReprocessable() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID))
                .thenThrow(new IdrepoDraftReprocessableException("IDR-IDS-003", "Key manager unavailable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // UPDATE / RES_UPDATE
    // -----------------------------------------------------------------------

    @Test
    public void testUpdatePacketDraftSuccess() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), eq(EXISTING_UIN), any(), eq(true));
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_SUCCESS.getCode());
    }

    @Test
    public void testUpdatePacketDraftFailedNullResponse() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(true)))
                .thenReturn(emptyIdResponse());

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_FAILED.getCode());
    }

    @Test
    public void testUpdatePacketUinNotFound_FallsThroughNewPath() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(null);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), isNull(), any(), isNull()))
                .thenReturn(failedIdResponseWithErrorCode("IDR-IDC-005"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
    }

    @Test
    public void testUpdatePacketDiscardAndRecreate() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoDiscardDraft(REG_ID);
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), eq(EXISTING_UIN), any(), eq(true));
    }

    @Test
    public void testUpdatePacketPopulateDraftThrowsIdrepoDraftException() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(true)))
                .thenThrow(new IdrepoDraftException("CDS-005", "populate draft failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testResUpdatePacketSuccess() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), eq(EXISTING_UIN), any(), eq(true));
    }

    @Test
    public void testResUpdatePacketUinNotResolved_FallsThroughNewPath() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(null);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), isNull(), any(), isNull()))
                .thenReturn(failedIdResponseWithErrorCode("IDR-IDC-005"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
    }

    // -----------------------------------------------------------------------
    // LOST packet
    // -----------------------------------------------------------------------

    @Test
    public void testLostPacketDraftSuccess() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), eq(false));
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_SUCCESS.getCode());
    }

    @Test
    public void testLostPacketDiscardAndRecreate() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoDiscardDraft(REG_ID);
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), eq(false));
    }

    @Test
    public void testLostPacketIdrepoDraftException() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftException("CDS-005", "LOST draft failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testLostPacketApisResourceAccessException() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new ApisResourceAccessException("ID Repo unavailable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testLostPacketNullResponse_FailsWithoutInternalError() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(false)))
                .thenReturn(emptyIdResponse());

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_FAILED.getCode());
    }

    // -----------------------------------------------------------------------
    // ACTIVATED packet
    // -----------------------------------------------------------------------

    @Test
    public void testActivatedPacketSuccess() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("DEACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(idResponseWithStatus("ACTIVATED"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.UIN_ACTIVATED_SUCCESS.getCode());
        assertLastUpdatedStatusCode(RegistrationStatusCode.PROCESSED.toString());
    }

    @Test
    public void testActivatedPacketAlreadyActivated() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("ACTIVATED"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.UIN_ALREADY_ACTIVATED.getCode());
        assertLastUpdatedStatusCode(RegistrationStatusCode.FAILED.toString());
    }

    @Test
    public void testActivatedPacketWrongStatusAfterUpdate() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("DEACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(idResponseWithStatus("PROCESSING"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.UIN_ACTIVATED_FAILED.getCode());
    }

    @Test
    public void testActivatedPacketNullResponseFromUpdate() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("DEACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(emptyIdResponse());

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.UIN_REACTIVATION_FAILED.getCode());
    }

    @Test
    public void testActivatedPacketGetApiThrowsInternalError() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenThrow(new ApisResourceAccessException("ID Repo unreachable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // DEACTIVATED packet
    // -----------------------------------------------------------------------

    @Test
    public void testDeactivatedPacketSuccess() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("ACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(idResponseWithStatus("DEACTIVATED"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.UIN_DEACTIVATION_SUCCESS.getCode());
    }

    @Test
    public void testDeactivatedPacketAlreadyDeactivated() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("DEACTIVATED"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.UIN_ALREADY_DEACTIVATED.getCode());
    }

    @Test
    public void testDeactivatedPacketNullResponseFromUpdate() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("ACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(emptyIdResponse());

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.UIN_DEACTIVATION_FAILED.getCode());
    }

    @Test
    public void testDeactivatedPacketWrongStatusAfterUpdate() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("ACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(idResponseWithStatus("PROCESSING"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), eq(EXISTING_UIN), any(), eq(true));
    }

    @Test
    public void testDeactivatedPacketGetApiThrowsInternalError() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenThrow(new ApisResourceAccessException("ID Repo unreachable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    // -----------------------------------------------------------------------
    // Stale check
    // -----------------------------------------------------------------------

    @Test
    public void testStaleCheckUnavailable_TriggersReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(utility.isLatestPacket(nullable(String.class), nullable(String.class), anyString()))
                .thenReturn(StaleCheckResult.UNAVAILABLE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_UNABLE_TO_CHECK_STALE.getCode());
    }

    @Test
    public void testStaleCheckMarksPacketObsoleted() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(utility.isLatestPacket(nullable(String.class), nullable(String.class), anyString())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.CREATE_DRAFT_STALE_PACKET.getCode());
        assertLastUpdatedStatusCode(RegistrationStatusCode.FAILED.toString());
    }

    @Test
    public void testNewStaleReprocessCaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("NEW");
        when(utility.isLatestPacket(nullable(String.class), nullable(String.class), anyString())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    @Test
    public void testLostPacket_IgnoresStaleCheck() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        MessageDTO result = createDraftStage.process(messageDTO);

        // LOST path does not run handleStaleCheck — draft update still proceeds
        assertTrue(result.getIsValid());
        verify(utility, never()).isLatestPacket(nullable(String.class), nullable(String.class), anyString());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), eq(false));
    }

    @Test
    public void testActivatedStaleReprocessCaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(utility.isLatestPacket(nullable(String.class), nullable(String.class), anyString())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(registrationProcessorRestClientService, never())
                .getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(), eq(IdResponseDTO.class));
    }

    // -----------------------------------------------------------------------
    // Custom / unmapped packet types
    // -----------------------------------------------------------------------

    @Test
    public void testCustomTypeMappedToNewCreatesDraft() throws Exception {
        messageDTO.setReg_type("OPENCRVS_NEW");
        registrationStatusDto.setRegistrationType("OPENCRVS_NEW");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
    }

    @Test
    public void testUnknownTypeWithNoUin_CreatesDraftLikeNew() throws Exception {
        messageDTO.setReg_type("UNKNOWN_TYPE");
        registrationStatusDto.setRegistrationType("UNKNOWN_TYPE");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
    }

    // -----------------------------------------------------------------------
    // Audit / module id on success
    // -----------------------------------------------------------------------

    @Test
    public void testSuccessUsesCreateDraftPlatformSuccessCode() throws Exception {
        messageDTO.setReg_type("NEW");

        createDraftStage.process(messageDTO);

        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                eq(PlatformSuccessMessages.RPR_CREATE_DRAFT_SUCCESS.getCode()), anyString());
    }

    @Test
    public void testFailureUsesCreateDraftPlatformErrorCode() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenReturn(failedIdResponseWithErrorCode("IDR-IDC-999"));

        createDraftStage.process(messageDTO);

        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                eq(PlatformErrorMessages.RPR_CDS_DRAFT_CREATION_FAILED.getCode()), anyString());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static JSONObject buildSchemaVersionMappingJson() {
        LinkedHashMap<String, Object> schemaVersion = new LinkedHashMap<>();
        schemaVersion.put(MappingJsonConstants.VALUE, MappingJsonConstants.IDSCHEMA_VERSION);
        LinkedHashMap<String, Object> identity = new LinkedHashMap<>();
        identity.put(MappingJsonConstants.IDSCHEMA_VERSION, schemaVersion);
        return new JSONObject(identity);
    }

    private static JSONObject buildEmptyDocumentMappingJson() {
        return new JSONObject(new LinkedHashMap<>());
    }

    private static IdResponseDTO idResponseWithStatus(String status) {
        IdResponseDTO dto = new IdResponseDTO();
        ResponseDTO response = new ResponseDTO();
        response.setStatus(status);
        dto.setResponse(response);
        return dto;
    }

    private static IdResponseDTO emptyIdResponse() {
        return new IdResponseDTO();
    }

    private static IdResponseDTO failedIdResponseWithErrorCode(String errorCode) {
        IdResponseDTO dto = new IdResponseDTO();
        ErrorDTO error = new ErrorDTO();
        error.setErrorCode(errorCode);
        error.setMessage("ID Repo error");
        dto.setErrors(new ArrayList<>(Collections.singletonList(error)));
        return dto;
    }

    private void assertLastUpdatedSubStatus(String expectedSubStatusCode) {
        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                nullable(String.class), anyString());
        assertEquals(expectedSubStatusCode, statusCaptor.getValue().getSubStatusCode());
    }

    private void assertLastUpdatedStatusCode(String expectedStatusCode) {
        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                nullable(String.class), anyString());
        assertEquals(expectedStatusCode, statusCaptor.getValue().getStatusCode());
    }
}
