package io.mosip.registration.dao;

import java.util.List;

import io.mosip.registration.dto.mastersync.DynamicFieldValueJsonDto;
import io.mosip.registration.entity.DynamicField;

public interface DynamicFieldDAO {

	DynamicField getDynamicField(String fieldName, String Langcode);
	
	List<DynamicFieldValueJsonDto> getValueJSON(String fieldName, String Langcode);
}
