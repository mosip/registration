package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.device.BiometricsController;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.util.restclient.AuthTokenUtilService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

/**
 * {@code UserOnboardController} is to initialize user onboard
 * 
 * @author Dinesh Ashokan
 * @version 1.0
 *
 */
@Controller
public class UserOnboardController extends BaseController implements Initializable {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(UserOnboardController.class);

	@FXML
	private Label operatorName;
	@FXML
	private GridPane getOnboardedPane;
	@FXML
	private ImageView getOnboardedImageView;
	@FXML
	private GridPane onboardGridPane;
	@FXML
	private ImageView onboardImageView;
	@FXML
	private GridPane registerGridPane;
	@FXML
	private ImageView registerImageView;
	@FXML
	private GridPane syncDataGridPane;
	@FXML
	private ImageView syncDataImageView;
	@FXML
	private GridPane mapDevicesGridPane;
	@FXML
	private ImageView mapDevicesImageView;
	@FXML
	private GridPane uploadDataGridPane;
	@FXML
	private ImageView uploadDataImageView;
	@FXML
	private GridPane updateBiometricsGridPane;
	@FXML
	private ImageView updateBiometricsImageView;

	@Autowired
	private UserOnboardParentController userOnboardParentController;

	@Autowired
	private BiometricsController guardianBiometricsController;

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private AuthTokenUtilService authTokenUtilService;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setImagesOnHover();

		setImage(getOnboardedImageView, RegistrationConstants.GET_ONBOARD_IMG);
		setImage(onboardImageView, RegistrationConstants.ONBOARD_YOURSELF_IMG);
		setImage(registerImageView, RegistrationConstants.REGISTER_INDVIDUAL_IMG);
		setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);	
		setImage(mapDevicesImageView, RegistrationConstants.SYNC_IMG);
		setImage(uploadDataImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);		
		setImage(updateBiometricsImageView, RegistrationConstants.ACTIVE_BIOMETRIC_DETAILS_IMG);

		operatorName.setText(RegistrationUIConstants.USER_ONBOARD_HI + " " + SessionContext.userContext().getName()
				+ ", " + RegistrationUIConstants.USER_ONBOARD_NOTONBOARDED);
	}

	@FXML
	public void initUserOnboard() {
		clearOnboard();
		// BiometricDTO biometricDTO = new BiometricDTO();
		// biometricDTO.setOperatorBiometricDTO(createBiometricInfoDTO());
		userOnboardService.initializeOperatorBiometric();
		SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ, RegistrationConstants.DISABLE);
		userOnboardParentController.showCurrentPage(RegistrationConstants.ONBOARD_USER_PARENT,
				getOnboardPageDetails(RegistrationConstants.ONBOARD_USER_PARENT, RegistrationConstants.NEXT));

		clearAllValues();
		
		guardianBiometricsController.populateBiometricPage(true, false);
		
		LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "User Onboard Controller initUserOnboard Method Exit");
	}

	public void clearOnboard() {
		SessionContext.map().remove(RegistrationConstants.USER_ONBOARD_DATA);
		SessionContext.map().remove(RegistrationConstants.OLD_BIOMETRIC_EXCEPTION);
		SessionContext.map().remove(RegistrationConstants.NEW_BIOMETRIC_EXCEPTION);
	}

	@FXML
	public void onboardingYourself() {
		onboardGridPane.setOnMouseClicked(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(RegistrationConstants.MOSIP_URL));
				} catch (IOException ioException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
				} catch (URISyntaxException uriSyntaxException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							uriSyntaxException.getMessage() + ExceptionUtils.getStackTrace(uriSyntaxException));
				}
			}
		});
	}

	@FXML
	public void registeringIndividual() {
		registerGridPane.setOnMouseClicked(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(RegistrationConstants.MOSIP_URL));
				} catch (IOException ioException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
				} catch (URISyntaxException uriSyntaxException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							uriSyntaxException.getMessage() + ExceptionUtils.getStackTrace(uriSyntaxException));
				}
			}
		});
	}

	@FXML
	public void synchronizingData() {
		syncDataGridPane.setOnMouseClicked(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(RegistrationConstants.MOSIP_URL));
				} catch (IOException ioException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
				} catch (URISyntaxException uriSyntaxException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							uriSyntaxException.getMessage() + ExceptionUtils.getStackTrace(uriSyntaxException));
				}
			}
		});
	}

	@FXML
	public void mappingDevices() {
		mapDevicesGridPane.setOnMouseClicked(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(RegistrationConstants.MOSIP_URL));
				} catch (IOException ioException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
				} catch (URISyntaxException uriSyntaxException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							uriSyntaxException.getMessage() + ExceptionUtils.getStackTrace(uriSyntaxException));
				}
			}
		});
	}

	@FXML
	public void uploadingData() {
		uploadDataGridPane.setOnMouseClicked(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(RegistrationConstants.MOSIP_URL));
				} catch (IOException ioException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
				} catch (URISyntaxException uriSyntaxException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							uriSyntaxException.getMessage() + ExceptionUtils.getStackTrace(uriSyntaxException));
				}
			}
		});
	}

	@FXML
	public void updatingBiometrics() {
		updateBiometricsGridPane.setOnMouseClicked(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URI(RegistrationConstants.MOSIP_URL));
				} catch (IOException ioException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
				} catch (URISyntaxException uriSyntaxException) {
					LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
							uriSyntaxException.getMessage() + ExceptionUtils.getStackTrace(uriSyntaxException));
				}
			}
		});
	}

	private void setImagesOnHover() {
		getOnboardedPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(getOnboardedImageView, RegistrationConstants.GET_ONBOARDED_FOCUSED_IMG);
			} else {
				setImage(getOnboardedImageView, RegistrationConstants.GET_ONBOARD_IMG);
			}
		});
		onboardGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(onboardImageView, RegistrationConstants.ONBOARDING_FOCUSED_IMG);
				
			} else {
				setImage(onboardImageView, RegistrationConstants.ONBOARD_YOURSELF_IMG);
			}
		});
		registerGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(registerImageView, RegistrationConstants.REGISTERING_FOCUSED_IMG);
			} else {
				setImage(registerImageView, RegistrationConstants.REGISTER_INDVIDUAL_IMG);
			}
		});
		syncDataGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(syncDataImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {

				setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		mapDevicesGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(mapDevicesImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {
				setImage(mapDevicesImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		uploadDataGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(uploadDataImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
				
			} else {

				setImage(uploadDataImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		updateBiometricsGridPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(updateBiometricsImageView, RegistrationConstants.UPDATE_BIOMETRICS_FOCUSED_IMG);
			} else {

				setImage(updateBiometricsImageView, RegistrationConstants.ACTIVE_BIOMETRIC_DETAILS_IMG);
			}
		});
	}
}