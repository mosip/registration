package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.utility.PacketClassifierUtility;

/**
 * The Class IDObjectDataAvailabilityTagGeneratorTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class IDObjectDataAvailabilityTagGeneratorTest {

	private static String VALUE = "value";

	@InjectMocks
	private IDObjectDataAvailabilityTagGenerator idObjectDataAvailabilityTagGenerator;

	@Mock
	private Utilities utility;
	
	@Mock
    private PacketClassifierUtility classifierUtility;

	private static String notAvailableTagValue = "--TAG_VALUE_NOT_AVAILABLE--";

	private static Map<String, String> availabilityExpressionMap;
	private static Map<String, String> actualFieldNamesMap;
	private static Map<String, FieldDTO> idObjectFieldDTOMap;
	private static Map<String, String> tagValueMap;

	@Before
	public void setup() throws Exception {

		availabilityExpressionMap = new HashMap<>();
		availabilityExpressionMap.put("AVAILABILITY_CHECK_1", "IDSchemaVersion || dob || gender || individualBiometrics || poa");
		availabilityExpressionMap.put("AVAILABILITY_CHECK_2", "IDSchemaVersion && dob && gender && individualBiometrics && poa");
		availabilityExpressionMap.put("AVAILABILITY_CHECK_3", "IDSchemaVersion");
		availabilityExpressionMap.put("AVAILABILITY_CHECK_4", "dob");
		availabilityExpressionMap.put("AVAILABILITY_CHECK_5", "gender");
		availabilityExpressionMap.put("AVAILABILITY_CHECK_6", "individualBiometrics");
		availabilityExpressionMap.put("AVAILABILITY_CHECK_7", "poa");
		
		idObjectFieldDTOMap = new HashMap<>();
		idObjectFieldDTOMap.put("IDSchemaVersion", new FieldDTO("number", ""));
		idObjectFieldDTOMap.put("dateOfBirth", new FieldDTO("string", "1995/01/19"));
		idObjectFieldDTOMap.put("gender", new FieldDTO("simpleType", "[ {\n  \"language\" : \"eng\",\n  \"value\" : null\n} ]"));
		idObjectFieldDTOMap.put("individualBiometrics", new FieldDTO("biometricsType", "{\n  \"format\" : \"cbeff\",\n  \"version\" : 1.0,\n  \"value\" : \"individualBiometrics_bio_CBEFF\"\n}"));
		idObjectFieldDTOMap.put("proofOfAddress", new FieldDTO("documentType", "{\n  \"value\" : \"proofOfAddress\",\n  \"type\" : \"RNC\",\n  \"format\" : \"PDF\"\n}"));
		
		tagValueMap = new HashMap<>();
		tagValueMap.put("AVAILABILITY_CHECK_1", "true");
		tagValueMap.put("AVAILABILITY_CHECK_2", "false");
		tagValueMap.put("AVAILABILITY_CHECK_3", "false");
		tagValueMap.put("AVAILABILITY_CHECK_4", "true");
		tagValueMap.put("AVAILABILITY_CHECK_5", "false");
		tagValueMap.put("AVAILABILITY_CHECK_6", "true");
		tagValueMap.put("AVAILABILITY_CHECK_7", "true");


		Whitebox.setInternalState(idObjectDataAvailabilityTagGenerator, "availabilityExpressionMap", availabilityExpressionMap);
		Whitebox.setInternalState(idObjectDataAvailabilityTagGenerator, "notAvailableTagValue", notAvailableTagValue);

		actualFieldNamesMap = new HashMap<>();
		actualFieldNamesMap.put("IDSchemaVersion", "IDSchemaVersion");
		actualFieldNamesMap.put("dob", "dateOfBirth");
		actualFieldNamesMap.put("gender", "gender");
		actualFieldNamesMap.put("poa", "proofOfAddress");
		actualFieldNamesMap.put("individualBiometrics", "individualBiometrics");

		JSONObject mappingJSON = new JSONObject();
		for(Map.Entry<String, String> entry : actualFieldNamesMap.entrySet()) {
			String mappingFieldName = entry.getKey();
			LinkedHashMap<String,String> internalJSON = new LinkedHashMap<>();
			internalJSON.put(VALUE, entry.getValue());
			mappingJSON.put(mappingFieldName, internalJSON);
		}
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(mappingJSON);
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesForSuccess() throws BaseCheckedException {
		List<String> requiredIdObjectFieldNames = idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
		assertTrue(requiredIdObjectFieldNames.containsAll(actualFieldNamesMap.values()));
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesForEmptyMapInAvailabilityExpressionMap() throws BaseCheckedException {
		Whitebox.setInternalState(idObjectDataAvailabilityTagGenerator, "availabilityExpressionMap", new HashMap<>());
		List<String> requiredIdObjectFieldNames = idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(requiredIdObjectFieldNames.size(), 0);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetRequiredIdObjectFieldNamesForFieldNotAvailableInMappingJSON() throws BaseCheckedException {
		Map<String, String> expressionMap = new HashMap<String, String>();
		expressionMap.put("AVAILABILITY_CHECK_1", availabilityExpressionMap.get("AVAILABILITY_CHECK_1"));
		expressionMap.put("AVAILABILITY_CHECK_100", "address1 && gender");
		Whitebox.setInternalState(idObjectDataAvailabilityTagGenerator, "availabilityExpressionMap", expressionMap);
		idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetRequiredIdObjectFieldNamesForUtilityThrowingIOException() throws BaseCheckedException,
			IOException {
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString())).thenThrow(new IOException());
		idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
	}

	@Test
	public void testGenerateTagsForAllFieldTypesAndDifferntExpressions() throws BaseCheckedException {
		idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
		Map<String, String> tags = idObjectDataAvailabilityTagGenerator.generateTags("12345", "1234", "NEW",
			idObjectFieldDTOMap, null, 0);
		for(Map.Entry<String, String> entry : tagValueMap.entrySet()) {
			assertEquals(entry.getValue(), tags.get(entry.getKey()));
		}
	}

	@Test
	public void testGenerateTagsForFieldNotAvailableInFieldDTOMap() throws BaseCheckedException, JSONException {
		idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
		idObjectFieldDTOMap.remove("dateOfBirth");
		idObjectFieldDTOMap.remove("individualBiometrics");
		idObjectFieldDTOMap.remove("proofOfAddress");
		Mockito.when(classifierUtility.getLanguageBasedValueForSimpleType(anyString())).thenReturn(null);
		Map<String, String> tags = idObjectDataAvailabilityTagGenerator.generateTags("12345", "1234", "NEW", idObjectFieldDTOMap, null, 0);
		for(Map.Entry<String, String> entry : tagValueMap.entrySet()) {
			assertEquals("false", tags.get(entry.getKey()));
		}
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForUnknownFieldType() throws BaseCheckedException {
		idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
		FieldDTO fieldDTO = idObjectFieldDTOMap.get("IDSchemaVersion");
		fieldDTO.setType("notavailabletype");
		idObjectDataAvailabilityTagGenerator.generateTags("12345", "1234", "NEW", idObjectFieldDTOMap, null, 0);
	}
	@Test(expected = ParsingException.class)
	public void testGetRequiredIdObjectFieldNamesForUtilityThrowingIParsingException() throws BaseCheckedException,
			IOException {
		idObjectDataAvailabilityTagGenerator.getRequiredIdObjectFieldNames();
		idObjectFieldDTOMap.remove("dateOfBirth");
		idObjectFieldDTOMap.remove("individualBiometrics");
		idObjectFieldDTOMap.put("proofOfAddress", new FieldDTO("documentType", "{\n  \"value\" : \"proofOfAddress\"\n  \"type\" : \"RNC\",\n  \"format\" : \"PDF\"\n}"));

		Map<String, String> tags = idObjectDataAvailabilityTagGenerator.generateTags("12345", "1234", "NEW", idObjectFieldDTOMap, null, 0);
		
	}
}
