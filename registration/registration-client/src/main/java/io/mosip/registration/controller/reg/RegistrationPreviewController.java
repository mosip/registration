package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.Writer;
import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.template.TemplateService;
import io.mosip.registration.util.acktemplate.TemplateGenerator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;

@Controller
public class RegistrationPreviewController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(RegistrationPreviewController.class);

	@FXML
	private WebView webView;

	@FXML
	private Button backBtn;

	@FXML
	private ImageView backImageView;

	@Autowired
	private TemplateManagerBuilder templateManagerBuilder;

	@Autowired
	private TemplateGenerator templateGenerator;

	@Autowired
	private TemplateService templateService;

	@Autowired
	private RegistrationController registrationController;

	@FXML
	private Button nextButton;

	public Button getNextButton() {
		return nextButton;
	}

	private String consentText;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		Image backInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK_FOCUSED));
		Image backImage = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK));

		backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				backImageView.setImage(backInWhite);
			} else {
				backImageView.setImage(backImage);
			}
		});

		nextButton.setDisable(true);

		String key = RegistrationConstants.REG_CONSENT + applicationContext.getApplicationLanguage();
		consentText = getValueFromApplicationContext(key);

	}

	@FXML
	public void goToPrevPage(ActionEvent event) {
		auditFactory.audit(AuditEvent.REG_PREVIEW_BACK, Components.REG_PREVIEW, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		// if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
		// SessionContext.map().put(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW,
		// false);
		//
		// updateUINFlowMethod();
		// }
		registrationController.showCurrentPage(RegistrationConstants.REGISTRATION_PREVIEW,
				getPageByAction(RegistrationConstants.REGISTRATION_PREVIEW, RegistrationConstants.PREVIOUS));
//		guardianBiometricsController.populateBiometricPage(false, true);
		/*
		 * } else { registrationController.showCurrentPage(RegistrationConstants.
		 * REGISTRATION_PREVIEW,
		 * getPageByAction(RegistrationConstants.REGISTRATION_PREVIEW,
		 * RegistrationConstants.PREVIOUS)); }
		 */
	}

	private void updateUINFlowMethod() {

		long fingerPrintCount = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
				.getBiometricExceptionDTO().stream()
				.filter(bio -> bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT.toLowerCase()))
				.count();

		long irisCount = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
				.getBiometricExceptionDTO().stream()
				.filter(bio -> bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.IRIS)).count();

		long fingerPrintExceptionCount = biomerticExceptionCount(RegistrationConstants.FINGERPRINT);

		long irisExceptionCount = biomerticExceptionCount(RegistrationConstants.IRIS);

		if (!RegistrationConstants.DISABLE
				.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG))) {
			SessionContext.getInstance().getMapObject().put(RegistrationConstants.UIN_UPDATE_FACECAPTURE, true);
		} else if (irisCount > 0 || irisExceptionCount > 0) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_IRISCAPTURE, true);
		} else if (fingerPrintCount > 0 || fingerPrintExceptionCount > 0) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_FINGERPRINTCAPTURE, true);
		} else if (RegistrationConstants.ENABLE
				.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.DOC_DISABLE_FLAG))) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN, true);
		} else {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DEMOGRAPHICDETAIL, true);
		}
	}

	@FXML
	public void goToNextPage(ActionEvent event) {
		auditFactory.audit(AuditEvent.REG_PREVIEW_SUBMIT, Components.REG_PREVIEW, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		if (getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getConsentOfApplicant() != null) {
			if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
				SessionContext.map().put(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW, false);
				SessionContext.map().put(RegistrationConstants.UIN_UPDATE_OPERATORAUTHENTICATIONPANE, true);
				registrationController.showUINUpdateCurrentPage();
			} else {
				registrationController.showCurrentPage(RegistrationConstants.REGISTRATION_PREVIEW,
						getPageByAction(RegistrationConstants.REGISTRATION_PREVIEW, RegistrationConstants.NEXT));
			}
			registrationController.goToAuthenticationPage();
		} else {
			nextButton.setDisable(false);
		}
	}

	public void setUpPreviewContent() {
		LOGGER.info("REGISTRATION - UI - REGISTRATION_PREVIEW_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Setting up preview content has been started");
		try {
			String ackTemplateText = templateService.getHtmlTemplate(RegistrationConstants.PREVIEW_TEMPLATE_CODE,
					ApplicationContext.applicationLanguage());

			if (ackTemplateText != null && !ackTemplateText.isEmpty()) {
				templateGenerator.setConsentText(consentText);
				ResponseDTO templateResponse = templateGenerator.generateTemplate(ackTemplateText,
						getRegistrationDTOFromSession(), templateManagerBuilder,
						RegistrationConstants.TEMPLATE_PREVIEW);
				if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
					Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
							.get(RegistrationConstants.TEMPLATE_NAME);
					webView.getEngine().loadContent(stringWriter.toString());
					webView.getEngine().documentProperty()
							.addListener((observableValue, oldValue, document) -> listenToButton(document));
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_PREVIEW_PAGE);
					clearRegistrationData();
					goToHomePageFromRegistration();
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_PREVIEW_PAGE);
				clearRegistrationData();
				goToHomePageFromRegistration();
			}
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("REGISTRATION - UI- REGISTRATION_PREVIEW_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	private void listenToButton(Document document) {
		LOGGER.info("REGISTRATION - UI - REGISTRATION_PREVIEW_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Button click action happened on preview content");

		if (document == null) {
			return;
		}

		Element yes = document.getElementById(RegistrationConstants.REG_CONSENT_YES);
		Element no = document.getElementById(RegistrationConstants.REG_CONSENT_NO);
		((EventTarget) yes).addEventListener(RegistrationConstants.CLICK, event -> enableConsent(), false);
		((EventTarget) no).addEventListener(RegistrationConstants.CLICK, event -> disableConsent(), false);
	}

	private void enableConsent() {
		getRegistrationDTOFromSession().getRegistrationMetaDataDTO().setConsentOfApplicant(RegistrationConstants.YES);
		nextButton.setDisable(false);
	}

	private void disableConsent() {
		getRegistrationDTOFromSession().getRegistrationMetaDataDTO().setConsentOfApplicant(RegistrationConstants.NO);
		nextButton.setDisable(false);
	}
}
