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
import java.util.Optional;
import java.util.stream.Collectors;

import io.mosip.registration.context.SessionContext;
import lombok.NonNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.id.UserRoleId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.UserDetailService;
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

		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Entering into user detail save method...");
		try {

			//Precondition check, proceed only if met, otherwise throws exception
			proceedWithMasterAndKeySync(null);

			LinkedHashMap<String, Object> userDetailSyncResponse = getUsrDetails(triggerPoint);
			if (null == userDetailSyncResponse.get(RegistrationConstants.RESPONSE)) {
				setErrorResponse(responseDTO, RegistrationConstants.ERROR, null);
				return responseDTO;
			}

			String jsonString = new ObjectMapper()
					.writeValueAsString(userDetailSyncResponse.get(RegistrationConstants.RESPONSE));
			JSONObject jsonObject = new JSONObject(jsonString);

			if (jsonObject.has("userDetails")) {
				byte[] data = clientCryptoFacade
						.decrypt(CryptoUtil.decodeBase64((String) jsonObject.get("userDetails")));
				jsonString = new String(data);
			}

			List<UserDetailDto> userDtls = objectMapper.readValue(jsonString,
					new TypeReference<List<UserDetailDto>>() {});

			//Remove users who are not part of current sync
			List<UserDetail> existingUserDetails = userDetailDAO.getAllUsers();
			for (UserDetail existingUserDetail : existingUserDetails) {
				Optional<UserDetailDto> result = userDtls.stream().filter(userDetailDto -> userDetailDto
						.getUserName().equalsIgnoreCase(existingUserDetail.getId())).findFirst();
				if (!result.isPresent()) {
					LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
							"Deleting User : " + existingUserDetail.getId());
					userDetailDAO.deleteUser(existingUserDetail);
				}
			}

			userDtls.forEach(user -> userDetailDAO.save(user));

			responseDTO = setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, null);
			LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					"User Detail Sync Success......");

		} catch (RegBaseCheckedException | IOException exception) {
			LOGGER.error(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			setErrorResponse(responseDTO, exception.getMessage(), null);
		}
		return responseDTO;
	}


	private LinkedHashMap<String, Object> getUsrDetails(String triggerPoint) throws RegBaseCheckedException {

		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Entering into user detail rest calling method");

		ResponseDTO responseDTO = new ResponseDTO();
		List<ErrorResponseDTO> erResponseDTOs = new ArrayList<>();
		LinkedHashMap<String, Object> userDetailResponse = null;

		// Setting uri Variables
		Map<String, String> requestParamMap = new LinkedHashMap<>();
		String keyIndex = CryptoUtil
				.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null);
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

		} catch (Exception exception) {
			LOGGER.error(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(exception));
			throw new RegBaseCheckedException(exception.getMessage(),
					exception.getLocalizedMessage());
		}

		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Leaving into user detail rest calling method");

		return userDetailResponse;
	}


	@Override
	public List<UserDetail> getAllUsers() {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Get All Users from UserDetail");
		return userDetailDAO.getAllUsers();
	}
	
	@Override
	public List<String> getUserRoleByUserId(String userId) {
		LOGGER.info(LOG_REG_MASTER_SYNC, APPLICATION_NAME, APPLICATION_ID,
				"Finding role for the UserID : " + userId);
		
		List<UserRole> userRoles = userDetailDAO.getUserRoleByUserId(userId);
		List<UserRoleId> userRoleIdList = userRoles.stream().map(UserRole::getUserRoleId).collect(Collectors.toList());
		return userRoleIdList.stream().map(UserRoleId::getRoleCode).collect(Collectors.toList());	
	}

	@Override
	public boolean isValidUser(@NonNull String userId) {
		return (null == userDetailDAO.getUserDetail(userId)) ? false : true;
	}
}
