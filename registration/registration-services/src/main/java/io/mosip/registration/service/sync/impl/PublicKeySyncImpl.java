package io.mosip.registration.service.sync.impl;

import static io.mosip.registration.constants.LoggerConstants.REGISTRATION_PUBLIC_KEY_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.net.SocketTimeoutException;
import java.util.*;

import io.mosip.kernel.cryptomanager.util.CryptomanagerUtils;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
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

	private static final Logger LOGGER = AppConfig.getLogger(PolicySyncServiceImpl.class);

	@Autowired
	private KeymanagerService keymanagerService;

	@Autowired
	private KeymanagerUtil keymanagerUtil;

	@Autowired
	private CryptomanagerUtils cryptomanagerUtils;

	@Value("${mosip.sign.refid:SIGN}")
	private String signRefId;

	/**
	 *
	 * @param triggerPoint the trigger point
	 * 		User or System.
	 * @return
	 * @throws RegBaseCheckedException
	 */
	@Override
	public ResponseDTO getPublicKey(String triggerPoint) throws RegBaseCheckedException {
		LOGGER.info("Entering into get public key method.....");

		ResponseDTO responseDTO = new ResponseDTO();
		if (!RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			return setErrorResponse(responseDTO, RegistrationConstants.NO_INTERNET, null);
		}

		//Precondition check, proceed only if met, otherwise throws exception
		proceedWithMasterAndKeySync(null);

		try {
			String certificateData = getCertificateFromServer(); //fetch sign public key from server
			saveSignPublicKey(certificateData);
			return setSuccessResponse(responseDTO, RegistrationConstants.POLICY_SYNC_SUCCESS_MESSAGE, null);

		} catch(Throwable t) {
			LOGGER.error("", t);
		}
		return setErrorResponse(responseDTO, String.format(RegistrationExceptionConstants.REG_SYNC_FAILURE.getErrorMessage(),
				"Sign Key"), null);
	}

	public void saveSignPublicKey(String syncedCertificate) {
		KeyPairGenerateResponseDto certificateDto = getKeyFromLocalDB(); //get sign public key from DB

		//compare downloaded and saved one, if different then save it
		if(certificateDto != null &&
				Arrays.equals(cryptomanagerUtils.getCertificateThumbprint(keymanagerUtil.convertToCertificate(syncedCertificate)),
				cryptomanagerUtils.getCertificateThumbprint(keymanagerUtil.convertToCertificate(certificateDto.getCertificate())))) {
			LOGGER.debug("Downloaded key and existing Sign public key are same");
			return;
		}

		UploadCertificateRequestDto uploadCertRequestDto = new UploadCertificateRequestDto();
		uploadCertRequestDto.setApplicationId(RegistrationConstants.KERNEL_APP_ID);
		uploadCertRequestDto.setCertificateData(syncedCertificate);
		uploadCertRequestDto.setReferenceId(signRefId);
		keymanagerService.uploadOtherDomainCertificate(uploadCertRequestDto);

		LOGGER.debug("Sign Public Key Synced & saved in local DB successfully");
	}


	private String getCertificateFromServer() throws Exception {
		LOGGER.debug("Sign public Sync from server invoked");

		Map<String, String> requestParams = new HashMap<>();
		requestParams.put(RegistrationConstants.GET_CERT_APP_ID, RegistrationConstants.KERNEL_APP_ID);
		requestParams.put(RegistrationConstants.REF_ID, signRefId);

		LOGGER.info("Calling getCertificate rest call with request params {}", requestParams);
		LinkedHashMap<String, Object> publicKeySyncResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
				.get(RegistrationConstants.GET_CERTIFICATE, requestParams, false, "triggerPoint");


		if(null != publicKeySyncResponse.get(RegistrationConstants.RESPONSE)) {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) publicKeySyncResponse
					.get(RegistrationConstants.RESPONSE);
			return responseMap.get(RegistrationConstants.CERTIFICATE).toString();
		}

		if(publicKeySyncResponse.get(RegistrationConstants.ERRORS) != null &&
				((List<LinkedHashMap<String, String>>) publicKeySyncResponse.get(RegistrationConstants.ERRORS)).size() > 0 ) {
			LOGGER.error("Get Sign public key from server failed with error {}",
					publicKeySyncResponse.get(RegistrationConstants.ERRORS));
		}

		throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_POLICY_SYNC_FAILED.getErrorCode(),
				RegistrationExceptionConstants.REG_POLICY_SYNC_FAILED.getErrorMessage());
	}

	private KeyPairGenerateResponseDto getKeyFromLocalDB() {
		try {
			KeyPairGenerateResponseDto certificateDto = keymanagerService
					.getCertificate(RegistrationConstants.KERNEL_APP_ID, Optional.of(signRefId));

			if(certificateDto != null && certificateDto.getCertificate() != null)
				return certificateDto;

		} catch (Exception ex) {
			LOGGER.error("Error Fetching policy key from DB", ex);
		}
		return null;
	}
}
