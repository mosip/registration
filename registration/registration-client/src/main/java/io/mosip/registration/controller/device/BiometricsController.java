package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_BIOMETRIC_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.commons.packet.constants.PacketManagerConstants;
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
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.UserOnboardParentController;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.BiometricAttributeDto;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.operator.UserOnboardService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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

/**
 * {@code GuardianBiometricscontroller} is to capture and display the captured
 * biometrics of Guardian
 * 
 * @author Sravya Surampalli
 * @since 1.0
 */
@Controller
public class BiometricsController extends BaseController /* implements Initializable */ {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BiometricsController.class);

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

	private int rowIndex = 0;

	@FXML
	private Label subTypeLabel;

	@Autowired
	private BiometricExceptionsController biometricExceptionsController;

	private GridPane exceptionBiometricsPane;

	private Service<List<BiometricsDto>> taskService;

	public void stopRCaptureService() {
		if(taskService!=null && taskService.isRunning()) {
			taskService.cancel();
		}
	}

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
		currentMap = new LinkedHashMap<>();
		fxUtils = FXUtils.getInstance();
		leftHandImageBoxMap = new HashMap<>();
		exceptionMap = new HashMap<>();

		Image backInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK_FOCUSED));
		Image backImage = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK));
		backButton.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				backImageView.setImage(backInWhite);
			} else {
				backImageView.setImage(backImage);
			}
		});

		applicationLabelBundle = applicationContext.getApplicationLanguageBundle();

		if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getSelectionListDTO() != null) {

			registrationNavlabel.setText(ApplicationContext.applicationLanguageBundle()
					.getString(RegistrationConstants.UIN_UPDATE_UINUPDATENAVLBL));

		} else if (getRegistrationDTOFromSession() != null
				&& getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory() != null
				&& getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
						.equals(RegistrationConstants.PACKET_TYPE_LOST)) {

			registrationNavlabel.setText(
					ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));

		}
	}

	public void populateBiometricPage(boolean isUserOnboard, boolean isGoingBack) {
		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"populateBiometricPage invoked, isUserOnboard : " + isUserOnboard);

		try {
			BaseController.load(getClass().getResource("/fxml/BiometricExceptions.fxml"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isUserOnboardFlag = isUserOnboard;
		Map<Entry<String, String>, Map<String, List<List<String>>>> mapToProcess = isUserOnboardFlag
				? getOnboardUserMap()
				: getconfigureAndNonConfiguredBioAttributes(Arrays.asList(
						getValue(RegistrationConstants.FINGERPRINT_SLAB_LEFT,
								RegistrationConstants.leftHandUiAttributes),
						getValue(RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
								RegistrationConstants.rightHandUiAttributes),
						getValue(RegistrationConstants.FINGERPRINT_SLAB_THUMBS,
								RegistrationConstants.twoThumbsUiAttributes),
						getValue(RegistrationConstants.IRIS_DOUBLE, RegistrationConstants.eyesUiAttributes),
						getValue(RegistrationConstants.FACE, RegistrationConstants.faceUiAttributes)));

		if (mapToProcess.isEmpty()) {
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"populateBiometricPage mapToProcess is EMTPY");
			updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, false);
			return;
		}

		removeInapplicableCapturedData(mapToProcess);

		if (isUserOnboard) {
			registrationNavlabel.setVisible(false);
			backButton.setVisible(false);
			gheaderfooter.setVisible(false);
			continueBtn.setText("SAVE");
		} else {
			registrationNavlabel.setVisible(true);
			backButton.setVisible(true);
			gheaderfooter.setVisible(true);
			continueBtn.setText("CONTINUE");
		}

		checkBoxPane.getChildren().clear();
		leftPanelImageGridPane.getChildren().clear();

		// comboBoxMap.clear();
		// checkBoxMap.clear();
		currentMap.clear();
		leftHandImageBoxMap.clear();
		exceptionMap.clear();

		rowIndex = 0;
		leftPanelImageGridPane.setAlignment(Pos.TOP_LEFT);
		leftPanelImageGridPane.setPadding(new Insets(70, 100, 100, 70)); // margins around the whole grid
		// (top/right/bottom/left)
		for (Entry<Entry<String, String>, Map<String, List<List<String>>>> subType : mapToProcess.entrySet()) {

			// leftPanelImageGridPane.getChildren().add(new Label("Biometrics"));
			GridPane gridPane = getGridPane(subType.getKey());

			// ComboBox<Entry<String, String>> comboBox = buildComboBox(subType.getKey());
			HashMap<String, VBox> subMap = new HashMap<>();
			currentMap.put(subType.getKey().getKey(), new ArrayList<String>());
			for (Entry<String, List<List<String>>> biometric : subType.getValue().entrySet()) {
				List<List<String>> listOfCheckBoxes = biometric.getValue();
				currentMap.get(subType.getKey().getKey()).addAll(listOfCheckBoxes.get(0));
				if (!listOfCheckBoxes.get(0).isEmpty()) {

					// comboBox.getItems().add(new SimpleEntry<String, String>(biometric.getKey(),
					// applicationLabelBundle.getString(biometric.getKey())));

					gridPane.add(getImageVBox(biometric.getKey(), subType.getKey().getKey(), listOfCheckBoxes.get(0)),
							1, rowIndex);

					rowIndex++;

					// gridPane.getChildren().add(getImageVBox(biometric.getKey()));
					if (!listOfCheckBoxes.get(0).get(0).equals("face")) {

						VBox vboxForCheckBox = new VBox();
						vboxForCheckBox.setSpacing(5);
						Label checkBoxTitle = new Label();
						checkBoxTitle.setText(applicationLabelBundle.getString("exceptionCheckBoxPaneLabel"));
						vboxForCheckBox.getChildren().addAll(checkBoxTitle);
						checkBoxTitle.getStyleClass().add("demoGraphicFieldLabel");

						vboxForCheckBox.getChildren().add(getExceptionImagePane(biometric.getKey(),
								listOfCheckBoxes.get(0), listOfCheckBoxes.get(1), subType.getKey().getKey()));

						vboxForCheckBox.setVisible(false);
						vboxForCheckBox.setManaged(false);

						checkBoxPane.add(vboxForCheckBox, 0, 0);

						// checkBoxPane.setVisible(true);

						// checkBoxMap.put(subType.getKey().getKey(), subMap);
						subMap.put(biometric.getKey(), vboxForCheckBox);

						exceptionMap.put(subType.getKey().getKey(), subMap);

					}
				}
			}
		}

		initializeState(isGoingBack);
	}

	private void updateBiometricData(VBox vboxForCheckBox, CheckBox checkBox) {

		if (checkBox.isSelected()) {
			if (isUserOnboardFlag)
				userOnboardService.addOperatorBiometricException(currentSubType, checkBox.getId());
			else
				getRegistrationDTOFromSession().addBiometricException(currentSubType, checkBox.getId(),
						checkBox.getId(), "Temporary", "Temporary");
		} else {
			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometricException(currentSubType, checkBox.getId());
			else
				getRegistrationDTOFromSession().removeBiometricException(currentSubType, checkBox.getId());
		}

		boolean isAllMarked = true;
		for (Node exceptionCheckBox : vboxForCheckBox.getChildren()) {

			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometrics(currentSubType, exceptionCheckBox.getId());
			else
				getRegistrationDTOFromSession().removeBiometric(currentSubType, exceptionCheckBox.getId());

			if (isAllMarked) {
				if (exceptionCheckBox instanceof CheckBox) {
					CheckBox cB = (CheckBox) exceptionCheckBox;
					isAllMarked = cB.isSelected();
				}
			}

		}

		addImageInUIPane(currentSubType, currentModality, null, isAllMarked);
	}

	private void initializeState(boolean isGoingBack) {

		sizeOfLeftGridPaneImageList = leftHandImageBoxMap.size();

		if (sizeOfLeftGridPaneImageList > 0) {
			if (isGoingBack) {
				currentPosition = leftHandImageBoxMap.size() - 1;
				currentSubType = getListOfBiometricSubTypes().get(currentPosition);
				previousPosition = currentPosition - 1;
			} else {
				currentPosition = 0;
				currentSubType = getListOfBiometricSubTypes().get(currentPosition);
				previousPosition = 0;
			}
		}

		currentSubType = getListOfBiometricSubTypes().get(currentPosition);
		enableGridPane(findImageListGridPane());
		biometricBox.setVisible(false);
		retryBox.setVisible(false);
		refreshContinueButton();
		displaycurrentUiElements();
	}

	private GridPane findImageListGridPane() {

		return leftHandImageBoxMap.get(getListOfBiometricSubTypes().get(currentPosition));
	}

	private void enableGridPane(GridPane gridPane) {
		if (gridPane != null) {
			gridPane.setVisible(true);
			gridPane.setManaged(true);

			if (!isUserOnboardFlag) {
				subTypeLabel.setText(getMapOfbiometricSubtypes().get(currentSubType));
			} else {

				subTypeLabel.setText("Operator/Supervisor Biometrics");
			}

		}
	}

	private void disableGridPane(GridPane gridPane) {
		if (gridPane != null) {
			gridPane.setVisible(false);
			gridPane.setManaged(false);
			subTypeLabel.setText(RegistrationConstants.EMPTY);

		}
	}

	private void removeInapplicableCapturedData(
			Map<Entry<String, String>, Map<String, List<List<String>>>> mapToProcess) {

		// Nothing to remove on user-onboard
		if (isUserOnboardFlag)
			return;

		if (!getRegistrationDTOFromSession().getBiometrics().isEmpty()
				|| !getRegistrationDTOFromSession().getBiometricExceptions().isEmpty()) {

			List<String> applicableSubTypes = new ArrayList<String>();
			for (Entry<String, String> subType : mapToProcess.keySet()) {
				applicableSubTypes.add(String.format("%s_.*", subType.getKey()));
			}
			String pattern = String.join("|", applicableSubTypes);

			List<String> keys = new ArrayList<String>();
			keys.addAll(getRegistrationDTOFromSession().getBiometrics().keySet());
			keys.addAll(getRegistrationDTOFromSession().getBiometricExceptions().keySet());

			for (String key : keys) {
				if (!key.matches(pattern)) {
					getRegistrationDTOFromSession().getBiometrics().remove(key);
					getRegistrationDTOFromSession().getBiometricExceptions().remove(key);
				}
			}
		}
	}

	private void setScanButtonVisibility(boolean isAllExceptions, Button scanBtn2) {
		scanBtn.setDisable(isAllExceptions);

	}

	private boolean isAllExceptions(List<Node> exceptionImageViews) {
		boolean isAllExceptions = true;
		if (exceptionImageViews != null && !exceptionImageViews.isEmpty()) {

			for (Node exceptionImageView : ((Pane) exceptionImageViews.get(1)).getChildren()) {
				if (exceptionImageView instanceof ImageView && exceptionImageView.getId() != null
						&& !exceptionImageView.getId().isEmpty()) {
					isAllExceptions = ((ImageView) exceptionImageView).getOpacity() == 1 ? isAllExceptions
							? isBiometricExceptionAvailable(currentSubType, exceptionImageView.getId())
							: isAllExceptions : false;
				}
			}
		}

		return isAllExceptions;
	}

	private void goToNext() {
		if (currentPosition + 1 < sizeOfLeftGridPaneImageList) {

			disableGridPane(findImageListGridPane());

			previousPosition = currentPosition;
			++currentPosition;

			currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			enableGridPane(findImageListGridPane());

			refreshContinueButton();

			displaycurrentUiElements();

		}
	}

	@SuppressWarnings("unchecked")
	private void displaycurrentUiElements() {
		try {

			GridPane gridPane = findImageListGridPane();

			if (gridPane.getChildren() != null) {

				displayBiometric(gridPane.getChildren().get(0).getId());
			}
			// ComboBox<Object> comboBox = (ComboBox<Object>)
			// findComboBox().getChildren().get(1);
			//
			// comboBox.setValue((SimpleEntry<String, String>) comboBox.getItems().get(0));
		} catch (NullPointerException | ClassCastException exception) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));

		}

	}

	private void goToPrevious() {
		if (currentPosition > 0) {
			disableGridPane(findImageListGridPane());
			previousPosition = currentPosition;
			currentPosition--;

			currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			enableGridPane(findImageListGridPane());

			refreshContinueButton();

			displaycurrentUiElements();
		}
	}

	/**
	 * Displays biometrics
	 *
	 * @param event
	 *            the event for displaying biometrics
	 */
	private void displayBiometric(String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Displaying biometrics to capture");

		retryBox.setVisible(true);
		biometricBox.setVisible(true);
		biometricType.setText(applicationLabelBundle.getString(modality));

		disableLastCheckBoxSection();
		this.currentModality = modality;
		enableCurrentCheckBoxSection();

		// get List of captured Biometrics based on nonExceptionBio Attributes
		List<BiometricsDto> capturedBiometrics = null;

		if (!isFace(modality)) {
			List<Node> checkBoxNodes = getCheckBoxes(currentSubType, currentModality);

			List<String> exceptionBioAttributes = null;
			List<String> nonExceptionBioAttributes = null;

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
							nonExceptionBioAttributes = nonExceptionBioAttributes != null ? nonExceptionBioAttributes
									: new LinkedList<String>();
							nonExceptionBioAttributes.add(bioAttribute);
						}
					}
				}
			}

			if (nonExceptionBioAttributes != null) {
				capturedBiometrics = getBiometrics(currentSubType, nonExceptionBioAttributes);
			}

		} else {
			capturedBiometrics = getBiometrics(currentSubType,
					Arrays.asList(RegistrationConstants.faceUiAttributes.get(0)));
		}

		updateBiometric(modality, getImageIconPath(modality), getBiometricThreshold(modality), getRetryCount(modality));

		if (capturedBiometrics != null && !capturedBiometrics.isEmpty()) {

			loadBiometricsUIElements(capturedBiometrics, currentSubType, modality);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Parent/Guardian Biometrics captured");
	}

	private String getRetryCount(String modality) {
		// modality = modality.toLowerCase();

		String irisRetryCount = null;

		if (modality != null) {
			switch (modality) {

			case RegistrationConstants.FACE:
				irisRetryCount = RegistrationConstants.IRIS_RETRY_COUNT;
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
				biomnetricThreshold = RegistrationConstants.IRIS_THRESHOLD;
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

	private String getImageIconPath(String modality) {
		// modality = modality.toLowerCase();

		String imageIconPath = null;
		if (modality != null) {
			switch (modality) {

			case RegistrationConstants.FACE:
				imageIconPath = RegistrationConstants.FACE_IMG_PATH;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				imageIconPath = RegistrationConstants.RIGHT_IRIS_IMG_PATH;
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

	private void enableCurrentCheckBoxSection() {

		if (exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)) != null && exceptionMap
				.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality) != null) {
			exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
					.setVisible(true);
			exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
					.setManaged(true);
			checkBoxPane.setVisible(true);
			checkBoxPane.setManaged(true);

		}

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
	 * Scan the biometrics
	 *
	 * @param event
	 *            the event for scanning biometrics
	 */
	@FXML
	private void scan(ActionEvent event) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Displaying Scan popup for capturing biometrics");

		scanPopUpViewController.init(this, "Biometrics");

		Service<MdmBioDevice> deviceSearchTask = new Service<MdmBioDevice>() {
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
								"Capture request started" + System.currentTimeMillis());

						return deviceSpecificationFactory.getDeviceInfoByModality(
								isFace(currentModality) ? RegistrationConstants.FACE_FULLFACE : currentModality);

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

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Error while streaming : " + ExceptionUtils.getStackTrace(exception));

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

		 taskService = new Service<List<BiometricsDto>>() {
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

						currentSubType = getListOfBiometricSubTypes().get(currentPosition);
						return rCapture(currentSubType, currentModality);

					}
				};
			}
		};
		taskService.start();

		taskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR);

				scanPopUpViewController.getPopupStage().close();
				// Enable Auto-Logout
				SessionContext.setAutoLogout(true);

				streamer.setUrlStream(null);
			}
		});
		taskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				try {
					List<BiometricsDto> mdsCapturedBiometricsList = taskService.getValue();

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"biometrics captured from mock/real MDM");

					boolean isValidBiometric = mdsCapturedBiometricsList != null
							&& !mdsCapturedBiometricsList.isEmpty();

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"biometrics captured from mock/real MDM was valid : " + isValidBiometric);

					if (isValidBiometric) {
						// validate local de-dup check
						boolean isMatchedWithLocalBiometrics = false;
						if (bioService.isMdmEnabled() && !isUserOnboardFlag) {

							LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"Doing local de-dup validation");

							isMatchedWithLocalBiometrics = identifyInLocalGallery(mdsCapturedBiometricsList,
									Biometric
											.getSingleTypeByModality(
													isFace(currentModality) ? "FACE_FULL FACE" : currentModality)
											.value());
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
									auditFactory.audit(getAuditEventForScan(currentModality), Components.REG_BIOMETRICS,
											SessionContext.userId(),
											AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
								}
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
									addBioStreamImage(currentSubType, currentModality,
											registrationDTOBiometricsList.get(0).getNumOfRetries(),
											(bioService.isMdmEnabled()) ? streamer.getStreamImageBytes() : null);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
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

	private List<BiometricsDto> rCapture(String subType, String modality) throws RegBaseCheckedException, IOException {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Finding exception bio attributes" + System.currentTimeMillis());

		List<String> exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType, currentModality);
		// Check count
		int count = 1;

		MDMRequestDto mdmRequestDto = new MDMRequestDto(
				isFace(modality) ? RegistrationConstants.FACE_FULLFACE : modality,
				exceptionBioAttributes.toArray(new String[0]), "Registration",
				io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(
						RegistrationConstants.SERVER_ACTIVE_PROFILE),
				Integer.valueOf(getCaptureTimeOut()), count,
				getThresholdScoreInInt(getThresholdKeyByBioType(modality)));

		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"exceptionBioAttributes passed to mock/real MDM >>> " + exceptionBioAttributes);

		return bioService.captureModality(mdmRequestDto);

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

			return getRegistrationDTOFromSession().addAllBiometrics(subType, biometricsMap,
					getThresholdScoreInDouble(getThresholdKeyByBioType(currentModality)),
					getMaxRetryByModality(currentModality));
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

	private double getThresholdScoreInDouble(String thresholdKey) {
		/* Get Configued threshold score for bio type */

		String thresholdScore = getValueFromApplicationContext(thresholdKey);

		return thresholdScore != null ? Double.valueOf(thresholdScore) : 0;
	}

	private String getCaptureTimeOut() {

		/* Get Configued capture timeOut */
		return getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT);
	}

	/**
	 * Navigating to previous section
	 *
	 * @param event
	 *            the event for navigating to previous section
	 */
	@FXML
	private void previous(ActionEvent event) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Navigates to previous section");

		if (currentPosition != 0) {
			disableLastCheckBoxSection();
			goToPrevious();
			return;
		}

		registrationController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
				getPageByAction(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.PREVIOUS));

	}

	/**
	 * Navigating to next section
	 *
	 * @param event
	 *            the event for navigating to next section
	 */
	@FXML
	private void next(ActionEvent event) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Navigates to next section");

		if (currentPosition != sizeOfLeftGridPaneImageList - 1) {
			disableLastCheckBoxSection();
			goToNext();
			return;
		}

		if (isUserOnboardFlag) {
			userOnboardParentController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getOnboardPageDetails(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.NEXT));
		} else {
			registrationController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getPageByAction(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.NEXT));
		}

		initializeState(false);

	}

	/**
	 * Updating biometrics
	 *
	 * @param bioType
	 *            biometric type
	 * @param bioImage
	 *            biometric image
	 * @param biometricThreshold
	 *            threshold of biometric
	 * @param retryCount
	 *            retry count
	 */
	private void updateBiometric(String bioType, String bioImage, String biometricThreshold, String retryCount) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating biometrics and clearing previous data");
		this.bioType = constructBioType(bioType);
		clearCaptureData();
		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.BIOMETRIC_PANES_SELECTED);
		biometricImage.setImage(new Image(this.getClass().getResourceAsStream(bioImage)));

		bioValue = bioType;
		thresholdScoreLabel
				.setText(getQualityScore(Double.parseDouble(getValueFromApplicationContext(biometricThreshold))));
		createQualityBox(retryCount, biometricThreshold);
		qualityScore.setText(RegistrationConstants.HYPHEN);
		attemptSlap.setText(RegistrationConstants.HYPHEN);
		// duplicateCheckLbl.setText(RegistrationConstants.EMPTY);

		retryBox.setVisible(true);
		biometricBox.setVisible(true);

		bioProgress.setProgress(0);
		qualityText.setText("");

		if (!isFace(currentModality)) {
			setScanButtonVisibility(isAllExceptions(getCheckBoxes(currentSubType, currentModality)), scanBtn);
		} else {
			setScanButtonVisibility(false, scanBtn);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated biometrics and cleared previous data");
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
	 * @param capturedBio
	 *            biometric
	 * @param qltyScore
	 *            Qulaity score
	 * @param retry
	 *            retrycount
	 * @param thresholdValue
	 *            threshold value
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

	private int getMaxRetryByModality(String currentModality) {
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
												? RegistrationConstants.IRIS_RETRY_COUNT
												: modality;
	}

	/**
	 * Updating captured values
	 * 
	 * @param retryCount
	 *            retry count
	 * @param biometricThreshold
	 *            threshold value
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
		for (int retry = 0; retry < Integer.parseInt(getValueFromApplicationContext(retryCount)); retry++) {
			Label label = new Label();
			label.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
			label.setId(RegistrationConstants.RETRY_ATTEMPT_ID + (retry + 1));
			label.setVisible(true);
			label.setText(String.valueOf(retry + 1));
			label.setAlignment(Pos.CENTER);

			bioRetryBox.getChildren().add(label);
		}
		bioRetryBox.setOnMouseClicked(mouseEventHandler);

		String threshold = getValueFromApplicationContext(biometricThreshold);

		thresholdLabel.setAlignment(Pos.CENTER);
		thresholdLabel.setText(RegistrationUIConstants.THRESHOLD.concat("  ").concat(threshold)
				.concat(RegistrationConstants.PERCENTAGE));
		thresholdPane1.setPercentWidth(Double.parseDouble(threshold));
		thresholdPane2.setPercentWidth(100.00 - (Double.parseDouble(threshold)));
		// }

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated Quality and threshold values of biometrics");

	}

	/**
	 * Clear attempts box.
	 *
	 * @param styleClass
	 *            the style class
	 * @param retries
	 *            the retries
	 */
	private void clearAttemptsBox(String styleClass, int retries) {
		for (int retryBox = 1; retryBox <= retries; retryBox++) {
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retryBox).getStyleClass().clear();
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retryBox).getStyleClass().add(styleClass);
		}

		boolean nextRetryFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + ++retries) != null;
		while (nextRetryFound) {
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries).getStyleClass().clear();
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries).getStyleClass()
					.add(RegistrationConstants.QUALITY_LABEL_GREY);
			nextRetryFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + ++retries) != null;
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
		if (streamImage == null) {
			String imagePath = getStubStreamImagePath(modality);
			STREAM_IMAGES.put(String.format("%s_%s_%s", subType, modality, attempt),
					new Image(this.getClass().getResourceAsStream(imagePath)));
			if (getRegistrationDTOFromSession() != null) {
				getRegistrationDTOFromSession().streamImages.put(String.format("%s_%s_%s", subType, modality, attempt),
						IOUtils.toByteArray(this.getClass().getResourceAsStream(imagePath)));
			}
		} else {
			STREAM_IMAGES.put(String.format("%s_%s_%s", subType, modality, attempt),
					new Image(new ByteArrayInputStream(streamImage)));
			if (getRegistrationDTOFromSession() != null) {
				getRegistrationDTOFromSession().streamImages.put(String.format("%s_%s_%s", subType, modality, attempt),
						streamImage);
			}
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

		// if one or more biometric is marked as exception, then mandate collecting of
		// POE
		if (!isPOECollected(currentSubType)) {
			continueBtn.setDisable(true);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.EXCEPTION_PHOTO_MANDATORY);
			LOGGER.error("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME, "POE documents required");
			return;
		}

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
			operator = "introducer".equalsIgnoreCase(currentSubType) ? OR_OPERATOR : AND_OPERATOR;
			break;
		case RegistrationConstants.PACKET_TYPE_UPDATE:
			operator = getRegistrationDTOFromSession().isBiometricMarkedForUpdate()
					? ("introducer".equalsIgnoreCase(currentSubType) ? OR_OPERATOR : AND_OPERATOR)
					: OR_OPERATOR;
			break;
		case RegistrationConstants.PACKET_TYPE_LOST:
			operator = getValueFromApplicationContext(RegistrationConstants.LOST_REGISTRATION_BIO_MVEL_OPERATOR);
			operator = operator == null ? AND_OPERATOR : operator;
			break;
		}
		return operator;
	}

	/**
	 * POE needs to be collected only when biometrics all collected 1. only for
	 * applicant exceptions in new registration 2. update UIN and when applicant
	 * biometrics is opted to be updated 3. Not required for lostUIN
	 * 
	 * @return
	 */
	private boolean isPOECollected(String subtype) {
		if (isUserOnboardFlag || getRegistrationDTOFromSession().getBiometricExceptions().isEmpty()
				|| !RegistrationConstants.APPLICANT.equalsIgnoreCase(subtype))
			return true;

		// No exceptions found for provided subtype
		if (!getRegistrationDTOFromSession().getBiometricExceptions().keySet().stream()
				.anyMatch(k -> k.startsWith(String.format("%s_", subtype))))
			return true;

		LOGGER.debug("REGISTRATION - BIOMETRICS", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "isPOECollected invoked  subType >> " + subtype);

		if (RegistrationConstants.PACKET_TYPE_NEW.equals(getRegistrationDTOFromSession().getRegistrationCategory())
				|| (RegistrationConstants.PACKET_TYPE_UPDATE
						.equals(getRegistrationDTOFromSession().getRegistrationCategory())
						&& getRegistrationDTOFromSession().isBiometricMarkedForUpdate())) {

			List<String> poeFields = getValidationMap().values().stream()
					.filter(field -> field.getSubType() != null && "POE".equalsIgnoreCase(field.getSubType()))
					.map(UiSchemaDTO::getId).collect(Collectors.toList());

			boolean collected = false;
			if (poeFields != null && poeFields.size() > 0) {
				for (String fieldId : poeFields) {
					if (getRegistrationDTOFromSession().getDocuments().containsKey(fieldId)) {
						collected = true;
						break;
					}
				}
				return collected;
			}
		}
		return true;
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

	private List<BiometricsDto> getBiometrics(String subType, List<String> bioAttribute) {
		if (isUserOnboardFlag) {
			List<BiometricsDto> list = userOnboardService.getBiometrics(subType, bioAttribute);
			return list;
		} else
			return getRegistrationDTOFromSession().getBiometric(subType, bioAttribute);
	}

	private boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
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

	private VBox getImageVBox(String modality, String subtype, List<String> configBioAttributes) {

		VBox vBox = new VBox();

		vBox.setAlignment(Pos.BASELINE_LEFT);
		vBox.setId(modality);

		// Create Label with modality
		// Label label = new Label();
		// label.setText(applicationLabelBundle.getString(modality));
		// vBox.getChildren().add(label);

		HBox hBox = new HBox();
		// hBox.setAlignment(Pos.BOTTOM_RIGHT);

		// Image modalityImage = getImage(modality);
		List<BiometricsDto> biometricsDtos = getBiometrics(currentSubType, configBioAttributes);

		Image image = null;
		if (biometricsDtos != null && !biometricsDtos.isEmpty()) {

			image = getBioStreamImage(subtype, modality, biometricsDtos.get(0).getNumOfRetries());
		}

		ImageView imageView = new ImageView(
				image != null ? image : new Image(this.getClass().getResourceAsStream(getImageIconPath(modality))));
		imageView.setFitHeight(80);
		imageView.setFitWidth(85);

		Tooltip tooltip = new Tooltip(applicationLabelBundle.getString(modality));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		// Tooltip.install(hBox, tooltip);
		hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
		hBox.setOnMouseExited(event -> tooltip.hide());
		hBox.getChildren().add(imageView);

		boolean isAllExceptions = true;
		for (String configBioAttribute : configBioAttributes) {

			isAllExceptions = isBiometricExceptionAvailable(currentSubType, configBioAttribute) ? isAllExceptions
					: false;

			if (!isAllExceptions) {
				break;
			}
		}
		if (image != null || isAllExceptions) {
			if (hBox.getChildren().size() == 1) {
				ImageView tickImageView = new ImageView(
						new Image(this.getClass().getResourceAsStream(RegistrationConstants.TICK_CIRICLE_IMG_PATH)));

				tickImageView.setFitWidth(30);
				tickImageView.setFitHeight(30);
				hBox.getChildren().add(tickImageView);
			}
		}

		vBox.getChildren().add(hBox);

		// vBox.getChildren().add(imageView);

		vBox.setOnMouseClicked((event) -> {
			displayBiometric(vBox.getId());
		});

		vBox.setFillWidth(true);
		vBox.setMinWidth(100);

		// vBox.setMinHeight(100);
		vBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
		// vBox.setBorder(new Border(
		// new BorderStroke(Color.PINK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
		// BorderWidths.FULL)));

		return vBox;
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
								ImageView imageView = new ImageView(new Image(this.getClass()
										.getResourceAsStream(RegistrationConstants.TICK_CIRICLE_IMG_PATH)));

								imageView.setFitWidth(30);
								imageView.setFitHeight(30);
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

	private GridPane getGridPane(Entry<String, String> subMapKey) {

		GridPane gridPane = new GridPane();

		gridPane.setId(subMapKey.getKey());
		Label label = new Label(subMapKey.getValue());
		label.getStyleClass().add("paneHeader");

		leftPanelImageGridPane.add(gridPane, 1, 1);

		leftHandImageBoxMap.put(subMapKey.getKey(), gridPane);

		disableGridPane(gridPane);

		return gridPane;

	}

	private Pane getExceptionImagePane(String modality, List<String> configBioAttributes,
			List<String> nonConfigBioAttributes, String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Getting exception image pane for modality : " + modality);
		Pane exceptionImagePane = null;

		exceptionBiometricsPane = biometricExceptionsController.getExceptionBiometricsPane();

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Getting exception image pane for modality from BiometricsExceptionController: " + modality);

		if (exceptionBiometricsPane != null) {

			List<Node> listOfPanes = exceptionBiometricsPane.getChildren();

			for (Node paneList : listOfPanes) {
				Pane pane = (Pane) paneList;

				if (pane.getId().equalsIgnoreCase(modality)) {
					exceptionImagePane = pane;
					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Found exception image for : " + modality);

					break;
				}
			}

		}

		addExceptionsUiPane(exceptionImagePane, configBioAttributes, nonConfigBioAttributes, modality, subType);

		exceptionImagePane.setVisible(true);
		exceptionImagePane.setManaged(true);

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Completed of getting exception image pane");

		return exceptionImagePane;

	}

	public void updateBiometricData(ImageView clickedImageView, List<ImageView> bioExceptionImagesForSameModality) {

		if (clickedImageView.getOpacity() == 0) {
			clickedImageView.setOpacity(1);
			if (isUserOnboardFlag)
				userOnboardService.addOperatorBiometricException(currentSubType, clickedImageView.getId());
			else
				getRegistrationDTOFromSession().addBiometricException(currentSubType, clickedImageView.getId(),
						clickedImageView.getId(), "Temporary", "Temporary");
		} else {
			clickedImageView.setOpacity(0);
			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometricException(currentSubType, clickedImageView.getId());
			else
				getRegistrationDTOFromSession().removeBiometricException(currentSubType, clickedImageView.getId());
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

		displayBiometric(currentModality);
		addImageInUIPane(currentSubType, currentModality, null, isAllMarked);
		setScanButtonVisibility(isAllMarked, scanBtn);
		refreshContinueButton();
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
						else
							getRegistrationDTOFromSession().addBiometricException(currentSubType, node.getId(),
									node.getId(), "Temporary", "Temporary");
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

 
}