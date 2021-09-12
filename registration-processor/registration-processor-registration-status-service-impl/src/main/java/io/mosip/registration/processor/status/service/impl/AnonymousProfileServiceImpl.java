package io.mosip.registration.processor.status.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.Entry;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.anonymous.dto.AnonymousProfileDTO;
import io.mosip.registration.processor.core.anonymous.dto.BiometricInfoDTO;
import io.mosip.registration.processor.core.anonymous.dto.ExceptionsDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Document;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.status.entity.AnonymousProfileEntity;
import io.mosip.registration.processor.status.entity.AnonymousProfilePKEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
import io.mosip.registration.processor.status.utilities.RegistrationUtility;
@Component
public class AnonymousProfileServiceImpl implements AnonymousProfileService {
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(AnonymousProfileServiceImpl.class);
	/** The Anonymus Profile repository. */
	@Autowired
	private BaseRegProcRepository<AnonymousProfileEntity, String> anonymousProfileRepository;
	
	@Autowired
	ObjectMapper mapper;
	
	@Autowired 
	RestTemplate restTemplate;
	
	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;
	
	/** The get reg processor identity json. */
	@Value("${registration.processor.identityjson}")
	private String getRegProcessorIdentityJson;
	
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

	@Override
	public void saveAnonymousProfile(String regId, String processStage, String profileJson) {
		
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
					"AnonymousProfileServiceImpl::saveAnonymousProfile()::entry");
			try {
			AnonymousProfileEntity anonymousProfileEntity=new AnonymousProfileEntity();
			AnonymousProfilePKEntity anonymousProfilePKEntity=new AnonymousProfilePKEntity();
			anonymousProfilePKEntity.setId(RegistrationUtility.generateId());
			anonymousProfileEntity.setId(anonymousProfilePKEntity);
			anonymousProfileEntity.setProfile(profileJson);
			anonymousProfileEntity.setProcessStage(processStage);
			anonymousProfileEntity.setCreatedBy("SYSTEM");
			anonymousProfileEntity.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			anonymousProfileEntity.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			anonymousProfileEntity.setIsDeleted(false);
			
			anonymousProfileRepository.save(anonymousProfileEntity);
			} catch (DataAccessLayerException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						"", e.getMessage() + ExceptionUtils.getStackTrace(e));

				throw new TablenotAccessibleException(
						PlatformErrorMessages.RPR_RGS_ANONYMOUS_PROFILE_TABLE_NOT_ACCESSIBLE.getMessage(), e);
			} 
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), regId,
					"AnonymousProfileServiceImpl::saveAnonymousProfile()::exit");

	}

	@Override
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

		Date dob = DateUtils.parseToDate(fieldMap.get("dateOfBirth"), "yyyy/MM/dd");
		anonymousProfileDTO.setYearOfBirth(DateUtils.parseDateToLocalDateTime(dob).getYear());
		anonymousProfileDTO.setGender(getLanguageBasedValueForSimpleType(fieldMap.get(MappingJsonConstants.GENDER)));

		anonymousProfileDTO.setPreferredLanguages(Arrays.asList(fieldMap.get(MappingJsonConstants.PREFERRED_LANGUAGE)));
		List<String> channels = new ArrayList<String>();

		String mappingJsonString = restTemplate.getForObject(configServerFileStorageURL + getRegProcessorIdentityJson,
				String.class);
		org.json.simple.JSONObject mappingJsonObject = mapper.readValue(mappingJsonString,
				org.json.simple.JSONObject.class);
		org.json.simple.JSONObject regProcessorIdentityJson = JsonUtil.getJSONObject(mappingJsonObject,
				MappingJsonConstants.IDENTITY);

		channels.add(JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL),
				MappingJsonConstants.VALUE));
		channels.add(JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE),
				MappingJsonConstants.VALUE));
		anonymousProfileDTO.setChannel(channels);
		
		String location = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.ADDRESS),
				MappingJsonConstants.VALUE);
		List<String> locationList = Arrays.asList(location.split("\\s*,\\s*"));
		anonymousProfileDTO.setLocation(
				Arrays.asList(locationList.get(locationList.size() - 2), locationList.get(locationList.size() - 1)));
		
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
