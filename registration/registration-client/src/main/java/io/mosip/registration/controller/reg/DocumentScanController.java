package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.github.sarxos.webcam.Webcam;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.spi.PDFGenerator;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.device.BiometricsController;
import io.mosip.registration.controller.device.ScanPopUpViewController;
import io.mosip.registration.device.scanner.dto.ScanDevice;
import io.mosip.registration.device.webcam.impl.WebcamSarxosServiceImpl;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.doc.category.DocumentCategoryService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.DocumentFxControl;
import io.mosip.registration.util.scan.DocumentScanFacade;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * {@code DocumentScanController} is to handle the screen of the Demographic
 * document section details
 *
 * @author M1045980
 * @since 1.0.0
 */
@Controller
public class DocumentScanController extends BaseController {

	private static final Logger LOGGER = AppConfig.getLogger(DocumentScanController.class);

	@Autowired
	private RegistrationController registrationController;

	private String selectedDocument;

	private ComboBox<DocumentCategoryDto> selectedComboBox;

	private VBox selectedDocVBox;

	private Map<String, ComboBox<DocumentCategoryDto>> documentComboBoxes = new HashMap<>();

	private Map<String, VBox> documentVBoxes = new HashMap<>();

	@Autowired
	private ScanPopUpViewController scanPopUpViewController;

	@Autowired
	private DocumentScanFacade documentScanFacade;

	@Autowired
	private PDFGenerator pdfGenerator;

	@FXML
	protected GridPane documentScan;

	@FXML
	private GridPane documentPane;

	@FXML
	protected ImageView docPreviewImgView;

	@FXML
	protected Label docPreviewNext;

	@FXML
	protected Label docPreviewPrev;

	@FXML
	protected Label docPageNumber;

	@FXML
	protected Label docPreviewLabel;
	@FXML
	public GridPane documentScanPane;

	@FXML
	private VBox docScanVbox;

	private List<BufferedImage> scannedPages;

	@Autowired
	private BiometricsController guardianBiometricsController;

	@Autowired
	private MasterSyncService masterSyncService;

	@Autowired
	private DocumentCategoryService documentCategoryService;

	@Autowired
	private DemographicDetailController demographicDetailController;

	private List<BufferedImage> docPages;

	@FXML
	private Label registrationNavlabel;

	@FXML
	private Button continueBtn;
	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;
	@FXML
	private Label biometricExceptionReq;

	@Autowired
	private Validations validation;

	@Autowired
	private WebcamSarxosServiceImpl webcamSarxosServiceImpl;

	private String selectedScanDeviceName;

	private ImageView imageView;
	private Stage primaryStage;

	private String cropDocumentKey;

	private Webcam webcam;
	private FxControl fxControl;

	public Webcam getWebcam() {
		return webcam;
	}

	public void setWebcam(Webcam webcam) {
		this.webcam = webcam;
	}

	private DocumentFxControl documentFxControl;

	/**
	 * This method will display Scan window to scan and upload documents
	 */
	public void scanWindow() {

		Webcam webcam = webcamSarxosServiceImpl.getWebCam(selectedScanDeviceName);
		if ((selectedScanDeviceName == null || selectedScanDeviceName.isEmpty()) || webcam == null) {
			documentScanFacade.setStubScannerFactory();
		} else {
			documentScanFacade.setScannerFactory();
		}

		if (getRegistrationDTOFromSession().getDocuments() != null) {
			DocumentDto documentDto = getRegistrationDTOFromSession().getDocuments().get(selectedComboBox.getId());

			try {

				if (documentDto != null) {
					setScannedPages(documentScanFacade.pdfToImages(documentDto.getDocument()));
				}
			} catch (IOException exception) {
				LOGGER.error("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			}

		}
		String poeDocValue = getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);
		if (poeDocValue != null && selectedComboBox.getValue().getCode().matches(poeDocValue)) {

			startStream(this);
		} else {
			scanPopUpViewController.setDocumentScan(true);

			scanPopUpViewController.init(this, RegistrationUIConstants.SCAN_DOC_TITLE);

			if (webcam != null) {
				documentScanFacade.setStubScannerFactory();
				startStream(webcam);
			}

		}

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Scan window displayed to scan and upload documents");
	}

	public void startStream(BaseController baseController) {
		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Searching for webcams");

		List<Webcam> webcams = webcamSarxosServiceImpl.getWebCams();

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Found webcams: " + webcams);

		if (webcams != null && !webcams.isEmpty()) {
			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Initializing scan window to capture Exception photo");

			scanPopUpViewController.init(baseController, RegistrationUIConstants.SCAN_DOC_TITLE);
			webcam = webcams.get(0);

			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Checking webcam connectivity");

			if (!webcamSarxosServiceImpl.isWebcamConnected(webcam)) {
				LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "Opening webcam");

				startStream(webcam);
				// Enable Auto-Logout
				SessionContext.setAutoLogout(false);
				LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "Webcam stream started");
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.NO_DEVICE_FOUND);
				scanPopUpViewController.setDefaultImageGridPaneVisibility();

				LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "No webcam found");
			}
		} else {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.NO_DEVICE_FOUND);
			scanPopUpViewController.setDefaultImageGridPaneVisibility();
			return;
		}
	}

	private void startStream(Webcam webcam) {
		webcamSarxosServiceImpl.openWebCam(webcam, webcamSarxosServiceImpl.getWidth(),
				webcamSarxosServiceImpl.getHeight());
		scanPopUpViewController.setWebCamStream(true);
		Thread streamer_thread = new Thread(new Runnable() {

			public void run() {

				while (scanPopUpViewController.isWebCamStream()) {

					try {
						if (!scanPopUpViewController.isStreamPaused()) {
							scanPopUpViewController.getScanImage().setImage(
									SwingFXUtils.toFXImage(webcamSarxosServiceImpl.captureImage(webcam), null));
						}
					} catch (NullPointerException nullPointerException) {
						LOGGER.error(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, APPLICATION_NAME,
								RegistrationConstants.APPLICATION_ID,
								ExceptionUtils.getStackTrace(nullPointerException));

						scanPopUpViewController.setWebCamStream(false);
					}
				}
			}

		});

		streamer_thread.start();
	}

	/**
	 * This method will allow to scan and upload documents
	 */
	@Override
	public void scan(Stage popupStage) {
		try {

			if (RegistrationConstants.YES
					.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.DOC_SCANNER_ENABLED))) {
				scanFromScanner();
			} else {
				scanFromStubbed(popupStage);
			}
		} catch (IOException ioException) {
			LOGGER.error(LoggerConstants.LOG_REG_REGISTRATION_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s -> Exception while scanning documents for registration  %s -> %s",
							RegistrationConstants.USER_REG_DOC_SCAN_UPLOAD_EXP, ioException.getMessage(),
							ExceptionUtils.getStackTrace(ioException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_ERROR);
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LoggerConstants.LOG_REG_REGISTRATION_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s -> Exception while scanning documents for registration  %s",
							RegistrationConstants.USER_REG_DOC_SCAN_UPLOAD_EXP, runtimeException.getMessage())
							+ ExceptionUtils.getStackTrace(runtimeException));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_ERROR);
		}

	}

	/**
	 * This method will get the stubbed data for the scan
	 */
	private void scanFromStubbed(Stage popupStage) throws IOException {

		byte[] byteArray = null;
		documentScanFacade.setStubScannerFactory();
		BufferedImage bufferedImage = null;
		String poeDocValue = getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);
		if (poeDocValue != null && selectedComboBox.getValue().getCode().matches(poeDocValue)) {

//			bufferedImage = webcamSarxosServiceImpl.captureImage(webcam);
			bufferedImage = documentScanFacade.getScannedDocument();

		} else {

			bufferedImage = documentScanFacade.getScannedDocument();

		}

		if (bufferedImage == null) {
			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "captured buffered image was null");

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_ERROR);
			return;
		}
		if (scannedPages == null) {
			scannedPages = new ArrayList<>();
		}
		scannedPages.add(bufferedImage);

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "converting image bytes from buffered image");

		byteArray = documentScanFacade.getImageBytesFromBufferedImage(bufferedImage);

		/* show the scanned page in the preview */
		scanPopUpViewController.getScanImage().setImage(convertBytesToImage(byteArray));

		scanPopUpViewController.getScanningMsg().setVisible(false);

	}

	public byte[] captureAndConvertBufferedImage() throws IOException {

		BufferedImage bufferedImage = webcamSarxosServiceImpl.captureImage(webcam);
		byte[] byteArray = getImageBytesFromBufferedImage(bufferedImage);
		webcamSarxosServiceImpl.close(webcam);
		scanPopUpViewController.setDefaultImageGridPaneVisibility();
		// Enable Auto-Logout
		SessionContext.setAutoLogout(true);
		return byteArray;
	}

	/**
	 * This method is to scan from the scanner
	 */
	private void scanFromScanner() throws IOException {
		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Scanning from scanner");

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Setting scanner factory");

		/* setting the scanner factory */
		if (!documentScanFacade.setScannerFactory()) {
			LOGGER.error(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Setting scanner factory failed");

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_CONNECTION_ERR);
			return;
		}
		if (selectedScanDeviceName == null || selectedScanDeviceName.isEmpty()) {
			LOGGER.error(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Selected device name was empty");

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_CONNECTION_ERR);
			return;
		}

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Setting scan message name visible");

		scanPopUpViewController.getScanningMsg().setVisible(true);

		byte[] byteArray;
		BufferedImage bufferedImage;

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "checking for POE value");

		if (selectedComboBox.getValue().getCode()
				.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE))) {
			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "capturing POE document using native library webcam");

			bufferedImage = webcamSarxosServiceImpl.captureImage(webcam);

			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "closing web cam");

			webcamSarxosServiceImpl.close(webcam);
			scanPopUpViewController.setDefaultImageGridPaneVisibility();
		} else {
			bufferedImage = documentScanFacade.getScannedDocumentFromScanner(selectedScanDeviceName);
		}

		if (bufferedImage == null) {
			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "captured buffered image was null");

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.SCAN_DOCUMENT_ERROR);
			return;
		}
		if (scannedPages == null) {
			scannedPages = new ArrayList<>();
		}
		scannedPages.add(bufferedImage);

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "converting image bytes from buffered image");

		byteArray = documentScanFacade.getImageBytesFromBufferedImage(bufferedImage);
		/* show the scanned page in the preview */
		scanPopUpViewController.getScanImage().setImage(convertBytesToImage(byteArray));

		scanPopUpViewController.getScanningMsg().setVisible(false);
	}

	public byte[] getScannedPagesToBytes(List<BufferedImage> scannedPages) throws IOException {

		byte[] byteArray = null;
		if ("pdf".equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.DOC_TYPE))) {
			byteArray = documentScanFacade.asPDF(scannedPages);
		} else {
			byteArray = documentScanFacade.asImage(scannedPages);
		}
		return byteArray;
	}

	public List<BufferedImage> getScannedPages() {
		return scannedPages;
	}

	public void setScannedPages(List<BufferedImage> scannedPages) {
		this.scannedPages = scannedPages;
	}

	/**
	 * This method converts the BufferedImage to byte[]
	 *
	 * @param bufferedImage - holds the scanned image from the scanner
	 * @return byte[] - scanned document Content
	 * @throws IOException - holds the IOExcepion
	 */
	private byte[] getImageBytesFromBufferedImage(BufferedImage bufferedImage) throws IOException {
		byte[] imageInByte;

		ByteArrayOutputStream imagebyteArray = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, RegistrationConstants.SCANNER_IMG_TYPE, imagebyteArray);
		imagebyteArray.flush();
		imageInByte = imagebyteArray.toByteArray();
		imagebyteArray.close();

		return imageInByte;
	}

	public BufferedImage getScannedImage(int docPageNumber) {

		return scannedPages.get(docPageNumber);
	}

	public void scanDocument(DocumentFxControl documentFxControl, String fieldId, String docCode) {

		this.documentFxControl = documentFxControl;
		Webcam webcam = webcamSarxosServiceImpl.getWebCam(selectedScanDeviceName);
		if ((selectedScanDeviceName == null || selectedScanDeviceName.isEmpty()) || webcam == null) {
			documentScanFacade.setStubScannerFactory();
		} else {
			documentScanFacade.setScannerFactory();
		}

		if (getRegistrationDTOFromSession().getDocuments() != null) {
			DocumentDto documentDto = getRegistrationDTOFromSession().getDocuments().get(fieldId);

			try {

				if (documentDto != null) {
					setScannedPages(documentScanFacade.pdfToImages(documentDto.getDocument()));
				}
			} catch (IOException exception) {
				LOGGER.error("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID,
						exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			}

		}
		String poeDocValue = getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);
		if (poeDocValue != null && docCode.matches(poeDocValue)) {

			startStream(this);
		} else {
			scanPopUpViewController.setDocumentScan(true);

			scanPopUpViewController.init(this, RegistrationUIConstants.SCAN_DOC_TITLE);

			if (webcam != null) {
				documentScanFacade.setStubScannerFactory();
				startStream(webcam);
			}

		}

		LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Scan window displayed to scan and upload documents");

	}

	public DocumentScanFacade getDocumentScanFacade() {
		return documentScanFacade;
	}

	public FxControl getFxControl() {
		return fxControl;
	}

	public void setFxControl(FxControl fxControl) {
		this.fxControl = fxControl;
	}

}
