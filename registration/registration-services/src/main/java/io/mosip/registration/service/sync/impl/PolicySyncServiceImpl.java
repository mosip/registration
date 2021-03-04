package io.mosip.registration.service.sync.impl;

import static io.mosip.registration.constants.LoggerConstants.REGISTRATION_PUBLIC_KEY_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

/**
 * 
 * It provides the method to download the Mosip public key specific to the
 * user's local machines and center specific and store the same into local db
 * for further usage during registration process. The key has expiry period.
 * Based on the expiry period the new key would be downloaded from the server
 * through this service by triggering from batch process.
 * 
 * @author Brahmananda Reddy
 * @since 1.0.0
 *
 */
@Service
public class PolicySyncServiceImpl extends BaseService implements PolicySyncService {

	@Autowired
	private KeymanagerService keymanagerService;

	private static final Logger LOGGER = AppConfig.getLogger(PolicySyncServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.PolicySyncService#fetchPolicy(centerId)
	 */
	@Override
	synchronized public ResponseDTO fetchPolicy() throws RegBaseCheckedException {
		LOGGER.debug("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
				"sync the certificate is started");

		ResponseDTO responseDTO = new ResponseDTO();
		// if(null!getCenterId(getStationId(getMacAddress()));
		String stationId = getStationId(RegistrationSystemPropertiesChecker.getMachineId());
		String centerMachineId = getCenterId(stationId) + "_" + stationId;

		try {
			KeyPairGenerateResponseDto certificateDto = keymanagerService
					.getCertificate(RegistrationConstants.REG_APP_ID, Optional.of(centerMachineId));
			if (certificateDto == null || certificateDto.getCertificate() == null) {
				LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
						"Syncing the key as the certificate is null");

				responseDTO = getCertificateFromServer(responseDTO, centerMachineId);
			} else {
				responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE, null);
			}
		} catch (Exception exception) {
			LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
					"Syncing the key as the certificate is not found and gave exception: " + exception.getMessage()
							+ ExceptionUtils.getStackTrace(exception));

			responseDTO = getCertificateFromServer(responseDTO, centerMachineId);
		}
		return responseDTO;
	}

	private ResponseDTO getCertificateFromServer(ResponseDTO responseDTO, String centerMachineId)
			throws RegBaseCheckedException {
		if (!RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			LOGGER.error("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
					"User is not online to sync the certificate");
			responseDTO = setErrorResponse(responseDTO,
					RegistrationConstants.POLICY_SYNC_CLIENT_NOT_ONLINE_ERROR_MESSAGE, null);
		} else {
			responseDTO = getCertificate(responseDTO, centerMachineId,
					DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		}
		return responseDTO;
	}

	/**
	 * This method invokes the external service 'policysync' to download the public
	 * key with respect to local center and machine id combination. And store the
	 * key into the local database for further usage during registration process.
	 *
	 * @param responseDTO     the response DTO
	 * @param centerMachineId the center machine id
	 * @return
	 * @throws RegBaseCheckedException
	 */
	@SuppressWarnings("unchecked")
	private synchronized ResponseDTO getCertificate(ResponseDTO responseDTO, String centerMachineId, String validDate)
			throws RegBaseCheckedException {
		String stationId = getStationId(RegistrationSystemPropertiesChecker.getMachineId());
		LOGGER.debug("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID, getCenterId(stationId));
		if (validate(responseDTO, centerMachineId, getCenterId(stationId))) {
			List<ErrorResponseDTO> erResponseDTOs = new ArrayList<>();
			Map<String, String> requestParams = new HashMap<>();
			requestParams.put(RegistrationConstants.GET_CERT_APP_ID, RegistrationConstants.REG_APP_ID);
			requestParams.put(RegistrationConstants.REF_ID, centerMachineId);
			try {
				LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
						"Calling getCertificate rest call with request params " + requestParams);
				
				LinkedHashMap<String, Object> publicKeySyncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
						.get(RegistrationConstants.GET_CERTIFICATE, requestParams, false,
								RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
				if (null != publicKeySyncResponse.get(RegistrationConstants.RESPONSE)) {
					LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) publicKeySyncResponse
							.get(RegistrationConstants.RESPONSE);
					UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
					uploadCertRequestDto.setApplicationId(RegistrationConstants.REG_APP_ID);
					uploadCertRequestDto
							.setCertificateData(responseMap.get(RegistrationConstants.CERTIFICATE).toString());
					uploadCertRequestDto.setReferenceId(centerMachineId);
					keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);
					responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE,
							null);
					LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
							"getCertificate sync is completed");

				} else {
					ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
					errorResponseDTO.setCode(RegistrationConstants.ERRORS);

					errorResponseDTO.setMessage(publicKeySyncResponse.size() > 0
							? ((List<LinkedHashMap<String, String>>) publicKeySyncResponse
									.get(RegistrationConstants.ERRORS)).get(0).get(RegistrationConstants.ERROR_MSG)
							: "getCertificate Sync rest call Failure");
					erResponseDTOs.add(errorResponseDTO);
					responseDTO.setErrorResponseDTOs(erResponseDTOs);
					LOGGER.info(REGISTRATION_PUBLIC_KEY_SYNC, APPLICATION_NAME, APPLICATION_ID,
							((publicKeySyncResponse.size() > 0)
									? ((List<LinkedHashMap<String, String>>) publicKeySyncResponse
											.get(RegistrationConstants.ERRORS)).get(0)
													.get(RegistrationConstants.ERROR_MSG)
									: "getCertificate Sync Restful service error"));
				}

			} catch (Exception exception) {
				LOGGER.error("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));
				responseDTO = setErrorResponse(responseDTO, RegistrationConstants.POLICY_SYNC_ERROR_MESSAGE, null);
			}
		}
		return responseDTO;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PolicySyncService#checkKeyValidation()
	 */
	@Override
	public ResponseDTO checkKeyValidation() {
		LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID, "Key validation started");

		ResponseDTO responseDTO = new ResponseDTO();
		String stationId = getStationId(RegistrationSystemPropertiesChecker.getMachineId());
		String refId = getCenterId(stationId) + "_" + stationId;
		try {
			KeyPairGenerateResponseDto certificateDto = keymanagerService
					.getCertificate(RegistrationConstants.REG_APP_ID, Optional.of(refId));
			if (certificateDto == null || (certificateDto != null && certificateDto.getCertificate() == null)) {
				setErrorResponse(responseDTO, RegistrationConstants.INVALID_KEY, null);
			} else {
				setSuccessResponse(responseDTO, RegistrationConstants.VALID_KEY, null);
			}
		} catch (Exception exception) {
			LOGGER.error("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			setErrorResponse(responseDTO, RegistrationConstants.INVALID_KEY, null);
		}
		LOGGER.info("REGISTRATION_KEY_POLICY_SYNC", APPLICATION_NAME, APPLICATION_ID, "Key validation ended");

		return responseDTO;
	}

	private boolean validate(ResponseDTO responseDTO, String centerMachineId, String centerId)
			throws RegBaseCheckedException {
		if (responseDTO != null) {
			if (centerMachineId != null && centerId != null) {
				return true;
			} else {
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_POLICY_SYNC_SERVICE_IMPL_CENTERMACHINEID.getErrorCode(),
						RegistrationExceptionConstants.REG_POLICY_SYNC_SERVICE_IMPL_CENTERMACHINEID.getErrorMessage());
			}
		} else {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_POLICY_SYNC_SERVICE_IMPL.getErrorCode(),
					RegistrationExceptionConstants.REG_POLICY_SYNC_SERVICE_IMPL.getErrorMessage());
		}
	}
}
