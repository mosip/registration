package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

/**
 * 
 * @author M1048399 Horteppa
 *
 */
public class MandatoryValidation {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(MandatoryValidation.class);

	/** The adapter. */
	private PacketReaderService packetReaderService;

	private Utilities utility;

	/** The registration status dto. */
	private InternalRegistrationStatusDto registrationStatusDto;

	private String source;

	public static final String FILE_SEPARATOR = "\\";

	public MandatoryValidation(PacketReaderService packetReaderService, InternalRegistrationStatusDto registrationStatusDto,
			Utilities utility,String source) {
		this.packetReaderService = packetReaderService;
		this.registrationStatusDto = registrationStatusDto;
		this.utility = utility;
		this.source=source;
	}

	public boolean mandatoryFieldValidation(String regId) throws IOException, JSONException,
			PacketDecryptionFailureException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MandatoryValidation::mandatoryFieldValidation()::entry");
		JSONObject mapperIdentity = utility.getRegistrationProcessorIdentityJson();
		JSONObject idJsonObj = getDemoIdentity(regId);
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
				registrationStatusDto.setStatusComment(StatusMessage.MANDATORY_FIELD_MISSING);
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

	private boolean checkEmptyString(Object obj) throws JSONException {
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

	private JSONObject getDemoIdentity(String registrationId) throws IOException, PacketDecryptionFailureException,
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, ApiNotAccessibleException {
		InputStream documentInfoStream = packetReaderService.getFile(registrationId, PacketFiles.ID.name(),source);

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
