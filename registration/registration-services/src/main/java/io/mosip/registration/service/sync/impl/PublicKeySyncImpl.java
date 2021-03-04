package io.mosip.registration.service.sync.impl;

import static io.mosip.registration.constants.LoggerConstants.REGISTRATION_PUBLIC_KEY_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.sync.PublicKeySync;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * The Interface for Public Key service implementation.
 * 
 * It downloads the Mosip public key from server and store the same into local
 * database for further usage. The stored key will be used to validate the
 * signature provided in the external services response. If signature doesn't
 * match then response would be rejected and error response would be sent to the
 * invoking client application.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 *
 */
@Service
public class PublicKeySyncImpl extends BaseService implements PublicKeySync {

	@Autowired
	private KeymanagerService keymanagerService;

	@Value("${mosip.sign.refid:SIGN}")
	private String signRefId;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = AppConfig.getLogger(PublicKeySyncImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.PublicKeySync#getPublicKey()
	 */
	@Override
	public synchronized ResponseDTO getPublicKey(String triggerPoint) throws RegBaseCheckedException {
		LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Entering into get public key method.....");

		//Precondition check, proceed only if met, otherwise throws exception
		proceedWithMasterAndKeySync(null);

		ResponseDTO responseDTO = new ResponseDTO();
		try {
			KeyPairGenerateResponseDto certificateDto = keymanagerService
					.getCertificate(RegistrationConstants.KERNEL_APP_ID, Optional.of(signRefId));
			if (certificateDto == null || (certificateDto != null && certificateDto.getCertificate() == null)) {
				LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
						"Syncing the key as the certificate is null");
				
				responseDTO = getCertificateFromServer(responseDTO, triggerPoint);
			} else {
				responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE, null);
			}
		} catch (Exception exception) {
			LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
					"Syncing the key as the certificate is not found and gave exception: " + exception.getMessage());
			
			responseDTO = getCertificateFromServer(responseDTO, triggerPoint);
		}		
		return responseDTO;
	}

	private ResponseDTO getCertificateFromServer(ResponseDTO responseDTO, String triggerPoint) throws RegBaseCheckedException {
		if (triggerPointNullCheck(triggerPoint)) {
			try {
				LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
						"Fetching signed certificate.....");

				responseDTO = getResponse(triggerPoint);
			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
						ExceptionUtils.getStackTrace(regBaseCheckedException));

				responseDTO = setErrorResponse(new ResponseDTO(),
						isAuthTokenEmptyException(regBaseCheckedException) ? regBaseCheckedException.getErrorCode()
								: regBaseCheckedException.getMessage(),
						null);
			} catch (RuntimeException runtimeException) {
				LOGGER.error(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
						ExceptionUtils.getStackTrace(runtimeException));
				responseDTO = setErrorResponse(new ResponseDTO(), runtimeException.getMessage(), null);
			}

			LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Leaving getCertificateFromServer method.....");
		} else {
			LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					RegistrationConstants.TRIGGER_POINT_MSG);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_TRIGGER_POINT_MISSING.getErrorCode(),
					RegistrationExceptionConstants.REG_TRIGGER_POINT_MISSING.getErrorMessage());
		}
		return responseDTO;
	}

	private ResponseDTO getResponse(String triggerPoint) throws RegBaseCheckedException {
		ResponseDTO responseDTO = uploadCertificate(triggerPoint);
		if (null != responseDTO && null != responseDTO.getSuccessResponseDTO()) {
			responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE,
					null);
			LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					responseDTO.getSuccessResponseDTO().getMessage());
		} else {
			if (!isAuthTokenEmptyError(responseDTO)) {
				responseDTO = setErrorResponse(new ResponseDTO(),
						RegistrationConstants.POLICY_SYNC_ERROR_MESSAGE, null);
			}
			LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Public Key Sync Failure");
		}
		return responseDTO;
	}

	@SuppressWarnings("unchecked")
	private ResponseDTO uploadCertificate(String triggerPoint) throws RegBaseCheckedException {
		LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Entering into uploadCertificate method.....");

		ResponseDTO responseDTO = new ResponseDTO();
		Map<String, String> requestParamMap = new LinkedHashMap<>();
		requestParamMap.put(RegistrationConstants.GET_CERT_APP_ID, RegistrationConstants.KERNEL_APP_ID);
		requestParamMap.put(RegistrationConstants.REF_ID, signRefId);
		try {
			LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"Calling getCertificate rest call with request params " + requestParamMap);
			if (RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
				LinkedHashMap<String, Object> publicKeyResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
						.get(RegistrationConstants.GET_CERTIFICATE, requestParamMap, false, triggerPoint);
				if (null != publicKeyResponse && publicKeyResponse.size() > 0
						&& null != publicKeyResponse.get(RegistrationConstants.RESPONSE)) {
//					LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) publicKeyResponse
//							.get(RegistrationConstants.RESPONSE);
//					UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
//					uploadCertRequestDto.setApplicationId(RegistrationConstants.KERNEL_APP_ID);
//					uploadCertRequestDto.setCertificateData(responseMap.get(RegistrationConstants.CERTIFICATE).toString());
//					uploadCertRequestDto.setReferenceId(RegistrationConstants.KERNEL_REF_ID);
//					keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);
					responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE,
							null);
					
					LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
							"getCertificate sync successful...");
				} else {
					responseDTO = setErrorResponse(responseDTO,
							(null != publicKeyResponse && publicKeyResponse.size() > 0)
									? ((List<LinkedHashMap<String, String>>) publicKeyResponse
											.get(RegistrationConstants.ERRORS)).get(0)
													.get(RegistrationConstants.ERROR_MSG)
									: "GetCertificate Sync Restful service error",
							null);
					LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
							((null != publicKeyResponse && publicKeyResponse.size() > 0)
									? ((List<LinkedHashMap<String, String>>) publicKeyResponse
											.get(RegistrationConstants.ERRORS)).get(0)
													.get(RegistrationConstants.ERROR_MSG)
									: "GetCertificate Sync Restful service error"));

				}
			} else {
				LOGGER.error(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
						"Unable to sync certificate as there is no internet connection");
				responseDTO = setErrorResponse(responseDTO, RegistrationConstants.ERROR, null);
			}

		} catch (HttpClientErrorException | SocketTimeoutException reException) {
			LOGGER.error(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(reException));
			
			throw new RegBaseCheckedException("Exception in GetCertificate Rest Call", reException.getMessage());
		}
		LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Leaving uploadCertificate method.....");

		return responseDTO;
	}

	/**
	 * trigger point null check.
	 *
	 * @param triggerPoint the language code
	 * @return true, if successful
	 */
	private boolean triggerPointNullCheck(String triggerPoint) {
		if (StringUtils.isEmpty(triggerPoint)) {
			LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"triggerPoint is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}
	}

}
