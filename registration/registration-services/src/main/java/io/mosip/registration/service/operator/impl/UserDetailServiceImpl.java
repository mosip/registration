package io.mosip.registration.service.operator.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_MASTER_SYNC;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_USER_DETAIL;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.dto.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * Implementation for {@link UserDetailService}
 * 
 * @author Sreekar Chukka
 *
 */
@Service
public class UserDetailServiceImpl extends BaseService implements UserDetailService {

	@Autowired
	private UserDetailDAO userDetailDAO;

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(UserDetailServiceImpl.class);

	private ObjectMapper objectMapper = new ObjectMapper();

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.UserDetailService#save()
	 */
	public synchronized ResponseDTO save(String triggerPoint) throws RegBaseCheckedException {
		ResponseDTO responseDTO = new ResponseDTO();
		if (triggerPointNullCheck(triggerPoint)) {
			LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					"Entering into user detail save method...");

			if(!RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
				LOGGER.error(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
						" Unable to sync user detail data as there is no internet connection");
				setErrorResponse(responseDTO, RegistrationConstants.ERROR, null);
				return responseDTO;
			}

			try {
				LinkedHashMap<String, Object> userDetailSyncResponse = getUsrDetails(triggerPoint);;

				if (null != userDetailSyncResponse &&
						userDetailSyncResponse.size() > 0 &&
						null != userDetailSyncResponse.get(RegistrationConstants.RESPONSE)) {

					String jsonString = new ObjectMapper().writeValueAsString(
							userDetailSyncResponse.get(RegistrationConstants.RESPONSE));
					JSONObject jsonObject = new JSONObject(jsonString);

					if(jsonObject.has("userDetails")) {
						byte[] data = clientCryptoFacade.decrypt(CryptoUtil.decodeBase64((String) jsonObject.get("userDetails")));
						jsonString = new String(data);
					}

					List<UserDetailDto> userDtls = objectMapper.readValue(jsonString,
							new TypeReference<List<UserDetailDto>>() {});

					if (userDtls != null && !userDtls.isEmpty()) {
						userDetailDAO.save(userDtls);
						responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);
						LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
								"User Detail Sync Success......");
						return responseDTO;
					}
				}
			} catch (RegBaseCheckedException | IOException exRegBaseCheckedException) {
				LOGGER.error(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(exRegBaseCheckedException));
			}
			setErrorResponse(responseDTO, RegistrationConstants.ERROR, null);

		} else {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID, RegistrationConstants.TRIGGER_POINT_MSG);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_TRIGGER_POINT_MISSING.getErrorCode(),
					RegistrationExceptionConstants.REG_TRIGGER_POINT_MISSING.getErrorMessage());
		}
		return responseDTO;
	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<String, Object> getUsrDetails(String triggerPoint)
			throws RegBaseCheckedException {

		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Entering into user detail rest calling method");

		ResponseDTO responseDTO = new ResponseDTO();
		List<ErrorResponseDTO> erResponseDTOs = new ArrayList<>();
		LinkedHashMap<String, Object> userDetailResponse = null;

		// Setting uri Variables

		Map<String, String> requestParamMap = new LinkedHashMap<>();
		String keyIndex = CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null);
		requestParamMap.put("keyindex", keyIndex);

		try {

			userDetailResponse = (LinkedHashMap<String, Object>) serviceDelegateUtil
					.get(RegistrationConstants.USER_DETAILS_SERVICE_NAME, requestParamMap, true, triggerPoint);

			if (null != userDetailResponse.get(RegistrationConstants.RESPONSE)) {
				SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
				successResponseDTO.setCode(RegistrationConstants.SUCCESS);
				responseDTO.setSuccessResponseDTO(successResponseDTO);

			} else {

				ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
				errorResponseDTO.setCode(RegistrationConstants.ERRORS);

				errorResponseDTO.setMessage(userDetailResponse.size() > 0
						? ((List<LinkedHashMap<String, String>>) userDetailResponse.get(RegistrationConstants.ERRORS))
								.get(0).get(RegistrationConstants.ERROR_MSG)
						: "User Detail Restful service error");
				erResponseDTOs.add(errorResponseDTO);
				responseDTO.setErrorResponseDTOs(erResponseDTOs);
			}

		} catch (HttpClientErrorException httpClientErrorException) {
			LOGGER.error(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					httpClientErrorException.getRawStatusCode() + "Http error while pulling json from server"
							+ ExceptionUtils.getStackTrace(httpClientErrorException));
			throw new RegBaseCheckedException(Integer.toString(httpClientErrorException.getRawStatusCode()),
					httpClientErrorException.getStatusText());
		} catch (SocketTimeoutException socketTimeoutException) {
			LOGGER.error(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					socketTimeoutException.getMessage() + "Http error while pulling json from server"
							+ ExceptionUtils.getStackTrace(socketTimeoutException));
			throw new RegBaseCheckedException(socketTimeoutException.getMessage(),
					socketTimeoutException.getLocalizedMessage());
		}

		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Leaving into user detail rest calling method");

		return userDetailResponse;
	}

	/**
	 * Center id null check.
	 *
	 * @param triggerPoint the trigger point
	 * @return the linked hash map
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	/*private LinkedHashMap<String, Object> centerIdNullCheck(String triggerPoint) throws RegBaseCheckedException {
		LinkedHashMap<String, Object> userDetailSyncResponse = null;
		Map<String, String> mapOfcenterId = userOnboardService.getMachineCenterId();

		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Fetching registration center details......");

		if (null != mapOfcenterId && mapOfcenterId.size() > 0
				&& null != mapOfcenterId.get(RegistrationConstants.USER_CENTER_ID)) {

			LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					"Registration center id found....!" + mapOfcenterId.get(RegistrationConstants.USER_CENTER_ID));

			userDetailSyncResponse = getUsrDetails((String) mapOfcenterId.get(RegistrationConstants.USER_CENTER_ID),
					triggerPoint);
		} else {
			LOGGER.error(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					"Registration center id not found....!");
		}

		return userDetailSyncResponse;

	}*/

	/**
	 * trigger point null check.
	 *
	 * @param triggerPoint the language code
	 * @return true, if successful
	 */
	private boolean triggerPointNullCheck(String triggerPoint) {
		if (StringUtils.isEmpty(triggerPoint)) {
			LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
					"triggerPoint is missing it is a mandatory field.");
			return false;
		} else {
			return true;
		}

	}
}
