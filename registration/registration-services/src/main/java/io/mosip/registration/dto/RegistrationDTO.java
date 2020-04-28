package io.mosip.registration.dto;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Arrays;

import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.demographic.DemographicDTO;
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
	
	private SelectionListDTO selectionListDTO;
	private boolean isUpdateUINNonBiometric;	
	private boolean isNameNotUpdated;	
	private boolean isUpdateUINChild;
	private boolean isAgeCalculatedByDOB;
	
	private BiometricDTO biometricDTO = new BiometricDTO();
	private Map<String, Object> demographics = new HashMap<>();
	private Map<String, DocumentDetailsDTO> documents = new HashMap<>();
	
	private List<AuditDTO> auditDTOs;
	private Timestamp auditLogStartTime;
	private Timestamp auditLogEndTime;
	
	/** The acknowledge receipt. */
	private byte[] acknowledgeReceipt;

	/** The acknowledge receipt name. */
	private String acknowledgeReceiptName;
		
	public void addDemographicField(String fieldId, Object value) {
		this.demographics.put(fieldId, value);
	}
	
	public void addDemographicField(String fieldId, String language, String value) {
		this.demographics.put(fieldId, new ValuesDTO(language, value));
	}
	
	public void addDemographicField(String fieldId, String applicationLanguage, String applicationValue,
			String localLanguage, String localValue) {
		ValuesDTO valuesDTO[] = {new ValuesDTO(applicationLanguage, applicationValue), 
									new ValuesDTO(localLanguage, localValue)};		
		this.demographics.put(fieldId, Arrays.asList(valuesDTO));
	}
	
	public void removeDemographicField(String fieldId) {
		this.demographics.remove(fieldId);
	}
	
	public void addDocument(String fieldId, DocumentDetailsDTO value) {
		this.documents.put(fieldId, value);
	}
	
	public Map<String, Object> getIdentity() {
		Map<String, Object> allIdentityDetails = new LinkedHashMap<String, Object>();
		allIdentityDetails.put("IDSchemaVersion", idSchemaVersion);
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
