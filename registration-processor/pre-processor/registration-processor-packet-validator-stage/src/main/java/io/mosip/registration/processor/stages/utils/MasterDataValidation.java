package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.packet.dto.masterdata.StatusResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * The Class MasterDataValidation.
 * 
 * @author Nagalakshmi
 * 
 */

public class MasterDataValidation {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(MasterDataValidation.class);

	/** The registration processor rest service. */
	@Autowired
	RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	/** The env. */
	@Autowired
	Environment env;

	/** The Constant VALID. */
	private static final String VALID = "Valid";

	/** The utility. */
	@Autowired
	private Utilities utility;

	@Autowired
	private PacketManagerService packetManagerService;

	@Autowired
	private ObjectMapper objectMapper;

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	private static final String PRIMARY_LANGUAGE = "mosip.primary-language";

	private static final String SECONDARY_LANGUAGE = "mosip.secondary-language";

	private static final String ATTRIBUTES = "registration.processor.masterdata.validation.attributes";

	

	/**
	 * Validate master data.
	 *
	 * @param id
	 *            id
	 * @param source : source
	 * @param process : process
	 * @return the boolean
	 * @throws ApisResourceAccessException
	 */
	public boolean validateMasterData(String id, String source, String process) throws ApisResourceAccessException, IOException, io.mosip.kernel.core.util.exception.JsonProcessingException, PacketManagerException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MasterDataValidation::validateMasterData()::entry");
		boolean isValid = false;
		String primaryLanguage = env.getProperty(PRIMARY_LANGUAGE);
		String secondaryLanguage = env.getProperty(SECONDARY_LANGUAGE);
		String[] attributes = env.getProperty(ATTRIBUTES).split(",");
		if (attributes == null || attributes.length == 0)
			return true;

		List<String> list = new ArrayList<>(Arrays.asList(attributes));

		for (String element : list) {
			if (env.getProperty(ApiName.valueOf(element.toUpperCase()).name()) != null) {
				String primaryLangValue = null;
				String secondaryLangValue = null;
				String val = packetManagerService.getField(id, element, utility.getSourceFromIdField(MappingJsonConstants.IDENTITY, process, element), process);
				if (val != null) {
					Object object = objectMapper.readValue(val, Object.class);
					if (object instanceof ArrayList) {
						JSONArray node = objectMapper.readValue(val, JSONArray.class);
						JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
						primaryLangValue = getParameter(jsonValues, primaryLanguage);
						secondaryLangValue = getParameter(jsonValues, secondaryLanguage);
					} else if (object instanceof LinkedHashMap) {
						JSONObject json = objectMapper.readValue(val, JSONObject.class);
						primaryLangValue = (String) json.get(VALUE);
					} else
						primaryLangValue = (String) object;

					isValid = validateIdentityValues(element, primaryLangValue) && validateIdentityValues(element, secondaryLangValue);
					if (!isValid) {
						regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), "",
								PlatformErrorMessages.RPR_PVM_IDENTITY_INVALID.getMessage() + " " + element
										+ "and for values are" + primaryLangValue + " " + secondaryLangValue);

						break;
					}
				} else
					isValid = true;

			} else {
				isValid = false;
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						PlatformErrorMessages.RPR_PVM_RESOURCE_NOT_FOUND.getMessage() + " " + element);
				break;
			}
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MasterDataValidation::validateMasterData::exit");
		return isValid;

	}

	/**
	 * Validate identity values.
	 *
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @return true, if successful
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@SuppressWarnings("unchecked")
	private boolean validateIdentityValues(String key, String value) throws JsonParseException, JsonMappingException,
			JsonProcessingException, IOException, ApisResourceAccessException {
		StatusResponseDto statusResponseDto;
		ObjectMapper mapper = new ObjectMapper();
		boolean isvalidateIdentity = false;
		if (value != null) {
			try {

				List<String> pathsegmentsEng = new ArrayList<>();

				pathsegmentsEng.add(value);

				ResponseWrapper<StatusResponseDto> responseWrapper = (ResponseWrapper<StatusResponseDto>) registrationProcessorRestService
						.getApi(ApiName.valueOf(key.toUpperCase()), pathsegmentsEng, "", "", ResponseWrapper.class);
				statusResponseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
						StatusResponseDto.class);
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						"MasterDataValidation::validateIdentityValues():: MasterData Api call  ended with response data : "
								+ JsonUtil.objectMapperObjectToJson(statusResponseDto));
				if (statusResponseDto.getStatus().equalsIgnoreCase(VALID))
					isvalidateIdentity = true;
			} catch (ApisResourceAccessException ex) {
				if (ex.getCause() instanceof HttpClientErrorException) {
					isvalidateIdentity = false;
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "",
							PlatformErrorMessages.RPR_PVM_API_RESOUCE_ACCESS_FAILED.getMessage() + ex.getMessage());

				} else {
					throw ex;
				}
			}
		} else {
			isvalidateIdentity = true;
		}
		return isvalidateIdentity;

	}

	/**
	 * Gets the parameter.
	 *
	 * @param jsonValues
	 *            the json values
	 * @param langCode
	 *            the lang code
	 * @return the parameter
	 */
	private String getParameter(JsonValue[] jsonValues, String langCode) {

		String parameter = null;
		if (jsonValues != null) {
			for (int count = 0; count < jsonValues.length; count++) {
				String lang = jsonValues[count].getLanguage();
				if (langCode.contains(lang)) {
					parameter = jsonValues[count].getValue();
					break;
				}
			}
		}
		return parameter;
	}

}
