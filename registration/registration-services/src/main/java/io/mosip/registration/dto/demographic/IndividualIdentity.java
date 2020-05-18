package io.mosip.registration.dto.demographic;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Arrays;
import org.springframework.stereotype.Component;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.mosip.kernel.packetmanager.dto.DocumentDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class contains the applicant demographic, biometric, proofs and parent
 * or guardian biometric details.
 *
 * @author Balaji Sridharan
 * @since 1.0.0
 */
@Component
@Data
@EqualsAndHashCode(callSuper = false)
public class IndividualIdentity extends Identity {

	 /** The uin. */
	 @JsonProperty("UIN")
	 private BigInteger uin;
	
	 /** The full name. */
	 private List<ValuesDTO> fullName;
	
	 /** The date of birth. */
	 private String dateOfBirth;
	
	 /** The age. */
	 private Integer age;
	
	 /** The gender. */
	 private List<ValuesDTO> gender;
	
	 /** The full name. */
	 private List<ValuesDTO> residenceStatus;
	
	 /** The address line 1. */
	 private List<ValuesDTO> addressLine1;
	
	 /** The address line 2. */
	 private List<ValuesDTO> addressLine2;
	
	 /** The address line 3. */
	 private List<ValuesDTO> addressLine3;
	
	 /** The region. */
	 private List<ValuesDTO> region;
	
	 /** The province. */
	 private List<ValuesDTO> province;
	
	 /** The city. */
	 private List<ValuesDTO> city;
	
	 /** The postal code. */
	 private String postalCode;
	
	 /** The phone. */
	 private String phone;
	
	 /** The email. */
	 private String email;
	
	 /** Reference Identity number. */
	 private String referenceIdentityNumber;
	
	 /** The Zone. */
	 private List<ValuesDTO> zone;
	
	 /** The parent or guardian RID. */
	 private BigInteger parentOrGuardianRID;
	
	 /** The parent or guardian UIN. */
	 private BigInteger parentOrGuardianUIN;
	
	 /** The parent or guardian name. */
	 private List<ValuesDTO> parentOrGuardianName;
	
	 /** The proof of address. */
	 private DocumentDto proofOfAddress;
	
	 /** The proof of identity. */
	 private DocumentDto proofOfIdentity;
	
	 /** The proof of relationship. */
	 private DocumentDto proofOfRelationship;
	
	 /** The date of birth proof. */
	 private DocumentDto proofOfDateOfBirth;
	
	 /** The proof of exception. */
	 private DocumentDto proofOfException;
	
	 /** The individual biometrics. */
	 private CBEFFFilePropertiesDTO individualBiometrics;
	
	 /** The parent or guardian biometrics. */
	 private CBEFFFilePropertiesDTO parentOrGuardianBiometrics;

	/**
	 * Individual Identity map will be having all the demographic information of
	 * resident, with key as fxId and value as {@value ValuesDTO}
	 */
	private Map<String, Object> individualIdentityMap;
	
	private static Map<String, Object> documentsMap;
	
	/**
	 * Assign the map to individual identity of resident map
	 * 
	 * @param individualIdentityMap
	 *            demographic information of resident
	 */
	public void setIndividualIdentityMap(Map<String, Object> individualIdentityMap) {
		this.individualIdentityMap = individualIdentityMap;
	}

	public void addInIndividualIdentityMap(String fxId, String language, String value) {

		/** initilaize identity map if not added till now else take the same map */
		individualIdentityMap = individualIdentityMap == null ? new HashMap<>() : individualIdentityMap;

		/** Add to element to the identity map */
		individualIdentityMap.put(fxId, new ValuesDTO(language, value));
	}

	public void addInIndividualIdentityMap(String fxId, String applicationLanguage, String applicationValue,
			String localLanguage, String localValue) {

		/** initilaize identity map if not added till now else take the same map */
		individualIdentityMap = individualIdentityMap == null ? new HashMap<>() : individualIdentityMap;

		ValuesDTO valuesDTO[] = {new ValuesDTO(applicationLanguage, applicationValue), new ValuesDTO(localLanguage, localValue)};
		
		/** Add to element to the identity map */
		individualIdentityMap.put(fxId, Arrays.asList(valuesDTO));
	}
	
	public void addInIndividualIdentityMap(String fxId, Object value) {

		/** initilaize identity map if not added till now else take the same map */
		individualIdentityMap = individualIdentityMap == null ? new HashMap<>() : individualIdentityMap;

		/** Add to element to the identity map */
		individualIdentityMap.put(fxId, value);
	}

	public void removeInIndividualIdentityMap(String fxId) {

		if (individualIdentityMap != null) {
			
			/** Remove the element using fxId in the identity map */
			individualIdentityMap.remove(fxId);
		}
	}
	
	public static void addDocumentsInIndividualIdentityMap(String fxId, Object value) {

		/** initilaize identity map if not added till now else take the same map */
		documentsMap = documentsMap == null ? new HashMap<>() : documentsMap;

		/** Add to element to the identity map */
		documentsMap.put(fxId, value);
	}

}
