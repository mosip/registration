package io.mosip.registration.service.bio.impl;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_FACADE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
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
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BioServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.bio.BioService#isMdmEnabled()
	 */
	@Override
	public boolean isMdmEnabled() {
		// return true;
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

	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	protected RegistrationDTO getRegistrationDTOFromSession() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	@Override
	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {

		List<BiometricsDto> biometricsDtos = null;
		if (isMdmEnabled()) {

			biometricsDtos = captureRealModality(mdmRequestDto);

		} else {
			biometricsDtos = captureMockModality(mdmRequestDto, false);

		}

		return biometricsDtos;
	}

	private List<BiometricsDto> captureRealModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
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
	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Started capture for authentication" + System.currentTimeMillis() + mdmRequestDto.getModality());

		List<BiometricsDto> biometrics = null;

		if (isMdmEnabled()) {
			// if (getStream(mdmRequestDto.getModality()) != null) {

			biometrics = captureRealModality(mdmRequestDto);
			// }
		} else {
			biometrics = captureMockModality(mdmRequestDto, true);
		}
		return biometrics;
	}

	@Override
	public InputStream getStream(String modality) throws RegBaseCheckedException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Stream request : " + System.currentTimeMillis() + modality);

		MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(modality);

		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Bio Device found for modality : " + modality + "  " + System.currentTimeMillis() + modality);

		return getStream(bioDevice, modality);

	}

	@Override
	public InputStream getStream(MdmBioDevice mdmBioDevice, String modality) throws RegBaseCheckedException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Starting stream");

		if (mdmBioDevice != null) {
			MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
					.getMdsProvider(mdmBioDevice.getSpecVersion());

			LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					"MosipDeviceSpecificationProvider found for spec version : " + mdmBioDevice.getSpecVersion() + "  "
							+ System.currentTimeMillis() + deviceSpecificationProvider);

			return deviceSpecificationProvider.stream(mdmBioDevice, modality);

		} else {
			LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Bio Device is null");

			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
		}

	}

	private boolean isQualityScoreMaxInclusive(String qualityScore) {
		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Checking for max inclusive quality score");

		if (qualityScore == null) {
			return false;
		}
		return Double.parseDouble(qualityScore) <= RegistrationConstants.MAX_BIO_QUALITY_SCORE;
	}

}
