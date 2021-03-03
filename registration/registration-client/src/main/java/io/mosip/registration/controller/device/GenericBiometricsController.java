package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_BIOMETRIC_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.commons.packet.dto.packet.BiometricsException;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIR.BIRBuilder;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.PurposeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.RegistryIDType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.UserOnboardParentController;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.mastersync.BiometricAttributeDto;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.util.control.impl.BiometricFxControl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * {@code GuardianBiometricscontroller} is to capture and display the captured
 * biometrics of Guardian
 *
 * @author Sravya Surampalli
 * @since 1.0
 */
@Controller
public class GenericBiometricsController extends BaseController /* implements Initializable */ {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(GenericBiometricsController.class);

	@FXML
	private GridPane biometricBox;

	@FXML
	private GridPane retryBox;

	@FXML
	private ComboBox<BiometricAttributeDto> biometricTypecombo;

	@FXML
	private Label biometricType;

	@FXML
	private ImageView biometricImage;

	@FXML
	private Label qualityScore;

	@FXML
	private Label attemptSlap;

	@FXML
	private Label thresholdScoreLabel;

	@FXML
	private Label thresholdLabel;

	@FXML
	private GridPane biometricPane;

	@FXML
	private Button scanBtn;

	@FXML
	private ProgressBar bioProgress;

	@FXML
	private Label qualityText;

	@FXML
	private ColumnConstraints thresholdPane1;

	@FXML
	private ColumnConstraints thresholdPane2;

	@FXML
	private HBox bioRetryBox;

	@FXML
	private Button continueBtn;

	// @FXML
	// private Label duplicateCheckLbl;

	@FXML
	private Label guardianBiometricsLabel;

	@FXML
	private ImageView backImageView;

	public Label getGuardianBiometricsLabel() {
		return guardianBiometricsLabel;
	}

	public void setGuardianBiometricsLabel(Label guardianBiometricsLabel) {
		this.guardianBiometricsLabel = guardianBiometricsLabel;
	}

	@FXML
	private GridPane thresholdBox;

	@FXML
	private Label photoAlert;

	@Autowired
	private Streamer streamer;

	@FXML
	private Label registrationNavlabel;

	@FXML
	private Label captureTimeValue;

	/** The scan popup controller. */
	@Autowired
	private ScanPopUpViewController scanPopUpViewController;

	/** The registration controller. */
	@Autowired
	private RegistrationController registrationController;

	/** The face capture controller. */
	// @Autowired
	// private FaceCaptureController faceCaptureController;

	/** The finger print capture service impl. */
	// @Autowired
	// private AuthenticationService authenticationService;

	/** The iris facade. */
	@Autowired
	private BioService bioService;

	private String bioValue;

	private FXUtils fxUtils;

	@Value("${mosip.doc.stage.width:1200}")
	private int width;

	@Value("${mosip.doc.stage.height:620}")
	private int height;

	private static Map<String, Image> STREAM_IMAGES = new HashMap<String, Image>();

	private static Map<String, Double> BIO_SCORES = new HashMap<String, Double>();

	/** The face capture controller. */
	// @Autowired
	// private IrisCaptureController irisCaptureController;

	public ImageView getBiometricImage() {
		return biometricImage;
	}

	public Button getScanBtn() {
		return scanBtn;
	}

	public Button getContinueBtn() {
		return continueBtn;
	}

	public Label getPhotoAlert() {
		return photoAlert;
	}

	public GridPane getBiometricPane() {
		return biometricPane;
	}

	private String currentSubType;

	@FXML
	private GridPane checkBoxPane;

	private ResourceBundle applicationLabelBundle;

	private String currentModality;

	private int currentPosition = -1;

	private int previousPosition = -1;

	private int sizeOfLeftGridPaneImageList = -1;

	// private HashMap<String, VBox> comboBoxMap;
	//
	// private HashMap<String, HashMap<String, VBox>> checkBoxMap;

	private HashMap<String, HashMap<String, VBox>> exceptionMap;

	private HashMap<String, GridPane> leftHandImageBoxMap;

	private HashMap<String, List<String>> currentMap;

	private static final String AND_OPERATOR = " && ";
	private static final String OR_OPERATOR = " || ";

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	private boolean isUserOnboardFlag = false;

	@FXML
	private GridPane backButton;

	@FXML
	private GridPane gheaderfooter;

	@Autowired
	private UserOnboardParentController userOnboardParentController;

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private UserDetailDAO userDetailDAO;

	private String bioType;

	@FXML
	private GridPane leftPanelImageGridPane;

	@FXML
	private Label subTypeLabel;

	@FXML
	private GridPane parentProgressPane;

	@Autowired
	private DocumentScanController documentScanController;

	@Autowired
	private GenericController genericController;

	private Service<List<BiometricsDto>> rCaptureTaskService;

	private Service<MdmBioDevice> deviceSearchTask;

	public void stopRCaptureService() {
		if (rCaptureTaskService != null && rCaptureTaskService.isRunning()) {
			rCaptureTaskService.cancel();
		}
	}

	public void stopDeviceSearchService() {
		if (deviceSearchTask != null && deviceSearchTask.isRunning()) {
			deviceSearchTask.cancel();
		}
	}

	private Node exceptionVBox;

	private String loggerClassName = LOG_REG_BIOMETRIC_CONTROLLER;

	private BiometricFxControl fxControl;

	private List<String> configBioAttributes;

	private List<String> nonConfigBioAttributes;

	private VBox exceptionImgVBox;

	private Stage biometricPopUpStage;

	/*
	 * (non-Javadoc)
	 *
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@FXML
	public void initialize() {
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Loading of Guardian Biometric screen started");

		applicationLabelBundle = applicationContext.getBundle(applicationContext.getApplicationLanguage(),
				RegistrationConstants.LABELS);

	}

	private void displayExceptionBiometric(String modality) {

		retryBox.setVisible(true);
		biometricBox.setVisible(true);
		biometricType.setText(applicationLabelBundle.getString(modality));

		disableLastCheckBoxSection();
		this.currentModality = modality;
//		enableCurrentCheckBoxSection();

		setScanButtonVisibility(false, scanBtn);
		// Get the stream image from Bio ServiceImpl and load it in the image pane

		clearBioLabels();

		clearRetryAttemptsBox();

		thresholdScoreLabel.setText(RegistrationConstants.HYPHEN);

		thresholdPane1.setPercentWidth(Double.parseDouble("0"));
		thresholdPane2.setPercentWidth(100.00 - (Double.parseDouble("0")));

		thresholdLabel.setText(
				RegistrationUIConstants.THRESHOLD.concat("  ").concat("0").concat(RegistrationConstants.PERCENTAGE));

		if (hasApplicantBiometricException()
				&& getRegistrationDTOFromSession().getDocuments().containsKey("proofOfException")) {

			DocumentDto documentDto = getRegistrationDTOFromSession().getDocuments().get("proofOfException");

			Image image = convertBytesToImage(documentDto.getDocument());
			biometricImage.setImage(image);

			addImageInUIPane(currentSubType, currentModality, convertBytesToImage(documentDto.getDocument()), true);

		} else {
			biometricImage.setImage(new Image(this.getClass().getResourceAsStream(getImageIconPath(modality))));

			addImageInUIPane(currentModality, currentModality, null, false);
		}
	}

	private void setScanButtonVisibility(boolean isAllExceptions, Button scanBtn2) {
		scanBtn.setDisable(isAllExceptions);

	}

	/**
	 * Displays biometrics
	 *
	 * @param modality the modality for displaying biometrics
	 */
	private void displayBiometric(String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Displaying biometrics to capture");

		applicationLabelBundle = applicationLabelBundle = applicationContext
				.getBundle(applicationContext.getApplicationLanguage(), RegistrationConstants.LABELS);
		;
		retryBox.setVisible(true);
		biometricBox.setVisible(true);
		biometricType.setText(applicationLabelBundle.getString(modality));

		// get List of captured Biometrics based on nonExceptionBio Attributes
		List<BiometricsDto> capturedBiometrics = null;
		List<String> nonExceptionBioAttributes = isFace(modality) ? RegistrationConstants.faceUiAttributes : null;
		if (!isFace(modality) && !isExceptionPhoto(modality)) {
			setExceptionImg();

			// if (!isFace(modality)) {
			List<Node> checkBoxNodes = exceptionImgVBox.getChildren();

			List<String> exceptionBioAttributes = null;

			if (!checkBoxNodes.isEmpty()) {
				for (Node node : ((Pane) checkBoxNodes.get(1)).getChildren()) {
					if (node instanceof ImageView) {
						ImageView imageView = (ImageView) node;
						String bioAttribute = imageView.getId();
						if (bioAttribute != null && !bioAttribute.isEmpty()) {
							if (imageView.getOpacity() == 1) {
								exceptionBioAttributes = exceptionBioAttributes != null ? exceptionBioAttributes
										: new LinkedList<String>();
								exceptionBioAttributes.add(bioAttribute);
							} else {
								nonExceptionBioAttributes = nonExceptionBioAttributes != null
										? nonExceptionBioAttributes
										: new LinkedList<String>();
								nonExceptionBioAttributes.add(bioAttribute);
							}
						}
					}
				}
			}

		}
		if (nonExceptionBioAttributes != null) {
			capturedBiometrics = getBiometrics(currentSubType, nonExceptionBioAttributes);
		}

		/*
		 * } else { capturedBiometrics = getBiometrics(currentSubType,
		 * Arrays.asList(RegistrationConstants.faceUiAttributes.get(0))); }
		 */

		updateBiometric(modality, getImageIconPath(modality), getBiometricThreshold(modality), getRetryCount(modality));

		if (capturedBiometrics != null && !capturedBiometrics.isEmpty()) {

			loadBiometricsUIElements(capturedBiometrics, currentSubType, modality);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Parent/Guardian Biometrics captured");
	}

	private void setExceptionImg() {
		exceptionImgVBox = new VBox();
		exceptionImgVBox.setSpacing(5);
		Label checkBoxTitle = new Label();
		checkBoxTitle.setText(applicationLabelBundle.getString("exceptionCheckBoxPaneLabel"));
		exceptionImgVBox.getChildren().addAll(checkBoxTitle);
		checkBoxTitle.getStyleClass().add("demoGraphicFieldLabel");

		exceptionImgVBox.getChildren().add(
				getExceptionImagePane(currentModality, configBioAttributes, nonConfigBioAttributes, currentSubType));

		exceptionImgVBox.setVisible(true);
		exceptionImgVBox.setManaged(true);

		checkBoxPane.add(exceptionImgVBox, 0, 0);
	}

	private String getRetryCount(String modality) {
		// modality = modality.toLowerCase();

		String irisRetryCount = null;

		if (modality != null) {
			switch (modality) {

			case RegistrationConstants.FACE:
				irisRetryCount = RegistrationConstants.FACE_RETRY_COUNT;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				irisRetryCount = RegistrationConstants.IRIS_RETRY_COUNT;
				break;

			case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				irisRetryCount = RegistrationConstants.FINGERPRINT_RETRIES_COUNT;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				irisRetryCount = RegistrationConstants.FINGERPRINT_RETRIES_COUNT;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
				irisRetryCount = RegistrationConstants.FINGERPRINT_RETRIES_COUNT;
				break;

			}
		}

		return irisRetryCount;

	}

	private String getBiometricThreshold(String modality) {
		// modality = modality.toLowerCase();

		String biomnetricThreshold = null;
		if (modality != null) {
			switch (modality) {

			case RegistrationConstants.FACE:
				biomnetricThreshold = RegistrationConstants.FACE_THRESHOLD;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				biomnetricThreshold = RegistrationConstants.IRIS_THRESHOLD;
				break;

			case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				biomnetricThreshold = RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				biomnetricThreshold = RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
				biomnetricThreshold = RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD;
				break;

			}
		}

		return biomnetricThreshold;

	}

	public String getImageIconPath(String modality) {
		// modality = modality.toLowerCase();

		String imageIconPath = null;

		if (isExceptionPhoto(modality)) {
			return RegistrationConstants.DEFAULT_EXCEPTION_IMAGE_PATH;
		}

		if (modality != null) {
			switch (modality) {

			case RegistrationConstants.FACE:
				imageIconPath = RegistrationConstants.FACE_IMG_PATH;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				imageIconPath = RegistrationConstants.DOUBLE_IRIS_IMG_PATH;
				break;

			case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				imageIconPath = RegistrationConstants.RIGHTPALM_IMG_PATH;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				imageIconPath = RegistrationConstants.LEFTPALM_IMG_PATH;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
				imageIconPath = RegistrationConstants.THUMB_IMG_PATH;
				break;

			}
		}

		return imageIconPath;
	}

	private void disableLastCheckBoxSection() {
		checkBoxPane.setVisible(false);
		if (currentPosition != -1) {
			if (this.currentModality != null
					&& exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)) != null && exceptionMap
							.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality) != null) {
				exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
						.setVisible(false);
				exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
						.setManaged(false);
			}
		}

	}

	/**
	 * This method will allow to scan and upload documents
	 */
	@Override
	public void scan(Stage popupStage) {
		if (isExceptionPhoto(currentModality)) {
			try {
				byte[] byteArray = documentScanController.captureAndConvertBufferedImage();

				saveProofOfExceptionDocument(byteArray);
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS);

				scanPopUpViewController.getPopupStage().close();

			} catch (RuntimeException | IOException exception) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR);

				LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Error while capturing exception photo : " + ExceptionUtils.getStackTrace(exception));

			}

		}
	}

	private void saveProofOfExceptionDocument(byte[] byteArray) {
		DocumentDto documentDto = new DocumentDto();
		documentDto.setDocument(byteArray);
		documentDto.setType("EOP");

		String docType = getValueFromApplicationContext(RegistrationConstants.DOC_TYPE);

		docType = RegistrationConstants.SCANNER_IMG_TYPE;

		documentDto.setFormat(docType);
		documentDto.setCategory("POE");
		documentDto.setOwner("Applicant");
		documentDto.setValue("POE".concat(RegistrationConstants.UNDER_SCORE).concat("EOP"));

		getRegistrationDTOFromSession().addDocument("proofOfException", documentDto);

		fxControl.refreshExceptionMarks();
		displayExceptionBiometric(currentModality);

//		refreshContinueButton();
	}

	/**
	 * Scan the biometrics
	 *
	 * @param event the event for scanning biometrics
	 */
	@FXML
	private void scan(ActionEvent event) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Displaying Scan popup for capturing biometrics");

		auditFactory.audit(getAuditEventForScan(currentModality), Components.REG_BIOMETRICS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		scanPopUpViewController.setDocumentScan(false);
		scanPopUpViewController.init(this, "Biometrics");

		deviceSearchTask = new Service<MdmBioDevice>() {
			@Override
			protected Task<MdmBioDevice> createTask() {
				return new Task<MdmBioDevice>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected MdmBioDevice call() throws RegBaseCheckedException {

						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"device search request started" + System.currentTimeMillis());

						if (deviceSpecificationFactory.isDeviceAvailable(currentModality)) {

							return deviceSpecificationFactory.getDeviceInfoByModality(
									isFace(currentModality) || isExceptionPhoto(currentModality)
											? RegistrationConstants.FACE_FULLFACE
											: currentModality);
						} else {
							throw new RegBaseCheckedException(
									RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
									RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
						}

					}
				};
			}
		};
		if (!bioService.isMdmEnabled()) {
			rCaptureTaskService();
		} else {
			deviceSearchTask.start();
		}
		// mdmBioDevice = null;

		deviceSearchTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				MdmBioDevice mdmBioDevice = deviceSearchTask.getValue();

				try {

					if (isExceptionPhoto(currentModality) && (mdmBioDevice == null || mdmBioDevice.getSpecVersion()
							.equalsIgnoreCase(RegistrationConstants.SPEC_VERSION_092))) {

						streamLocalCamera();
						return;

					}
					if (bioService.isMdmEnabled()) {

						// Disable Auto-Logout
						SessionContext.setAutoLogout(false);

						if (mdmBioDevice == null) {
							setPopViewControllerMessage(true, RegistrationUIConstants.NO_DEVICE_FOUND);

							return;
						}

						// Start Stream
						setPopViewControllerMessage(true, RegistrationUIConstants.STREAMING_PREP_MESSAGE);

						InputStream urlStream = bioService.getStream(mdmBioDevice,
								isFace(currentModality) ? RegistrationConstants.FACE_FULLFACE : currentModality);

						boolean isStreamStarted = urlStream != null && urlStream.read() != -1;
						if (!isStreamStarted) {

							LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"URL Stream was null for : " + System.currentTimeMillis());

							deviceSpecificationFactory.init();

							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.STREAMING_ERROR);
							scanPopUpViewController.getPopupStage().close();

							return;
						}

						setPopViewControllerMessage(true, RegistrationUIConstants.STREAMING_INIT_MESSAGE);

						rCaptureTaskService();

						streamer.startStream(urlStream, scanPopUpViewController.getScanImage(), biometricImage);

					} else {
						rCaptureTaskService();
					}
				} catch (RegBaseCheckedException | IOException exception) {

					LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Error while streaming : " + ExceptionUtils.getStackTrace(exception));

					LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Checking if exception photo");

					if (isExceptionPhoto(currentModality)) {

						streamLocalCamera();

						return;

					}
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.STREAMING_ERROR);
					scanPopUpViewController.getPopupStage().close();

					// Enable Auto-Logout
					SessionContext.setAutoLogout(true);
				}

			}

		});

		// mdmBioDevice = null;
		deviceSearchTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Exception while finding bio device");

				if (isExceptionPhoto(currentModality)) {

					streamLocalCamera();
					return;

				}
				setPopViewControllerMessage(true, RegistrationUIConstants.NO_DEVICE_FOUND);

			}
		});

	}

	private boolean isFace(String currentModality) {
		return currentModality.toUpperCase().contains(RegistrationConstants.FACE.toUpperCase());
	}

	private List<String> getSelectedExceptionsByBioType(String subType, String modality)
			throws RegBaseCheckedException {
		List<String> selectedExceptions = new LinkedList<String>();

		// Get List of check boxes using the grid pane
		List<Node> paneList = getCheckBoxes(subType, modality);
		if (paneList != null && !paneList.isEmpty()) {

			Pane pane = (Pane) paneList.get(1);

			for (Node exceptionImage : pane.getChildren()) {
				if (exceptionImage instanceof ImageView && exceptionImage.getId() != null
						&& !exceptionImage.getId().isEmpty()) {
					ImageView image = (ImageView) exceptionImage;
					if (image.getOpacity() == 1) {

						selectedExceptions.add(image.getId());

					}
				}
			}
		}
		return selectedExceptions;
	}

	private List<Node> getCheckBoxes(String subType, String modality) {

		List<Node> exceptionCheckBoxes = new ArrayList<>();
		if (exceptionMap.get(subType) != null && exceptionMap.get(subType).get(modality) != null) {
			exceptionCheckBoxes = exceptionMap.get(subType).get(modality).getChildren();
		}
		return exceptionCheckBoxes;

	}

	public void rCaptureTaskService() {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Capture request called" + System.currentTimeMillis());

		rCaptureTaskService = new Service<List<BiometricsDto>>() {
			@Override
			protected Task<List<BiometricsDto>> createTask() {
				return new Task<List<BiometricsDto>>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected List<BiometricsDto> call() throws RegBaseCheckedException, IOException {

						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Capture request started" + System.currentTimeMillis());

//						currentSubType = getListOfBiometricSubTypes().get(currentPosition);
						return rCapture(currentSubType, currentModality);

					}
				};
			}
		};
		rCaptureTaskService.start();

		rCaptureTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "RCapture task failed");
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR);

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "closing popup stage");
				scanPopUpViewController.getPopupStage().close();

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Enabling LogOut");
				// Enable Auto-Logout
				SessionContext.setAutoLogout(true);

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Setting URL Stream as null");
				streamer.setUrlStream(null);
			}
		});
		rCaptureTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"RCapture task was successful");
				try {
					List<BiometricsDto> mdsCapturedBiometricsList = rCaptureTaskService.getValue();

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"biometrics captured from mock/real MDM");

					boolean isValidBiometric = isValidBiometric(mdsCapturedBiometricsList);

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"biometrics captured from mock/real MDM was valid : " + isValidBiometric);

					if (isValidBiometric) {

						// validate local de-dup check
						boolean isMatchedWithLocalBiometrics = false;

						if (!isExceptionPhoto(currentModality)) {
							if (bioService.isMdmEnabled() && !isUserOnboardFlag) {

								// TODO Remove dedup enable/disable validation, currently added for testing
								// purpose
								if (RegistrationConstants.ENABLE
										.equalsIgnoreCase(RegistrationConstants.DEDUPLICATION_ENABLE_FLAG)) {
									LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
											"Doing local de-dup validation");

									isMatchedWithLocalBiometrics = identifyInLocalGallery(mdsCapturedBiometricsList,
											Biometric.getSingleTypeByModality(
													isFace(currentModality) ? "FACE_FULL FACE" : currentModality)
													.value());
								}
							}
						}

						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Doing local de-dup validation : " + isMatchedWithLocalBiometrics);

						if (!isMatchedWithLocalBiometrics) {

							List<BiometricsDto> registrationDTOBiometricsList = new LinkedList<>();

							double qualityScore = 0;
							List<String> exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType,
									currentModality);
							// save to registration DTO
							for (BiometricsDto biometricDTO : mdsCapturedBiometricsList) {
								LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
										"BiometricDTO captured from mock/real MDM >>> "
												+ biometricDTO.getBioAttribute());

								if (!exceptionBioAttributes.isEmpty()
										&& exceptionBioAttributes.contains(biometricDTO.getBioAttribute())) {
									LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
											"As bio atrribute marked as exception not storing into registration DTO : "
													+ biometricDTO.getBioAttribute());
									continue;
								} else {
									qualityScore += biometricDTO.getQualityScore();
									biometricDTO.setSubType(currentSubType);
									registrationDTOBiometricsList.add(biometricDTO);
								}
							}

							if (isExceptionPhoto(currentModality)) {

								LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
										"started Saving Exception photo captured using MDS");
								saveProofOfExceptionDocument(
										extractFaceImageData(registrationDTOBiometricsList.get(0).getAttributeISO()));
								generateAlert(RegistrationConstants.ALERT_INFORMATION,
										RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS);

								genericController.refreshFields();
								scanPopUpViewController.getPopupStage().close();
								return;
							}
							LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"started Saving filtered biometrics into registration DTO");
							registrationDTOBiometricsList = saveCapturedBiometricData(currentSubType,
									registrationDTOBiometricsList);

							LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"Completed Saving filtered biometrics into registration DTO");

							if (!registrationDTOBiometricsList.isEmpty()) {
								// if all the above check success show alert capture success
								generateAlert(RegistrationConstants.ALERT_INFORMATION,
										RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS);

								/*
								 * Image streamImage = null; if (bioService.isMdmEnabled()) { streamImage =
								 * streamer.getStreamImage(); } else { streamImage = new Image(
								 * this.getClass().getResourceAsStream(getStubStreamImagePath(modality))); }
								 */

								LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
										"Adding streaming image into local map");

								try {

									byte[] byteimage = (bioService.isMdmEnabled()) ? streamer.getStreamImageBytes()
											: null;
									if (isFace(currentModality) && bioService.isMdmEnabled()) {
										byteimage = extractFaceImageData(
												registrationDTOBiometricsList.get(0).getAttributeISO());
									}
									addBioStreamImage(currentSubType, currentModality,
											registrationDTOBiometricsList.get(0).getNumOfRetries(), byteimage);

									if (currentModality.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)
											&& bioService.isMdmEnabled()) {

										for (BiometricsDto biometricsDto : registrationDTOBiometricsList) {
											byteimage = extractIrisImageData(biometricsDto.getAttributeISO());

											if (byteimage != null) {
												addRegistrationStreamImage(currentSubType,
														biometricsDto.getBioAttribute(),
														registrationDTOBiometricsList.get(0).getNumOfRetries(),
														byteimage);
											}
										}
									}

									fxControl.refreshExceptionMarks();
								} catch (IOException exception) {
									LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
											ExceptionUtils.getStackTrace(exception));
								}

								LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
										"Adding bio scores into local map");

								addBioScores(currentSubType, currentModality,
										String.valueOf(registrationDTOBiometricsList.get(0).getNumOfRetries()),
										qualityScore / registrationDTOBiometricsList.size());

								LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
										"using captured response fill the fields like quality score and progress bar,,etc,.. UI");
								loadBiometricsUIElements(registrationDTOBiometricsList, currentSubType,
										currentModality);

								refreshContinueButton();
							} else {
								// request response mismatch
								generateAlert(RegistrationConstants.ALERT_INFORMATION,
										RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
							}

						} else {

							LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"Local De-Dup check failed");
							// if any above checks failed show alert capture failure
							generateAlert(RegistrationConstants.ALERT_INFORMATION,
									RegistrationUIConstants.LOCAL_DEDUP_CHECK_FAILED);
						}
					} else {

						// if any above checks failed show alert capture failure
						generateAlert(RegistrationConstants.ALERT_INFORMATION,
								RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
					}

				} catch (RuntimeException | RegBaseCheckedException runtimeException) {
					LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, String.format(
							"Exception while getting the scanned biometrics for user registration: %s caused by %s",
							runtimeException.getMessage(),
							runtimeException.getCause() + ExceptionUtils.getStackTrace(runtimeException)));

					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR);
				}

				scanPopUpViewController.getPopupStage().close();
				// Enable Auto-Logout
				SessionContext.setAutoLogout(true);

				streamer.setUrlStream(null);
			}

		});
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scan process ended for capturing biometrics");

	}

	private boolean isValidBiometric(List<BiometricsDto> mdsCapturedBiometricsList) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Validating captured biometrics");

		boolean isValid = mdsCapturedBiometricsList != null && !mdsCapturedBiometricsList.isEmpty();

		if (isValid) {
			for (BiometricsDto biometricsDto : mdsCapturedBiometricsList) {
				if (biometricsDto.getBioAttribute() == null
						|| biometricsDto.getBioAttribute().equalsIgnoreCase(RegistrationConstants.JOB_UNKNOWN)) {

					LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Unknown bio attribute identified in captured biometrics");

					isValid = false;
					break;
				}
			}
		}

		return isValid;
	}

	private List<BiometricsDto> rCapture(String subType, String modality) throws RegBaseCheckedException, IOException {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Finding exception bio attributes" + System.currentTimeMillis());

		List<String> exceptionBioAttributes = new LinkedList<>();
		if (isExceptionPhoto(modality)) {

			if (getRegistrationDTOFromSession() != null) {
				for (Entry<String, BiometricsException> bs : getRegistrationDTOFromSession().getBiometricExceptions()
						.entrySet()) {

					if (isApplicant(bs.getValue().getIndividualType())) {

						exceptionBioAttributes.add(bs.getValue().getMissingBiometric());
					}

				}
			}

		} else {
			exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType, currentModality);
		}
		// Check count
		int count = 1;

		MDMRequestDto mdmRequestDto = new MDMRequestDto(
				isFace(modality) || isExceptionPhoto(modality) ? RegistrationConstants.FACE_FULLFACE : modality,
				exceptionBioAttributes.toArray(new String[0]), "Registration",
				io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(
						RegistrationConstants.SERVER_ACTIVE_PROFILE),
				Integer.valueOf(getCaptureTimeOut()), count,
				getThresholdScoreInInt(getThresholdKeyByBioType(modality)));

		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"exceptionBioAttributes passed to mock/real MDM >>> " + exceptionBioAttributes);

		return bioService.captureModality(mdmRequestDto);

	}

	public boolean isApplicant(String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Checking subType is whether applicant or not");
		return subType != null && subType.equalsIgnoreCase(RegistrationConstants.APPLICANT);
	}

	public boolean isExceptionPhoto(String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Checking subType is whether exception photo or not");
		return modality != null && modality.equalsIgnoreCase(RegistrationConstants.EXCEPTION_PHOTO);
	}

	private AuditEvent getAuditEventForScan(String modality) {
		AuditEvent auditEvent = AuditEvent.REG_DOC_NEXT;
		if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
			auditEvent = AuditEvent.REG_BIO_RIGHT_SLAP_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
			auditEvent = AuditEvent.REG_BIO_LEFT_SLAP_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
			auditEvent = AuditEvent.REG_BIO_THUMBS_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)) {
			auditEvent = AuditEvent.REG_BIO_IRIS_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FACE)) {
			auditEvent = AuditEvent.REG_BIO_FACE_CAPTURE;
		}
		return auditEvent;
	}

	private List<BiometricsDto> saveCapturedBiometricData(String subType, List<BiometricsDto> biometrics) {
		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"saveCapturedBiometricData invoked size >> " + biometrics.size());
		if (isUserOnboardFlag) {
			List<BiometricsDto> savedCaptures = new ArrayList<>();
			for (BiometricsDto value : biometrics) {
				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"updating userOnboard biometric data >> " + value.getModalityName());
				savedCaptures.add(userOnboardService.addOperatorBiometrics(subType, value.getBioAttribute(), value));

			}
			return savedCaptures;
		} else {

			Map<String, BiometricsDto> biometricsMap = new LinkedHashMap<>();

			for (BiometricsDto biometricsDto : biometrics) {

				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Adding registration biometric data >> " + biometricsDto.getBioAttribute());
				biometricsMap.put(biometricsDto.getBioAttribute(), biometricsDto);
			}

			fxControl.setData(biometricsMap);
			return (List<BiometricsDto>) fxControl.getData();

		}
	}

	private void loadBiometricsUIElements(List<BiometricsDto> biometricDTOList, String subType, String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating progress Bar,Text and attempts Box in UI");

		int retry = biometricDTOList.get(0).getNumOfRetries();

		setCapturedValues(getAverageQualityScore(biometricDTOList), retry,
				getThresholdScoreInInt(getThresholdKeyByBioType(modality)));

		// Get the stream image from Bio ServiceImpl and load it in the image pane
		biometricImage.setImage(getBioStreamImage(subType, modality, retry));

		addImageInUIPane(subType, modality, getBioStreamImage(subType, modality, retry), true);
	}

	private double getAverageQualityScore(List<BiometricsDto> biometricDTOList) {

		double qualityScore = 0;
		if (biometricDTOList != null && !biometricDTOList.isEmpty()) {

			for (BiometricsDto biometricDTO : biometricDTOList) {
				qualityScore += biometricDTO.getQualityScore();
			}

			qualityScore = qualityScore / biometricDTOList.size();
		}
		return qualityScore;
	}

	private int getThresholdScoreInInt(String thresholdKey) {
		/* Get Configued threshold score for bio type */

		String thresholdScore = getValueFromApplicationContext(thresholdKey);

		return thresholdScore != null ? Integer.valueOf(thresholdScore) : 0;
	}

	public double getThresholdScoreInDouble(String thresholdKey) {
		/* Get Configued threshold score for bio type */

		String thresholdScore = getValueFromApplicationContext(thresholdKey);

		return thresholdScore != null ? Double.valueOf(thresholdScore) : 0;
	}

	private String getCaptureTimeOut() {

		/* Get Configued capture timeOut */
		return getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT);
	}

	/**
	 * Updating biometrics
	 *
	 * @param bioType            biometric type
	 * @param bioImage           biometric image
	 * @param biometricThreshold threshold of biometric
	 * @param retryCount         retry count
	 */
	private void updateBiometric(String bioType, String bioImage, String biometricThreshold, String retryCount) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating biometrics and clearing previous data");
		this.bioType = constructBioType(bioType);

		bioValue = bioType;
		biometricImage.setImage(new Image(this.getClass().getResourceAsStream(bioImage)));

		String threshold = null;
		if (biometricThreshold != null) {
			threshold = getValueFromApplicationContext(biometricThreshold);
		}

		double qualityScore = threshold == null || threshold.isEmpty() ? 0 : Double.parseDouble(threshold);

		thresholdScoreLabel.setText(getQualityScore(qualityScore));
		createQualityBox(retryCount, biometricThreshold);

		clearBioLabels();
		if (!isFace(currentModality) && !isExceptionPhoto(currentModality)) {
			setScanButtonVisibility(fxControl.isAllExceptions(currentModality), scanBtn);
		} else {
			setScanButtonVisibility(false, scanBtn);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated biometrics and cleared previous data");
	}

	private void clearBioLabels() {
		clearCaptureData();
		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.BIOMETRIC_PANES_SELECTED);
		qualityScore.setText(RegistrationConstants.HYPHEN);
		attemptSlap.setText(RegistrationConstants.HYPHEN);
		// duplicateCheckLbl.setText(RegistrationConstants.EMPTY);

		retryBox.setVisible(true);
		biometricBox.setVisible(true);

		bioProgress.setProgress(0);
		qualityText.setText("");

	}

	private String constructBioType(String bioType) {
		if (bioType.equalsIgnoreCase(RegistrationUIConstants.RIGHT_SLAP)) {
			bioType = RegistrationConstants.FINGERPRINT_SLAB_RIGHT;
		} else if (bioType.equalsIgnoreCase(RegistrationUIConstants.LEFT_SLAP)) {
			bioType = RegistrationConstants.FINGERPRINT_SLAB_LEFT;
		} else if (bioType.equalsIgnoreCase(RegistrationUIConstants.THUMBS)) {
			bioType = RegistrationConstants.FINGERPRINT_SLAB_THUMBS;
		}

		return bioType;
	}

	/**
	 * Updating captured values
	 *
	 * @param qltyScore      Qulaity score
	 * @param retry          retrycount
	 * @param thresholdValue threshold value
	 */
	private void setCapturedValues(double qltyScore, int retry, double thresholdValue) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating captured values of biometrics");

		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.FINGERPRINT_PANES_SELECTED);
		qualityScore.setText(getQualityScore(qltyScore));
		attemptSlap.setText(String.valueOf(retry));

		bioProgress.setProgress(
				Double.valueOf(getQualityScore(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) / 100);
		qualityText.setText(getQualityScore(qltyScore));

		retry = retry == 0 ? 1 : retry;
		if (Double.valueOf(getQualityScore(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) >= thresholdValue) {
			clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_GREEN, retry);
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_GREEN);
		} else {
			clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_RED, retry);
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_RED);
		}

		if (retry == getMaxRetryByModality(currentModality)) {
			scanBtn.setDisable(true);
		} else {
			scanBtn.setDisable(false);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated captured values of biometrics");
	}

	public int getMaxRetryByModality(String currentModality) {
		String key = getMaxRetryKeyByModality(currentModality);

		String val = getValueFromApplicationContext(key);
		return val != null ? Integer.parseInt(val) : 0;
	}

	private String getMaxRetryKeyByModality(String modality) {
		return modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)
				? RegistrationConstants.FINGERPRINT_RETRIES_COUNT
				: modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_LEFT)
						? RegistrationConstants.FINGERPRINT_RETRIES_COUNT
						: modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)
								? RegistrationConstants.FINGERPRINT_RETRIES_COUNT
								: modality.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)
										? RegistrationConstants.IRIS_RETRY_COUNT
										: modality.equalsIgnoreCase(RegistrationConstants.FACE)
												? RegistrationConstants.FACE_RETRY_COUNT
												: modality;
	}

	/**
	 * Updating captured values
	 *
	 * @param retryCount         retry count
	 * @param biometricThreshold threshold value
	 */
	private void createQualityBox(String retryCount, String biometricThreshold) {
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating Quality and threshold values of biometrics");

		final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
			public void handle(final MouseEvent mouseEvent) {

				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Mouse Event by attempt Started");

				String eventString = mouseEvent.toString();
				int index = eventString.indexOf(RegistrationConstants.RETRY_ATTEMPT_ID);

				if (index == -1) {
					String text = "Text[text=";
					index = eventString.indexOf(text) + text.length() + 1;

				} else {
					index = index + RegistrationConstants.RETRY_ATTEMPT_ID.length();
				}
				try {

					// updateByAttempt(modality,
					// Character.getNumericValue(eventString.charAt(index)), biometricImage,
					// qualityText, bioProgress, qualityScore);

					int attempt = Character.getNumericValue(eventString.charAt(index));

					double qualityScoreVal = getBioScores(currentSubType, currentModality, attempt);
					if (qualityScoreVal != 0) {
						updateByAttempt(qualityScoreVal, getBioStreamImage(currentSubType, currentModality, attempt),
								getThresholdScoreInInt(getThresholdKeyByBioType(currentModality)), biometricImage,
								qualityText, bioProgress, qualityScore);
					}

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Mouse Event by attempt Ended. modality : " + currentModality);

				} catch (RuntimeException runtimeException) {
					LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

				}
			}

		};

		bioRetryBox.getChildren().clear();
		// if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))
		// {

		String retryCountVal = getValueFromApplicationContext(retryCount);

		retryCountVal = retryCountVal == null || retryCountVal.isEmpty() ? "0" : retryCountVal;
		for (int retry = 0; retry < Integer.parseInt(retryCountVal); retry++) {
			Label label = new Label();
			label.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
			label.setId(RegistrationConstants.RETRY_ATTEMPT_ID + (retry + 1));
			label.setVisible(true);
			label.setText(String.valueOf(retry + 1));
			label.setAlignment(Pos.CENTER);

			bioRetryBox.getChildren().add(label);
		}
		bioRetryBox.setOnMouseClicked(mouseEventHandler);

		String threshold = biometricThreshold != null ? getValueFromApplicationContext(biometricThreshold) : "0";

		thresholdLabel.setAlignment(Pos.CENTER);

		double thresholdValDouble = threshold != null && !threshold.isEmpty() ? Double.parseDouble(threshold) : 0;
		thresholdLabel.setText(RegistrationUIConstants.THRESHOLD.concat("  ").concat(String.valueOf(thresholdValDouble))
				.concat(RegistrationConstants.PERCENTAGE));
		thresholdPane1.setPercentWidth(thresholdValDouble);
		thresholdPane2.setPercentWidth(100.00 - (thresholdValDouble));
		// }

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated Quality and threshold values of biometrics");

	}

	/**
	 * Clear attempts box.
	 *
	 * @param styleClass the style class
	 * @param retries    the retries
	 */
	private void clearAttemptsBox(String styleClass, int retries) {
		for (int retryBox = 1; retryBox <= retries; retryBox++) {

			Node node = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retryBox);

			if (node != null) {
				node.getStyleClass().clear();
				node.getStyleClass().add(styleClass);
			}
		}

		boolean nextRetryFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + ++retries) != null;
		while (nextRetryFound) {

			Node node = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries);

			if (node != null) {
				node.getStyleClass().clear();
				node.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
			}
			nextRetryFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + ++retries) != null;
		}
	}

	public void clearRetryAttemptsBox() {

		int attempt = 1;
		boolean retryBoxFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt) != null;

		while (retryBoxFound) {
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt).getStyleClass().clear();
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt).getStyleClass()
					.add(RegistrationConstants.QUALITY_LABEL_GREY);
			++attempt;
			retryBoxFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt) != null;

		}
	}

	/**
	 * Clear captured data
	 *
	 */
	private void clearCaptureData() {

		clearUiElements();

	}

	private void clearUiElements() {

		retryBox.setVisible(false);
		biometricBox.setVisible(false);

	}

	/**
	 * Clear Biometric data
	 */
	public void clearCapturedBioData() {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Clearing the captured biometric data");

		// clearAllBiometrics();

		if (getRegistrationDTOFromSession() != null && (getRegistrationDTOFromSession().isUpdateUINChild()
				|| (SessionContext.map().get(RegistrationConstants.IS_Child) != null
						&& (Boolean) SessionContext.map().get(RegistrationConstants.IS_Child)))) {
			getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.setFingerprintDetailsDTO(new ArrayList<>());

			getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.setIrisDetailsDTO(new ArrayList<>());
		}
		// duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
		clearCaptureData();
		biometricBox.setVisible(false);
		retryBox.setVisible(false);
		bioValue = RegistrationUIConstants.SELECT;

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Cleared the captured biometric data");

	}

	public void addBioStreamImage(String subType, String modality, int attempt, byte[] streamImage) throws IOException {
		if (streamImage == null && !bioService.isMdmEnabled()) {
			String imagePath = getStubStreamImagePath(modality);
			STREAM_IMAGES.put(String.format("%s_%s_%s", subType, modality, attempt),
					new Image(this.getClass().getResourceAsStream(imagePath)));

			addRegistrationStreamImage(subType, isFace(modality) ? RegistrationConstants.FACE_FULLFACE : modality,
					attempt, IOUtils.toByteArray(this.getClass().getResourceAsStream(imagePath)));

		} else {
			STREAM_IMAGES.put(String.format("%s_%s_%s", subType, modality, attempt),
					new Image(new ByteArrayInputStream(streamImage)));

			addRegistrationStreamImage(subType, isFace(modality) ? RegistrationConstants.FACE_FULLFACE : modality,
					attempt, streamImage);

		}
	}

	private void addRegistrationStreamImage(String subType, String modality, int attempt, byte[] streamImage) {
		if (getRegistrationDTOFromSession() != null) {
			getRegistrationDTOFromSession().streamImages.put(String.format("%s_%s_%s", subType, modality, attempt),
					streamImage);
		}

	}

	public Image getBioStreamImage(String subType, String modality, int attempt) {

		return STREAM_IMAGES.get(String.format("%s_%s_%s", subType, modality, attempt));
	}

	public void refreshContinueButton() {
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "refreshContinueButton invoked");

		if (getListOfBiometricSubTypes().isEmpty()) {

			LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME,
					"refreshContinueButton NON of the BIOMETRIC FIELD IS ENABLED");
			return;
		}

		String currentSubType = getListOfBiometricSubTypes().get(currentPosition);

		List<String> bioAttributes = currentMap.get(currentSubType);

		List<String> leftHandBioAttributes = getContainsAllElements(RegistrationConstants.leftHandUiAttributes,
				bioAttributes);
		List<String> rightHandBioAttributes = getContainsAllElements(RegistrationConstants.rightHandUiAttributes,
				bioAttributes);
		List<String> twoThumbsBioAttributes = getContainsAllElements(RegistrationConstants.twoThumbsUiAttributes,
				bioAttributes);
		List<String> faceBioAttributes = getContainsAllElements(RegistrationConstants.faceUiAttributes, bioAttributes);
		List<String> irisBioAttributes = getContainsAllElements(RegistrationConstants.eyesUiAttributes, bioAttributes);

		String operator = getBooleanOperator();
		boolean considerExceptionAsCaptured = OR_OPERATOR.equals(operator) ? false : true;

		Map<String, Boolean> capturedDetails = new HashMap<String, Boolean>();
		capturedDetails = setCapturedDetailsMap(capturedDetails, leftHandBioAttributes,
				isBiometricsCaptured(currentSubType, leftHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, rightHandBioAttributes,
				isBiometricsCaptured(currentSubType, rightHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, twoThumbsBioAttributes,
				isBiometricsCaptured(currentSubType, twoThumbsBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, irisBioAttributes,
				isBiometricsCaptured(currentSubType, irisBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.IRIS_THRESHOLD), considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, faceBioAttributes, isBiometricsCaptured(currentSubType,
				faceBioAttributes, getThresholdScoreInInt(RegistrationConstants.FACE), considerExceptionAsCaptured));

		String expression = String.join(operator, bioAttributes);
		boolean result = MVEL.evalToBoolean(expression, capturedDetails);

		if (result && considerExceptionAsCaptured) {

			if (hasApplicantBiometricException()) {

				result = getRegistrationDTOFromSession().getDocuments().containsKey("proofOfException");

			}

		}
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "capturedDetails >> " + capturedDetails);
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,

				RegistrationConstants.APPLICATION_NAME, "Expression >> " + expression + " :: result >> " + result);
		if (result) {
			auditFactory.audit(AuditEvent.REG_BIO_CAPTURE_NEXT, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		}
		continueBtn.setDisable(result ? false : true);
	}

	private boolean hasApplicantBiometricException() {
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Checking whether applicant has biometric exceptions");

		boolean hasApplicantBiometricException = false;
		if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getBiometricExceptions() != null
				&& !getRegistrationDTOFromSession().getBiometricExceptions().isEmpty()) {
			for (Entry<String, BiometricsException> bs : getRegistrationDTOFromSession().getBiometricExceptions()
					.entrySet()) {
				if (isApplicant(bs.getValue().getIndividualType())) {

					LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton",
							RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
							"Applicant biometric exception found");

					hasApplicantBiometricException = true;
					break;
				}
			}
		}

		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME,
				"Completed checking whether applicant has biometric exceptions : " + hasApplicantBiometricException);

		return hasApplicantBiometricException;
	}

	/**
	 * Is UserOnboarding, should capture all biometrics as configured During
	 * registration, by default for NEW applicant, all bioattributes are mandatory
	 * (exceptions are considered as capture) On updateUIN, and if update biometrics
	 * is opted then all bioattributes are mandatory (exceptions are considered as
	 * capture) else any biometrics can be provided for authentication (exceptions
	 * are NOT considered as capture) On LostUIN, this is based on configuration. BY
	 * default all bioAttributes are mandatory
	 *
	 * @return
	 */
	private String getBooleanOperator() {
		if (isUserOnboardFlag)
			return AND_OPERATOR;

		String operator = AND_OPERATOR;
		switch (getRegistrationDTOFromSession().getRegistrationCategory()) {
		case RegistrationConstants.PACKET_TYPE_NEW:

			operator = RegistrationConstants.APPLICANT.equalsIgnoreCase(currentSubType) ? AND_OPERATOR : OR_OPERATOR;

			break;
		case RegistrationConstants.PACKET_TYPE_UPDATE:
			operator = getRegistrationDTOFromSession().isBiometricMarkedForUpdate()

					? (RegistrationConstants.APPLICANT.equalsIgnoreCase(currentSubType) ? AND_OPERATOR : OR_OPERATOR)

					: OR_OPERATOR;
			break;
		case RegistrationConstants.PACKET_TYPE_LOST:
			operator = getValueFromApplicationContext(RegistrationConstants.LOST_REGISTRATION_BIO_MVEL_OPERATOR);
			operator = operator == null ? AND_OPERATOR : operator;
			break;
		}
		return operator;
	}

	private Map<String, Boolean> setCapturedDetailsMap(Map<String, Boolean> capturedDetails, List<String> bioAttributes,
			boolean isBiometricsCaptured) {

		for (String bioAttribute : bioAttributes) {
			capturedDetails.put(bioAttribute, isBiometricsCaptured);
		}
		return capturedDetails;
	}

	private boolean isBiometricsCaptured(String subType, List<String> bioAttributes, int thresholdScore,
			boolean considerExceptionAsCaptured) {
		// if no bio attributes then it is not configured to be captured
		if (bioAttributes == null || bioAttributes.isEmpty())
			return false;

		boolean isCaptured = false, isForceCaptured = false;
		int qualityScore = 0, exceptionBioCount = 0;

		for (String bioAttribute : bioAttributes) {

			BiometricsDto biometricDTO = getBiometrics(subType, bioAttribute);
			if (biometricDTO != null) {
				/* Captures check */
				qualityScore += biometricDTO.getQualityScore();
				isCaptured = true;
				isForceCaptured = biometricDTO.isForceCaptured();

			} else if (isBiometricExceptionAvailable(subType, bioAttribute)) {
				/* Exception bio check */
				isCaptured = true;
				++exceptionBioCount;

			} else {
				isCaptured = false;
				break;
			}
		}

		if (isCaptured && !isForceCaptured) {
			if (bioAttributes.size() == exceptionBioCount)
				isCaptured = considerExceptionAsCaptured ? true : false;
			else
				isCaptured = (qualityScore / (bioAttributes.size() - exceptionBioCount)) >= thresholdScore;
		}
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME,
				"isBiometricsCaptured invoked  subType >> " + subType + " bioAttributes >> " + bioAttributes
						+ " exceptionBioCount >> " + exceptionBioCount + " isCaptured >> " + isCaptured);
		return isCaptured;
	}

	private BiometricsDto getBiometrics(String subType, String bioAttribute) {
		if (isUserOnboardFlag) {
			List<BiometricsDto> list = userOnboardService.getBiometrics(subType, Arrays.asList(bioAttribute));
			return !list.isEmpty() ? list.get(0) : null;
		} else
			return getRegistrationDTOFromSession().getBiometric(subType, bioAttribute);
	}

	public List<BiometricsDto> getBiometrics(String subType, List<String> bioAttribute) {
		if (isUserOnboardFlag) {
			List<BiometricsDto> list = userOnboardService.getBiometrics(subType, bioAttribute);
			return list;
		} else
			return getRegistrationDTOFromSession().getBiometric(subType, bioAttribute);
	}

	public boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
		if (isUserOnboardFlag)
			return userOnboardService.isBiometricException(subType, bioAttribute);
		else
			return getRegistrationDTOFromSession().isBiometricExceptionAvailable(subType, bioAttribute);
	}

	public void addBioScores(String subType, String modality, String attempt, double qualityScore) {

		BIO_SCORES.put(String.format("%s_%s_%s", subType, modality, attempt), qualityScore);
	}

	public double getBioScores(String subType, String modality, int attempt) {

		double qualityScore = 0.0;
		try {
			qualityScore = BIO_SCORES.get(String.format("%s_%s_%s", subType, modality, attempt));
		} catch (NullPointerException nullPointerException) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(nullPointerException));

		}

		return qualityScore;
	}

	public void clearBioCaptureInfo() {

		BIO_SCORES.clear();
		STREAM_IMAGES.clear();
	}

	private String getStubStreamImagePath(String modality) {
		String path = "";
		switch (modality) {
		case PacketManagerConstants.FINGERPRINT_SLAB_LEFT:
			path = RegistrationConstants.LEFTHAND_SLAP_FINGERPRINT_PATH;
			break;
		case PacketManagerConstants.FINGERPRINT_SLAB_RIGHT:
			path = RegistrationConstants.RIGHTHAND_SLAP_FINGERPRINT_PATH;
			break;
		case PacketManagerConstants.FINGERPRINT_SLAB_THUMBS:
			path = RegistrationConstants.BOTH_THUMBS_FINGERPRINT_PATH;
			break;
		case PacketManagerConstants.IRIS_DOUBLE:
			path = RegistrationConstants.IRIS_IMAGE_LOCAL;
			break;
		case "FACE":
		case PacketManagerConstants.FACE_FULLFACE:
			path = RegistrationConstants.FACE_IMG_PATH;
			break;
		}
		return path;
	}

	private List<String> getListOfBiometricSubTypes() {
		return new ArrayList<String>(currentMap.keySet());
	}

	private boolean identifyInLocalGallery(List<BiometricsDto> biometrics, String modality) {
		BiometricType biometricType = BiometricType.fromValue(modality);
		Map<String, List<BIR>> gallery = new HashMap<>();
		List<UserBiometric> userBiometrics = userDetailDAO.findAllActiveUsers(biometricType.value());
		if (userBiometrics.isEmpty())
			return false;

		userBiometrics.forEach(userBiometric -> {
			String userId = userBiometric.getUserBiometricId().getUsrId();
			gallery.computeIfAbsent(userId, k -> new ArrayList<BIR>())
					.add(buildBir(userBiometric.getBioIsoImage(), biometricType));
		});

		List<BIR> sample = new ArrayList<>(biometrics.size());
		biometrics.forEach(biometricDto -> {
			sample.add(buildBir(biometricDto.getAttributeISO(), biometricType));
		});

		try {
			Map<String, Boolean> result = bioAPIFactory.getBioProvider(biometricType, BiometricFunction.MATCH)
					.identify(sample, gallery, biometricType, null);
			return result.entrySet().stream().anyMatch(e -> e.getValue() == true);
		} catch (BiometricException e) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Failed to dedupe >> " + ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	private BIR buildBir(byte[] biometricImageISO, BiometricType modality) {
		return new BIRBuilder().withBdb(biometricImageISO)
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(new RegistryIDType())
						.withType(Collections.singletonList(SingleType.fromValue(modality.value())))
						.withPurpose(PurposeType.IDENTIFY).build())
				.build();
	}

	private void addImageInUIPane(String subType, String modality, Image uiImage, boolean isCaptured) {
		for (GridPane gridPane : leftHandImageBoxMap.values()) {
			if (gridPane.getId().equals(subType)) {

				for (Node node : gridPane.getChildren()) {

					if (node.getId().equalsIgnoreCase(modality)) {
						VBox vBox = (VBox) node;
						HBox hBox = (HBox) vBox.getChildren().get(0);
						// hBox.getChildren().clear();
						((ImageView) (hBox.getChildren().get(0))).setImage(uiImage != null ? uiImage
								: new Image(this.getClass().getResourceAsStream(getImageIconPath(modality))));

						if (isCaptured) {
							if (hBox.getChildren().size() == 1) {
								ImageView imageView;
								if (uiImage == null) {
									imageView = new ImageView(new Image(this.getClass()
											.getResourceAsStream(RegistrationConstants.EXCLAMATION_IMG_PATH)));
								} else {
									imageView = new ImageView(new Image(this.getClass()
											.getResourceAsStream(RegistrationConstants.TICK_CIRICLE_IMG_PATH)));
								}

								imageView.setFitWidth(40);
								imageView.setFitHeight(40);
								hBox.getChildren().add(imageView);
							}
						} else {

							if (hBox.getChildren().size() > 1) {
								hBox.getChildren().remove(1);
							}
						}

					}
				}

			}
		}

	}

	private Pane getExceptionImagePane(String modality, List<String> configBioAttributes,
			List<String> nonConfigBioAttributes, String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Getting exception image pane for modality : " + modality);

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Getting exception image pane for modality from BiometricsExceptionController: " + modality);

		Pane exceptionImagePane = getExceptionImagePane(modality);

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Completed of getting exception image pane");

		if (exceptionImagePane != null) {
			addExceptionsUiPane(exceptionImagePane, configBioAttributes, nonConfigBioAttributes, modality, subType);

			exceptionImagePane.setVisible(true);
			exceptionImagePane.setManaged(true);
		}
		return exceptionImagePane;

	}

	public void updateBiometricData(ImageView clickedImageView, List<ImageView> bioExceptionImagesForSameModality) {

		if (clickedImageView.getOpacity() == 0) {
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Marking exceptions for biometrics");

			auditFactory.audit(AuditEvent.REG_BIO_EXCEPTION_MARKING, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			clickedImageView.setOpacity(1);

			if (isUserOnboardFlag)
				userOnboardService.addOperatorBiometricException(currentSubType, clickedImageView.getId());
			else {
				getRegistrationDTOFromSession().addBiometricException(currentSubType, clickedImageView.getId(),
						clickedImageView.getId(), "Temporary", "Temporary");
			}
		} else {
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Removing exceptions for biometrics");

			auditFactory.audit(AuditEvent.REG_BIO_EXCEPTION_REMOVING, Components.REG_BIOMETRICS,
					SessionContext.userId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			clickedImageView.setOpacity(0);
			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometricException(currentSubType, clickedImageView.getId());
			else {
				getRegistrationDTOFromSession().removeBiometricException(currentSubType, clickedImageView.getId());

			}
		}

		if (hasApplicantBiometricException()) {

			setBiometricExceptionVBox(true);

		} else {

			if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getDocuments() != null) {

				getRegistrationDTOFromSession().getDocuments().remove("proofOfException");
			}
			addImageInUIPane(RegistrationConstants.APPLICANT, RegistrationConstants.EXCEPTION_PHOTO, null, false);
			setBiometricExceptionVBox(false);
		}

		boolean isAllMarked = true;
		for (ImageView exceptionImageView : bioExceptionImagesForSameModality) {

			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometrics(currentSubType, exceptionImageView.getId());
			else
				getRegistrationDTOFromSession().removeBiometric(currentSubType, exceptionImageView.getId());

			if (isAllMarked) {

				isAllMarked = isAllMarked && exceptionImageView.getOpacity() == 1 ? true : false;

			}

		}
		genericController.refreshFields();

		displayBiometric(currentModality);
		setScanButtonVisibility(isAllMarked, scanBtn);
		fxControl.refreshExceptionMarks();
	}

	private void setBiometricExceptionVBox(boolean visible) {
		if (exceptionVBox != null) {
			// exceptionVBox.setVisible(disable);
			exceptionVBox.setVisible(visible);

		}

	}

	private void addExceptionsUiPane(Pane pane, List<String> configBioAttributes, List<String> nonConfigBioAttributes,
			String modality, String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"started adding exception images in ui pane");

		if (pane != null) {
			List<Node> nodes = pane.getChildren();

			if (nodes != null && !nodes.isEmpty()) {
				for (Node node : nodes) {

					if (configBioAttributes.contains(node.getId())) {
						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Not marked exception image : " + node.getId() + " as default");

						node.setVisible(true);
						node.setDisable(false);
						node.setOpacity(isBiometricExceptionAvailable(subType, node.getId()) ? 1 : 0);

					}
					if (nonConfigBioAttributes.contains(node.getId())) {
						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Marked exception image : " + node.getId() + " as default");

						node.setVisible(true);
						node.setDisable(true);
						node.setOpacity(1.0);

						if (isUserOnboardFlag)
							userOnboardService.addOperatorBiometricException(currentSubType, node.getId());
						else {
							getRegistrationDTOFromSession().addBiometricException(currentSubType, node.getId(),
									node.getId(), "Temporary", "Temporary");
							genericController.refreshFields();
							;
						}
					}
				}
			}
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Completed adding exception images in ui pane");

	}

	private void setPopViewControllerMessage(boolean enableCloseButton, String message) {
		if (enableCloseButton) {
			scanPopUpViewController.enableCloseButton();
		}
		scanPopUpViewController.setScanningMsg(message);

	}

	public byte[] extractFaceImageData(byte[] decodedBioValue) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Started converting iso to jpg for face");

		try (DataInputStream din = new DataInputStream(new ByteArrayInputStream(decodedBioValue))) {

			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Started processing bio value : " + new Date());
			// Parsing general header
			byte[] format = new byte[4];
			din.read(format, 0, 4);
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"format >>>>>>>>>" + new String(format));

			byte[] version = new byte[4];
			din.read(version, 0, 4);

			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"version >>>>>>>>>" + new String(version));

			int recordLength = din.readInt(); // 4 bytes

			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"recordLength >>>>>>>>>" + recordLength);

			short numberofRepresentionRecord = din.readShort();

			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"numberofRepresentionRecord >>>>>>>>>" + numberofRepresentionRecord);

			// NOTE: No certification schemes are available for this part of ISO/IEC 19794.
			byte certificationFlag = din.readByte();
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"certificationFlag >>>>>>>>>" + certificationFlag);

			byte[] temporalSequence = new byte[2];
			din.read(temporalSequence, 0, 2);
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"recordLength >>>>>>>>>" + recordLength);

			// Parsing representation header
			int representationLength = din.readInt();
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"representationLength >>>>>>>>>" + representationLength);

			byte[] representationData = new byte[representationLength - 4];
			din.read(representationData, 0, representationData.length);

			try (DataInputStream rdin = new DataInputStream(new ByteArrayInputStream(representationData))) {
				byte[] captureDetails = new byte[14];
				rdin.read(captureDetails, 0, 14);

				byte noOfQualityBlocks = rdin.readByte();
				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"noOfQualityBlocks >>>>>>>>>" + noOfQualityBlocks);

				if (noOfQualityBlocks > 0) {
					byte[] qualityBlocks = new byte[noOfQualityBlocks * 5];
					rdin.read(qualityBlocks, 0, qualityBlocks.length);
				}

				short noOfLandmarkPoints = rdin.readShort();
				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"noOfLandmarkPoints >>>>>>>>>" + noOfLandmarkPoints);

				// read next 15 bytes
				byte[] facialInformation = new byte[15];
				rdin.read(facialInformation, 0, 15);

				// read all landmarkpoints
				if (noOfLandmarkPoints > 0) {
					byte[] landmarkPoints = new byte[noOfLandmarkPoints * 8];
					rdin.read(landmarkPoints, 0, landmarkPoints.length);
				}

				byte faceType = rdin.readByte();
				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"faceType >>>>>>>>>" + faceType);

				// The (1 byte) Image Data Type field denotes the encoding type of the Image
				// Data block
				// JPEG 00 HEX
				// JPEG2000 lossy 01 HEX
				// JPEG 2000 lossless 02 HEX
				// PNG 03 HEX
				// Reserved by SC 37 for future use 04 HEX to FF HEX
				byte imageDataType = rdin.readByte();
				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"imageDataType >>>>>>>>>" + imageDataType);

				byte[] otherImageInformation = new byte[9];
				rdin.read(otherImageInformation, 0, otherImageInformation.length);

				// reading representationData -> imageData + 3d info + 3d data
				int lengthOfImageData = rdin.readInt();
				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"lengthOfImageData >>>>>>>>>" + lengthOfImageData);

				byte[] image = new byte[lengthOfImageData];
				rdin.read(image, 0, lengthOfImageData);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(ImageIO.read(new ByteArrayInputStream(image)), "jpg", baos);

				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Converted JP2 to jpg");

				return baos.toByteArray();
			}
		} catch (Exception exception) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Error while parsing iso to jpg : " + ExceptionUtils.getStackTrace(exception));

		}
		return null;
	}

	public byte[] extractIrisImageData(byte[] decodedBioValue) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Extracting iris image from decode bio value");

		try (DataInputStream din = new DataInputStream(new ByteArrayInputStream(decodedBioValue))) {

			// general header parsing
			byte[] formatIdentifier = new byte[4];
			din.read(formatIdentifier, 0, formatIdentifier.length);

			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"formatIdentifier >>>> " + new String(formatIdentifier));

			int version = din.readInt();
			int recordLength = din.readInt();
			short noOfRepresentations = din.readShort();
			byte certificationFlag = din.readByte();

			// 1 -- left / right
			// 2 -- both left and right
			// 0 -- unknown
			byte noOfIrisRepresented = din.readByte();

			for (int i = 0; i < noOfRepresentations; i++) {
				// Reading representation header
				int representationLength = din.readInt();

				byte[] captureDetails = new byte[14];
				din.read(captureDetails, 0, captureDetails.length);

				// qualityBlock
				byte noOfQualityBlocks = din.readByte();
				if (noOfQualityBlocks > 0) {
					byte[] qualityBlocks = new byte[noOfQualityBlocks * 5];
					din.read(qualityBlocks, 0, qualityBlocks.length);
				}

				short representationSequenceNo = din.readShort();

				// 0 -- undefined
				// 1 -- right
				// 2 - left
				byte eyeLabel = din.readByte();

				byte imageType = din.readByte(); // cropped / uncropped

				// 2 -- raw
				// 10 -- jpeg2000
				// 14 -- png
				byte imageFormat = din.readByte();

				byte[] otherDetails = new byte[24];
				din.read(otherDetails, 0, otherDetails.length);

				int imageLength = din.readInt();
				byte[] image = new byte[imageLength];
				din.read(image, 0, image.length);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(ImageIO.read(new ByteArrayInputStream(image)), "jpg", baos);

				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Converted JP2 to jpg");

				return baos.toByteArray();
			}
		} catch (Exception exception) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Error while parsing iso to jpg : " + ExceptionUtils.getStackTrace(exception));

		}
		return null;
	}

	private Pane getExceptionImagePane(String modality) {

		Pane exceptionImagePane = null;

		if (isExceptionPhoto(modality)) {
			return null;
		}

		if (modality != null) {
			switch (modality) {

			case RegistrationConstants.FACE:
				exceptionImagePane = null;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				exceptionImagePane = getTwoIrisSlabExceptionPane(modality);
				break;

			case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				exceptionImagePane = getRightSlabExceptionPane(modality);
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				exceptionImagePane = getLeftSlabExceptionPane(modality);
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
				exceptionImagePane = getTwoThumbsSlabExceptionPane(modality);
				break;

			}
		}

		return exceptionImagePane;
	}

	private Pane getLeftSlabExceptionPane(String modality) {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Preparing Left Slab Exception Image ");

		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.LEFTPALM_IMG_PATH, 144, 163, 6, 6, true, true,
				false);

		// Left Middle

		ImageView leftMiddleImageView = getImageView("leftMiddle", RegistrationConstants.LEFTMIDDLE_IMG_PATH, 92, 27,
				70, 41, true, true, true);
		ImageView leftIndexImageView = getImageView("leftIndex", RegistrationConstants.LEFTINDEX_IMG_PATH, 75, 28, 97,
				55, true, true, true);
		ImageView leftRingImageView = getImageView("leftRing", RegistrationConstants.LEFTRING_IMG_PATH, 75, 28, 45, 55,
				true, true, true);
		ImageView leftLittleImageView = getImageView("leftLittle", RegistrationConstants.LEFTLITTLE_IMG_PATH, 49, 26,
				19, 82, true, true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(leftMiddleImageView);
		pane.getChildren().add(leftIndexImageView);
		pane.getChildren().add(leftRingImageView);
		pane.getChildren().add(leftLittleImageView);

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Completed Preparing Left Slap Exception Image ");

		return pane;
	}

	private Pane getRightSlabExceptionPane(String modality) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Preparing Right Slab Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.RIGHTPALM_IMG_PATH, 144, 163, 3, 4, true,
				true, false);

		// Left Middle

		ImageView middleImageView = getImageView("rightMiddle", RegistrationConstants.LEFTMIDDLE_IMG_PATH, 92, 30, 72,
				37, true, true, true);
		ImageView ringImageView = getImageView("rightRing", RegistrationConstants.LEFTRING_IMG_PATH, 82, 27, 99, 54,
				true, true, true);
		ImageView indexImageView = getImageView("rightIndex", RegistrationConstants.LEFTINDEX_IMG_PATH, 75, 30, 46, 54,
				true, true, true);

		ImageView littleImageView = getImageView("rightLittle", RegistrationConstants.LEFTLITTLE_IMG_PATH, 57, 28, 125,
				75, true, true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(middleImageView);
		pane.getChildren().add(ringImageView);
		pane.getChildren().add(indexImageView);
		pane.getChildren().add(littleImageView);
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Completed Preparing Right Slab Exception Image ");

		return pane;
	}

	private Pane getTwoThumbsSlabExceptionPane(String modality) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Preparing Two Thumbs Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.THUMB_IMG_PATH, 144, 171, 14, 7, true, true,
				false);

		ImageView left = getImageView("leftThumb", RegistrationConstants.LEFTTHUMB_IMG_PATH, 92, 28, 55, 37, true, true,
				true);
		ImageView right = getImageView("rightThumb", RegistrationConstants.LEFTTHUMB_IMG_PATH, 99, 28, 115, 38, true,
				true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(left);
		pane.getChildren().add(right);
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Completed Preparing Two Thumbs Exception Image ");
		return pane;
	}

	private Pane getTwoIrisSlabExceptionPane(String modality) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Preparing Two Iris Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);
		ImageView topImageView = getImageView(null, RegistrationConstants.DOUBLE_IRIS_IMG_PATH, 144, 189.0, 7, 4, true,
				true, false);

		ImageView rightImageView = getImageView("rightEye", RegistrationConstants.RIGHTEYE_IMG_PATH, 43, 48, 118, 54,
				true, true, true);
		ImageView leftImageView = getImageView("leftEye", RegistrationConstants.LEFTEYE_IMG_PATH, 43, 48, 35, 54, true,
				true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(rightImageView);
		pane.getChildren().add(leftImageView);

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Completed Preparing Two Iris Exception Image ");
		return pane;
	}

	private ImageView getImageView(String id, String url, double fitHeight, double fitWidth, double layoutX,
			double layoutY, boolean pickOnBounds, boolean preserveRatio, boolean hasActionEvent) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Started Preparing exception image view for : " + id);

		ImageView imageView = new ImageView(new Image(this.getClass().getResourceAsStream(url)));

		if (id != null) {
			imageView.setId(id);
		}
		imageView.setFitHeight(fitHeight);
		imageView.setFitWidth(fitWidth);
		imageView.setLayoutX(layoutX);
		imageView.setLayoutY(layoutY);
		imageView.setPickOnBounds(pickOnBounds);
		imageView.setPreserveRatio(preserveRatio);

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Is Action required : " + hasActionEvent);

		if (hasActionEvent) {
			imageView.setOnMouseClicked((event) -> {

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Action event triggered on click of exception image");
				addException(event);
			});

		}

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Completed Preparing exception image view for : " + id);
		return imageView;

	}

	public void addException(MouseEvent event) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Clicked on exception Image");

		ImageView exceptionImage = (ImageView) event.getSource();

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Clicked on exception Image : " + exceptionImage.getId());

		Pane pane = (Pane) exceptionImage.getParent();

		List<ImageView> paneExceptionBioAttributes = new LinkedList<>();
		for (Node node : pane.getChildren()) {
			if (node instanceof ImageView && node.getId() != null && !node.getId().isEmpty()) {

				paneExceptionBioAttributes.add((ImageView) node);
			}
		}
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"All exception images for same modality" + paneExceptionBioAttributes);

		updateBiometricData(exceptionImage, paneExceptionBioAttributes);

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Add or remove exception completed");

	}

	private void streamLocalCamera() {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Streaming Using Local Camera");

		if (scanPopUpViewController.getPopupStage() != null) {
			scanPopUpViewController.getPopupStage().close();
		}

		LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Capturing with local camera");

		documentScanController.startStream(this);
	}

	public void init(BiometricFxControl fxControl, String subType, String modality, List<String> configBioAttributes,
			List<String> nonConfigBioAttributes) throws IOException {

		this.fxControl = fxControl;
		this.currentSubType = subType;
		this.currentModality = modality;
		this.configBioAttributes = configBioAttributes;
		this.nonConfigBioAttributes = nonConfigBioAttributes;

		biometricPopUpStage = new Stage();
		biometricPopUpStage.initStyle(StageStyle.UNDECORATED);
		biometricPopUpStage.setResizable(false);
		Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.BIOMETRIC_FXML));

		Scene scene = new Scene(scanPopup, width, height);
		scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
		biometricPopUpStage.setScene(scene);
		biometricPopUpStage.initModality(Modality.WINDOW_MODAL);
		biometricPopUpStage.initOwner(fXComponents.getStage());
		biometricPopUpStage.show();

		displayBiometric(modality);

	}

	/**
	 * event class to exit from present pop up window.
	 * 
	 * @param event
	 */
	public void exitWindow(ActionEvent event) {

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Calling exit window to close the popup");

		biometricPopUpStage.close();

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Popup is closed");

	}

	public boolean canContinue(String subType, List<String> bioAttributes) {

		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "refreshContinueButton invoked");

		this.currentSubType = subType;
		if (bioAttributes == null || bioAttributes.isEmpty()) {

			LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME,
					"refreshContinueButton NON of the BIOMETRIC FIELD IS ENABLED");
			return true;
		}

		List<String> leftHandBioAttributes = getContainsAllElements(RegistrationConstants.leftHandUiAttributes,
				bioAttributes);
		List<String> rightHandBioAttributes = getContainsAllElements(RegistrationConstants.rightHandUiAttributes,
				bioAttributes);
		List<String> twoThumbsBioAttributes = getContainsAllElements(RegistrationConstants.twoThumbsUiAttributes,
				bioAttributes);
		List<String> faceBioAttributes = getContainsAllElements(RegistrationConstants.faceUiAttributes, bioAttributes);
		List<String> irisBioAttributes = getContainsAllElements(RegistrationConstants.eyesUiAttributes, bioAttributes);

		String operator = getBooleanOperator();
		boolean considerExceptionAsCaptured = OR_OPERATOR.equals(operator) ? false : true;

		Map<String, Boolean> capturedDetails = new HashMap<String, Boolean>();
		capturedDetails = setCapturedDetailsMap(capturedDetails, leftHandBioAttributes,
				isBiometricsCaptured(currentSubType, leftHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, rightHandBioAttributes,
				isBiometricsCaptured(currentSubType, rightHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, twoThumbsBioAttributes,
				isBiometricsCaptured(currentSubType, twoThumbsBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, irisBioAttributes,
				isBiometricsCaptured(currentSubType, irisBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.IRIS_THRESHOLD), considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, faceBioAttributes, isBiometricsCaptured(currentSubType,
				faceBioAttributes, getThresholdScoreInInt(RegistrationConstants.FACE), considerExceptionAsCaptured));

		String expression = String.join(operator, bioAttributes);
		boolean result = MVEL.evalToBoolean(expression, capturedDetails);

		if (result && considerExceptionAsCaptured) {

			if (hasApplicantBiometricException()) {

				result = getRegistrationDTOFromSession().getDocuments().containsKey("proofOfException");

			}

		}
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "capturedDetails >> " + capturedDetails);
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,

				RegistrationConstants.APPLICATION_NAME, "Expression >> " + expression + " :: result >> " + result);
		if (result) {
			auditFactory.audit(AuditEvent.REG_BIO_CAPTURE_NEXT, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		}
		return result;

	}

}