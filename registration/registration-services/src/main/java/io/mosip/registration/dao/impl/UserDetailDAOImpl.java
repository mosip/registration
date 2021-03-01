package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_USER_DETAIL;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_USER_DETAIL_DAO;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserPassword;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.UserToken;
import io.mosip.registration.entity.id.UserRoleId;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.UserBiometricRepository;
import io.mosip.registration.repositories.UserDetailRepository;
import io.mosip.registration.repositories.UserPwdRepository;
import io.mosip.registration.repositories.UserRoleRepository;
import io.mosip.registration.repositories.UserTokenRepository;

/**
 * The implementation class of {@link UserDetailDAO}.
 *
 * @author Sravya Surampalli
 * @since 1.0.0
 */
@Repository
@Transactional
public class UserDetailDAOImpl implements UserDetailDAO {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(UserDetailDAOImpl.class);

	/** The userDetail repository. */
	@Autowired
	private UserDetailRepository userDetailRepository;

	/** The userPwd repository. */
	@Autowired
	private UserPwdRepository userPwdRepository;

	/** The userRole repository. */
	@Autowired
	private UserRoleRepository userRoleRepository;

	/** The userBiometric repository. */
	@Autowired
	private UserBiometricRepository userBiometricRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationUserDetailDAO#getUserDetail(java.lang.
	 * String)
	 */
	public UserDetail getUserDetail(String userId) {

		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Fetching User details");

		UserDetail userDetail = userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(userId);

		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"User details fetched successfully");

		return userDetail;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.dao.UserDetailDAO#updateLoginParams(io.
	 * mosip.registration.entity.UserDetail)
	 */
	public void updateLoginParams(UserDetail userDetail) {

		LOGGER.info("REGISTRATION - UPDATE_LOGIN_PARAMS - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "Updating Login params");

		userDetailRepository.save(userDetail);

		LOGGER.info("REGISTRATION - UPDATE_LOGIN_PARAMS - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "Updated Login params successfully");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mosip.registration.dao.RegistrationUserDetailDAO#getAllActiveUsers(java.
	 * lang. String)
	 */
	public List<UserBiometric> getAllActiveUsers(String attrCode) {

		LOGGER.info("REGISTRATION - ACTIVE_USERS - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Fetching all active users");

		return userBiometricRepository.findByUserBiometricIdBioAttributeCodeAndIsActiveTrue(attrCode);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mosip.registration.dao.RegistrationUserDetailDAO#
	 * getUserSpecificBioDetails(java.lang. String, java.lang.String)
	 */
	public List<UserBiometric> getUserSpecificBioDetails(String userId, String bioType) {

		LOGGER.info("REGISTRATION - USER_SPECIFIC_BIO - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "Fetching user specific biometric details");

		return userBiometricRepository
				.findByUserBiometricIdUsrIdAndIsActiveTrueAndUserBiometricIdBioTypeCodeIgnoreCase(userId, bioType);
	}

	public void save(UserDetailDto userDetailDto) {
		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID, "Entering user detail save method...");
		UserDetail existingUserDetail = userDetailRepository.findByIdIgnoreCase(userDetailDto.getUserName());

		UserDetail userDetail = new UserDetail();
		UserPassword usrPwd = new UserPassword();

		boolean userNewStatus = userDetailDto.getIsActive() != null ? userDetailDto.getIsActive().booleanValue() : true;
		if(existingUserDetail != null) {
			deleteUserRole(existingUserDetail.getName());//cleanup existing roles
			usrPwd.setPwd(existingUserDetail.getUserPassword().getPwd());
			userDetail.setSalt(existingUserDetail.getSalt());

			if(!userNewStatus) {//clean all auth token belonging to inactive user
				LOGGER.debug(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID, "Removed auth token of user : " +
						existingUserDetail.getName());
				userTokenRepository.deleteByUsrId(existingUserDetail.getName());
			}
		}

		usrPwd.setUsrId(userDetailDto.getUserName());
		usrPwd.setStatusCode("00");
		usrPwd.setIsActive(userNewStatus);
		usrPwd.setLangCode(ApplicationContext.applicationLanguage());
		usrPwd.setCrBy(SessionContext.isSessionContextAvailable() ? SessionContext.userContext().getUserId() :
				RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
		usrPwd.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

		userDetail.setId(userDetailDto.getUserName());
		userDetail.setUserPassword(usrPwd);
		userDetail.setEmail(userDetailDto.getMail());
		userDetail.setMobile(userDetailDto.getMobile());
		userDetail.setName(userDetailDto.getName());
		userDetail.setLangCode(ApplicationContext.applicationLanguage());
		userDetail.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		userDetail.setIsActive(userNewStatus);
		userDetail.setCrBy(SessionContext.isSessionContextAvailable() ? SessionContext.userContext().getUserId() :
				RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
		userDetail.setStatusCode("00");

		userDetailRepository.save(userDetail);
		userPwdRepository.save(usrPwd);

		userDetailDto.getRoles().forEach(role -> {
			UserRole userRole = new UserRole();
			userRole.setIsActive(userNewStatus);
			userRole.setLangCode(ApplicationContext.applicationLanguage());
			userRole.setCrBy(SessionContext.isSessionContextAvailable() ? SessionContext.userContext().getUserId() :
					RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
			userRole.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
			UserRoleId roleId = new UserRoleId();
			roleId.setRoleCode(role);
			roleId.setUsrId(userDetailDto.getUserName());
			userRole.setUserRoleId(roleId);
			userRoleRepository.save(userRole);
		});
		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID, "leaving user detail save method...");
	}


	@Override
	public UserBiometric getUserSpecificBioDetail(String userId, String bioType, String subType) {
		LOGGER.info("REGISTRATION - USER_SPECIFIC_BIO - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME,
				APPLICATION_ID, "Fetching user specific subtype level biometric detail");

		return userBiometricRepository
				.findByUserBiometricIdUsrIdAndIsActiveTrueAndUserBiometricIdBioTypeCodeAndUserBiometricIdBioAttributeCodeIgnoreCase(
						userId, bioType, subType);
	}

	@Override
	public List<UserBiometric> findAllActiveUsers(String bioType) {
		LOGGER.info(LOG_REG_USER_DETAIL, APPLICATION_NAME, APPLICATION_ID,
				"Fetching all local users for bioType >>> " + bioType);
		return userBiometricRepository.findByUserBiometricIdBioTypeCodeAndIsActiveTrue(bioType);
	}

	@Override
	public void updateAuthTokens(String userId, String authToken, String refreshToken, long tokenExpiry,
			long refreshTokenExpiry) {
		UserDetail userDetail = userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(userId);
		UserToken userToken = null;
		if (userDetail != null) {
			if (userDetail.getUserToken() == null) {
				userToken = new UserToken();
				userToken.setUsrId(userId);
				userToken.setCrDtime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
				userToken.setCrBy("System");
				userToken.setIsActive(true);
			} else
				userToken = userDetail.getUserToken();

			userToken.setToken(authToken);
			userToken.setRefreshToken(refreshToken);
			userToken.setTokenExpiry(tokenExpiry);
			userToken.setRtokenExpiry(refreshTokenExpiry);
			userToken.setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

			userTokenRepository.save(userToken);

			userDetail.setUserToken(userToken);
			userDetailRepository.save(userDetail);
		}
	}

	@Override
	public void updateUserPwd(String userId, String password) throws Exception {
		UserDetail userDetail = userDetailRepository.findByIdIgnoreCaseAndIsActiveTrue(userId);
		if (userDetail != null) {
			if (userDetail.getSalt() == null)
				userDetail
						.setSalt(CryptoUtil.encodeBase64(DateUtils.formatToISOString(LocalDateTime.now()).getBytes()));

			userDetail.getUserPassword().setPwd(HMACUtils2.digestAsPlainTextWithSalt(password.getBytes(),
					CryptoUtil.decodeBase64(userDetail.getSalt())));
			userDetail.getUserPassword().setUpdDtimes(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

			userPwdRepository.save(userDetail.getUserPassword());
			userDetailRepository.save(userDetail);
		}
	}

	@Override
	public List<UserDetail> getAllUsers() {
		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Fetching All User details");
		return userDetailRepository.findAll();
	}

	@Override
	public void deleteUser(UserDetail userDetail) {
		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Deleting user  : " + userDetail.getId());
		userDetailRepository.delete(userDetail);
	}

	@Override
	public void deleteUserRole(String userName) {
		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Deleting Roles for user : " + userName);
		
		userRoleRepository.delete(userName);
	}

	@Override
	public void update(UserDetail userDetail) {

		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Updating User : " + userDetail.getId());

		userDetailRepository.update(userDetail);
	}
	
	@Override
	public List<UserRole> getUserRoleByUserId(String userId) {
		LOGGER.info("REGISTRATION - USER_DETAIL - REGISTRATION_USER_DETAIL_DAO_IMPL", APPLICATION_NAME, APPLICATION_ID,
				"Finding role for the UserID : " + userId);
		
		return userRoleRepository.findByUserRoleIdUsrId(userId);
	}

}
