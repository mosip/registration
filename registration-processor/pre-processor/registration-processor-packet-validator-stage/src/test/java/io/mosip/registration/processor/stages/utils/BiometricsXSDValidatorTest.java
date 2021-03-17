package io.mosip.registration.processor.stages.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.entities.BIRInfo.BIRInfoBuilder;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.entities.VersionType;
import io.mosip.kernel.core.cbeffutil.common.CbeffValidator;
import io.mosip.kernel.core.cbeffutil.exception.CbeffException;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ Utilities.class, JsonUtil.class, IOUtils.class,CbeffValidator.class ,URL.class,BiometricsXSDValidator.class})
public class BiometricsXSDValidatorTest {
	
	@InjectMocks
	private BiometricsXSDValidator biometricsXSDValidator = new BiometricsXSDValidator();
	BiometricRecord biometricRecord = new BiometricRecord();
	@Before
	public void setUp() throws Exception {
		
        ReflectionTestUtils.setField(biometricsXSDValidator, "configServerFileStorageURL", "http://localhost:51000/config/registration-processor/mz/master/");
        ReflectionTestUtils.setField(biometricsXSDValidator, "schemaFileName", "mosip-cbeff.xsd");
        ClassLoader classLoader = getClass().getClassLoader();
		File xsdFile = new File(classLoader.getResource("mosip-cbeff.xsd").getFile());
		InputStream xsd = new FileInputStream(xsdFile);
		URL u = PowerMockito.mock(URL.class);
        String url = "http://localhost:51000/config/registration-processor/mz/master/mosip-cbeff.xsd";
        PowerMockito.whenNew(URL.class).withArguments(url).thenReturn(u);
        PowerMockito.when(u.openStream()).thenReturn(xsd);
        List<io.mosip.kernel.biometrics.entities.BIR> birTypeList = new ArrayList<>();
        io.mosip.kernel.biometrics.entities.BIR birType1 = new BIR.BIRBuilder().build();
        io.mosip.kernel.biometrics.entities.BDBInfo bdbInfoType1 = new BDBInfo.BDBInfoBuilder().build();
        io.mosip.kernel.biometrics.entities.RegistryIDType registryIDType = new RegistryIDType("HMAC", "SHA-256");
        io.mosip.kernel.biometrics.constant.QualityType quality = new QualityType();
        quality.setAlgorithm(registryIDType);
        quality.setScore(100l);
        bdbInfoType1.setQuality(quality);
        BiometricType singleType1 = BiometricType.FINGER;
        List<BiometricType> singleTypeList1 = new ArrayList<>();
        singleTypeList1.add(singleType1);
        List<String> subtype1 = new ArrayList<>(Arrays.asList("Left", "Thumb"));
        bdbInfoType1.setSubtype(subtype1);
        bdbInfoType1.setType(singleTypeList1);
        bdbInfoType1.setLevel(ProcessedLevelType.RAW);
        bdbInfoType1.setPurpose(PurposeType.ENROLL);
        bdbInfoType1.setCreationDate(LocalDateTime.now());
        RegistryIDType format=new RegistryIDType();
        format.setOrganization("Mosip");
        format.setType("7");
        bdbInfoType1.setFormat(format);
        birType1.setBdbInfo(bdbInfoType1);
        birType1.setBdb("bdb".getBytes());
        VersionType versionType=new VersionType(1, 1);
        birType1.setVersion(versionType);
        birType1.setCbeffversion(versionType);
        BIRInfoBuilder birInfoBuilder=new BIRInfoBuilder();
        birInfoBuilder.withIntegrity(true);
        birType1.setBirInfo(new BIRInfo(birInfoBuilder));
        birTypeList.add(birType1);
        biometricRecord.setBirInfo(new BIRInfo(birInfoBuilder));
        biometricRecord.setCbeffversion(versionType);
        biometricRecord.setVersion(versionType);
        biometricRecord.setSegments(birTypeList);
        PowerMockito.mockStatic(CbeffValidator.class);
        PowerMockito.when(CbeffValidator.createXMLBytes(Mockito.any(), Mockito.any())).thenReturn("bdd".getBytes());
	}
	
	
	@Test
	public void testvalidateXSD() throws IOException, Exception {
		assertTrue(biometricsXSDValidator.validateXSD(biometricRecord));
	}
	
	@Test(expected=CbeffException.class)
	public void testvalidateXSDFailure() throws IOException, Exception {
		 PowerMockito.when(CbeffValidator.createXMLBytes(Mockito.any(), Mockito.any())).thenThrow(new CbeffException("XSD validation failed ."));			
		assertFalse(biometricsXSDValidator.validateXSD(biometricRecord));
	}
}
