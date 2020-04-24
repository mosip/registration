package io.mosip.registration.dto.biometric;

import java.util.LinkedHashMap;
import java.util.Map;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.BaseDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * This class contains the Biometric details
 * 
 * @author Dinesh Asokan
 * @since 1.0.0
 *
 */
public class BiometricDTO extends BaseDTO {

	private Map<String, BiometricInfoDTO> biometricsMap;

	public BiometricDTO() {
		biometricsMap = new LinkedHashMap<>();
		biometricsMap.put(RegistrationConstants.applicantBiometricDTO, null);
		biometricsMap.put(RegistrationConstants.introducerBiometricDTO, null);
		biometricsMap.put(RegistrationConstants.supervisorBiometricDTO, null);
		biometricsMap.put(RegistrationConstants.operatorBiometricDTO, null);

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
	}

	public BiometricInfoDTO getIntroducerBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.introducerBiometricDTO);

	}

	public void setIntroducerBiometricDTO(BiometricInfoDTO introducerBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.introducerBiometricDTO, introducerBiometricDTO);
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

}
