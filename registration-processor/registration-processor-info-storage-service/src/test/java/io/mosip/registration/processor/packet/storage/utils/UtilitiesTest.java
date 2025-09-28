package io.mosip.registration.processor.packet.storage.utils;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.idrepo.dto.RidDTO;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.exception.BiometricClassificationException;
import io.mosip.registration.processor.packet.storage.exception.PacketDateComputationException;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
public class UtilitiesTest {

    @InjectMocks
    private Utilities utilities;

    @Mock
    private PriorityBasedPacketManagerService packetManagerService;

    @Mock
    private IdRepoService idRepoService;

    @Mock
    private SyncRegistrationRepository syncRegistrationRepository;

    private InternalRegistrationStatusDto registrationStatusDto;
    private RidDTO ridDto;
    private SimpleDateFormat sdf;

    @Before
    public void setUp() {

        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId("10049100271000420250319064824");
        registrationStatusDto.setRegistrationType("UPDATE");
        ridDto = new RidDTO();
        ridDto.setRid("10049100271000420240319064824");
        ridDto.setUpd_dtimes("2024-01-01T12:00:00");
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        ReflectionTestUtils.setField(utilities, "dobFormat", "yyyy/MM/dd");
        ReflectionTestUtils.setField(utilities, "ageLimit", "5");
        ReflectionTestUtils.setField(utilities, "isVidSupportedForUpdate", false);
        ReflectionTestUtils.setField(utilities, "ageLimitBuffer", "1");
        ReflectionTestUtils.setField(utilities, "ageLimit", "5");
        ReflectionTestUtils.setField(utilities, "expectedPacketProcessingDurationHours", "0");
    }

    @Test
    public void testParseToLocalDateTime_Valid() {
        String dateStr = "2023-09-15T01:01:01.000Z";
        LocalDate result = Utilities.parseToLocalDate(dateStr, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testParseToLocalDate_Valid() {
        String dateStr = "20230915010101";
        LocalDate result = Utilities.parseToLocalDate(dateStr, "yyyyMMddHHmmss");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testParseToLocalDate_FutureDate() {
        String dateStr = LocalDateTime.now().plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDate result = Utilities.parseToLocalDate(dateStr, "yyyyMMddHHmmss");
        assertNotNull(result);
    }

    @Test
    public void testParseToLocalDate_TooOldDate() {
        String dateStr = LocalDateTime.now().minusYears(150).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDate result = Utilities.parseToLocalDate(dateStr, "yyyyMMddHHmmss");
        assertNotNull(result);
    }

    @Test
    public void testCalculateAgeAtLastPacketProcessing() throws Exception {
        LocalDate dob = LocalDate.of(2010, 1, 1);
        LocalDate packetDate = LocalDate.of(2020, 1, 1);

        int age = utilities.calculateAgeAtLastPacketProcessing(dob, packetDate);

        assertEquals(10, age);
    }

    @Test
    public void testGetPacketCreatedDateFromRid_Valid() {
        String rid = "1234567890123420230915010101";
        LocalDate result = utilities.getPacketCreatedDateFromRid(rid);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testGetPacketCreatedDateFromRid_InvalidShortRid() {
        String rid = "12345";
        LocalDate result = utilities.getPacketCreatedDateFromRid(rid);
        assertNull(result);
    }


    @Test
    public void testWasInfantWhenLastPacketProcessed_Success_idvid() throws Exception {
        LinkedHashMap<String, Object> dobMap = new LinkedHashMap<>();
        dobMap.put(MappingJsonConstants.VALUE, "dateOfBirth");
        LinkedHashMap<String, Object> packetCreatedOnMap = new LinkedHashMap<>();
        packetCreatedOnMap.put(MappingJsonConstants.VALUE, "packetCreatedOn");

        LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
        identityMap.put(MappingJsonConstants.DOB, dobMap);
        identityMap.put(MappingJsonConstants.PACKET_CREATED_ON, packetCreatedOnMap);

        JSONObject mappingJsonObject = new JSONObject();
        mappingJsonObject.put("identity", identityMap);
        ReflectionTestUtils.setField(utilities, "mappingJsonObject", mappingJsonObject);

        String uin = "12345";
        String dob = "2023/01/01";
        String packetCreatedDate = "2025-04-30T07:04:49.681Z";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        response.put("packetCreatedOn", packetCreatedDate);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420250319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);

        assertTrue(result);
    }


    @Test(expected = PacketDateComputationException.class)
    public void testWasInfantWhenLastPacketProcessed_nullIdVidMetadata_throwsPacketDateComputationException() throws Exception {

        LinkedHashMap<String, Object> dobMap = new LinkedHashMap<>();
        dobMap.put(MappingJsonConstants.VALUE, "dateOfBirth");

        LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
        identityMap.put(MappingJsonConstants.DOB, dobMap);

        JSONObject mappingJsonObject = new JSONObject();
        mappingJsonObject.put("identity", identityMap);
        ReflectionTestUtils.setField(utilities, "mappingJsonObject", mappingJsonObject);

        String uin = "12345";
        String dob = "2023/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        when(idRepoService.searchIdVidMetadata(anyString())).thenReturn(null);

        utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_fromRegListTable_returnsTrue() throws Exception {

        LinkedHashMap<String, Object> dobMap = new LinkedHashMap<>();
        dobMap.put(MappingJsonConstants.VALUE, "dateOfBirth");

        LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
        identityMap.put(MappingJsonConstants.DOB, dobMap);

        JSONObject mappingJsonObject = new JSONObject();
        mappingJsonObject.put("identity", identityMap);
        ReflectionTestUtils.setField(utilities, "mappingJsonObject", mappingJsonObject);

        String uin = "12345";
        String dob = "2023/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        when(idRepoService.searchIdVidMetadata(anyString())).thenReturn(ridDto);

        SyncRegistrationEntity syncRegistration = new SyncRegistrationEntity();
        syncRegistration.setRegistrationId("10049100271000420240319064824");
        syncRegistration.setPacketId("10049100271000420240319064824");
        syncRegistration.setCreateDateTime(LocalDateTime.of(2024, 1, 1, 12, 30, 45));
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        syncRegistrationEntityList.add(syncRegistration);
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWasInfantWhenLastPacketProcessed_responseDTONull_returnsNull() throws Exception {

        LinkedHashMap<String, Object> dobMap = new LinkedHashMap<>();
        dobMap.put(MappingJsonConstants.VALUE, "dateOfBirth");

        LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
        identityMap.put(MappingJsonConstants.DOB, dobMap);

        JSONObject mappingJsonObject = new JSONObject();
        mappingJsonObject.put("identity", identityMap);
        ReflectionTestUtils.setField(utilities, "mappingJsonObject", mappingJsonObject);

        String uin = "12345";
        String dob = "2023/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(null);

        when(idRepoService.searchIdVidMetadata(anyString())).thenReturn(ridDto);

        SyncRegistrationEntity syncRegistration = new SyncRegistrationEntity();
        syncRegistration.setRegistrationId("10049100271000420240319064824");
        syncRegistration.setPacketId("10049100271000420240319064824"); // last 14 chars = 20240101123045
        syncRegistration.setCreateDateTime(LocalDateTime.of(2024, 1, 1, 12, 30, 45));
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        syncRegistrationEntityList.add(syncRegistration);
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test
    public void testWasInfant_LastPacketFromRidFallback() throws Exception {
        LinkedHashMap<String, Object> dobMap = new LinkedHashMap<>();
        dobMap.put(MappingJsonConstants.VALUE, "dateOfBirth");

        LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
        identityMap.put(MappingJsonConstants.DOB, dobMap);

        JSONObject mappingJsonObject = new JSONObject();
        mappingJsonObject.put("identity", identityMap);
        ReflectionTestUtils.setField(utilities, "mappingJsonObject", mappingJsonObject);

        String uin = "12345";
        String dob = "2023/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        when(idRepoService.searchIdVidMetadata(anyString())).thenReturn(ridDto);

        SyncRegistrationEntity syncRegistration = new SyncRegistrationEntity();
        syncRegistration.setRegistrationId("10049100271000420240319064824");
        syncRegistration.setPacketId(null);
        syncRegistration.setCreateDateTime(LocalDateTime.of(2024, 1, 1, 12, 30, 45));
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        syncRegistrationEntityList.add(syncRegistration);
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testWasInfant_IdentityUpdateFallback() throws Exception {
        LinkedHashMap<String, Object> dobMap = new LinkedHashMap<>();
        dobMap.put(MappingJsonConstants.VALUE, "dateOfBirth");

        LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
        identityMap.put(MappingJsonConstants.DOB, dobMap);

        JSONObject mappingJsonObject = new JSONObject();
        mappingJsonObject.put("identity", identityMap);
        ReflectionTestUtils.setField(utilities, "mappingJsonObject", mappingJsonObject);

        String uin = "12345";
        String dob = "2023/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        ridDto = new RidDTO();
        ridDto.setRid(null);
        ridDto.setUpd_dtimes("2025-09-26T11:09:22.477Z");

        when(idRepoService.searchIdVidMetadata(anyString())).thenReturn(ridDto);

        SyncRegistrationEntity syncRegistration = new SyncRegistrationEntity();
        syncRegistration.setRegistrationId(null);
        syncRegistration.setPacketId(null);
        syncRegistration.setCreateDateTime(LocalDateTime.of(2024, 1, 1, 12, 30, 45));
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        syncRegistrationEntityList.add(syncRegistration);
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testGetEffectiveAgeLimit() {
        int limit = utilities.getEffectiveAgeLimit();
        assertEquals(6, limit);
    }

    @Test
    public void getPacketCreatedDateFromRid_Valid(){
        LocalDate date = utilities.getPacketCreatedDateFromRid("RID20250917090000");
        assertEquals(LocalDate.of(2025, 9, 17), date);
    }

    @Test
    public void getPacketCreatedDateFromRid_InvalidShortRid() {
        LocalDate date = utilities.getPacketCreatedDateFromRid("RID123");
        assertNull(date);
    }

    @Test
    public void testRetrieveCreatedDateFromPacketFromPacketManager() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
        Map<String, String> metaInfo = new HashMap<>();
        String packetCreatedDate = "2025-05-28T10:53:13.973Z";
        metaInfo.put("creationDate",packetCreatedDate);
        when(packetManagerService.getMetaInfo(anyString(),anyString(),any(ProviderStageName.class))).thenReturn(metaInfo);
        String res = utilities.retrieveCreatedDateFromPacket("10049100271000420250319064824","NEW", ProviderStageName.UIN_GENERATOR);
        assertEquals(packetCreatedDate,res);
    }

    @Test
    public void testRetrieveCreatedDateFromPacket_nullDate_returnsNull()
            throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
        String rid = "10049100271000420250319064824";
        String process = "PROCESS";
        ProviderStageName stageName = ProviderStageName.BIO_DEDUPE;

        Map<String, String> metaInfo = Collections.emptyMap();
        when(packetManagerService.getMetaInfo(rid, process, stageName)).thenReturn(metaInfo);

        String result = utilities.retrieveCreatedDateFromPacket(rid, process, stageName);

        assertNull(result);
    }

    private BIR createBIR(BiometricType type, String exceptionValue) {
        BDBInfo bdbInfo = mock(BDBInfo.class);
        when(bdbInfo.getType()).thenReturn(Collections.singletonList(type));

        BIR bir = mock(BIR.class);
        when(bir.getBdbInfo()).thenReturn(bdbInfo);

        Map<String, String> others = new HashMap<>();
        if (exceptionValue != null) {
            others.put("EXCEPTION", exceptionValue);
        }
        when(bir.getOthers()).thenReturn((HashMap<String, String>) others);
        return bir;
    }

    @Test(expected = BiometricException.class)
    public void testAllBiometricHaveException_nullList_throwsException() throws BiometricException {
        utilities.allBiometricHaveException(null);
    }

    @Test
    public void testAllBiometricHaveException_allExceptionsTrue_returnsTrue() throws BiometricException {
        List<BIR> birs = Arrays.asList(
                createBIR(BiometricType.FINGER, "true"),
                createBIR(BiometricType.IRIS, "true")
        );

        boolean result = utilities.allBiometricHaveException(birs);
        assertTrue(result);
    }

    @Test
    public void testAllBiometricHaveException_missingException_returnsFalse() throws BiometricException {
        List<BIR> birs = Arrays.asList(
                createBIR(BiometricType.FINGER, "true"),
                createBIR(BiometricType.IRIS, null)
        );

        boolean result = utilities.allBiometricHaveException(birs);
        assertFalse(result);
    }

    @Test
    public void testAllBiometricHaveException_faceOrExceptionPhotoIgnored_returnsTrue() throws BiometricException {
        List<BIR> birs = Arrays.asList(
                createBIR(BiometricType.FACE, null),
                createBIR(BiometricType.EXCEPTION_PHOTO, null),
                createBIR(BiometricType.IRIS, "true")
        );

        boolean result = utilities.allBiometricHaveException(birs);
        assertTrue(result);
    }

    @Test
    public void testAllBiometricHaveException_noOthers_returnsFalse() throws BiometricException {
        BIR bir = mock(BIR.class);
        BDBInfo bdbInfo = mock(BDBInfo.class);
        when(bdbInfo.getType()).thenReturn(Collections.singletonList(BiometricType.IRIS));
        when(bir.getBdbInfo()).thenReturn(bdbInfo);
        when(bir.getOthers()).thenReturn(null);

        List<BIR> birs = Collections.singletonList(bir);

        boolean result = utilities.allBiometricHaveException(birs);
        assertFalse(result);
    }

    private BIR createBIRWithOthers(boolean hasOthers) {
        BIR bir = mock(BIR.class);
        Map<String, String> map = hasOthers ? Collections.singletonMap("EXCEPTION", "true") : Collections.emptyMap();
        when(bir.getOthers()).thenReturn(new HashMap<>(map));
        return bir;
    }

    @Test
    public void testHasBiometricWithOthers_nullList_returnsFalse() {
        assertFalse(utilities.hasBiometricWithOthers(null));
    }

    @Test
    public void testHasBiometricWithOthers_emptyList_returnsFalse() {
        assertFalse(utilities.hasBiometricWithOthers(Collections.emptyList()));
    }

    @Test
    public void testHasBiometricWithOthers_noOthers_returnsFalse() {
        List<BIR> birs = Arrays.asList(createBIRWithOthers(false));
        assertFalse(utilities.hasBiometricWithOthers(birs));
    }

    @Test
    public void testHasBiometricWithOthers_hasOthers_returnsTrue() {
        List<BIR> birs = Arrays.asList(createBIRWithOthers(false), createBIRWithOthers(true));
        assertTrue(utilities.hasBiometricWithOthers(birs));
    }

    @Test(expected = BiometricClassificationException.class)
    public void testAllBiometricHaveException_rid_exception() throws Exception {
        String rid = "6654322111444444";
        String registrationType = "UPDATE";
        ProviderStageName stageName = ProviderStageName.BIO_DEDUPE;

        when(packetManagerService.getField(rid, MappingJsonConstants.UIN, registrationType, stageName))
                .thenThrow(new RuntimeException("Service failure"));

        utilities.allBiometricHaveException(rid, registrationType, stageName);
    }

    @Test
    public void testGetBiometricRecordfromIdrepo_noDocuments_returnsNull() throws Exception {
        String uin = "UIN123";
        Utilities spyUtils = spy(utilities);
        doReturn(Collections.emptyList()).when(spyUtils).retrieveIdrepoDocument(uin);

        BiometricRecord result = spyUtils.getBiometricRecordfromIdrepo(uin);
        assertNull(result);
    }
}
