package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.template.NotificationService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class SendNotificationController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(SendNotificationController.class);

	@FXML
	private TextField email;
	@FXML
	private TextField mobile;
	@FXML
	private ImageView emailIcon;
	@FXML
	private ImageView mobileIcon;
	@FXML
	private Button send;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private Validations validations;

	private Stage popupStage;

	public void init() {
		try {
			popupStage = new Stage();
			popupStage.initStyle(StageStyle.UNDECORATED);
			Parent sendEmailPopup = BaseController
					.load(getClass().getResource(RegistrationConstants.SEND_NOTIFICATION_PAGE));
			popupStage.setResizable(false);
			Scene scene = new Scene(sendEmailPopup);
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			scene.getStylesheets().add(loader.getResource(getCssName()).toExternalForm());
			popupStage.setScene(scene);
			popupStage.initModality(Modality.WINDOW_MODAL);
			popupStage.initOwner(fXComponents.getStage());
			popupStage.show();
			send.disableProperty()
					.bind(Bindings.isEmpty(email.textProperty()).and(Bindings.isEmpty(mobile.textProperty())));
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - UI- SEND_NOTIFICATION", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage());
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_NOTIFICATION_PAGE);
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		String modeOfCommunication = (getValueFromApplicationContext(RegistrationConstants.MODE_OF_COMMUNICATION))
				.toLowerCase();
		if (!modeOfCommunication.contains(RegistrationConstants.EMAIL_SERVICE.toLowerCase())) {
			email.setVisible(false);
			emailIcon.setVisible(false);
		}
		if (!modeOfCommunication.contains(RegistrationConstants.SMS_SERVICE.toLowerCase())) {
			mobile.setVisible(false);
			mobileIcon.setVisible(false);
		}
	}

	/**
	 * To generate email and SMS notification to the user after successful
	 * registration
	 * 
	 * @param event - action to be happened on click of send button
	 */
	@FXML
	public void sendNotification(ActionEvent event) {
		LOGGER.debug("REGISTRATION - UI - SEND_NOTIFICATION", RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Nothing to do");
	}

	@FXML
	public void closeWindow(MouseEvent event) {
		LOGGER.debug("REGISTRATION - UI- SEND_NOTIFICATION", APPLICATION_NAME, APPLICATION_ID,
				"Calling exit window to close the popup");

		popupStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		popupStage.close();

		LOGGER.debug("REGISTRATION - UI- SEND_NOTIFICATION", APPLICATION_NAME, APPLICATION_ID, "Popup is closed");
	}

	private boolean validateMail(String emailId) {
		return validations.validateSingleString(emailId, email.getId(), ApplicationContext.applicationLanguage());
	}

	private boolean validateMobile(String mobileNo) {
		return validations.validateSingleString(mobileNo, mobile.getId(), ApplicationContext.applicationLanguage());
	}
}
