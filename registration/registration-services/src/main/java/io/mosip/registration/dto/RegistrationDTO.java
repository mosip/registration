package io.mosip.registration.dto;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.demographic.DocumentDetailsDTO;
import io.mosip.registration.packetmananger.dto.AuditDto;
import io.mosip.registration.packetmananger.dto.BiometricsDto;
import io.mosip.registration.packetmananger.dto.DocumentDto;
import io.mosip.registration.packetmananger.dto.SimpleDto;
import io.mosip.registration.packetmananger.dto.metadata.BiometricsException;
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
	
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	
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
	private Map<String, DocumentDto> documents = new HashMap<>();
	private Map<String, BiometricsDto> biometrics = new HashMap<>();
	private Map<String, BiometricsException> biometricExceptions = new HashMap<>(); 
	
	private List<BiometricDTO> supervisorBiometrics;
	private List<BiometricDTO> officerBiometrics;
	
	private List<AuditDto> auditDTOs;
	private Timestamp auditLogStartTime;
	private Timestamp auditLogEndTime;
	
	/** The acknowledge receipt. */
	private byte[] acknowledgeReceipt;

	/** The acknowledge receipt name. */
	private String acknowledgeReceiptName;
		
	public void addDemographicField(String fieldId, Object value) {
		this.demographics.put(fieldId, (value != null) ? value : null);
	}
	
	public void addDemographicField(String fieldId, String applicationLanguage, String value,
			String localLanguage, String localValue) {
		List<SimpleDto> values = new ArrayList<SimpleDto>();
		if(value != null && !value.isEmpty())
			values.add(new SimpleDto(applicationLanguage, value));
		
		if(localValue != null && !localValue.isEmpty())
			values.add(new SimpleDto(localLanguage, localValue));
	
		this.demographics.put(fieldId, values);
	}
	
	public void removeDemographicField(String fieldId) {
		this.demographics.remove(fieldId);
	}
	
	public void setDateField(String fieldId, String day, String month, String year) {
		if(day != null && month != null && year != null) {
			LocalDate date = LocalDate.of(Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(day));
			this.demographics.put(fieldId, date.format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
		}		
	}
	
	public String[] getDateField(String fieldId) {
		if(this.demographics.containsKey(fieldId)) {
			String value = (String) this.demographics.get(fieldId);
			LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern(DATE_FORMAT));
			return new String[]{ String.valueOf(date.getDayOfMonth()), String.valueOf(date.getMonthValue()) , 
					String.valueOf(date.getYear()) };
		}
		return null;
	}
	
	public void addDocument(String fieldId, DocumentDto value) {
		this.documents.put(fieldId, value);
	}
	
	public List<BiometricsDto> getBiometric(String subType, List<String> bioAttributes) {
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();
		for(String bioAttribute : bioAttributes) {
			String key = String.format("%s_%s", subType, bioAttribute);
			if(this.biometrics.containsKey(key))
				list.add(this.biometrics.get(key));
		}
		return list;
	}
	
	public void removeAllBiometrics(String subType, String bioAttribute) {
		this.biometrics.remove(String.format("%s_%s", subType, bioAttribute));
	}
	
	public void addBiometric(String subType, String bioAttribute, BiometricsDto value) {
		String key = String.format("%s_%s", subType, bioAttribute);
		int currentCount = 0;
		if(this.biometrics.get(key) != null) {
			currentCount = this.biometrics.get(key).getNumOfRetries();
		}
		value.setNumOfRetries(currentCount+1);
		this.biometrics.put(key, value);
		this.biometricExceptions.remove(key);
	}
	
	public void addBiometricException(String subType, String bioAttribute, String reason, String exceptionType) {
		String key = String.format("%s_%s", subType, bioAttribute);
		this.biometrics.remove(String.format("%s_%s", subType, bioAttribute));
		this.biometricExceptions.put(key, new BiometricsException(null, bioAttribute, reason, exceptionType, 
				subType));
	}
	
	public boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
		return this.biometricExceptions.containsKey(String.format("%s_%s", subType, bioAttribute));
	}
	
	public void removeBiometricException(String subType, String bioAttribute) {
		this.biometricExceptions.remove(String.format("%s_%s", subType, bioAttribute));
	}
	
	public BiometricsDto getBiometric(String subType, String bioAttribute) {
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
	
	public BiometricsDto removeBiometric(String subType, String bioAttribute) {
		String key = String.format("%s_%s", subType, bioAttribute);
		return this.biometrics.remove(key);
	}
}
