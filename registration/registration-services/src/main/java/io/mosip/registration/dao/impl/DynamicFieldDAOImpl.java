package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.DynamicFieldDAO;
import io.mosip.registration.dto.mastersync.DynamicFieldValueDto;
import io.mosip.registration.entity.DynamicField;
import io.mosip.registration.repositories.DynamicFieldRepository;
import io.mosip.registration.util.mastersync.MapperUtils;

@Repository
public class DynamicFieldDAOImpl implements DynamicFieldDAO {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DynamicFieldDAOImpl.class);

	
	@Autowired
	private DynamicFieldRepository dynamicFieldRepository;
	
	@Override
	public DynamicField getDynamicField(String fieldName, String langCode) {

		LOGGER.debug("DynamicFieldDAOImpl", APPLICATION_NAME, APPLICATION_ID,
				"fetching the dynamic field >>> " + fieldName + " for langCode >>> " + langCode);

		return dynamicFieldRepository.findByIsActiveTrueAndNameAndLangCode(fieldName, langCode);
	}

	@Override
	public List<DynamicFieldValueDto> getDynamicFieldValues(String fieldName, String langCode) {
		
		LOGGER.debug("DynamicFieldDAOImpl", APPLICATION_NAME, APPLICATION_ID,
				"fetching the valueJSON ");		
		
		DynamicField dynamicField = getDynamicField(fieldName, langCode);
		
		try {
			String valueJson = (dynamicField != null) ? dynamicField.getValueJson() : "[]" ;

			List<DynamicFieldValueDto> fields = MapperUtils.convertJSONStringToDto(valueJson == null ? "[]" : valueJson,
					new TypeReference<List<DynamicFieldValueDto>>() {});

			if(fields != null)
				fields.sort((DynamicFieldValueDto d1, DynamicFieldValueDto d2) -> d1.getCode().compareTo(d2.getCode()));

			return fields;
			
		} catch (IOException e) {
			LOGGER.error("Unable to parse value json for dynamic field: " + fieldName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

}
