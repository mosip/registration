package io.mosip.registration.processor.core.anonymous.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.Entry;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.anonymous.dto.AnonymousProfileDTO;
import io.mosip.registration.processor.core.anonymous.dto.BiometricInfoDTO;
import io.mosip.registration.processor.core.anonymous.dto.ExceptionsDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.packet.dto.Document;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.util.JsonUtil;

@Component
public class AnonymousProfileService {

	@Autowired
	ObjectMapper mapper;

	/**
	 * The mandatory languages that should be used when dealing with field type that
	 * has values in multiple languages
	 */
	@Value("#{T(java.util.Arrays).asList('${mosip.mandatory-languages:}')}")
	private List<String> mandatoryLanguages;

	/** The constant for language label in JSON parsing */
	private static final String LANGUAGE_LABEL = "language";

	/** The constant for value label in JSON parsing */
	private static final String VALUE_LABEL = "value";

	/** The Constant TRUE. */
	private static final String TRUE = "true";

	private static final String ANONYMOUS_PROFILE_DATETIME_PATTERN = "yyyy-MM-dd";

	public String buildJsonStringFromPacketInfo(BiometricRecord biometricRecord, Map<String, String> fieldMap,
			Map<String, String> metaInfoMap, String statusCode, String processStage)
			throws JSONException, ApisResourceAccessException, PacketManagerException, IOException {

		AnonymousProfileDTO anonymousProfileDTO = new AnonymousProfileDTO();
		anonymousProfileDTO.setProcessName(
				getFieldValueFromMetaInfo(metaInfoMap, JsonConstant.METADATA, JsonConstant.REGISTRATIONTYPE));
		anonymousProfileDTO.setProcessStage(processStage);
		anonymousProfileDTO.setStatus(statusCode);
		String date = metaInfoMap.get(JsonConstant.CREATIONDATE);
		anonymousProfileDTO.setDate(DateUtils.getTimeFromDate(
				DateUtils.parseToDate(date, ANONYMOUS_PROFILE_DATETIME_PATTERN), ANONYMOUS_PROFILE_DATETIME_PATTERN));

		String dateArr[] = fieldMap.get("dateOfBirth").split("/");
		anonymousProfileDTO.setYearOfBirth(dateArr[0]);
		anonymousProfileDTO.setGender(getLanguageBasedValueForSimpleType(fieldMap.get(MappingJsonConstants.GENDER)));

		String zone = getLanguageBasedValueForSimpleType(fieldMap.get("zone"));
		String postalCode = fieldMap.get("postalCode");
		anonymousProfileDTO.setLocation(Arrays.asList(zone, postalCode));
		anonymousProfileDTO.setPreferredLanguages(Arrays.asList(fieldMap.get(MappingJsonConstants.PREFERRED_LANGUAGE)));
		List<String> channels = new ArrayList<String>();
		if (fieldMap.containsKey(MappingJsonConstants.PHONE) && fieldMap.get(MappingJsonConstants.PHONE) != null) {
			channels.add(MappingJsonConstants.PHONE);
		}
		if (fieldMap.containsKey(MappingJsonConstants.EMAIL) && fieldMap.get(MappingJsonConstants.EMAIL) != null) {
			channels.add(MappingJsonConstants.EMAIL);
		}
		anonymousProfileDTO.setChannel(channels);
		anonymousProfileDTO.setDocuments(getDocumentsDataFromMetaInfo(metaInfoMap));
		anonymousProfileDTO.setEnrollmentCenterId(
				getFieldValueFromMetaInfo(metaInfoMap, JsonConstant.METADATA, JsonConstant.CENTERID));
		anonymousProfileDTO.setAssisted(Arrays
				.asList(getFieldValueFromMetaInfo(metaInfoMap, JsonConstant.OPERATIONSDATA, JsonConstant.OFFICERID)));
		getExceptionAndBiometricInfo(biometricRecord, anonymousProfileDTO);
		return JsonUtil.objectMapperObjectToJson(anonymousProfileDTO);
	}

	private String getLanguageBasedValueForSimpleType(String fieldValue) throws JSONException {
		JSONArray jsonArray = new JSONArray(fieldValue);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (jsonObject.getString(LANGUAGE_LABEL).equals(mandatoryLanguages.get(0))) {
				if (jsonObject.isNull(VALUE_LABEL))
					return null;
				return jsonObject.getString(VALUE_LABEL);
			}
		}
		return null;
	}

	private List<String> getDocumentsDataFromMetaInfo(Map<String, String> metaInfoMap)
			throws ApisResourceAccessException, PacketManagerException, IOException, JSONException {
		String metadata = metaInfoMap.get(MappingJsonConstants.DOCUMENT);
		List<String> documentTypes = new ArrayList<String>();
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
					Document document = mapper.readValue(jsonObject.toString(), Document.class);
					if (document.getDocumentType() != null) {
						documentTypes.add(document.getDocumentType());
					}
				}
			}
		}
		return documentTypes;
	}

	private String getFieldValueFromMetaInfo(Map<String, String> metaInfoMap, String field, String label)
			throws ApisResourceAccessException, PacketManagerException, IOException, JSONException {
		String metadata = metaInfoMap.get(field);
		String value = null;
		if (StringUtils.isNotEmpty(metadata)) {
			JSONArray jsonArray = new JSONArray(metadata);

			for (int i = 0; i < jsonArray.length(); i++) {
				if (!jsonArray.isNull(i)) {
					org.json.JSONObject jsonObject = (org.json.JSONObject) jsonArray.get(i);
					FieldValue fieldValue = mapper.readValue(jsonObject.toString(), FieldValue.class);
					if (fieldValue.getLabel().equalsIgnoreCase(label)) {
						value = fieldValue.getValue();
						break;
					}
				}
			}
		}
		return value;
	}

	private void getExceptionAndBiometricInfo(BiometricRecord biometricRecord, AnonymousProfileDTO anonymousProfileDTO)
			throws JSONException {
		List<BiometricInfoDTO> biometrics = new ArrayList<BiometricInfoDTO>();
		List<ExceptionsDTO> exceptions = new ArrayList<ExceptionsDTO>();

		List<BIR> birs = biometricRecord.getSegments();
		for (BIR bir : birs) {
			List<Entry> othersInfo = bir.getOthers();

			if (othersInfo == null) {
				continue;
			}

			String retries = null;
			String digitalID = null;
			boolean exceptionValue = false;
			for (Entry other : othersInfo) {
				if (other.getKey().equals(JsonConstant.BIOMETRICRECORDEXCEPTION) && other.getValue().equals(TRUE)) {
					exceptionValue = true;
				}
				if (other.getKey().equals(JsonConstant.RETRY_COUNT)) {
					retries = other.getValue();
				}
				if (other.getKey().equals(JsonConstant.PAYLOAD)) {
					JSONObject jsonObject = new JSONObject(other.getValue());
					digitalID = jsonObject.getString(JsonConstant.DIGITALID);
				}
			}

			if (exceptionValue) {
				ExceptionsDTO exceptionsDTO = new ExceptionsDTO();
				exceptionsDTO.setType(bir.getBdbInfo().getType().get(0).name());
				exceptionsDTO.setSubType(bir.getBdbInfo().getSubtype().get(0));
				exceptions.add(exceptionsDTO);
			} else {
				BiometricInfoDTO biometricInfoDTO = new BiometricInfoDTO();
				biometricInfoDTO.setType(bir.getBdbInfo().getType().get(0).name());
				if (!bir.getBdbInfo().getSubtype().isEmpty()) {
					biometricInfoDTO.setSubType(String.join(" ", bir.getBdbInfo().getSubtype()));
				}
				biometricInfoDTO.setQualityScore(bir.getBdbInfo().getQuality().getScore());
				biometricInfoDTO.setAttempts(retries);
				biometricInfoDTO.setDigitalId(digitalID);
				biometrics.add(biometricInfoDTO);
			}

		}
		anonymousProfileDTO.setBiometricInfo(biometrics);
		anonymousProfileDTO.setExceptions(exceptions);
	}

}
