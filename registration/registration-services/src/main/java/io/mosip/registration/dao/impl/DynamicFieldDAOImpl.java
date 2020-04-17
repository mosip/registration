package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.DynamicFieldDAO;
import io.mosip.registration.dto.mastersync.DynamicFieldValueJsonDto;
import io.mosip.registration.entity.DynamicField;
import io.mosip.registration.repositories.DynamicFieldRepository;

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

		return dynamicFieldRepository.findByIsActiveTrueAndNameAndLangCode(fieldName, langCode);
	}

	@Override
	public List<DynamicFieldValueJsonDto> getValueJSON(String fieldName, String langCode) {
		
		LOGGER.info("DynamicFieldDAOImpl", APPLICATION_NAME, APPLICATION_ID,
				"fetching the valueJSON ");
		
		DynamicField dynamicField = getDynamicField(fieldName, langCode);
		ObjectMapper mapper  = new ObjectMapper();
		try {
			@SuppressWarnings("unchecked")
			List<DynamicFieldValueJsonDto> listOfFieldValueJson = mapper.readValue(dynamicField.getValueJson(), List.class);
			if(listOfFieldValueJson==null)
				throw new Exception();
			return listOfFieldValueJson;
		} catch (Exception exception) {
			LOGGER.error("Unable to fetch detail of ValueJson of Dynamic field", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}

}
