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

		LOGGER.info("DynamicFieldDAOImpl", APPLICATION_NAME, APPLICATION_ID,
				"fetching the dynamic field");

		return dynamicFieldRepository.findByNameAndLangCode(fieldName, langCode);
	}

	@Override
	public List<DynamicFieldValueDto> getDynamicFieldValues(String fieldName, String langCode) {
		
		LOGGER.info("DynamicFieldDAOImpl", APPLICATION_NAME, APPLICATION_ID,
				"fetching the valueJSON ");		
		
		DynamicField dynamicField = getDynamicField(fieldName, langCode);
		
		try {			
			return MapperUtils.convertJSONStringToDto(dynamicField.getValueJson() == null ? "[]" : dynamicField.getValueJson(), 
					new TypeReference<List<DynamicFieldValueDto>>() {});			
			
		} catch (IOException e) {
			LOGGER.error("Unable to parse value json for dynamic field: " + fieldName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

}
