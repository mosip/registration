package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

/**
 * The Class PacketValidatorStageTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*", 
	"javax.xml.*", "org.xml.*" })
public class IDObjectFieldsTagGeneratorTest {

	private static String VALUE = "value";

	@InjectMocks
	private IDObjectFieldsTagGenerator idObjectFieldsTagGenerator;

	@Mock
	private Utilities utility;

	private static String tagNamePrefix = "ID_OBJECT-";
	private static String tagLanguage= "eng";
	private static String notAvailableTagValue = "--TAG_VALUE_NOT_AVAILABLE--";

	private static List<String> mappingFieldNames;
	private static List<String> actualFieldNames;
	private static Map<String, FieldDTO> idObjectFieldDTOMap;
	private static List<String> tagValues;

	@Before
	public void setup() throws Exception {

		mappingFieldNames = new ArrayList<String>();
		mappingFieldNames.add("IDSchemaVersion");
		mappingFieldNames.add("dob");
		mappingFieldNames.add("gender");
	
		actualFieldNames = new ArrayList<>();
		actualFieldNames.add("IDSchemaVersion");
		actualFieldNames.add("dateOfBirth");
		actualFieldNames.add("gender");
		
		idObjectFieldDTOMap = new HashMap<>();
		idObjectFieldDTOMap.put(actualFieldNames.get(0), new FieldDTO("number", "0.5"));
		idObjectFieldDTOMap.put(actualFieldNames.get(1), new FieldDTO("string", "1995/01/19"));
		idObjectFieldDTOMap.put(actualFieldNames.get(2), new FieldDTO("simpleType", "[ {\n  \"language\" : \"eng\",\n  \"value\" : \"MALE\"\n} ]"));

		tagValues = new ArrayList<>();
		tagValues.add("0.5");
		tagValues.add("1995/01/19");
		tagValues.add("MALE");

		Whitebox.setInternalState(idObjectFieldsTagGenerator, "tagNamePrefix", tagNamePrefix);
		Whitebox.setInternalState(idObjectFieldsTagGenerator, "mappingFieldNames", mappingFieldNames);
		Whitebox.setInternalState(idObjectFieldsTagGenerator, "tagLanguage", tagLanguage);
		Whitebox.setInternalState(idObjectFieldsTagGenerator, "notAvailableTagValue", notAvailableTagValue);

		JSONObject mappingJSON = new JSONObject();
		for( int i=0; i< mappingFieldNames.size(); i++) {
			String mappingFieldName = mappingFieldNames.get(i);
			LinkedHashMap<String,String> internalJSON = new LinkedHashMap<>();
			internalJSON.put(VALUE, actualFieldNames.get(i));
			mappingJSON.put(mappingFieldName, internalJSON);
		}
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString())).thenReturn(mappingJSON);
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesForSuccess() throws BaseCheckedException {
		List<String> requiredIdObjectFieldNames = idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
		assertTrue(requiredIdObjectFieldNames.containsAll(actualFieldNames));
	}

	@Test
	public void testGetRequiredIdObjectFieldNamesForEmptyArrayInMappingFieldNames() throws BaseCheckedException {
		Whitebox.setInternalState(idObjectFieldsTagGenerator, "mappingFieldNames", new ArrayList<>());
		List<String> requiredIdObjectFieldNames = idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
		assertEquals(requiredIdObjectFieldNames.size(), 0);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetRequiredIdObjectFieldNamesForFieldNotAvailableInMappingJSON() throws BaseCheckedException {
		List<String> fieldNames = new ArrayList<String>();
		fieldNames.add(mappingFieldNames.get(0));
		fieldNames.add("name");
		Whitebox.setInternalState(idObjectFieldsTagGenerator, "mappingFieldNames", fieldNames);
		idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
	}

	@Test(expected = BaseCheckedException.class)
	public void testGetRequiredIdObjectFieldNamesForUtilityThrowingIOException() throws BaseCheckedException,
			IOException {
		Mockito.when(utility.getRegistrationProcessorMappingJson(anyString())).thenThrow(new IOException());
		idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
	}

	@Test
	public void testGenerateTagsForAllFieldTypes() throws BaseCheckedException {
		idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
		Map<String, String> tags = idObjectFieldsTagGenerator.generateTags("1234", "NEW", 
			idObjectFieldDTOMap, null);
		for(int i=0; i < actualFieldNames.size(); i++) {
			String actualFieldName = actualFieldNames.get(i);
			assertEquals(tags.get(tagNamePrefix + actualFieldName), tagValues.get(i));
		}
	}

	public void testGenerateTagsForFieldNotAvailableInFieldDTOMap() throws BaseCheckedException {
		idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
		idObjectFieldDTOMap.remove(actualFieldNames.get(0));
		Map<String, String> tags = idObjectFieldsTagGenerator.generateTags("1234", "NEW", idObjectFieldDTOMap, null);
		assertEquals(tags.get(tagNamePrefix + actualFieldNames.get(0)), notAvailableTagValue);
	}

	@Test(expected = BaseCheckedException.class)
	public void testGenerateTagsForUnknownFieldType() throws BaseCheckedException {
		idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
		FieldDTO fieldDTO = idObjectFieldDTOMap.get(actualFieldNames.get(0));
		fieldDTO.setType("notavailabletype");
		idObjectFieldsTagGenerator.generateTags("1234", "NEW", idObjectFieldDTOMap, null);
	}

	@Test(expected = ParsingException.class)
	public void testGenerateTagsForInvalidJSONStringInFieldValue() throws BaseCheckedException {
		idObjectFieldsTagGenerator.getRequiredIdObjectFieldNames();
		idObjectFieldDTOMap.put(actualFieldNames.get(2), 
			new FieldDTO("simpleType", "[ {\n  \"language\" : \"eng\",\n  \"value\" : \"MALE\"\n} "));
		idObjectFieldsTagGenerator.generateTags("1234", "NEW", idObjectFieldDTOMap, null);
	}
	
}
