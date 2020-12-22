package io.mosip.registration.service.template.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.TemplateDao;
import io.mosip.registration.entity.Template;
import io.mosip.registration.entity.TemplateFileFormat;
import io.mosip.registration.entity.TemplateType;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.template.TemplateService;

/**
 * Implementation class for {@link TemplateService}
 *
 * @author Himaja Dhanyamraju
 *
 */
@Service
public class TemplateServiceImpl implements TemplateService {

	private static final Logger LOGGER = AppConfig.getLogger(TemplateServiceImpl.class);

	@Autowired
	private TemplateDao templateDao;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.service.template.TemplateService#getHtmlTemplate(java.
	 * lang.String, java.lang.String)
	 */
	public String getHtmlTemplate(String templateTypeCode, String langCode) throws RegBaseCheckedException {
		LOGGER.debug("REGISTRATION - TEMPLATE_GENERATION - TEMPLATE_SERVICE_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Getting required template from DB started");

		StringBuilder templateBuilder = new StringBuilder();
		if (nullCheckForTemplate(templateTypeCode, langCode)) {
			List<Template> templateParts = templateDao.getAllTemplates(templateTypeCode+"%", langCode);
			if(templateParts != null) {
				templateParts.forEach(template -> {
					templateBuilder.append(template.getFileText());
				});
				return templateBuilder.toString();
			}
		}
		throw new RegBaseCheckedException(RegistrationExceptionConstants.TEMPLATE_CHECK_EXCEPTION.getErrorCode(),
				RegistrationExceptionConstants.TEMPLATE_CHECK_EXCEPTION.getErrorMessage());
	}

	private boolean nullCheckForTemplate(String templateTypeCode, String langCode) {
		if (StringUtils.isEmpty(templateTypeCode)) {
			LOGGER.error("REGISTRATION - TEMPLATE_GENERATION - TEMPLATE_SERVICE_IMPL", APPLICATION_NAME, APPLICATION_ID,
					"Template Type Code is empty or null");
			return false;
		} else if (StringUtils.isEmpty(langCode)) {
			LOGGER.error("REGISTRATION - TEMPLATE_GENERATION - TEMPLATE_SERVICE_IMPL", APPLICATION_NAME, APPLICATION_ID,
					"Lang Code is empty or null");
			return false;
		} else {
			return true;
		}
	}
}