package io.mosip.registration.processor.packet.storage.utils;


import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.packet.dto.RidDto;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private BasePacketRepository basePacketRepository;


    private InternalRegistrationStatusDto registrationStatusDto;
    private RidDto ridDto;
    private SimpleDateFormat sdf;

    @Before
    public void setUp() {

        registrationStatusDto = new InternalRegistrationStatusDto();
        registrationStatusDto.setRegistrationId("10049100271000420250319064824");
        registrationStatusDto.setRegistrationType("UPDATE");
        ridDto = new RidDto();
        ridDto.setRid("10049100271000420240319064824");
        ridDto.setUpd_dtimes("2024-01-01T12:00:00");
        sdf = new SimpleDateFormat("yyyy/MM/dd");

        // Set configuration values
        ReflectionTestUtils.setField(utilities, "dobFormat", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ReflectionTestUtils.setField(utilities, "expectedLifeSpan", 100);
        ReflectionTestUtils.setField(utilities, "bufferInMonthes", 1);
        ReflectionTestUtils.setField(utilities, "MinAgeLimit", 0);
        ReflectionTestUtils.setField(utilities, "MaxAgeLimit", 150);
        ReflectionTestUtils.setField(utilities, "ageLimit", "5");

    }


    @Test
    public void testWasApplicantInfant_Success_getFromIdrepo() throws Exception {
        // Setup
        String uin = "12345";
        String dob = "2023/01/01";
        String packetCreatedDate = "2025-04-30T07:04:49.681Z";
        Mockito.when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        response.put("packet_created_on", packetCreatedDate);
        //response.put("packet_created_on", packetCreatedDate);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        Mockito.when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);

        // Execute
        boolean result = utilities.wasApplicantInfant(registrationStatusDto);

        // Verify
        assertTrue(result);
    }

    @Test
    public void testWasApplicantInfant_getFromListTable() throws Exception {
        // Setup
        String uin = "12345";
        String dob = "2023/01/01";
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
            .thenReturn(new JSONObject())
            .thenReturn(identityJson);
        when(idRepoService.getRidByIndividualId(anyString())).thenReturn(ridDto);
        when(basePacketRepository.getPacketIdfromRegprcList(anyString())).thenReturn("10049100271000420250319064824");

        // Execute
        boolean result = utilities.wasApplicantInfant(registrationStatusDto);

        // Verify
        assertTrue(result);
        verify(packetManagerService, atLeastOnce()).getField(anyString(), anyString(), anyString(), any());
    }

    @Test
    public void testPacketCreatedDateTimeByRidFromIdRepo_Success() throws Exception {
        // Setup
        String uin = "12345";
        String dob = "2023/01/01";
        when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        response.put("packet_created_on", "2025-03-19T06:48:24.000Z");
        RidDto ridDto1=new RidDto();
        ridDto1.setRid("10049100271000420250319064824");
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        when(idRepoService.getRidByIndividualId(anyString())).thenReturn(ridDto1);
        when(basePacketRepository.getPacketIdfromRegprcList(any())).thenReturn(null);

        // Execute
        boolean result = utilities.wasApplicantInfant(registrationStatusDto);

        // Verify
        assertTrue(result);
    }

//    @Test
    public void testGetPacketUpdateDateAndTimesFromIdRepo_Success() throws Exception {
        // Setup
        String uin = "12345";
        String dob = "2023/01/01";
        when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        RidDto ridDto1=new RidDto();
        ridDto1.setUpd_dtimes("2025-04-30T07:04:49.681Z");
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any())).thenReturn(identityJson);
        when(idRepoService.getRidByIndividualId(anyString())).thenReturn(ridDto1);
        when(basePacketRepository.getPacketIdfromRegprcList(any())).thenReturn(null);

        // Execute
        boolean result = utilities.wasApplicantInfant(registrationStatusDto);

        // Verify
        assertTrue(result);
    }

    @Test
    public void testGetDateOfBirthFromIdRepo_Success() throws Exception {
        // Setup
        String uin = "12345";
        String dob = "2023/01/01";
        when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);

        // Execute
        String result = utilities.getDateOfBirthFromIdrepo(uin, registrationStatusDto.getRegistrationType());

        // Verify
        assertEquals("2023-01-01T00:00:00.000Z", result);
        verify(packetManagerService, times(1)).getField(anyString(), anyString(), anyString(), any());
    }

    @Test
    public void testParseDate_FromRidSuccess() throws Exception {
        // Setup
        String packetCreatedDate = "20250319064824";
        // Execute
        String result = utilities.parseDate(packetCreatedDate);
        // Verify
        assertEquals("2025-03-19T06:48:24.000Z", result);
    }

    @Test
    public void testParseDate_FromIdRepoSuccess() throws Exception {
        // Setup
        String packetCreatedDate = "2025-03-19T06:48:24.000Z";
        // Execute
        String result = utilities.parseDate(packetCreatedDate);
        // Verify
        assertEquals("2025-03-19T06:48:24.000Z", result);
    }

    @Test
    public void testIsValidDate_ValidDate() throws ParseException {
        // Setup
        Date date= Date.from(Instant.parse("2025-03-19T06:48:24.000Z"));
        // Execute
        boolean result = utilities.isValidDate(date);
        // Verify
        assertTrue(result);
    }

    @Test
    public void testIsValidDate_ValidDate_failed() throws ParseException {
        // Execute
        String date  = utilities.parseDate("2026-03-19T06:48:24.000Z");
        // Verify
        assertNull(date);

    }

    @Test
    public void testCalculateAgeAtTheTimeOfRegistration_Success() throws Exception {
        // Setup
        Date dob = sdf.parse("2020/01/01");
        Date registeredDate = utilities.convertToDate("2024-03-19T06:48:24.000Z");
        // Execute
        int age = utilities.calculateAgeAtTheTimeOfRegistration(dob, registeredDate);
        // Verify
        assertEquals(4, age);
    }

    @Test
    public void testGetResponseFromIdRepo_Success() throws Exception {
        // Setup
        String uin = "12345";
        when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        when(idRepoService.getRidByIndividualId(uin)).thenReturn(ridDto);

        // Execute
        RidDto result = utilities.getIndividualIdResponceFromIdrepo(registrationStatusDto.getRegistrationId(),registrationStatusDto.getRegistrationType());

        // Verify
        assertEquals(ridDto, result);
        verify(packetManagerService, times(1)).getField(anyString(), anyString(), anyString(), any());
    }

    @Test
    public void testGetPacketCreationDateTimeFromRegList_Success() throws Exception {
        // Setup
        RidDto ridDto1=new RidDto();
        ridDto1.setUpd_dtimes("2025-03-19T06:48:24.000Z");
        when(basePacketRepository.getPacketIdfromRegprcList(anyString())).thenReturn("10049100271000420250319064824");

        // Execute
        Date result = utilities.getPacketCreationDateTimeFromRegList(ridDto.getRid());

        // Verify
        verify(basePacketRepository, times(1)).getPacketIdfromRegprcList(anyString());
    }

    @Test(expected = IOException.class)
    public void testWasApplicantInfant_failure() throws Exception {
        // Setup
        String uin = "12345";
        String dob = "2023/01/01";
        String packetCreatedDate = "2025-04-30T07:04:49.681Z";
        Map<String, String> response = new HashMap<>();
        response.put("dateOfBirth", dob);
        RidDto ridDto1=new RidDto();
        ridDto1.setUpd_dtimes("");
        ridDto1.setRid("10049100271000420260319064824");
        when(idRepoService.getRidByIndividualId(anyString())).thenReturn(ridDto1);
        when(packetManagerService.getField(anyString(), anyString(), anyString(), any(ProviderStageName.class)))
                .thenReturn(uin);
        String jsonString = new ObjectMapper().writeValueAsString(response);
        JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
        when(idRepoService.getIdJsonFromIDRepo(anyString(), any()))
                .thenReturn(identityJson);
        when(basePacketRepository.getPacketIdfromRegprcList(anyString())).thenReturn(ridDto1.getRid());

        // Execute
        utilities.wasApplicantInfant(registrationStatusDto);

    }

    @Test
    public void testGetPacketCreatedDateFromPacketManager() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
        Map<String, String> metaInfo = new HashMap<>();
        String packetCreatedDate = "2025-05-28T10:53:13.973Z";
        metaInfo.put("creationDate",packetCreatedDate);
        when(packetManagerService.getMetaInfo(anyString(),anyString(),any(ProviderStageName.class))).thenReturn(metaInfo);
        String res = utilities.getPacketCreatedDateFromPacketManager("10049100271000420250319064824","NEW", ProviderStageName.UIN_GENERATOR);
        assertEquals(packetCreatedDate,res);
    }

    @Test
    public void TestisALLBiometricHaveExceptionWithOutOthersAllExceptionAsFalse() throws JAXBException, IOException, BiometricException, PacketManagerException, ApisResourceAccessException, JsonProcessingException {
        //without other tag and all of BDB exception is marked as false(No exception)
        String pathString= "BIRWithOutOther.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res=  utilities.allBiometricHaveException(bir.getBirs());
        assertFalse(res);
    }

    /**     with other tag and all of BDB exception is marked as true(All exception)**/
    @Test
    public void TestisALLBiometricHaveExceptionWithOthersSuccessMarkedAllExceptionAsFalse() throws JAXBException, IOException, BiometricException, PacketManagerException, ApisResourceAccessException, JsonProcessingException {
        String pathString= "BIRWithOther.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res=  utilities.allBiometricHaveException(bir.getBirs());
        assertFalse(res);
    }

    /** with other tag and all of BDB exception is marked as true(All exception) **/
    @Test
    public void TestisALLBiometricHaveExceptionWithOthersSuccessAllExceptionAsTrue() throws JAXBException, IOException, BiometricException, PacketManagerException, ApisResourceAccessException, JsonProcessingException {
        String pathString= "BIRWithOtherAllException.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res=  utilities.allBiometricHaveException(bir.getBirs());
        assertTrue(res);
    }

    @Test
    public void TestisALLBiometricHaveExceptionWithOutOthersSuccessAllExceptionAsTrue() throws JAXBException, IOException, BiometricException, PacketManagerException, ApisResourceAccessException, JsonProcessingException {
        String pathString= "BIRWithOutOtherAllException.xml";
        ClassPathResource resource1 = new ClassPathResource(pathString);
        JAXBContext jaxbContext = JAXBContext.newInstance(BIR.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        BIR bir = (BIR) unmarshaller.unmarshal(resource1.getFile());
        Boolean res=  utilities.allBiometricHaveException(bir.getBirs());
        assertTrue(res);
    }


}
