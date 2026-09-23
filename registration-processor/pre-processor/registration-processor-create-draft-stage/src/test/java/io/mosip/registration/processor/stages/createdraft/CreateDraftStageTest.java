package io.mosip.registration.processor.stages.createdraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.PacketManagerNonRecoverableException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdRequestDto;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.dto.Document;
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
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

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
    private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

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
        ReflectionTestUtils.setField(createDraftStage, "additionalProcessCategoryMapping", new HashMap<String, String>());
        ReflectionTestUtils.setField(createDraftStage, "objectMapper", new ObjectMapper());

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
    public void testNewPacketIdRepoValidationError_MarksFailed() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftException("IDR-IDC-005", "Input Data Validation Failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
    }

    @Test
    public void testNewPacketIdrepoDraftReprocessableException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftReprocessableException("IDR-IDC-004", "Unknown error occurred"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION.getCode());
        assertLastUpdatedStatusCode(RegistrationStatusCode.PROCESSING.toString());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
    }

    @Test
    public void testNewPacketRecordAlreadyExists_MarksFailed() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftException("IDR-IDC-012", "Record already exists in DB"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
    }

    @Test
    public void testNewPacketHasDraftException_MarksFailed() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoHasDraft(REG_ID))
                .thenThrow(new IdrepoDraftException(PlatformErrorMessages.DRAFT_CHECK_FAILED.getCode(),
                        PlatformErrorMessages.DRAFT_CHECK_FAILED.getMessage()));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
    }

    @Test
    public void testNewPacketIdrepoDraftException_InternalError() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftException("IDR-IDC-002", "Invalid Input Parameter"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
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
    public void testUpdatePacketNullResponse_MarksReprocess() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(true)))
                .thenThrow(new ApisResourceAccessException("Null response from idrepoUpdateDraftV2 for id " + REG_ID));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
    }

    @Test
    public void testUpdatePacketUinNotFound_FallsThroughNewPath() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(null);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), isNull(), any(), isNull()))
                .thenThrow(new IdrepoDraftException("IDR-IDC-005", "Input Data Validation Failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
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
                .thenThrow(new IdrepoDraftException("IDR-IDC-002", "Invalid Input Parameter"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
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
                .thenThrow(new IdrepoDraftException("IDR-IDC-005", "Input Data Validation Failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
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
                .thenThrow(new IdrepoDraftException("IDR-IDC-002", "Invalid Input Parameter"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.FAILED.toString());
    }

    @Test
    public void testLostPacketIdrepoDraftReprocessableException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(false)))
                .thenThrow(new IdrepoDraftReprocessableException("IDR-IDS-003", "Key manager failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IDREPO_DRAFT_REPROCESSABLE_EXCEPTION.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
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
    public void testLostPacketNullResponse_MarksReprocess() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(false)))
                .thenThrow(new ApisResourceAccessException("Null response from idrepoUpdateDraftV2 for id " + REG_ID));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
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
    public void testActivatedPacketNullResponseFromUpdate_MarksReprocess() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("DEACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenThrow(new ApisResourceAccessException("Null response from idrepoUpdateDraftV2 for id " + REG_ID));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
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
    public void testDeactivatedPacketNullResponseFromUpdate_MarksReprocess() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("ACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenThrow(new ApisResourceAccessException("Null response from idrepoUpdateDraftV2 for id " + REG_ID));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
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
        assertLastUpdatedStatusCode(RegistrationStatusCode.PROCESSING.toString());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
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
    public void testNewStalePacketMarksObsoleted() throws Exception {
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

        // LOST path does not run handleStaleCheck or retrieve packetCreatedOn — draft update still proceeds
        assertTrue(result.getIsValid());
        verify(utility, never()).isLatestPacket(nullable(String.class), nullable(String.class), anyString());
        verify(utility, never()).retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), any());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), eq(false));
    }

    @Test
    public void testActivatedStalePacketMarksObsoleted() throws Exception {
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
    // Demographic identity parsing
    // -----------------------------------------------------------------------

    @Test
    public void testNewPacketLoadsDemographicIdentityFieldTypes() throws Exception {
        messageDTO.setReg_type("NEW");
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("email", "mono@mono.com");
        fieldMap.put("skipped", null);
        fieldMap.put("individualBiometrics",
                "{\"format\":\"cbeff\",\"value\":\"individualBiometrics_bio_CBEFF\",\"version\":1}");
        fieldMap.put("selectedHandles", "[\"nrcId\",\"email\"]");
        fieldMap.put("fullName", "[{\"language\":\"eng\",\"value\":\"Bob\"}]");
        when(packetManagerService.getFields(anyString(), any(), any(), any())).thenReturn(fieldMap);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        JSONObject identity = captureNewPacketIdentity();
        assertEquals("mono@mono.com", identity.get("email"));
        assertFalse(identity.containsKey("skipped"));
        assertTrue(identity.get("individualBiometrics") instanceof Map);
        assertEquals("cbeff", ((Map<?, ?>) identity.get("individualBiometrics")).get("format"));
        assertTrue(identity.get("selectedHandles") instanceof List);
        assertEquals("nrcId", ((List<?>) identity.get("selectedHandles")).get(0));
        assertTrue(identity.get("fullName") instanceof List);
        assertEquals("Bob", ((Map<?, ?>) ((List<?>) identity.get("fullName")).get(0)).get("value"));
    }

    @Test
    public void testNewPacketTrimsWhitespaceOnSimpleTypeValue() throws Exception {
        messageDTO.setReg_type("NEW");
        ReflectionTestUtils.setField(createDraftStage, "trimWhitespaces", true);
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("fullName", "[{\"language\":\"eng\",\"value\":\"  Bob  \"}]");
        when(packetManagerService.getFields(anyString(), any(), any(), any())).thenReturn(fieldMap);

        createDraftStage.process(messageDTO);

        JSONObject identity = captureNewPacketIdentity();
        assertEquals("Bob", ((Map<?, ?>) ((List<?>) identity.get("fullName")).get(0)).get("value"));
    }

    @Test
    public void testConvertIdSchemaToDoubleFalse_KeepsSchemaVersionAsString() throws Exception {
        messageDTO.setReg_type("NEW");
        ReflectionTestUtils.setField(createDraftStage, "convertIdSchemaToDouble", false);

        createDraftStage.process(messageDTO);

        JSONObject identity = captureNewPacketIdentity();
        assertEquals("0.1", identity.get(MappingJsonConstants.IDSCHEMA_VERSION));
    }

    @Test
    public void testUinFieldStringNull_UsesNewDraftPath() throws Exception {
        messageDTO.setReg_type("NEW");
        registrationStatusDto.setRegistrationType("NEW");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn("null");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), any());
    }

    // -----------------------------------------------------------------------
    // packetCreatedOn
    // Always fetched for stale check. Stored in demographic identity only for
    // NEW/UPDATE when the ID schema default fields include PACKET_CREATED_ON.
    // -----------------------------------------------------------------------

    @Test
    public void testNewPacketFetchesPacketCreatedOnWhenInSchema() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");
        when(utility.getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON)).thenReturn("packetCreatedOn");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(isNull(), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        assertEquals("2019-01-17T06:29:01.940Z", captureNewPacketIdentity().get("packetCreatedOn"));
    }

    @Test
    public void testUpdatePacketFetchesPacketCreatedOnWhenInSchema() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");
        when(utility.getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON)).thenReturn("packetCreatedOn");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(eq(EXISTING_UIN), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        JSONObject identity = captureUpdatePacketIdentity();
        assertEquals("2019-01-17T06:29:01.940Z", identity.get("packetCreatedOn"));
    }

    @Test
    public void testNewPacketFetchesPacketCreatedOnWhenNotInSchema_DoesNotAddToIdentity() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(Arrays.asList("fullName", "dateOfBirth"));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(isNull(), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        verify(utility, never()).getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        JSONObject identity = captureNewPacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testUpdatePacketFetchesPacketCreatedOnWhenNotInSchema_DoesNotAddToIdentity() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idSchemaUtil.getDefaultFields(anyDouble())).thenReturn(Arrays.asList("fullName", "dateOfBirth"));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(eq(EXISTING_UIN), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        verify(utility, never()).getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        JSONObject identity = captureUpdatePacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testResUpdateFetchesPacketCreatedOnForStaleCheck_DoesNotAddToIdentityEvenWhenInSchema() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(eq(EXISTING_UIN), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        verify(utility, never()).getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        JSONObject identity = captureUpdatePacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testActivatedFetchesPacketCreatedOnForStaleCheck_DoesNotAddToIdentityEvenWhenInSchema() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("DEACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(idResponseWithStatus("ACTIVATED"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(eq(EXISTING_UIN), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        verify(utility, never()).getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        JSONObject identity = captureUpdatePacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testDeactivatedFetchesPacketCreatedOnForStaleCheck_DoesNotAddToIdentityEvenWhenInSchema() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(idResponseWithStatus("ACTIVATED"));
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), eq(EXISTING_UIN), any(), eq(true)))
                .thenReturn(idResponseWithStatus("DEACTIVATED"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(eq(EXISTING_UIN), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        verify(utility, never()).getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        JSONObject identity = captureUpdatePacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testPacketCreatedOnSkippedWhenMappingMissing() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn("2019-01-17T06:29:01.940Z");
        when(utility.getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON)).thenReturn(null);

        createDraftStage.process(messageDTO);

        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(isNull(), eq("2019-01-17T06:29:01.940Z"), eq(REG_ID));
        JSONObject identity = captureNewPacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testPacketCreatedOnSkippedWhenValueMissingEvenIfInSchema() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idSchemaUtil.getDefaultFields(anyDouble()))
                .thenReturn(Arrays.asList(MappingJsonConstants.PACKET_CREATED_ON));
        when(utility.retrieveCreatedDateFromPacket(anyString(), anyString(), any(ProviderStageName.class), nullable(Map.class)))
                .thenReturn(null);

        createDraftStage.process(messageDTO);

        verify(utility, times(1)).retrieveCreatedDateFromPacket(eq(REG_ID), anyString(),
                eq(ProviderStageName.CREATE_DRAFT), nullable(Map.class));
        verify(utility).isLatestPacket(isNull(), isNull(String.class), eq(REG_ID));
        verify(utility, never()).getMappedFieldName(MappingJsonConstants.PACKET_CREATED_ON);
        JSONObject identity = captureNewPacketIdentity();
        assertFalse(identity.containsKey("packetCreatedOn"));
        assertNull(identity.get("packetCreatedOn"));
    }

    @Test
    public void testRetrieveCreatedDateThrows_MarksPacketManagerReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getMetaInfo(anyString(), anyString(), any(ProviderStageName.class)))
                .thenThrow(new PacketManagerException("RPR-PKM-001", "metaInfo failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    @Test
    public void testGetUInThrowsIoException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class)))
                .thenThrow(new IOException("unable to read UIN"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IO_EXCEPTION.getCode());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Documents / biometrics
    // -----------------------------------------------------------------------

    @Test
    public void testNewPacketAttachesDocumentsAndBiometrics() throws Exception {
        messageDTO.setReg_type("NEW");
        stubDocumentAndBiometricMappings();
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("proofOfAddress", "{\"value\":\"POA_Rental\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
        fieldMap.put("individualBiometrics",
                "{\"format\":\"cbeff\",\"version\":1,\"value\":\"applicant_bio_CBEFF\"}");
        when(packetManagerService.getFields(anyString(), any(), any(), any())).thenReturn(fieldMap);

        Document document = new Document();
        document.setDocument("document".getBytes());
        document.setValue("proofOfAddress");
        when(packetManagerService.getDocument(eq(REG_ID), eq("proofOfAddress"), anyString(),
                eq(ProviderStageName.CREATE_DRAFT))).thenReturn(document);
        when(packetManagerService.getBiometrics(eq(REG_ID), eq("individualBiometrics"), anyString(),
                eq(ProviderStageName.CREATE_DRAFT))).thenReturn(new BiometricRecord());
        when(cbeffutil.createXML(any())).thenReturn("cbeff".getBytes());
        when(utilities.getMappingJsonValue(eq("individualBiometrics"), eq(MappingJsonConstants.IDENTITY)))
                .thenReturn("individualBiometrics");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), isNull());
        List<Documents> documents = requestCaptor.getValue().getRequest().getDocuments();
        assertEquals(2, documents.size());
        verify(packetManagerService, times(1)).getDocument(eq(REG_ID), eq("proofOfAddress"), anyString(),
                eq(ProviderStageName.CREATE_DRAFT));
        verify(packetManagerService, times(1)).getBiometrics(eq(REG_ID), eq("individualBiometrics"), anyString(),
                eq(ProviderStageName.CREATE_DRAFT));
    }

    @Test
    public void testNewPacketSkipsNullDocumentAndUnmappedDocKey() throws Exception {
        messageDTO.setReg_type("NEW");
        stubDocumentAndBiometricMappings();
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("proofOfAddress", "{\"value\":\"POA_Rental\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
        when(packetManagerService.getFields(anyString(), any(), any(), any())).thenReturn(fieldMap);
        when(packetManagerService.getDocument(eq(REG_ID), eq("proofOfAddress"), anyString(),
                eq(ProviderStageName.CREATE_DRAFT))).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), isNull());
        List<Documents> documents = requestCaptor.getValue().getRequest().getDocuments();
        assertTrue(documents == null || documents.isEmpty());
        verify(packetManagerService, never()).getBiometrics(anyString(), anyString(), anyString(), any());
    }

    @Test
    public void testNewPacketDocumentFetchThrows_MarksPacketManagerReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        stubDocumentAndBiometricMappings();
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("proofOfAddress", "{\"value\":\"POA_Rental\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
        when(packetManagerService.getFields(anyString(), any(), any(), any())).thenReturn(fieldMap);
        when(packetManagerService.getDocument(eq(REG_ID), eq("proofOfAddress"), anyString(),
                eq(ProviderStageName.CREATE_DRAFT))).thenThrow(new PacketManagerException("RPR-PKM-001", "doc failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // process() exception catches
    // -----------------------------------------------------------------------

    @Test
    public void testJsonProcessingException_MarksFailed() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any()))
                .thenThrow(new JsonProcessingException("invalid json"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.JSON_PARSING_EXCEPTION.getCode());
    }

    @Test
    public void testPacketManagerNonRecoverableException_MarksFailed() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getFields(anyString(), any(), any(), any()))
                .thenThrow(new PacketManagerNonRecoverableException("RPR-PKM-004", "non recoverable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.PACKET_MANAGER_NON_RECOVERABLE_EXCEPTION.getCode());
    }

    @Test
    public void testPacketManagerException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getFields(anyString(), any(), any(), any()))
                .thenThrow(new PacketManagerException("RPR-PKM-001", "recoverable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.PACKET_MANAGER_EXCEPTION.getCode());
    }

    @Test
    public void testIoExceptionFromGetFields_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getFields(anyString(), any(), any(), any())).thenThrow(new IOException("io"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IO_EXCEPTION.getCode());
    }

    @Test
    public void testUnknownException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getFields(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
    }

    // -----------------------------------------------------------------------
    // HTTP wrapping
    // -----------------------------------------------------------------------

    @Test
    public void testNewPacketHttpClientErrorException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(apiExceptionWithCause(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
    }

    @Test
    public void testNewPacketHttpServerErrorException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(apiExceptionWithCause(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
    }

    @Test
    public void testUpdatePacketHttpClientErrorException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), eq(true)))
                .thenThrow(apiExceptionWithCause(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
    }

    @Test
    public void testActivatedGetApiHttpClientErrorException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class)))
                .thenThrow(apiExceptionWithCause(new HttpClientErrorException(HttpStatus.BAD_REQUEST)));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
    }

    @Test
    public void testActivatedGetApiHttpServerErrorException_MarksReprocess() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class)))
                .thenThrow(apiExceptionWithCause(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.API_RESOUCE_ACCESS_FAILED.getCode());
    }

    // -----------------------------------------------------------------------
    // LOST updateInfo
    // -----------------------------------------------------------------------

    @Test
    public void testLostPacketUpdateInfoPopulatesMappedFields() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        ReflectionTestUtils.setField(createDraftStage, "updateInfo", "phone,email");
        putIdentityMapping("phone", "phone");
        putIdentityMapping("email", "email");
        Map<String, String> fetched = new HashMap<>();
        fetched.put("phone", "9999999999");
        fetched.put("email", "lost@example.com");
        when(packetManagerService.getFields(eq(REG_ID), any(), any(), any())).thenReturn(fetched);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), eq(false));
        JSONObject identity = (JSONObject) requestCaptor.getValue().getRequest().getIdentity();
        assertEquals("9999999999", identity.get("phone"));
        assertEquals("lost@example.com", identity.get("email"));
    }

    @Test
    public void testLostPacketUpdateInfoSkipsUnmappedKeysAndNullValues() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        ReflectionTestUtils.setField(createDraftStage, "updateInfo", "unknownField,phone,email");
        putIdentityMapping("phone", "phone");
        putIdentityMapping("email", "email");
        Map<String, String> fetched = new HashMap<>();
        fetched.put("phone", "9999999999");
        fetched.put("email", null);
        when(packetManagerService.getFields(eq(REG_ID), any(), any(), any())).thenReturn(fetched);

        createDraftStage.process(messageDTO);

        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), eq(false));
        JSONObject identity = (JSONObject) requestCaptor.getValue().getRequest().getIdentity();
        assertEquals("9999999999", identity.get("phone"));
        assertFalse(identity.containsKey("email"));
        assertFalse(identity.containsKey("unknownField"));
    }

    @Test
    public void testLostPacketUpdateInfoNullFetchedFields_DoesNotFail() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        ReflectionTestUtils.setField(createDraftStage, "updateInfo", "phone");
        putIdentityMapping("phone", "phone");
        when(packetManagerService.getFields(eq(REG_ID), any(), any(), any())).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), eq(false));
    }

    @Test
    public void testLostPacketDoesNotSendDocumentsInDraftRequest() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        stubDocumentAndBiometricMappings();
        Map<String, String> packetFields = new HashMap<>();
        packetFields.put("proofOfAddress", "{\"value\":\"POA_Rental\",\"type\":\"Rental contract\",\"format\":\"jpg\"}");
        packetFields.put("individualBiometrics",
                "{\"format\":\"cbeff\",\"version\":1,\"value\":\"applicant_bio_CBEFF\"}");
        lenient().when(packetManagerService.getFields(anyString(), any(), any(), any())).thenReturn(packetFields);
        Document document = new Document();
        document.setDocument("document".getBytes());
        document.setValue("proofOfAddress");
        lenient().when(packetManagerService.getDocument(anyString(), anyString(), anyString(), any())).thenReturn(document);
        lenient().when(packetManagerService.getBiometrics(anyString(), anyString(), anyString(), any()))
                .thenReturn(new BiometricRecord());
        lenient().when(cbeffutil.createXML(any())).thenReturn("cbeff".getBytes());

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), eq(false));
        assertNull(requestCaptor.getValue().getRequest().getDocuments());
        verify(packetManagerService, never()).getDocument(anyString(), anyString(), anyString(), any());
        verify(packetManagerService, never()).getBiometrics(anyString(), anyString(), anyString(), any());
    }

    @Test
    public void testLostPacketSendsOnlyConfiguredUpdateInfoFields() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");
        ReflectionTestUtils.setField(createDraftStage, "updateInfo", "phone,email");
        putIdentityMapping("phone", "phone");
        putIdentityMapping("email", "email");
        putIdentityMapping("fullName", "fullName");
        putIdentityMapping("dob", "dateOfBirth");
        Map<String, String> packetFields = new HashMap<>();
        packetFields.put("phone", "9999999999");
        packetFields.put("email", "lost@example.com");
        packetFields.put("fullName", "[{\"language\":\"eng\",\"value\":\"Bob\"}]");
        packetFields.put("dateOfBirth", "1990/01/01");
        packetFields.put("addressLine1", "not configured");
        when(packetManagerService.getFields(eq(REG_ID), any(), any(), eq(ProviderStageName.CREATE_DRAFT)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> requested = invocation.getArgument(1);
                    Map<String, String> filtered = new HashMap<>();
                    for (String field : requested) {
                        if (packetFields.containsKey(field)) {
                            filtered.put(field, packetFields.get(field));
                        }
                    }
                    return filtered;
                });

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> requestedFieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(packetManagerService).getFields(eq(REG_ID), requestedFieldsCaptor.capture(), eq("LOST"),
                eq(ProviderStageName.CREATE_DRAFT));
        assertEquals(Arrays.asList("phone", "email"), requestedFieldsCaptor.getValue());

        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), eq(false));
        JSONObject identity = (JSONObject) requestCaptor.getValue().getRequest().getIdentity();
        assertEquals("9999999999", identity.get("phone"));
        assertEquals("lost@example.com", identity.get("email"));
        assertFalse(identity.containsKey("fullName"));
        assertFalse(identity.containsKey("dateOfBirth"));
        assertFalse(identity.containsKey("addressLine1"));
    }

    // -----------------------------------------------------------------------
    // Custom process mapped to UPDATE
    // -----------------------------------------------------------------------

    @Test
    public void testCustomTypeMappedToUpdateCreatesDraftWithUin() throws Exception {
        messageDTO.setReg_type("CRVS_UPDATE");
        registrationStatusDto.setRegistrationType("CRVS_UPDATE");
        Map<String, String> mapping = new HashMap<>();
        mapping.put("CRVS_UPDATE", "UPDATE");
        ReflectionTestUtils.setField(createDraftStage, "additionalProcessCategoryMapping", mapping);
        when(utilities.getInternalProcess(any(), eq("CRVS_UPDATE"))).thenReturn("UPDATE");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), eq(EXISTING_UIN), any(), eq(true));
    }

    // -----------------------------------------------------------------------
    // GET-by-UIN null response (not IdrepoDraftService)
    // -----------------------------------------------------------------------

    @Test
    public void testActivatedGetApiNullResponse_MarksReprocess() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        IdResponseDTO empty = new IdResponseDTO();
        empty.setResponse(null);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(empty);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.UIN_REACTIVATION_FAILED.getCode());
    }

    @Test
    public void testDeactivatedGetApiNullResponse_MarksReprocess() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        IdResponseDTO empty = new IdResponseDTO();
        empty.setResponse(null);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(empty);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.UIN_DEACTIVATION_FAILED.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
    }

    @Test
    public void testDeactivatedGetApiNullDto_MarksReprocess() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");
        when(utility.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(eq(ApiName.IDREPOGETIDBYUIN), any(), anyString(), anyString(),
                eq(IdResponseDTO.class))).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
        assertLastUpdatedSubStatus(StatusUtil.UIN_DEACTIVATION_FAILED.getCode());
        assertLastUpdatedTransactionStatus(RegistrationTransactionStatusCode.REPROCESS.toString());
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
    public void testFailureUsesIdrepoDraftExceptionPlatformErrorCode() throws Exception {
        messageDTO.setReg_type("NEW");
        when(idrepoDraftService.idrepoUpdateDraftV2(anyString(), any(), any(), any()))
                .thenThrow(new IdrepoDraftException("IDR-IDC-002", "Invalid Input Parameter"));

        createDraftStage.process(messageDTO);

        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                eq(PlatformErrorMessages.IDREPO_DRAFT_EXCEPTION.getCode()), anyString());
    }

    // -----------------------------------------------------------------------
    // registration_list packet metaInfo
    // -----------------------------------------------------------------------

    @Test
    public void testStorePacketMetaInfoWritesJsonColumnsAndReusesMetaInfoForCreatedDate() throws Exception {
        messageDTO.setReg_type("NEW");
        String metaData = "[{\"label\":\"centerId\",\"value\":\"10001\"}]";
        String operationsData = "[{\"label\":\"officerId\",\"value\":\"officer\"}]";
        String capturedDevices = "[{\"deviceCode\":\"DEV-1\"}]";
        Map<String, String> metaInfo = packetMetaInfo(metaData, operationsData, capturedDevices);
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT)).thenReturn(metaInfo);

        SyncRegistrationEntity entity = registrationListRow("MOSIP", LocalDateTime.of(2024, 1, 2, 3, 4));
        when(syncRegistrationService.findByWorkflowInstanceId("wf-001")).thenReturn(entity);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        ArgumentCaptor<SyncRegistrationEntity> saved = ArgumentCaptor.forClass(SyncRegistrationEntity.class);
        verify(syncRegistrationService).findByWorkflowInstanceId("wf-001");
        verify(syncRegistrationService).update(saved.capture());
        assertEquals(metaData, saved.getValue().getPacketMetaData());
        assertEquals(operationsData, saved.getValue().getPacketOperationsData());
        assertEquals(capturedDevices, saved.getValue().getPacketCapturedDevices());
        assertEquals("MOSIP", saved.getValue().getUpdatedBy());
        assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4), saved.getValue().getUpdateDateTime());
        verify(packetManagerService, times(1)).getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT);
        verify(utility).retrieveCreatedDateFromPacket(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT, metaInfo);
    }

    @Test
    public void testStorePacketMetaInfoOverwritesSameRowOnReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        SyncRegistrationEntity entity = registrationListRow("MOSIP", LocalDateTime.of(2024, 1, 2, 3, 4));
        entity.setPacketMetaData("[{\"label\":\"centerId\",\"value\":\"old\"}]");
        entity.setPacketOperationsData("[{\"label\":\"officerId\",\"value\":\"old\"}]");
        entity.setPacketCapturedDevices("[{\"deviceCode\":\"OLD\"}]");
        when(syncRegistrationService.findByWorkflowInstanceId("wf-001")).thenReturn(entity);

        String metaData = "[{\"label\":\"centerId\",\"value\":\"10001\"}]";
        String operationsData = "[{\"label\":\"officerId\",\"value\":\"officer\"}]";
        String capturedDevices = "[{\"deviceCode\":\"DEV-1\"}]";
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT))
                .thenReturn(packetMetaInfo(metaData, operationsData, capturedDevices));

        createDraftStage.process(messageDTO);

        ArgumentCaptor<SyncRegistrationEntity> saved = ArgumentCaptor.forClass(SyncRegistrationEntity.class);
        verify(syncRegistrationService, times(1)).update(saved.capture());
        assertEquals(entity, saved.getValue());
        assertEquals(metaData, saved.getValue().getPacketMetaData());
        assertEquals(operationsData, saved.getValue().getPacketOperationsData());
        assertEquals(capturedDevices, saved.getValue().getPacketCapturedDevices());
        assertEquals("MOSIP", saved.getValue().getUpdatedBy());
        assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4), saved.getValue().getUpdateDateTime());
    }

    @Test
    public void testStorePacketMetaInfoStoresNullForEmptyAndLiteralNull() throws Exception {
        messageDTO.setReg_type("NEW");
        Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put(JsonConstant.METADATA, "null");
        metaInfo.put(JsonConstant.OPERATIONSDATA, "");
        metaInfo.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, "NULL");
        metaInfo.put(JsonConstant.CREATIONDATE, "2025-05-28T10:53:13.973Z");
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT)).thenReturn(metaInfo);
        when(syncRegistrationService.findByWorkflowInstanceId("wf-001"))
                .thenReturn(registrationListRow("MOSIP", LocalDateTime.of(2024, 1, 2, 3, 4)));

        createDraftStage.process(messageDTO);

        ArgumentCaptor<SyncRegistrationEntity> saved = ArgumentCaptor.forClass(SyncRegistrationEntity.class);
        verify(syncRegistrationService).update(saved.capture());
        assertNull(saved.getValue().getPacketMetaData());
        assertNull(saved.getValue().getPacketOperationsData());
        assertNull(saved.getValue().getPacketCapturedDevices());
    }

    @Test
    public void testStorePacketMetaInfoSkipsUpdateWhenMetaInfoMissing() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT)).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        verify(syncRegistrationService, never()).findByWorkflowInstanceId(anyString());
        verify(syncRegistrationService, never()).update(any());
    }

    @Test
    public void testStorePacketMetaInfoSkipsUpdateWhenMetaInfoEmpty() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT))
                .thenReturn(new HashMap<>());

        createDraftStage.process(messageDTO);

        verify(syncRegistrationService, never()).findByWorkflowInstanceId(anyString());
        verify(syncRegistrationService, never()).update(any());
    }

    @Test
    public void testStorePacketMetaInfoSkipsUpdateWhenRegistrationListRowMissing() throws Exception {
        messageDTO.setReg_type("NEW");
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT))
                .thenReturn(packetMetaInfo("[{\"label\":\"centerId\",\"value\":\"10001\"}]", "[]", "[]"));
        when(syncRegistrationService.findByWorkflowInstanceId("wf-001")).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(syncRegistrationService, never()).update(any());
        verify(idrepoDraftService, times(1)).idrepoUpdateDraftV2(eq(REG_ID), isNull(), any(), isNull());
    }

    @Test
    public void testStorePacketMetaInfoInvalidJsonMarksReprocess() throws Exception {
        messageDTO.setReg_type("NEW");
        Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put(JsonConstant.METADATA, "{not-json");
        when(packetManagerService.getMetaInfo(REG_ID, "NEW", ProviderStageName.CREATE_DRAFT)).thenReturn(metaInfo);
        when(syncRegistrationService.findByWorkflowInstanceId("wf-001"))
                .thenReturn(registrationListRow("MOSIP", LocalDateTime.of(2024, 1, 2, 3, 4)));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getInternalError());
        assertLastUpdatedSubStatus(StatusUtil.IO_EXCEPTION.getCode());
        verify(syncRegistrationService, never()).update(any());
        verify(idrepoDraftService, never()).idrepoUpdateDraftV2(anyString(), any(), any(), any());
    }

    private static Map<String, String> packetMetaInfo(String metaData, String operationsData, String capturedDevices) {
        Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put(JsonConstant.METADATA, metaData);
        metaInfo.put(JsonConstant.OPERATIONSDATA, operationsData);
        metaInfo.put(JsonConstant.CAPTUREDREGISTEREDDEVICES, capturedDevices);
        metaInfo.put(JsonConstant.CREATIONDATE, "2025-05-28T10:53:13.973Z");
        return metaInfo;
    }

    private static SyncRegistrationEntity registrationListRow(String updatedBy, LocalDateTime updateDateTime) {
        SyncRegistrationEntity entity = new SyncRegistrationEntity();
        entity.setUpdatedBy(updatedBy);
        entity.setUpdateDateTime(updateDateTime);
        return entity;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JSONObject captureNewPacketIdentity() throws Exception {
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), isNull(), requestCaptor.capture(), isNull());
        return (JSONObject) requestCaptor.getValue().getRequest().getIdentity();
    }

    private JSONObject captureUpdatePacketIdentity() throws Exception {
        ArgumentCaptor<IdRequestDto> requestCaptor = ArgumentCaptor.forClass(IdRequestDto.class);
        verify(idrepoDraftService).idrepoUpdateDraftV2(eq(REG_ID), eq(EXISTING_UIN), requestCaptor.capture(), eq(true));
        return (JSONObject) requestCaptor.getValue().getRequest().getIdentity();
    }

    @SuppressWarnings("unchecked")
    private void putIdentityMapping(String mappingKey, String actualFieldName) {
        LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        inner.put(MappingJsonConstants.VALUE, actualFieldName);
        identityMappingJson.put(mappingKey, inner);
    }

    private void stubDocumentAndBiometricMappings() throws IOException {
        putIdentityMapping(MappingJsonConstants.INDIVIDUAL_BIOMETRICS, "individualBiometrics");
        LinkedHashMap<String, Object> poa = new LinkedHashMap<>();
        poa.put(MappingJsonConstants.VALUE, "proofOfAddress");
        LinkedHashMap<String, Object> documents = new LinkedHashMap<>();
        documents.put("proofOfAddress", poa);
        documentMappingJson = new JSONObject(documents);
        when(utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT))
                .thenReturn(documentMappingJson);
        when(utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY))
                .thenReturn(identityMappingJson);
    }

    private static ApisResourceAccessException apiExceptionWithCause(Exception cause) {
        return new ApisResourceAccessException(cause.getMessage(), cause);
    }

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

    private void assertLastUpdatedSubStatus(String expectedSubStatusCode) {
        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                nullable(String.class), anyString());
        assertEquals(expectedSubStatusCode, statusCaptor.getValue().getSubStatusCode());
    }

    private void assertLastUpdatedTransactionStatus(String expectedTransactionStatus) {
        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                nullable(String.class), anyString());
        assertEquals(expectedTransactionStatus, statusCaptor.getValue().getLatestTransactionStatusCode());
    }

    private void assertLastUpdatedStatusCode(String expectedStatusCode) {
        verify(registrationStatusService).updateRegistrationStatus(statusCaptor.capture(),
                nullable(String.class), anyString());
        assertEquals(expectedStatusCode, statusCaptor.getValue().getStatusCode());
    }
}
