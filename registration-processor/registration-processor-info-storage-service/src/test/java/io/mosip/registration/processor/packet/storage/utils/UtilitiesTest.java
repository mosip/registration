package io.mosip.registration.processor.packet.storage.utils;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.idrepo.dto.IdVidMetadataRequest;
import io.mosip.registration.processor.core.idrepo.dto.IdVidMetadataResponse;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.core.exception.BiometricClassificationException;
import io.mosip.registration.processor.core.exception.PacketDateComputationException;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.repositary.SyncRegistrationRepository;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
@PrepareForTest({ Utilities.class, CryptoUtil.class, RegProcessorLogger.class, CbeffValidator.class, DateUtils.class, Utility.class })
public class UtilitiesTest {

    @InjectMocks
    private Utilities utilities;

    @InjectMocks
    private Utility utility;

    @Mock
    private PriorityBasedPacketManagerService packetManagerService;

    @Mock
    private IdRepoService idRepoService;

    @Mock
    private SyncRegistrationRepository syncRegistrationRepository;

    @Mock
    private Logger regProcLogger;

    private InternalRegistrationStatusDto registrationStatusDto;
    private IdVidMetadataResponse idVidMetadataResponse;
    private SimpleDateFormat sdf;
    private String identityMappingjsonString;
    JSONObject identityObj;

    @Before
    public void setUp() throws IOException {

        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId("10049100271000420250319064824");
        registrationStatusDto.setRegistrationType("UPDATE");
        idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");
        idVidMetadataResponse.setUpdatedOn("2024-01-01T12:00:00");
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        ReflectionTestUtils.setField(utilities, "dobFormat", "yyyy/MM/dd");
        ReflectionTestUtils.setField(utilities, "ageLimit", "5");
        ReflectionTestUtils.setField(utility, "isVidSupportedForUpdate", false);
        ReflectionTestUtils.setField(utilities, "ageLimitBuffer", 0);
        ReflectionTestUtils.setField(utilities, "expectedPacketProcessingDurationHours", 0);

//        InputStream inputStream = getClass().getClassLoader()
//                .getResourceAsStream("RegistrationProcessorIdentity.json");
//        String identityMappingjsonString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
//
//        ObjectMapper mapper = new ObjectMapper();
//        LinkedHashMap jsonMap = mapper.readValue(identityMappingjsonString, LinkedHashMap.class);
//
//        LinkedHashMap identityMap = (LinkedHashMap) jsonMap.get("identity");
//
//        JSONObject identityObj = new JSONObject(identityMap);
//
//        when(utilities.getRegistrationProcessorMappingJson(any())).thenReturn(identityObj);

        ClassLoader classLoader1 = getClass().getClassLoader();
        File idJsonFile1 = new File(classLoader1.getResource("RegistrationProcessorIdentity.json").getFile());
        InputStream idJsonStream1 = new FileInputStream(idJsonFile1);
        LinkedHashMap hm = new com.fasterxml.jackson.databind.ObjectMapper().readValue(idJsonStream1, LinkedHashMap.class);
        JSONObject jsonObject = new JSONObject(hm);
        identityMappingjsonString = jsonObject.toJSONString();
        identityObj = JsonUtil.getJSONObject(new com.fasterxml.jackson.databind.ObjectMapper().readValue(identityMappingjsonString, JSONObject.class), MappingJsonConstants.IDENTITY);
        when(utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(identityObj);

        PowerMockito.mockStatic(Utilities.class);
        PowerMockito.when(Utilities.getJson(anyString(), anyString())).thenReturn(identityMappingjsonString);
        PowerMockito.mockStatic(RegProcessorLogger.class);
        when(RegProcessorLogger.getLogger(any(Class.class))).thenReturn(regProcLogger);

    }

    @Test
    public void testParseToLocalDateTime_Valid() {
        String dateStr = "2023-09-15T01:01:01.000Z";
        LocalDate result = utilities.parseToLocalDate(dateStr, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testParseToLocalDate_Valid() {
        String dateStr = "20230915010101";
        LocalDate result = utilities.parseToLocalDate(dateStr, "yyyyMMddHHmmss");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testParseToLocalDate_Valid_DOB() {
        String dateStr = "2023/09/15";
        LocalDate result = utilities.parseToLocalDate(dateStr, "yyyy/MM/dd");
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testParseToLocalDate_FutureDate() {
        String dateStr = LocalDateTime.now().plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDate result = utilities.parseToLocalDate(dateStr, "yyyyMMddHHmmss");
        assertNull(result);
    }

    @Test
    public void testParseToLocalDate_TooOldDate() {
        String dateStr = LocalDateTime.now().minusYears(250).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDate result = utilities.parseToLocalDate(dateStr, "yyyyMMddHHmmss");
        assertNull(result);
    }

    @Test
    public void testParseToLocalDate_ShouldEnterCatchBlock_ForInvalidDate() {
        String invalidDate = "abccdefgh";
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        LocalDate result = utilities.parseToLocalDate(invalidDate, pattern);
        assertNull("Expected null for invalid date string", result);
    }

    @Test
    public void testCalculateAgeAtLastPacketProcessing() {
        LocalDate dob = LocalDate.of(2010, 1, 1);
        LocalDate packetDate = LocalDate.of(2020, 1, 1);
        String rid = "20241211";

        int age = utilities.calculateAgeAtLastPacketProcessing(dob, packetDate, rid);

        assertEquals(10, age);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_infantScenario_returnsTrue() throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

        String registrationId = "10049100271000420250319064824";
        String registrationType = "NEW";
        ProviderStageName stageName = ProviderStageName.BIO_DEDUPE;

        IdVidMetadataResponse mockIdVidMetadataResponse = new IdVidMetadataResponse();
        mockIdVidMetadataResponse.setRid(registrationId);
        mockIdVidMetadataResponse.setUpdatedOn("2025-10-25T10:00:00.000Z");
        mockIdVidMetadataResponse.setCreatedOn("2025-10-25T09:00:00.000Z");

        when(utility.getUIn(anyString(), anyString(), any())).thenReturn("123456789012");
        String uin = "12345";
        String dob = "2023/01/01";

        JSONObject identityJson = new JSONObject();
        identityJson.put("dateOfBirth", dob);

        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
                .thenReturn(uin);

        when(utilities.getIdVidMetadata(anyString(), any())).thenReturn(mockIdVidMetadataResponse);
        when(utilities.getDateOfBirthFromIdrepo(anyString(), any(JSONObject.class))).thenReturn(LocalDate.of(2023, 1, 1));
        when(utilities.computePacketCreatedFromIdentityUpdate(any(IdVidMetadataResponse.class), anyString())).thenReturn(LocalDate.of(2025, 10, 24));

        boolean result = utilities.wasInfantWhenLastPacketProcessed(registrationId, registrationType, stageName);

        assertTrue(result);
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
    public void testWasInfantWhenLastPacketProcessed_Success_idvid() throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

        String uin = "12345";
        String dob = "2023/01/01";
        String packetCreatedDate = "2025-04-30T07:04:49.681Z";

        JSONObject identityJson = new JSONObject();
        identityJson.put("dateOfBirth", dob);
        identityJson.put("packetCreatedOn", packetCreatedDate);

        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
                .thenReturn(uin);

        boolean result = utilities.wasInfantWhenLastPacketProcessed(
                "10004133140010820251009123300", "UPDATE", ProviderStageName.BIO_DEDUPE);

        assertTrue(result);
    }

    @Test(expected = PacketDateComputationException.class)
    public void testWasInfantWhenLastPacketProcessed_NullidvidResponse() throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

        String uin = "123458665";
        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(null);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
                .thenReturn(uin);
        utilities.wasInfantWhenLastPacketProcessed(
                "10004133140010820251009123300", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test(expected = PacketDateComputationException.class)
    public void testWasInfantWhenLastPacketProcessed_NullDOB() throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

        String uin = "123458665";
        String packetCreatedDate = "2025-04-30T07:04:49.681Z";

        JSONObject identityJson = new JSONObject();
        identityJson.put("packetCreatedOn", packetCreatedDate);
        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
                .thenReturn(uin);
        utilities.wasInfantWhenLastPacketProcessed(
                "10004133140010820251009123300", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test(expected = PacketDateComputationException.class)
    public void testWasInfantWhenLastPacketProcessed_NullPacketCreatedOn() throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {

        String uin = "1444458665";
        String dob = "2023/01/01";

        JSONObject identityJson = new JSONObject();
        identityJson.put("dateOfBirth", dob);
        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
                .thenReturn(uin);
        utilities.wasInfantWhenLastPacketProcessed(
                "10004133140010820251009123300", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_WhenUinIsNull_ReturnsFalse() throws Exception {

        Utility utilityMock = Mockito.mock(Utility.class);
        ReflectionTestUtils.setField(utilities, "utility", utilityMock);

        when(utilityMock.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(" ");

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10004133140010820251009123300", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertFalse(result);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_WhenUinIsEmpty_ReturnsFalse() throws Exception {

        Utility utilityMock = Mockito.mock(Utility.class);
        ReflectionTestUtils.setField(utilities, "utility", utilityMock);

        when(utilityMock.getUIn(anyString(), anyString(), any(ProviderStageName.class))).thenReturn(" ");

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10004133140010820251009123300", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertFalse(result);
    }

    @Test(expected = PacketDateComputationException.class)
    public void testWasInfantWhenLastPacketProcessed_nullIdVidMetadata_throwsPacketDateComputationException() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

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

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(null);

        utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_returnIdVidMetadata() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

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

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");

       when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);

       boolean result =  utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
       assertTrue(result);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_fromRegListTable_returnsTrue() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

        String uin = "123454433";
        String dob = "2021/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);

        SyncRegistrationEntity syncRegistration = new SyncRegistrationEntity();
        syncRegistration.setRegistrationId("10049100271000420240319064824");
        syncRegistration.setPacketId("10028100061024620250528155257-10028_10006-20250528155257");
        syncRegistration.setCreateDateTime(LocalDateTime.of(2024, 1, 1, 12, 30, 45));
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        syncRegistrationEntityList.add(syncRegistration);
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testWasInfant_LastPacketFromRidFallback() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

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

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);

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
    public void testWasInfant_computePacketCreatedFromIdentityUpdate_returnUpdatedOn() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

        String uin = "6654433332";
        String dob = "2022/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");
        idVidMetadataResponse.setUpdatedOn("2025-09-27T11:09:22.477Z");
        idVidMetadataResponse.setCreatedOn("2025-09-26T11:09:22.477Z");

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);

        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testWasInfant_computePacketCreatedFromIdentityUpdate_returnCreatedOn() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

        String uin = "6654433332";
        String dob = "2022/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");
        idVidMetadataResponse.setUpdatedOn(null);
        idVidMetadataResponse.setCreatedOn("2025-09-26T11:09:22.477Z");

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_InfantWithinBuffer_ReturnsTrue() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

        String uin = "6654433332";
        String dob = "2020/01/01";
        ReflectionTestUtils.setField(utilities, "ageLimitBuffer", 3);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid("10049100271000420240319064824");
        idVidMetadataResponse.setUpdatedOn(null);
        idVidMetadataResponse.setCreatedOn("2027-01-01T11:09:22.477Z");

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);
        List<SyncRegistrationEntity> syncRegistrationEntityList = new ArrayList<>();
        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(syncRegistrationEntityList);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testWasInfantWhenLastPacketProcessed_lastPacketProcessedTimeGreaterThanDOB_withExpectedPacketProcessingDurationHours() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

        String uin = "12345";
        String dob = "2020/01/01";
        String packetCreatedDate = "2020-01-03T10:00:00.000Z";
        ReflectionTestUtils.setField(utilities, "expectedPacketProcessingDurationHours", 120);

        JSONObject identityJson = new JSONObject();
        identityJson.put("dateOfBirth", dob);
        identityJson.put("packetCreatedOn", packetCreatedDate);

        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any()))
                .thenReturn(uin);

        boolean result = utilities.wasInfantWhenLastPacketProcessed("10049100271000420250319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
        assertTrue(result);
    }

    @Test
    public void testComputePacketCreatedFromIdentityUpdate_withExpectedPacketProcessingDurationHours() {
        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setUpdatedOn("2025-10-27T10:00:00.000Z");

        ReflectionTestUtils.setField(utilities, "expectedPacketProcessingDurationHours", 24);

        PowerMockito.mockStatic(DateUtils.class);
        when(DateUtils.parseUTCToLocalDateTime(anyString(), anyString()))
                .thenReturn(LocalDateTime.of(2025, 10, 27, 10, 0));

        LocalDate result = utilities.computePacketCreatedFromIdentityUpdate(idVidMetadataResponse, "10049100271000420250319064824");

        assertEquals(LocalDate.of(2025, 10, 26), result);
    }

    @Test(expected = PacketDateComputationException.class)
    public void testComputePacketCreatedFromIdentityUpdate_ParseException() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {

        String uin = "6654433332";
        String dob = "2022/01/01";
        Mockito.when(packetManagerService.getFieldByMappingJsonKey(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        IdVidMetadataRequest idVidMetadataRequest = new IdVidMetadataRequest();
        idVidMetadataRequest.setIndividualId(uin);

        IdVidMetadataResponse idVidMetadataResponse = new IdVidMetadataResponse();
        idVidMetadataResponse.setRid(null);
        idVidMetadataResponse.setUpdatedOn(null);
        idVidMetadataResponse.setCreatedOn("2025-09-26");

        when(idRepoService.searchIdVidMetadata(idVidMetadataRequest)).thenReturn(idVidMetadataResponse);

        when(syncRegistrationRepository.findByRegistrationId(anyString())).thenReturn(null);

        utilities.wasInfantWhenLastPacketProcessed("10049100271000420240319064824", "UPDATE", ProviderStageName.BIO_DEDUPE);
    }

    @Test
    public void testGetEffectiveAgeLimit() {
        ReflectionTestUtils.setField(utilities, "ageLimitBuffer", 1);
        int limit = utilities.getEffectiveAgeLimit();
        assertEquals(6, limit);
    }

    @Test
    public void getPacketCreatedDateFromRid_InvalidShortRid() {
        LocalDate date = utilities.getPacketCreatedDateFromRid("");
        assertNull(date);
    }

    @Test
    public void testGetPacketCreatedDateFromRid_AlphanumericRid() {
        String rid = "1001190001ABCD01234567";
        LocalDate date  = utilities.getPacketCreatedDateFromRid(rid);
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

    @Test(expected = BiometricClassificationException.class)
    public void testAllBiometricHaveException_nullList_throwsException() throws BiometricClassificationException {
        utilities.allBiometricHaveException(null, null);
    }

    @Test
    public void testAllBiometricHaveException_noOthers_returnsFalse() throws  BiometricClassificationException{
        String rid = "10049100271000420250319064824";
        BIR bir = mock(BIR.class);
        BDBInfo bdbInfo = mock(BDBInfo.class);
        when(bdbInfo.getType()).thenReturn(Collections.singletonList(BiometricType.IRIS));
        when(bir.getBdbInfo()).thenReturn(bdbInfo);
        when(bir.getOthers()).thenReturn(null);

        List<BIR> birs = Collections.singletonList(bir);

        boolean result = utilities.allBiometricHaveException(birs, rid);
        assertFalse(result);
    }

    @Test
    public void testHasBiometricWithOthers_nullList_returnsFalse() {
        assertFalse(utilities.hasBiometricWithOthers(null));
    }

    @Test
    public void testHasBiometricWithOthers_emptyList_returnsFalse() {
        assertFalse(utilities.hasBiometricWithOthers(Collections.emptyList()));
    }

    @Test(expected = BiometricClassificationException.class)
    public void testAllBiometricHaveException_rid_exception() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
        String rid = "6654322111444444";
        String registrationType = "UPDATE";
        ProviderStageName stageName = ProviderStageName.BIO_DEDUPE;

        when(packetManagerService.getField(rid, MappingJsonConstants.UIN, registrationType, stageName))
                .thenThrow(new RuntimeException("Service failure"));

        utilities.allBiometricHaveException(rid, registrationType, stageName);
    }

    @Test(expected = BiometricClassificationException.class)
    public void testGetBiometricRecordfromIdrepo_noDocuments_returnsNull() throws ApisResourceAccessException, IOException {
        String uin = "66554444";
        String rid = "10049100271000420240319064824";
        when(utilities.retrieveIdrepoDocument(uin)).thenReturn(Collections.emptyList());
        utilities.getBiometricRecordfromIdrepo(uin, rid);
    }

    @Test
    public void testGetBiometricRecordfromIdrepo_Success() throws Exception {
        String uin = "1234567890";
        String rid = "10049100271000420230319064824";

        PowerMockito.mockStatic(RegProcessorLogger.class);
        when(RegProcessorLogger.getLogger(any())).thenReturn(regProcLogger);

        Documents doc = new Documents();
        doc.setCategory("IndividualBiometrics");
        doc.setValue("YmFzZTY0RW5jb2RlZEJpb0RhdGE=");
        List<Documents> docs = Arrays.asList(doc);
        doReturn(docs).when(utilities).retrieveIdrepoDocument(uin);

        PowerMockito.mockStatic(CryptoUtil.class);
        byte[] decoded = "decodedXML".getBytes();
        when(CryptoUtil.decodeURLSafeBase64(anyString())).thenReturn(decoded);
        PowerMockito.mockStatic(CbeffValidator.class);
        BIR bir = mock(BIR.class);
        when(bir.getBirs()).thenReturn(Collections.emptyList());
        HashMap<String, String> othersMap = new HashMap<>();
        othersMap.put("key1", "value1");
        when(bir.getOthers()).thenReturn(othersMap);
        when(CbeffValidator.getBIRFromXML(any(byte[].class))).thenReturn(bir);

        BiometricRecord result = utilities.getBiometricRecordfromIdrepo(uin, rid);

        assertNotNull(result);
        assertEquals("value1", result.getOthers().get("key1"));
        assertTrue(result.getSegments().isEmpty());
    }

    @Test
    public void testisALLBiometricHaveExceptionWithCbeffWithOthersAllBioException() throws JAXBException, IOException {
        String rid = "10049100271000420250319064824";
        String pathString= "CbeffWithOthersAllBioException.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res =  utilities.allBiometricHaveException(bir.getBirs(), rid);
        assertTrue(res);
    }

    @Test
    public void testisALLBiometricHaveExceptionWithCbeffWithOthersNoBioException() throws JAXBException, IOException {
        String rid = "10049100271000420250319064824";
        String pathString= "CbeffWithOthersNoBioException.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res =  utilities.allBiometricHaveException(bir.getBirs(), rid);
        assertFalse(res);
    }

    @Test
    public void testisALLBiometricHaveExceptionWithCbeffWithoutOthersAllBioException() throws JAXBException, IOException {
        String rid = "10049100271000420250319064824";
        String pathString= "CbeffWithoutOthersAllBioException.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res =  utilities.allBiometricHaveException(bir.getBirs(), rid);
        assertTrue(res);
    }

    @Test
    public void testisALLBiometricHaveExceptionWithCbeffWithoutOthersNoBioException() throws JAXBException, IOException {
        String rid = "10049100271000420250319064824";
        String pathString= "CbeffWithoutOthersNoBioException.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res =  utilities.allBiometricHaveException(bir.getBirs(), rid);
        assertFalse(res);
    }

    @Test
    public void testisALLBiometricHaveExceptionWithCbeffWithOthersSingleBioException() throws ParserConfigurationException, SAXException, IOException, JAXBException {
        String rid = "10049100271000420250319064824";
        String pathString = "CbeffWithOthersSingleBioException.xml";

        ClassPathResource resource = new ClassPathResource(pathString);

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(false);

        XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(resource.getInputStream());
        SAXSource saxSource = new SAXSource(xmlReader, inputSource);

        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        BIR bir = (BIR) unmarshaller.unmarshal(saxSource);

        Boolean res = utilities.allBiometricHaveException(bir.getBirs(), rid);
        assertFalse(res);
    }

    @Test
    public void testisALLBiometricHaveExceptionWithCbeffWithoutOthersSingleBioException() throws ParserConfigurationException, SAXException, IOException, JAXBException {
        String rid = "10049100271000420250319064824";
        String pathString = "CbeffWithoutOthersSingleBioException.xml";

        ClassPathResource resource = new ClassPathResource(pathString);

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(false);

        XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();
        InputSource inputSource = new InputSource(resource.getInputStream());
        SAXSource saxSource = new SAXSource(xmlReader, inputSource);

        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        BIR bir = (BIR) unmarshaller.unmarshal(saxSource);

        Boolean res = utilities.allBiometricHaveException(bir.getBirs(), rid);
        assertFalse(res);
    }
}
