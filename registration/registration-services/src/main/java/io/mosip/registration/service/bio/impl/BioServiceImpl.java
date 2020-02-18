package io.mosip.registration.service.bio.impl;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_FACADE;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_FACADE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.machinezoo.sourceafis.FingerprintTemplate;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.device.fp.FingerprintProvider;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.BiometricExceptionDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.CaptureResponsBioDataDto;
import io.mosip.registration.mdm.dto.CaptureResponseBioDto;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.security.AuthenticationService;
import javafx.scene.image.Image;

/**
 * This class {@code BioServiceImpl} handles all the biometric captures and
 * validations through MDM service
 * 
 * @author taleev.aalam
 *
 */
@Service
public class BioServiceImpl extends BaseService implements BioService {

	@Autowired
	MosipBioDeviceManager mosipBioDeviceManager;

	@Autowired
	private AuthenticationService authService;

	@Autowired
	private FingerprintProvider fingerprintProvider;

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BioServiceImpl.class);

	private byte[] isoImage;

	private static HashMap<String, CaptureResponsBioDataDto> BEST_CAPTURES = new HashMap<>();

	private static Map<String, Map<Integer, Double>> BIO_QUALITY_SCORE = new HashMap<String, Map<Integer, Double>>();
	
	private static Map<String, Map<Integer, Image>> BIO_STREAM_IMAGES = new HashMap<String, Map<Integer, Image>>();


	public static HashMap<String, CaptureResponsBioDataDto> getBestCaptures() {
		return BEST_CAPTURES;
	}

	public static void clearCaptures(List<String> captures) {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Clearing caprures of : "+captures);

		captures.forEach(key -> BEST_CAPTURES.remove(key));

	}

	public static void clearAllCaptures() {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Clearing All captures");
		
		BEST_CAPTURES.clear();
		BIO_QUALITY_SCORE.clear();
		BIO_STREAM_IMAGES.clear();

	}

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
	@Override
	public AuthenticationValidatorDTO getFingerPrintAuthenticationDto(String userId, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Invoking FingerPrint validator");

		if (isNull(userId))
			throwRegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_FINGERPRINT_AUTTHENTICATION);

		AuthenticationValidatorDTO authenticationValidatorDTO = null;
		CaptureResponseDto captureResponseDto = null;
		CaptureResponsBioDataDto captureResponseData = null;
		List<FingerprintDetailsDTO> fingerprintDetailsDTOs = new ArrayList<>();
		String bioType = "Right little";
		if (isMdmEnabled()) {
			captureResponseDto = mosipBioDeviceManager.authScan(requestDetail);
			if (captureResponseDto == null)
				throw new RegBaseCheckedException("202", "Decice is not available");
			if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
				throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
						captureResponseDto.getError().getErrorInfo());
			
			captureResponseDto.getMosipBioDeviceDataResponses().forEach(auth -> {
				FingerprintDetailsDTO fingerprintDetailsDTO = new FingerprintDetailsDTO();
				fingerprintDetailsDTO.setFingerPrintISOImage(
						Base64.getUrlDecoder().decode(auth.getCaptureResponseData().getBioExtract()));
				fingerprintDetailsDTO.setFingerType(auth.getCaptureResponseData().getBioSubType());
				fingerprintDetailsDTO.setForceCaptured(true);
				fingerprintDetailsDTOs.add(fingerprintDetailsDTO);
			});
			
			LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					"Calling for finger print validation through authService");
			authenticationValidatorDTO = new AuthenticationValidatorDTO();
			authenticationValidatorDTO.setFingerPrintDetails(fingerprintDetailsDTOs);
			authenticationValidatorDTO.setUserId(userId);
			authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.MULTIPLE);
			authenticationValidatorDTO.setAuthValidationFlag(true);
			validateFingerPrint(authenticationValidatorDTO);
			
			
		} else {
			isoImage = IOUtils.toByteArray(
					this.getClass().getResourceAsStream("/UserOnboard/rightHand/rightLittle/ISOTemplate.iso"));
		}

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "End FingerPrint validator");

		return authenticationValidatorDTO;
	}

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
	public boolean validateFingerPrint(AuthenticationValidatorDTO authenticationValidatorDTO) {
		return authService.authValidator(RegistrationConstants.FINGERPRINT, authenticationValidatorDTO);
	}

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
	@Override
	public AuthenticationValidatorDTO getIrisAuthenticationDto(String userId, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Scanning Iris");
		if (isNull(userId))
			throwRegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_AUTHENTICATION);

		AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
		List<IrisDetailsDTO> irisDetailsDTOs = new ArrayList<>();
		IrisDetailsDTO irisDetailsDTO = new IrisDetailsDTO();
		captureIris(irisDetailsDTO, requestDetail);
		irisDetailsDTOs.add(irisDetailsDTO);
		authenticationValidatorDTO.setUserId(userId);
		authenticationValidatorDTO.setIrisDetails(irisDetailsDTOs);
		authenticationValidatorDTO.setAuthValidationFlag(true);
		authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.SINGLE);

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Iris scan done");
		return authenticationValidatorDTO;

	}

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
	public boolean validateIris(AuthenticationValidatorDTO authenticationValidatorDTO) {
		return authService.authValidator(RegistrationConstants.IRIS, authenticationValidatorDTO);
	}

	/**
	 * Gets the finger print image as DTO with MDM
	 *
	 * @param fpDetailsDTO
	 *            the fp details DTO
	 * @param fingerType
	 *            the finger type
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 * @throws IOException
	 */
	public void getFingerPrintImageAsDTOWithMdm(FingerprintDetailsDTO fpDetailsDTO, RequestDetail requestDetail,
			int attempt) throws RegBaseCheckedException, IOException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Entering into getFingerPrintImageAsDTOWithMdm method..");
		CaptureResponseDto captureResponseDto = mosipBioDeviceManager.regScan(requestDetail);
		if (captureResponseDto == null)
			throw new RegBaseCheckedException("202", "Decice is not available");
		if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
			throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
					captureResponseDto.getError().getErrorInfo());
		fpDetailsDTO.setSegmentedFingerprints(new ArrayList<FingerprintDetailsDTO>());
		List<CaptureResponseBioDto> mosipBioDeviceDataResponses = captureResponseDto.getMosipBioDeviceDataResponses();

		int currentCaptureQualityScore = 0;

		for (CaptureResponseBioDto captured : mosipBioDeviceDataResponses) {

			FingerprintDetailsDTO fingerPrintDetail = new FingerprintDetailsDTO();
			CaptureResponsBioDataDto captureRespoonse = captured.getCaptureResponseData();

			currentCaptureQualityScore += Integer.parseInt(captureRespoonse.getQualityScore());

			// Get Best Capture
			captureRespoonse = getBestCapture(captureRespoonse);

			fingerPrintDetail.setFingerPrintISOImage(Base64.getUrlDecoder().decode(captureRespoonse.getBioExtract()));
			fingerPrintDetail.setFingerType(captureRespoonse.getBioSubType());
			fingerPrintDetail.setFingerPrint(Base64.getUrlDecoder().decode(captureRespoonse.getBioValue()));
			fingerPrintDetail.setQualityScore(Integer.parseInt(captureRespoonse.getQualityScore()));
			fingerPrintDetail.setFingerprintImageName("FingerPrint " + captureRespoonse.getBioSubType());
			fpDetailsDTO.getSegmentedFingerprints().add(fingerPrintDetail);
		}

		setBioQualityScores(requestDetail.getType(), attempt,
				currentCaptureQualityScore / mosipBioDeviceDataResponses.size());

		double slapQuality = (fpDetailsDTO.getSegmentedFingerprints().stream()
				.mapToDouble(finger -> finger.getQualityScore()).sum())
				/ fpDetailsDTO.getSegmentedFingerprints().size();
		fpDetailsDTO.setCaptured(true);
		fpDetailsDTO.setFingerType(requestDetail.getType());
		fpDetailsDTO.setQualityScore(slapQuality);
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Leaving getFingerPrintImageAsDTOWithMdm..");
	}

	/**
	 * Gets the finger print image as DTO without MDM.
	 *
	 * @param fpDetailsDTO
	 *            the fp details DTO
	 * @param fingerType
	 *            the finger type
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private void getFingerPrintImageAsDTONonMdm(FingerprintDetailsDTO fpDetailsDTO, RequestDetail requestDetail)
			throws RegBaseCheckedException {

		Map<String, Object> fingerMap = null;

		try {
			// TODO : Currently stubbing the data. once we have the device, we
			// can remove
			// this.

			if (requestDetail.getType().equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
				fingerMap = getFingerPrintScannedImageWithStub(RegistrationConstants.LEFTHAND_SLAP_FINGERPRINT_PATH);
			} else if (requestDetail.getType().equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				fingerMap = getFingerPrintScannedImageWithStub(RegistrationConstants.RIGHTHAND_SLAP_FINGERPRINT_PATH);
			} else if (requestDetail.getType().equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
				fingerMap = getFingerPrintScannedImageWithStub(RegistrationConstants.BOTH_THUMBS_FINGERPRINT_PATH);
			}

			if ((fingerMap != null)
					&& ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER) || (fpDetailsDTO
							.getQualityScore() < (double) fingerMap.get(RegistrationConstants.IMAGE_SCORE_KEY)))) {
				fpDetailsDTO.setFingerPrint((byte[]) fingerMap.get(RegistrationConstants.IMAGE_BYTE_ARRAY_KEY));
				fpDetailsDTO.setFingerprintImageName(requestDetail.getType().concat(RegistrationConstants.DOT)
						.concat((String) fingerMap.get(RegistrationConstants.IMAGE_FORMAT_KEY)));
				fpDetailsDTO.setFingerType(requestDetail.getType());
				fpDetailsDTO.setForceCaptured(false);
				fpDetailsDTO.setCaptured(true);
				if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
					fpDetailsDTO.setQualityScore((double) fingerMap.get(RegistrationConstants.IMAGE_SCORE_KEY));
				}
			}

		} finally {
			if (fingerMap != null && !fingerMap.isEmpty())
				fingerMap.clear();
		}
	}

	/**
	 * Stub method to get the finger print scanned image from local hard disk. Once
	 * SDK and device avilable then we can remove it.
	 *
	 * @param path
	 *            the path
	 * @return the finger print scanned image
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private Map<String, Object> getFingerPrintScannedImageWithStub(String path) throws RegBaseCheckedException {
		if (isNull(path))
			throwRegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_FINGERPRINT_SCANNED_PATH);

		try {
			LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Scanning of fingerprints details for user registration");

			BufferedImage bufferedImage = ImageIO.read(this.getClass().getResourceAsStream(path));

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "jpeg", byteArrayOutputStream);

			byte[] scannedFingerPrintBytes = byteArrayOutputStream.toByteArray();

			// Add image format, image and quality score in bytes array to map
			Map<String, Object> scannedFingerPrints = new WeakHashMap<>();
			scannedFingerPrints.put(RegistrationConstants.IMAGE_FORMAT_KEY, "jpg");
			scannedFingerPrints.put(RegistrationConstants.IMAGE_BYTE_ARRAY_KEY, scannedFingerPrintBytes);
			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				if (path.contains(RegistrationConstants.THUMBS)) {
					scannedFingerPrints.put(RegistrationConstants.IMAGE_SCORE_KEY, 90.0);
				} else if (path.contains(RegistrationConstants.LEFTPALM)) {
					scannedFingerPrints.put(RegistrationConstants.IMAGE_SCORE_KEY, 85.0);
				} else if (path.contains(RegistrationConstants.RIGHTPALM)) {
					scannedFingerPrints.put(RegistrationConstants.IMAGE_SCORE_KEY, 90.0);
				}
			}

			LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Scanning of fingerprints details for user registration completed");

			return scannedFingerPrints;
		} catch (Exception runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"Exception while scanning fingerprints details for user registration: %s caused by %s",
							runtimeException.getMessage(), runtimeException.getCause()));
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_FINGERPRINT_SCANNING_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_FINGERPRINT_SCANNING_ERROR.getErrorMessage());
		}
	}

	/**
	 * Gets the finger print image as DTO from the MDM service based on the
	 * fingerType
	 *
	 *
	 * @param fpDetailsDTO
	 *            the fp details DTO
	 * @param fingerType
	 *            the finger type
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 * @throws IOException
	 */
	public void getFingerPrintImageAsDTO(FingerprintDetailsDTO fpDetailsDTO, RequestDetail requestDetail, int attempt)
			throws RegBaseCheckedException, IOException {
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID, "Entering into BioServiceImpl-FingerPrintImageAsDTO");
		if (isMdmEnabled())
			getFingerPrintImageAsDTOWithMdm(fpDetailsDTO, requestDetail, attempt);
		else
			getFingerPrintImageAsDTONonMdm(fpDetailsDTO, requestDetail);
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID, "Leaving BioServiceImpl-FingerPrintImageAsDTO");
	}

	/**
	 * checks if the MDM service is enabled
	 * 
	 * @return boolean the validation result. <code>true</code> if match is found,
	 *         else <code>false</code>
	 */
	@Override
	public boolean isMdmEnabled() {
		return RegistrationConstants.ENABLE
				.equalsIgnoreCase(((String) ApplicationContext.map().get(RegistrationConstants.MDM_ENABLED)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#segmentFingerPrintImage(io.mosip
	 * .registration.dto.biometric.FingerprintDetailsDTO, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public void segmentFingerPrintImage(FingerprintDetailsDTO fingerprintDetailsDTO, String[] filePath,
			String fingerType) throws RegBaseCheckedException {

		if (isNull(fingerType))
			throwRegBaseCheckedException(
					RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_FINGERPRINT_IMAGE_TYPE);

		readSegmentedFingerPrintsSTUB(fingerprintDetailsDTO, filePath, fingerType);

	}

	/**
	 * {@code readFingerPrints} is to read the scanned fingerprints.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @param path
	 *            the path
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private void readSegmentedFingerPrintsSTUB(FingerprintDetailsDTO fingerprintDetailsDTO, String[] path,
			String fingerType) throws RegBaseCheckedException {
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID, "Reading scanned Finger has started");

		try {

			List<BiometricExceptionDTO> biometricExceptionDTOs;

			if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				biometricExceptionDTOs = ((BiometricDTO) SessionContext.map()
						.get(RegistrationConstants.USER_ONBOARD_DATA)).getOperatorBiometricDTO()
								.getBiometricExceptionDTO();
			} else if (((RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA))
					.isUpdateUINNonBiometric() || (boolean) SessionContext.map().get(RegistrationConstants.IS_Child)) {
				biometricExceptionDTOs = ((RegistrationDTO) SessionContext.map()
						.get(RegistrationConstants.REGISTRATION_DATA)).getBiometricDTO().getIntroducerBiometricDTO()
								.getBiometricExceptionDTO();
			} else {
				biometricExceptionDTOs = ((RegistrationDTO) SessionContext.map()
						.get(RegistrationConstants.REGISTRATION_DATA)).getBiometricDTO().getApplicantBiometricDTO()
								.getBiometricExceptionDTO();
			}

			if (!isMdmEnabled()) {

				prepareSegmentedBiometrics(fingerprintDetailsDTO, path, biometricExceptionDTOs);
			}

		} catch (IOException ioException) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_FINGERPRINT_SCANNING_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_FINGERPRINT_SCANNING_ERROR.getErrorMessage());
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID, String.format(
					"Exception while reading scanned fingerprints details for user registration: %s caused by %s",
					runtimeException.getMessage(), runtimeException.getCause()));

			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_FINGERPRINT_SCAN_EXP, String.format(
					"Exception while reading scanned fingerprints details for user registration: %s caused by %s",
					runtimeException.getMessage(), runtimeException.getCause()));
		}
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID, "Reading scanned Finger has ended");
	}

	/**
	 * Preparing segmentation detail of Biometric
	 * 
	 * @param fingerprintDetailsDTO
	 * @param path
	 * @param biometricExceptionDTOs
	 * @throws IOException
	 */
	private void prepareSegmentedBiometrics(FingerprintDetailsDTO fingerprintDetailsDTO, String[] path,
			List<BiometricExceptionDTO> biometricExceptionDTOs) throws IOException {
		List<String> filePaths = Arrays.asList(path);

		boolean isExceptionFinger = false;

		for (String folderPath : filePaths) {
			isExceptionFinger = false;
			String[] imageFileName = folderPath.split("/");

			for (BiometricExceptionDTO exceptionDTO : biometricExceptionDTOs) {

				if (imageFileName[3].equals(exceptionDTO.getMissingBiometric())) {
					isExceptionFinger = true;
					break;
				}
			}
			if (!isExceptionFinger) {
				FingerprintDetailsDTO segmentedDetailsDTO = new FingerprintDetailsDTO();

				byte[] isoTemplateBytes = IOUtils
						.resourceToByteArray(folderPath.concat(RegistrationConstants.ISO_FILE));
				segmentedDetailsDTO.setFingerPrint(isoTemplateBytes);

				byte[] isoImageBytes = IOUtils
						.resourceToByteArray(folderPath.concat(RegistrationConstants.ISO_IMAGE_FILE));
				segmentedDetailsDTO.setFingerPrintISOImage(isoTemplateBytes);

				segmentedDetailsDTO.setFingerType(imageFileName[3]);
				segmentedDetailsDTO.setFingerprintImageName(imageFileName[3]);
				segmentedDetailsDTO.setNumRetry(fingerprintDetailsDTO.getNumRetry());
				segmentedDetailsDTO.setForceCaptured(false);
				segmentedDetailsDTO.setQualityScore(90);

				if (fingerprintDetailsDTO.getSegmentedFingerprints() == null) {
					List<FingerprintDetailsDTO> segmentedFingerprints = new ArrayList<>(5);
					fingerprintDetailsDTO.setSegmentedFingerprints(segmentedFingerprints);
				}
				fingerprintDetailsDTO.getSegmentedFingerprints().add(segmentedDetailsDTO);
			}
		}
	}

	/**
	 * Capture Iris
	 * 
	 * @return byte[] of captured Iris
	 * @throws IOException
	 */
	private void captureIris(IrisDetailsDTO IrisDetailsDTO, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException {

		LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID, "Stub data for Iris");

		byte[] capturedByte = null;
		BufferedImage bufferedImage = null;

		if (isMdmEnabled()) {
			CaptureResponseDto captureResponseDto = mosipBioDeviceManager.authScan(requestDetail);
			if (captureResponseDto == null)
				throw new RegBaseCheckedException("202", "Decice is not available");
			if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
				throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
						captureResponseDto.getError().getErrorInfo());
			capturedByte = Base64.getDecoder().decode(captureResponseDto.getMosipBioDeviceDataResponses().get(0).getCaptureResponseData().getBioExtract());
			IrisDetailsDTO.setIrisType(
					captureResponseDto.getMosipBioDeviceDataResponses().get(0).getCaptureResponseData().getBioSubType());
			IrisDetailsDTO.setIrisIso(capturedByte);
		} else {
			bufferedImage = ImageIO.read(this.getClass().getResourceAsStream(RegistrationConstants.IRIS_IMAGE_LOCAL));
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
			capturedByte = byteArrayOutputStream.toByteArray();
			IrisDetailsDTO.setIrisIso(capturedByte);
			IrisDetailsDTO.setIrisType("Left Eye");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateFace(java.lang.String)
	 */
	@Override
	public boolean validateFace(AuthenticationValidatorDTO authenticationValidatorDTO) {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Authenticating face");

		return authService.authValidator(RegistrationConstants.FACE, authenticationValidatorDTO);
	}

	public AuthenticationValidatorDTO getFaceAuthenticationDto(String userId, RequestDetail requestDetail)
			throws RegBaseCheckedException, IOException {
		AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
		FaceDetailsDTO faceDetailsDTO = new FaceDetailsDTO();
		CaptureResponseDto captureResponseDto = null;
		captureResponseDto = mosipBioDeviceManager.regScan(requestDetail);
		if (captureResponseDto == null)
			throw new RegBaseCheckedException("202", "Decice is not available");
		if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
			throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
					captureResponseDto.getError().getErrorInfo());

		byte[] faceBytes = mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto);
		if (null != faceBytes) {
			faceDetailsDTO.setFaceISO(faceBytes);
		} else {
			faceDetailsDTO.setFaceISO(RegistrationConstants.FACE.toLowerCase().getBytes());
		}
		authenticationValidatorDTO.setUserId(userId);
		authenticationValidatorDTO.setFaceDetail(faceDetailsDTO);
		authenticationValidatorDTO.setAuthValidationFlag(true);
		return authenticationValidatorDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.bio.BioService#getIrisImageAsDTO(io.mosip.
	 * registration.dto.biometric.IrisDetailsDTO, java.lang.String)
	 */
	@Override
	public void getIrisImageAsDTO(IrisDetailsDTO irisDetailsDTO, RequestDetail requestDetail, int leftEyeAttempt,
			int rightEyeAttempt) throws RegBaseCheckedException, IOException {

		if (isNull(requestDetail.getType()))
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_IMAGE);

		if (isMdmEnabled()) {
			requestDetail.setType(RegistrationConstants.IRIS_DOUBLE);
			getIrisImageAsDTOWithMdm(irisDetailsDTO, requestDetail, leftEyeAttempt, rightEyeAttempt);
		} else
			getIrisImageAsDTONonMdm(irisDetailsDTO, requestDetail.getType());
	}

	/**
	 * Get the Iris Image with MDM
	 * 
	 * @param detailsDTO
	 * @param eyeType
	 * @throws RegBaseCheckedException
	 * @throws IOException
	 */
	private void getIrisImageAsDTOWithMdm(IrisDetailsDTO detailsDTO, RequestDetail requestDetail, int leftEyeAttempt,
			int rightEyeAttempt) throws RegBaseCheckedException, IOException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Entering into getIrisImageAsDTOWithMdm method..");
		if (isNull(requestDetail.getType()))
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_IMAGE);
		requestDetail.setType(RegistrationConstants.IRIS_DOUBLE);
		CaptureResponseDto captureResponseDto = mosipBioDeviceManager.regScan(requestDetail);
		if (captureResponseDto == null)
			throw new RegBaseCheckedException("202", "Device is not available");
		if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
			throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
					captureResponseDto.getError().getErrorInfo());

		detailsDTO.setIrises(new ArrayList<IrisDetailsDTO>());

		List<CaptureResponseBioDto> mosipBioDeviceDataResponses = captureResponseDto.getMosipBioDeviceDataResponses();
		mosipBioDeviceDataResponses.forEach(captured -> {
			IrisDetailsDTO irisDetails = new IrisDetailsDTO();
			CaptureResponsBioDataDto captureRespoonse = captured.getCaptureResponseData();

			int attempt = captureRespoonse.getBioSubType().equals(RegistrationConstants.LEFT_EYE) ? leftEyeAttempt
					: rightEyeAttempt;

			setBioQualityScores(captureRespoonse.getBioSubType(), attempt, Integer.parseInt(captureRespoonse.getQualityScore()));
			setBioStreamImages(convertBytesToImage(Base64.getDecoder().decode(captureRespoonse.getBioValue())), captureRespoonse.getBioSubType(), attempt);
		
			// Get Best Capture
			captureRespoonse = getBestCapture(captureRespoonse);

			irisDetails.setIrisIso((Base64.getDecoder().decode(captureRespoonse.getBioExtract())));
			irisDetails.setIrisImageName(captureRespoonse.getBioSubType());
			irisDetails.setIris(Base64.getDecoder().decode(captureRespoonse.getBioValue()));
			irisDetails.setQualityScore(Integer.parseInt(captureRespoonse.getQualityScore()));
			irisDetails.setIrisType(captureRespoonse.getBioSubType());
			irisDetails.setCaptured(true);
			detailsDTO.getIrises().add(irisDetails);

			
		});
		detailsDTO.setCaptured(true);
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Leaving into getIrisImageAsDTOWithMdm method..");
	}

	/**
	 * Convert bytes to image.
	 *
	 * @param imageBytes
	 *            the image bytes
	 * @return the image
	 */
	protected Image convertBytesToImage(byte[] imageBytes) {
		Image image = null;
		if (imageBytes != null) {
			image = new Image(new ByteArrayInputStream(imageBytes));
		}
		return image;
	}

	/**
	 * Gets the iris stub image as DTO without MDM
	 *
	 * @param irisDetailsDTO
	 *            the iris details DTO
	 * @param irisType
	 *            the iris type
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private void getIrisImageAsDTONonMdm(IrisDetailsDTO irisDetails, String irisType) throws RegBaseCheckedException {
		try {
			LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Stubbing iris details for user registration");

			if (isNull(irisType))
				throwRegBaseCheckedException(RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_IMAGE);

			Map<String, Object> scannedIrisMap = getIrisScannedImage(irisType);
			double qualityScore = 0;
			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				qualityScore = (double) scannedIrisMap.get(RegistrationConstants.IMAGE_SCORE_KEY);
			}

			IrisDetailsDTO irisDetailsDTO = new IrisDetailsDTO();
			if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)
					|| Double.compare(irisDetailsDTO.getQualityScore(), qualityScore) < 0) {
				// Set the values in IrisDetailsDTO object
				irisDetailsDTO.setIris((byte[]) scannedIrisMap.get(RegistrationConstants.IMAGE_BYTE_ARRAY_KEY));
				irisDetailsDTO.setIrisIso((byte[]) scannedIrisMap.get(RegistrationConstants.IMAGE_BYTE_ISO));
				irisDetailsDTO.setForceCaptured(false);
				irisDetailsDTO.setIrisImageName(irisType.concat(RegistrationConstants.DOT)
						.concat((String) scannedIrisMap.get(RegistrationConstants.IMAGE_FORMAT_KEY)));
				irisDetailsDTO.setIrisType(irisType);
				if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
					irisDetailsDTO.setQualityScore(qualityScore);
				}
				irisDetailsDTO.setQualityScore(91.0);
				irisDetails.setIrises(new ArrayList<IrisDetailsDTO>());
				irisDetails.getIrises().add(irisDetailsDTO);
				irisDetails.setCaptured(true);
			}

			LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Stubbing iris details for user registration completed");
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_IRIS_SCAN_EXP,
					String.format("Exception while stubbing the iris details for user registration: %s caused by %s",
							runtimeException.getMessage(), runtimeException.getCause()));
		}
	}

	private Map<String, Object> getIrisScannedImage(String irisType) throws RegBaseCheckedException {
		try {
			LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Scanning of iris details for user registration");

			if (isNull(irisType))
				throwRegBaseCheckedException(RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_IMAGE);

			double qualityScore;
			byte iso[];
			BufferedImage bufferedImage;
			if (irisType.equalsIgnoreCase(RegistrationConstants.TEMPLATE_LEFT_EYE)) {
				bufferedImage = ImageIO
						.read(this.getClass().getResourceAsStream(RegistrationConstants.IRIS_IMAGE_LOCAL));
				qualityScore = 90.5;
				iso = IOUtils.resourceToByteArray(RegistrationConstants.LEFT_EYE_ISO);
			} else {
				bufferedImage = ImageIO
						.read(this.getClass().getResourceAsStream(RegistrationConstants.IRIS_IMAGE_LOCAL_RIGHT));
				iso = IOUtils.resourceToByteArray(RegistrationConstants.RIGHT_EYE_ISO);
				qualityScore = 50.0;
			}

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);

			byte[] scannedIrisBytes = byteArrayOutputStream.toByteArray();

			// Add image format, image and quality score in bytes array to map
			Map<String, Object> scannedIris = new WeakHashMap<>();
			scannedIris.put(RegistrationConstants.IMAGE_FORMAT_KEY, RegistrationConstants.IMAGE_FORMAT_PNG);
			scannedIris.put(RegistrationConstants.IMAGE_BYTE_ARRAY_KEY, scannedIrisBytes);
			scannedIris.put(RegistrationConstants.IMAGE_BYTE_ISO, iso);
			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				scannedIris.put(RegistrationConstants.IMAGE_SCORE_KEY, qualityScore);
			}

			LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Scanning of iris details for user registration completed");

			return scannedIris;
		} catch (IOException ioException) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_IRIS_SCANNING_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_IRIS_SCANNING_ERROR.getErrorMessage());
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_IRIS_STUB_IMAGE_EXP,
					String.format("Exception while scanning iris details for user registration: %s caused by %s",
							runtimeException.getMessage(), runtimeException.getCause()));
		}
	}

	/**
	 * Capture Face
	 * 
	 * @return byte[] of captured Face
	 * @throws IOException 
	 * @throws RegBaseCheckedException 
	 */
	@Override
	public CaptureResponseDto captureFace(RequestDetail requestDetail) throws RegBaseCheckedException, IOException {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Entering Capture Face Method");
			return  mosipBioDeviceManager.regScan(requestDetail);
	}

	@Override
	public byte[] getSingleBioValue(CaptureResponseDto captureResponseDto) {

		return mosipBioDeviceManager.getSingleBioValue(captureResponseDto);
	}

	@Override
	public byte[] getSingleBiometricIsoTemplate(CaptureResponseDto captureResponseDto) {

		return mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateFP(io.mosip.registration
	 * .dto.biometric.FingerprintDetailsDTO, java.util.List)
	 */
	@Override
	public boolean validateFP(FingerprintDetailsDTO fingerprintDetailsDTO, List<UserBiometric> userFingerprintDetails) {
		FingerprintTemplate fingerprintTemplate = new FingerprintTemplate()
				.convert(fingerprintDetailsDTO.getFingerPrint());
		String minutiae = fingerprintTemplate.serialize();
		int fingerPrintScore = Integer
				.parseInt(String.valueOf(ApplicationContext.map().get(RegistrationConstants.FINGER_PRINT_SCORE)));
		userFingerprintDetails.forEach(fingerPrintTemplateEach -> {
			if (fingerprintProvider.scoreCalculator(minutiae,
					fingerPrintTemplateEach.getBioMinutia()) > fingerPrintScore) {
				fingerprintDetailsDTO.setFingerType(fingerPrintTemplateEach.getUserBiometricId().getBioAttributeCode());
			}
		});
		return userFingerprintDetails.stream()
				.anyMatch(bio -> fingerprintProvider.scoreCalculator(minutiae, bio.getBioMinutia()) > fingerPrintScore);
	}

	private CaptureResponsBioDataDto getBestCapture(CaptureResponsBioDataDto captureResponsBioDataDto) {

		// Get Stored Capture of bio type
		CaptureResponsBioDataDto responsBioDataDtoStored = BEST_CAPTURES.get(captureResponsBioDataDto.getBioSubType());

		if (responsBioDataDtoStored != null) {
			int qualityScore = Integer.parseInt(captureResponsBioDataDto.getQualityScore());

			int qualityScoreStored = Integer.parseInt(responsBioDataDtoStored.getQualityScore());

			// If Better capture found, store it
			if (qualityScoreStored < qualityScore) {

				BEST_CAPTURES.put(captureResponsBioDataDto.getBioSubType(), captureResponsBioDataDto);

			}
		} else {

			// If Not stored, store it
			BEST_CAPTURES.put(captureResponsBioDataDto.getBioSubType(), captureResponsBioDataDto);

		}

		return BEST_CAPTURES.get(captureResponsBioDataDto.getBioSubType());
	}

	@Override
	public Double getHighQualityScoreByBioType(String bioType, Double qualityScore) {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Getting highest quality score for : " + bioType );
		
		try {
		qualityScore = BIO_QUALITY_SCORE.get(bioType).entrySet().stream().max(Comparator.comparing(Map.Entry::getValue)).get()
				.getValue();
		}catch(Exception e) {
			return qualityScore;
		}
		
		return qualityScore;

	}

	public void setBioQualityScores(String bioType, int attempt, double qualityScore) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Started Set Quality Score of : " + bioType + " for attempt : " + attempt);

		Map<Integer, Double> bioData = null;

		if (BIO_QUALITY_SCORE.get(bioType) != null) {
			bioData = BIO_QUALITY_SCORE.get(bioType);

		} else {

			bioData = new HashMap<Integer, Double>();

		}

		bioData.put(attempt, qualityScore);

		BIO_QUALITY_SCORE.put(bioType, bioData);

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Completed Set Stream image of : " + bioType + " for attempt : " + attempt);

	}

	public Double getBioQualityScores(String bioType, int attempt) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Get Stream  Quality Score of : " + bioType + " for attempt : " + attempt);

		if (BIO_QUALITY_SCORE.get(bioType) != null) {
			return BIO_QUALITY_SCORE.get(bioType).get(attempt);
		}

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"NOT FOUND : Stream image of : " + bioType + " for attempt : " + attempt);

		return null;

	}

	public static void clearBIOScoreByBioType(List<String> captures) {
		
		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Clearing bio scores of : "+captures);

		captures.forEach(key -> BIO_QUALITY_SCORE.remove(key));
		
	}
	
	public static void setBioStreamImages(Image image, String bioType, int attempt) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Started Set Stream image of : " + bioType + " for attempt : " + attempt);

		Map<Integer, Image> bioImage = null;

		if (BIO_STREAM_IMAGES.get(bioType) != null) {
			bioImage = BIO_STREAM_IMAGES.get(bioType);

		} else {

			bioImage = new HashMap<Integer, Image>();

		}

		//image = image == null ? streamImage : image;
		bioImage.put(attempt, image);

		BIO_STREAM_IMAGES.put(bioType, bioImage);

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Completed Set Stream image of : " + bioType + " for attempt : " + attempt);

	}

	
	
	
public static void clearBIOStreamImagesByBioType(List<String> captures) {
		
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Clearing Stream images of : " + captures);
		
		captures.forEach(key -> BIO_STREAM_IMAGES.remove(key));
		
	}

	public static void clearAllStreamImages() {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Clearing all Stream images");

		BIO_STREAM_IMAGES.clear();
	}

	public Image getBioStreamImage(String bioType, int attempt) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Get Stream  Quality Score of : " + bioType + " for attempt : " + attempt);

		if (BIO_STREAM_IMAGES.get(bioType) != null) {
			if(!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))
				return BIO_STREAM_IMAGES.get(bioType).get(attempt);
			return BIO_STREAM_IMAGES.get(bioType).get(1);
		}

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"NOT FOUND : Stream image of : " + bioType + " for attempt : " + attempt);

		return null;

	}

}
