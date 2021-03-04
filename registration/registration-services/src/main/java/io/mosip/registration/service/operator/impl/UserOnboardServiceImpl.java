package io.mosip.registration.service.operator.impl;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleAnySubtypeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.cryptomanager.dto.CryptomanagerRequestDto;
import io.mosip.kernel.cryptomanager.dto.CryptomanagerResponseDto;
import io.mosip.kernel.cryptomanager.service.CryptomanagerService;
import io.mosip.kernel.keygenerator.bouncycastle.util.KeyGeneratorUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.exception.InvalidApplicationIdException;
import io.mosip.kernel.keymanagerservice.exception.KeymanagerServiceException;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.mosip.registration.context.ApplicationContext;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_USER_ONBOARD;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 * Implementation for {@link UserOnboardService}
 * 
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@Service
public class UserOnboardServiceImpl extends BaseService implements UserOnboardService {

	@Autowired
	private UserOnboardDAO userOnBoardDao;

	@Autowired
	private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	@Autowired
	private KeymanagerUtil keymanagerUtil;

	@Autowired
	private CryptomanagerService cryptomanagerService;

	@Autowired
	private KeymanagerService keymanagerService;

	@Autowired
	private BioAPIFactory bioAPIFactory;

	/**
	 * logger for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(UserOnboardServiceImpl.class);

	private static final String BIOMETRIC_KEY_PATTERN = "%s_%s_%s";
	private Map<String, BiometricsDto> operatorBiometrics;

	@Override
	public ResponseDTO validateWithIDAuthAndSave(List<BiometricsDto> biometrics) throws RegBaseCheckedException {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "validateWithIDAuthAndSave invoked ");

		if (Objects.isNull(biometrics))
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorMessage());

		ResponseDTO responseDTO = new ResponseDTO();
		if (validateWithIDA(biometrics, responseDTO)) {
			responseDTO = save(biometrics);
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG);
		}
		return responseDTO;
	}

	@SuppressWarnings("unchecked")
	public boolean validateWithIDA(List<BiometricsDto> biometrics, ResponseDTO responseDTO) throws PreConditionCheckException {

		//Precondition check, proceed only if met, otherwise throws exception
		proceedWithOperatorOnboard();

		Map<String, Object> idaRequestMap = new LinkedHashMap<>();
		idaRequestMap.put(RegistrationConstants.ID, RegistrationConstants.IDENTITY);
		idaRequestMap.put(RegistrationConstants.VERSION, RegistrationConstants.PACKET_SYNC_VERSION);
		idaRequestMap.put(RegistrationConstants.REQUEST_TIME,
				DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		idaRequestMap.put(RegistrationConstants.ENV, io.mosip.registration.context.ApplicationContext
				.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE));
		idaRequestMap.put(RegistrationConstants.DOMAIN_URI, RegistrationAppHealthCheckUtil
				.prepareURLByHostName(RegistrationAppHealthCheckUtil.mosipHostNamePlaceHolder));
		idaRequestMap.put(RegistrationConstants.TRANSACTION_ID, RegistrationConstants.TRANSACTION_ID_VALUE);
		idaRequestMap.put(RegistrationConstants.CONSENT_OBTAINED, true);
		idaRequestMap.put(RegistrationConstants.INDIVIDUAL_ID, SessionContext.userContext().getUserId());
		idaRequestMap.put(RegistrationConstants.INDIVIDUAL_ID_TYPE, RegistrationConstants.USER_ID_CODE);
		idaRequestMap.put(RegistrationConstants.KEY_INDEX, "");

		Map<String, Boolean> tempMap = new HashMap<>();
		tempMap.put(RegistrationConstants.BIO, true);
		idaRequestMap.put(RegistrationConstants.REQUEST_AUTH, tempMap);

		List<Map<String, Object>> listOfBiometric = new ArrayList<>();
		Map<String, Object> requestMap = new LinkedHashMap<>();

		Map<String, String> requestParamMap = new LinkedHashMap<>();
		requestParamMap.put(RegistrationConstants.REF_ID, RegistrationConstants.IDA_REFERENCE_ID);
		requestParamMap.put(RegistrationConstants.TIME_STAMP,
				DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));

		try {
			String certificateData = getCertificate(requestParamMap);

			if(certificateData == null) {
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						RegistrationConstants.ON_BOARD_PUBLIC_KEY_ERROR);
				setErrorResponse(responseDTO, RegistrationConstants.ON_BOARD_PUBLIC_KEY_ERROR, null);
				return false;
			}

			Certificate certificate = keymanagerUtil.convertToCertificate(certificateData);

			if (Objects.nonNull(biometrics) && !biometrics.isEmpty()) {
				String previousHash = HMACUtils2.digestAsPlainText("".getBytes());

				for (BiometricsDto dto : biometrics) {
					SingleType bioType = Biometric.getSingleTypeByAttribute(dto.getBioAttribute());
					String bioSubType = getSubTypes(bioType, dto.getBioAttribute());
					LinkedHashMap<String, Object> dataBlock = buildDataBlock(bioType.name(), bioSubType,
							dto.getAttributeISO(), previousHash, dto);
					dataBlock.put(RegistrationConstants.ONBOARD_CERT_THUMBPRINT, CryptoUtil.encodeBase64(getCertificateThumbprint(certificate)));
					previousHash = (String) dataBlock.get(RegistrationConstants.AUTH_HASH);
					listOfBiometric.add(dataBlock);
				}
			}

			if (listOfBiometric.isEmpty())
				throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorCode(),
						RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorMessage());

			requestMap.put(RegistrationConstants.ON_BOARD_BIOMETRICS, listOfBiometric);
			requestMap.put(RegistrationConstants.ON_BOARD_TIME_STAMP,
					DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));

			Map<String, Object> response = getIdaAuthResponse(idaRequestMap, requestMap, requestParamMap,
					certificate, responseDTO);
			boolean onboardAuthFlag = userOnBoardStatusFlag(response, responseDTO);
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"User Onboarded authentication flag... :" + onboardAuthFlag);

			if (onboardAuthFlag) {
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG);
				return true;
			} else {
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG);
				setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG,
						response);
			}

		} catch (Exception e) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
			setErrorResponse(responseDTO, e.getMessage(), null);
		}
		return false;
	}

	private String getCertificate(Map<String, String> requestParamMap) throws Exception {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "getCertificate invoked ....");
		try {
			KeyPairGenerateResponseDto certificateDto = keymanagerService.getCertificate(RegistrationConstants.AP_IDA,
					Optional.of(RegistrationConstants.IDA_REFERENCE_ID));

			if (certificateDto != null && certificateDto.getCertificate() != null)
				return certificateDto.getCertificate();
		} catch (InvalidApplicationIdException | KeymanagerServiceException ex) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"No entry found for applicationId : IDA");
		}

		Map<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(
				RegistrationConstants.PUBLIC_KEY_IDA_REST, requestParamMap, true,
				RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
		LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) response
				.get(RegistrationConstants.RESPONSE);
		String certificateData = responseMap.get(RegistrationConstants.CERTIFICATE).toString();

		UploadCertificateRequestDto uploadCertificateRequestDto = new UploadCertificateRequestDto();
		uploadCertificateRequestDto.setApplicationId("IDA");
		uploadCertificateRequestDto.setReferenceId(RegistrationConstants.IDA_REFERENCE_ID);
		uploadCertificateRequestDto.setCertificateData(certificateData);
		keymanagerService.uploadOtherDomainCertificate(uploadCertificateRequestDto);
		return certificateData;
	}

	private LinkedHashMap<String, Object> buildDataBlock(String bioType, String bioSubType, byte[] attributeISO,
			String previousHash, BiometricsDto biometricsDto) throws NoSuchAlgorithmException {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Building data block for User Onboard Authentication with IDA");

		LinkedHashMap<String, Object> dataBlock = new LinkedHashMap<>();
		Map<String, Object> data = new HashMap<>();
		data.put(RegistrationConstants.ON_BOARD_TIME_STAMP,
				DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		data.put(RegistrationConstants.ON_BOARD_BIO_TYPE, bioType);
		data.put(RegistrationConstants.ON_BOARD_BIO_SUB_TYPE, bioSubType);
		SplittedEncryptedData responseMap = getSessionKey(data, attributeISO);
		data.put(RegistrationConstants.ON_BOARD_BIO_VALUE, responseMap.getEncryptedData());
		data.put(RegistrationConstants.TRANSACTION_Id, RegistrationConstants.TRANSACTION_ID_VALUE);
		data.put(RegistrationConstants.PURPOSE, RegistrationConstants.PURPOSE_AUTH);
		data.put(RegistrationConstants.ENV, io.mosip.registration.context.ApplicationContext
				.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE));
		data.put(RegistrationConstants.DOMAIN_URI, RegistrationAppHealthCheckUtil
				.prepareURLByHostName(RegistrationAppHealthCheckUtil.mosipHostNamePlaceHolder));
		String dataBlockJsonString = RegistrationConstants.EMPTY;
		try {
			dataBlockJsonString = new ObjectMapper().writeValueAsString(data);
			dataBlock.put(RegistrationConstants.ON_BOARD_BIO_DATA,
					CryptoUtil.encodeBase64(dataBlockJsonString.getBytes()));
		} catch (IOException exIoException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exIoException));
		}

		String presentHash = HMACUtils2.digestAsPlainText(dataBlockJsonString.getBytes());
		String concatenatedHash = previousHash + presentHash;
		String finalHash = HMACUtils2.digestAsPlainText(concatenatedHash.getBytes());

		dataBlock.put(RegistrationConstants.AUTH_HASH, finalHash);
		dataBlock.put(RegistrationConstants.SESSION_KEY, responseMap.getEncryptedSessionKey());

		//TODO - We cannot pull private key( appId:IDA, refId:SIGN ) to sign the data here .. need to check with Loga
		//dataBlock.put("signature", "");

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Returning the dataBlock for User Onboard Authentication with IDA");

		return dataBlock;
	}

	private String getSubTypes(SingleType singleType, String bioAttribute) {
		List<String> subtypes = new LinkedList<>();
		switch (singleType) {
		case FINGER:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			if (bioAttribute.toLowerCase().contains("thumb"))
				subtypes.add(SingleAnySubtypeType.THUMB.value());
			else {
				String val = bioAttribute.toLowerCase().replace("left", "").replace("right", "");
				subtypes.add(SingleAnySubtypeType.fromValue(StringUtils.capitalizeFirstLetter(val).concat("Finger"))
						.value());
			}
			break;
		case IRIS:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value()
					: SingleAnySubtypeType.RIGHT.value());
			break;
		case FACE:
			break;
		}
		return String.join(" ", subtypes);
	}

	/**
	 * Adds the face data.
	 *
	 * @param listOfBiometric   the list of biometric
	 * @param faceISO           the face ISO
	 * @param previousHashArray the previous hash array
	 * @param responseDTO       the response DTO
	 */
	private void addFaceData(List<Map<String, Object>> listOfBiometric, byte[] faceISO, String[] previousHashArray,
			ResponseDTO responseDTO) throws NoSuchAlgorithmException {
		if (null != faceISO) {
			LinkedHashMap<String, Object> faceData = new LinkedHashMap<>();
			Map<String, Object> data = new HashMap<>();
			data.put(RegistrationConstants.ON_BOARD_TIME_STAMP, DateUtils.getUTCCurrentDateTimeString());
			data.put(RegistrationConstants.ON_BOARD_BIO_TYPE, RegistrationConstants.ON_BOARD_FACE);
			data.put(RegistrationConstants.ON_BOARD_BIO_SUB_TYPE, RegistrationConstants.ON_BOARD_FACE);
			SplittedEncryptedData responseMap = getSessionKey(data, faceISO);
			if (null != responseMap && null != responseMap.getEncryptedData()) {
				data.put(RegistrationConstants.ON_BOARD_BIO_VALUE, responseMap.getEncryptedData());
				faceData.put(RegistrationConstants.SESSION_KEY, responseMap.getEncryptedSessionKey());
			}
			String dataBlockJsonString = RegistrationConstants.EMPTY;
			try {
				dataBlockJsonString = new ObjectMapper().writeValueAsString(data);
				faceData.put(RegistrationConstants.ON_BOARD_BIO_DATA,
						CryptoUtil.encodeBase64(dataBlockJsonString.getBytes()));
			} catch (IOException exIoException) {
				LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						ExceptionUtils.getStackTrace(exIoException));
				setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_EXCEPTION, null);
			}

			String presentHash = HMACUtils2.digestAsPlainText(dataBlockJsonString.getBytes());

			String concatenatedHash = previousHashArray[0] + presentHash;
			String finalHash = HMACUtils2.digestAsPlainText(concatenatedHash.getBytes());
			faceData.put(RegistrationConstants.AUTH_HASH, finalHash);
			faceData.put(RegistrationConstants.SIGNATURE, "");
			listOfBiometric.add(faceData);
		}
	}

	/**
	 * Save.
	 *
	 * @param biometrics the biometric DTO
	 * @return the string
	 */
	private ResponseDTO save(List<BiometricsDto> biometrics) {
		ResponseDTO responseDTO = new ResponseDTO();
		String onBoardingResponse = RegistrationConstants.EMPTY;

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Entering save method");

		try {
			List<BiometricsDto> fingerprintsList = getBiometricsByModality(RegistrationConstants.FINGERPRINT_UPPERCASE,
					biometrics);
			List<BiometricsDto> irisList = getBiometricsByModality(RegistrationConstants.IRIS, biometrics);
			List<BiometricsDto> face = getBiometricsByModality(RegistrationConstants.FACE, biometrics);

			List<BIR> fpTemplates = getExtractedTemplates(fingerprintsList);
			List<BIR> irisTemplates = getExtractedTemplates(irisList);
			List<BIR> faceTemplates = getExtractedTemplates(face);

			List<BIR> templates = Stream.of(fpTemplates, irisTemplates, faceTemplates).flatMap(Collection::stream)
					.collect(Collectors.toList());

			onBoardingResponse = userOnBoardDao.insertExtractedTemplates(templates);
			if (onBoardingResponse.equalsIgnoreCase(RegistrationConstants.SUCCESS)) {
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "operator details inserted");

				if ((RegistrationConstants.SUCCESS).equalsIgnoreCase(userOnBoardDao.save())) {
					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
							"center user machine details inserted");

					setSuccessResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG, null);

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "user onboarding sucessful");
				}
			}
		} catch (RegBaseUncheckedException | BiometricException uncheckedException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, uncheckedException.getMessage()
					+ onBoardingResponse + ExceptionUtils.getStackTrace(uncheckedException));

			setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_ERROR_RESPONSE, null);

		}
		return responseDTO;
	}

	private List<BIR> getExtractedTemplates(List<BiometricsDto> biometrics) throws BiometricException {
		List<BIR> templates = new ArrayList<>();
		if (biometrics != null && !biometrics.isEmpty()) {
			List<BIR> birList = new ArrayList<>();
			for (BiometricsDto biometricsDto : biometrics) {
				BIR bir = buildBir(biometricsDto);
				LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Adding bir");
				birList.add(bir);
			}

			templates = bioAPIFactory
					.getBioProvider(BiometricType.fromValue(birList.get(0).getBdbInfo().getType().get(0).value()),
							BiometricFunction.EXTRACT)
					.extractTemplate(birList, null);
		}
		return templates;
	}

	private List<BiometricsDto> getBiometricsByModality(String modality, List<BiometricsDto> biometrics) {
		return biometrics.stream().filter(dto -> dto.getModalityName().toLowerCase().contains(modality.toLowerCase()))
				.collect(Collectors.toList());
	}


	/**
	 * User on board status flag.
	 *
	 * @param onBoardResponseMap the on board response map
	 * @return the boolean
	 */
	@SuppressWarnings("unchecked")
	private Boolean userOnBoardStatusFlag(Map<String, Object> onBoardResponseMap, ResponseDTO responseDTO) {

		Boolean userOnbaordFlag = false;

		if (null != onBoardResponseMap && null != onBoardResponseMap.get(RegistrationConstants.RESPONSE)
				&& null == onBoardResponseMap.get(RegistrationConstants.ERRORS)) {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) onBoardResponseMap
					.get(RegistrationConstants.RESPONSE);
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "authStatus true");
			userOnbaordFlag = (Boolean) responseMap.get(RegistrationConstants.ON_BOARD_AUTH_STATUS);
		} else if (null != onBoardResponseMap && null != onBoardResponseMap.get(RegistrationConstants.ERRORS)) {
			List<LinkedHashMap<String, Object>> listOfFailureResponse = (List<LinkedHashMap<String, Object>>) onBoardResponseMap
					.get(RegistrationConstants.ERRORS);
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) onBoardResponseMap
					.get(RegistrationConstants.RESPONSE);
			userOnbaordFlag = (Boolean) responseMap.get(RegistrationConstants.ON_BOARD_AUTH_STATUS);
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, listOfFailureResponse.toString());
			setErrorResponse(responseDTO,
					listOfFailureResponse.size() > 0 ? (String) listOfFailureResponse.get(0).get("errorMessage")
							: RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG,
					null);
		}

		return userOnbaordFlag;

	}

	@Override
	public Timestamp getLastUpdatedTime(String usrId) {
		return userOnBoardDao.getLastUpdatedTime(usrId);
	}

	/**
	 * Dto null check.
	 *
	 * @param biometricDTO the biometric DTO
	 * @return true, if successful
	 */
	private boolean dtoNullCheck(BiometricDTO biometricDTO) {
		if (null != biometricDTO && null != biometricDTO.getOperatorBiometricDTO()) {
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"biometricDTO/operator bio metrics are mandatroy");
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getIdaAuthResponse(Map<String, Object> idaRequestMap, Map<String, Object> requestMap,
			Map<String, String> requestParamMap, Certificate certificate, ResponseDTO responseDTO) {
		try {

			PublicKey publicKey = certificate.getPublicKey();
			idaRequestMap.put(RegistrationConstants.ONBOARD_CERT_THUMBPRINT, CryptoUtil.encodeBase64(getCertificateThumbprint(certificate)));

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Getting Symmetric Key.....");
			// Symmetric key alias session key
			KeyGenerator keyGenerator = KeyGeneratorUtils.getKeyGenerator("AES", 256);
			// Generate AES Session Key
			final SecretKey symmentricKey = keyGenerator.generateKey();

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "preparing request.....");
			// request
			idaRequestMap.put(RegistrationConstants.ON_BOARD_REQUEST,
					CryptoUtil.encodeBase64(cryptoCore.symmetricEncrypt(symmentricKey,
							new ObjectMapper().writeValueAsString(requestMap).getBytes(), null)));

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "preparing request HMAC.....");
			// requestHMAC
			idaRequestMap.put(RegistrationConstants.ON_BOARD_REQUEST_HMAC,
					CryptoUtil.encodeBase64(cryptoCore.symmetricEncrypt(symmentricKey, HMACUtils2
							.digestAsPlainText(new ObjectMapper().writeValueAsString(requestMap).getBytes()).getBytes(),
							null)));

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "preparing request Session Key.....");
			// requestSession Key
			idaRequestMap.put(RegistrationConstants.ON_BOARD_REQUEST_SESSION_KEY,
					CryptoUtil.encodeBase64(cryptoCore.asymmetricEncrypt(publicKey, symmentricKey.getEncoded())));

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Ida Auth rest calling.....");

			LinkedHashMap<String, Object> onBoardResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil.post(
					RegistrationConstants.ON_BOARD_IDA_VALIDATION, idaRequestMap,
					RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);

			return onBoardResponse;

		} catch (RegBaseCheckedException | IOException | RuntimeException
				| NoSuchAlgorithmException regBasedCheckedException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBasedCheckedException));
			setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_EXCEPTION, null);
		}
		return null;

	}

	private synchronized SplittedEncryptedData getSessionKey(Map<String, Object> requestMap, byte[] data) {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Getting sessionKey for User Onboard Authentication with IDA");

		String timestamp = (String) requestMap.get(RegistrationConstants.ON_BOARD_TIME_STAMP);
		byte[] xorBytes = getXOR(timestamp, RegistrationConstants.TRANSACTION_ID_VALUE);
		byte[] saltLastBytes = getLastBytes(xorBytes, 12);
		byte[] aadLastBytes = getLastBytes(xorBytes, 16);

		CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
		cryptomanagerRequestDto.setAad(CryptoUtil.encodeBase64(aadLastBytes));
		cryptomanagerRequestDto.setApplicationId(RegistrationConstants.AP_IDA);
		cryptomanagerRequestDto.setData(CryptoUtil.encodeBase64(data));
		cryptomanagerRequestDto.setReferenceId(RegistrationConstants.IDA_REFERENCE_ID);
		cryptomanagerRequestDto.setSalt(CryptoUtil.encodeBase64(saltLastBytes));
		cryptomanagerRequestDto.setTimeStamp(DateUtils.getUTCCurrentDateTime());
		//Note: As thumbprint is sent as part of request, there is no need to prepend thumbprint in encrypted data
		cryptomanagerRequestDto.setPrependThumbprint(false);
		CryptomanagerResponseDto cryptomanagerResponseDto = cryptomanagerService.encrypt(cryptomanagerRequestDto);

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Returning the sessionKey for User Onboard Authentication with IDA");
		return splitEncryptedData(cryptomanagerResponseDto.getData());
	}

	/**
	 * Method to insert specified number of 0s in the beginning of the given string
	 * 
	 * @param string
	 * @param count  - number of 0's to be inserted
	 * @return bytes
	 */
	private byte[] prependZeros(byte[] string, int count) {
		byte[] newBytes = new byte[string.length + count];
		int i = 0;
		for (; i < count; i++) {
			newBytes[i] = 0;
		}

		for (int j = 0; i < newBytes.length; i++, j++) {
			newBytes[i] = string[j];
		}

		return newBytes;
	}

	/**
	 * Method to return the XOR of the given strings
	 * 
	 */
	private byte[] getXOR(String timestamp, String transactionId) {
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Started getting XOR of timestamp and transactionId");

		byte[] timestampBytes = timestamp.getBytes();
		byte[] transactionIdBytes = transactionId.getBytes();
		// Lengths of the given strings
		int timestampLength = timestampBytes.length;
		int transactionIdLength = transactionIdBytes.length;

		// Make both the strings of equal lengths
		// by inserting 0s in the beginning
		if (timestampLength > transactionIdLength) {
			transactionIdBytes = prependZeros(transactionIdBytes, timestampLength - transactionIdLength);
		} else if (transactionIdLength > timestampLength) {
			timestampBytes = prependZeros(timestampBytes, transactionIdLength - timestampLength);
		}

		// Updated length
		int length = Math.max(timestampLength, transactionIdLength);
		byte[] xorBytes = new byte[length];

		// To store the resultant XOR
		for (int i = 0; i < length; i++) {
			xorBytes[i] = (byte) (timestampBytes[i] ^ transactionIdBytes[i]);
		}

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"Returning XOR of timestamp and transactionId");

		return xorBytes;
	}

	/**
	 * Gets the last bytes.
	 *
	 * @param xorBytes
	 * @param lastBytesNum the last bytes num
	 * @return the last bytes
	 */
	private byte[] getLastBytes(byte[] xorBytes, int lastBytesNum) {
		assert (xorBytes.length >= lastBytesNum);
		return Arrays.copyOfRange(xorBytes, xorBytes.length - lastBytesNum, xorBytes.length);
	}

	/**
	 * Split encrypted data.
	 *
	 * @param data the data
	 * @return the splitted encrypted data
	 */
	public SplittedEncryptedData splitEncryptedData(String data) {
		byte[] dataBytes = CryptoUtil.decodeBase64(data);
		byte[][] splits = splitAtFirstOccurance(dataBytes,
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.KEY_SPLITTER)).getBytes());
		return new SplittedEncryptedData(CryptoUtil.encodeBase64(splits[0]), CryptoUtil.encodeBase64(splits[1]));
	}

	/**
	 * Split at first occurance.
	 *
	 * @param strBytes the str bytes
	 * @param sepBytes the sep bytes
	 * @return the byte[][]
	 */
	private static byte[][] splitAtFirstOccurance(byte[] strBytes, byte[] sepBytes) {
		int index = findIndex(strBytes, sepBytes);
		if (index >= 0) {
			byte[] bytes1 = new byte[index];
			byte[] bytes2 = new byte[strBytes.length - (bytes1.length + sepBytes.length)];
			System.arraycopy(strBytes, 0, bytes1, 0, bytes1.length);
			System.arraycopy(strBytes, (bytes1.length + sepBytes.length), bytes2, 0, bytes2.length);
			return new byte[][] { bytes1, bytes2 };
		} else {
			return new byte[][] { strBytes, new byte[0] };
		}
	}

	/**
	 * Find index.
	 *
	 * @param arr    the arr
	 * @param subarr the subarr
	 * @return the int
	 */
	private static int findIndex(byte arr[], byte[] subarr) {
		int len = arr.length;
		int subArrayLen = subarr.length;
		return IntStream.range(0, len).filter(currentIndex -> {
			if ((currentIndex + subArrayLen) <= len) {
				byte[] sArray = new byte[subArrayLen];
				System.arraycopy(arr, currentIndex, sArray, 0, subArrayLen);
				return Arrays.equals(sArray, subarr);
			}
			return false;
		}).findFirst() // first occurence
				.orElse(-1); // No element found
	}

	/**
	 * The Class SplittedEncryptedData.
	 */
	public static class SplittedEncryptedData {
		private String encryptedSessionKey;
		private String encryptedData;

		public SplittedEncryptedData() {
			super();
		}

		public SplittedEncryptedData(String encryptedSessionKey, String encryptedData) {
			super();
			this.encryptedData = encryptedData;
			this.encryptedSessionKey = encryptedSessionKey;
		}

		public String getEncryptedData() {
			return encryptedData;
		}

		public void setEncryptedData(String encryptedData) {
			this.encryptedData = encryptedData;
		}

		public String getEncryptedSessionKey() {
			return encryptedSessionKey;
		}

		public void setEncryptedSessionKey(String encryptedSessionKey) {
			this.encryptedSessionKey = encryptedSessionKey;
		}

	}

	@Override
	public void initializeOperatorBiometric() {
		operatorBiometrics = new HashMap<String, BiometricsDto>();
	}

	@Override
	public BiometricsDto addOperatorBiometrics(String operatorType, String uiSchemaAttribute, BiometricsDto value) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"addOperatorBiometrics >>> operatorType :: " + operatorType + " bioAttribute :: " + uiSchemaAttribute);

		operatorBiometrics.put(String.format(BIOMETRIC_KEY_PATTERN, operatorType, uiSchemaAttribute, ""), value);
		return value;
	}

	@Override
	public void addOperatorBiometricException(String operatorType, String bioAttribute) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"addOperatorBiometricException >>> operatorType :: " + operatorType + " bioAttribute :: "
						+ bioAttribute);
		operatorBiometrics.remove(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, ""));
		operatorBiometrics.put(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, "exp"), null);
	}

	@Override
	public void removeOperatorBiometrics(String operatorType, String bioAttribute) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"removeOperatorBiometrics >>>> operatorType :: " + operatorType + " bioAttribute :: " + bioAttribute);
		operatorBiometrics.remove(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, ""));
	}

	@Override
	public void removeOperatorBiometricException(String operatorType, String bioAttribute) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
				"removeOperatorBiometricException >>>>> operatorType :: " + operatorType + " bioAttribute :: "
						+ bioAttribute);
		operatorBiometrics.remove(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, "exp"));
	}

	@Override
	public List<BiometricsDto> getAllBiometrics() {
		if (Objects.isNull(operatorBiometrics))
			return null;

		return operatorBiometrics.entrySet().stream().filter(m -> m.getKey().endsWith("_")).map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	@Override
	public List<BiometricsDto> getAllBiometricExceptions() {
		if (Objects.isNull(operatorBiometrics))
			return null;

		return operatorBiometrics.entrySet().stream().filter(m -> m.getKey().endsWith("_exp")).map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	@Override
	public boolean isBiometricException(String operatorType, String bioAttribute) {
		if (Objects.isNull(operatorBiometrics))
			return false;

		Optional<String> result = operatorBiometrics.entrySet().stream()
				.filter(m -> m.getKey()
						.equalsIgnoreCase(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, "exp")))
				.map(Map.Entry::getKey).findFirst();

		return result.isPresent() ? true : false;
	}

	@Override
	public List<BiometricsDto> getBiometrics(String operatorType, List<String> attributeNames) {
		if (Objects.isNull(operatorBiometrics))
			return null;

		List<BiometricsDto> list = new ArrayList<>();
		attributeNames.forEach(name -> {
			Optional<BiometricsDto> result = operatorBiometrics.entrySet().stream()
					.filter(m -> m.getKey().equals(String.format(BIOMETRIC_KEY_PATTERN, operatorType, name, "")))
					.map(Map.Entry::getValue).findFirst();

			if (result.isPresent())
				list.add(result.get());
		});
		return list;
	}

	private byte[] getCertificateThumbprint(Certificate cert) {
		try {
			return DigestUtils.sha256(cert.getEncoded());
		} catch (CertificateEncodingException e) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Failed to get cert thumbprint >> " +
					ExceptionUtils.getStackTrace(e));
		}
		return new byte[]{};
	}
}
