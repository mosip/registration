package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.JPanel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.device.webcam.impl.WebcamSarxosServiceImpl;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class ScanPopUpViewController extends BaseController {
	private static final Logger LOGGER = AppConfig.getLogger(ScanPopUpViewController.class);

	@Autowired
	private BaseController baseController;

	@Autowired
	private DocumentScanController documentScanController;

	@Autowired
	private WebcamSarxosServiceImpl webcamSarxosServiceImpl;

	@FXML
	private ImageView scanImage;

	@FXML
	private Label popupTitle;

	@FXML
	private Text totalScannedPages;

	@FXML
	private Button saveBtn;

	@FXML
	private Label scannedPagesLabel;

	@FXML
	private Text scanningMsg;

	@FXML
	private GridPane imageParent;

	@FXML
	private GridPane webcamParent;

	@FXML
	private SwingNode webcamNode;

	private boolean isDocumentScan;

	@Autowired
	private Streamer streamer;

	@FXML
	private Hyperlink closeButton;

	public TextField streamerValue;

	@Value("${mosip.doc.stage.width:1200}")
	private int width;

	@Value("${mosip.doc.stage.height:620}")
	private int height;

	/**
	 * @return the scanImage
	 */
	public ImageView getScanImage() {
		return scanImage;
	}

	private Stage popupStage;

	@FXML
	private Button captureBtn;

	@Autowired
	private BiometricsController biometricsController;

	/**
	 * @return the popupStage
	 */
	public Stage getPopupStage() {
		return popupStage;
	}

	/**
	 * @param popupStage the popupStage to set
	 */
	public void setPopupStage(Stage popupStage) {
		this.popupStage = popupStage;
	}

	public GridPane getImageParent() {
		return imageParent;
	}

	public void setImageParent(GridPane imageParent) {
		this.imageParent = imageParent;
	}

	public GridPane getWebcamParent() {
		return webcamParent;
	}

	public void setWebcamParent(GridPane webcamParent) {
		this.webcamParent = webcamParent;
	}

	public SwingNode getWebcamNode() {
		return webcamNode;
	}

	public void setWebcamNode(SwingNode webcamNode) {
		this.webcamNode = webcamNode;
	}

	/**
	 * This method will open popup to scan
	 * 
	 * @param parentControllerObj
	 * @param title
	 */
	public void init(BaseController parentControllerObj, String title) {

		try {

			LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to scan for user registration");

			streamerValue = new TextField();
			baseController = parentControllerObj;
			popupStage = new Stage();
			popupStage.initStyle(StageStyle.UNDECORATED);

			LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "loading scan.fxml");
			Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.SCAN_PAGE));
			setDefaultImageGridPaneVisibility();
			popupStage.setResizable(false);
			popupTitle.setText(title);
			Scene scene = null;

			
			if (!isDocumentScan) {

				scene = new Scene(scanPopup);
				captureBtn.setVisible(false);
				totalScannedPages.setVisible(false);
				saveBtn.setVisible(false);
				scannedPagesLabel.setVisible(false);
			} else {
				LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Setting doc screen width : " + width);

				LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Setting doc screen height : " + height);

				scene = new Scene(scanPopup, width, height);

				isDocumentScan = false;

			}
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			popupStage.setScene(scene);
			popupStage.initModality(Modality.WINDOW_MODAL);
			popupStage.initOwner(fXComponents.getStage());
			popupStage.show();

			LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "scan screen launched");

			scanningMsg.textProperty().addListener((observable, oldValue, newValue) -> {

				Platform.runLater(() -> {
					if (RegistrationUIConstants.NO_DEVICE_FOUND.contains(newValue)) {

						// captureBtn.setDisable(false);

						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.NO_DEVICE_FOUND);
						popupStage.close();

					}
				});

			});

			LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to scan for user registration");

		} catch (IOException ioException) {
			LOGGER.error(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"%s -> Exception while Opening pop-up screen to capture in user registration  %s -> %s",
							RegistrationConstants.USER_REG_SCAN_EXP, ioException.getMessage(),
							ExceptionUtils.getStackTrace(ioException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP);
		}

	}

	/**
	 * This method will allow to scan
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	@FXML
	public void scan() throws MalformedURLException, IOException {
		scanningMsg.setVisible(true);
		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Invoke scan method for the passed controller");
		baseController.scan(popupStage);
	}

	/**
	 * event class to exit from present pop up window.
	 * 
	 * @param event
	 */
	public void exitWindow(ActionEvent event) {

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Calling exit window to close the popup");

		biometricsController.stopRCaptureService();
		streamer.stop();
		if (webcamSarxosServiceImpl.isWebcamConnected()) {
			webcamSarxosServiceImpl.close();
		}
		popupStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		popupStage.close();

		if (documentScanController.getScannedPages() != null) {
			documentScanController.getScannedPages().clear();
		}

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Popup is closed");

	}

	public void enableCloseButton() {
		if (null != closeButton)
			closeButton.setDisable(false);
	}

	public void disableCloseButton() {
		if (null != closeButton)
			closeButton.setDisable(true);
	}

	@FXML
	private void save() {
		webcamSarxosServiceImpl.close();
		setDefaultImageGridPaneVisibility();
		// Enable Auto-Logout
		SessionContext.setAutoLogout(true);
		if (baseController instanceof DocumentScanController) {
			DocumentScanController documentScanController = (DocumentScanController) baseController;
			try {
				documentScanController.attachScannedDocument(popupStage);
			} catch (IOException ioException) {
				LOGGER.error(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						ExceptionUtils.getStackTrace(ioException));
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_ERROR);
			}
		}

	}

	public boolean isDocumentScan() {
		return isDocumentScan;
	}

	public void setDocumentScan(boolean isDocumentScan) {
		this.isDocumentScan = isDocumentScan;
	}

	public Text getTotalScannedPages() {
		return totalScannedPages;
	}

	public void setTotalScannedPages(Text totalScannedPages) {
		this.totalScannedPages = totalScannedPages;
	}

	public Text getScanningMsg() {
		return scanningMsg;
	}

	public void setScanningMsg(String msg) {
		if (scanningMsg != null) {
			scanningMsg.setText(msg);
			scanningMsg.getStyleClass().add("scanButton");
		}
	}

	public void setDefaultImageGridPaneVisibility() {

		LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Setting default visibilities for webCamParent and imageParent");
		webcamParent.setVisible(false);
		imageParent.setVisible(true);
	}

	public void setWebCamPanel(JPanel jPanelWindow) {

		LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Setting jPanel : " + jPanelWindow);
		webcamNode.setContent(jPanelWindow);
		imageParent.setVisible(false);
		webcamParent.setVisible(true);

		LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Setting jPanel completed");
	}

}
