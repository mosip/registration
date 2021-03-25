package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Controller;

import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.BaseController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;

/**
 * Class for Registration Page Controller
 * 
 * @author Taleev.Aalam
 * @since 1.0.0
 *
 */

@Controller
public class RegistrationHeaderController extends BaseController implements Initializable {
	
	@FXML
	private ImageView activeDemographicDetailsImgView;
	
	@FXML
	private ImageView deactiveAuthenticationImgView;
	
	@FXML
	private ImageView deactiveBiometriDetailsImgView;
	
	@FXML
	private ImageView deactiveUploadDocumentImgView;
	
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
	
		setImage(activeDemographicDetailsImgView	, RegistrationConstants.ACTIVE_DEMO_DETAILS_IMG);
		setImage(deactiveAuthenticationImgView	, RegistrationConstants.DEACTIVE_AUTH_IMG);
		setImage(deactiveBiometriDetailsImgView	, RegistrationConstants.DEACTIVE_BIOMETRIC_IMG);
		setImage(deactiveUploadDocumentImgView	, RegistrationConstants.DEACTIVEUPLOAD_DOCUMENT_IMG);
		
	}
	
}
