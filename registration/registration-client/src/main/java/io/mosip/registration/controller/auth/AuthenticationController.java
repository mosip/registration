package io.mosip.registration.controller.auth;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.ProcessNames;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.device.Streamer;
import io.mosip.registration.controller.reg.PacketHandlerController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.security.AuthenticationService;
import io.mosip.registration.util.common.OTPManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Class for Operator Authentication
 *
 * 
 * 
 * 
 */
@Controller
public class AuthenticationController extends BaseController implements Initializable {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AuthenticationController.class);

	@FXML
	private AnchorPane temporaryLogin;
	@FXML
	private GridPane pwdBasedLogin;
	@FXML
	private GridPane otpBasedLogin;
	@FXML
	private GridPane fingerprintBasedLogin;
	@FXML
	private GridPane irisBasedLogin;
	@FXML
	private GridPane faceBasedLogin;
	@FXML
	private GridPane errorPane;
	@FXML
	private Label errorLabel;
	@FXML
	private Label errorText1;
	@FXML
	private Label errorText2;
	@FXML
	private Label otpValidity;
	@FXML
	private TextField fpUserId;
	@FXML
	private TextField irisUserId;
	@FXML
	private TextField faceUserId;
	@FXML
	private TextField username;
	@FXML
	private TextField password;
	@FXML
	private TextField otpUserId;
	@FXML
	private TextField otp;
	@FXML
	private GridPane operatorAuthenticationPane;
	@FXML
	private Button operatorAuthContinue;
	@FXML
	private Label registrationNavlabel;
	@FXML
	private Label otpLabel;
	@FXML
	private Label fpLabel;
	@FXML
	private Label irisLabel;
	@FXML
	private Label photoLabel;
	@FXML
	private Label pwdLabel;
	@FXML
	private Button getOTP;
	@FXML
	private ImageView irisImageView;
	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;

	@Autowired
	private PacketHandlerController packetHandlerController;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private OTPManager otpManager;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private LoginService loginService;

	@Autowired
	private Validations validations;

	private boolean isSupervisor = false;

	private boolean isEODAuthentication = false;

	private List<String> userAuthenticationTypeList;

	private List<String> userAuthenticationTypeListValidation;

	private List<String> userAuthenticationTypeListSupervisorValidation;

	private int authCount = 0;

	private String userNameField;

	@Autowired
	private BaseController baseController;

	@Autowired
	private BioService bioService;

	private int fingerPrintAuthCount;
	private int irisPrintAuthCount;
	private int facePrintAuthCount;

	@FXML
	private Label authCounter;

	/**
	 * to generate OTP in case of OTP based authentication
	 */
	public void generateOtp() {

		auditFactory.audit(isSupervisor ? AuditEvent.REG_SUPERVISOR_AUTH_GET_OTP : AuditEvent.REG_OPERATOR_AUTH_GET_OTP,
				Components.REG_OS_AUTH, otpUserId.getText(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Generate OTP for OTP based Authentication");

		if (!otpUserId.getText().isEmpty()) {
			// Response obtained from server
			ResponseDTO responseDTO = null;

			// Service Layer interaction
			responseDTO = otpManager.getOTP(otpUserId.getText());
			if (responseDTO.getSuccessResponseDTO() != null) {
				// Enable submit button
				// Generate alert to show OTP
				getOTP.setVisible(false);
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.OTP_GENERATION_SUCCESS_MESSAGE);
			} else if (responseDTO.getErrorResponseDTOs() != null) {
				// Generate Alert to show INVALID USERNAME
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.OTP_GENERATION_ERROR_MESSAGE);
			}

		} else {
			// Generate Alert to show username field was empty
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
		}
	}

	/**
	 * to validate OTP in case of OTP based authentication
	 */
	public void validateOTP() {

		auditFactory.audit(
				isSupervisor ? AuditEvent.REG_SUPERVISOR_AUTH_SUBMIT_OTP : AuditEvent.REG_OPERATOR_AUTH_SUBMIT_OTP,
				Components.REG_OS_AUTH,
				otpUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : otpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Validating OTP for OTP based Authentication");

		if (validations.validateTextField(operatorAuthenticationPane, otp, otp.getId(), true)) {
			if (isSupervisor) {
				if (!otpUserId.getText().isEmpty()) {
					if (fetchUserRole(otpUserId.getText())) {
						if (null != authenticationService.authValidator(RegistrationConstants.OTP, otpUserId.getText(),
								otp.getText(), haveToSaveAuthToken(otpUserId.getText()))) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = otpUserId.getText();
							if (!isEODAuthentication) {
								getOSIData().setSupervisorID(userNameField);
								getOSIData().setSuperviorAuthenticatedByPIN(true);
							}
							loadNextScreen();
						} else {
							generateAlert(RegistrationConstants.ERROR,
									RegistrationUIConstants.OTP_VALIDATION_ERROR_MESSAGE);
						}
					} else {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USER_NOT_AUTHORIZED);
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
				}
			} else {
				if (null != authenticationService.authValidator(RegistrationConstants.OTP, otpUserId.getText(),
						otp.getText(), haveToSaveAuthToken(otpUserId.getText()))) {
					if (!isEODAuthentication) {
						getOSIData().setOperatorAuthenticatedByPIN(true);
					}
					userAuthenticationTypeListValidation.remove(0);
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.OTP_VALIDATION_ERROR_MESSAGE);
				}
			}
		}
	}

	public void validatePwd() {

		auditFactory.audit(
				isSupervisor ? AuditEvent.REG_SUPERVISOR_AUTH_PASSWORD : AuditEvent.REG_OPERATOR_AUTH_PASSWORD,
				Components.REG_OS_AUTH,
				username.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : username.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		String status = RegistrationConstants.EMPTY;
		if (isSupervisor) {
			if (!username.getText().isEmpty()) {
				if (fetchUserRole(username.getText())) {
					if (password.getText().isEmpty()) {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PWORD_FIELD_EMPTY);
					} else {
						status = validatePwd(username.getText(), password.getText());
						if (RegistrationConstants.SUCCESS.equals(status)) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = username.getText();
							if (!isEODAuthentication) {
								getOSIData().setSupervisorID(userNameField);
								getOSIData().setSuperviorAuthenticatedByPassword(true);
							}
							loadNextScreen();
						} else if (RegistrationConstants.FAILURE.equals(status)) {
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.AUTHENTICATION_FAILURE);
						}
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USER_NOT_AUTHORIZED);
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
			}
		} else {
			if (!username.getText().isEmpty()) {
				if (password.getText().isEmpty()) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PWORD_FIELD_EMPTY);
				} else {
					status = validatePwd(username.getText(), password.getText());
					if (RegistrationConstants.SUCCESS.equals(status)) {
						userAuthenticationTypeListValidation.remove(0);
						userNameField = username.getText();
						if (!isEODAuthentication) {
							getOSIData().setOperatorAuthenticatedByPassword(true);
						}
						loadNextScreen();
					} else if (RegistrationConstants.FAILURE.equals(status)) {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.AUTHENTICATION_FAILURE);
					}
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
			}
		}

	}

	/**
	 * to validate the fingerprint in case of fingerprint based authentication
	 */
	public void validateFingerprint() {

		auditFactory.audit(
				isSupervisor ? AuditEvent.REG_SUPERVISOR_AUTH_FINGERPRINT : AuditEvent.REG_OPERATOR_AUTH_FINGERPRINT,
				Components.REG_OS_AUTH,
				fpUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : fpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		authCounter = new Label();

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Validating Fingerprint for Fingerprint based Authentication");

		if (isSupervisor) {
			if (!fpUserId.getText().isEmpty()) {
				if (fetchUserRole(fpUserId.getText())) {
					try {

						if (captureAndValidateFP(fpUserId.getText(), new MDMRequestDto(
								RegistrationConstants.FINGERPRINT_SLAB_LEFT, null, "Registration",
								io.mosip.registration.context.ApplicationContext
										.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
								io.mosip.registration.context.ApplicationContext
										.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
								1, io.mosip.registration.context.ApplicationContext.getIntValueFromApplicationMap(
										RegistrationConstants.FINGERPRINT_AUTHENTICATION_THRESHHOLD)))) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = fpUserId.getText();
							if (!isEODAuthentication) {
								getOSIData().setSupervisorID(userNameField);
							}
							loadNextScreen();
						} else {
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGER_PRINT_MATCH);
						}
					} catch (RegBaseCheckedException | IOException exception) {

						LOGGER.error("AuthenticationController", APPLICATION_NAME, APPLICATION_ID,
								"Exception while getting the scanned biometrics for user authentication: %s caused by %s"
										+ ExceptionUtils.getStackTrace(exception));
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR);
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USER_NOT_AUTHORIZED);
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
			}
		} else {
			try {
				if (captureAndValidateFP(fpUserId.getText(),
						new MDMRequestDto(RegistrationConstants.FINGERPRINT_SLAB_LEFT, null, "Registration",
								io.mosip.registration.context.ApplicationContext
										.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
								io.mosip.registration.context.ApplicationContext
										.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
								1, io.mosip.registration.context.ApplicationContext.getIntValueFromApplicationMap(
										RegistrationConstants.FINGERPRINT_AUTHENTICATION_THRESHHOLD)))) {
					userAuthenticationTypeListValidation.remove(0);
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGER_PRINT_MATCH);
				}
			} catch (RegBaseCheckedException | IOException exception) {
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.getMessageLanguageSpecific(exception.getMessage().substring(0, 3)
								+ RegistrationConstants.UNDER_SCORE + RegistrationConstants.MESSAGE.toUpperCase()));
			}
		}
		authCounter.setText(++fingerPrintAuthCount + "");
	}

	/**
	 * to validate the iris in case of iris based authentication
	 */
	public void validateIris() {

		auditFactory.audit(isSupervisor ? AuditEvent.REG_SUPERVISOR_AUTH_IRIS : AuditEvent.REG_OPERATOR_AUTH_IRIS,
				Components.REG_OS_AUTH,
				irisUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : irisUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		authCounter = new Label();

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Validating Iris for Iris based Authentication");

		if (isSupervisor) {
			if (!irisUserId.getText().isEmpty()) {
				if (fetchUserRole(irisUserId.getText())) {
					try {
						if (captureAndValidateIris(irisUserId.getText())) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = irisUserId.getText();
							if (!isEODAuthentication) {
								getOSIData().setSupervisorID(userNameField);
							}
							loadNextScreen();
						} else {
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.IRIS_MATCH);
						}
					} catch (RegBaseCheckedException | IOException exception) {
						generateAlert(RegistrationConstants.ALERT_INFORMATION,
								RegistrationUIConstants.getMessageLanguageSpecific(
										exception.getMessage().substring(0, 3) + RegistrationConstants.UNDER_SCORE
												+ RegistrationConstants.MESSAGE.toUpperCase()));
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USER_NOT_AUTHORIZED);
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
			}
		} else {
			try {
				if (captureAndValidateIris(irisUserId.getText())) {
					userAuthenticationTypeListValidation.remove(0);
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.IRIS_MATCH);
				}
			} catch (RegBaseCheckedException | IOException exception) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.NO_DEVICE_FOUND);
			}
		}
		authCounter.setText(++irisPrintAuthCount + "");
	}

	@FXML
	private ImageView faceImage;

	@Autowired
	private Streamer streamer;

	@FXML
	private void startStream() {
		faceImage.setImage(null);

		try {
			streamer.startStream(bioService.getStream(RegistrationConstants.FACE_FULLFACE), faceImage, null);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	/**
	 * to validate the face in case of face based authentication
	 * 
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	public void validateFace() {

		auditFactory.audit(isSupervisor ? AuditEvent.REG_SUPERVISOR_AUTH_FACE : AuditEvent.REG_OPERATOR_AUTH_FACE,
				Components.REG_OS_AUTH,
				faceUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : faceUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		authCounter = new Label();

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Validating Face for Face based Authentication");
		try {
			if (isSupervisor) {
				if (!faceUserId.getText().isEmpty()) {
					if (fetchUserRole(faceUserId.getText())) {
						if (captureAndValidateFace(faceUserId.getText())) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = faceUserId.getText();
							if (!isEODAuthentication) {
								getOSIData().setSupervisorID(userNameField);
							}
							loadNextScreen();
						} else {
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FACE_MATCH);
						}
					} else {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USER_NOT_AUTHORIZED);
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.USERNAME_FIELD_EMPTY);
				}
			} else {
				if (captureAndValidateFace(faceUserId.getText())) {
					userAuthenticationTypeListValidation.remove(0);
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FACE_MATCH);
				}
			}
		} catch (RegBaseCheckedException | IOException exception) {
			generateAlert(RegistrationConstants.ALERT_INFORMATION,
					RegistrationUIConstants.getMessageLanguageSpecific(exception.getMessage().substring(0, 3)
							+ RegistrationConstants.UNDER_SCORE + RegistrationConstants.MESSAGE.toUpperCase()));
		}

		authCounter.setText(++facePrintAuthCount + "");

	}

	/**
	 * to get the configured modes of authentication
	 * 
	 * @throws RegBaseCheckedException
	 */
	private void getAuthenticationModes(String authType) throws RegBaseCheckedException {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Loading configured modes of authentication");

		Set<String> roleSet = new HashSet<>(SessionContext.userContext().getRoles());

		userAuthenticationTypeList = loginService.getModesOfLogin(authType, roleSet);
		userAuthenticationTypeListValidation = loginService.getModesOfLogin(authType, roleSet);
		userAuthenticationTypeListSupervisorValidation = loginService.getModesOfLogin(authType, roleSet);

		if (userAuthenticationTypeList.isEmpty()) {
			isSupervisor = false;
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.AUTHENTICATION_ERROR_MSG);
			if (isEODAuthentication) {
				throw new RegBaseCheckedException();
			}
		} else {

			LOGGER.info(LoggerConstants.LOG_REG_AUTH, APPLICATION_NAME, APPLICATION_ID,
					"Ignoring FingerPrint, Iris, Face Authentication if the configuration is off");

			String fingerprintDisableFlag = getValueFromApplicationContext(
					RegistrationConstants.FINGERPRINT_DISABLE_FLAG);
			String irisDisableFlag = getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG);
			String faceDisableFlag = getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG);

			removeAuthModes(userAuthenticationTypeList, fingerprintDisableFlag,
					RegistrationConstants.FINGERPRINT_UPPERCASE);
			removeAuthModes(userAuthenticationTypeList, irisDisableFlag, RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeList, faceDisableFlag, RegistrationConstants.FACE);

			LOGGER.info(LoggerConstants.LOG_REG_AUTH, APPLICATION_NAME, APPLICATION_ID,
					"Ignoring FingerPrint, Iris, Face Supervisror Authentication if the configuration is off");

			removeAuthModes(userAuthenticationTypeListValidation, fingerprintDisableFlag,
					RegistrationConstants.FINGERPRINT);
			removeAuthModes(userAuthenticationTypeListValidation, irisDisableFlag, RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeListValidation, faceDisableFlag, RegistrationConstants.FACE);

			removeAuthModes(userAuthenticationTypeListSupervisorValidation, fingerprintDisableFlag,
					RegistrationConstants.FINGERPRINT);
			removeAuthModes(userAuthenticationTypeListSupervisorValidation, irisDisableFlag,
					RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeListSupervisorValidation, faceDisableFlag,
					RegistrationConstants.FACE);

			loadNextScreen();
		}
	}

	/**
	 * to load the respective screen with respect to the list of configured
	 * authentication modes
	 */
	private void loadNextScreen() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Loading next authentication screen");
		try {
			Set<String> roleSet = new HashSet<>(SessionContext.userContext().getRoles());
			Boolean toogleBioException = false;
			if (!SessionContext.userMap().isEmpty()) {
				if (SessionContext.userMap().get(RegistrationConstants.IS_LOW_QUALITY_BIOMETRICS) == null) {
					SessionContext.userMap().put(RegistrationConstants.IS_LOW_QUALITY_BIOMETRICS, false);
				}
				toogleBioException = (Boolean) SessionContext.userContext().getUserMap()
						.get(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION)
						|| (Boolean) SessionContext.userContext().getUserMap()
								.get(RegistrationConstants.IS_LOW_QUALITY_BIOMETRICS);
			}

			if (!userAuthenticationTypeList.isEmpty()) {
				authCount++;
				String authenticationType = String
						.valueOf(userAuthenticationTypeList.get(RegistrationConstants.PARAM_ZERO));

				if (authenticationType.equalsIgnoreCase(RegistrationConstants.OTP)) {
					getOTP.setVisible(true);
				}
				if ((RegistrationConstants.DISABLE.equalsIgnoreCase(
						getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG))
						&& authenticationType.equalsIgnoreCase(RegistrationConstants.FINGERPRINT))
						|| (RegistrationConstants.DISABLE.equalsIgnoreCase(
								getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))
								&& authenticationType.equalsIgnoreCase(RegistrationConstants.IRIS))
						|| (RegistrationConstants.DISABLE.equalsIgnoreCase(
								getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG))
								&& authenticationType.equalsIgnoreCase(RegistrationConstants.FACE))) {

					enableErrorPage();
					if (!isEODAuthentication) {
						operatorAuthContinue.setDisable(true);
					}
				} else {
					loadAuthenticationScreen(authenticationType);
					if (!isEODAuthentication) {
						operatorAuthContinue.setDisable(false);
					}
				}
			} else {
				if (!isSupervisor) {

					/*
					 * Check whether the biometric exceptions are enabled and supervisor
					 * authentication is required
					 */
					if (!getRegistrationDTOFromSession().getBiometricExceptions().isEmpty()
							&& RegistrationConstants.ENABLE.equalsIgnoreCase(
									getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_AUTH_CONFIG))
							&& !roleSet.contains(RegistrationConstants.SUPERVISOR)) {
						authCount = 0;
						isSupervisor = true;
						getAuthenticationModes(ProcessNames.EXCEPTION.getType());
					} else {
						submitRegistration();
					}
				} else {
					if (isEODAuthentication) {

						baseController.updateAuthenticationStatus();
					} else {
						submitRegistration();
					}
				}
			}
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
	}

	/**
	 * to enable the respective authentication mode
	 * 
	 * @param loginMode
	 *            - name of authentication mode
	 */
	public void loadAuthenticationScreen(String loginMode) {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Loading the respective authentication screen in UI");
		errorPane.setVisible(false);
		pwdBasedLogin.setVisible(false);
		otpBasedLogin.setVisible(false);
		fingerprintBasedLogin.setVisible(false);
		faceBasedLogin.setVisible(false);
		irisBasedLogin.setVisible(false);

		switch (loginMode.toUpperCase()) {
		case RegistrationConstants.OTP:
			enableOTP();
			break;
		case RegistrationConstants.PWORD:
			enablePWD();
			break;
		case RegistrationConstants.FINGERPRINT_UPPERCASE:
			enableFingerPrint();
			break;
		case RegistrationConstants.IRIS:
			enableIris();
			break;
		case RegistrationConstants.FACE:
			enableFace();
			break;
		default:
			enablePWD();
		}

		userAuthenticationTypeList.remove(RegistrationConstants.PARAM_ZERO);
	}

	/**
	 * to enable the OTP based authentication mode and disable rest of modes
	 */
	private void enableErrorPage() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Enabling OTP based Authentication Screen in UI");

		pwdBasedLogin.setVisible(false);
		otpBasedLogin.setVisible(false);
		fingerprintBasedLogin.setVisible(false);
		irisBasedLogin.setVisible(false);
		faceBasedLogin.setVisible(false);
		errorPane.setVisible(true);
		errorPane.setDisable(false);
		errorText1.setText(RegistrationUIConstants.BIOMETRIC_DISABLE_SCREEN_4);
		errorText1.setVisible(true);
		errorText2.setText(RegistrationUIConstants.BIOMETRIC_DISABLE_SCREEN_3);
		errorText1.setVisible(true);

		if (isSupervisor) {
			errorLabel.setText(RegistrationUIConstants.SUPERVISOR_VERIFICATION);
		}
	}

	/**
	 * to enable the OTP based authentication mode and disable rest of modes
	 */
	private void enableOTP() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Enabling OTP based Authentication Screen in UI");

		otpLabel.setText(ApplicationContext.applicationLanguageBundle().getString("otpAuthentication"));
		otpBasedLogin.setVisible(true);
		otp.clear();
		otpUserId.clear();
		otpUserId.setEditable(false);
		if (isSupervisor) {
			otpLabel.setText(ApplicationContext.applicationLanguageBundle().getString("supervisorOtpAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				otpUserId.setText(userNameField);
			} else {
				otpUserId.setEditable(true);
			}
		} else {
			otpUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the password based authentication mode and disable rest of modes
	 */
	private void enablePWD() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Enabling Password based Authentication Screen in UI");

		pwdLabel.setText(ApplicationContext.applicationLanguageBundle().getString("pwdAuthentication"));
		pwdBasedLogin.setVisible(true);
		username.clear();
		password.clear();
		username.setEditable(false);
		if (isSupervisor) {
			pwdLabel.setText(ApplicationContext.applicationLanguageBundle().getString("supervisorPwdAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				username.setText(userNameField);
			} else {
				username.setEditable(true);
			}
		} else {
			username.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the fingerprint based authentication mode and disable rest of modes
	 */
	private void enableFingerPrint() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Enabling Fingerprint based Authentication Screen in UI");

		fpLabel.setText(ApplicationContext.applicationLanguageBundle().getString("fpAuthentication"));
		fingerprintBasedLogin.setVisible(true);
		fpUserId.clear();
		fpUserId.setEditable(false);
		if (isSupervisor) {
			fpLabel.setText(ApplicationContext.applicationLanguageBundle().getString("supervisorFpAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				fpUserId.setText(userNameField);
			} else {
				fpUserId.setEditable(true);
			}
		} else {
			fpUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the iris based authentication mode and disable rest of modes
	 */
	private void enableIris() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Enabling Iris based Authentication Screen in UI");

		irisLabel.setText(ApplicationContext.applicationLanguageBundle().getString("irisAuthentication"));
		irisBasedLogin.setVisible(true);
		irisUserId.clear();
		irisUserId.setEditable(false);
		if (isSupervisor) {
			irisLabel.setText(ApplicationContext.applicationLanguageBundle().getString("supervisorIrisAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				irisUserId.setText(userNameField);
			} else {
				irisUserId.setEditable(true);
			}
		} else {
			irisUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the face based authentication mode and disable rest of modes
	 */
	private void enableFace() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Enabling Face based Authentication Screen in UI");

		photoLabel.setText(ApplicationContext.applicationLanguageBundle().getString("photoAuthentication"));
		faceBasedLogin.setVisible(true);
		faceUserId.clear();
		faceUserId.setEditable(false);
		if (isSupervisor) {
			photoLabel.setText(ApplicationContext.applicationLanguageBundle().getString("supervisorPhotoAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				faceUserId.setText(userNameField);
			} else {
				faceUserId.setEditable(true);
			}
		} else {
			faceUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to check the role of supervisor in case of biometric exception
	 * 
	 * @param userId
	 *            - username entered by the supervisor in the authentication screen
	 * @return boolean variable "true", if the person is authenticated as supervisor
	 *         or "false", if not
	 */
	private boolean fetchUserRole(String userId) {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Fetching the user role in case of Supervisor Authentication");

		UserDTO userDTO = loginService.getUserDetail(userId);
		if (userDTO != null) {
			return userDTO.getUserRole().stream()
					.anyMatch(userRole -> userRole.getRoleCode().equalsIgnoreCase(RegistrationConstants.SUPERVISOR)
							|| userRole.getRoleCode().equalsIgnoreCase(RegistrationConstants.ADMIN_ROLE)
							|| userRole.getRoleCode().equalsIgnoreCase(RegistrationConstants.ROLE_DEFAULT));
		}
		return false;
	}

	/**
	 * to capture and validate the fingerprint for authentication
	 * 
	 * @param userId
	 *            - username entered in the textfield
	 * @return true/false after validating fingerprint
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	private boolean captureAndValidateFP(String userId, MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException, IOException {
		List<BiometricsDto> biometrics = bioService.captureModalityForAuth(mdmRequestDto);


		boolean fpMatchStatus;
		if (!isEODAuthentication) {
			if (isSupervisor) {
				RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
						.get(RegistrationConstants.REGISTRATION_DATA);
				// registrationDTO.getBiometricDTO().getSupervisorBiometricDTO().setFingerprintDetailsDTO(fingerPrintDetailsDTOs);
				registrationDTO.addSupervisorBiometrics(biometrics);
			} else {
				RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
						.get(RegistrationConstants.REGISTRATION_DATA);
				// registrationDTO.getBiometricDTO().getOperatorBiometricDTO().setFingerprintDetailsDTO(fingerPrintDetailsDTOs);
				registrationDTO.addOfficerBiometrics(biometrics);
			}
		}
		fpMatchStatus = authenticationService.authValidator(userId, SingleType.FINGER.value(), biometrics);


		return fpMatchStatus;
	}

	/**
	 * to capture and validate the iris for authentication
	 * 
	 * @param userId
	 *            - username entered in the textfield
	 * @return true/false after validating iris
	 * @throws IOException
	 */
	private boolean captureAndValidateIris(String userId) throws RegBaseCheckedException, IOException {
		MDMRequestDto mdmRequestDto = new MDMRequestDto(RegistrationConstants.IRIS_DOUBLE, null, "Registration",
				io.mosip.registration.context.ApplicationContext
						.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
				io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
				2, io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.IRIS_THRESHOLD));
		List<BiometricsDto> biometrics = bioService.captureModalityForAuth(mdmRequestDto);

		/*
		 * AuthenticationValidatorDTO authenticationValidatorDTO =
		 * bioService.getIrisAuthenticationDto(userId, new
		 * RequestDetail(RegistrationConstants.IRIS_DOUBLE,
		 * getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 2,
		 * getValueFromApplicationContext(RegistrationConstants.IRIS_THRESHOLD), null));
		 * List<IrisDetailsDTO> irisDetailsDTOs =
		 * authenticationValidatorDTO.getIrisDetails();
		 */
		if (!isEODAuthentication) {
			if (isSupervisor) {
				RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
						.get(RegistrationConstants.REGISTRATION_DATA);
				// registrationDTO.getBiometricDTO().getSupervisorBiometricDTO().setIrisDetailsDTO(irisDetailsDTOs);
				// SessionContext.getInstance().getMapObject().get(RegistrationConstants.REGISTRATION_DATA);
				registrationDTO.addSupervisorBiometrics(biometrics);
			} else {
				RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
						.get(RegistrationConstants.REGISTRATION_DATA);
				// registrationDTO.getBiometricDTO().getOperatorBiometricDTO().setIrisDetailsDTO(irisDetailsDTOs);
				registrationDTO.addOfficerBiometrics(biometrics);
			}
		}
		// boolean irisMatchStatus =
		// bioService.validateIris(authenticationValidatorDTO);
		return authenticationService.authValidator(userId, SingleType.IRIS.value(), biometrics);
	}

	/**
	 * to capture and validate the iris for authentication
	 * 
	 * @param userId
	 *            - username entered in the textfield
	 * @return true/false after validating face
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	private boolean captureAndValidateFace(String userId) throws RegBaseCheckedException, IOException {
		MDMRequestDto mdmRequestDto = new MDMRequestDto(RegistrationConstants.FACE_FULLFACE, null, "Registration",
				io.mosip.registration.context.ApplicationContext
						.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
				io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
				1, io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.FACE_THRESHOLD));

		List<BiometricsDto> biometrics = bioService.captureModalityForAuth(mdmRequestDto);

		/*
		 * AuthenticationValidatorDTO authenticationValidatorDTO =
		 * bioService.getFaceAuthenticationDto(userId, new
		 * RequestDetail(RegistrationConstants.FACE_FULLFACE,
		 * getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 1,
		 * getValueFromApplicationContext(RegistrationConstants.FACE_THRESHOLD), null));
		 * FaceDetailsDTO faceDetailsDTO = authenticationValidatorDTO.getFaceDetail();
		 */
		if (!isEODAuthentication) {
			if (isSupervisor) {
				RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
						.get(RegistrationConstants.REGISTRATION_DATA);
				// registrationDTO.getBiometricDTO().getSupervisorBiometricDTO().setFace(faceDetailsDTO);
				// SessionContext.getInstance().getMapObject().get(RegistrationConstants.REGISTRATION_DATA);
				registrationDTO.addSupervisorBiometrics(biometrics);
			} else {
				RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
						.get(RegistrationConstants.REGISTRATION_DATA);
				// registrationDTO.getBiometricDTO().getOperatorBiometricDTO().setFace(faceDetailsDTO);
				registrationDTO.addOfficerBiometrics(biometrics);
			}
		}
		// return bioService.validateFace(authenticationValidatorDTO);
		return authenticationService.authValidator(userId, SingleType.FACE.value(), biometrics);
	}

	/**
	 * to submit the registration after successful authentication
	 */
	public void submitRegistration() {
		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Submit Registration after Operator Authentication");

		packetHandlerController.showReciept();
	}

	/**
	 * event class to exit from authentication window. pop up window.
	 * 
	 * @param event
	 *            - the action event
	 */
	public void exitWindow(ActionEvent event) {
		Stage primaryStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		primaryStage.close();

	}

	/**
	 * Setting the init method to the Basecontroller
	 * 
	 * @param parentControllerObj
	 *            - Parent Controller name
	 * @param authType
	 *            - Authentication Type
	 * @throws RegBaseCheckedException
	 */
	public void init(BaseController parentControllerObj, String authType) throws RegBaseCheckedException {
		authCount = 0;
		isSupervisor = true;
		isEODAuthentication = true;
		baseController = parentControllerObj;
		getAuthenticationModes(authType);

	}

	public void initData(String authType) throws RegBaseCheckedException {
		authCount = 0;
		int otpExpirySeconds = Integer
				.parseInt((getValueFromApplicationContext(RegistrationConstants.OTP_EXPIRY_TIME)).trim());
		int minutes = otpExpirySeconds / 60;
		String seconds = String.valueOf(otpExpirySeconds % 60);
		seconds = seconds.length() < 2 ? "0" + seconds : seconds;
		otpValidity.setText(RegistrationUIConstants.OTP_VALIDITY + " " + minutes + ":" + seconds + " "
				+ RegistrationUIConstants.MINUTES);
		stopTimer();
		isSupervisor = false;
		isEODAuthentication = false;
		getAuthenticationModes(authType);
	}

	private OSIDataDTO getOSIData() {
		return ((RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA)).getOsiDataDTO();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		int FingerPrintAuthCount = 0;
		int IrisPrintAuthCount = 0;
		int FacePrintAuthCount = 0;

		setImageOnHover();

		irisImageView.setImage(
				new Image(getClass().getResource(RegistrationConstants.RIGHT_IRIS_IMG_PATH).toExternalForm()));
		int otpExpirySeconds = Integer
				.parseInt((getValueFromApplicationContext(RegistrationConstants.OTP_EXPIRY_TIME)).trim());
		int minutes = otpExpirySeconds / 60;
		String seconds = String.valueOf(otpExpirySeconds % 60);
		seconds = seconds.length() < 2 ? "0" + seconds : seconds;
		otpValidity.setText(RegistrationUIConstants.OTP_VALIDITY + " " + minutes + ":" + seconds);
		stopTimer();
		if (getRegistrationDTOFromSession() != null
				&& getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory() != null
				&& getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
						.equals(RegistrationConstants.PACKET_TYPE_LOST)) {
			registrationNavlabel.setText(
					ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));
		}

		if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getSelectionListDTO() != null) {
			registrationNavlabel.setText(ApplicationContext.applicationLanguageBundle()
					.getString(RegistrationConstants.UIN_UPDATE_UINUPDATENAVLBL));
		}
	}

	private void setImageOnHover() {
		Image backInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK_FOCUSED));
		Image backImage = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK));
		Image scanInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.SCAN_FOCUSED));
		Image scanImage = new Image(getClass().getResourceAsStream(RegistrationConstants.SCAN));

		backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				backImageView.setImage(backInWhite);
			} else {
				backImageView.setImage(backImage);
			}
		});
	}

	public void goToPreviousPage() {
		auditFactory.audit(AuditEvent.REG_PREVIEW_BACK, Components.REG_PREVIEW, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_OPERATORAUTHENTICATIONPANE, false);
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW, true);
			registrationController.showUINUpdateCurrentPage();

		} else {
			registrationController.showCurrentPage(RegistrationConstants.OPERATOR_AUTHENTICATION,
					getPageByAction(RegistrationConstants.OPERATOR_AUTHENTICATION, RegistrationConstants.PREVIOUS));
		}
	}

	public void goToNextPage() {
		if (userAuthenticationTypeListValidation.isEmpty()) {
			userAuthenticationTypeListValidation = userAuthenticationTypeListSupervisorValidation;
		}

		switch (userAuthenticationTypeListValidation.get(0).toUpperCase()) {
		case RegistrationConstants.OTP:
			validateOTP();
			break;
		case RegistrationConstants.PWORD:
			validatePwd();
			break;
		case RegistrationConstants.FINGERPRINT_UPPERCASE:
			validateFingerprint();
			break;
		case RegistrationConstants.IRIS:
			validateIris();
			break;
		case RegistrationConstants.FACE:
			validateFace();
			break;
		default:

		}

	}

	/**
	 * This method will remove the auth method from list
	 * 
	 * @param authList
	 *            authentication list
	 * @param disableFlag
	 *            configuration flag
	 * @param authCode
	 *            auth mode
	 */
	private void removeAuthModes(List<String> authList, String flag, String authCode) {

		LOGGER.info(LoggerConstants.LOG_REG_AUTH, APPLICATION_NAME, APPLICATION_ID,
				"Ignoring FingerPrint, Iris, Face Authentication if the configuration is off");

		authList.removeIf(auth -> authList.size() > 1 && RegistrationConstants.DISABLE.equalsIgnoreCase(flag)
				&& auth.equalsIgnoreCase(authCode));
	}

	private boolean haveToSaveAuthToken(String userId) {
		return SessionContext.userId().equals(userId);
	}

}
