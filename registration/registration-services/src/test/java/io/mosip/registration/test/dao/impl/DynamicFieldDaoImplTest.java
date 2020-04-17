package io.mosip.registration.test.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.dao.impl.DynamicFieldDAOImpl;
import io.mosip.registration.dto.mastersync.DynamicFieldValueJsonDto;
import io.mosip.registration.entity.DynamicField;
import io.mosip.registration.repositories.DynamicFieldRepository;

public class DynamicFieldDaoImplTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private DynamicFieldDAOImpl dynamicFieldDaoImpl;
	
	@Mock
	private DynamicFieldRepository dynamicFieldRepository;
	
	private List<DynamicFieldValueJsonDto> listOfFieldValueJson;
	
	private DynamicField dynamicField;
	
	@Before
	public void beforeTestStarts() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper  = new ObjectMapper();
		dynamicField = new DynamicField();
		dynamicField.setActive(true);
		dynamicField.setDataType("dataType");
		dynamicField.setDescription("description");
		dynamicField.setId("id");
		dynamicField.setLangCode("langCode");
		dynamicField.setName("fieldName");
		dynamicField.setValueJson("[{\r\n" + 
				"	\"code\": \"TT0\",\r\n" + 
				"	\"value\": \"test\",\r\n" + 
				"	\"isActive\": true\r\n" + 
				"}, {\r\n" + 
				"\r\n" + 
				"	\"code\": \"TT1\",\r\n" + 
				"	\"value\": \"test - test\",\r\n" + 
				"	\"isActive\": true\r\n" + 
				"}]");
		listOfFieldValueJson = mapper.readValue(dynamicField.getValueJson(), List.class);
		
	}
	
	@Test
	public void testForDynamicField() throws JsonParseException, JsonMappingException, IOException {

	
		Mockito.when(dynamicFieldRepository.findByIsActiveTrueAndNameAndLangCode("fieldName", "langCode")).thenReturn(dynamicField);
		
		assertEquals(dynamicField, dynamicFieldDaoImpl.getDynamicField("fieldName", "langCode"));
		assertEquals(listOfFieldValueJson, dynamicFieldDaoImpl.getValueJSON("fieldName", "langCode"));
	}
	
	@Test
	public void testForException() {
		listOfFieldValueJson = null;
		assertEquals(null, dynamicFieldDaoImpl.getValueJSON("fieldName", "langCode"));
	}
	
}
