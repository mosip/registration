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
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
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

	@Autowired
	private BioAPIFactory bioAPIFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.bio.BioService#isMdmEnabled()
	 */
	@Override
	public boolean isMdmEnabled() {
		return true;
//		return RegistrationConstants.ENABLE
//				.equalsIgnoreCase(((String) ApplicationContext.map().get(RegistrationConstants.MDM_ENABLED)));
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

		try {
			for (BiometricsDto biometricsDto : biometricsDtos) {
				if (biometricsDto != null
						&& isQualityScoreMaxInclusive(String.valueOf(biometricsDto.getQualityScore()))) {
					String checkSDKQualityScore = (String) ApplicationContext.map()
							.getOrDefault(RegistrationConstants.QUALITY_CHECK_WITH_SDK, RegistrationConstants.DISABLE);
					if (checkSDKQualityScore.equalsIgnoreCase(RegistrationConstants.ENABLE)) {
						LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
								"Quality check with Biometric SDK flag is enabled..");

						BiometricType biometricType = BiometricType
								.fromValue(Biometric.getSingleTypeByAttribute(biometricsDto.getBioAttribute()).name());
						BIR bir = buildBir(biometricsDto);
						BIR[] birList = new BIR[] { bir };
						Map<BiometricType, Float> scoreMap = bioAPIFactory
								.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK)
								.getModalityQuality(birList, null);

						String updateQualityScore = (String) ApplicationContext.map().getOrDefault(
								RegistrationConstants.UPDATE_SDK_QUALITY_SCORE, RegistrationConstants.DISABLE);
						if (updateQualityScore.equalsIgnoreCase(RegistrationConstants.ENABLE)) {
							LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
									"Flag to update quality score evaluated from Biometric SDK is enabled");

							biometricsDto.setQualityScore(scoreMap.get(biometricType));

							LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
									"Quality score is evaluated and assigned to biometricsDto");
						}
					}
					list.add(biometricsDto);
				}
			}
		} catch (Exception exception) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorMessage()
							+ ExceptionUtils.getStackTrace(exception));
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

			if (deviceSpecificationFactory.isDeviceAvailable(mdmRequestDto.getModality())) {
				biometrics = captureRealModality(mdmRequestDto);
			} else {
				throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
						RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
			}
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
