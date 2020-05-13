package io.mosip.registration.dto.biometric;

import java.util.LinkedHashMap;
import java.util.Map;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.builder.Builder;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.BaseDTO;
import io.mosip.registration.dto.demographic.CBEFFFilePropertiesDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class contains the Biometric details
 * 
 * @author Dinesh Asokan
 * @since 1.0.0
 *
 */
@Data
public class BiometricDTO {
	
	private byte[] attributeISO;
	private String bioAttribute;
	private double qualityScore;
	private boolean isForceCaptured;
	private int numOfRetries;
	private long formatType;	
	private boolean isCaptured;
	
	//TODO need to remove below fields and handle them

	private Map<String, BiometricInfoDTO> biometricsMap;
	
	private CBEFFFilePropertiesDTO applicantBiometrics;
	private CBEFFFilePropertiesDTO introducerBiometrics;
	
	public BiometricDTO(String bioAttribute, byte[] attributeISO, double qualityScore) {
		this.bioAttribute = bioAttribute;
		this.attributeISO = attributeISO;
		this.qualityScore = qualityScore;
	}

	public BiometricDTO() {
		biometricsMap = new LinkedHashMap<>();
		biometricsMap.put(RegistrationConstants.applicantBiometricDTO, new BiometricInfoDTO());
		biometricsMap.put(RegistrationConstants.introducerBiometricDTO, new BiometricInfoDTO());
		biometricsMap.put(RegistrationConstants.supervisorBiometricDTO, new BiometricInfoDTO());
		biometricsMap.put(RegistrationConstants.operatorBiometricDTO, new BiometricInfoDTO());
	}

	public Map<String, BiometricInfoDTO> getBiometricsMap() {
		return biometricsMap;
	}

	public void setBiometricsMap(Map<String, BiometricInfoDTO> biometricsMap) {
		this.biometricsMap = biometricsMap;
	}

	public BiometricInfoDTO getApplicantBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.applicantBiometricDTO);
	}

	public void setApplicantBiometricDTO(BiometricInfoDTO applicantBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.applicantBiometricDTO, applicantBiometricDTO);
		applicantBiometrics = buildCBEFFDTO(isCBEFFNotAvailable(applicantBiometricDTO),
				RegistrationConstants.APPLICANT_BIO_CBEFF_FILE_NAME);
	}

	public BiometricInfoDTO getIntroducerBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.introducerBiometricDTO);

	}

	public void setIntroducerBiometricDTO(BiometricInfoDTO introducerBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.introducerBiometricDTO, introducerBiometricDTO);
		introducerBiometrics = buildCBEFFDTO(isCBEFFNotAvailable(introducerBiometricDTO),
				RegistrationConstants.AUTHENTICATION_BIO_CBEFF_FILE_NAME);
	}

	public BiometricInfoDTO getSupervisorBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.supervisorBiometricDTO);
	}

	public void setSupervisorBiometricDTO(BiometricInfoDTO supervisorBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.supervisorBiometricDTO, supervisorBiometricDTO);
	}

	public BiometricInfoDTO getOperatorBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.operatorBiometricDTO);
	}

	public void setOperatorBiometricDTO(BiometricInfoDTO operatorBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.operatorBiometricDTO, operatorBiometricDTO);
	}

	public void addBiometricsToMap(String key, BiometricInfoDTO biometricDTO) {
		biometricsMap.put(key, biometricDTO);
	}
	
	private CBEFFFilePropertiesDTO buildCBEFFDTO(boolean isCBEFFNotRequired, String cbeffFileName) {
		return isCBEFFNotRequired ? null
				: (CBEFFFilePropertiesDTO) Builder.build(CBEFFFilePropertiesDTO.class)
						.with(cbeffProperties -> cbeffProperties.setFormat(RegistrationConstants.CBEFF_FILE_FORMAT))
						.with(cbeffProperty -> cbeffProperty.setValue(cbeffFileName
								.replace(RegistrationConstants.XML_FILE_FORMAT, RegistrationConstants.EMPTY)))
						.with(cbeffProperty -> cbeffProperty.setVersion(1.0)).get();
	}

	private boolean isCBEFFNotAvailable(BiometricInfoDTO personBiometric) {
		return personBiometric.getFingerprintDetailsDTO().isEmpty() && personBiometric.getIrisDetailsDTO().isEmpty()
				&& personBiometric.getFace().getFace() == null;
	}
}
