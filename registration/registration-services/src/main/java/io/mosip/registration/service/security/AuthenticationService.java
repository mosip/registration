package io.mosip.registration.service.security;

import java.util.List;

import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;

public interface AuthenticationService {

	/**
	 * Validator for Biometric authentication
	 * @param userId
	 * @param modality
	 * @param biometrics
	 * @return
	 */
	Boolean authValidator(String userId, String modality, List<BiometricsDto> biometrics);
	
	/**
	 * Validator for OTP authentication
	 * 
	 * @param validatorType
	 *            The type of validator which is OTP
	 * @param userId
	 *            The userId
	 * @param otp
	 *            otp entered
	 * @param haveToSaveAuthToken
	 *            flag indicating whether the Authorization Token have to be saved
	 *            in context
	 * @return {@link AuthTokenDTO} returning authtokendto
	 */
	AuthTokenDTO authValidator(String validatorType, String userId, String otp, boolean haveToSaveAuthToken);


	/**
	 * This method is used to validate pwd authentication
	 * 
	 * @param authenticationValidatorDTO
	 *            The authentication validation inputs with user id and pwd
	 * @return String
	 */
	String validatePassword(AuthenticationValidatorDTO authenticationValidatorDTO);

}