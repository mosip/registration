package io.mosip.registration.validator;

import java.util.List;

import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;

/**
 * This class will be the base class for all the validator classes like FingerprintValidator, 
 * FaceValidator and Irisvalidator
 * 
 * @author Saravanakumar G
 *
 */
public abstract class AuthenticationBaseValidator {

	/**
	 * <p>This method will be implemented in all the Validator classes.</p>
	 * <p>It will validate the Finger/Iris/Face based on the input 
	 * parameter {@link AuthenticationValidatorDTO}</p>
	 * <p>The {@link AuthenticationValidatorDTO} data object contains all the biometric 
	 * related details for validations</p>
	 * 
	 * @param authenticationValidatorDTO The DTO which contains the data to be validated
	 * @return boolean Return whether the Validation is success or not
	 */
	public abstract boolean validate(AuthenticationValidatorDTO authenticationValidatorDTO);
	
	/**
	 *This is the seperate method for otp authentication. This method will internally call 
	 * the OTP service to validate the otp.
	 * <p>If Online:</p>
	 * 	<p>If the Validation Success:</p>
	 * 		<p>Then it will populate and return the {@link AuthenticationValidatorDTO}</p>
	 * 	<p>If the validation fails:</p>
	 * 		<p>Then it will return the {@link AuthenticationValidatorDTO} as null</p>
	 * <p>If Offline:</p>
	 * 	<p>Throws the corresponding error meessage</p>
	 * @param userId - the User ID
	 * @param otp - the OTP to be validated
	 * @param haveToSaveAuthToken
	 *            flag indicating whether the Authorization Token have to be saved
	 *            in context
	 * @return the {@link AuthTokenDTO} after validation
	 */
	public abstract AuthTokenDTO validate(String userId, String otp, boolean haveToSaveAuthToken);
	
	/**
	 * Bio mertics validator.
	 *
	 * @param listOfBiometrics the list of biometrics
	 * @return true, if successful
	 */
	public abstract boolean bioMerticsValidator(List<BiometricsDto> listOfBiometrics);

}
