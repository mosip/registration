package io.mosip.registration.service.login.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_LOGIN_SERVICE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.mapper.CustomObjectMapper.MAPPER_FACADE;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.registration.constants.*;
import io.mosip.registration.dto.*;
import io.mosip.registration.service.sync.CertificateSyncService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.AppAuthenticationDAO;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.ScreenAuthorizationDAO;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PublicKeySync;
import io.mosip.registration.service.sync.TPMPublicKeySyncService;


/**
 * Implementation for {@link LoginService}
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 *
 */
@Service
public class LoginServiceImpl extends BaseService implements LoginService {
	
	private final String PUBLIC_KEY_SYNC_STEP = "PublicKey Sync";
	private final String MACHINE_KEY_VERIFICATION_STEP = "Machine-Key verification";
	private final String GLOBAL_PARAM_SYNC_STEP = "Global parameter Sync";
	private final String CLIENTSETTINGS_SYNC_STEP = "Client settings / Master data Sync";
	private final String USER_DETAIL_SYNC_STEP = "User detail Sync";
	private final String CACERT_SYNC_STEP = "CA CERT Sync";

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(LoginServiceImpl.class);

	/**
	 * Instance of {@code AuditFactory}
	 */
	@Autowired
	private AuditManagerService auditFactory;

	/**
	 * Class to retrieve the Login Details from DB
	 */
	@Autowired
	private AppAuthenticationDAO appAuthenticationDAO;

	/**
	 * Class to retrieve the Officer Details from DB
	 */
	@Autowired
	private UserDetailDAO userDetailDAO;

	/**
	 * Class to retrieve the Registration Center details from DB
	 */
	@Autowired
	private RegistrationCenterDAO registrationCenterDAO;

	/**
	 * Class to retrieve the Screen authorization from DB
	 */
	@Autowired
	private ScreenAuthorizationDAO screenAuthorizationDAO;

	@Autowired
	private PublicKeySync publicKeySyncImpl;

	@Autowired
	private GlobalParamService globalParamService;

	@Autowired
	private MasterSyncService masterSyncService;

	@Autowired
	private UserDetailService userDetailService;

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private TPMPublicKeySyncService tpmPublicKeySyncService;

	@Autowired
	private AuthTokenUtilService authTokenUtilService;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private CertificateSyncService certificateSyncService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.login.LoginService#getModesOfLogin(java.lang.
	 * String, java.util.Set)
	 */
	@Override
	public List<String> getModesOfLogin(String authType, Set<String> roleList) {
		// Retrieve Login information

		LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Fetching list of login modes");

		List<String> loginModes = new ArrayList<>();

		try {
			getModesOfLoginValidation(authType, roleList);

			boolean mandatePwdLogin = RegistrationAppHealthCheckUtil.isNetworkAvailable() && !authTokenUtilService.hasAnyValidToken();

			LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID, "PWD LOGIN MANDATED ? " + mandatePwdLogin);

			auditFactory.audit(AuditEvent.LOGIN_MODES_FETCH, Components.LOGIN_MODES,
					RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());
			if ((roleList != null && roleList.contains(RegistrationConstants.ROLE_DEFAULT))) {
				loginModes.clear();
				loginModes.add(RegistrationConstants.PWORD);
			}
			else {
				loginModes = appAuthenticationDAO.getModesOfLogin(authType, roleList);

				if(mandatePwdLogin) {
					Optional<String> pwdMode = loginModes.stream().filter(loginMode ->
							loginMode.equalsIgnoreCase(LoginMode.OTP.getCode()) ||
									loginMode.equalsIgnoreCase(LoginMode.PASSWORD.getCode()) ||
									loginMode.equalsIgnoreCase(RegistrationConstants.PWORD)).findFirst();

					LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID, "PWD LOGIN mode already present ? " + pwdMode.isPresent());
					if(!pwdMode.isPresent())
						loginModes.add(RegistrationConstants.PWORD);

					return loginModes;
				}

				if(loginModes != null && loginModes.size() > 1) {
					if(RegistrationConstants.DISABLE.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_DISABLE_FLAG))
						loginModes.remove(RegistrationConstants.FINGERPRINT);
					if(RegistrationConstants.DISABLE.equalsIgnoreCase(RegistrationConstants.IRIS_DISABLE_FLAG))
						loginModes.remove(RegistrationConstants.IRIS);
					if(RegistrationConstants.DISABLE.equalsIgnoreCase(RegistrationConstants.FACE_DISABLE_FLAG))
						loginModes.remove(RegistrationConstants.FACE);
				}
			}
			
			LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					"Completed fetching list of login modes");
			
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));

		}
		return loginModes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.login.LoginService#getUserDetail(java.lang.
	 * String)
	 */
	@Override
	public UserDTO getUserDetail(String userId) {
		// Retrieving Officer details
		LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Fetching User details");

		UserDTO userDTO = null;

		try {
			getUserDetailValidation(userId);

			auditFactory.audit(AuditEvent.FETCH_USR_DET, Components.USER_DETAIL, RegistrationConstants.APPLICATION_NAME,
					AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			userDTO = MAPPER_FACADE.map(userDetailDAO.getUserDetail(userId), UserDTO.class);
			
			LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					"Completed fetching User details, user found : " + (userDTO == null ? false : true));
			
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		return userDTO;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.login.LoginService#getRegistrationCenterDetails
	 * (java.lang.String, java.lang.String)
	 */
	@Override
	public RegistrationCenterDetailDTO getRegistrationCenterDetails(String centerId, String langCode) {
		// Retrieving Registration Center details

		LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Fetching Center details");

		RegistrationCenterDetailDTO registrationCenterDetailDTO = null;

		try {
			getRegistrationCenterDetailsValidation(centerId, langCode);
			
			auditFactory.audit(AuditEvent.FETCH_CNTR_DET, Components.CENTER_DETAIL,
					RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			registrationCenterDetailDTO = registrationCenterDAO.getRegistrationCenterDetails(centerId, langCode);			

			LOGGER.info(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					"Completed fetching of Center details");

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		return registrationCenterDetailDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.login.LoginService#
	 * getScreenAuthorizationDetails(java.util.List)
	 */
	@Override
	public AuthorizationDTO getScreenAuthorizationDetails(List<String> roleCode) {
		// Fetching screen authorization details

		LOGGER.info(LoggerConstants.LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Fetching list of Screens to be Authorized");
		
		AuthorizationDTO authorizationDTO = null;
		
		try {
			getScreenAuthorizationDetailsValidation(roleCode);
			
			auditFactory.audit(AuditEvent.FETCH_SCR_AUTH, Components.SCREEN_AUTH, RegistrationConstants.APPLICATION_NAME,
					AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			LOGGER.info(LoggerConstants.LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					"Completed fetching list of Screens to be Authorized");
			
			authorizationDTO = screenAuthorizationDAO.getScreenAuthorizationDetails(roleCode);

		} catch(RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		return authorizationDTO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.login.LoginService#updateLoginParams(io.mosip.
	 * registration.dto.UserDTO)
	 */
	public void updateLoginParams(UserDTO userDTO) {

		LOGGER.info("REGISTRATION - UPDATELOGINPARAMS - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID,
				"Updating Login Params");
		
		try {
			userDTOValidation(userDTO);
			
			UserDetail userDetail = userDetailDAO.getUserDetail(userDTO.getId());

			userDetail.setLastLoginDtimes(userDTO.getLastLoginDtimes());
			userDetail.setLastLoginMethod(userDTO.getLastLoginMethod());
			userDetail.setUnsuccessfulLoginCount(userDTO.getUnsuccessfulLoginCount());
			userDetail.setUserlockTillDtimes(userDTO.getUserlockTillDtimes());

			userDetailDAO.updateLoginParams(userDetail);

			LOGGER.info("REGISTRATION - UPDATELOGINPARAMS - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID,
					"Updated Login Params");

		} catch(RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}
	
	/* Must follow the same order on every initial sync
	 * 1. signing key sync
	 * 2. verify machine-key mapping
	 * 3. global parameters sync
	 * 4. client-settings / master-data sync
	 * 5. user details sync
	 * user salt sync is removed @Since 1.1.3
	 */
	@Override
	public List<String> initialSync(String triggerPoint) {
		long start = System.currentTimeMillis();
		LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, "Started Initial sync");
		List<String> results = new LinkedList<>();
		ResponseDTO responseDTO = null;
		
		try {
			long taskStart = System.currentTimeMillis();
			responseDTO = publicKeySyncImpl.getPublicKey(triggerPoint);
			validateResponse(responseDTO, PUBLIC_KEY_SYNC_STEP);
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, PUBLIC_KEY_SYNC_STEP+ " task completed in (ms) : " +
					(System.currentTimeMillis() - taskStart));

			taskStart = System.currentTimeMillis();
			responseDTO = tpmPublicKeySyncService.syncTPMPublicKey();
			validateResponse(responseDTO, MACHINE_KEY_VERIFICATION_STEP);
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, MACHINE_KEY_VERIFICATION_STEP+ " task completed in (ms) : " +
					(System.currentTimeMillis() - taskStart));

			String keyIndex = CryptoUtil.computeFingerPrint(clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null);
			ApplicationContext.map().put(RegistrationConstants.KEY_INDEX, keyIndex);

			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, "Initial Verifiation Done : " + MACHINE_KEY_VERIFICATION_STEP);

			taskStart = System.currentTimeMillis();
			responseDTO = globalParamService.synchConfigData(false);
			validateResponse(responseDTO, GLOBAL_PARAM_SYNC_STEP);
			if(responseDTO.getSuccessResponseDTO().getOtherAttributes() != null)
				results.add(RegistrationConstants.RESTART);
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, GLOBAL_PARAM_SYNC_STEP+ " task completed in (ms) : " +
					(System.currentTimeMillis() - taskStart));

			taskStart = System.currentTimeMillis();
			responseDTO = masterSyncService.getMasterSync(RegistrationConstants.OPT_TO_REG_MDS_J00001, triggerPoint) ;
			validateResponse(responseDTO, CLIENTSETTINGS_SYNC_STEP);
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, CLIENTSETTINGS_SYNC_STEP+ " task completed in (ms) : " +
					(System.currentTimeMillis() - taskStart));

			taskStart = System.currentTimeMillis();
			responseDTO = userDetailService.save(triggerPoint);
			validateResponse(responseDTO, USER_DETAIL_SYNC_STEP);
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, USER_DETAIL_SYNC_STEP+ " task completed in (ms) : " +
					(System.currentTimeMillis() - taskStart));

			taskStart = System.currentTimeMillis();
			responseDTO = certificateSyncService.getCACertificates(triggerPoint);
			validateResponse(responseDTO, CACERT_SYNC_STEP);
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, CACERT_SYNC_STEP+ " task completed in (ms) : " +
					(System.currentTimeMillis() - taskStart));

			if(isInitialSync()) {
				LoginUserDTO loginUserDTO = (LoginUserDTO) ApplicationContext.map().get(RegistrationConstants.USER_DTO);
				userDetailDAO.updateUserPwd(loginUserDTO.getUserId(), loginUserDTO.getPassword());
			}

			results.add(RegistrationConstants.SUCCESS);
			globalParamService.update(RegistrationConstants.INITIAL_SETUP, RegistrationConstants.DISABLE);
			
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, "completed Initial sync in (ms) : " +
					(System.currentTimeMillis() - start));
		
		} catch (Exception e) {
			LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
			results.add(isAuthTokenEmptyException(e) ? RegistrationConstants.AUTH_TOKEN_NOT_RECEIVED_ERROR : RegistrationConstants.FAILURE);			
		}
		return results;
	}

	//Not required as this validation is handled in ClientSecurityFacade
	/*private String verifyMachinePublicKeyMapping(boolean isInitialSetup) throws RegBaseCheckedException {
		final boolean tpmAvailable = RegistrationConstants.ENABLE.equals(getGlobalConfigValueOf(RegistrationConstants.TPM_AVAILABILITY));
		final String environment = getGlobalConfigValueOf(RegistrationConstants.SERVER_ACTIVE_PROFILE);

		if(RegistrationConstants.SERVER_PROD_PROFILE.equalsIgnoreCase(environment) && !tpmAvailable) {
			LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, "TPM IS REQUIRED TO BE ENABLED.");
			throw new RegBaseCheckedException(RegistrationExceptionConstants.TPM_REQUIRED.getErrorCode(),
					RegistrationExceptionConstants.TPM_REQUIRED.getErrorMessage());
		}

		LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, "CURRENT PROFILE : " +
				environment != null ? environment : RegistrationConstants.SERVER_NO_PROFILE);

		return tpmPublicKeySyncService.syncTPMPublicKey();
	}*/
	
	private void validateResponse(ResponseDTO responseDTO, String syncStep) throws RegBaseCheckedException {
		if(responseDTO == null)
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_SYNC_NO_RESPONSE.getErrorCode(), 
					RegistrationExceptionConstants.REG_SYNC_NO_RESPONSE.getErrorMessage());
		
		if(responseDTO.getErrorResponseDTOs() != null) {
			if(RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode()
			.equals(responseDTO.getErrorResponseDTOs().get(0).getMessage()))
				throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorCode(), 
						RegistrationExceptionConstants.AUTH_TOKEN_COOKIE_NOT_FOUND.getErrorMessage());
			else
				throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_SYNC_FAILURE.getErrorCode(),
						String.format(RegistrationExceptionConstants.REG_SYNC_FAILURE.getErrorMessage(), syncStep));
		}
		
		LOGGER.info("REGISTRATION  - LOGINSERVICE", APPLICATION_NAME, APPLICATION_ID, "Initial Sync Done : " + syncStep);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.login.LoginService#validateInvalidLogin(io.
	 * mosip.registration.dto.UserDTO, java.lang.String, int, int)
	 */
	public String validateInvalidLogin(UserDTO userDTO, String errorMessage, int invalidLoginCount,
			int invalidLoginTime) {

		LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID, "validating invalid login params");

		try {
			userDTOValidation(userDTO);

			int loginCount = userDTO.getUnsuccessfulLoginCount() != null
					? userDTO.getUnsuccessfulLoginCount().intValue()
					: RegistrationConstants.PARAM_ZERO;

			Timestamp loginTime = userDTO.getUserlockTillDtimes();

			LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
					"Comparing timestamps in case of invalid login attempts");

			if (loginCount >= invalidLoginCount
					&& TimeUnit.MILLISECONDS.toMinutes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()).getTime()
							- loginTime.getTime()) >= invalidLoginTime) {

				loginCount = RegistrationConstants.PARAM_ZERO;
				userDTO.setUnsuccessfulLoginCount(RegistrationConstants.PARAM_ZERO);

				updateLoginParams(userDTO);

			}

			if (loginCount >= invalidLoginCount) {

				LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
						"validating login count and time ");

				if (TimeUnit.MILLISECONDS
						.toMinutes(loginTime.getTime() - System.currentTimeMillis()) >= invalidLoginTime) {

					userDTO.setUnsuccessfulLoginCount(RegistrationConstants.PARAM_ONE);

					updateLoginParams(userDTO);

				} else {
					return RegistrationConstants.ERROR;
				}
				return "false";

			} else {
				if (!errorMessage.isEmpty()) {

					LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							"updating login count and time for invalid login attempts");
					loginCount = loginCount + RegistrationConstants.PARAM_ONE;
					userDTO.setUserlockTillDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
					userDTO.setUnsuccessfulLoginCount(loginCount);

					updateLoginParams(userDTO);

					if (loginCount >= invalidLoginCount) {
						return RegistrationConstants.ERROR;
					} else {
						return errorMessage;
					}
				}
				return "true";
			}

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}

		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.login.LoginService#validateUser(java.lang.
	 * String)
	 */
	public ResponseDTO validateUser(String userId) {
		ResponseDTO responseDTO = new ResponseDTO();
		
		LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID, "Validating User");
		
		try {
			getUserDetailValidation(userId);
			
			UserDTO userDTO = getUserDetail(userId);
			if (userDTO == null) {
				setErrorResponse(responseDTO, RegistrationConstants.USER_NAME_VALIDATION, null);
			} else {
				String stationId = getStationId();
				String centerId = getCenterId(stationId);

				//excluding the case where center is inactive, in which case centerId is null
				//We will need user to login when center is inactive to finish pending tasks
				if(centerId != null && !userDTO.getRegCenterUser().getRegcntrId().equals(centerId)) {
					setErrorResponse(responseDTO, RegistrationConstants.USER_MACHINE_VALIDATION_MSG, null);
					return responseDTO;
				}

				ApplicationContext.map().put(RegistrationConstants.USER_CENTER_ID, centerId);
				if (userDTO.getStatusCode().equalsIgnoreCase(RegistrationConstants.BLOCKED)) {
					setErrorResponse(responseDTO, RegistrationConstants.BLOCKED_USER_ERROR, null);
				} else {
					for (UserMachineMappingDTO userMachineMapping : userDTO.getUserMachineMapping()) {
						ApplicationContext.map().put(RegistrationConstants.DONGLE_SERIAL_NUMBER,
								userMachineMapping.getMachineMaster().getSerialNum());
					}

					Set<String> roleList = new LinkedHashSet<>();
					userDTO.getUserRole().forEach(roleCode -> {
						if (roleCode.isActive()) {
							roleList.add(String.valueOf(roleCode.getRoleCode()));
						}
					});

					LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID, "Validating roles");
					// Checking roles
					if (roleList.isEmpty() || !(roleList.contains(RegistrationConstants.OFFICER)
							|| roleList.contains(RegistrationConstants.SUPERVISOR)
							|| roleList.contains(RegistrationConstants.ADMIN_ROLE)
							|| roleList.contains(RegistrationConstants.ROLE_DEFAULT))) {
						setErrorResponse(responseDTO, RegistrationConstants.ROLES_EMPTY_ERROR, null);
					} else {
						ApplicationContext.map().put(RegistrationConstants.USER_STATION_ID, stationId);

						Map<String, Object> params = new LinkedHashMap<>();
						params.put(RegistrationConstants.ROLES_LIST, roleList);
						params.put(RegistrationConstants.USER_DTO, userDTO);
						setSuccessResponse(responseDTO, RegistrationConstants.SUCCESS, params);
					}
				}
			}

			LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID, "completed validating user");
			
		} catch(RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_LOGIN_SERVICE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
			
			setErrorResponse(responseDTO, RegistrationConstants.USER_NAME_VALIDATION, null);
		}
		return responseDTO;
	}
	
	/**
	 * Gets the modes of login validation.
	 *
	 * @param authType the auth type
	 * @param roleList the role list
	 * @return the modes of login validation
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	private void getModesOfLoginValidation(String authType, Set<String> roleList) throws RegBaseCheckedException {
		
		if(isStringEmpty(authType)) {			
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_LOGIN_AUTH_TYPE_EXCEPTION);
		} else if(isSetEmpty(roleList)) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_LOGIN_ROLES_EXCEPTION);			
		}
	}
	
	private void getUserDetailValidation(String userId) throws RegBaseCheckedException {
		
		if(isStringEmpty(userId)) {					
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_USER_ID_EXCEPTION);
		}
	}
	
	/**
	 * Gets the registration center details validation.
	 *
	 * @param centerId the center id
	 * @param langCode the lang code
	 * @return the registration center details validation
	 * @throws RegBaseCheckedException the reg base checked exception
	 */
	private void getRegistrationCenterDetailsValidation(String centerId, String langCode) throws RegBaseCheckedException {
		
		if(isStringEmpty(centerId)) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_LOGIN_CENTER_ID_EXCEPTION);
		} else if(isStringEmpty(langCode)) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_LOGIN_LANG_CODE_EXCEPTION);
		}
	}
	
	private void getScreenAuthorizationDetailsValidation(List<String> roleCode) throws RegBaseCheckedException {
		if(isListEmpty(roleCode)) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_LOGIN_ROLES_EXCEPTION);
		}
	}
	
	private void userDTOValidation(UserDTO userDTO) throws RegBaseCheckedException {
		
		if(null == userDTO) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_LOGIN_USER_DTO_EXCEPTION);
		} 
	}
}
