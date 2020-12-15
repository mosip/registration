package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
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
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.util.common.RubberBandSelection;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
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

	@FXML
	protected Label docPreviewNext;

	@FXML
	protected Label docPreviewPrev;

	@FXML
	protected Text docCurrentPageNumber;

	@FXML
	protected GridPane previewOption;

	private Stage popupStage;

	@FXML
	private Button captureBtn;

	@FXML
	private Button cancelBtn;

	@FXML
	private Button cropButton;

	@Autowired
	private BiometricsController biometricsController;

	private Stage cropStage;

	private ImageView cropImageView;

	/**
	 * @return the popupStage
	 */
	public Stage getPopupStage() {
		return popupStage;
	}

	/**
	 * @return the scanImage
	 */
	public ImageView getScanImage() {
		return scanImage;
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

			previewOption.setVisible(false);
			Scene scene = null;

			if (!isDocumentScan) {

				scene = new Scene(scanPopup);
				captureBtn.setVisible(false);
				saveBtn.setVisible(false);
				cancelBtn.setVisible(false);
				cropButton.setVisible(false);
			} else {
				LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Setting doc screen width : " + width);

				LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Setting doc screen height : " + height);

				scene = new Scene(scanPopup, width, height);

				if (documentScanController.getScannedPages() != null
						&& !documentScanController.getScannedPages().isEmpty()) {

					initializeDocPages(1, documentScanController.getScannedPages().size());
					previewOption.setVisible(true);
				}

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

		if (isDocumentScan) {

			String docNumber = docCurrentPageNumber.getText();

			totalScannedPages.setText(String.valueOf(documentScanController.getScannedPages().size()));
			if (docNumber == null || docNumber.isEmpty()) {
				docCurrentPageNumber.setText("1");
				docPreviewPrev.setDisable(true);
				docPreviewNext.setDisable(true);

			} else {
				docCurrentPageNumber.setText(String.valueOf(documentScanController.getScannedPages().size()));
				docPreviewPrev.setDisable(false);
				docPreviewNext.setDisable(true);
			}

			previewOption.setVisible(true);
		}

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
		biometricsController.stopDeviceSearchService();
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

				documentScanController.getScannedPages().clear();
				popupStage.close();
			} catch (IOException | RuntimeException ioException) {
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

	/**
	 * This method will preview the next document
	 */
	public void previewNextPage() {

		Integer docNumber = getDocPreviewNumber();

		if (docNumber != null && docNumber > 0) {

			int previousDocNumber = docNumber;

			BufferedImage bufferedImage = null;
			if (documentScanController.getScannedPages().size() > (previousDocNumber)) {

				bufferedImage = documentScanController.getScannedImage(previousDocNumber);

			}

			if (bufferedImage != null) {

				scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

				docCurrentPageNumber.setText(String.valueOf(previousDocNumber + 1));

				docPreviewPrev.setDisable(false);

				if (documentScanController.getScannedPages().size() == (previousDocNumber + 1)) {

					docPreviewNext.setDisable(true);
				}

			}

		}
	}

	/**
	 * This method will preview the previous document
	 */
	public void previewPrevPage() {

		Integer docNumber = getDocPreviewNumber();

		if (docNumber != null && docNumber > 0) {

			int previousDocNumber = docNumber - 2;

			BufferedImage bufferedImage = null;

			if (documentScanController.getScannedPages().size() > (previousDocNumber)) {

				bufferedImage = documentScanController.getScannedImage(previousDocNumber);

			}

			if (bufferedImage != null) {

				scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

				docCurrentPageNumber.setText(String.valueOf(docNumber - 1));

				docPreviewNext.setDisable(false);

				if ((previousDocNumber) == 0) {

					docPreviewPrev.setDisable(true);
				}
			}

		}
	}

	private Integer getDocPreviewNumber() {

		Integer docNumber = null;
		String docPreviewNumber = docCurrentPageNumber.getText();

		if (docPreviewNumber != null && !docPreviewNumber.isEmpty()) {

			docNumber = Integer.valueOf(docPreviewNumber);

		}

		return docNumber;
	}

	@FXML
	public void crop() {

		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"crop has been selected");

		cropStage = new Stage();

		Group imageLayer = new Group();

		Image image = scanImage.getImage();

		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Set Image for crop");

		if (image != null) {

			GridPane gridpane = new GridPane();

			RowConstraints rc = new RowConstraints();
			rc.setVgrow(Priority.ALWAYS);

			gridpane.getRowConstraints().add(rc);

			HBox hBox = new HBox();

			hBox.setMinHeight(0);
			cropImageView = new ImageView(image);
			cropImageView.fitHeightProperty().bind(hBox.heightProperty());
			cropImageView.setPreserveRatio(true);
			imageLayer.getChildren().add(cropImageView);
			hBox.getChildren().add(imageLayer);
			gridpane.add(hBox, 0, 0);

			RubberBandSelection rubberBandSelection = new RubberBandSelection(imageLayer);

			rubberBandSelection.setscanPopUpViewController(this);
			cropStage.setScene(new Scene(gridpane));
			cropStage.setTitle("Crop Document");
			cropStage.setMaximized(true);

			LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Shown stage for crop");

			cropStage.show();
		}

	}

	public void save(Bounds bounds) throws IOException {

		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Saving cropped image");

		if (bounds.getHeight() == 1.0 || bounds.getWidth() == 1.0)
			return;

		int width = (int) bounds.getWidth();
		int height = (int) bounds.getHeight();

		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT);
		parameters.setViewport(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), width, height));

		WritableImage wi = new WritableImage(width, height);
		cropImageView.snapshot(parameters, wi);

		BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
		BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(),
				BufferedImage.OPAQUE);

		Graphics2D graphics = bufImageRGB.createGraphics();
		graphics.drawImage(bufImageARGB, 0, 0, null);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bufImageRGB, "jpg", baos);

		// attachDocuments(documentCategoryDto, selectedDocVBox, baos.toByteArray(),
		// true);

		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Saving cropped image into session");

		int pageNumber = Integer
				.valueOf(docCurrentPageNumber.getText().isEmpty() ? "1" : docCurrentPageNumber.getText());

		documentScanController.getScannedPages().remove(pageNumber - 1);
		documentScanController.getScannedPages().add(pageNumber - 1,
				ImageIO.read(new ByteArrayInputStream(baos.toByteArray())));

		scanImage.setImage(SwingFXUtils.toFXImage(documentScanController.getScannedPages().get(pageNumber - 1), null));
		graphics.dispose();
		cropStage.close();

		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Saving cropped image completed");

	}

	@FXML
	public void cancel() {

		int currentDocPageNumber = Integer.valueOf(docCurrentPageNumber.getText());
		int pageNumberIndex = currentDocPageNumber - 1;

		// Remove current page
		documentScanController.getScannedPages().remove(pageNumberIndex);

		// If first page
		if (currentDocPageNumber == 1) {

			// Remove current doc
			if (documentScanController.getScannedPages().size() > 0) {

				BufferedImage bufferedImage = documentScanController.getScannedImage(0);

				if (bufferedImage != null) {

					scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

				}

				initializeDocPages(1, documentScanController.getScannedPages().size());

			} else {

				scanImage.setImage(null);

				initializeDocPages(0, 0);

				previewOption.setVisible(false);

			}
		}

		// If last page
		else if (currentDocPageNumber == documentScanController.getScannedPages().size() + 1) {

			BufferedImage bufferedImage = documentScanController.getScannedImage(pageNumberIndex - 1);

			if (bufferedImage != null) {

				scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

				initializeDocPages(currentDocPageNumber - 1, documentScanController.getScannedPages().size());

			}
		}

		// If middle page
		else {
			BufferedImage bufferedImage = documentScanController.getScannedImage(pageNumberIndex);

			if (bufferedImage != null) {

				scanImage.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

				initializeDocPages(currentDocPageNumber, documentScanController.getScannedPages().size());

			}
		}

	}

	private void initializeDocPages(int currentPage, int totalPages) {
		docCurrentPageNumber.setText(String.valueOf(currentPage));

		totalScannedPages.setText(String.valueOf(totalPages));

		boolean prevPageDisable = currentPage > 1 ? false : true;

		docPreviewPrev.setDisable(prevPageDisable);

		boolean nextPageDisable = currentPage < totalPages ? false : true;

		docPreviewNext.setDisable(nextPageDisable);

	}
}
