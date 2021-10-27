package io.mosip.registration.processor.stages.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * The Class ApplicantDocumentValidationTest.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class,JsonUtil.class })
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class ApplicantDocumentValidationTest {

	@Mock
	Utilities utility;

	@InjectMocks
	private ApplicantDocumentValidation applicantDocumentValidation;

	JSONObject regProcessorIdentityJson=mock(JSONObject.class);
	JSONObject documentLabel=mock(JSONObject.class);
	String label="label";
	String source="source";
	JSONObject demographicIdentityJSONObject=mock(JSONObject.class);
	JSONObject proofOfDocument;

	@Mock
	private PriorityBasedPacketManagerService packetManagerService;

	@Before
	public void setUp()
			throws Exception {
		Map<String,String> map=new LinkedHashMap<>();
		map.put("value", "documentValue");
		proofOfDocument=new JSONObject(map);

		JSONObject identityJSON = new JSONObject();
		identityJSON.put("proofOfAddress", map);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT)).thenReturn(identityJSON);

		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(regProcessorIdentityJson);

		PowerMockito.mockStatic(JsonUtil.class);
		PowerMockito.when(JsonUtil.class, "getJSONObject", any(), any())
				.thenReturn(proofOfDocument);
		PowerMockito.when(JsonUtil.class, "getJSONValue", any(), anyString())
		.thenReturn(label);
		when(utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY)).thenReturn(demographicIdentityJSONObject);

		Document document = new Document();
		document.setType("POA");
		document.setValue("POA");
		document.setDocument("document".getBytes());

		when(packetManagerService.getDocument(anyString(),anyString(),anyString(), any())).thenReturn(document);

		Map<String, String> docFields = new HashMap<>();
		docFields.put("label", "value");
		when(packetManagerService.getFields(anyString(), anyList(), anyString(), any())).thenReturn(docFields);
		when(packetManagerService.getField(anyString(), any(), anyString(), any())).thenReturn(docFields.toString());

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

		BiometricRecord biometricRecord = new BiometricRecord();
		biometricRecord.setSegments(birTypeList);

		when(packetManagerService.getBiometrics(anyString(),any(), any(), any())).thenReturn(biometricRecord);
		when(packetManagerService.getBiometricsByMappingJsonKey(anyString(), anyString(), any(), any())).thenReturn(biometricRecord);
	}

	@Test
	public void testApplicantDocumentValidationSuccess() throws Exception {
		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234", "NEW");
		assertTrue("Test for successful Applicant Document Validation success for adult", isApplicantDocumentValidated);
	}

	@Test
	public void testApplicantDocumentValidationFailure() throws Exception {
		when(packetManagerService.getDocument(anyString(),anyString(),anyString(), any())).thenReturn(null);
		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234", "NEW");
		assertFalse(isApplicantDocumentValidated);
	}


}
