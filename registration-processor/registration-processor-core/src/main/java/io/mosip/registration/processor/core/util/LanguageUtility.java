package io.mosip.registration.processor.core.util;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.kernel.master.dto.LanguageDto;
import io.mosip.registration.processor.core.kernel.master.dto.LanguageResponseDto;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.exception.LanguagesUtilException;

@Component
public class LanguageUtility {
	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;
	
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(LanguageUtility.class);

	@Autowired
	ObjectMapper mapper;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getLangCodeFromNativeName(String nativeName) {
		String langCode=null;
		try {
			ResponseWrapper<LanguageResponseDto> response = (ResponseWrapper) registrationProcessorRestService.getApi(ApiName.LANGUAGE,null, "", "", ResponseWrapper.class);

			if (response.getErrors() != null && response.getErrors().size() > 0) {
				response.getErrors().stream().forEach(r -> {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
							"LanguageUtility::getLangCodeFromNativeName():: error with error message " + r.getMessage());
				});
			}

			LanguageResponseDto languageResponseDto = mapper.readValue(mapper.writeValueAsString(response.getResponse()), LanguageResponseDto.class);
			
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"LanguageUtility::getLangCodeFromNativeName()::exit");
			for(LanguageDto dto:languageResponseDto.getLanguages()) {
				if(dto.getNativeName().equalsIgnoreCase(nativeName) || dto.getCode().equalsIgnoreCase(nativeName)
						|| dto.getName().equalsIgnoreCase(nativeName)) {
					langCode= dto.getCode();
				}
			}
			
		} catch (ApisResourceAccessException | IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
					"LanguageUtility::getLangCodeFromNativeName():: error with error message " + e.getMessage());
			throw new LanguagesUtilException(e.getMessage(), e);
		}
		return langCode;
		
	}
}
