package io.mosip.registration.processor.stages.utils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 
 * @author M1048399 Horteppa
 *
 */
@Component
public class MandatoryValidation {

	private static Logger regProcLogger = RegProcessorLogger.getLogger(MandatoryValidation.class);

	@Autowired
	private Utilities utility;

	@Autowired
	private PacketManagerService packetManagerService;

	public static final String FILE_SEPARATOR = "\\";

	public boolean mandatoryFieldValidation(String regId, String source, String process, PacketValidationDto packetValidationDto)
			throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MandatoryValidation::mandatoryFieldValidation()::entry");
		JSONObject mapperIdentity = utility.getRegistrationProcessorMappingJson();
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
			if (packetManagerService.getField(regId, keyLabel, source, process) == null) {
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

}
