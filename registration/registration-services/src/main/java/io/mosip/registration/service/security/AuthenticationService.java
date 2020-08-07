package io.mosip.registration.service.security;

import java.util.List;

import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;

public interface AuthenticationService {

	/**
	 * Common Validator for all the Authentications
	 * 
	 * @param validatorType
	 *            The type of validator
	 * @param authenticationValidatorDTO
	 *            The authentication validation inputs
	 * @return Boolean returning whether it is matched or not
	 */
	//Boolean authValidator(String validatorType, AuthenticationValidatorDTO authenticationValidatorDTO);
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
	 * This method is used to set the Authentication validators
	 * 
	 * @param authBaseValidators
	 *            List of validators
	 */
//	void setAuthenticationBaseValidator(List<AuthenticationBaseValidator> authBaseValidators);
	
	/**
	 * This method is used to validate pwd authentication
	 * 
	 * @param authenticationValidatorDTO
	 *            The authentication validation inputs with user id and pwd
	 * @return String
	 */
	String validatePassword(AuthenticationValidatorDTO authenticationValidatorDTO);
	/**
	 * Common Validator for all the Authentications same method is reused in deduplication check
	 * 
	 * @param validatorType
	 *            The type of validator
	 * @param authenticationValidatorDTO
	 *            The authentication validation inputs
	 * @return Boolean returning whether it is matched or not
	 */
	//Boolean validateBiometrics(String validatorType,List<BiometricsDto> listOfBiometrics);
}