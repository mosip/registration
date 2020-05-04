package io.mosip.registration.dao;

import java.util.List;

import io.mosip.registration.dto.mastersync.DynamicFieldValueDto;
import io.mosip.registration.entity.DynamicField;

public interface DynamicFieldDAO {

	DynamicField getDynamicField(String fieldName, String Langcode);
	
	List<DynamicFieldValueDto> getDynamicFieldValues(String fieldName, String Langcode);
}
