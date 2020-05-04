package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.dao.impl.DynamicFieldDAOImpl;
import io.mosip.registration.dto.mastersync.DynamicFieldValueDto;
import io.mosip.registration.entity.DynamicField;
import io.mosip.registration.repositories.DynamicFieldRepository;
import io.mosip.registration.util.mastersync.MapperUtils;

public class DynamicFieldDaoImplTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private DynamicFieldDAOImpl dynamicFieldDaoImpl;
	
	@Mock
	private DynamicFieldRepository dynamicFieldRepository;
	
	private List<DynamicFieldValueDto> fieldValues;
	
	private DynamicField dynamicField;
	
	private String valueJson = "[{\r\n" + 
			"	\"code\": \"TT0\",\r\n" + 
			"	\"value\": \"test\",\r\n" + 
			"	\"active\": true\r\n" + 
			"}, {\r\n" + 
			"\r\n" + 
			"	\"code\": \"TT1\",\r\n" + 
			"	\"value\": \"test - test\",\r\n" + 
			"	\"active\": true\r\n" + 
			"}]";
	
	@Before
	public void beforeTestStarts() throws IOException {
		
		dynamicField = new DynamicField();
		dynamicField.setActive(true);
		dynamicField.setDataType("dataType");
		dynamicField.setDescription("description");
		dynamicField.setId("id");
		dynamicField.setLangCode("langCode");
		dynamicField.setName("fieldName");
		dynamicField.setValueJson(valueJson);
		
		fieldValues = MapperUtils.convertJSONStringToDto(dynamicField.getValueJson(), new TypeReference<List<DynamicFieldValueDto>>() {});
		
	}
	
	@Test
	public void testForDynamicField() throws IOException {	
		Mockito.when(dynamicFieldRepository.findByNameAndLangCode("fieldName", "langCode")).thenReturn(dynamicField);		
		assertEquals(dynamicField, dynamicFieldDaoImpl.getDynamicField("fieldName", "langCode"));		
	}
	
	@Test
	public void testDynamicFieldValues() throws IOException {	
		dynamicField.setValueJson(valueJson);
		Mockito.when(dynamicFieldRepository.findByNameAndLangCode("fieldName", "langCode")).thenReturn(dynamicField);	
		assertEquals(fieldValues, dynamicFieldDaoImpl.getDynamicFieldValues("fieldName", "langCode"));
	}
	
	@Test
	public void testDynamicFieldValuesEmptyArray() {		
		dynamicField.setValueJson("[]");
		Mockito.when(dynamicFieldRepository.findByNameAndLangCode("fieldName", "langCode")).thenReturn(dynamicField);
		
		List<DynamicFieldValueDto> list = dynamicFieldDaoImpl.getDynamicFieldValues("fieldName", "langCode");
		assertNotNull(list);
		assertEquals(0, list.size());				
	}	
	
	@Test()
	public void testDynamicFieldValuesWithNull() {
		dynamicField.setValueJson(null);
		Mockito.when(dynamicFieldRepository.findByNameAndLangCode("fieldName", "langCode")).thenReturn(dynamicField);
		
		List<DynamicFieldValueDto> list = dynamicFieldDaoImpl.getDynamicFieldValues("fieldName", "langCode");
		assertNotNull(list);
		assertEquals(0, list.size());
	}
	
	@Test
	public void testDynamicFieldValuesWithInvalidJson() {
		dynamicField.setValueJson("{}");
		Mockito.when(dynamicFieldRepository.findByNameAndLangCode("fieldName", "langCode")).thenReturn(dynamicField);
		
		List<DynamicFieldValueDto> list = dynamicFieldDaoImpl.getDynamicFieldValues("fieldName", "langCode");
		assertEquals(null, list);
	}
	
}
