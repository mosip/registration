package io.mosip.registration.processor.stages.createdraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.packet.manager.dto.IdResponseDTO;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftException;
import io.mosip.registration.processor.packet.manager.exception.IdrepoDraftReprocessableException;
import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.storage.utils.Utility;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.stages.createdraft.stage.CreateDraftStage;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.dto.ResponseDTO;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.entity.RegLostUinDetEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.StaleCheckResult;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * Unit tests for {@link CreateDraftStage}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateDraftStageTest {

    private static final String REG_ID = "10001100770000320200720095022";
    private static final String EXISTING_UIN = "9876543210";

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

    @Mock
    private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

    @Mock
    private PriorityBasedPacketManagerService packetManagerService;

    @Mock
    private IdSchemaUtil idSchemaUtil;

    @Mock
    private Utilities utilities;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CbeffUtil cbeffutil;

    @Mock
    private RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    @Mock
    private BasePacketRepository<RegLostUinDetEntity, String> regLostUinDetEntity;

    @Mock
    private IdRepoService idRepoService;

    private MessageDTO messageDTO;
    private InternalRegistrationStatusDto registrationStatusDto;

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

        when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(registrationStatusDto);
        when(registrationStatusMapperUtil.getStatusCode(any())).thenReturn("ERROR");
        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.NOT_STALE);

        // Populate-draft path stubs: make the new code in process() succeed without
        // forcing every test to re-stub them. Tests that need different behaviour can
        // override these.
        ReflectionTestUtils.setField(createDraftStage, "idRepoUpdate", "mosip.id.update");
        ReflectionTestUtils.setField(createDraftStage, "convertIdschemaToDouble", true);
        ReflectionTestUtils.setField(createDraftStage, "trimWhitespaces", false);

        when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), any(), any()))
                .thenReturn("0.1");
        when(packetManagerService.getFields(anyString(), any(), any(), any()))
                .thenReturn(new HashMap<>());
        when(idSchemaUtil.getDefaultFields(any(Double.class))).thenReturn(new ArrayList<>());
        when(utilities.getRegistrationProcessorMappingJson(anyString())).thenReturn(new JSONObject());
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any()))
                .thenReturn(new IdResponseDTO());
    }

    // -----------------------------------------------------------------------
    // NEW packet – happy path
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_NewPacket_NoDraftExists_Success() throws Exception {
        messageDTO.setReg_type("NEW");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoDiscardDraft(anyString());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, null, true);
    }

    @Test
    public void testProcess_NewPacket_DraftAlreadyExists_DiscardAndRecreate() throws Exception {
        messageDTO.setReg_type("NEW");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoDiscardDraft(REG_ID);
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, null, true);
    }

    // -----------------------------------------------------------------------
    // UPDATE packet – happy path
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_UpdatePacket_NoDraftExists_Success() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false);
    }

    // -----------------------------------------------------------------------
    // Non-NEW/UPDATE packet types – skip
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_LostPacket_DraftAlreadyExists_DiscardAndRecreate() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoDiscardDraft(REG_ID);
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, null, false);
    }

    @Test
    public void testProcess_ActivatedPacket_Success() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // Current status is DEACTIVATED — valid to activate
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("DEACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        // idrepoUpdateDraft returns ACTIVATED status
        ResponseDTO activatedStatus = new ResponseDTO();
        activatedStatus.setStatus("ACTIVATED");
        IdResponseDTO activatedIdResponse = new IdResponseDTO();
        activatedIdResponse.setResponse(activatedStatus);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any()))
                .thenReturn(activatedIdResponse);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // ACTIVATED packet – error paths
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_ActivatedPacket_AlreadyActivated() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // UIN is already ACTIVATED — guard rejects with FAILED, no internalError
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("ACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
    }

    @Test
    public void testProcess_ActivatedPacket_WrongStatusAfterUpdate() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // Current status DEACTIVATED — activation allowed, but update echoes wrong status
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("DEACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        ResponseDTO wrongStatus = new ResponseDTO();
        wrongStatus.setStatus("PROCESSING");
        IdResponseDTO wrongIdResponse = new IdResponseDTO();
        wrongIdResponse.setResponse(wrongStatus);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(wrongIdResponse);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
    }

    @Test
    public void testProcess_ActivatedPacket_NullResponseFromUpdate() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // Current status DEACTIVATED — activation allowed
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("DEACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        // setUp default: idrepoUpdateDraft returns new IdResponseDTO() with null response
        // → isIdResponseNotNull = false → UIN_REACTIVATION_FAILED

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
    }

    // -----------------------------------------------------------------------
    // DEACTIVATED packet – happy path and error paths
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_DeactivatedPacket_Success() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // Current status ACTIVATED — deactivation allowed
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("ACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        // Update echoes DEACTIVATED → success
        ResponseDTO deactivatedStatus = new ResponseDTO();
        deactivatedStatus.setStatus("DEACTIVATED");
        IdResponseDTO deactivatedIdResponse = new IdResponseDTO();
        deactivatedIdResponse.setResponse(deactivatedStatus);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(deactivatedIdResponse);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
    }

    @Test
    public void testProcess_DeactivatedPacket_AlreadyDeactivated() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // UIN already DEACTIVATED — guard rejects with FAILED, no internalError
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("DEACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    @Test
    public void testProcess_DeactivatedPacket_NullResponseFromUpdate() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        // Current status ACTIVATED — deactivation allowed
        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("ACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        // setUp default: idrepoUpdateDraft returns new IdResponseDTO() with null response
        // → isIdResponseNotNull = false → UIN_DEACTIVATION_FAILED

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
    }

    // -----------------------------------------------------------------------
    // LOST packet – match found paths
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_LostPacket_Success() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, null, false);
    }

    @Test
    public void testProcess_LostPacket_CreateDraftV2Throws_InternalError() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, false))
                .thenThrow(new ApisResourceAccessException("ID Repo unavailable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testProcess_LostPacket_PopulateDraftThrows_InternalError() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, false)).thenReturn(true);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any()))
                .thenThrow(new IdrepoDraftException("DRAFT_ERROR", "update draft failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    // -----------------------------------------------------------------------
    // Error scenarios
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_NewPacket_DraftCreationReturnsFalse_PermanentFailure() throws Exception {
        messageDTO.setReg_type("NEW");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true)).thenReturn(false);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, null, true);
    }

    @Test
    public void testProcess_NewPacket_CreateDraftThrowsApiException_InternalError() throws Exception {
        messageDTO.setReg_type("NEW");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true))
                .thenThrow(new ApisResourceAccessException("ID Repo not reachable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testProcess_UpdatePacket_UinNotFound_InternalError() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
    }

    @Test
    public void testProcess_DraftCreationFails_InternalError() throws Exception {
        messageDTO.setReg_type("NEW");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true))
                .thenThrow(new IdrepoDraftException("CDS-001", "Draft creation failed"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testProcess_DraftCreationReprocessableException_InternalError() throws Exception {
        messageDTO.setReg_type("NEW");

        // Draft already exists; discarding it raises a reprocessable exception
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID))
                .thenThrow(new IdrepoDraftReprocessableException("IDR-IDS-003", "Key manager error"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    @Test
    public void testProcess_StaleCheckUnavailable_TriggersReprocess() throws Exception {
        messageDTO.setReg_type("NEW");

        when(utility.isLatestPacket(any(), any(), any()))
                .thenReturn(StaleCheckResult.UNAVAILABLE);

        MessageDTO result = createDraftStage.process(messageDTO);

        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
        verify(idrepoDraftService, never()).idrepoPublishDraft(anyString());
        assertTrue(result.getInternalError());
    }

    // -----------------------------------------------------------------------
    // Stale reprocess – STALE path → markAsObsoleted
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_StaleCheck_MarksPacketObsoleted() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        // markAsObsoleted sets FAILED status: isValid=false, internalError=false (no requeue)
        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    // -----------------------------------------------------------------------
    // RES_UPDATE – treated as update-like by isUpdateType()
    // -----------------------------------------------------------------------

    @Test
    public void testProcess_ResUpdatePacket_TreatedAsUpdateLike_Success() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false);
    }

    // -----------------------------------------------------------------------
    // Stale reprocess scenarios (Check 1 — CreateDraftStage)
    // -----------------------------------------------------------------------

    /**
     * Scenario 1 — NEW reprocess with no subsequent committed UPDATE.
     * isLatestPacket returns NOT_STALE (no newer packet found for null UIN) →
     * draft creation proceeds normally.
     */
    @Test
    public void testScenario1_NewReprocess_NoSubsequentUpdate_NotStale() throws Exception {
        messageDTO.setReg_type("NEW");
        registrationStatusDto.setRegistrationType("NEW");

        // Default setUp already stubs isLatestPacket → NOT_STALE
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue("Not-stale NEW reprocess must proceed to draft creation", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, null, true);
    }

    /**
     * Scenario 2 — Reprocess of the latest committed UPDATE.
     * lastCommittedRegId == currentRegId → isLatestPacket returns NOT_STALE →
     * draft created successfully.
     */
    @Test
    public void testScenario2_LatestUpdateReprocess_NotStale() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        // isLatestPacket → NOT_STALE (default from setUp)
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue("Latest UPDATE reprocess must not be marked stale", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false);
    }

    /**
     * Scenario 3 — Reprocess of the latest committed RES_UPDATE.
     * lastCommittedRegId == currentRegId → NOT_STALE → draft created successfully.
     */
    @Test
    public void testScenario3_LatestResUpdateReprocess_NotStale() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue("Latest RES_UPDATE reprocess must not be marked stale", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false);
    }

    /**
     * Scenario 4 — NEW stale reprocess caught at Check 1.
     * RID-001 (NEW, T1) was already processed; a newer UPDATE (RID-002, T2 > T1)
     * was committed afterwards. isLatestPacket returns STALE → REPROCESS_OBSOLETED.
     */
    @Test
    public void testScenario4_NewStaleReprocess_CaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("NEW");
        registrationStatusDto.setRegistrationType("NEW");

        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse("Stale NEW reprocess must be marked obsoleted (isValid=false)", result.getIsValid());
        assertFalse("Obsoleted packet must not set internalError (no requeue)", result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    /**
     * Scenario 5 — UPDATE stale reprocess caught at Check 1.
     * UPDATE-A (T2) was already processed; UPDATE-B (T3 > T2) was committed
     * afterwards. isLatestPacket returns STALE → REPROCESS_OBSOLETED.
     */
    @Test
    public void testScenario5_UpdateStaleReprocess_CaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse("Stale UPDATE reprocess must be marked obsoleted", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
    }

    /**
     * Scenario 6 — RES_UPDATE stale reprocess caught at Check 1.
     * RES_UPDATE (RID-002, T2) was committed; a newer UPDATE (RID-003, T3 > T2)
     * followed. isLatestPacket returns STALE → REPROCESS_OBSOLETED.
     */
    @Test
    public void testScenario6_ResUpdateStaleReprocess_CaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse("Stale RES_UPDATE reprocess must be marked obsoleted", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
    }

    /**
     * Scenario 7 — Reprocess of LOST RID-002 (an older LOST packet, not the latest).
     * isLatestPacket returns NOT_STALE → proceeds to draft creation.
     */
    @Test
    public void testScenario7_LostPacket_OlderPacket_NotStale() throws Exception {
        final String RID_002 = "10001100770000320200720095023";
        messageDTO.setRid(RID_002);
        messageDTO.setReg_type("LOST");

        InternalRegistrationStatusDto lostStatusDto = new InternalRegistrationStatusDto();
        lostStatusDto.setRegistrationId(RID_002);
        lostStatusDto.setRegistrationType("LOST");
        lostStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
        when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(lostStatusDto);

        when(idrepoDraftService.idrepoHasDraft(RID_002)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(RID_002, null, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue("LOST RID-002 reprocess must not be marked stale", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(RID_002, null, false);
    }

    /**
     * Scenario 8 — Reprocess of LOST RID-003 (the latest committed LOST packet).
     * lastCommittedRegId == currentRegId → NOT_STALE → draft created normally.
     */
    @Test
    public void testScenario8_LostPacket_LatestPacket_NotStale() throws Exception {
        final String RID_003 = "10001100770000320200720095024";
        messageDTO.setRid(RID_003);
        messageDTO.setReg_type("LOST");

        InternalRegistrationStatusDto lostStatusDto = new InternalRegistrationStatusDto();
        lostStatusDto.setRegistrationId(RID_003);
        lostStatusDto.setRegistrationType("LOST");
        lostStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
        when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(lostStatusDto);

        when(idrepoDraftService.idrepoHasDraft(RID_003)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(RID_003, null, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue("Latest LOST packet reprocess must not be marked stale", result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(RID_003, null, false);
    }

    /**
     * Scenario 9 — ACTIVATE stale reprocess caught at Check 1.
     * ACTIVATE (RID-A, T1) was processed; a newer UPDATE (RID-B, T2 > T1) was
     * committed afterwards. isLatestPacket returns STALE → REPROCESS_OBSOLETED.
     */
    @Test
    public void testScenario9_ActivateStaleReprocess_CaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse("Stale ACTIVATE reprocess must be marked obsoleted", result.getIsValid());
        assertFalse(result.getInternalError());
        // Stale check short-circuits before any ID Repo call
        verify(registrationProcessorRestClientService, never())
                .getApi(any(), any(), anyString(), anyString(), any());
    }

    /**
     * Scenario 10 — LOST stale reprocess caught at Check 1.
     * A LOST packet (RID-L, T1) was already processed; isLatestPacket returns
     * STALE → marked obsoleted, no draft operation attempted.
     *
     * Covers the gap identified from DSL scenario analysis: stale-check tests
     * existed for NEW/UPDATE/RES_UPDATE/ACTIVATED but not for LOST.
     */
    @Test
    public void testScenario10_LostStaleReprocess_CaughtAtCheck1() throws Exception {
        messageDTO.setReg_type("LOST");
        registrationStatusDto.setRegistrationType("LOST");

        when(utility.isLatestPacket(any(), any(), any())).thenReturn(StaleCheckResult.STALE);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse("Stale LOST reprocess must be marked obsoleted (isValid=false)", result.getIsValid());
        assertFalse("Obsoleted packet must not set internalError (no requeue)", result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    // -----------------------------------------------------------------------
    // UPDATE / RES_UPDATE – additional paths (from DSL Scenario_29/33 analysis)
    // -----------------------------------------------------------------------

    /**
     * UPDATE reprocess where a stale draft already exists from the previous
     * attempt. Mirrors the NEW/LOST test: old draft must be discarded before
     * re-creation.
     *
     * Root cause of Scenario_29/33 analysis: UPDATE reprocess path was missing
     * a discard+recreate test even though the same code path is exercised.
     */
    @Test
    public void testProcess_UpdatePacket_DraftAlreadyExists_DiscardAndRecreate() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(REG_ID)).thenReturn(true);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(true);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoDiscardDraft(REG_ID);
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false);
    }

    /**
     * UPDATE packet where idrepoCreateDraftV2 returns false → permanent failure,
     * no requeue (internalError=false).
     */
    @Test
    public void testProcess_UpdatePacket_DraftCreationReturnsFalse_PermanentFailure() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(false);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, times(1)).idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false);
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    /**
     * UPDATE packet where idrepoUpdateDraft (populate-draft call) throws
     * IdrepoDraftException → internal error, requeued for reprocess.
     */
    @Test
    public void testProcess_UpdatePacket_PopulateDraftThrows_IdrepoDraftException() throws Exception {
        messageDTO.setReg_type("UPDATE");
        registrationStatusDto.setRegistrationType("UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, EXISTING_UIN, false)).thenReturn(true);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any()))
                .thenThrow(new IdrepoDraftException("CDS-003", "populate draft failed for UPDATE"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    /**
     * RES_UPDATE packet with no UIN available → permanent failure, no draft
     * operation attempted. Mirrors the equivalent UPDATE test.
     */
    @Test
    public void testProcess_ResUpdatePacket_UinNotFound_PermanentFailure() throws Exception {
        messageDTO.setReg_type("RES_UPDATE");
        registrationStatusDto.setRegistrationType("RES_UPDATE");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(null);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // NEW – populate-draft exception paths
    // -----------------------------------------------------------------------

    /**
     * NEW packet where idrepoUpdateDraft (populate-draft) throws IdrepoDraftException
     * after the draft is successfully created → internal error, requeued.
     */
    @Test
    public void testProcess_NewPacket_PopulateDraftThrows_IdrepoDraftException() throws Exception {
        messageDTO.setReg_type("NEW");

        when(idrepoDraftService.idrepoHasDraft(REG_ID)).thenReturn(false);
        when(idrepoDraftService.idrepoCreateDraftV2(REG_ID, null, true)).thenReturn(true);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any()))
                .thenThrow(new IdrepoDraftException("CDS-002", "populate draft failed for NEW"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
    }

    /**
     * NEW packet where an existing draft's discard raises IdrepoDraftReprocessableException
     * on a second attempt with a different RID (ensures the reprocessable path is
     * reachable via discard, not only via populate-draft).
     *
     * Note: idrepoCreateDraftV2 does not declare IdrepoDraftReprocessableException
     * so the reprocessable path must be triggered through idrepoDiscardDraft.
     */
    @Test
    public void testProcess_NewPacket_DiscardThrowsReprocessable_OnSecondAttempt() throws Exception {
        final String RID_RETRY = "10001100770000320200720095029";
        messageDTO.setRid(RID_RETRY);
        messageDTO.setReg_type("NEW");

        InternalRegistrationStatusDto retryStatus = new InternalRegistrationStatusDto();
        retryStatus.setRegistrationId(RID_RETRY);
        retryStatus.setRegistrationType("NEW");
        retryStatus.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
        when(registrationStatusService.getRegistrationStatus(anyString(), any(), any(), any()))
                .thenReturn(retryStatus);

        // Draft from first attempt still exists; discarding it now throws reprocessable
        when(idrepoDraftService.idrepoHasDraft(RID_RETRY)).thenReturn(true);
        when(idrepoDraftService.idrepoDiscardDraft(RID_RETRY))
                .thenThrow(new IdrepoDraftReprocessableException("IDR-IDS-003", "Key manager unavailable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
    }

    // -----------------------------------------------------------------------
    // ACTIVATED / DEACTIVATED – API failure and wrong-status paths
    // -----------------------------------------------------------------------

    /**
     * ACTIVATED packet where the getIdRepoDataByUIN REST call throws
     * ApisResourceAccessException → caught by the outer catch, requeued.
     */
    @Test
    public void testProcess_ActivatedPacket_GetApiThrowsException_InternalError() throws Exception {
        messageDTO.setReg_type("ACTIVATED");
        registrationStatusDto.setRegistrationType("ACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenThrow(new ApisResourceAccessException("ID Repo unreachable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    /**
     * DEACTIVATED packet where the getIdRepoDataByUIN REST call throws
     * ApisResourceAccessException → caught by the outer catch, requeued.
     */
    @Test
    public void testProcess_DeactivatedPacket_GetApiThrowsException_InternalError() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenThrow(new ApisResourceAccessException("ID Repo unreachable"));

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertTrue(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

    /**
     * DEACTIVATED packet where idrepoUpdateDraft returns an unexpected status
     * (not DEACTIVATED) → isValid=false, internalError=false (REPROCESS
     * transactionStatusCode set, but not propagated as internalError).
     */
    @Test
    public void testProcess_DeactivatedPacket_WrongStatusAfterUpdate() throws Exception {
        messageDTO.setReg_type("DEACTIVATED");
        registrationStatusDto.setRegistrationType("DEACTIVATED");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn(EXISTING_UIN);

        ResponseDTO currentStatus = new ResponseDTO();
        currentStatus.setStatus("ACTIVATED");
        IdResponseDTO currentIdResponse = new IdResponseDTO();
        currentIdResponse.setResponse(currentStatus);
        when(registrationProcessorRestClientService.getApi(any(), any(), anyString(), anyString(), any()))
                .thenReturn(currentIdResponse);

        ResponseDTO wrongStatus = new ResponseDTO();
        wrongStatus.setStatus("PROCESSING");
        IdResponseDTO wrongIdResponse = new IdResponseDTO();
        wrongIdResponse.setResponse(wrongStatus);
        when(idrepoDraftService.idrepoUpdateDraft(anyString(), any(), any())).thenReturn(wrongIdResponse);

        MessageDTO result = createDraftStage.process(messageDTO);

        assertFalse(result.getIsValid());
        assertFalse(result.getInternalError());
    }

    // -----------------------------------------------------------------------
    // Unknown / custom packet type → pass-through (skip)
    // -----------------------------------------------------------------------

    /**
     * An unrecognised packet type (e.g. a custom CRVS type not mapped via
     * additionalProcessCategoryMapping) must pass through the stage without any
     * draft operation. Relevant to Scenario_13 analysis: ensures non-standard
     * packets don't erroneously trigger draft creation.
     */
    @Test
    public void testProcess_UnknownPacketType_DraftSkipped_PassThrough() throws Exception {
        messageDTO.setReg_type("UNKNOWN_TYPE");
        registrationStatusDto.setRegistrationType("UNKNOWN_TYPE");

        MessageDTO result = createDraftStage.process(messageDTO);

        assertTrue(result.getIsValid());
        assertFalse(result.getInternalError());
        verify(idrepoDraftService, never()).idrepoCreateDraftV2(anyString(), any(), anyBoolean());
        verify(idrepoDraftService, never()).idrepoUpdateDraft(anyString(), any(), any());
    }

}
