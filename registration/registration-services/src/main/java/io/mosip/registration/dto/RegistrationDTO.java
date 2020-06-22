package io.mosip.registration.dto;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.packetmanager.constants.Biometric;
import io.mosip.kernel.packetmanager.dto.AuditDto;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.kernel.packetmanager.dto.DocumentDto;
import io.mosip.kernel.packetmanager.dto.SimpleDto;
import io.mosip.kernel.packetmanager.dto.metadata.BiometricsException;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.biometric.BiometricDTO;
import lombok.Data;

/**
 * This DTO class contains the Registration details.
 * 
 * @author Dinesh Asokan
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Data
public class RegistrationDTO {

	protected ApplicationContext applicationContext = ApplicationContext.getInstance();
	private static final String DATE_FORMAT = "yyyy/MM/dd";

	private double idSchemaVersion;
	private String registrationId;
	private String preRegistrationId;
	private String registrationCategory;
	private int age;
	private boolean isChild;
	private boolean isBiometricMarkedForUpdate;

	private RegistrationMetaDataDTO registrationMetaDataDTO;
	private OSIDataDTO osiDataDTO;

	private HashMap<String, Object> selectionListDTO;
	private List<String> updatableFields;
	private List<String> updatableFieldGroups;
	private boolean isUpdateUINNonBiometric;
	private boolean isNameNotUpdated;
	private boolean isUpdateUINChild;
	private boolean isAgeCalculatedByDOB;

	private BiometricDTO biometricDTO = new BiometricDTO();
	private Map<String, Object> demographics = new HashMap<>();
	private Map<String, DocumentDto> documents = new HashMap<>();
	private Map<String, BiometricsDto> biometrics = new HashMap<>();
	private Map<String, BiometricsException> biometricExceptions = new HashMap<>();

	private List<BiometricsDto> supervisorBiometrics = new ArrayList<>();
	private List<BiometricsDto> officerBiometrics = new ArrayList<>();
	private Map<String, BiometricsException> osBioExceptions = new HashMap<>();

	private List<AuditDto> auditDTOs;
	private Timestamp auditLogStartTime;
	private Timestamp auditLogEndTime;

	/** The acknowledge receipt. */
	private byte[] acknowledgeReceipt;

	/** The acknowledge receipt name. */
	private String acknowledgeReceiptName;

	public void addDemographicField(String fieldId, String value) {
		this.demographics.put(fieldId, (value != null && !value.isEmpty()) ? value : null);
	}

	public void addDemographicField(String fieldId, String applicationLanguage, String value, String localLanguage,
			String localValue) {
		List<SimpleDto> values = new ArrayList<SimpleDto>();
		if (value != null && !value.isEmpty())
			values.add(new SimpleDto(applicationLanguage, value));

		if (localValue != null && !localValue.isEmpty())
			values.add(new SimpleDto(localLanguage, localValue));

		if (!values.isEmpty())
			this.demographics.put(fieldId, values);
	}

	public void removeDemographicField(String fieldId) {
		this.demographics.remove(fieldId);
	}

	public void setDateField(String fieldId, String day, String month, String year) {
		if (isValidValue(day) && isValidValue(month) && isValidValue(year)) {
			LocalDate date = LocalDate.of(Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(day));
			this.demographics.put(fieldId, date.format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
			this.age = Period.between(date, LocalDate.now(ZoneId.of("UTC"))).getYears();

			int minAge = Integer
					.parseInt((String) applicationContext.getApplicationMap().get(RegistrationConstants.MIN_AGE));
			int maxAge = Integer
					.parseInt((String) applicationContext.getApplicationMap().get(RegistrationConstants.MAX_AGE));
			this.isChild = this.age < minAge;
		}
	}
	
	public void setDateField(String fieldId, String dateString) {
		if (isValidValue(dateString)) {
			LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(DATE_FORMAT));
			setDateField(fieldId, String.valueOf(date.getDayOfMonth()), String.valueOf(date.getMonthValue()),
					String.valueOf(date.getYear()));
		}
	}

	public String[] getDateField(String fieldId) {
		if (this.demographics.containsKey(fieldId)) {
			String value = (String) this.demographics.get(fieldId);
			LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern(DATE_FORMAT));
			return new String[] { String.valueOf(date.getDayOfMonth()), String.valueOf(date.getMonthValue()),
					String.valueOf(date.getYear()) };
		}
		return null;
	}

	public void addDocument(String fieldId, DocumentDto value) {
		this.documents.put(fieldId, value);
	}

	public void removeDocument(String fieldId) {
		this.documents.remove(fieldId);
	}

	public void removeAllDocuments() {
		this.documents.clear();
	}

	public List<BiometricsDto> getBiometric(String subType, List<String> bioAttributes) {
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();
		for (String bioAttribute : bioAttributes) {
			String key = String.format("%s_%s", subType, bioAttribute);
			if (this.biometrics.containsKey(key))
				list.add(this.biometrics.get(key));
		}
		return list;
	}

	public BiometricsDto addBiometric(String subType, String bioAttribute, BiometricsDto value) {
		String key = String.format("%s_%s", subType, bioAttribute);
		int currentCount = 0;
		if (this.biometrics.get(key) != null) {
			currentCount = this.biometrics.get(key).getNumOfRetries();
		}
		value.setNumOfRetries(currentCount + 1);
		value.setSubType(subType);
		this.biometrics.put(key, value);
		this.biometricExceptions.remove(key);
		return value;
	}

	public void addBiometricException(String subType, String uiSchemaAttribute, String bioAttribute, String reason,
			String exceptionType) {
		String key = String.format("%s_%s", subType, uiSchemaAttribute);
		SingleType type = io.mosip.registration.mdm.dto.Biometric.getSingleTypeBySpecConstant(uiSchemaAttribute);
		this.biometricExceptions.put(key, new BiometricsException(type == null ? null : type.value(), bioAttribute,
				reason, exceptionType, subType));
		this.biometrics.remove(key);
	}

	public boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
		return biometricExceptions.containsKey(String.format("%s_%s", subType, bioAttribute));
	}

	public void removeBiometricException(String subType, String bioAttribute) {
		this.biometricExceptions.remove(String.format("%s_%s", subType, bioAttribute));
	}

	public BiometricsDto getBiometric(String subType, String bioAttribute) {
		String key = String.format("%s_%s", subType, bioAttribute);
		return this.biometrics.get(key);
	}

	public BiometricsDto removeBiometric(String subType, String bioAttribute) {
		String key = String.format("%s_%s", subType, bioAttribute);
		return this.biometrics.remove(key);
	}

	public void addSupervisorBiometrics(List<BiometricsDto> biometrics) {
		this.supervisorBiometrics.addAll(biometrics);
	}

	public void addOfficerBiometrics(List<BiometricsDto> biometrics) {
		this.officerBiometrics.addAll(biometrics);
	}

	/*
	 * public Map<String, Object> getIdentity() { Map<String, Object>
	 * allIdentityDetails = new LinkedHashMap<String, Object>();
	 * allIdentityDetails.put("IDSchemaVersion", idSchemaVersion);
	 * if(registrationMetaDataDTO.getUin() != null) allIdentityDetails.put("UIN",
	 * registrationMetaDataDTO.getUin());
	 * 
	 * allIdentityDetails.putAll(this.demographics);
	 * allIdentityDetails.putAll(this.documents);
	 * 
	 * if(biometricDTO.getApplicantBiometrics() != null)
	 * allIdentityDetails.put("applicantBiometrics",
	 * biometricDTO.getApplicantBiometrics());
	 * if(biometricDTO.getIntroducerBiometrics() != null)
	 * allIdentityDetails.put("introducerBiometrics",
	 * biometricDTO.getIntroducerBiometrics());
	 * 
	 * Map<String, Object> identity = new LinkedHashMap<String, Object>();
	 * identity.put("identity", allIdentityDetails); return identity; }
	 */

	public Map<String, Object> getMVELDataContext() {
		Map<String, Object> allIdentityDetails = new LinkedHashMap<String, Object>();
		allIdentityDetails.put("IDSchemaVersion", idSchemaVersion);
		allIdentityDetails.put("isNew", RegistrationConstants.PACKET_TYPE_NEW.equals(this.registrationCategory));
		allIdentityDetails.put("isUpdate", RegistrationConstants.PACKET_TYPE_UPDATE.equals(this.registrationCategory));
		allIdentityDetails.put("isLost", RegistrationConstants.PACKET_TYPE_LOST.equals(this.registrationCategory));
		allIdentityDetails.put("age", this.age);
		allIdentityDetails.put("isChild", this.isChild);

		allIdentityDetails.put("updatableFields",
				this.updatableFields == null ? Collections.EMPTY_LIST : this.updatableFields);
		allIdentityDetails.put("updatableFieldGroups",
				this.updatableFieldGroups == null ? Collections.EMPTY_LIST : this.updatableFieldGroups);
		allIdentityDetails.putAll(this.demographics);
		allIdentityDetails.putAll(this.documents);
		allIdentityDetails.putAll(this.biometrics);
		return allIdentityDetails;
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty();
	}

	public List<BiometricsDto> addAllBiometrics(String subType, Map<String, BiometricsDto> biometricsDTOMap,
			double thresholdScore, int maxRetryAttempt) {

		List<BiometricsDto> savedBiometrics = null;
		if (subType != null && biometricsDTOMap != null && !biometricsDTOMap.isEmpty()) {

			savedBiometrics = new LinkedList<>();

			boolean isForceCaptured = false;

			/** Find force capture or not */
			if (getQualityScore(biometricsDTOMap.values().stream().collect(Collectors.toList())) < thresholdScore) {

				if (maxRetryAttempt == 1) {
					isForceCaptured = true;
				} else {
					BiometricsDto biometricsDto = getBiometric(subType, Biometric
							.getBiometricByMDMConstant(biometricsDTOMap.get(0).getBioAttribute()).getAttributeName());

					if (biometricsDto != null && biometricsDto.getNumOfRetries() + 1 >= maxRetryAttempt) {
						isForceCaptured = true;
					}
				}
			}

			/** Modify the Biometrics DTO and save */
			for (Entry<String, BiometricsDto> entry : biometricsDTOMap.entrySet()) {

				BiometricsDto savedRegistrationBiometric = getBiometric(subType, entry.getKey());

				BiometricsDto value = entry.getValue();

				if (savedRegistrationBiometric != null
						&& savedRegistrationBiometric.getQualityScore() > value.getQualityScore()) {
					value = savedRegistrationBiometric;
				}
				value.setForceCaptured(isForceCaptured);

				value.setSubType(subType);
				savedBiometrics.add(addBiometric(subType, entry.getKey(), value));

			}
		}

		return savedBiometrics;
	}

	/*
	 * private String getRegistrationDTOBioAttribute(String attribute) {
	 * 
	 * String bioAttributeByMap = RegistrationConstants.regBioMap.get(attribute);
	 * 
	 * return bioAttributeByMap != null ? bioAttributeByMap : attribute; }
	 */

	private double getQualityScore(List<BiometricsDto> biometrics) {
		double qualityScore = 0.0;

		for (BiometricsDto biometricsDto : biometrics) {
			qualityScore += biometricsDto.getQualityScore();
		}

		return qualityScore;
	}
}
