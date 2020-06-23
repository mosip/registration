package io.mosip.registration.service.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.RequestDetail;

/**
 * This class {@code BioService} handles all the biometric captures and
 * validations through MDM service
 * 
 * @author taleev.aalam
 * @since 1.0.0
 */
public interface BioService {

	/**
	 * Returns Authentication validator Dto that will be passed
	 * 
	 * <p>
	 * The method will return fingerPrint validator Dto that will be passed to
	 * finger print authentication method for fingerPrint validator
	 * </p>
	 * .
	 *
	 * @param userId
	 *            - the user ID
	 * @return AuthenticationValidatorDTO - authenticationValidatorDto
	 * @throws RegBaseCheckedException
	 *             - the exception that handles all checked exceptions
	 * @throws IOException
	 *             - Exception that may occur while reading the resource
	 */
	/*public AuthenticationValidatorDTO getFingerPrintAuthenticationDto(String userId, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException;*/

	/**
	 * Validates FingerPrint after getting the scanned data for the particular given
	 * User id
	 * 
	 * <p>
	 * The MDM service will be triggered to capture the user fingerprint which will
	 * be validated against the stored fingerprint from the DB
	 * </p>
	 * .
	 *
	 * @param authenticationValidatorDTO
	 *            - authenticationValidatorDto
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
	/*public boolean validateFingerPrint(AuthenticationValidatorDTO authenticationValidatorDTO);*/

	/**
	 * Returns Authentication validator Dto that will be passed
	 * 
	 * <p>
	 * The method will return iris validator Dto that will be passed to finger print
	 * authentication method for iris validator
	 * </p>
	 * .
	 *
	 * @param userId
	 *            - the user ID
	 * @return AuthenticationValidatorDTO - authenticationValidatorDto
	 * @throws RegBaseCheckedException
	 *             - the exception that handles all checked exceptions
	 * @throws IOException
	 *             - Exception that may occur while reading the resource
	 */
	/*public AuthenticationValidatorDTO getIrisAuthenticationDto(String userId, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException;*/

	/**
	 * Validates Iris after getting the scanned data for the given user ID
	 * 
	 * *
	 * <p>
	 * The MDM service will be triggered to capture the user Iris data which will be
	 * validated against the stored iris from the DB through auth validator service
	 * </p>
	 * .
	 *
	 * @param authenticationValidatorDTO
	 *            - authenticationValidtorDto
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
	//public boolean validateIris(AuthenticationValidatorDTO authenticationValidatorDTO);

	/**
	 * Gets the finger print image as DTO from the MDM service based on the
	 * fingerType
	 * 
	 * @param fpDetailsDTO
	 *            the fp details DTO
	 * @param requestDetail
	 *            FP Request Detail
	 * @param attempt
	 *            attempt number
	 * @return FingerPrint Details
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 * @throws IOException
	 *             the IOexception
	 */
	FingerprintDetailsDTO getFingerPrintImageAsDTO(RequestDetail requestDetail, int attempt)
			throws RegBaseCheckedException, IOException;

	/**
	 * checks if the MDM service is enabled
	 * 
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
	boolean isMdmEnabled();

	/**
	 * invokes the MDM service and gets the Segmented finger print images.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @param filePath
	 *            the file path
	 * @param fingerType
	 *            the finger type
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	void segmentFingerPrintImage(FingerprintDetailsDTO fingerprintDetailsDTO, String[] filePath, String fingerType)
			throws RegBaseCheckedException;

	/**
	 * Validates Face after getting the scanned data
	 * 
	 * 
	 * @param authenticationValidatorDTO
	 *            - the AuthenticationValidator DTO
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
	//boolean validateFace(AuthenticationValidatorDTO authenticationValidatorDTO);

	/**
	 * Returns Authentication validator Dto that will be passed
	 * 
	 * <p>
	 * The method will return face validator Dto that will be passed to finger print
	 * authentication method for face validator
	 * </p>
	 * .
	 *
	 * @param userId
	 *            - the user ID
	 * @return AuthenticationValidatorDTO - authenticationValidatorDto
	 */
	/*public AuthenticationValidatorDTO getFaceAuthenticationDto(String userId, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException;*/

	/**
	 * Gets the iris stub image as DTO.
	 *
	 * @param requestDetail
	 *            details of iris request
	 * @param leftEyeAttempt
	 *            leftEye attempt number
	 * @param rightEyeAttempt
	 *            right eye attempt number
	 * @throws IOException
	 *             the IO exception
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 * @return Captured Iris Details DTO
	 */
	IrisDetailsDTO getIrisImageAsDTO(RequestDetail requestDetail, int leftEyeAttempt, int rightEyeAttempt)
			throws RegBaseCheckedException, IOException;

	/**
	 * Validate the Input Finger with the finger that is fetched from the Database.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @param userFingerprintDetails
	 *            the user fingerprint details
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
//	boolean validateFP(FingerprintDetailsDTO fingerprintDetailsDTO, List<UserBiometric> userFingerprintDetails);

	/**
	 * Captures the face
	 * 
	 * @return CaptureResponseDto
	 */
	CaptureResponseDto captureFace(RequestDetail requestDetail) throws RegBaseCheckedException, IOException;

	/**
	 * Returns single biometric bio value
	 * 
	 * @param CaptureResponseDto
	 *            capture respsone dto
	 * @return byte[]
	 */
	byte[] getSingleBioValue(CaptureResponseDto captureResponseDto);

	/**
	 * Returns single biometric iso template
	 * 
	 * @param CaptureResponseDto
	 *            catpure response dto
	 * @return byte[]
	 * @throws IOException 
	 */
	byte[] getSingleBiometricIsoTemplate(CaptureResponseDto captureResponseDto) throws IOException;

	/**
	 * @param bioType
	 *            biometricType
	 * 
	 * @param attempt
	 *            attemptNumber
	 * @return QualityScore
	 */
	public Double getBioQualityScores(String bioType, int attempt);

	/**
	 * @param bioType
	 *            biometricType
	 * @return quality score
	 */
	public Double getHighQualityScoreByBioType(String bioType, Double qualityScore);

	/**
	 * @param bioType
	 *            biometricType
	 * @param attempt
	 *            attempt number
	 * @return
	 */
	public byte[] getBioStreamImage(String bioType, int attempt);

	/**
	 * @param detailsDTO
	 *            Captured Fingerprint Details
	 * @return whether captured fingerprints were valid or not
	 */
//	public boolean isValidFingerPrints(FingerprintDetailsDTO detailsDTO,boolean isAuth);

	/**
	 * @param segmentedFingerprints
	 *            captured segmented fingerprints
	 * @return whether captured segmentedFingerprints were duplicated or not
	 */
	//public boolean validateBioDeDup(List<FingerprintDetailsDTO> segmentedFingerprints);

	/**
	 * @return whether All non exception fingers were captured or not
	 */
	//public boolean isAllNonExceptionFingerprintsCaptured();

	public Map<String, List<String>> getLowQualityBiometrics();
	
	public boolean hasBiometricExceptionToggleEnabled() ;
	
//	public void remove
	
	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto)  throws RegBaseCheckedException, IOException ;
	
	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto)  throws RegBaseCheckedException, IOException ;

	public InputStream getStream(String modality) throws MalformedURLException, IOException ;
}