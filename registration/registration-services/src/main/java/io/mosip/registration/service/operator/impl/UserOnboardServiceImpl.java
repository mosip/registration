package io.mosip.registration.service.operator.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_USER_ONBOARD;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.crypto.SecretKey;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleAnySubtypeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.kernel.packetmanager.constants.Biometric;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.biometric.BiometricDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.publickey.PublicKeyGenerationUtil;

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
	private KeyGenerator keyGenerator;

	@Autowired
	private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	/**
	 * logger for logging
	 */
	private static final Logger LOGGER = AppConfig.getLogger(UserOnboardServiceImpl.class);
	
	private static final String BIOMETRIC_KEY_PATTERN = "%s_%s_%s";
	private Map<String, BiometricsDto> operatorBiometrics;
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.UserOnBoardService#validate(io.mosip.
	 * registration.dto.biometric.BiometricDTO)
	 */
	//@Override
	/*public ResponseDTO validate(BiometricDTO biometricDTO) throws RegBaseCheckedException {

		ResponseDTO responseDTO = new ResponseDTO();
		if (dtoNullCheck(biometricDTO)) {
			Map<String, Object> idaRequestMap = new LinkedHashMap<>();

			idaRequestMap.put(RegistrationConstants.ID, RegistrationConstants.IDENTITY);
			idaRequestMap.put(RegistrationConstants.VERSION, RegistrationConstants.PACKET_SYNC_VERSION);
			idaRequestMap.put(RegistrationConstants.REQUEST_TIME, DateUtils.getUTCCurrentDateTimeString());
			idaRequestMap.put(RegistrationConstants.TRANSACTION_ID, RegistrationConstants.TRANSACTION_ID_VALUE);
			Map<String, Boolean> tempMap = new HashMap<>();
			tempMap.put(RegistrationConstants.BIO, true);
			idaRequestMap.put(RegistrationConstants.REQUEST_AUTH, tempMap);
			idaRequestMap.put(RegistrationConstants.CONSENT_OBTAINED, true);
			idaRequestMap.put(RegistrationConstants.INDIVIDUAL_ID, SessionContext.userContext().getUserId());
			idaRequestMap.put(RegistrationConstants.INDIVIDUAL_ID_TYPE, RegistrationConstants.USER_ID_CODE);
			idaRequestMap.put(RegistrationConstants.KEY_INDEX, "");

			List<Map<String, Object>> listOfBiometric = new ArrayList<>();
			Map<String, Object> requestMap = new LinkedHashMap<>();

			if (RegistrationAppHealthCheckUtil.isNetworkAvailable()) {

				String[] previousHashArray = { HMACUtils.digestAsPlainText(HMACUtils.generateHash("".getBytes())) };

				biometricDTO.getOperatorBiometricDTO().getFingerprintDetailsDTO().forEach(bio -> {

					bio.getSegmentedFingerprints().forEach(finger -> {
						LinkedHashMap<String, Object> dataBlockFinger = new LinkedHashMap<>();
						Map<String, Object> data = new HashMap<>();
						data.put(RegistrationConstants.ON_BOARD_TIME_STAMP, DateUtils.getUTCCurrentDateTimeString());
						data.put(RegistrationConstants.ON_BOARD_BIO_TYPE, RegistrationConstants.ON_BOARD_FINGER_ID);
						data.put(RegistrationConstants.ON_BOARD_BIO_SUB_TYPE,
								RegistrationConstants.userOnBoardBioFlag.get(finger.getFingerType()));
						SplittedEncryptedData responseMap = getSessionKey(data, finger.getFingerPrintISOImage());
						data.put(RegistrationConstants.ON_BOARD_BIO_VALUE, responseMap.getEncryptedData());
						String dataBlockJsonString = RegistrationConstants.EMPTY;
						try {
							dataBlockJsonString = new ObjectMapper().writeValueAsString(data);
							dataBlockFinger.put(RegistrationConstants.ON_BOARD_BIO_DATA,
									CryptoUtil.encodeBase64(dataBlockJsonString.getBytes()));
						} catch (IOException exIoException) {
							LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
									ExceptionUtils.getStackTrace(exIoException));
						}

						String presentHash = HMACUtils
								.digestAsPlainText(HMACUtils.generateHash(dataBlockJsonString.getBytes()));

						String concatenatedHash = previousHashArray[0] + presentHash;
						String finalHash = HMACUtils
								.digestAsPlainText(HMACUtils.generateHash(concatenatedHash.getBytes()));
						dataBlockFinger.put(RegistrationConstants.AUTH_HASH, finalHash);
						dataBlockFinger.put(RegistrationConstants.SESSION_KEY, responseMap.getEncryptedSessionKey());
						dataBlockFinger.put(RegistrationConstants.SIGNATURE, "");
						listOfBiometric.add(dataBlockFinger);
						previousHashArray[0] = finalHash;

					});
				});

				biometricDTO.getOperatorBiometricDTO().getIrisDetailsDTO().forEach(iris -> {
					LinkedHashMap<String, Object> dataBlockIris = new LinkedHashMap<>();
					Map<String, Object> data = new HashMap<>();
					data.put(RegistrationConstants.ON_BOARD_TIME_STAMP, DateUtils.getUTCCurrentDateTimeString());
					data.put(RegistrationConstants.ON_BOARD_BIO_TYPE, RegistrationConstants.ON_BOARD_IRIS_ID);
					data.put(RegistrationConstants.ON_BOARD_BIO_SUB_TYPE,
							RegistrationConstants.userOnBoardBioFlag.get(iris.getIrisImageName()));
					SplittedEncryptedData responseMap = getSessionKey(data, iris.getIrisIso());
					data.put(RegistrationConstants.ON_BOARD_BIO_VALUE, responseMap.getEncryptedData());
					String dataBlockJsonString = RegistrationConstants.EMPTY;
					try {
						dataBlockJsonString = new ObjectMapper().writeValueAsString(data);
						dataBlockIris.put(RegistrationConstants.ON_BOARD_BIO_DATA,
								CryptoUtil.encodeBase64(dataBlockJsonString.getBytes()));
					} catch (IOException exIoException) {
						LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
								ExceptionUtils.getStackTrace(exIoException));
					}
					String presentHash = HMACUtils
							.digestAsPlainText(HMACUtils.generateHash(dataBlockJsonString.getBytes()));
					String concatenatedHash = previousHashArray[0] + presentHash;
					String finalHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(concatenatedHash.getBytes()));
					dataBlockIris.put(RegistrationConstants.AUTH_HASH, finalHash);
					dataBlockIris.put(RegistrationConstants.SESSION_KEY, responseMap.getEncryptedSessionKey());
					dataBlockIris.put(RegistrationConstants.SIGNATURE, "");
					listOfBiometric.add(dataBlockIris);
					previousHashArray[0] = finalHash;
				});
				addFaceData(listOfBiometric, biometricDTO.getOperatorBiometricDTO().getFace().getFaceISO(),
						previousHashArray, responseDTO);
				requestMap.put(RegistrationConstants.ON_BOARD_BIOMETRICS, listOfBiometric);
				requestMap.put(RegistrationConstants.ON_BOARD_TIME_STAMP, DateUtils.getUTCCurrentDateTimeString());
				Map<String, String> requestParamMap = new LinkedHashMap<>();
				requestParamMap.put(RegistrationConstants.REF_ID, RegistrationConstants.IDA_REFERENCE_ID);
				requestParamMap.put(RegistrationConstants.TIME_STAMP, DateUtils.getUTCCurrentDateTimeString());
				responseDTO = isIdaAuthRequired(idaRequestMap, requestMap, biometricDTO, requestParamMap);
			} else {
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, RegistrationConstants.NO_INTERNET);
				setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);
			}

		} else {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorMessage());
		}

		return responseDTO;
	}*/
	
	@Override
	public ResponseDTO validateWithIDAuthAndSave(List<BiometricsDto> biometrics) throws RegBaseCheckedException {
		boolean idAuthEnabled = RegistrationConstants.ENABLE.equalsIgnoreCase((String) ApplicationContext.map().
				get(RegistrationConstants.USER_ON_BOARD_IDA_AUTH));
		
		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "validateWithIDAuthAndSave invoked idAuthEnabled >> " + idAuthEnabled);
		
		if(Objects.isNull(biometrics))
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorMessage());
		
		ResponseDTO responseDTO = new ResponseDTO();		
		if(!idAuthEnabled || (idAuthEnabled && validateWithIDA(biometrics, responseDTO)) ) {
			responseDTO = save(biometrics);
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG);
		}
		return responseDTO;
	}
	
	@SuppressWarnings("unchecked")
	public boolean validateWithIDA(List<BiometricsDto> biometrics, ResponseDTO responseDTO) 
			throws RegBaseCheckedException {
		
		if(!RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, RegistrationConstants.NO_INTERNET);
			setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);
			return false;
		}
		
		Map<String, Object> idaRequestMap = new LinkedHashMap<>();
		idaRequestMap.put(RegistrationConstants.ID, RegistrationConstants.IDENTITY);
		idaRequestMap.put(RegistrationConstants.VERSION, RegistrationConstants.PACKET_SYNC_VERSION);
		idaRequestMap.put(RegistrationConstants.REQUEST_TIME, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
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
				
		if(Objects.nonNull(biometrics) && !biometrics.isEmpty()) {
			String previousHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash("".getBytes()));
			
			for(BiometricsDto dto : biometrics) {
				SingleType bioType = Biometric.getSingleTypeByAttribute(dto.getBioAttribute());
				String bioSubType = getSubTypes(bioType, dto.getBioAttribute());
				LinkedHashMap<String, Object> dataBlock = buildDataBlock(bioType.name(), bioSubType, 
						dto.getAttributeISO(), previousHash);
				previousHash = (String) dataBlock.get(RegistrationConstants.AUTH_HASH);
				listOfBiometric.add(dataBlock);
			}
		}
		
		if(listOfBiometric.isEmpty())
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_DTO_NULL.getErrorMessage());		
		
		requestMap.put(RegistrationConstants.ON_BOARD_BIOMETRICS, listOfBiometric);
		requestMap.put(RegistrationConstants.ON_BOARD_TIME_STAMP, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		Map<String, String> requestParamMap = new LinkedHashMap<>();
		requestParamMap.put(RegistrationConstants.REF_ID, RegistrationConstants.IDA_REFERENCE_ID);
		requestParamMap.put(RegistrationConstants.TIME_STAMP, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		
		try {			
			Map<String, Object> response = (LinkedHashMap<String, Object>) serviceDelegateUtil.get(
					RegistrationConstants.PUBLIC_KEY_IDA_REST, requestParamMap, false,
					RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
			
			if (null != response && response.size() > 0
					&& null != response.get(RegistrationConstants.RESPONSE)) {
				response = getIdaAuthResponse(idaRequestMap, requestMap, requestParamMap, response, responseDTO);
				boolean onboardAuthFlag = userOnBoardStatusFlag(response);
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						"User Onboarded authentication flag... :" + onboardAuthFlag);

				if (onboardAuthFlag) {						
					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
							RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG);
					return true;
				} else {
					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
							RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG);
					setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG, response);
				}
			}
			else {
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						RegistrationConstants.ON_BOARD_PUBLIC_KEY_ERROR);
				setErrorResponse(responseDTO, RegistrationConstants.ON_BOARD_PUBLIC_KEY_ERROR, null);
			}
		} catch(Exception e) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(e));
		}			
		return false;
	}
	
	private LinkedHashMap<String, Object> buildDataBlock(String bioType, String bioSubType, byte[] attributeISO, String previousHash) {
		LinkedHashMap<String, Object> dataBlock = new LinkedHashMap<>();
		Map<String, Object> data = new HashMap<>();
		data.put(RegistrationConstants.ON_BOARD_TIME_STAMP, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		data.put(RegistrationConstants.ON_BOARD_BIO_TYPE, bioType);
		data.put(RegistrationConstants.ON_BOARD_BIO_SUB_TYPE, bioSubType);
		SplittedEncryptedData responseMap = getSessionKey(data, attributeISO);
		data.put(RegistrationConstants.ON_BOARD_BIO_VALUE, responseMap.getEncryptedData());
		String dataBlockJsonString = RegistrationConstants.EMPTY;
		try {
			dataBlockJsonString = new ObjectMapper().writeValueAsString(data);
			dataBlock.put(RegistrationConstants.ON_BOARD_BIO_DATA,
					CryptoUtil.encodeBase64(dataBlockJsonString.getBytes()));
		} catch (IOException exIoException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exIoException));
		}

		String presentHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(dataBlockJsonString.getBytes()));
		String concatenatedHash = previousHash + presentHash;
		String finalHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(concatenatedHash.getBytes()));
		
		dataBlock.put(RegistrationConstants.AUTH_HASH, finalHash);
		dataBlock.put(RegistrationConstants.SESSION_KEY, responseMap.getEncryptedSessionKey());
		dataBlock.put(RegistrationConstants.SIGNATURE, "");
		return dataBlock;
	}
	
	private String getSubTypes(SingleType singleType, String bioAttribute) {
		List<String> subtypes = new LinkedList<>();		
		switch (singleType) {
		case FINGER:
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value() :
				SingleAnySubtypeType.RIGHT.value());
			if(bioAttribute.toLowerCase().contains("thumb"))
				subtypes.add(SingleAnySubtypeType.THUMB.value());
			else {
				String val = bioAttribute.toLowerCase().replace("left", "").replace("right", "");
				subtypes.add(SingleAnySubtypeType.fromValue(StringUtils.capitalizeFirstLetter(val).concat("Finger")).value());
			}
			break;
		case IRIS:			
			subtypes.add(bioAttribute.contains("left") ? SingleAnySubtypeType.LEFT.value() :
				SingleAnySubtypeType.RIGHT.value());
			break;
		case FACE:
			break;
		}
		return String.join(" ", subtypes);
	}

	
	/**
	 * Adds the face data.
	 *
	 * @param listOfBiometric the list of biometric
	 * @param faceISO the face ISO
	 * @param previousHashArray the previous hash array
	 * @param responseDTO the response DTO
	 */
	private void addFaceData(List<Map<String, Object>> listOfBiometric, byte[] faceISO, String[] previousHashArray,
			ResponseDTO responseDTO) {
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

			String presentHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(dataBlockJsonString.getBytes()));

			String concatenatedHash = previousHashArray[0] + presentHash;
			String finalHash = HMACUtils.digestAsPlainText(HMACUtils.generateHash(concatenatedHash.getBytes()));
			faceData.put(RegistrationConstants.AUTH_HASH, finalHash);
			faceData.put(RegistrationConstants.SIGNATURE, "");
			listOfBiometric.add(faceData);
		}
	}

	/**
	 * Save.
	 *
	 * @param biometricDTO the biometric DTO
	 * @return the string
	 */
	private ResponseDTO save(List<BiometricsDto> biometrics) {

		ResponseDTO responseDTO = new ResponseDTO();
		String onBoardingResponse = RegistrationConstants.EMPTY;

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Entering save method");

		try {

			onBoardingResponse = userOnBoardDao.insert(biometrics);

			if (onBoardingResponse.equalsIgnoreCase(RegistrationConstants.SUCCESS)) {

				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "operator details inserted");

				if ((RegistrationConstants.SUCCESS).equalsIgnoreCase(userOnBoardDao.save())) {

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
							"center user machine details inserted");

					setSuccessResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG, null);

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "user onboarding sucessful");
				}
			}

		} catch (RegBaseUncheckedException uncheckedException) {

			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, uncheckedException.getMessage()
					+ onBoardingResponse + ExceptionUtils.getStackTrace(uncheckedException));

			setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_ERROR_RESPONSE, null);

		}

		return responseDTO;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.UserOnboardService#getStationID(java.lang.
	 * String)
	 */
	@Override
	public Map<String, String> getMachineCenterId() {

		Map<String, String> mapOfCenterId = new WeakHashMap<>();

		String stationId = RegistrationConstants.EMPTY;
		String centerId = RegistrationConstants.EMPTY;

		LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "fetching getMachineCenterId ....");

		try {

			// get stationID
			stationId = userOnBoardDao.getStationID(RegistrationSystemPropertiesChecker.getMachineId());

			// get CenterID
			centerId = userOnBoardDao.getCenterID(stationId);

			// setting data into map
			mapOfCenterId.put(RegistrationConstants.USER_STATION_ID, stationId);
			mapOfCenterId.put(RegistrationConstants.USER_CENTER_ID, centerId);

			LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					"station Id = " + stationId + "---->" + "center Id = " + centerId);

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}

		return mapOfCenterId;
	}

	/**
	 * User on board status flag.
	 *
	 * @param onBoardResponseMap the on board response map
	 * @return the boolean
	 */
	@SuppressWarnings("unchecked")
	private Boolean userOnBoardStatusFlag(Map<String, Object> onBoardResponseMap) {

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
			LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, listOfFailureResponse.toString());
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
			Map<String, String> requestParamMap, Map<String, Object> publicKeyResponse, 
			ResponseDTO responseDTO) {
		try {
			
					LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) publicKeyResponse
							.get(RegistrationConstants.RESPONSE);

					// Getting Public Key
					PublicKey publicKey = PublicKeyGenerationUtil
							.generatePublicKey(responseMap.get(RegistrationConstants.PUBLIC_KEY).toString().getBytes());

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Getting Symmetric Key.....");
					// Symmetric key alias session key
					SecretKey symmentricKey = keyGenerator.getSymmetricKey();

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "preparing request.....");
					// request
					idaRequestMap.put(RegistrationConstants.ON_BOARD_REQUEST,
							CryptoUtil.encodeBase64(cryptoCore.symmetricEncrypt(symmentricKey,
									new ObjectMapper().writeValueAsString(requestMap).getBytes(), null)));

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "preparing request HMAC.....");
					// requestHMAC
					idaRequestMap
							.put(RegistrationConstants.ON_BOARD_REQUEST_HMAC,
									CryptoUtil.encodeBase64(cryptoCore.symmetricEncrypt(symmentricKey,
											HMACUtils.digestAsPlainText(HMACUtils.generateHash(
													new ObjectMapper().writeValueAsString(requestMap).getBytes()))
													.getBytes(),
											null)));

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
							"preparing request Session Key.....");
					// requestSession Key
					idaRequestMap.put(RegistrationConstants.ON_BOARD_REQUEST_SESSION_KEY, CryptoUtil
							.encodeBase64(cryptoCore.asymmetricEncrypt(publicKey, symmentricKey.getEncoded())));

					LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "Ida Auth rest calling.....");

					LinkedHashMap<String, Object> onBoardResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
							.post(RegistrationConstants.ON_BOARD_IDA_VALIDATION, idaRequestMap,
									RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);

					return onBoardResponse;
					/*
					 * boolean onboardAuthFlag = userOnBoardStatusFlag(onBoardResponse);
					 * 
					 * LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					 * "User Onboarded authentication flag... :" + onboardAuthFlag);
					 * 
					 * if (onboardAuthFlag) { responseDTO = save(biometricDTO);
					 * LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					 * RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG); } else {
					 * LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					 * RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG);
					 * setErrorResponse(responseDTO,
					 * RegistrationConstants.USER_ON_BOARDING_THRESHOLD_NOT_MET_MSG,
					 * onBoardResponse); }
					 * 
					 * } else { LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					 * RegistrationConstants.ON_BOARD_PUBLIC_KEY_ERROR);
					 * setErrorResponse(responseDTO,
					 * RegistrationConstants.ON_BOARD_PUBLIC_KEY_ERROR, null); }
					 */

			/*} else {
				responseDTO = save(biometricDTO);
				LOGGER.info(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
						RegistrationConstants.USER_ON_BOARDING_SUCCESS_MSG);
			}*/

		} catch (RegBaseCheckedException | InvalidKeySpecException | NoSuchAlgorithmException | IOException
				| RuntimeException regBasedCheckedException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBasedCheckedException));
			setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_EXCEPTION, null);
		}
		return null;

	}

	/**
	 * Gets the session key.
	 *
	 * @param requestMap the request map
	 * @param data the data
	 * @return the session key
	 */
	@SuppressWarnings("unchecked")
	private synchronized SplittedEncryptedData getSessionKey(Map<String, Object> requestMap, byte[] data) {
		ResponseDTO responseDTO = new ResponseDTO();
		SplittedEncryptedData splittedData = null;
		Map<String, Object> mapRequest = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		String timestamp = (String) requestMap.get(RegistrationConstants.ON_BOARD_TIME_STAMP);
		String aad = CryptoUtil.encodeBase64String(timestamp.substring(timestamp.length() - 16).getBytes());
		String salt = CryptoUtil.encodeBase64String(timestamp.substring(timestamp.length() - 12).getBytes());
		map.put(RegistrationConstants.ADD, aad);
		map.put(RegistrationConstants.AP_ID, RegistrationConstants.AP_IDA);
		map.put(RegistrationConstants.ON_BOARD_BIO_DATA, CryptoUtil.encodeBase64(data));
		map.put(RegistrationConstants.REF_ID, RegistrationConstants.IDA_REFERENCE_ID);
		map.put(RegistrationConstants.SALT, salt);
		map.put(RegistrationConstants.TIME_STAMP, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		mapRequest.put(RegistrationConstants.ON_BOARD_REQUEST, map);
		mapRequest.put(RegistrationConstants.REQ_TIME, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		try {
			Map<String, Object> responseResult = (Map<String, Object>) serviceDelegateUtil.post(
					"ida_session_key", mapRequest, RegistrationConstants.JOB_TRIGGER_POINT_USER);
			if (null != responseResult && null != responseResult.get(RegistrationConstants.RESPONSE)) {
				LinkedHashMap<String, Object> splitData = (LinkedHashMap<String, Object>) responseResult
						.get(RegistrationConstants.RESPONSE);
				splittedData = splitEncryptedData((String) splitData.get(RegistrationConstants.ON_BOARD_BIO_DATA));
			}
		} catch (HttpClientErrorException | ResourceAccessException | SocketTimeoutException
				| RegBaseCheckedException regBasedCheckedException) {
			LOGGER.error(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBasedCheckedException));
			setErrorResponse(responseDTO, RegistrationConstants.USER_ON_BOARDING_EXCEPTION, null);
		}

		return splittedData;

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
	 * @param arr the arr
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
	public BiometricsDto addOperatorBiometrics(String operatorType, String uiSchemaAttribute, 
			BiometricsDto value) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "addOperatorBiometrics >>> operatorType :: " + operatorType + " bioAttribute :: " + 
				uiSchemaAttribute);

		operatorBiometrics.put(String.format(BIOMETRIC_KEY_PATTERN, operatorType, uiSchemaAttribute, ""), value);
		return value;
	} 

	@Override
	public void addOperatorBiometricException(String operatorType, String bioAttribute) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "addOperatorBiometricException >>> operatorType :: " + operatorType + " bioAttribute :: " + 
				bioAttribute);
		operatorBiometrics.remove(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, ""));
		operatorBiometrics.put(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, "exp"), null);
	}

	@Override
	public void removeOperatorBiometrics(String operatorType, String bioAttribute) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "removeOperatorBiometrics >>>> operatorType :: " + operatorType + " bioAttribute :: " + 
				bioAttribute);
		operatorBiometrics.remove(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, ""));		
	}

	@Override
	public void removeOperatorBiometricException(String operatorType, String bioAttribute) {
		LOGGER.debug(LOG_REG_USER_ONBOARD, APPLICATION_NAME, APPLICATION_ID, "removeOperatorBiometricException >>>>> operatorType :: " + operatorType + " bioAttribute :: " + 
				bioAttribute);
		operatorBiometrics.remove(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, "exp"));
	}

	@Override
	public List<BiometricsDto> getAllBiometrics() {
		if(Objects.isNull(operatorBiometrics))
			return null;
		
		return operatorBiometrics.entrySet().stream()
				.filter(m-> m.getKey().endsWith("_"))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	@Override
	public List<BiometricsDto> getAllBiometricExceptions() {
		if(Objects.isNull(operatorBiometrics))
			return null;
		
		return operatorBiometrics.entrySet().stream()
				.filter(m-> m.getKey().endsWith("_exp"))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	@Override
	public boolean isBiometricException(String operatorType, String bioAttribute) {
		if(Objects.isNull(operatorBiometrics))
			return false;
		
		Optional<String> result = operatorBiometrics.entrySet().stream()
				.filter(m-> m.getKey().equalsIgnoreCase(String.format(BIOMETRIC_KEY_PATTERN, operatorType, bioAttribute, "exp")))
				.map(Map.Entry::getKey).findFirst();
		
		return result.isPresent() ? true : false;
	}

	@Override
	public List<BiometricsDto> getBiometrics(String operatorType, List<String> attributeNames) {
		if(Objects.isNull(operatorBiometrics))
			return null;
		
		List<BiometricsDto> list = new ArrayList<>();
		attributeNames.forEach( name -> {
			Optional<BiometricsDto> result = operatorBiometrics.entrySet().stream()
					.filter(m-> m.getKey().equals(String.format(BIOMETRIC_KEY_PATTERN, operatorType, name, "")))
					.map(Map.Entry::getValue).findFirst();
			
			if(result.isPresent())
				list.add(result.get());
		});
		return list;
	}
}
