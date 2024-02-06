package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;

/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class ExceptionBiometricsTagGeneratorTest {

	private static String tagName = "EXCEPTION_BIOMETRICS";

	private static String notAvailableTagValue = "--TAG_VALUE_NOT_AVAILABLE--";

	@InjectMocks
	private ExceptionBiometricsTagGenerator exceptionBiometricsTagGenerator;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	Map<String, String> metamap = new HashMap<>();

	@Before
	public void setup() throws Exception {
		Whitebox.setInternalState(exceptionBiometricsTagGenerator, "tagName", tagName);
		Map<String, String> bioValueMapping = new LinkedHashMap<>();
		bioValueMapping.put("leftLittle", "LL");
		bioValueMapping.put("leftRing", "LR");
		bioValueMapping.put("leftMiddle", "LM");
		bioValueMapping.put("leftIndex", "LI");
		bioValueMapping.put("leftThumb", "LT");
		bioValueMapping.put("rightLittle", "RL");
		bioValueMapping.put("rightRing", "RR");
		bioValueMapping.put("rightMiddle", "RM");
		bioValueMapping.put("rightIndex", "RI");
		bioValueMapping.put("rightThumb", "RT");
		bioValueMapping.put("leftEye", "LE");
		bioValueMapping.put("rightEye", "RE");
		Whitebox.setInternalState(exceptionBiometricsTagGenerator, "bioValueMapping", bioValueMapping);
		Whitebox.setInternalState(exceptionBiometricsTagGenerator, "notAvailableTagValue", 
			notAvailableTagValue);
		List<String> regClientVersionsBeforeExceptionbiometrics = new ArrayList();
		regClientVersionsBeforeExceptionbiometrics.add("1.1.3");
		regClientVersionsBeforeExceptionbiometrics.add("1.1.4");
		Whitebox.setInternalState(exceptionBiometricsTagGenerator, "regClientVersionsBeforeExceptionMetaInfoChange",
				regClientVersionsBeforeExceptionbiometrics);

	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailable()
			throws BaseCheckedException, JsonMappingException, JsonProcessingException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, 
				"{\"individualBiometrics\" : {\"leftEye\" : {\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}}}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.2.0.1\"\n} ]");
		Map<String, String> tags = 
			exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals("LE", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailableForAllModalites() 
			throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, 
				"{\"individualBiometrics\" : {\n    \"leftEye\" : {\n      \"type\" : \"Iris\",\n      \"missingBiometric\" : \"leftEye\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightEye\" : {\n      \"type\" : \"Iris\",\n      \"missingBiometric\" : \"rightEye\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightIndex\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightIndex\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightLittle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightLittle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightRing\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightRing\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightMiddle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightMiddle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftIndex\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftIndex\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftLittle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftLittle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftRing\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftRing\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftMiddle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftMiddle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftThumb\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftThumb\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightThumb\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightThumb\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    }\n  }\n}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.2.0.1\"\n} ]");
		Map<String, String> tags = 
			exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals("LL,LR,LM,LI,LT,RL,RR,RM,RI,RT,LE,RE", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsNotAvailable() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.2.0.1\"\n} ]");
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, "{\"individualBiometrics\" : {}}");
		Map<String, String> tags = 
			exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals("", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsApplicantNotAvailable() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.2.0.1\"\n} ]");
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, "{}");
		Map<String, String> tags = 
			exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}

	@Test(expected = ParsingException.class)
	public void testGenerateTagsForMetaInfoMapExceptionBiometricsContainingInvalidJSON() 
		throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, 
			"\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}]");
		exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
	}

	@Test
	public void testGenerateTagsForMetaInfoMapDoesNotContainExceptionBiometrics() 
		throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		Map<String, String> tags = 
			exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}
	
	@Test
	public void getRequiredIdObjectFieldNamesTest() throws Exception {
		List<String> result = exceptionBiometricsTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(result, null);
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailableWithBCTest()
			throws BaseCheckedException, JsonMappingException, JsonProcessingException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS,
				"{\"applicant\" : {\"leftEye\" : {\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}}}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.1.4\"\n} ]");

		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals("LE", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailableWithoutversion()
			throws BaseCheckedException, JsonMappingException, JsonProcessingException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS,
				"{\"applicant\" : {\"leftEye\" : {\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}}}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n} ]");
		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals("LE", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsNotAvailableWithBCTest()
			throws BaseCheckedException, JsonMappingException, JsonProcessingException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, "{\"applicant\" : {}}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.1.4\"\n} ]");
		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals("", tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsApplicantNotAvailableWithBCTest()
			throws BaseCheckedException, JsonMappingException, JsonProcessingException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, "{}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.1.4\"\n} ]");
		Map<String, String> tags = 
			exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null, metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailableWithLatestversionBCTest()
			throws BaseCheckedException, JsonMappingException, JsonProcessingException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS,
				"{\"applicant\" : {\"leftEye\" : {\"type\" : \"iris\", \"missingBiometric\" : \"leftEye\",\"reason\" : \"Missing biometrics\",\"exceptionType\" : \"Permanent\",\"individualType\" : \"INDIVIDUAL\"}}}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.2.0.1\"\n} ]");

		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAndVersionBothNotAvailabel() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n} ]");
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS, "{}");
		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailableWithoutVersion() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS,
				"{\"individualBiometrics\" : {\n    \"leftEye\" : {\n      \"type\" : \"Iris\",\n      \"missingBiometric\" : \"leftEye\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightEye\" : {\n      \"type\" : \"Iris\",\n      \"missingBiometric\" : \"rightEye\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightIndex\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightIndex\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightLittle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightLittle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightRing\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightRing\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightMiddle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightMiddle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftIndex\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftIndex\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftLittle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftLittle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftRing\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftRing\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftMiddle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftMiddle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftThumb\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftThumb\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightThumb\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightThumb\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    }\n  }\n}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n}]");
		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}

	@Test
	public void testGenerateTagsForExceptionBiometricsAvailableWithOldVersion() throws BaseCheckedException {
		Map<String, String> metaInfoMap = new HashMap<>();
		metaInfoMap.put(JsonConstant.EXCEPTIONBIOMETRICS,
				"{\"individualBiometrics\" : {\n    \"leftEye\" : {\n      \"type\" : \"Iris\",\n      \"missingBiometric\" : \"leftEye\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightEye\" : {\n      \"type\" : \"Iris\",\n      \"missingBiometric\" : \"rightEye\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightIndex\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightIndex\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightLittle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightLittle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightRing\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightRing\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightMiddle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightMiddle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftIndex\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftIndex\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftLittle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftLittle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftRing\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftRing\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftMiddle\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftMiddle\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"leftThumb\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"leftThumb\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    },\n    \"rightThumb\" : {\n      \"type\" : \"Finger\",\n      \"missingBiometric\" : \"rightThumb\",\n      \"reason\" : \"Temporary\",\n      \"exceptionType\" : \"Temporary\",\n      \"individualType\" : \"applicant\"\n    }\n  }\n}");
		metaInfoMap.put(JsonConstant.METADATA,
				"[ {\n  \"label\" : \"registrationId\",\n  \"value\" : \"1234\"\n},{\n  \"label\" : \"Registration Client Version Number\",\n  \"value\" : \"1.1.4\"\n} ]");
		Map<String, String> tags = exceptionBiometricsTagGenerator.generateTags("12345", "1234", "NEW", null,
				metaInfoMap, 0);
		assertEquals(notAvailableTagValue, tags.get(tagName));
	}
}
