package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.device.webcam.IMosipWebcamService;
import io.mosip.registration.device.webcam.PhotoCaptureFacade;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.CaptureResponseDto;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.service.bio.BioService;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Class for Opening Web Camera
 *
 * @author Himaja Dhanyamraju
 */
@Controller
public class WebCameraController extends BaseController implements Initializable {

	/**
	 * Instance of {@link MosipLogger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(WebCameraController.class);

	@FXML
	public GridPane webCameraPane;

	@FXML
	private SwingNode webcamera;

	@FXML
	protected Button capture;

	@FXML
	private Button clear;

	@FXML
	protected ImageView camImageView;

	@FXML
	private Button close;

	@FXML
	public Label message;

	private BaseController parentController = null;

	private BufferedImage capturedImage = null;

	private IMosipWebcamService photoProvider = null;
	@Autowired
	private PhotoCaptureFacade photoCaptureFacade;

	@Autowired
	private BioService bioService;

	@Autowired
	private Streamer streamer;

	private String imageType;

	private Stage webCameraStage;

	public Stage getWebCameraStage() {
		return webCameraStage;
	}

	public void setWebCameraStage(Stage webCameraStage) {
		this.webCameraStage = webCameraStage;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Page loading has been started");

		if (bioService.isMdmEnabled()) {
			camImageView.setVisible(true);
			webcamera.setVisible(false);
		} else {
			JPanel jPanelWindow = photoProvider.getCameraPanel();
			webcamera.setContent(jPanelWindow);
		}
	}

	public void init(BaseController parentController, String imageType) {
		LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Initializing the controller to be used and imagetype to be captured");

		this.parentController = parentController;
		this.imageType = imageType;
	}

	public boolean isWebcamPluggedIn() {
		LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Connecting to the webcam");

		photoProvider = photoCaptureFacade
				.getPhotoProviderFactory(getValueFromApplicationContext(RegistrationConstants.WEBCAM_LIBRARY_NAME));

		if (!photoProvider.isWebcamConnected()) {
			photoProvider.connect(640, 480);
		}
		return photoProvider.isWebcamConnected();
	}

	@FXML
	public void captureImage(ActionEvent event) throws RegBaseCheckedException, IOException {
		LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"capturing the image from webcam");
		boolean isDuplicateFound = false;
		if (capturedImage != null) {
			capturedImage.flush();
		}
		CaptureResponseDto captureResponseDto = null;
		Instant start = Instant.now();
		if (bioService.isMdmEnabled()) {

			try {
				captureResponseDto = bioService.captureFace(new RequestDetail(RegistrationConstants.FACE_FULLFACE,
						getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 1,
						getValueFromApplicationContext(RegistrationConstants.FACE_THRESHOLD), null));

				// Get ISO value
				byte[] isoImage = bioService.getSingleBiometricIsoTemplate(captureResponseDto);

				if (isoImage != null) {
					AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
					authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId());
					FaceDetailsDTO faceDetail = new FaceDetailsDTO();
					faceDetail.setFaceISO(isoImage);
					authenticationValidatorDTO.setFaceDetail(faceDetail);
					isDuplicateFound = generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.FACE_CAPTURE_SUCCESS, () -> {
								if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))
									return false;
								return bioService.validateFace(authenticationValidatorDTO);
							}, this);

					parentController.saveApplicantPhoto(
							ImageIO.read(new ByteArrayInputStream(bioService.getSingleBioValue(captureResponseDto))),
							imageType, captureResponseDto,
							Duration.between(start, Instant.now()).toString().replace("PT", ""), isDuplicateFound);

					setScanningMsg(RegistrationUIConstants.FACE_CAPTURE_SUCCESS_MSG);
					if (isDuplicateFound)
						setScanningMsg(RegistrationUIConstants.FACE_DUPLICATE_ERROR);
					// parentController.calculateRecaptureTime(imageType);
					capture.setDisable(true);

					clear.setDisable(false);

				} else {
					generateAlert(RegistrationConstants.ALERT, RegistrationUIConstants.FACE_CAPTURE_ERROR);
					streamer.stop();
					webCameraStage.close();
				}

			} catch (RegBaseCheckedException | IOException exception) {
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.getMessageLanguageSpecific(exception.getMessage().substring(0, 3)
								+ RegistrationConstants.UNDER_SCORE + RegistrationConstants.MESSAGE.toUpperCase()));
				streamer.stop();
				webCameraStage.close();
			}

		} else {
			
			LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					"Capturing face as proxy");
			
			
			parentController.saveApplicantPhoto(photoProvider.captureImage(), imageType, captureResponseDto,
					Duration.between(start, Instant.now()).toString().replace("PT", ""), isDuplicateFound);
			
			setScanningMsg(RegistrationUIConstants.FACE_CAPTURE_SUCCESS_MSG);

			capture.setDisable(true);

			clear.setDisable(false);
		}

	}

	@FXML
	public void clearImage(ActionEvent event) {
		LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"clearing the image from webcam");

		parentController.clearPhoto(imageType);
		clear.setDisable(true);
	}

	@FXML
	public void closeWindow(ActionEvent event) {
		LOGGER.info("REGISTRATION - UI - WEB_CAMERA_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"closing the webcam window");
		streamer.stop();
		if (capturedImage != null) {
			capturedImage.flush();
		}
		Stage stage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		stage.close();

	}

	public void closeWebcam() {
		if (photoProvider != null) {
			photoProvider.close();
		}
	}
}
