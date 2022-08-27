package io.mosip.registration.processor.stages.validator.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.commons.BiometricsSignatureHelper;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BiometricSignatureValidationException;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.JWTSignatureVerifyRequestDto;
import io.mosip.registration.processor.core.packet.dto.JWTSignatureVerifyResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;

@Component
public class BiometricsSignatureValidator {

	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static Logger regProcLogger = RegProcessorLogger.getLogger(BiometricsSignatureValidator.class);

	/** The Constant TRUE. */
	private static final String TRUE = "true";

	@Autowired
	private RegistrationProcessorRestClientService<Object> registrationProcessorRestService;

	@Autowired
	private Environment env;

	@Autowired
	ObjectMapper mapper;

	@Value("#{T(java.util.Arrays).asList('${mosip.regproc.common.before-cbeff-others-attibute.reg-client-versions:}')}")
	private List<String> regClientVersionsBeforeCbeffOthersAttritube;

	public void validateSignature(String id, String process, BiometricRecord biometricRecord,
			Map<String, String> metaInfoMap) throws JSONException, BiometricSignatureValidationException,
			ApisResourceAccessException, PacketManagerException, IOException, io.mosip.kernel.core.util.exception.JsonProcessingException {

		// backward compatibility check
		String version = getRegClientVersionFromMetaInfo(id, process, metaInfoMap);
		if (regClientVersionsBeforeCbeffOthersAttritube.contains(version)) {
			return;
		}

		List<BIR> birs = biometricRecord.getSegments();
		for (BIR bir : birs) {
			HashMap<String, String> othersInfo = bir.getOthers();
			if (othersInfo == null) {
				throw new BiometricSignatureValidationException("Others value is null inside BIR");
			}

			boolean exceptionValue = false;
			for (Map.Entry other : othersInfo.entrySet()) {
				if (other.getKey().equals(JsonConstant.BIOMETRICRECORDEXCEPTION)) {
					if (other.getValue().equals(TRUE)) {
						exceptionValue = true;
					}
					break;
				}
			}

			if (exceptionValue) {
				continue;
			}

			String token = BiometricsSignatureHelper.extractJWTToken(bir);
			validateJWTToken(id, token);
		}

	}

	private String getRegClientVersionFromMetaInfo(String id, String process, Map<String, String> metaInfoMap)
			throws ApisResourceAccessException, PacketManagerException, IOException, JSONException {
		String metadata = metaInfoMap.get(JsonConstant.METADATA);
		String version = null;
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
					FieldValue fieldValue = mapper.readValue(jsonObject.toString(), FieldValue.class);
					if (fieldValue.getLabel().equalsIgnoreCase(JsonConstant.REGCLIENTVERSION)) {
						version = fieldValue.getValue();
						break;
					}
				}
			}
		}
		return version;
	}

	private void validateJWTToken(String id, String token)
			throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException,
			BiometricSignatureValidationException, ApisResourceAccessException, io.mosip.kernel.core.util.exception.JsonProcessingException {
		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
		jwtSignatureVerifyRequestDto.setApplicationId("REGISTRATION");
		jwtSignatureVerifyRequestDto.setReferenceId("SIGN");
		jwtSignatureVerifyRequestDto.setJwtSignatureData(token);
		jwtSignatureVerifyRequestDto.setActualData(token.split("\\.")[1]);

		// in packet validator stage we are checking only the structural part of the
		// packet so setting validTrust to false
		jwtSignatureVerifyRequestDto.setValidateTrust(false);
		jwtSignatureVerifyRequestDto.setDomain("Device");
		RequestWrapper<JWTSignatureVerifyRequestDto> request = new RequestWrapper<>();

		request.setRequest(jwtSignatureVerifyRequestDto);
		request.setVersion("1.0");
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		request.setRequesttime(localdatetime);

		ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) registrationProcessorRestService
				.postApi(ApiName.JWTVERIFY, "", "", request, ResponseWrapper.class);
		if (responseWrapper.getResponse() != null) {
			JWTSignatureVerifyResponseDto jwtResponse = mapper.readValue(
					mapper.writeValueAsString(responseWrapper.getResponse()), JWTSignatureVerifyResponseDto.class);

			if (!jwtResponse.isSignatureValid()) {
				regProcLogger.error(LoggerFileConstant.REGISTRATIONID.toString(), id,
						"Request -> " + JsonUtils.javaObjectToJsonString(request)
						," Response -> " + JsonUtils.javaObjectToJsonString(responseWrapper));
				throw new BiometricSignatureValidationException(
						StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getCode(),
						StatusUtil.BIOMETRICS_SIGNATURE_VALIDATION_FAILURE.getMessage());
			}
		} else {
			throw new BiometricSignatureValidationException(responseWrapper.getErrors().get(0).getErrorCode(),
					responseWrapper.getErrors().get(0).getMessage());
		}

	}

}
