package io.mosip.registration.service.sync.impl;

import java.util.*;

import io.mosip.kernel.cryptomanager.util.CryptomanagerUtils;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.sync.PolicySyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

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

	private static final Logger LOGGER = AppConfig.getLogger(PolicySyncServiceImpl.class);

	@Autowired
	private KeymanagerService keymanagerService;

	@Autowired
	private KeymanagerUtil keymanagerUtil;

	@Autowired
	private CryptomanagerUtils cryptomanagerUtils;


	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.registration.service.PolicySyncService#fetchPolicy(centerId)
	 */
	@Override
	public ResponseDTO fetchPolicy() throws RegBaseCheckedException {
		LOGGER.debug("fetchPolicy invoked");

		ResponseDTO responseDTO = new ResponseDTO();
		if (!RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			return setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);
		}

		//Precondition check, proceed only if met, otherwise throws exception
		proceedWithMasterAndKeySync(null);

		String stationId = getStationId();
		String centerId = stationId != null ? getCenterId(stationId) : null;
		validate(centerId, stationId);
		String centerMachineId = centerId.concat(RegistrationConstants.UNDER_SCORE).concat(stationId);

		try {
			String certificateData = getCertificateFromServer(centerMachineId); //fetch policy key from server
			KeyPairGenerateResponseDto certificateDto = getKeyFromLocalDB(centerMachineId); //get policy key from DB

			//compare downloaded and saved one, if different then save it
			if(certificateDto != null && Arrays.equals(cryptomanagerUtils.getCertificateThumbprint(keymanagerUtil.convertToCertificate(certificateData)),
					cryptomanagerUtils.getCertificateThumbprint(keymanagerUtil.convertToCertificate(certificateDto.getCertificate())))) {
				LOGGER.debug("Downloaded key and existing policy key are same");
				return setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE, null);
			}

			UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
			uploadCertRequestDto.setApplicationId(RegistrationConstants.REG_APP_ID);
			uploadCertRequestDto.setCertificateData(certificateData);
			uploadCertRequestDto.setReferenceId(centerMachineId);
			keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);
			LOGGER.debug("Policy Sync saved in local DB successfully");
			return setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE, null);

		} catch (Throwable t) {
			LOGGER.error("", t);
		}
		return setErrorResponse(responseDTO, RegistrationExceptionConstants.REG_POLICY_SYNC_FAILED.getErrorMessage(), null);
	}



	/**
	 * This method invokes the external service 'policysync' to download the public
	 * key with respect to local center and machine id combination. And store the
	 * key into the local database for further usage during registration process.
	 *
	 * @return
	 * @throws RegBaseCheckedException
	 */
	private String getCertificateFromServer(String centerMachineId) throws Exception {
		LOGGER.debug("Policy Sync from server invoked");

		Map<String, String> requestParams = new HashMap<>();
		requestParams.put(RegistrationConstants.GET_CERT_APP_ID, RegistrationConstants.REG_APP_ID);
		requestParams.put(RegistrationConstants.REF_ID, centerMachineId);

		LOGGER.info("Calling getCertificate rest call with request params {}", requestParams);
		LinkedHashMap<String, Object> publicKeySyncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
				.get(RegistrationConstants.GET_CERTIFICATE, requestParams, false,
						RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);


		if(null != publicKeySyncResponse.get(RegistrationConstants.RESPONSE)) {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) publicKeySyncResponse
					.get(RegistrationConstants.RESPONSE);
			return responseMap.get(RegistrationConstants.CERTIFICATE).toString();
		}

		if(publicKeySyncResponse.get(RegistrationConstants.ERRORS) != null &&
				((List<LinkedHashMap<String, String>>) publicKeySyncResponse.get(RegistrationConstants.ERRORS)).size() > 0 ) {
			LOGGER.error("Get Policy key from server failed with error {}", publicKeySyncResponse.get(RegistrationConstants.ERRORS));
		}

		throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_POLICY_SYNC_FAILED.getErrorCode(),
				RegistrationExceptionConstants.REG_POLICY_SYNC_FAILED.getErrorMessage());
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.sync.PolicySyncService#checkKeyValidation()
	 */
	@Override
	public ResponseDTO checkKeyValidation() {
		LOGGER.info("Key validation started");
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			String stationId = getStationId();
			String centerId = stationId != null ? getCenterId(stationId) : null;
			validate(centerId, stationId);
			String centerMachineId = centerId.concat(RegistrationConstants.UNDER_SCORE).concat(stationId);

			KeyPairGenerateResponseDto certificateDto = getKeyFromLocalDB(centerMachineId);

			if(certificateDto != null)
				return setSuccessResponse(responseDTO, RegistrationConstants.VALID_KEY, null);

		} catch (Exception exception) {
			LOGGER.error("POLICY_KEY validation failed", exception);
		}
		return setErrorResponse(responseDTO, RegistrationConstants.INVALID_KEY, null);
	}

	private KeyPairGenerateResponseDto getKeyFromLocalDB(String refId) {
		try {
			KeyPairGenerateResponseDto certificateDto = keymanagerService
					.getCertificate(RegistrationConstants.REG_APP_ID, Optional.of(refId));

			if(certificateDto != null && certificateDto.getCertificate() != null)
				return certificateDto;

		} catch (Exception ex) {
			LOGGER.error("Error Fetching policy key from DB", ex);
		}
		return null;
	}

	private boolean validate(String centerId, String machineId)
			throws RegBaseCheckedException {
		if (centerId == null || machineId == null)
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_POLICY_SYNC_SERVICE_IMPL_CENTERMACHINEID.getErrorCode(),
					RegistrationExceptionConstants.REG_POLICY_SYNC_SERVICE_IMPL_CENTERMACHINEID.getErrorMessage());

		return true;
	}
}
