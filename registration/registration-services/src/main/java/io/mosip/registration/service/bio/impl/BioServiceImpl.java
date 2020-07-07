package io.mosip.registration.service.bio.impl;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_FACADE;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_FACADE;
import static io.mosip.registration.constants.LoggerConstants.STREAMER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.constants.Biometric;
import io.mosip.kernel.packetmanager.constants.PacketManagerConstants;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.biometric.BiometricExceptionDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.CaptureResponsBioDataDto;
import io.mosip.registration.mdm.dto.CaptureResponseBioDto;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.bio.BioService;

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
	private MosipBioDeviceManager mosipBioDeviceManager;

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	// @Autowired
	// private AuthenticationService authService;

	// @Autowired
	// private FingerprintProvider fingerprintProvider;

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BioServiceImpl.class);

	// private byte[] isoImage;

	/** The finger print capture service impl. */
	// @Autowired
	// private AuthenticationService authenticationService;

	private static HashMap<String, CaptureResponsBioDataDto> BEST_CAPTURES = new HashMap<>();

	private static Map<String, Map<Integer, Double>> BIO_QUALITY_SCORE = new HashMap<String, Map<Integer, Double>>();

	private static Map<String, Map<Integer, byte[]>> BIO_STREAM_IMAGES = new HashMap<String, Map<Integer, byte[]>>();

	private static Map<String, List<String>> lowQualityBiometrics = new HashMap<>();

	private static List<String> bioAttributes = new LinkedList<>();

	public static List<String> getBioAttributes() {
		return bioAttributes;
	}

	public static void setBioAttributes(List<String> bioAttributes) {
		BioServiceImpl.bioAttributes = bioAttributes;
	}

	public Map<String, List<String>> getLowQualityBiometrics() {
		return lowQualityBiometrics;
	}

	public static HashMap<String, CaptureResponsBioDataDto> getBestCaptures() {
		return BEST_CAPTURES;
	}

	public static void clearCaptures(List<String> captures) {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Clearing caprures of : " + captures);

		captures.forEach(key -> BEST_CAPTURES.remove(key));

	}

	public static void clearAllCaptures() {

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Clearing All captures");

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
	/*
	 * @Override public AuthenticationValidatorDTO
	 * getFingerPrintAuthenticationDto(String userId, RequestDetail requestDetail)
	 * throws RegBaseCheckedException, IOException {
	 * 
	 * LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
	 * "Invoking FingerPrint validator");
	 * 
	 * if (isNull(userId)) throwRegBaseCheckedException(
	 * RegistrationExceptionConstants.
	 * REG_MASTER_BIO_SERVICE_IMPL_FINGERPRINT_AUTTHENTICATION);
	 * 
	 * AuthenticationValidatorDTO authenticationValidatorDTO = null;
	 * CaptureResponseDto captureResponseDto = null; CaptureResponsBioDataDto
	 * captureResponseData = null; List<FingerprintDetailsDTO>
	 * fingerprintDetailsDTOs = new ArrayList<>(); String bioType = "Right little";
	 * if (isMdmEnabled()) { captureResponseDto =
	 * mosipBioDeviceManager.authScan(requestDetail); if (captureResponseDto ==
	 * null) throw new RegBaseCheckedException("202", "Decice is not available"); if
	 * (captureResponseDto.getError().getErrorCode().matches(
	 * "102|101|202|403|404|409")) throw new
	 * RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
	 * captureResponseDto.getError().getErrorInfo());
	 * 
	 * captureResponseDto.getMosipBioDeviceDataResponses().forEach(auth -> { if
	 * (isQualityScoreMaxInclusive(auth.getCaptureResponseData().getQualityScore()))
	 * { FingerprintDetailsDTO fingerprintDetailsDTO = new FingerprintDetailsDTO();
	 * fingerprintDetailsDTO.setFingerPrintISOImage(
	 * Base64.getUrlDecoder().decode(auth.getCaptureResponseData().getBioExtract()))
	 * ; fingerprintDetailsDTO.setFingerType(auth.getCaptureResponseData().
	 * getBioSubType()); fingerprintDetailsDTO.setForceCaptured(true); int
	 * qualityScore = 0; try { qualityScore =
	 * Integer.parseInt(auth.getCaptureResponseData().getQualityScore()); } catch
	 * (Exception exception) { LOGGER.info(LoggerConstants.BIO_SERVICE,
	 * APPLICATION_NAME, APPLICATION_ID,
	 * "Did not recieve quality for the segment, hence setting the minimum quality"
	 * ); } if (qualityScore > Integer.parseInt((String) ApplicationContext.map()
	 * .get(RegistrationConstants.FINGERPRINT_AUTHENTICATION_THRESHHOLD)))
	 * fingerprintDetailsDTOs.add(fingerprintDetailsDTO); } });
	 * 
	 * LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
	 * "Calling for finger print validation through authService");
	 * authenticationValidatorDTO = new AuthenticationValidatorDTO();
	 * authenticationValidatorDTO.setFingerPrintDetails(fingerprintDetailsDTOs);
	 * authenticationValidatorDTO.setUserId(userId);
	 * authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.
	 * MULTIPLE); authenticationValidatorDTO.setAuthValidationFlag(true);
	 * validateFingerPrint(authenticationValidatorDTO);
	 * 
	 * } else { isoImage = IOUtils.toByteArray( this.getClass().getResourceAsStream(
	 * "/UserOnboard/rightHand/rightLittle/ISOTemplate.iso")); }
	 * 
	 * LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
	 * "End FingerPrint validator");
	 * 
	 * return authenticationValidatorDTO; }
	 */

	private boolean isQualityScoreMaxInclusive(String qualityScore) {
		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Checking for max inclusive quality score");

		if (qualityScore == null) {
			return false;
		}
		return Double.parseDouble(qualityScore) <= RegistrationConstants.MAX_BIO_QUALITY_SCORE;
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
	/*
	 * public boolean validateFingerPrint(AuthenticationValidatorDTO
	 * authenticationValidatorDTO) { return
	 * authService.authValidator(RegistrationConstants.FINGERPRINT,
	 * authenticationValidatorDTO); }
	 */

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
	/*
	 * @Override public AuthenticationValidatorDTO getIrisAuthenticationDto(String
	 * userId, RequestDetail requestDetail) throws RegBaseCheckedException,
	 * IOException {
	 * 
	 * LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
	 * "Scanning Iris"); if (isNull(userId)) throwRegBaseCheckedException(
	 * RegistrationExceptionConstants.
	 * REG_MASTER_BIO_SERVICE_IMPL_IRIS_AUTHENTICATION);
	 * 
	 * AuthenticationValidatorDTO authenticationValidatorDTO = new
	 * AuthenticationValidatorDTO(); List<IrisDetailsDTO> irisDetailsDTOs = new
	 * ArrayList<>(); IrisDetailsDTO irisDetailsDTO = new IrisDetailsDTO();
	 * captureIris(irisDetailsDTO, requestDetail);
	 * irisDetailsDTOs.add(irisDetailsDTO);
	 * authenticationValidatorDTO.setUserId(userId);
	 * authenticationValidatorDTO.setIrisDetails(irisDetailsDTOs);
	 * authenticationValidatorDTO.setAuthValidationFlag(true);
	 * authenticationValidatorDTO.setAuthValidationType(RegistrationConstants.SINGLE
	 * );
	 * 
	 * LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
	 * "Iris scan done"); return authenticationValidatorDTO;
	 * 
	 * }
	 */

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
	/*
	 * public boolean validateIris(AuthenticationValidatorDTO
	 * authenticationValidatorDTO) { return
	 * authService.authValidator(RegistrationConstants.IRIS,
	 * authenticationValidatorDTO); }
	 */

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
	public FingerprintDetailsDTO getFingerPrintImageAsDTOWithMdm(RequestDetail requestDetail, int attempt)
			throws RegBaseCheckedException, IOException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Entering into getFingerPrintImageAsDTOWithMdm method..");
		CaptureResponseDto captureResponseDto = mosipBioDeviceManager.regScan(requestDetail);
		if (captureResponseDto == null)
			throw new RegBaseCheckedException("202", "Decice is not available");
		if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
			throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
					captureResponseDto.getError().getErrorInfo());

		FingerprintDetailsDTO fpDetailsDTO = new FingerprintDetailsDTO();
		fpDetailsDTO.setSegmentedFingerprints(new ArrayList<FingerprintDetailsDTO>());
		List<CaptureResponseBioDto> mosipBioDeviceDataResponses = captureResponseDto.getMosipBioDeviceDataResponses();

		int currentCaptureQualityScore = 0;

		for (CaptureResponseBioDto captured : mosipBioDeviceDataResponses) {

			FingerprintDetailsDTO fingerPrintDetail = new FingerprintDetailsDTO();
			CaptureResponsBioDataDto captureResponse = captured.getCaptureResponseData();

			if (captureResponse != null && isQualityScoreMaxInclusive(captureResponse.getQualityScore())) {
				currentCaptureQualityScore += Integer.parseInt(captureResponse.getQualityScore());

				// Get Best Capture
				captureResponse = getBestCapture(captureResponse);

				fingerPrintDetail
						.setFingerPrintISOImage(Base64.getUrlDecoder().decode(captureResponse.getBioExtract()));
				fingerPrintDetail.setFingerType(captureResponse.getBioSubType());
				fingerPrintDetail.setFingerPrint(Base64.getUrlDecoder().decode(captureResponse.getBioValue()));
				fingerPrintDetail.setQualityScore(Integer.parseInt(captureResponse.getQualityScore()));
				fingerPrintDetail.setFingerprintImageName("FingerPrint " + captureResponse.getBioSubType());
				fpDetailsDTO.getSegmentedFingerprints().add(fingerPrintDetail);
			} else {
				fpDetailsDTO.setCaptured(false);
				return fpDetailsDTO;
			}
		}

		setBioQualityScores(requestDetail.getType(), attempt,
				currentCaptureQualityScore / (double) mosipBioDeviceDataResponses.size());

		double slapQuality = (fpDetailsDTO.getSegmentedFingerprints().stream()
				.mapToDouble(finger -> finger.getQualityScore()).sum())
				/ fpDetailsDTO.getSegmentedFingerprints().size();

		fpDetailsDTO.setCaptured(true);
		fpDetailsDTO.setFingerType(requestDetail.getType());
		fpDetailsDTO.setNumRetry(attempt);
		fpDetailsDTO.setQualityScore(slapQuality);

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Leaving getFingerPrintImageAsDTOWithMdm..");

		return fpDetailsDTO;
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
	private FingerprintDetailsDTO getFingerPrintImageAsDTONonMdm(RequestDetail requestDetail, int attempt)
			throws RegBaseCheckedException {

		Map<String, Object> fingerMap = null;

		FingerprintDetailsDTO fpDetailsDTO = new FingerprintDetailsDTO();

		try {

			if (requestDetail.getType().equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
				fingerMap = getFingerPrintScannedImageWithStub(RegistrationConstants.LEFTHAND_SLAP_FINGERPRINT_PATH);
			} else if (requestDetail.getType().equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				fingerMap = getFingerPrintScannedImageWithStub(RegistrationConstants.RIGHTHAND_SLAP_FINGERPRINT_PATH);
			} else if (requestDetail.getType().equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
				fingerMap = getFingerPrintScannedImageWithStub(RegistrationConstants.BOTH_THUMBS_FINGERPRINT_PATH);
			}

			if ((fingerMap != null)) {
				fpDetailsDTO.setFingerPrint((byte[]) fingerMap.get(RegistrationConstants.IMAGE_BYTE_ARRAY_KEY));
				fpDetailsDTO.setFingerprintImageName(requestDetail.getType().concat(RegistrationConstants.DOT)
						.concat((String) fingerMap.get(RegistrationConstants.IMAGE_FORMAT_KEY)));
				fpDetailsDTO.setFingerType(requestDetail.getType());
				fpDetailsDTO.setForceCaptured(false);
				fpDetailsDTO.setCaptured(true);
				fpDetailsDTO.setNumRetry(attempt);
				if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
					fpDetailsDTO.setQualityScore((double) fingerMap.get(RegistrationConstants.IMAGE_SCORE_KEY));
				}
			}

		} finally {
			if (fingerMap != null && !fingerMap.isEmpty()) {
				fingerMap.clear();
			}
		}

		return fpDetailsDTO;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#getFingerPrintImageAsDTO(io.
	 * mosip.registration.dto.biometric.FingerprintDetailsDTO,
	 * io.mosip.registration.mdm.dto.RequestDetail, int)
	 */
	public FingerprintDetailsDTO getFingerPrintImageAsDTO(RequestDetail requestDetail, int attempt)
			throws RegBaseCheckedException, IOException {
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
				"Entering into BioServiceImpl-FingerPrintImageAsDTO");
		if (isMdmEnabled())
			return getFingerPrintImageAsDTOWithMdm(requestDetail, attempt);

		else {
			return getFingerPrintImageAsDTONonMdm(requestDetail, attempt);
		}

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

				String fingerprintImageName = imageFileName[3];

				fingerprintImageName = getSegmentedFingerName(fingerprintImageName);
				segmentedDetailsDTO.setFingerType(fingerprintImageName);
				segmentedDetailsDTO.setFingerprintImageName(fingerprintImageName);
				segmentedDetailsDTO.setNumRetry(fingerprintDetailsDTO.getNumRetry());
				segmentedDetailsDTO.setForceCaptured(false);
				segmentedDetailsDTO.setQualityScore(90);

				// if (fingerprintImageName.equals("Left Index")) {
				// segmentedDetailsDTO.setQualityScore(20);
				// }

				if (fingerprintDetailsDTO.getSegmentedFingerprints() == null) {
					List<FingerprintDetailsDTO> segmentedFingerprints = new ArrayList<>(5);
					fingerprintDetailsDTO.setSegmentedFingerprints(segmentedFingerprints);
				}
				fingerprintDetailsDTO.getSegmentedFingerprints().add(segmentedDetailsDTO);
			}
		}
	}

	private String getSegmentedFingerName(String fingerprintImageName) {
		return fingerprintImageName.contains("rightIndex") ? RegistrationConstants.RightIndex
				: fingerprintImageName.contains("leftIndex") ? RegistrationConstants.LeftIndex
						: fingerprintImageName.contains("rightMiddle") ? RegistrationConstants.RightMiddle
								: fingerprintImageName.contains("leftMiddle") ? RegistrationConstants.LeftMiddle
										: fingerprintImageName.contains("rightLittle")
												? RegistrationConstants.RightLittle
												: fingerprintImageName.contains("leftLittle")
														? RegistrationConstants.LeftLittle
														: fingerprintImageName.contains("rightRing")
																? RegistrationConstants.RightRing
																: fingerprintImageName.contains("leftRing")
																		? RegistrationConstants.LeftRing
																		: fingerprintImageName.contains("rightThumb")
																				? RegistrationConstants.RightThumb
																				: fingerprintImageName
																						.contains("leftThumb")
																								? RegistrationConstants.LeftThumb
																								: fingerprintImageName;
	}

	/**
	 * Capture Iris
	 * 
	 * @return byte[] of captured Iris
	 * @throws IOException
	 */
	/*
	 * private void captureIris(IrisDetailsDTO irisDetailsDTO, RequestDetail
	 * requestDetail) throws RegBaseCheckedException, IOException {
	 * 
	 * LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID,
	 * "Stub data for Iris");
	 * 
	 * byte[] capturedByte = null; BufferedImage bufferedImage = null;
	 * 
	 * if (isMdmEnabled()) { CaptureResponseDto captureResponseDto =
	 * mosipBioDeviceManager.authScan(requestDetail); if (captureResponseDto ==
	 * null) throw new RegBaseCheckedException("202", "Decice is not available"); if
	 * (captureResponseDto.getError().getErrorCode().matches(
	 * "102|101|202|403|404|409")) throw new
	 * RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
	 * captureResponseDto.getError().getErrorInfo()); capturedByte =
	 * Base64.getDecoder().decode(captureResponseDto.getMosipBioDeviceDataResponses(
	 * ).get(0) .getCaptureResponseData().getBioExtract()); CaptureResponsBioDataDto
	 * captureResponsBioDataDto =
	 * captureResponseDto.getMosipBioDeviceDataResponses()
	 * .get(0).getCaptureResponseData(); if
	 * (isQualityScoreMaxInclusive(captureResponsBioDataDto.getQualityScore())) {
	 * irisDetailsDTO.setIrisType(captureResponseDto.getMosipBioDeviceDataResponses(
	 * ).get(0) .getCaptureResponseData().getBioSubType());
	 * irisDetailsDTO.setIrisIso(capturedByte); } } else { bufferedImage =
	 * ImageIO.read(this.getClass().getResourceAsStream(RegistrationConstants.
	 * IRIS_IMAGE_LOCAL)); ByteArrayOutputStream byteArrayOutputStream = new
	 * ByteArrayOutputStream(); ImageIO.write(bufferedImage,
	 * RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream); capturedByte
	 * = byteArrayOutputStream.toByteArray();
	 * irisDetailsDTO.setIrisIso(capturedByte);
	 * irisDetailsDTO.setIrisType("Left Eye"); }
	 * 
	 * }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateFace(java.lang.String)
	 */
	/*
	 * @Override public boolean validateFace(AuthenticationValidatorDTO
	 * authenticationValidatorDTO) {
	 * 
	 * LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
	 * "Authenticating face");
	 * 
	 * return authService.authValidator(RegistrationConstants.FACE,
	 * authenticationValidatorDTO); }
	 * 
	 * public AuthenticationValidatorDTO getFaceAuthenticationDto(String userId,
	 * RequestDetail requestDetail) throws RegBaseCheckedException, IOException {
	 * AuthenticationValidatorDTO authenticationValidatorDTO = new
	 * AuthenticationValidatorDTO(); FaceDetailsDTO faceDetailsDTO = new
	 * FaceDetailsDTO(); CaptureResponseDto captureResponseDto = null;
	 * captureResponseDto = mosipBioDeviceManager.regScan(requestDetail); if
	 * (captureResponseDto == null) throw new RegBaseCheckedException("202",
	 * "Decice is not available"); if
	 * (captureResponseDto.getError().getErrorCode().matches(
	 * "102|101|202|403|404|409")) throw new
	 * RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
	 * captureResponseDto.getError().getErrorInfo());
	 * 
	 * byte[] faceBytes =
	 * mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto); if
	 * (null != faceBytes) { faceDetailsDTO.setFaceISO(faceBytes); } else {
	 * faceDetailsDTO.setFaceISO(RegistrationConstants.FACE.toLowerCase().getBytes()
	 * ); } authenticationValidatorDTO.setUserId(userId);
	 * authenticationValidatorDTO.setFaceDetail(faceDetailsDTO);
	 * authenticationValidatorDTO.setAuthValidationFlag(true); return
	 * authenticationValidatorDTO; }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.bio.BioService#getIrisImageAsDTO(io.mosip.
	 * registration.dto.biometric.IrisDetailsDTO, java.lang.String)
	 */
	@Override
	public IrisDetailsDTO getIrisImageAsDTO(RequestDetail requestDetail, int leftEyeAttempt, int rightEyeAttempt)
			throws RegBaseCheckedException, IOException {

		if (isNull(requestDetail.getType()))
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_IMAGE);

		if (isMdmEnabled()) {
			requestDetail.setType(RegistrationConstants.IRIS_DOUBLE);
			return getIrisImageAsDTOWithMdm(requestDetail, leftEyeAttempt, rightEyeAttempt);
		} else {
			return getIrisImageAsDTONonMdm(requestDetail.getType(), leftEyeAttempt, rightEyeAttempt);
		}

	}

	/**
	 * Get the Iris Image with MDM
	 * 
	 * @param detailsDTO
	 * @param eyeType
	 * @throws RegBaseCheckedException
	 * @throws IOException
	 */
	private IrisDetailsDTO getIrisImageAsDTOWithMdm(RequestDetail requestDetail, int leftEyeAttempt,
			int rightEyeAttempt) throws RegBaseCheckedException, IOException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Entering into getIrisImageAsDTOWithMdm method..");
		if (isNull(requestDetail.getType()))
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_MASTER_BIO_SERVICE_IMPL_IRIS_IMAGE);
		requestDetail.setType(RegistrationConstants.IRIS_DOUBLE);
		CaptureResponseDto captureResponseDto = mosipBioDeviceManager.regScan(requestDetail);
		if (captureResponseDto == null)
			throw new RegBaseCheckedException("202", "Device is not available");
		if (captureResponseDto.getError().getErrorCode().matches("102|101|202|403|404|409"))
			throw new RegBaseCheckedException(captureResponseDto.getError().getErrorCode(),
					captureResponseDto.getError().getErrorInfo());

		IrisDetailsDTO detailsDTO = new IrisDetailsDTO();

		detailsDTO.setIrises(new ArrayList<IrisDetailsDTO>());

		List<CaptureResponseBioDto> mosipBioDeviceDataResponses = captureResponseDto.getMosipBioDeviceDataResponses();
		mosipBioDeviceDataResponses.forEach(captured -> {
			IrisDetailsDTO irisDetails = new IrisDetailsDTO();
			CaptureResponsBioDataDto captureResponse = captured.getCaptureResponseData();

			if (captureResponse != null && isQualityScoreMaxInclusive(captureResponse.getQualityScore())) {
				int attempt = captureResponse.getBioSubType().toLowerCase()
						.contains(RegistrationConstants.LEFT.toLowerCase()) ? leftEyeAttempt : rightEyeAttempt;

				setBioQualityScores(captureResponse.getBioSubType(), attempt,
						Integer.parseInt(captureResponse.getQualityScore()));
				setBioStreamImages(Base64.getDecoder().decode(captureResponse.getBioValue()),
						captureResponse.getBioSubType(), attempt);

				// Get Best Capture
				captureResponse = getBestCapture(captureResponse);

				irisDetails.setIrisIso((Base64.getDecoder().decode(captureResponse.getBioExtract())));
				irisDetails.setIrisImageName(captureResponse.getBioSubType());
				irisDetails.setIris(Base64.getDecoder().decode(captureResponse.getBioValue()));
				irisDetails.setQualityScore(Integer.parseInt(captureResponse.getQualityScore()));
				irisDetails.setIrisType(captureResponse.getBioSubType());
				irisDetails.setCaptured(true);
				irisDetails.setNumOfIrisRetry(attempt);
				detailsDTO.getIrises().add(irisDetails);
			}

		});
		detailsDTO.setCaptured(true);
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Leaving into getIrisImageAsDTOWithMdm method..");

		return detailsDTO;
	}

	/**
	 * Gets the iris stub image as DTO without MDM
	 *
	 * @param irisDetailsDTO
	 *            the iris details DTO
	 * @param irisType
	 *            the iris type
	 * @return Iris Details DTO
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private IrisDetailsDTO getIrisImageAsDTONonMdm(String irisType, int leftEyeAttempt, int rightEyeAttempt)
			throws RegBaseCheckedException {
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

			IrisDetailsDTO irisDetails = new IrisDetailsDTO();

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
				int attempt;
				if (irisType.contains(RegistrationConstants.LEFT)) {
					attempt = leftEyeAttempt;
				} else {
					attempt = rightEyeAttempt;
				}
				if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
					irisDetailsDTO.setQualityScore(qualityScore);
				}
				irisDetailsDTO.setQualityScore(91.0);
				irisDetailsDTO.setNumOfIrisRetry(attempt);
				irisDetails.setIrises(new ArrayList<IrisDetailsDTO>());
				irisDetails.getIrises().add(irisDetailsDTO);
				irisDetails.setCaptured(true);
			}

			LOGGER.info(LOG_REG_IRIS_FACADE, APPLICATION_NAME, APPLICATION_ID,
					"Stubbing iris details for user registration completed");

			return irisDetails;
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
		return mosipBioDeviceManager.regScan(requestDetail);
	}

	@Override
	public byte[] getSingleBioValue(CaptureResponseDto captureResponseDto) {

		return mosipBioDeviceManager.getSingleBioValue(captureResponseDto);
	}

	@Override
	public byte[] getSingleBiometricIsoTemplate(CaptureResponseDto captureResponseDto) throws IOException {

		if (!isMdmEnabled())
			return IOUtils.resourceToByteArray(RegistrationConstants.FACE_ISO);
		else {
			return mosipBioDeviceManager.getSingleBiometricIsoTemplate(captureResponseDto);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateFP(io.mosip.registration
	 * .dto.biometric.FingerprintDetailsDTO, java.util.List)
	 */
	/*
	 * @Override public boolean validateFP(FingerprintDetailsDTO
	 * fingerprintDetailsDTO, List<UserBiometric> userFingerprintDetails) {
	 * FingerprintTemplate fingerprintTemplate = new FingerprintTemplate()
	 * .convert(fingerprintDetailsDTO.getFingerPrint()); String minutiae =
	 * fingerprintTemplate.serialize(); int fingerPrintScore = Integer
	 * .parseInt(String.valueOf(ApplicationContext.map().get(RegistrationConstants.
	 * FINGER_PRINT_SCORE))); userFingerprintDetails.forEach(fingerPrintTemplateEach
	 * -> { if (fingerprintProvider.scoreCalculator(minutiae,
	 * fingerPrintTemplateEach.getBioMinutia()) > fingerPrintScore) {
	 * fingerprintDetailsDTO.setFingerType(fingerPrintTemplateEach.
	 * getUserBiometricId().getBioAttributeCode()); } }); return
	 * userFingerprintDetails.stream() .anyMatch(bio ->
	 * fingerprintProvider.scoreCalculator(minutiae, bio.getBioMinutia()) >
	 * fingerPrintScore); }
	 */

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
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Getting highest quality score for : " + bioType);

		try {
			qualityScore = BIO_QUALITY_SCORE.get(bioType).entrySet().stream()
					.max(Comparator.comparing(Map.Entry::getValue)).get().getValue();
		} catch (Exception e) {
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

		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Clearing bio scores of : " + captures);

		captures.forEach(key -> BIO_QUALITY_SCORE.remove(key));

	}

	public static void setBioStreamImages(byte[] image, String bioType, int attempt) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Started Set Stream image of : " + bioType + " for attempt : " + attempt);

		Map<Integer, byte[]> bioImage = null;

		if (BIO_STREAM_IMAGES.get(bioType) != null) {
			bioImage = BIO_STREAM_IMAGES.get(bioType);

		} else {

			bioImage = new HashMap<Integer, byte[]>();

		}

		// image = image == null ? streamImage : image;
		bioImage.put(attempt, image);

		BIO_STREAM_IMAGES.put(bioType, bioImage);

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Completed Set Stream image of : " + bioType + " for attempt : " + attempt);

	}

	public static void clearBIOStreamImagesByBioType(List<String> captures) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Clearing Stream images of : " + captures);

		captures.forEach(key -> BIO_STREAM_IMAGES.remove(key));

	}

	public static void clearAllStreamImages() {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Clearing all Stream images");

		BIO_STREAM_IMAGES.clear();
	}

	public byte[] getBioStreamImage(String bioType, int attempt) {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Get Stream  Quality Score of : " + bioType + " for attempt : " + attempt);

		if (BIO_STREAM_IMAGES.get(bioType) != null) {
			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))
				return BIO_STREAM_IMAGES.get(bioType).get(attempt);
			return BIO_STREAM_IMAGES.get(bioType).get(1);
		}

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"NOT FOUND : Stream image of : " + bioType + " for attempt : " + attempt);

		return null;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#isValidFingerPrints(io.mosip.
	 * registration.dto.biometric.FingerprintDetailsDTO)
	 */
	/*
	 * public boolean isValidFingerPrints(FingerprintDetailsDTO
	 * fingerprintDetailsDTO, boolean isAuth) {
	 * 
	 * boolean isValid = false;
	 * 
	 * if (isAuth) {
	 * 
	 * String threshold = getThresholdKey(fingerprintDetailsDTO.getFingerType());
	 * 
	 * List<FingerprintDetailsDTO> lowQualityBiometrics = new ArrayList<>();
	 * 
	 * double qualityScore = 0; for (FingerprintDetailsDTO detailsDTO :
	 * fingerprintDetailsDTO.getSegmentedFingerprints()) {
	 * 
	 * if (detailsDTO.getQualityScore() <
	 * Double.valueOf(getGlobalConfigValueOf(threshold))) {
	 * lowQualityBiometrics.add(detailsDTO);
	 * 
	 * } else { qualityScore += fingerprintDetailsDTO.getQualityScore(); } }
	 * 
	 * fingerprintDetailsDTO.getSegmentedFingerprints().removeAll(
	 * lowQualityBiometrics);
	 * 
	 * setBioQualityScores(fingerprintDetailsDTO.getFingerType(),
	 * fingerprintDetailsDTO.getNumRetry(), qualityScore / (double)
	 * fingerprintDetailsDTO.getSegmentedFingerprints().size());
	 * 
	 * return !fingerprintDetailsDTO.getSegmentedFingerprints().isEmpty(); } else if
	 * (isAllNonExceptionBiometricsCaptured(fingerprintDetailsDTO.
	 * getSegmentedFingerprints(), fingerprintDetailsDTO.getFingerType(),
	 * getExceptionFingersByBioType(fingerprintDetailsDTO.getFingerType()))) {
	 * 
	 * isValid = true; if (!(boolean)
	 * SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) { return
	 * validateQualityScore(fingerprintDetailsDTO); } } return isValid; }
	 * 
	 * private String getThresholdKey(String fingerType) { String threshold = null;
	 * if (fingerType == null) { return threshold; }
	 * 
	 * if (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
	 * threshold = RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD; } else if
	 * (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) { threshold
	 * = RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD; } else if
	 * (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
	 * threshold = RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD; } return
	 * threshold; }
	 * 
	 * private void removeBelowThresholdBiometrics(FingerprintDetailsDTO
	 * fingerprintDetailsDTO) {
	 * 
	 * }
	 * 
	 * private List<String> getExceptionFingersByBioType(String bioSubType) {
	 * 
	 * List<String> exceptionBiometrics = new LinkedList<>();
	 * 
	 * List<String> allExceptionFingers = getAllExceptionFingers();
	 * 
	 * if (allExceptionFingers != null) { for (String missingBiometric :
	 * allExceptionFingers) {
	 * 
	 * if (bioSubType.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT) &&
	 * RegistrationConstants.LEFT_SLAP.contains(missingBiometric)) {
	 * exceptionBiometrics.add(missingBiometric);
	 * 
	 * }
	 * 
	 * else if (bioSubType.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT) &&
	 * RegistrationConstants.RIGHT_SLAP.contains(missingBiometric)) {
	 * exceptionBiometrics.add(missingBiometric); } else if
	 * (bioSubType.equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS) &&
	 * RegistrationConstants.TWO_THUMBS.contains(missingBiometric)) {
	 * exceptionBiometrics.add(missingBiometric); } } }
	 * 
	 * return exceptionBiometrics; }
	 * 
	 * 
	 * private List<String> getAllExceptionFingers() { List<BiometricExceptionDTO>
	 * biometricExceptionDTOs;
	 * 
	 * if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
	 * biometricExceptionDTOs =
	 * getBiometricDTOFromSession().getOperatorBiometricDTO().
	 * getBiometricExceptionDTO(); } else { biometricExceptionDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getBiometricExceptionDTO();
	 * 
	 * if (biometricExceptionDTOs == null || biometricExceptionDTOs.isEmpty()) {
	 * biometricExceptionDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
	 * .getBiometricExceptionDTO(); } }
	 * 
	 * biometricExceptionDTOs = biometricExceptionDTOs.stream()
	 * .filter((biometricExceptionDTO) ->
	 * !biometricExceptionDTO.getBiometricType().toUpperCase()
	 * .contains(RegistrationConstants.IRIS)) .collect(Collectors.toList());
	 * List<String> exceptionBiometrics = null; if (biometricExceptionDTOs != null
	 * && !biometricExceptionDTOs.isEmpty()) {
	 * 
	 * exceptionBiometrics = new LinkedList<>(); for (BiometricExceptionDTO
	 * biometricExceptionDTO : biometricExceptionDTOs) {
	 * 
	 * String missingBiometric =
	 * getSegmentedFingerName(biometricExceptionDTO.getMissingBiometric());
	 * exceptionBiometrics.add(missingBiometric); } }
	 * 
	 * if (exceptionBiometrics != null) {
	 * exceptionBiometrics.remove(RegistrationConstants.FACE);
	 * exceptionBiometrics.remove(RegistrationConstants.FACE_EXCEPTION); } return
	 * exceptionBiometrics;
	 * 
	 * }
	 */

	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	protected RegistrationDTO getRegistrationDTOFromSession() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	/**
	 * Gets the biometric DTO from session.
	 *
	 * @return the biometric DTO from session
	 */
	/*
	 * protected BiometricDTO getBiometricDTOFromSession() { return (BiometricDTO)
	 * SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA); }
	 */

	private boolean isAllNonExceptionBiometricsCaptured(List<FingerprintDetailsDTO> segmentedFingerPrints,
			String fingerType, List<String> exceptionFingers) {

		boolean isValid = false;

		List<String> selectedSlap = new LinkedList<>();
		if (fingerType == null || fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
			selectedSlap.addAll(RegistrationConstants.LEFT_SLAP);

		}

		if (fingerType == null || fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
			selectedSlap.addAll(RegistrationConstants.RIGHT_SLAP);

		}
		if (fingerType == null || fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
			selectedSlap.addAll(RegistrationConstants.TWO_THUMBS);

		}

		if (exceptionFingers != null && !exceptionFingers.isEmpty()) {
			isValid = true;

			// TODO Temp fix

			isValid = selectedSlap.size() - exceptionFingers.size() <= segmentedFingerPrints.size();

			// if (selectedSlap.size() - exceptionFingers.size() <=
			// segmentedFingerPrints.size() ) {
			// for (FingerprintDetailsDTO detailsDTO : segmentedFingerPrints) {
			// if (exceptionFingers.contains(detailsDTO.getFingerType())
			// || !selectedSlap.contains(detailsDTO.getFingerType())) {
			// isValid = false;
			// break;
			// }
			// }
			// } else {
			// isValid = false;
			// }

		} else {
			return selectedSlap.size() == segmentedFingerPrints.size();
		}

		return isValid;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.bio.BioService#validateBioDeDup(java.util.List)
	 */
	/*
	 * public boolean validateBioDeDup(List<FingerprintDetailsDTO>
	 * fingerprintDetailsDTOs) { AuthenticationValidatorDTO
	 * authenticationValidatorDTO = new AuthenticationValidatorDTO();
	 * authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId()
	 * ); authenticationValidatorDTO.setFingerPrintDetails(fingerprintDetailsDTOs);
	 * authenticationValidatorDTO.setAuthValidationType("multiple"); boolean isValid
	 * = !authenticationService.authValidator("Fingerprint",
	 * authenticationValidatorDTO); if (null !=
	 * getGlobalConfigValueOf("IDENTY_SDK")) { isValid = false; } return isValid;
	 * 
	 * }
	 */

	/**
	 * Validates QualityScore.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @param handThreshold
	 *            the hand threshold
	 * @return boolean
	 */
	private Boolean validate(FingerprintDetailsDTO fingerprintDetailsDTO, String handThreshold) {

		double qualityScore;
		if (!isMdmEnabled()) {
			qualityScore = fingerprintDetailsDTO.getQualityScore();
		} else {
			qualityScore = getHighQualityScoreByBioType(fingerprintDetailsDTO.getFingerType(),
					fingerprintDetailsDTO.getQualityScore());
		}
		return qualityScore >= Double.parseDouble(getGlobalConfigValueOf(handThreshold))
				|| (qualityScore < Double.parseDouble(getGlobalConfigValueOf(handThreshold))
						&& fingerprintDetailsDTO.getNumRetry() == Integer
								.parseInt(getGlobalConfigValueOf(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)))
				|| fingerprintDetailsDTO.isForceCaptured();
	}

	/**
	 * Validating quality score of captured fingerprints.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @return true, if successful
	 */
	protected boolean validateQualityScore(FingerprintDetailsDTO fingerprintDetailsDTO) {
		try {
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating quality score of captured fingerprints started");
			if (fingerprintDetailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
				return validate(fingerprintDetailsDTO, RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
			} else if (fingerprintDetailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				return validate(fingerprintDetailsDTO, RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD);
			} else if (fingerprintDetailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
				return validate(fingerprintDetailsDTO, RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD);
			}
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating quality score of captured fingerprints ended");
			return false;
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_FINGERPRINT_SCORE_VALIDATION_EXP,
					String.format(
							"Exception while validating the quality score of captured Fingerprints: %s caused by %s",
							runtimeException.getMessage(), runtimeException.getCause()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.bio.BioService#
	 * isAllNonExceptionFingerprintsCaptured()
	 */
	/*
	 * public boolean isAllNonExceptionFingerprintsCaptured() {
	 * List<FingerprintDetailsDTO> fingerprintDetailsDTOs; if ((boolean)
	 * SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
	 * fingerprintDetailsDTOs =
	 * getBiometricDTOFromSession().getOperatorBiometricDTO().
	 * getFingerprintDetailsDTO(); } else if
	 * (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
	 * fingerprintDetailsDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getFingerprintDetailsDTO(); } else { fingerprintDetailsDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
	 * .getFingerprintDetailsDTO(); }
	 * 
	 * List<FingerprintDetailsDTO> segmentedFingerPrints = new LinkedList<>(); for
	 * (FingerprintDetailsDTO detailsDTO : fingerprintDetailsDTOs) {
	 * segmentedFingerPrints.addAll(detailsDTO.getSegmentedFingerprints()); }
	 * 
	 * List<String> fingerBioAttributes = new LinkedList<>();
	 * fingerBioAttributes.addAll(RegistrationConstants.leftHandUiAttributes);
	 * fingerBioAttributes.addAll(RegistrationConstants.rightHandUiAttributes);
	 * fingerBioAttributes.addAll(RegistrationConstants.twoThumbsUiAttributes);
	 * 
	 * //List<String> nonConfigFiongers =
	 * getNonConfigBioAttributes(fingerBioAttributes);
	 * 
	 * //nonConfigFiongers = nonConfigFiongers != null ? nonConfigFiongers : new
	 * LinkedList<>();
	 * 
	 * List<String> exceptionFingers = getAllExceptionFingers();
	 * 
	 * //if (exceptionFingers != null) { //for (String exceptiopnFinger :
	 * exceptionFingers) { // if (!nonConfigFiongers.contains(exceptiopnFinger)) {
	 * // nonConfigFiongers.add(exceptiopnFinger); // } //} //}
	 * 
	 * if (!isAllNonExceptionBiometricsCaptured(segmentedFingerPrints, null,
	 * exceptionFingers)) { return false; } return true; }
	 */

	public boolean hasBiometricExceptionToggleEnabled() {
		return (Boolean) SessionContext.userContext().getUserMap()
				.get(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION);
	}

	protected List<String> getNonConfigBioAttributes(List<String> constantAttributes) {

		List<String> nonConfigBiometrics = new LinkedList<>();

		// Get Bio Attributes
		List<String> uiAttributes = getBioAttributes();

		for (String attribute : constantAttributes) {
			if (!uiAttributes.contains(attribute)) {
				nonConfigBiometrics.add(RegistrationConstants.regBioMap.get(attribute));
			}
		}
		return nonConfigBiometrics;
	}

	@Override
	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException, IOException {

		List<BiometricsDto> biometricsDtos = null;
		if (isMdmEnabled()) {

			biometricsDtos = captureRealModality(mdmRequestDto);

		} else {
			biometricsDtos = captureMockModality(mdmRequestDto, false);

		}

		return biometricsDtos;
	}

	private List<BiometricsDto> captureRealModality(MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException, IOException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Entering into captureModality method.." + System.currentTimeMillis());

		List<BiometricsDto> list = new ArrayList<BiometricsDto>();

		MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(mdmRequestDto.getModality());

		MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
				.getMdsProvider(bioDevice.getSpecVersion());

		List<BiometricsDto> biometricsDtos = deviceSpecificationProvider.rCapture(bioDevice, mdmRequestDto);

		for (BiometricsDto biometricsDto : biometricsDtos) {

			if (biometricsDto != null && isQualityScoreMaxInclusive(String.valueOf(biometricsDto.getQualityScore()))) {

				list.add(biometricsDto);
			}
		}

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Ended captureModality method.." + System.currentTimeMillis());
		return list;
	}

	private List<BiometricsDto> captureMockModality(MDMRequestDto mdmRequestDto, boolean isUserOnboarding)
			throws RegBaseCheckedException {
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
				"Scanning of mock modality for user registration");
		List<BiometricsDto> list = new ArrayList<>();
		try {

			List<String> attributes = Biometric.getDefaultAttributes(mdmRequestDto.getModality());
			if (mdmRequestDto.getExceptions() != null)
				attributes.removeAll(Arrays.asList(mdmRequestDto.getExceptions()));

			for (String bioAttribute : attributes) {
				BiometricsDto biometricDto = new BiometricsDto(
						Biometric.getBiometricByAttribute(bioAttribute).getAttributeName(), IOUtils.resourceToByteArray(
								getFilePath(mdmRequestDto.getModality(), bioAttribute, isUserOnboarding)),
						90.0);
				biometricDto.setCaptured(true);
				list.add(biometricDto);
			}
		} catch (Exception e) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_FINGERPRINT_SCANNING_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_FINGERPRINT_SCANNING_ERROR.getErrorMessage()
							+ ExceptionUtils.getStackTrace(e));
		}
		return list;
	}

	private String getFilePath(String modality, String bioAttribute, boolean isUserOnboarding) throws IOException {
		LOGGER.info(LOG_REG_FINGERPRINT_FACADE, APPLICATION_NAME, APPLICATION_ID,
				"Current Modality >>>>>>>>>>>>>>>>>>>>>>>>> " + modality + "  bioAttribute >>>>> " + bioAttribute);
		String path = null;
		switch (modality) {
		case PacketManagerConstants.FINGERPRINT_SLAB_LEFT:
			path = String.format(isUserOnboarding ? "/UserOnboard/leftHand/%s/ISOTemplate.iso"
					: "/fingerprints/lefthand/%s/ISOTemplate.iso", bioAttribute);
			break;
		case PacketManagerConstants.FINGERPRINT_SLAB_RIGHT:
			path = String.format(isUserOnboarding ? "/UserOnboard/rightHand/%s/ISOTemplate.iso"
					: "/fingerprints/Srighthand/%s/ISOTemplate.iso", bioAttribute);
			break;
		case PacketManagerConstants.FINGERPRINT_SLAB_THUMBS:
			path = String.format(isUserOnboarding ? "/UserOnboard/thumb/%s/ISOTemplate.iso"
					: "/fingerprints/thumb/%s/ISOTemplate.iso", bioAttribute);
			break;
		case PacketManagerConstants.IRIS_DOUBLE:
			path = String.format("/images/%s.iso", bioAttribute);
			break;
		case "FACE":
		case PacketManagerConstants.FACE_FULLFACE:
			path = String.format("/images/%s.iso", "face");
			break;
		}
		return path;
	}

	@Override
	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException, IOException {

		List<BiometricsDto> biometrics = null;

		if (isMdmEnabled()) {
			if (getStream(mdmRequestDto.getModality()) != null) {

				biometrics = captureRealModality(mdmRequestDto);
			}
		} else {
			biometrics = captureMockModality(mdmRequestDto, true);
		}
		return biometrics;
	}

	@Override
	public InputStream getStream(String modality) throws MalformedURLException, IOException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Stream request : " + System.currentTimeMillis() + modality);

		LOGGER.info(STREAMER, APPLICATION_NAME, APPLICATION_ID,
				"Constructing Stream URL Started" + System.currentTimeMillis());

		MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(modality);

		MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
				.getMdsProvider(bioDevice.getSpecVersion());

		return deviceSpecificationProvider.stream(bioDevice, modality);

	}
}
