package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.AbstractMap.SimpleEntry;
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

import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

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
import io.mosip.kernel.packetmanager.constants.PacketManagerConstants;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.registration.config.AppConfig;
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
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.sync.MasterSyncService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * {@code GuardianBiometricscontroller} is to capture and display the captured
 * biometrics of Guardian
 * 
 * @author Sravya Surampalli
 * @since 1.0
 */
@Controller
public class GuardianBiometricsController extends BaseController /* implements Initializable */ {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(GuardianBiometricsController.class);

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

	@FXML
	private Label duplicateCheckLbl;

	@FXML
	private Label guardianBiometricsLabel;

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

	// @Autowired
	// private Transliteration<String> transliteration;

	@Autowired
	private MasterSyncService masterSync;

	@Autowired
	private WebCameraController webCameraController;

	private String bioValue;

	private FXUtils fxUtils;

	private int leftSlapCount = 0;
	private int rightSlapCount = 0;
	private int thumbCount = 0;
	private int rightIrisCount = 0;
	private int leftIrisCount = 0;
	private int retries = 0;

	private List<String> leftSlapexceptionList = new ArrayList<String>();
	private List<String> rightSlapexceptionList = new ArrayList<String>();
	private List<String> thumbsexceptionList = new ArrayList<String>();

	// private List<String> fingerException;

	private String bioType;

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
	private GridPane ContentHeader;

	@FXML
	private GridPane checkBoxPane;

	private ResourceBundle applicationLabelBundle;

	private String currentModality;

	private int currentPosition = -1;

	private int previousPosition = -1;

	private int sizeOfCombobox = -1;

	private HashMap<String, VBox> comboBoxMap;

	private HashMap<String, HashMap<String, VBox>> checkBoxMap;

	private HashMap<String, List<String>> currentMap;

	private static final String AND_OPERATOR = " && ";
	private static final String OR_OPERATOR = " || ";

	@Autowired
	private UserOnboardService userOnboardService;

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


	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@FXML
	public void initialize() {
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Loading of Guardian Biometric screen started");
		comboBoxMap = new HashMap<>();
		checkBoxMap = new HashMap<>();
		currentMap = new LinkedHashMap<>();
		fxUtils = FXUtils.getInstance();
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

	public void populateBiometricPage(boolean isUserOnboard) {
		LOGGER.debug(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"populateBiometricPage invoked, isUserOnboard : " + isUserOnboard);
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
			LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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

		ContentHeader.getChildren().clear();
		checkBoxPane.getChildren().clear();

		comboBoxMap.clear();
		checkBoxMap.clear();
		currentMap.clear();

		for (Entry<Entry<String, String>, Map<String, List<List<String>>>> subType : mapToProcess.entrySet()) {
			ComboBox<Entry<String, String>> comboBox = buildComboBox(subType.getKey());
			HashMap<String, VBox> subMap = new HashMap<>();
			currentMap.put(subType.getKey().getKey(), new ArrayList<String>());

			for (Entry<String, List<List<String>>> biometric : subType.getValue().entrySet()) {
				List<List<String>> listOfCheckBoxes = biometric.getValue();
				currentMap.get(subType.getKey().getKey()).addAll(listOfCheckBoxes.get(0));
				if (!listOfCheckBoxes.get(0).isEmpty()) {

					comboBox.getItems().add(new SimpleEntry<String, String>(biometric.getKey(),
							applicationLabelBundle.getString(biometric.getKey())));

					if (!listOfCheckBoxes.get(0).get(0).equals("face")) {

						VBox vboxForCheckBox = new VBox();
						vboxForCheckBox.setSpacing(5);
						Label checkBoxTitle = new Label();
						checkBoxTitle.setText(applicationLabelBundle.getString("exceptionCheckBoxPaneLabel"));
						vboxForCheckBox.getChildren().addAll(checkBoxTitle);
						checkBoxTitle.getStyleClass().add("demoGraphicFieldLabel");

						for (int i = 0; i < listOfCheckBoxes.size(); i++) {

							for (String exception : biometric.getValue().get(i)) {
								CheckBox checkBox = new CheckBox(applicationLabelBundle.getString(exception));
								checkBox.setId(exception);
								checkBox.getStyleClass().add(RegistrationConstants.updateUinCheckBox);
								if (i == 1) {
									checkBox.setSelected(true);
									checkBox.setDisable(true);
								}

								vboxForCheckBox.getChildren().add(checkBox);
								checkBox.selectedProperty().addListener((obsValue, oldValue, newValue) -> {
									updateBiometricData(vboxForCheckBox, checkBox);
									setScanButtonVisibility(isAllExceptions(vboxForCheckBox.getChildren()), scanBtn);
									refreshContinueButton();
									displayBiometric(currentModality);

								});

								if (!isUserOnboardFlag && getRegistrationDTOFromSession()
										.isBiometricExceptionAvailable(currentSubType, checkBox.getId()))
									checkBox.setSelected(true);
							}
						}

						checkBoxPane.add(vboxForCheckBox, 0, 0);

						vboxForCheckBox.setVisible(false);
						vboxForCheckBox.setManaged(false);

						checkBoxMap.put(subType.getKey().getKey(), subMap);
						subMap.put(biometric.getKey(), vboxForCheckBox);

					}
				}
			}
		}

		initializeState();
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

		for (Node exceptionCheckBox : vboxForCheckBox.getChildren()) {

			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometrics(currentSubType, exceptionCheckBox.getId());
			else
				getRegistrationDTOFromSession().removeBiometric(currentSubType, exceptionCheckBox.getId());

		}
	}

	private void initializeState() {
		sizeOfCombobox = comboBoxMap.size();
		if (sizeOfCombobox > 0) {
			currentPosition = 0;
			currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			previousPosition = 0;

		}

		if (null != findComboBox()) {
			findComboBox().setVisible(true);
			findComboBox().setManaged(true);
		}

		biometricBox.setVisible(false);
		retryBox.setVisible(false);
		refreshContinueButton();
		displaycurrentUiElements();
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

	private ComboBox<Entry<String, String>> buildComboBox(Entry<String, String> subMapKey) {
		VBox vb = new VBox();
		vb.setSpacing(10);
		vb.setId(subMapKey + "vbox");
		Label label = new Label(subMapKey.getValue());
		label.getStyleClass().add("paneHeader");
		ComboBox<Entry<String, String>> comboBox = new ComboBox<>();

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			displayBiometric(newValue.getKey());
		});
		renderBiometrics(comboBox);
		comboBox.getStyleClass().add("demographicCombobox");
		comboBox.setId(subMapKey + "combobox");

		vb.getChildren().addAll(label, comboBox);
		ContentHeader.add(vb, 1, 1);
		vb.setVisible(false);
		vb.setManaged(false);
		comboBoxMap.put(subMapKey.getKey(), vb);
		return comboBox;
	}

	private void setScanButtonVisibility(boolean isAllExceptions, Button scanBtn2) {
		scanBtn.setDisable(isAllExceptions);

	}

	private boolean isAllExceptions(List<Node> checkBoxNodes) {
		boolean isAllExceptions = true;
		if (checkBoxNodes != null && !checkBoxNodes.isEmpty()) {

			for (Node exceptionCheckBox : checkBoxNodes) {
				if (exceptionCheckBox instanceof CheckBox) {
					isAllExceptions = !((CheckBox) exceptionCheckBox).isDisable()
							? isAllExceptions ? isBiometricExceptionAvailable(currentSubType, exceptionCheckBox.getId())
									: isAllExceptions
							: isAllExceptions;
				}
			}
		}

		return isAllExceptions;
	}

	private VBox findComboBox() {
		return comboBoxMap.get(getListOfBiometricSubTypes().get(currentPosition));
	}

	private void goToNext() {
		if (currentPosition + 1 < sizeOfCombobox) {
			findComboBox().setVisible(false);
			findComboBox().setManaged(false);
			previousPosition = currentPosition;
			currentPosition++;
			findComboBox().setVisible(true);
			findComboBox().setManaged(true);
			currentSubType = getListOfBiometricSubTypes().get(currentPosition);

			refreshContinueButton();

			displaycurrentUiElements();

		}
	}

	@SuppressWarnings("unchecked")
	private void displaycurrentUiElements() {
		try {
			ComboBox<Object> comboBox = (ComboBox<Object>) findComboBox().getChildren().get(1);

			comboBox.setValue((SimpleEntry<String, String>) comboBox.getItems().get(0));
		} catch (NullPointerException | ClassCastException exception) {
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));

		}

	}

	private void goToPrevious() {
		if (currentPosition > 0) {
			findComboBox().setVisible(false);
			findComboBox().setManaged(false);
			previousPosition = currentPosition;
			currentPosition--;
			findComboBox().setVisible(true);
			findComboBox().setManaged(true);
			currentSubType = getListOfBiometricSubTypes().get(currentPosition);

			refreshContinueButton();

			displaycurrentUiElements();
		}
	}

	public void intializeCaptureCount() {
		leftSlapCount = 0;
		rightSlapCount = 0;
		thumbCount = 0;
		rightIrisCount = 0;
		leftIrisCount = 0;
		retries = 0;
	}

	/**
	 * Displays biometrics
	 *
	 * @param event
	 *            the event for displaying biometrics
	 */
	@FXML
	private void displayBiometric(ActionEvent event) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Displaying biometrics to capture");

		if (null != biometricTypecombo.getValue()) {

			currentModality = getModality(biometricTypecombo.getValue().getName());

			if (biometricTypecombo.getValue().getName().equalsIgnoreCase(RegistrationUIConstants.RIGHT_SLAP)) {
				updateBiometric(biometricTypecombo.getValue().getName(), RegistrationConstants.RIGHTPALM_IMG_PATH,
						RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD,
						RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
			} else if (biometricTypecombo.getValue().getName().equalsIgnoreCase(RegistrationUIConstants.LEFT_SLAP)) {
				updateBiometric(biometricTypecombo.getValue().getName(), RegistrationConstants.LEFTPALM_IMG_PATH,
						RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD,
						RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
			} else if (biometricTypecombo.getValue().getName().equalsIgnoreCase(RegistrationUIConstants.THUMBS)) {
				updateBiometric(biometricTypecombo.getValue().getName(), RegistrationConstants.THUMB_IMG_PATH,
						RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD,
						RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
			} else if (biometricTypecombo.getValue().getName().equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)) {
				updateBiometric(biometricTypecombo.getValue().getName(), RegistrationConstants.RIGHT_IRIS_IMG_PATH,
						RegistrationConstants.IRIS_THRESHOLD, RegistrationConstants.IRIS_RETRY_COUNT);
			} else if (biometricTypecombo.getValue().getName().equalsIgnoreCase(RegistrationUIConstants.LEFT_IRIS)) {
				updateBiometric(biometricTypecombo.getValue().getName(), RegistrationConstants.LEFT_IRIS_IMG_PATH,
						RegistrationConstants.IRIS_THRESHOLD, RegistrationConstants.IRIS_RETRY_COUNT);
			}
		}

		if (!bioValue.equalsIgnoreCase(RegistrationUIConstants.SELECT)) {
			scanBtn.setDisable(false);
			continueBtn.setDisable(true);
			biometricBox.setVisible(true);
			retryBox.setVisible(true);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Parent/Guardian Biometrics captured");
	}

	/**
	 * Displays biometrics
	 *
	 * @param event
	 *            the event for displaying biometrics
	 */
	private void displayBiometric(String modality) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Displaying biometrics to capture");

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

			for (Node node : checkBoxNodes) {
				if (node instanceof CheckBox) {
					CheckBox checkBox = (CheckBox) node;
					String bioAttribute = checkBox.getId();
					if (isBiometricExceptionAvailable(currentSubType, bioAttribute))
						checkBox.setSelected(true);

					if (checkBox.isSelected() || checkBox.isDisable()) {
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

			if (nonExceptionBioAttributes != null) {
				capturedBiometrics = getBiometrics(currentSubType, nonExceptionBioAttributes);
			}

		} else {
			capturedBiometrics = getBiometrics(currentSubType,
					Arrays.asList(RegistrationConstants.faceUiAttributes.get(0)));
		}

		if (capturedBiometrics == null || capturedBiometrics.isEmpty()) {
			if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				updateBiometric(modality, RegistrationConstants.RIGHTPALM_IMG_PATH,
						RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD,
						RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
			} else if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
				updateBiometric(modality, RegistrationConstants.LEFTPALM_IMG_PATH,
						RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD,
						RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
			} else if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
				updateBiometric(modality, RegistrationConstants.THUMB_IMG_PATH,
						RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD,
						RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
			} else if (modality.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)) {
				updateBiometric(modality, RegistrationConstants.RIGHT_IRIS_IMG_PATH,
						RegistrationConstants.IRIS_THRESHOLD, RegistrationConstants.IRIS_RETRY_COUNT);
			} else if (modality.equalsIgnoreCase(RegistrationConstants.FACE)) {
				updateBiometric(modality, RegistrationConstants.FACE_IMG_PATH, RegistrationConstants.IRIS_THRESHOLD,
						RegistrationConstants.IRIS_RETRY_COUNT);
			}
		} else {
			loadBiometricsUIElements(capturedBiometrics, currentSubType, modality);
		}

		// if (!bioValue.equalsIgnoreCase(RegistrationUIConstants.SELECT)) {
		// scanBtn.setDisable(false);
		// continueBtn.setDisable(true);
		// biometricBox.setVisible(true);
		// retryBox.setVisible(true);
		// }

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Parent/Guardian Biometrics captured");
	}

	/*
	 * private String getRegistrationDTOBioAttribute(String attribute) {
	 * 
	 * String bioAttributeByMap = RegistrationConstants.regBioMap.get(attribute);
	 * 
	 * return bioAttributeByMap != null ? bioAttributeByMap : attribute; }
	 * 
	 * private String getRegistrationDTOBioAttributeByMdsAttribute(String attribute)
	 * {
	 * 
	 * String bioAttributeByMap =
	 * RegistrationConstants.mdsToRegBioMap.get(attribute);
	 * 
	 * return bioAttributeByMap != null ? bioAttributeByMap : attribute; }
	 */

	private void enableCurrentCheckBoxSection() {

		if (checkBoxMap.get(getListOfBiometricSubTypes().get(currentPosition)) != null && checkBoxMap
				.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality) != null) {
			checkBoxMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
					.setVisible(true);
			checkBoxMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
					.setManaged(true);
			checkBoxPane.setVisible(true);

		}

	}

	private void disableLastCheckBoxSection() {
		checkBoxPane.setVisible(false);
		if (currentPosition != -1) {
			if (this.currentModality != null
					&& checkBoxMap.get(getListOfBiometricSubTypes().get(currentPosition)) != null && checkBoxMap
							.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality) != null) {
				checkBoxMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
						.setVisible(false);
				checkBoxMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
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

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Displaying Scan popup for capturing biometrics");

		scanPopUpViewController.init(this, "Biometrics");

		if (bioService.isMdmEnabled()) {
			streamer.startStream(isFace(currentModality) ? RegistrationConstants.FACE_FULLFACE : currentModality,
					scanPopUpViewController.getScanImage(), biometricImage);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scan popup closed and captured biometrics");

	}

	private boolean isFace(String currentModality) {
		return currentModality.toUpperCase().contains(RegistrationConstants.FACE.toUpperCase());
	}

	private List<String> getSelectedExceptionsByBioType(String subType, String modality)
			throws RegBaseCheckedException {
		List<String> selectedExceptions = new LinkedList<String>();

		// Get List of check boxes using the grid pane
		List<Node> exceptionCheckBoxes = getCheckBoxes(subType, modality);
		if (exceptionCheckBoxes != null && !exceptionCheckBoxes.isEmpty()) {
			for (Node checkBoxx : exceptionCheckBoxes) {
				if (checkBoxx instanceof CheckBox) {
					CheckBox checkBox = (CheckBox) checkBoxx;
					if (checkBox.isSelected()) {

						selectedExceptions.add(checkBox.getId());

					}
				}
			}
		}
		return selectedExceptions;
	}

	private List<Node> getCheckBoxes(String subType, String modality) {

		List<Node> exceptionCheckBoxes = new ArrayList<>();
		if (checkBoxMap.get(subType) != null && checkBoxMap.get(subType).get(modality) != null) {
			exceptionCheckBoxes = checkBoxMap.get(subType).get(modality).getChildren();
		}
		return exceptionCheckBoxes;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.controller.BaseController#scan(javafx.stage.Stage)
	 */
	@Override
	public void scan(Stage popupStage) {
		try {

			LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Scan process started for capturing biometrics");

			currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			captureBiometrics(currentSubType, currentModality);

			refreshContinueButton();

		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"Exception while getting the scanned biometrics for user registration: %s caused by %s",
							runtimeException.getMessage(),
							runtimeException.getCause() + ExceptionUtils.getStackTrace(runtimeException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scan process ended for capturing biometrics");
	}

	private void captureBiometrics(String subType, String modality) throws RegBaseCheckedException {

		// Get Exception attributes
		List<String> exceptionBioAttributes = getSelectedExceptionsByBioType(subType, modality);

		// Check count
		int count = 1;

		MDMRequestDto mdmRequestDto = new MDMRequestDto(
				isFace(modality) ? RegistrationConstants.FACE_FULLFACE : modality,
				exceptionBioAttributes.toArray(new String[0]), "Registration",
				io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(
						RegistrationConstants.SERVER_ACTIVE_PROFILE),
				Integer.valueOf(getCaptureTimeOut()), count,
				getThresholdScoreInInt(getThresholdKeyByBioType(modality)));

		LOGGER.debug(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"exceptionBioAttributes passed to mock/real MDM >>> " + exceptionBioAttributes);
		try {
			// Get Response from the MDS
			List<BiometricsDto> mdsCapturedBiometricsList = bioService.captureModality(mdmRequestDto);

			boolean isValidBiometric = mdsCapturedBiometricsList != null && !mdsCapturedBiometricsList.isEmpty();

			// validate local de-dup check
			boolean isMatchedWithLocalBiometrics = false;
			if(bioService.isMdmEnabled()) {
//				isMatchedWithLocalBiometrics = identifyInLocalGallery(mdsCapturedBiometricsList, modality);
			}
			
			if (isValidBiometric && !isMatchedWithLocalBiometrics) {

				List<BiometricsDto> registrationDTOBiometricsList = new LinkedList<>();

				double qualityScore = 0;
				// save to registration DTO
				for (BiometricsDto biometricDTO : mdsCapturedBiometricsList) {
					LOGGER.debug(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"BiometricDTO captured from mock/real MDM >>> " + biometricDTO.getBioAttribute());

					if (!exceptionBioAttributes.isEmpty()
							&& exceptionBioAttributes.contains(biometricDTO.getBioAttribute())) {
						continue;
					} else {
						qualityScore += biometricDTO.getQualityScore();
						biometricDTO.setSubType(currentSubType);
						registrationDTOBiometricsList.add(biometricDTO);
					}
				}

				registrationDTOBiometricsList = saveCapturedBiometricData(subType, registrationDTOBiometricsList);

				if (!registrationDTOBiometricsList.isEmpty()) {
					// if all the above check success show alert capture success
					generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS);

					Image streamImage = null;
					if (bioService.isMdmEnabled()) {
						streamImage = streamer.getStreamImage();
					} else {
						streamImage = new Image(this.getClass().getResourceAsStream(getStubStreamImagePath(modality)));
					}

					addBioStreamImage(subType, currentModality, registrationDTOBiometricsList.get(0).getNumOfRetries(),
							streamImage);

					addBioScores(subType, currentModality,
							String.valueOf(registrationDTOBiometricsList.get(0).getNumOfRetries()),
							qualityScore / registrationDTOBiometricsList.size());

					// using captured response fill the fields like quality score and progress
					// bar,,etc,.. UI
					loadBiometricsUIElements(registrationDTOBiometricsList, subType, currentModality);
				} else {
					// request response mismatch
					generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
				}

			} else {

				// if any above checks failed show alert capture failure
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("Exception while capturing biometrics : ", exception.getMessage(),
							ExceptionUtils.getStackTrace(exception)));

			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
		}
		scanPopUpViewController.getPopupStage().close();
	}

	private List<BiometricsDto> saveCapturedBiometricData(String subType, List<BiometricsDto> biometrics) {
		LOGGER.debug(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"saveCapturedBiometricData invoked size >> " + biometrics.size());
		if (isUserOnboardFlag) {
			List<BiometricsDto> savedCaptures = new ArrayList<>();
			for (BiometricsDto value : biometrics) {
				LOGGER.debug(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"updating userOnboard biometric data >> " + value.getModalityName());
				savedCaptures.add(userOnboardService.addOperatorBiometrics(subType, value.getBioAttribute(), value));
				// savedCaptures.add(userOnboardService.addOperatorBiometrics(subType,
				// io.mosip.registration.mdm.dto.Biometric.getUiSchemaAttributeName(
				// value.getBioAttribute(), mosipBioDeviceManger.getLatestSpecVersion()),
				// value));

			}
			return savedCaptures;
		} else {

			Map<String, BiometricsDto> biometricsMap = new LinkedHashMap<>();

			for (BiometricsDto biometricsDto : biometrics) {
				biometricsMap.put(biometricsDto.getBioAttribute(), biometricsDto);
				// biometricsMap.put(
				// io.mosip.registration.mdm.dto.Biometric.getUiSchemaAttributeName(
				// biometricsDto.getBioAttribute(),
				// mosipBioDeviceManger.getLatestSpecVersion()),
				// biometricsDto);

			}

			return getRegistrationDTOFromSession().addAllBiometrics(subType, biometricsMap,
					getThresholdScoreInDouble(getThresholdKeyByBioType(currentModality)),
					getMaxRetryByModality(currentModality));
		}
	}

	private void loadBiometricsUIElements(List<BiometricsDto> biometricDTOList, String subType, String modality) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating progress Bar,Text and attempts Box in UI");

		int retry = biometricDTOList.get(0).getNumOfRetries();

		setCapturedValues(getAverageQualityScore(biometricDTOList), retry,
				getThresholdScoreInInt(getThresholdKeyByBioType(modality)));

		// Get the stream image from Bio ServiceImpl and load it in the image pane
		biometricImage.setImage(getBioStreamImage(subType, modality, retry));
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

	/*
	 * private void clearCapturesBySubTypeAndBioType(String subType, String bioType)
	 * {
	 * 
	 * List<String> bioSubTypeList = getBioSubTypeListByBioType(bioType);
	 * 
	 * // TODO Merge with anusha code to remove all bio captures of bio sub type //
	 * TODO pass subType and bioType }
	 * 
	 * private void clearExceptionBySubTypeAndBioType(String subType, String
	 * bioSubType) {
	 * 
	 * // TODO Merge with anusha code to remove exception of bio sub type
	 * 
	 * // TODO pass subType and bioType }
	 * 
	 * private List<String> getBioSubTypeListByBioType(String bioType) {
	 * 
	 * // TODO Get Constant attributes return null; }
	 * 
	 * private String getThresholdScore(String thresholdKey) { // Get Configued
	 * threshold score for bio type
	 * 
	 * return getValueFromApplicationContext(thresholdKey); }
	 */

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

	private String getModality(String bioType) {

		// Get Modality
		return bioType.equalsIgnoreCase(RegistrationUIConstants.RIGHT_SLAP)
				? RegistrationConstants.FINGERPRINT_SLAB_RIGHT
				: bioType.equalsIgnoreCase(RegistrationUIConstants.LEFT_SLAP)
						? RegistrationConstants.FINGERPRINT_SLAB_LEFT
						: bioType.equalsIgnoreCase(RegistrationUIConstants.THUMBS)
								? RegistrationConstants.FINGERPRINT_SLAB_THUMBS
								: bioType.equalsIgnoreCase(RegistrationConstants.IRIS)
										? RegistrationConstants.LEFT.concat(RegistrationConstants.EYE)
										: bioType.equalsIgnoreCase(RegistrationConstants.FACE)
												? RegistrationConstants.FACE
												: bioType;

	}

	/**
	 * Navigating to previous section
	 *
	 * @param event
	 *            the event for navigating to previous section
	 */
	@FXML
	private void previous(ActionEvent event) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Navigates to previous section");

		if (currentPosition != 0) {
			disableLastCheckBoxSection();
			goToPrevious();
			return;
		}
		webCameraController.closeWebcam();

		// if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
		// SessionContext.map().put(RegistrationConstants.UIN_UPDATE_PARENTGUARDIAN_DETAILS,
		// false);
		//
		// if ((boolean) SessionContext.getInstance().getUserContext().getUserMap()
		// .get(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION)) {
		// SessionContext.map().put(RegistrationConstants.UIN_UPDATE_BIOMETRICEXCEPTION,
		// true);
		// } else {
		// SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN,
		// true);
		// }
		// registrationController.showUINUpdateCurrentPage();
		//
		// } else {
		registrationController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
				getPageByAction(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.PREVIOUS));
		// }
	}

	/**
	 * Navigating to next section
	 *
	 * @param event
	 *            the event for navigating to next section
	 */
	@FXML
	private void next(ActionEvent event) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Navigates to next section");

		if (currentPosition != sizeOfCombobox - 1) {
			disableLastCheckBoxSection();
			goToNext();
			return;
		}

		webCameraController.closeWebcam();

		/*
		 * if (isChild() || getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
		 * SessionContext.map().put(RegistrationConstants.
		 * UIN_UPDATE_PARENTGUARDIAN_DETAILS, false); if (!RegistrationConstants.DISABLE
		 * .equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.
		 * FACE_DISABLE_FLAG))) {
		 * 
		 * SessionContext.getInstance().getMapObject().put(RegistrationConstants.
		 * UIN_UPDATE_FACECAPTURE, true); } else {
		 * SessionContext.getInstance().getMapObject().put(RegistrationConstants.
		 * UIN_UPDATE_REGISTRATIONPREVIEW, true);
		 * registrationPreviewController.setUpPreviewContent(); } //
		 * faceCaptureController.checkForException();
		 * registrationController.showUINUpdateCurrentPage(); } else {
		 */
		if (isUserOnboardFlag) {
			userOnboardParentController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getOnboardPageDetails(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.NEXT));
		} else {
			registrationController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getPageByAction(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.NEXT));
		}

		initializeState();
		// faceCaptureController.checkForException();
		// }
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

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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
		duplicateCheckLbl.setText(RegistrationConstants.EMPTY);

		retryBox.setVisible(true);
		biometricBox.setVisible(true);

		bioProgress.setProgress(0);
		qualityText.setText("");

		intializeCaptureCount();

		if (!isFace(currentModality)) {
			setScanButtonVisibility(isAllExceptions(getCheckBoxes(currentSubType, currentModality)), scanBtn);
		} else {
			setScanButtonVisibility(false, scanBtn);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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
	 * Scan Iris
	 * 
	 * @param irisType
	 *            iris type
	 * @param popupStage
	 *            stage
	 * @param thresholdValue
	 *            threshold value
	 *
	 */
	/*
	 * private void scanIris(String irisType, Stage popupStage, int thresholdValue)
	 * throws RegBaseCheckedException {
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Scanning Iris");
	 * 
	 * IrisDetailsDTO detailsDTO = null; Instant start = null; Instant end = null;
	 * 
	 * IrisDetailsDTO irisDetailsDTO;
	 * 
	 * int attempt = 0;
	 * 
	 * List<IrisDetailsDTO> guardianIris = null;
	 * 
	 * if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) { guardianIris
	 * =
	 * getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
	 * .getIrisDetailsDTO(); } else if ((boolean)
	 * SessionContext.map().get(RegistrationConstants.IS_Child)) { guardianIris =
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getIrisDetailsDTO(); }
	 * 
	 * irisDetailsDTO = guardianIris.isEmpty() ? null : guardianIris.get(0); attempt
	 * = irisDetailsDTO != null ? irisDetailsDTO.getNumOfIrisRetry() + 1 : 1;
	 * 
	 * try { detailsDTO = new IrisDetailsDTO(); start = Instant.now(); detailsDTO =
	 * bioService.getIrisImageAsDTO( new RequestDetail(irisType,
	 * getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 2,
	 * String.valueOf(thresholdValue), null), attempt, attempt); end =
	 * Instant.now(); } catch (RegBaseCheckedException | IOException
	 * runtimeException) { LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER,
	 * APPLICATION_NAME, APPLICATION_ID, String.format(
	 * "%s Exception while getting the scanned iris details for user registration: %s caused by %s"
	 * , RegistrationConstants.USER_REG_IRIS_SAVE_EXP,
	 * runtimeException.getMessage(),
	 * ExceptionUtils.getStackTrace(runtimeException)));
	 * generateAlert(RegistrationConstants.ALERT_INFORMATION,
	 * RegistrationUIConstants.NO_DEVICE_FOUND); return; } boolean isDuplicate =
	 * false; if (detailsDTO.isCaptured()) {
	 * 
	 * final List<IrisDetailsDTO> irisToCheck = detailsDTO.getIrises(); isDuplicate
	 * = generateAlert(RegistrationConstants.ALERT_INFORMATION,
	 * RegistrationUIConstants.IRIS_SUCCESS_MSG, () -> { return
	 * validateIrisLocalDedup(irisToCheck); }, scanPopUpViewController);
	 * popupStage.close();
	 * 
	 * captureTimeValue.setText(Duration.between(start,
	 * end).toString().replace("PT", ""));
	 * 
	 * // If Not Duplicate // If Iris is valid if (!isDuplicate &&
	 * validateIrisQulaity(detailsDTO.getIrises().get(0), new
	 * Double(thresholdValue))) {
	 * 
	 * if (!guardianIris.isEmpty()) { guardianIris.clear();
	 * 
	 * } guardianIris.add(detailsDTO.getIrises().get(0));
	 * 
	 * guardianIris.forEach((iris) -> {
	 * 
	 * scanPopUpViewController.getScanImage().setImage(convertBytesToImage(iris.
	 * getIris())); biometricImage.setImage(bioService.isMdmEnabled() ?
	 * convertBytesToImage( bioService.getBioStreamImage(iris.getIrisType(),
	 * iris.getNumOfIrisRetry())) : convertBytesToImage(iris.getIris()));
	 * 
	 * setCapturedValues(bioService.isMdmEnabled() ?
	 * bioService.getBioQualityScores(iris.getIrisType(), iris.getNumOfIrisRetry())
	 * : iris.getQualityScore(), iris.getNumOfIrisRetry(), thresholdValue);
	 * 
	 * continueBtn.setDisable(false);
	 * 
	 * });
	 * 
	 * } else if (isDuplicate) { continueBtn.setDisable(true);
	 * duplicateCheckLbl.setText("Found Duplicate" + " " + irisType); } else {
	 * continueBtn.setDisable(false);
	 * generateAlert(RegistrationConstants.ALERT_INFORMATION,
	 * RegistrationUIConstants.IRIS_SCANNING_ERROR); } }
	 * 
	 * else { generateAlert(RegistrationConstants.ALERT_INFORMATION,
	 * RegistrationUIConstants.IRIS_SCANNING_ERROR); }
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Iris scanning is completed");
	 * 
	 * }
	 * 
	 * private boolean validateIrisLocalDedup(List<IrisDetailsDTO> irises) {
	 * 
	 * AuthenticationValidatorDTO authenticationValidatorDTO = new
	 * AuthenticationValidatorDTO();
	 * authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId()
	 * ); authenticationValidatorDTO.setIrisDetails(irises);
	 * authenticationValidatorDTO.setAuthValidationType("single"); boolean isValid =
	 * authenticationService.authValidator(RegistrationConstants.IRIS,
	 * authenticationValidatorDTO); return isValid;
	 * 
	 * }
	 */

	/**
	 * Scan Fingers
	 * 
	 * @param fingerType
	 *            finger type
	 * @param segmentedFingersPath
	 *            segmented finger path
	 * @param popupStage
	 *            stage
	 * @param thresholdValue
	 *            threshold value
	 */
	/*
	 * private void scanFingers(String fingerType, String[] segmentedFingersPath,
	 * Stage popupStage, int thresholdValue) throws RegBaseCheckedException {
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Scanning Fingerprints started");
	 * 
	 * FingerprintDetailsDTO detailsDTO = null;
	 * 
	 * int attempt = 0; Instant start = null; Instant end = null;
	 * List<FingerprintDetailsDTO> fingerprintDetailsDTOs = null; if
	 * (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
	 * fingerprintDetailsDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
	 * .getFingerprintDetailsDTO(); } else if ((boolean)
	 * SessionContext.map().get(RegistrationConstants.IS_Child)) {
	 * fingerprintDetailsDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getFingerprintDetailsDTO(); }
	 * 
	 * if (fingerprintDetailsDTOs != null) {
	 * 
	 * for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
	 * if (fingerprintDetailsDTO.getFingerType() != null &&
	 * fingerprintDetailsDTO.getSegmentedFingerprints() != null &&
	 * fingerprintDetailsDTO.getFingerType().equals(fingerType)) { attempt =
	 * fingerprintDetailsDTO.getNumRetry();
	 * 
	 * break; } }
	 * 
	 * }
	 * 
	 * attempt = attempt != 0 ? attempt + 1 : 1;
	 * 
	 * // TODO to be passed through MDS capture request List<String> exceptions =
	 * null; // getExceptionBioAttributesByBioType(biometricType.getText());
	 * 
	 * try { start = Instant.now();
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Capturing Fingerprints for attempt : " + attempt);
	 * detailsDTO = bioService.getFingerPrintImageAsDTO(new
	 * RequestDetail(fingerType,
	 * getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 1,
	 * String.valueOf(thresholdValue), fingerException), attempt);
	 * 
	 * end = Instant.now(); streamer.stop();
	 * bioService.segmentFingerPrintImage(detailsDTO, segmentedFingersPath,
	 * fingerType); } catch (Exception exception) { streamer.stop();
	 * LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, String.
	 * format("%s Exception while getting the scanned finger details for user registration: %s "
	 * , exception.getMessage(), ExceptionUtils.getStackTrace(exception))); return;
	 * }
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Validating Captured FingerPrints"); if
	 * (detailsDTO.isCaptured() && bioService.isValidFingerPrints(detailsDTO, true))
	 * {
	 * 
	 * boolean isNotMatched = true;
	 * 
	 * if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))
	 * { LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID,
	 * "Verifying Local Deduplication check of captured fingerprints against Operator Biometrics"
	 * ); isNotMatched =
	 * bioService.validateBioDeDup(detailsDTO.getSegmentedFingerprints()); }
	 * popupStage.close(); if (!isNotMatched) {
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID,
	 * "Failure :Local Deduplication found on captured fingerprints against Operator Biometrics"
	 * ); generateAlert(RegistrationConstants.ALERT_INFORMATION,
	 * RegistrationUIConstants.FINGERPRINT_DUPLICATION_ALERT); return; }
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Updating captured fingerprints in session");
	 * updateFingerBiometricsSession(fingerprintDetailsDTOs, detailsDTO);
	 * 
	 * continueBtn.setDisable(false);
	 * 
	 * captureTimeValue.setText(Duration.between(start,
	 * end).toString().replace("PT", "")); if
	 * (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
	 * rightSlapCount = detailsDTO.getNumRetry(); retries = rightSlapCount; } else
	 * if (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
	 * 
	 * leftSlapCount = detailsDTO.getNumRetry(); retries = leftSlapCount;
	 * 
	 * } else { thumbCount = detailsDTO.getNumRetry(); retries = thumbCount; }
	 * streamer.setBioStreamImages(null, detailsDTO.getFingerType(),
	 * detailsDTO.getNumRetry());
	 * 
	 * if (!bioService.isMdmEnabled())
	 * scanPopUpViewController.getScanImage().setImage(convertBytesToImage(
	 * detailsDTO.getFingerPrint())); else {
	 * detailsDTO.setFingerPrint(streamer.imageBytes); }
	 * biometricImage.setImage(bioService.isMdmEnabled() ? convertBytesToImage(
	 * bioService.getBioStreamImage(detailsDTO.getFingerType(),
	 * detailsDTO.getNumRetry())) :
	 * convertBytesToImage(detailsDTO.getFingerPrint()));
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Updating progress Bar,Text and attempts Box in UI");
	 * setCapturedValues(bioService.isMdmEnabled() ?
	 * bioService.getBioQualityScores(detailsDTO.getFingerType(),
	 * detailsDTO.getNumRetry()) : detailsDTO.getQualityScore(),
	 * detailsDTO.getNumRetry(), thresholdValue);
	 * 
	 * }
	 * 
	 * else { LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Validating Captured FingerPrints Failed");
	 * generateAlert(RegistrationConstants.ALERT_INFORMATION,
	 * RegistrationUIConstants.FP_DEVICE_ERROR); }
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Fingerprints Scanning is completed");
	 * 
	 * }
	 * 
	 * private void updateFingerBiometricsSession(List<FingerprintDetailsDTO>
	 * fingerprintDetailsDTOs, FingerprintDetailsDTO detailsDTO) {
	 * FingerprintDetailsDTO fingerprintDetails = null; for (FingerprintDetailsDTO
	 * fingerprintDetailsDTO : fingerprintDetailsDTOs) { if
	 * (fingerprintDetailsDTO.getFingerType() != null
	 * 
	 * && fingerprintDetailsDTO.getFingerType().equals(detailsDTO.getFingerType()))
	 * {
	 * 
	 * fingerprintDetails = fingerprintDetailsDTO; break; } }
	 * 
	 * if (fingerprintDetails != null) {
	 * fingerprintDetailsDTOs.remove(fingerprintDetails); }
	 * fingerprintDetailsDTOs.add(detailsDTO);
	 * 
	 * }
	 */

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

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating Quality and threshold values of biometrics");

		final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
			public void handle(final MouseEvent mouseEvent) {

				LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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

					LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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

		clearAllBiometrics();
	}

	private void clearUiElements() {

		retryBox.setVisible(false);
		biometricBox.setVisible(false);

	}

	/**
	 * Validates QualityScore.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @param handThreshold
	 *            the hand threshold
	 * @return boolean
	 */
	/*
	 * private Boolean validateFingerPrintQulaity(FingerprintDetailsDTO
	 * fingerprintDetailsDTO, Double handThreshold) {
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Validating the quality score of the captured fingers");
	 * double qualityScore = bioService.isMdmEnabled() ?
	 * bioService.getHighQualityScoreByBioType(fingerprintDetailsDTO.getFingerType()
	 * , fingerprintDetailsDTO.getQualityScore()) :
	 * fingerprintDetailsDTO.getQualityScore();
	 * 
	 * return qualityScore >= handThreshold || (qualityScore < handThreshold) &&
	 * fingerprintDetailsDTO.getNumRetry() == Integer
	 * .parseInt(getValueFromApplicationContext(RegistrationConstants.
	 * FINGERPRINT_RETRIES_COUNT)) || fingerprintDetailsDTO.isForceCaptured(); }
	 */

	/**
	 * Fingerdeduplication check.
	 *
	 * @param fingerprintDetailsDTOs
	 *            the fingerprint details DT os
	 * @return true, if successful
	 */
	/*
	 * private boolean fingerdeduplicationCheck(List<FingerprintDetailsDTO>
	 * fingerprintDetailsDTOs) {
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Validating the dedupecheck of the captured fingers");
	 * 
	 * List<FingerprintDetailsDTO> segmentedFingerprintDetailsDTOs = new
	 * ArrayList<>();
	 * 
	 * boolean isValid = false;
	 * 
	 * for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
	 * for (FingerprintDetailsDTO segmentedFingerprintDetailsDTO :
	 * fingerprintDetailsDTO .getSegmentedFingerprints()) {
	 * segmentedFingerprintDetailsDTOs.add(segmentedFingerprintDetailsDTO); } } if
	 * (!validateFingerprint(segmentedFingerprintDetailsDTOs)) { isValid = true; }
	 * else { duplicateCheckLbl.setText(RegistrationUIConstants.
	 * FINGERPRINT_DUPLICATION_ALERT); }
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Validated the dedupecheck of the captured fingers");
	 * 
	 * return isValid; }
	 */

	/**
	 * Validates QualityScore.
	 *
	 * @param irisDetailsDTO
	 *            the iris details DTO
	 * @param irisThreshold
	 *            the iris threshold
	 * @return boolean
	 */
	/*
	 * private boolean validateIrisQulaity(IrisDetailsDTO irisDetailsDTO, Double
	 * irisThreshold) {
	 * 
	 * LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
	 * APPLICATION_ID, "Validating the quality score of the captured iris");
	 * 
	 * double qualityScore = bioService.isMdmEnabled() ?
	 * bioService.getHighQualityScoreByBioType(irisDetailsDTO.getIrisType(),
	 * irisDetailsDTO.getQualityScore()) : irisDetailsDTO.getQualityScore();
	 * 
	 * return qualityScore >= irisThreshold || (Double.compare(qualityScore,
	 * irisThreshold) < 0 && irisDetailsDTO.getNumOfIrisRetry() == Integer
	 * .parseInt(getValueFromApplicationContext(RegistrationConstants.
	 * IRIS_RETRY_COUNT)) || irisDetailsDTO.isForceCaptured()); }
	 */

	/**
	 * Clear Biometric data
	 */
	public void clearCapturedBioData() {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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
		duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
		clearCaptureData();
		biometricBox.setVisible(false);
		retryBox.setVisible(false);
		bioValue = RegistrationUIConstants.SELECT;

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Cleared the captured biometric data");

	}

	/**
	 * Exception fingers count.
	 */
	/*
	 * private Map<String, Integer> exceptionFingersCount(int leftSlapCount, int
	 * rightSlapCount, int thumbCount, int irisCount) {
	 * leftSlapexceptionList.clear(); rightSlapexceptionList.clear();
	 * thumbsexceptionList.clear(); Map<String, Integer> exceptionCountMap = new
	 * HashMap<>(); List<BiometricExceptionDTO> biometricExceptionDTOs; if
	 * ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
	 * biometricExceptionDTOs =
	 * getBiometricDTOFromSession().getOperatorBiometricDTO().
	 * getBiometricExceptionDTO(); } else if
	 * (getRegistrationDTOFromSession().isUpdateUINNonBiometric() || (boolean)
	 * SessionContext.map().get(RegistrationConstants.IS_Child)) {
	 * biometricExceptionDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getBiometricExceptionDTO(); } else { biometricExceptionDTOs =
	 * getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
	 * .getBiometricExceptionDTO(); } for (BiometricExceptionDTO
	 * biometricExceptionDTO : biometricExceptionDTOs) {
	 * 
	 * if
	 * ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * LEFT.toLowerCase()) && biometricExceptionDTO.isMarkedAsException()) &&
	 * !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * THUMB) &&
	 * !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * EYE)) { getExceptionIdentifier(leftSlapexceptionList,
	 * biometricExceptionDTO.getMissingBiometric()); leftSlapCount++; } if
	 * ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * RIGHT.toLowerCase()) && biometricExceptionDTO.isMarkedAsException()) &&
	 * !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * THUMB) &&
	 * !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * EYE)) { getExceptionIdentifier(rightSlapexceptionList,
	 * biometricExceptionDTO.getMissingBiometric()); rightSlapCount++; } if
	 * ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * THUMB) && biometricExceptionDTO.isMarkedAsException())) {
	 * getExceptionIdentifier(thumbsexceptionList,
	 * biometricExceptionDTO.getMissingBiometric()); thumbCount++; } if
	 * ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.
	 * EYE) && biometricExceptionDTO.isMarkedAsException())) { irisCount++; } }
	 * exceptionCountMap.put(RegistrationConstants.LEFTSLAPCOUNT, leftSlapCount);
	 * exceptionCountMap.put(RegistrationConstants.RIGHTSLAPCOUNT, rightSlapCount);
	 * exceptionCountMap.put(RegistrationConstants.THUMBCOUNT, thumbCount);
	 * exceptionCountMap.put(RegistrationConstants.EXCEPTIONCOUNT, leftSlapCount +
	 * rightSlapCount + thumbCount + irisCount);
	 * 
	 * return exceptionCountMap; }
	 */

	/**
	 * Manage biometrics list based on exceptions.
	 */
	/*
	 * public void manageBiometricsListBasedOnExceptions() {
	 * 
	 * if
	 * (getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO(
	 * ) .getBiometricExceptionDTO() != null ||
	 * !getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO(
	 * ) .getBiometricExceptionDTO().isEmpty()) { int leftSlapCount = 0; int
	 * rightSlapCount = 0; int thumbCount = 0; int irisCount = 0;
	 * biometricTypecombo.getItems().clear(); populateBiometrics();
	 * 
	 * Map<String, Integer> exceptionCount = exceptionFingersCount(leftSlapCount,
	 * rightSlapCount, thumbCount, irisCount); int excepCount =
	 * exceptionCount.get(RegistrationConstants.EXCEPTIONCOUNT);
	 * 
	 * if ((RegistrationConstants.DISABLE.equalsIgnoreCase(
	 * getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG
	 * )) && excepCount == 2) || (RegistrationConstants.DISABLE
	 * .equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.
	 * IRIS_DISABLE_FLAG)) && excepCount == 10) || excepCount == 12) { bioValue =
	 * RegistrationUIConstants.SELECT; biometricBox.setVisible(true);
	 * bioProgress.setProgress(1); biometricTypecombo.setVisible(false);
	 * thresholdBox.setVisible(false);
	 * scanBtn.setText(RegistrationUIConstants.TAKE_PHOTO);
	 * duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
	 * retryBox.setVisible(false); if
	 * (getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO(
	 * ).getFace() .getFace() == null) { scanBtn.setDisable(false);
	 * continueBtn.setDisable(true); updateBiometric(RegistrationUIConstants.PHOTO,
	 * RegistrationConstants.IMAGE_PATH, "",
	 * String.valueOf(RegistrationConstants.PARAM_ZERO)); } else {
	 * continueBtn.setDisable(false); } } else {
	 * biometricTypecombo.setVisible(true); if
	 * (bioValue.equalsIgnoreCase(RegistrationUIConstants.SELECT)) {
	 * biometricBox.setVisible(false); } else { biometricBox.setVisible(true); }
	 * thresholdBox.setVisible(true); scanBtn.setText(RegistrationUIConstants.SCAN);
	 * duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
	 * photoAlert.setVisible(false); if
	 * (exceptionCount.get(RegistrationConstants.LEFTSLAPCOUNT) == 4) {
	 * modifyBiometricType(RegistrationUIConstants.LEFT_SLAP); } if
	 * (exceptionCount.get(RegistrationConstants.RIGHTSLAPCOUNT) == 4) {
	 * modifyBiometricType(RegistrationUIConstants.RIGHT_SLAP); } if
	 * (exceptionCount.get(RegistrationConstants.THUMBCOUNT) == 2) {
	 * modifyBiometricType(RegistrationUIConstants.THUMBS); } if
	 * (anyIrisException(RegistrationConstants.LEFT)) {
	 * modifyBiometricType(RegistrationUIConstants.LEFT_IRIS); } if
	 * (anyIrisException(RegistrationConstants.RIGHT)) {
	 * modifyBiometricType(RegistrationUIConstants.RIGHT_IRIS); } List<String>
	 * bioList = new ArrayList<>(); biometricTypecombo.getItems().forEach(bio ->
	 * bioList.add(bio.getName())); if (!bioList.contains(bioValue)) { bioValue =
	 * RegistrationUIConstants.SELECT; clearCapturedBioData(); } }
	 * 
	 * } biometricTypecombo.getSelectionModel().clearSelection();
	 * biometricTypecombo.setPromptText(bioValue); if (bioProgress.getProgress() !=
	 * 0) { if
	 * (!scanBtn.getText().equalsIgnoreCase(RegistrationUIConstants.TAKE_PHOTO)) {
	 * scanBtn.setDisable(true); } continueBtn.setDisable(false); } else {
	 * continueBtn.setDisable(true); } }
	 */

	/**
	 * Modifying the combobox details
	 */
	private void modifyBiometricType(String biometricType) {
		List<BiometricAttributeDto> biometricAttributeDtos = biometricTypecombo.getItems();
		List<BiometricAttributeDto> bio = new ArrayList<>();
		bio.addAll(biometricAttributeDtos);
		for (BiometricAttributeDto biometricAttributeDto2 : biometricAttributeDtos) {
			if (biometricAttributeDto2.getName().equalsIgnoreCase(biometricType)) {
				bio.remove(biometricAttributeDto2);
				break;
			}

		}
		biometricTypecombo.getItems().clear();
		biometricTypecombo.getItems().addAll(bio);
	}

	/**
	 * Fetching the combobox details
	 */
	@SuppressWarnings("unchecked")
	private <T> void renderBiometrics(ComboBox<Entry<String, String>> biometricTypecombo) {
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RENDER_COMBOBOXES", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Rendering of comboboxes started");

		try {
			StringConverter<T> uiRenderForComboBox = fxUtils.getStringConverterForComboBox();

			biometricTypecombo.setConverter((StringConverter<Entry<String, String>>) uiRenderForComboBox);
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.REGISTRATION_CONTROLLER,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException), runtimeException);
		}
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RENDER_COMBOBOXES", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Rendering of comboboxes ended");
	}

	private void populateBiometrics() {
		try {
			biometricTypecombo.getItems().addAll(masterSync.getBiometricType(ApplicationContext.applicationLanguage()));
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("REGISTRATION - LOADING_BIOMETRICS - RENDER_COMBOBOXES", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	/*
	 * private boolean validateFingerprint(List<FingerprintDetailsDTO>
	 * fingerprintDetailsDTOs) { AuthenticationValidatorDTO
	 * authenticationValidatorDTO = new AuthenticationValidatorDTO();
	 * authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId()
	 * ); authenticationValidatorDTO.setFingerPrintDetails(fingerprintDetailsDTOs);
	 * authenticationValidatorDTO.setAuthValidationType("multiple"); boolean isValid
	 * = authenticationService.authValidator("Fingerprint",
	 * authenticationValidatorDTO); return isValid;
	 * 
	 * }
	 * 
	 * private void updateByAttempt(String bioType, int attempt) {
	 * 
	 * double qualityScoreValue = bioService.getBioQualityScores(bioType, attempt);
	 * String qualityScore = getQualityScore(qualityScoreValue);
	 * 
	 * if (qualityScore != null) { Image streamImage =
	 * convertBytesToImage(bioService.getBioStreamImage(bioType, attempt)); // Set
	 * Stream image biometricImage.setImage(streamImage);
	 * 
	 * // Quality Label qualityText.setText(qualityScore);
	 * 
	 * // Progress BAr bioProgress.setProgress(qualityScoreValue / 100);
	 * 
	 * // Progress Bar Quality Score this.qualityScore.setText(String.valueOf((int)
	 * qualityScoreValue).concat(RegistrationConstants.PERCENTAGE));
	 * 
	 * }
	 * 
	 * }
	 */

	// public void addBioStreamImage(byte[] streamImageBytes) {
	//
	// STREAM_IMAGES.put(String.format("%s_%s", currentSubType, currentModality),
	// streamImageBytes);
	// }

	public void addBioStreamImage(String subType, String modality, int attempt, Image streamImage) {

		STREAM_IMAGES.put(String.format("%s_%s_%s", subType, modality, attempt), streamImage);
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

	/*
	 * private boolean isBiometricCaptured(String bioAttribute) { boolean isCaptured
	 * = false; if (getRegistrationDTOFromSession().getBiometric(currentSubType,
	 * bioAttribute) != null ||
	 * getRegistrationDTOFromSession().isBiometricExceptionAvailable(currentSubType,
	 * bioAttribute)) { isCaptured = true; } return isCaptured; }
	 */

	private boolean isBiometricsCaptured(String subType, List<String> bioAttributes, int thresholdScore,
			boolean considerExceptionAsCaptured) {
		// if no bio attributes then it is not configured to be captured
		if (bioAttributes == null || bioAttributes.isEmpty())
			return false;

		boolean isCaptured = false, isForceCaptured = false;
		int qualityScore = 0, exceptionBioCount = 0;

		for (String bioAttribute : bioAttributes) {

			// // TODO Change bio Attribute names of proxy
			// if (bioService.isMdmEnabled()) {
			// bioAttribute =
			// io.mosip.registration.mdm.dto.Biometric.getSpecConstantByAttributeName(bioAttribute);
			// }

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

	/**
	 * Gets the bio type.
	 *
	 * @param bioType
	 *            the bio type
	 * @return the bio type
	 */
	/*
	 * private String getBioType(String bioType) {
	 * 
	 * if (bioType.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE) ||
	 * bioType.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
	 * String[] returnEye = bioType.split("_"); bioType = returnEye[0]; } return
	 * bioType;
	 * 
	 * }
	 */

	public void addBioScores(String subType, String modality, String attempt, double qualityScore) {

		BIO_SCORES.put(String.format("%s_%s_%s", subType, modality, attempt), qualityScore);
	}

	public double getBioScores(String subType, String modality, int attempt) {

		double qualityScore = 0.0;
		try {
			qualityScore = BIO_SCORES.get(String.format("%s_%s_%s", subType, modality, attempt));
		} catch (NullPointerException nullPointerException) {
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
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
		if(userBiometrics.isEmpty())
			return false;
		
		userBiometrics.forEach(userBiometric -> {
			String userId = userBiometric.getUserBiometricId().getUsrId();
			gallery.computeIfAbsent(userId, k -> new ArrayList<BIR>()).add(buildBir(userBiometric.getBioIsoImage(), biometricType));
		});
					
		List<BIR> sample = new ArrayList<>(biometrics.size());
		biometrics.forEach( biometricDto -> {
			sample.add(buildBir(biometricDto.getAttributeISO(), biometricType));
		});
		
		try {
			Map<String, Boolean> result = bioAPIFactory.getBioProvider(biometricType, BiometricFunction.MATCH).
					identify(sample, gallery, biometricType, null);
			return result.entrySet().stream().anyMatch(e -> e.getValue() == true);
		} catch(BiometricException e) {
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Failed to dedupe >> " + ExceptionUtils.getStackTrace(e));
		}
		return false;
	}
	
	private BIR buildBir(byte[] biometricImageISO, BiometricType modality) {
		return new BIRBuilder().withBdb(biometricImageISO)
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(new RegistryIDType())
						.withType(Collections.singletonList(SingleType.fromValue(modality.value())))
						.withPurpose(PurposeType.IDENTIFY)
						.build())
				.build();
	}

}