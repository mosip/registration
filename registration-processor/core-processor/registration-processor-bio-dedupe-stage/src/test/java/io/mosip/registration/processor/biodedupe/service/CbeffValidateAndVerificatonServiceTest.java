package io.mosip.registration.processor.biodedupe.service;


import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.biodedupe.stage.exception.CbeffNotFoundException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*" })
public class CbeffValidateAndVerificatonServiceTest {

    private static final String id = "id";
    private static final String process = "NEW";

    @InjectMocks
    private CbeffValidateAndVerificatonService service;

    @Mock
    private Utilities utilities;

    @Mock
    private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

    @Before
    public void setup() throws IOException, PacketManagerException, ApisResourceAccessException, JsonProcessingException {

        ReflectionTestUtils.setField(service, "policyId", "mpolicy-default-abis");
        ReflectionTestUtils.setField(service, "subscriberId", "mpartner-default-abis");

        String mandatory = "Right,Left,Left RingFinger,Left LittleFinger,"
                + "Right RingFinger,Left Thumb,Left IndexFinger,Right IndexFinger,Right LittleFinger,Right MiddleFinger,"
                + "Left MiddleFinger,Right Thumb,Face";
        ReflectionTestUtils.setField(service, "mandatoryModalities", Lists.newArrayList(mandatory.split(",")));

        JSONObject regProcessorIdentityJson = new JSONObject();
        LinkedHashMap bioIdentity = new LinkedHashMap<String, String>();
        bioIdentity.put("value", "biometrics");
        regProcessorIdentityJson.put("individualBiometrics", bioIdentity);

        Mockito.when(utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(regProcessorIdentityJson);

        List<BIR> birTypeList = new ArrayList<>();
        BIR birType1 = new BIR.BIRBuilder().build();
        BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
        io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new RegistryIDType();
        registryIDType.setOrganization("Mosip");
        registryIDType.setType("257");
        io.mosip.kernel.biometrics.constant.QualityType quality = new QualityType();
        quality.setAlgorithm(registryIDType);
        quality.setScore(90l);
        bdbInfoType1.setQuality(quality);
        BiometricType singleType1 = BiometricType.FINGER;
        List<BiometricType> singleTypeList1 = new ArrayList<>();
        singleTypeList1.add(singleType1);
        List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "RingFinger"));
        bdbInfoType1.setSubtype(subtype1);
        bdbInfoType1.setType(singleTypeList1);
        birType1.setBdbInfo(bdbInfoType1);
        birTypeList.add(birType1);
        HashMap<String, String> othersMap = new HashMap<>();
        othersMap.put("EXCEPTION", "true");
        birType1.setOthers(othersMap);

        BiometricRecord biometricRecord = new BiometricRecord();
        biometricRecord.setSegments(birTypeList);


        Mockito.when(priorityBasedPacketManagerService.getBiometrics(any(),any(),any(),any(),any())).thenReturn(biometricRecord);

    }

    @Test(expected = CbeffNotFoundException.class)
    public void validateBiometricsTest() throws PacketManagerException, ApisResourceAccessException, IOException, JsonProcessingException {
        service.validateBiometrics(id, process);
    }

}
