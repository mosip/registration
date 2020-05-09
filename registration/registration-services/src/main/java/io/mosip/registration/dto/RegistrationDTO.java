package io.mosip.registration.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.demographic.DocumentDetailsDTO;
import io.mosip.registration.dto.demographic.ValuesDTO;
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
	
	private double idSchemaVersion;	
	private String registrationId;
	private String preRegistrationId;
	
	private RegistrationMetaDataDTO registrationMetaDataDTO;
	private OSIDataDTO osiDataDTO;
	
	private HashMap<String, Object> selectionListDTO;
	private boolean isUpdateUINNonBiometric;	
	private boolean isNameNotUpdated;	
	private boolean isUpdateUINChild;
	private boolean isAgeCalculatedByDOB;
	
	private BiometricDTO biometricDTO = new BiometricDTO();
	private Map<String, Object> demographics = new HashMap<>();
	private Map<String, DocumentDetailsDTO> documents = new HashMap<>();
	private Map<String, BiometricDTO> biometrics = new HashMap<>();
	
	private List<BiometricDTO> supervisorBiometrics;
	private List<BiometricDTO> officerBiometrics;
	
	private List<AuditDTO> auditDTOs;
	private Timestamp auditLogStartTime;
	private Timestamp auditLogEndTime;
	
	/** The acknowledge receipt. */
	private byte[] acknowledgeReceipt;

	/** The acknowledge receipt name. */
	private String acknowledgeReceiptName;
		
	public void addDemographicField(String fieldId, Object value) {
		this.demographics.put(fieldId, (value != null) ? value : null);
	}
	
	public void addDemographicField(String fieldId, String language, String value) {
		this.demographics.put(fieldId, new ValuesDTO(language, (value != null && !value.isEmpty()) ? 
					value : null));
	}
	
	public void addDemographicField(String fieldId, String applicationLanguage, String value,
			String localLanguage, String localValue) {
		List<ValuesDTO> values = new ArrayList<>();
		if(value != null && !value.isEmpty())
			values.add(new ValuesDTO(applicationLanguage, value));
		
		if(localValue != null && !localValue.isEmpty())
			values.add(new ValuesDTO(localLanguage, localValue));
	
		this.demographics.put(fieldId, values);
	}
	
	public void removeDemographicField(String fieldId) {
		this.demographics.remove(fieldId);
	}
	
	public void addDocument(String fieldId, DocumentDetailsDTO value) {
		this.documents.put(fieldId, value);
	}
	
	public List<BiometricDTO> getBiometric(String subType, List<String> bioAttributes) {
		List<BiometricDTO> list = new ArrayList<BiometricDTO>();
		for(String bioAttribute : bioAttributes) {
			String key = String.format("%s_%s", subType, bioAttribute);
			if(this.biometrics.containsKey(key))
				list.add(this.biometrics.get(key));
		}
		return list;
	}
	
	public void addBiometric(String subType, String bioAttribute, BiometricDTO value) {
		String key = String.format("%s_%s", subType, bioAttribute);
		this.biometrics.put(key, value);
	}
	
	public void addBiometricException(String subType, String bioAttribute, String exceptionType, String reason) {
		String key = String.format("%s_%s", subType, bioAttribute);
		BiometricDTO value = new BiometricDTO();
		value.setException(true);
		value.setExceptionType(exceptionType);
		value.setReason(reason);
		this.biometrics.put(key, value);
	}
	
	public void removeBiometricException(String subType, String bioAttribute) {
		String key = String.format("%s_%s", subType, bioAttribute);
		this.biometrics.remove(key);
	}
	
	public BiometricDTO getBiometric(String subType, String bioAttribute) {
		String key = String.format("%s_%s", subType, bioAttribute);
		return this.biometrics.get(key);
	}
	
	public Map<String, Object> getIdentity() {
		Map<String, Object> allIdentityDetails = new LinkedHashMap<String, Object>();
		allIdentityDetails.put("IDSchemaVersion", idSchemaVersion);
		if(registrationMetaDataDTO.getUin() != null)
			allIdentityDetails.put("UIN", registrationMetaDataDTO.getUin());
		
		allIdentityDetails.putAll(this.demographics);
		allIdentityDetails.putAll(this.documents);
		
		if(biometricDTO.getApplicantBiometrics() != null)
			allIdentityDetails.put("applicantBiometrics", biometricDTO.getApplicantBiometrics());
		if(biometricDTO.getIntroducerBiometrics() != null)
			allIdentityDetails.put("introducerBiometrics", biometricDTO.getIntroducerBiometrics());
				
		Map<String, Object> identity = new LinkedHashMap<String, Object>();
		identity.put("identity", allIdentityDetails);
		return identity;	
	}
}
