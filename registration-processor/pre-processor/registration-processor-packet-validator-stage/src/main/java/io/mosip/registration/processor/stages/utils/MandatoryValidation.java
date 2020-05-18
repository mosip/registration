package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 
 * @author M1048399 Horteppa
 *
 */
@Component
public class MandatoryValidation {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(MandatoryValidation.class);

	/** The PacketReaderService. */
	@Autowired
	private PacketReaderService packetReaderService;

	@Autowired
	private Utilities utility;

	public static final String FILE_SEPARATOR = "\\";

	public boolean mandatoryFieldValidation(String regId, String source, PacketValidationDto packetValidationDto) throws IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MandatoryValidation::mandatoryFieldValidation()::entry");
		JSONObject mapperIdentity = utility.getRegistrationProcessorMappingJson();
		JSONObject idJsonObj = getDemoIdentity(regId, source);
		List<String> list = new ArrayList<>();
		for (Object key : mapperIdentity.keySet()) {
			LinkedHashMap<String, Object> jsonObject = JsonUtil.getJSONValue(mapperIdentity, (String) key);
			String values = (String) jsonObject.get("value");
			Boolean isMandatory = (Boolean) jsonObject.get("isMandatory");
			for (String value : values.split(",")) {
				if (isMandatory != null && isMandatory == Boolean.TRUE)
					list.add(value);
			}

		}

		for (String keyLabel : list) {
			if (JsonUtil.getJSONValue(idJsonObj, keyLabel) == null
					|| checkEmptyString(JsonUtil.getJSONValue(idJsonObj, keyLabel))) {
				packetValidationDto.setPacketValidaionFailureMessage(StatusMessage.MANDATORY_FIELD_MISSING);
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), regId,
						PlatformErrorMessages.RPR_PVM_MANDATORY_FIELD_MISSING.getCode(),
						PlatformErrorMessages.RPR_PVM_MANDATORY_FIELD_MISSING.getMessage() + keyLabel);
				return false;
			}
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MandatoryValidation::mandatoryFieldValidation()::exit");
		return true;
	}

	private boolean checkEmptyString(Object obj) {
		ArrayList<HashMap> objArray;
		if (obj instanceof String)
			return ((String) obj).trim().isEmpty() ? true : false;
		if (obj instanceof ArrayList) {
			objArray = (ArrayList<HashMap>) obj;
			for (int i = 0; i < objArray.size(); i++) {
				Map jObj = objArray.get(i);
				return jObj.get("value") == null || jObj.get("language") == null;
			}
		}

		return false;
	}

	private JSONObject getDemoIdentity(String registrationId, String source) throws IOException, PacketDecryptionFailureException,
			io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		InputStream documentInfoStream = packetReaderService.getFile(registrationId, PacketFiles.ID.name(), source);

		byte[] bytes = IOUtils.toByteArray(documentInfoStream);
		String demographicJsonString = new String(bytes);
		JSONObject demographicJson = (JSONObject) JsonUtil.objectMapperReadValue(demographicJsonString,
				JSONObject.class);
		JSONObject idJsonObj = JsonUtil.getJSONObject(demographicJson, "identity");
		if (idJsonObj == null)
			throw new IdentityNotFoundException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
		return idJsonObj;
	}

}
