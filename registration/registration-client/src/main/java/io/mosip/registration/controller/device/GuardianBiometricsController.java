package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.BiometricExceptionDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.dto.biometric.IrisDetailsDTO;
import io.mosip.registration.dto.mastersync.BiometricAttributeDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.packetmananger.dto.BiometricsDto;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.security.AuthenticationService;
import io.mosip.registration.service.sync.MasterSyncService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
public class GuardianBiometricsController extends BaseController implements Initializable {

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
	@Autowired
	private FaceCaptureController faceCaptureController;

	/** The finger print capture service impl. */
	@Autowired
	private AuthenticationService authenticationService;

	/** The iris facade. */
	@Autowired
	private BioService bioService;

	@Autowired
	private Transliteration<String> transliteration;

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

	private List<String> fingerException;

	private String bioType;

	private static Map<String, Image> STREAM_IMAGES = new HashMap<String, Image>();

	/** The face capture controller. */
	@Autowired
	private IrisCaptureController irisCaptureController;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Loading of Guardian Biometric screen started");
		comboBoxMap = new HashMap<>();
		checkBoxMap = new HashMap<>();
		fxUtils = FXUtils.getInstance();
		applicationLabelBundle = applicationContext.getApplicationLanguageBundle();
		HashMap<Entry<String, String>, HashMap<String, List<List<String>>>> mapToProcess = getconfigureAndNonConfiguredBioAttributes(
				Arrays.asList(
						getValue(RegistrationConstants.FINGERPRINT_SLAB_LEFT,
								RegistrationConstants.leftHandUiAttributes),
						getValue(RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
								RegistrationConstants.rightHandUiAttributes),
						getValue(RegistrationConstants.FINGERPRINT_SLAB_THUMBS,
								RegistrationConstants.twoThumbsUiAttributes),
						getValue(RegistrationConstants.IRIS_DOUBLE, RegistrationConstants.eyesUiAttributes),
						getValue(RegistrationConstants.FACE, RegistrationConstants.faceUiAttributes)));

		for (Entry<Entry<String, String>, HashMap<String, List<List<String>>>> subType : mapToProcess.entrySet()) {
			VBox vb = new VBox();
			vb.setSpacing(10);
			vb.setId(subType.getKey() + "vbox");
			Label label = new Label(subType.getKey().getValue());
			label.getStyleClass().add("paneHeader");
			ComboBox<Entry<String, String>> comboBox = new ComboBox<>();
			comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
				displayBiometric(newValue.getKey());
			});
			renderBiometrics(comboBox);
			comboBox.getStyleClass().add("demographicCombobox");
			comboBox.setId(subType.getKey() + "combobox");

			vb.getChildren().addAll(label, comboBox);
			ContentHeader.add(vb, 1, 1);
			vb.setVisible(false);
			vb.setManaged(false);
			comboBoxMap.put(subType.getKey().getKey(), vb);
			HashMap<String, VBox> subMap = new HashMap<>();
			for (Entry<String, List<List<String>>> biometric : subType.getValue().entrySet()) {
				List<List<String>> listOfCheckBoxes = biometric.getValue();
				if (!listOfCheckBoxes.get(0).isEmpty()) {

					comboBox.getItems().add(new SimpleEntry<String, String>(biometric.getKey(),
							applicationLabelBundle.getString(biometric.getKey())));
					
					if(listOfCheckBoxes.get(0).get(0).equals("face")) {
						subMap.put(biometric.getKey(), null);
						checkBoxMap.put(subType.getKey().getKey(), subMap);
					}
					else {

					//if (!listOfCheckBoxes.get(0).get(0).equals("face")) {
						VBox vboxForCheckBox = new VBox();
						vboxForCheckBox.setSpacing(5);

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

									for (Node exceptionCheckBox : vboxForCheckBox.getChildren()) {
										getRegistrationDTOFromSession().removeBiometric(currentSubType,
												exceptionCheckBox.getId());
									}
									if (checkBox.isSelected()) {
										getRegistrationDTOFromSession().addBiometricException(currentSubType,
												checkBox.getId(), "Temporary", "Temporary");
									} else {
										getRegistrationDTOFromSession().removeBiometricException(currentSubType,
												checkBox.getId());
									}
									refreshContinueButton();

								});

							}
						}
						checkBoxPane.add(vboxForCheckBox, 0, 0);
						vboxForCheckBox.setVisible(false);
						vboxForCheckBox.setManaged(false);
						subMap.put(biometric.getKey(), vboxForCheckBox);
						checkBoxMap.put(subType.getKey().getKey(), subMap);		

					}
				}

			}

		}

		sizeOfCombobox = comboBoxMap.size();
		if (sizeOfCombobox > 0) {
			currentPosition = 0;
			previousPosition = 0;
		}

		if (null != findComboBox()) {
			findComboBox().setVisible(true);
			findComboBox().setManaged(true);
		}

		//
		// // TODO replace the value from the comboMap
		// currentSubType = RegistrationConstants.INDIVIDUAL;
		//
		// if (getRegistrationDTOFromSession() != null &&
		// getRegistrationDTOFromSession().getSelectionListDTO() != null) {
		// registrationNavlabel.setText(ApplicationContext.applicationLanguageBundle()
		// .getString(RegistrationConstants.UIN_UPDATE_UINUPDATENAVLBL));
		// }
		//
		// if (getRegistrationDTOFromSession() != null
		// &&
		// getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
		// != null
		// &&
		// getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
		// .equals(RegistrationConstants.PACKET_TYPE_LOST)) {
		// registrationNavlabel.setText(
		// ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));
		// }
		// intializeCaptureCount();
		// fxUtils.setTransliteration(transliteration);
		// bioValue = RegistrationUIConstants.SELECT;
		biometricBox.setVisible(false);
		retryBox.setVisible(false);
		continueBtn.setDisable(true);
		// populateBiometrics();
		//
		// LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME,
		// APPLICATION_ID,
		// "Loading of Guardian Biometric screen ended");
		//
		// biometricTypecombo.valueProperty().addListener(new
		// ChangeListener<BiometricAttributeDto>() {
		//
		// @Override
		// public void changed(ObservableValue<? extends BiometricAttributeDto> arg0,
		// BiometricAttributeDto previousValue, BiometricAttributeDto currentValue) {
		// if (null != previousValue && null != currentValue
		// && !previousValue.getName().equalsIgnoreCase(currentValue.getName())) {
		// continueBtn.setDisable(true);
		//
		// getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
		// .getFingerprintDetailsDTO().clear();
		// getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
		// .getFingerprintDetailsDTO().clear();
		//
		// getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO().getIrisDetailsDTO()
		// .clear();
		// getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().getIrisDetailsDTO()
		// .clear();
		// }
		//
		// }
		// });
	}

	private VBox findComboBox() {
		return comboBoxMap.get(getListOfBiometricSubTypess().get(currentPosition));
	}

	private void goToNext() {
		if (currentPosition + 1 < sizeOfCombobox) {
			findComboBox().setVisible(false);
			findComboBox().setManaged(false);
			previousPosition = currentPosition;
			currentPosition++;
			findComboBox().setVisible(true);
			findComboBox().setManaged(true);

			currentSubType = getListOfBiometricSubTypess().get(currentPosition);
			refreshContinueButton();

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

			currentSubType = getListOfBiometricSubTypess().get(currentPosition);
			refreshContinueButton();

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

		disableLastCheckBoxSection();
		this.currentModality = modality;
		enableCurrentCheckBoxSection();

		// TODO Get Current modalities bio attributes using sub type and modality
		List<String> currentModalityBioAttributes = null;

		if (currentModalityBioAttributes != null && !currentModalityBioAttributes.isEmpty()) {
			List<BiometricsDto> biometricsDtos = getRegistrationDTOFromSession().getBiometric(currentSubType,
					currentModalityBioAttributes);

			loadBiometricsUIElements(biometricsDtos, currentSubType);
		}
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
			updateBiometric(modality, RegistrationConstants.RIGHT_IRIS_IMG_PATH, RegistrationConstants.IRIS_THRESHOLD,
					RegistrationConstants.IRIS_RETRY_COUNT);
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FACE)) {
			updateBiometric(modality, RegistrationConstants.LEFT_IRIS_IMG_PATH, RegistrationConstants.IRIS_THRESHOLD,
					RegistrationConstants.IRIS_RETRY_COUNT);
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

	private void enableCurrentCheckBoxSection() {

		if (!this.currentModality.toUpperCase().contains("FACE")) {
			checkBoxMap.get(getListOfBiometricSubTypess().get(currentPosition)).get(this.currentModality)
					.setVisible(true);
			checkBoxMap.get(getListOfBiometricSubTypess().get(currentPosition)).get(this.currentModality)
					.setManaged(true);
		}
	}

	private void disableLastCheckBoxSection() {
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				this.currentModality + " currentPosition >> " + currentPosition + " checkBoxMap >>> " + new JSONObject(checkBoxMap));
		if (currentPosition != -1) {
			if (this.currentModality != null && !this.currentModality.toUpperCase().contains("FACE")) {
				checkBoxMap.get(getListOfBiometricSubTypess().get(currentPosition)).get(this.currentModality)
						.setVisible(false);
				checkBoxMap.get(getListOfBiometricSubTypess().get(currentPosition)).get(this.currentModality)
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
			streamer.startStream(currentModality, scanPopUpViewController.getScanImage(), biometricImage);
		}
		// if (scanBtn.getText().equalsIgnoreCase(RegistrationUIConstants.TAKE_PHOTO)) {
		// faceCaptureController.openWebCamWindow(RegistrationConstants.GUARDIAN_IMAGE);
		// } else {
		// String headerText = "";
		// String bioType = "";
		// if
		// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.RIGHT_SLAP))
		// {
		// headerText = RegistrationUIConstants.FINGERPRINT;
		// bioType = RegistrationConstants.FINGERPRINT_SLAB_RIGHT;
		// fingerException = rightSlapexceptionList;
		//
		// } else if
		// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.LEFT_SLAP))
		// {
		// headerText = RegistrationUIConstants.FINGERPRINT;
		// bioType = RegistrationConstants.FINGERPRINT_SLAB_LEFT;
		// fingerException = leftSlapexceptionList;
		// } else if
		// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.THUMBS)) {
		// SessionContext.map().put("CAPTURE_EXCEPTION", thumbsexceptionList);
		// headerText = RegistrationUIConstants.FINGERPRINT;
		// bioType = RegistrationConstants.FINGERPRINT_SLAB_THUMBS;
		// fingerException = thumbsexceptionList;
		// } else if
		// (biometricType.getText().contains(RegistrationConstants.IRIS_LOWERCASE)) {
		// headerText = RegistrationUIConstants.IRIS_SCAN;
		// bioType = "IRIS_DOUBLE";
		// }
		//
		// scanPopUpViewController.init(this, headerText);
		// if (bioService.isMdmEnabled())
		// streamer.startStream(currentModality, scanPopUpViewController.getScanImage(),
		// biometricImage);
		// }

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scan popup closed and captured biometrics");

	}

	private List<String> getExceptionBioAttributesByBioType(String subType, String bioType) {

		// Exception fingers
		List<String> exceptionBioAttributes = null;

		// Get Non configured bio attributes Using Ui Schema
		// List<String> nonConfigBioAttributesByBioType =
		// getNonConfigBioAttributes(currentSubType,
		// Biometric.getDefaultAttributes(bioType));
		//
		// if (nonConfigBioAttributesByBioType != null &&
		// !nonConfigBioAttributesByBioType.isEmpty()) {
		// exceptionBioAttributes = exceptionBioAttributes != null ?
		// exceptionBioAttributes : new LinkedList<String>();
		//
		// exceptionBioAttributes.addAll(nonConfigBioAttributesByBioType);
		//
		// }

		// Get Operator selected Exception biometric attributes
		List<String> selectedExceptionBioAttributes = getSelectedExceptionsByBioType(subType, bioType);

		if (selectedExceptionBioAttributes != null && !selectedExceptionBioAttributes.isEmpty()) {
			exceptionBioAttributes = exceptionBioAttributes != null ? exceptionBioAttributes : new LinkedList<String>();

			exceptionBioAttributes.addAll(selectedExceptionBioAttributes);

		}

		return exceptionBioAttributes;
	}

	private List<String> getSelectedExceptionsByBioType(String subType, String modality) {
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"getSelectedExceptionsByBioType : " +subType + " modality : " + modality);
		
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"checkBoxMap >>>>>>>>>>>>>>>>  : " + new JSONObject(checkBoxMap).toString());
		
		List<String> selectedExceptions = null;
		// TODO Get Grid pane where check boxes created

		// TODO Get List of check boxes using the grid pane
		List<Node> exceptionCheckBoxes = null;

		if (checkBoxMap.get(subType).get(modality) != null) {
			exceptionCheckBoxes = checkBoxMap.get(subType).get(modality).getChildren();
		}

		if (exceptionCheckBoxes != null && !exceptionCheckBoxes.isEmpty()) {
			for (Node checkBoxx : exceptionCheckBoxes) {
				CheckBox checkBox = (CheckBox) checkBoxx;
				if (checkBox.isSelected()) {

					selectedExceptions = selectedExceptions != null ? selectedExceptions : new LinkedList<String>();
					String exceptionName = RegistrationConstants.uIToMDSExceptionMap.get(checkBox.getId());
					selectedExceptions.add(exceptionName != null ? exceptionName : checkBox.getId());
				}
			}
		}

		return selectedExceptions;
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
			//
			// if
			// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.RIGHT_SLAP))
			// {
			// scanFingers(RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
			// RegistrationConstants.RIGHTHAND_SEGMNTD_DUPLICATE_FILE_PATHS, popupStage,
			// Integer.parseInt(
			// getValueFromApplicationContext(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD)));
			// } else if
			// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.LEFT_SLAP))
			// {
			// scanFingers(RegistrationConstants.FINGERPRINT_SLAB_LEFT,
			// RegistrationConstants.LEFTHAND_SEGMNTD_FILE_PATHS, popupStage,
			// Integer.parseInt(
			// getValueFromApplicationContext(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD)));
			// } else if
			// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.THUMBS)) {
			// scanFingers(RegistrationConstants.FINGERPRINT_SLAB_THUMBS,
			// RegistrationConstants.THUMBS_SEGMNTD_FILE_PATHS, popupStage,
			// Integer.parseInt(
			// getValueFromApplicationContext(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD)));
			// } else if
			// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.RIGHT_IRIS))
			// {
			// scanIris(RegistrationConstants.RIGHT.concat(RegistrationConstants.EYE),
			// popupStage,
			// Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.IRIS_THRESHOLD)));
			// } else if
			// (biometricType.getText().equalsIgnoreCase(RegistrationUIConstants.LEFT_IRIS))
			// {
			// scanIris(RegistrationConstants.LEFT.concat(RegistrationConstants.EYE),
			// popupStage,
			// Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.IRIS_THRESHOLD)));
			// }

			currentSubType = getListOfBiometricSubTypess().get(currentPosition);
			captureBiometrics(currentSubType, currentModality);
			refreshContinueButton();

		} catch (RuntimeException runtimeException) {
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

	private void captureBiometrics(String subType, String modality) {

		// Get Exception attributes
		String[] exceptionBioAttributes = null;
		List<String> exceptionBioAttributesList = getSelectedExceptionsByBioType(subType, modality);
		if (exceptionBioAttributesList != null && !exceptionBioAttributesList.isEmpty()) {
			exceptionBioAttributes = new String[exceptionBioAttributesList.size()];
			exceptionBioAttributes = exceptionBioAttributesList.toArray(exceptionBioAttributes);
		}

		// TODO prepare bio attributes
		String[] bioAttributes = null;

		// Check count
		int count = 1;

		modality = modality.toUpperCase().contains(RegistrationConstants.FACE) ? RegistrationConstants.FACE_FULLFACE
				: modality;
		MDMRequestDto mdmRequestDto = new MDMRequestDto(modality, exceptionBioAttributes, "Registration", "Staging",
				Integer.valueOf(getCaptureTimeOut()), count,
				getThresholdScoreInInt(getThresholdKeyByBioType(modality)));

		try {
			// TODO Get Response from the MDS
			List<BiometricsDto> biometricDTOList = bioService.captureModality(mdmRequestDto);

			// TODO Validate the biometricDTO from the MDS with threshold values
			boolean isValidBiometric = true;
			// if (biometricDTOList != null && !biometricDTOList.isEmpty()) {
			// for (BiometricDTO biometricDTO : biometricDTOList) {
			//
			// if (biometricDTO.isForceCaptured()
			// || biometricDTO.getQualityScore() >=
			// Double.valueOf(getThresholdScore(bioType))) {
			// isValidBiometric = true;
			// } else {
			// isValidBiometric = false;
			// }
			// }
			// }

			// TODO validate local de-dup check
			boolean isMatchedWithLocalBiometrics = false;

			// TODO if threshold values and local-dedup passed save into the registration
			// DTO

			if (isValidBiometric && !isMatchedWithLocalBiometrics) {

				// save to registration DTO
				for (BiometricsDto biometricDTO : biometricDTOList) {

					getRegistrationDTOFromSession().addBiometric(currentSubType, biometricDTO.getBioAttribute(),
							biometricDTO);
				}

				// if all the above check success show alert capture success
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS);

				// using captured response fill the fields like quality score and progress
				// bar,,etc,.. UI
				loadBiometricsUIElements(biometricDTOList, currentSubType);

			} else {

				// if any above checks failed show alert capture failure
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
			}
		} catch (Exception exception) {

			exception.printStackTrace();
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("Exception while capturing biometrics : ", exception.getMessage(),
							exception.getCause() + ExceptionUtils.getStackTrace(exception)));

			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE);
		}

		scanPopUpViewController.getPopupStage().close();

	}

	private void loadBiometricsUIElements(List<BiometricsDto> biometricDTOList, String subType) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating progress Bar,Text and attempts Box in UI");

		setCapturedValues(getAverageQualityScore(biometricDTOList), biometricDTOList.get(0).getNumOfRetries(),
				getThresholdScoreInInt(getThresholdKeyByBioType(currentModality)));

		int retry = biometricDTOList.get(0).getNumOfRetries();

		// TODO get retry configured value
		int maxRetries = 3;

		if (retry < maxRetries) {
			scanBtn.setDisable(false);
		} else {
			scanBtn.setDisable(true);
		}

		addBioStreamImage(subType, currentModality, String.valueOf(retry), streamer.getStreamImage());

		// Get the stream image from Bio ServiceImpl and load it in the image pane
		biometricImage.setImage(getBioStreamImage(currentSubType, currentModality, String.valueOf(retry)));
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

	private void clearCapturesBySubTypeAndBioType(String subType, String bioType) {

		List<String> bioSubTypeList = getBioSubTypeListByBioType(bioType);

		// TODO Merge with anusha code to remove all bio captures of bio sub type
		// TODO pass subType and bioType
	}

	private void clearExceptionBySubTypeAndBioType(String subType, String bioSubType) {

		// TODO Merge with anusha code to remove exception of bio sub type

		// TODO pass subType and bioType
	}

	private List<String> getBioSubTypeListByBioType(String bioType) {

		// TODO Get Constant attributes
		return null;
	}

	private String getThresholdScore(String thresholdKey) {
		/* Get Configued threshold score for bio type */

		return getValueFromApplicationContext(thresholdKey);
	}

	private int getThresholdScoreInInt(String thresholdKey) {
		/* Get Configued threshold score for bio type */

		String thresholdScore = getValueFromApplicationContext(thresholdKey);

		return thresholdScore != null ? Integer.valueOf(thresholdScore) : 0;
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

		if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_PARENTGUARDIAN_DETAILS, false);

			if ((boolean) SessionContext.getInstance().getUserContext().getUserMap()
					.get(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION)) {
				SessionContext.map().put(RegistrationConstants.UIN_UPDATE_BIOMETRICEXCEPTION, true);
			} else {
				SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN, true);
			}
			registrationController.showUINUpdateCurrentPage();

		} else {
			registrationController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getPageByAction(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.PREVIOUS));
		}
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

		if (isChild() || getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_PARENTGUARDIAN_DETAILS, false);
			if (!RegistrationConstants.DISABLE
					.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG))) {

				SessionContext.getInstance().getMapObject().put(RegistrationConstants.UIN_UPDATE_FACECAPTURE, true);
			} else {
				SessionContext.getInstance().getMapObject().put(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW,
						true);
				registrationPreviewController.setUpPreviewContent();
			}
			// faceCaptureController.checkForException();
			registrationController.showUINUpdateCurrentPage();
		} else {
			registrationController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getPageByAction(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.NEXT));
			// faceCaptureController.checkForException();
		}
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
		biometricType.setText(applicationLabelBundle.getString(bioType));
		if (!bioType.equalsIgnoreCase(RegistrationUIConstants.PHOTO)) {
			bioValue = bioType;
			thresholdScoreLabel
					.setText(getQualityScore(Double.parseDouble(getValueFromApplicationContext(biometricThreshold))));
			createQualityBox(retryCount, biometricThreshold);
			qualityScore.setText(RegistrationConstants.HYPHEN);
			attemptSlap.setText(RegistrationConstants.HYPHEN);
			duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
		}
		// getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().getFingerprintDetailsDTO()
		// .clear();
		// getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().getIrisDetailsDTO().clear();
		//
		// getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().setFace(new
		// FaceDetailsDTO());
		intializeCaptureCount();

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
	private void scanIris(String irisType, Stage popupStage, int thresholdValue) throws RegBaseCheckedException {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Scanning Iris");

		IrisDetailsDTO detailsDTO = null;
		Instant start = null;
		Instant end = null;

		IrisDetailsDTO irisDetailsDTO;

		int attempt = 0;

		List<IrisDetailsDTO> guardianIris = null;

		if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			guardianIris = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
					.getIrisDetailsDTO();
		} else if ((boolean) SessionContext.map().get(RegistrationConstants.IS_Child)) {
			guardianIris = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getIrisDetailsDTO();
		}

		irisDetailsDTO = guardianIris.isEmpty() ? null : guardianIris.get(0);
		attempt = irisDetailsDTO != null ? irisDetailsDTO.getNumOfIrisRetry() + 1 : 1;

		try {
			detailsDTO = new IrisDetailsDTO();
			start = Instant.now();
			detailsDTO = bioService.getIrisImageAsDTO(
					new RequestDetail(irisType, getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT),
							2, String.valueOf(thresholdValue), null),
					attempt, attempt);
			end = Instant.now();
		} catch (RegBaseCheckedException | IOException runtimeException) {
			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, String.format(
					"%s Exception while getting the scanned iris details for user registration: %s caused by %s",
					RegistrationConstants.USER_REG_IRIS_SAVE_EXP, runtimeException.getMessage(),
					ExceptionUtils.getStackTrace(runtimeException)));
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.NO_DEVICE_FOUND);
			return;
		}
		boolean isDuplicate = false;
		if (detailsDTO.isCaptured()) {

			final List<IrisDetailsDTO> irisToCheck = detailsDTO.getIrises();
			isDuplicate = generateAlert(RegistrationConstants.ALERT_INFORMATION,
					RegistrationUIConstants.IRIS_SUCCESS_MSG, () -> {
						return validateIrisLocalDedup(irisToCheck);
					}, scanPopUpViewController);
			popupStage.close();

			captureTimeValue.setText(Duration.between(start, end).toString().replace("PT", ""));

			// If Not Duplicate
			// If Iris is valid
			if (!isDuplicate && validateIrisQulaity(detailsDTO.getIrises().get(0), new Double(thresholdValue))) {

				if (!guardianIris.isEmpty()) {
					guardianIris.clear();

				}
				guardianIris.add(detailsDTO.getIrises().get(0));

				guardianIris.forEach((iris) -> {

					scanPopUpViewController.getScanImage().setImage(convertBytesToImage(iris.getIris()));
					biometricImage.setImage(bioService.isMdmEnabled()
							? convertBytesToImage(
									bioService.getBioStreamImage(iris.getIrisType(), iris.getNumOfIrisRetry()))
							: convertBytesToImage(iris.getIris()));

					setCapturedValues(bioService.isMdmEnabled()
							? bioService.getBioQualityScores(iris.getIrisType(), iris.getNumOfIrisRetry())
							: iris.getQualityScore(), iris.getNumOfIrisRetry(), thresholdValue);

					continueBtn.setDisable(false);

				});

			} else if (isDuplicate) {
				continueBtn.setDisable(true);
				duplicateCheckLbl.setText("Found Duplicate" + " " + irisType);
			} else {
				continueBtn.setDisable(false);
				generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.IRIS_SCANNING_ERROR);
			}
		}

		else {
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.IRIS_SCANNING_ERROR);
		}
		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Iris scanning is completed");

	}

	private boolean validateIrisLocalDedup(List<IrisDetailsDTO> irises) {

		AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId());
		authenticationValidatorDTO.setIrisDetails(irises);
		authenticationValidatorDTO.setAuthValidationType("single");
		boolean isValid = authenticationService.authValidator(RegistrationConstants.IRIS, authenticationValidatorDTO);
		if (null != getValueFromApplicationContext("IDENTY_SDK")) {
			isValid = false;
		}
		return isValid;

	}

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
	private void scanFingers(String fingerType, String[] segmentedFingersPath, Stage popupStage, int thresholdValue)
			throws RegBaseCheckedException {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scanning Fingerprints started");

		FingerprintDetailsDTO detailsDTO = null;

		int attempt = 0;
		Instant start = null;
		Instant end = null;
		List<FingerprintDetailsDTO> fingerprintDetailsDTOs = null;
		if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			fingerprintDetailsDTOs = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
					.getFingerprintDetailsDTO();
		} else if ((boolean) SessionContext.map().get(RegistrationConstants.IS_Child)) {
			fingerprintDetailsDTOs = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getFingerprintDetailsDTO();
		}

		if (fingerprintDetailsDTOs != null) {

			for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
				if (fingerprintDetailsDTO.getFingerType() != null
						&& fingerprintDetailsDTO.getSegmentedFingerprints() != null
						&& fingerprintDetailsDTO.getFingerType().equals(fingerType)) {
					attempt = fingerprintDetailsDTO.getNumRetry();

					break;
				}
			}

		}

		attempt = attempt != 0 ? attempt + 1 : 1;

		// TODO to be passed through MDS capture request
		List<String> exceptions = null; // getExceptionBioAttributesByBioType(biometricType.getText());

		try {
			start = Instant.now();

			LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Capturing Fingerprints for attempt : " + attempt);
			detailsDTO = bioService.getFingerPrintImageAsDTO(new RequestDetail(fingerType,
					getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 1,
					String.valueOf(thresholdValue), fingerException), attempt);

			end = Instant.now();
			streamer.stop();
			bioService.segmentFingerPrintImage(detailsDTO, segmentedFingersPath, fingerType);
		} catch (Exception exception) {
			streamer.stop();
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s Exception while getting the scanned finger details for user registration: %s ",
							exception.getMessage(), ExceptionUtils.getStackTrace(exception)));
			return;
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Validating Captured FingerPrints");
		if (detailsDTO.isCaptured() && bioService.isValidFingerPrints(detailsDTO, true)) {

			boolean isNotMatched = true;

			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Verifying Local Deduplication check of captured fingerprints against Operator Biometrics");
				isNotMatched = bioService.validateBioDeDup(detailsDTO.getSegmentedFingerprints());
			}
			popupStage.close();
			if (!isNotMatched) {

				LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Failure :Local Deduplication found on captured fingerprints against Operator Biometrics");
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.FINGERPRINT_DUPLICATION_ALERT);
				return;
			}

			LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Updating captured fingerprints in session");
			updateFingerBiometricsSession(fingerprintDetailsDTOs, detailsDTO);

			continueBtn.setDisable(false);

			captureTimeValue.setText(Duration.between(start, end).toString().replace("PT", ""));
			if (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				rightSlapCount = detailsDTO.getNumRetry();
				retries = rightSlapCount;
			} else if (fingerType.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {

				leftSlapCount = detailsDTO.getNumRetry();
				retries = leftSlapCount;

			} else {
				thumbCount = detailsDTO.getNumRetry();
				retries = thumbCount;
			}
			streamer.setBioStreamImages(null, detailsDTO.getFingerType(), detailsDTO.getNumRetry());

			if (!bioService.isMdmEnabled())
				scanPopUpViewController.getScanImage().setImage(convertBytesToImage(detailsDTO.getFingerPrint()));
			else {
				detailsDTO.setFingerPrint(streamer.imageBytes);
			}
			biometricImage.setImage(bioService.isMdmEnabled()
					? convertBytesToImage(
							bioService.getBioStreamImage(detailsDTO.getFingerType(), detailsDTO.getNumRetry()))
					: convertBytesToImage(detailsDTO.getFingerPrint()));

			LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Updating progress Bar,Text and attempts Box in UI");
			setCapturedValues(bioService.isMdmEnabled()
					? bioService.getBioQualityScores(detailsDTO.getFingerType(), detailsDTO.getNumRetry())
					: detailsDTO.getQualityScore(), detailsDTO.getNumRetry(), thresholdValue);

		}

		else {
			LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating Captured FingerPrints Failed");
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.FP_DEVICE_ERROR);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Fingerprints Scanning is completed");

	}

	private void updateFingerBiometricsSession(List<FingerprintDetailsDTO> fingerprintDetailsDTOs,
			FingerprintDetailsDTO detailsDTO) {
		FingerprintDetailsDTO fingerprintDetails = null;
		for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
			if (fingerprintDetailsDTO.getFingerType() != null

					&& fingerprintDetailsDTO.getFingerType().equals(detailsDTO.getFingerType())) {

				fingerprintDetails = fingerprintDetailsDTO;
				break;
			}
		}

		if (fingerprintDetails != null) {
			fingerprintDetailsDTOs.remove(fingerprintDetails);
		}
		fingerprintDetailsDTOs.add(detailsDTO);

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

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating captured values of biometrics");

		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.FINGERPRINT_PANES_SELECTED);
		qualityScore.setText(getQualityScore(qltyScore));
		attemptSlap.setText(String.valueOf(retry));

		thresholdLabel.setText(String.valueOf(thresholdValue));
		thresholdScoreLabel.setText(getQualityScore(thresholdValue));

		bioProgress.setProgress(
				Double.valueOf(getQualityScore(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) / 100);
		qualityText.setText(getQualityScore(qltyScore));
		if (Double.valueOf(getQualityScore(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) >= thresholdValue) {
			// clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_GREEN, retry);
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_GREEN);
		} else {
			// clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_RED, retry);
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_RED);
		}

		if (retry == Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.IRIS_RETRY_COUNT))) {
			scanBtn.setDisable(true);
		} else {
			scanBtn.setDisable(false);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated captured values of biometrics");
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

				if (bioService.isMdmEnabled()) {
					String eventString = mouseEvent.toString();
					int index = eventString.indexOf(RegistrationConstants.RETRY_ATTEMPT_ID);

					if (index == -1) {
						String text = "Text[text=";
						index = eventString.indexOf(text) + text.length() + 1;

					} else {
						index = index + RegistrationConstants.RETRY_ATTEMPT_ID.length();
					}
					try {

						String modality;
						if (bioType.contains("Iris")) {
							List<IrisDetailsDTO> guardianIris = getRegistrationDTOFromSession().getBiometricDTO()
									.getIntroducerBiometricDTO().getIrisDetailsDTO();
							IrisDetailsDTO irisDetailsDTO = guardianIris.isEmpty() ? null : guardianIris.get(0);
							modality = irisDetailsDTO != null ? irisDetailsDTO.getIrisType() : null;
						} else {
							List<FingerprintDetailsDTO> gurdianFingerPrints = getRegistrationDTOFromSession()
									.getBiometricDTO().getIntroducerBiometricDTO().getFingerprintDetailsDTO();
							FingerprintDetailsDTO fingerprintDetailsDTO = gurdianFingerPrints.isEmpty() ? null
									: gurdianFingerPrints.get(0);
							modality = fingerprintDetailsDTO != null ? fingerprintDetailsDTO.getFingerType() : null;
						}

						System.out.println(modality);

						updateByAttempt(modality, Character.getNumericValue(eventString.charAt(index)), biometricImage,
								qualityText, bioProgress, qualityScore);

						LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Mouse Event by attempt Ended");

					} catch (RuntimeException runtimeException) {
						LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

					}
				}

			}

		};

		if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
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
		}

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
		bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries).getStyleClass().clear();
		bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries).getStyleClass().add(styleClass);
	}

	/**
	 * Clear captured data
	 *
	 */
	private void clearCaptureData() {
		bioProgress.setProgress(0);
		bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
		bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);

		qualityText.setText(RegistrationConstants.EMPTY);
		qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
		qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);

		bioRetryBox.getChildren().clear();

		clearAllBiometrics();
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
	private Boolean validateFingerPrintQulaity(FingerprintDetailsDTO fingerprintDetailsDTO, Double handThreshold) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Validating the quality score of the captured fingers");
		double qualityScore = bioService.isMdmEnabled()
				? bioService.getHighQualityScoreByBioType(fingerprintDetailsDTO.getFingerType(),
						fingerprintDetailsDTO.getQualityScore())
				: fingerprintDetailsDTO.getQualityScore();

		return qualityScore >= handThreshold
				|| (qualityScore < handThreshold) && fingerprintDetailsDTO.getNumRetry() == Integer
						.parseInt(getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT))
				|| fingerprintDetailsDTO.isForceCaptured();
	}

	/**
	 * Fingerdeduplication check.
	 *
	 * @param fingerprintDetailsDTOs
	 *            the fingerprint details DT os
	 * @return true, if successful
	 */
	private boolean fingerdeduplicationCheck(List<FingerprintDetailsDTO> fingerprintDetailsDTOs) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Validating the dedupecheck of the captured fingers");

		List<FingerprintDetailsDTO> segmentedFingerprintDetailsDTOs = new ArrayList<>();

		boolean isValid = false;

		for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
			for (FingerprintDetailsDTO segmentedFingerprintDetailsDTO : fingerprintDetailsDTO
					.getSegmentedFingerprints()) {
				segmentedFingerprintDetailsDTOs.add(segmentedFingerprintDetailsDTO);
			}
		}
		if (!validateFingerprint(segmentedFingerprintDetailsDTOs)) {
			isValid = true;
		} else {
			duplicateCheckLbl.setText(RegistrationUIConstants.FINGERPRINT_DUPLICATION_ALERT);
		}

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Validated the dedupecheck of the captured fingers");

		return isValid;
	}

	/**
	 * Validates QualityScore.
	 *
	 * @param irisDetailsDTO
	 *            the iris details DTO
	 * @param irisThreshold
	 *            the iris threshold
	 * @return boolean
	 */
	private boolean validateIrisQulaity(IrisDetailsDTO irisDetailsDTO, Double irisThreshold) {

		LOGGER.info(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Validating the quality score of the captured iris");

		double qualityScore = bioService.isMdmEnabled()
				? bioService.getHighQualityScoreByBioType(irisDetailsDTO.getIrisType(),
						irisDetailsDTO.getQualityScore())
				: irisDetailsDTO.getQualityScore();

		return qualityScore >= irisThreshold || (Double.compare(qualityScore, irisThreshold) < 0
				&& irisDetailsDTO.getNumOfIrisRetry() == Integer
						.parseInt(getValueFromApplicationContext(RegistrationConstants.IRIS_RETRY_COUNT))
				|| irisDetailsDTO.isForceCaptured());
	}

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
	private Map<String, Integer> exceptionFingersCount(int leftSlapCount, int rightSlapCount, int thumbCount,
			int irisCount) {
		leftSlapexceptionList.clear();
		rightSlapexceptionList.clear();
		thumbsexceptionList.clear();
		Map<String, Integer> exceptionCountMap = new HashMap<>();
		List<BiometricExceptionDTO> biometricExceptionDTOs;
		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			biometricExceptionDTOs = getBiometricDTOFromSession().getOperatorBiometricDTO().getBiometricExceptionDTO();
		} else if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()
				|| (boolean) SessionContext.map().get(RegistrationConstants.IS_Child)) {
			biometricExceptionDTOs = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getBiometricExceptionDTO();
		} else {
			biometricExceptionDTOs = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
					.getBiometricExceptionDTO();
		}
		for (BiometricExceptionDTO biometricExceptionDTO : biometricExceptionDTOs) {

			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.LEFT.toLowerCase())
					&& biometricExceptionDTO.isMarkedAsException())
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)) {
				getExceptionIdentifier(leftSlapexceptionList, biometricExceptionDTO.getMissingBiometric());
				leftSlapCount++;
			}
			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.RIGHT.toLowerCase())
					&& biometricExceptionDTO.isMarkedAsException())
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)) {
				getExceptionIdentifier(rightSlapexceptionList, biometricExceptionDTO.getMissingBiometric());
				rightSlapCount++;
			}
			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
					&& biometricExceptionDTO.isMarkedAsException())) {
				getExceptionIdentifier(thumbsexceptionList, biometricExceptionDTO.getMissingBiometric());
				thumbCount++;
			}
			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)
					&& biometricExceptionDTO.isMarkedAsException())) {
				irisCount++;
			}
		}
		exceptionCountMap.put(RegistrationConstants.LEFTSLAPCOUNT, leftSlapCount);
		exceptionCountMap.put(RegistrationConstants.RIGHTSLAPCOUNT, rightSlapCount);
		exceptionCountMap.put(RegistrationConstants.THUMBCOUNT, thumbCount);
		exceptionCountMap.put(RegistrationConstants.EXCEPTIONCOUNT,
				leftSlapCount + rightSlapCount + thumbCount + irisCount);

		return exceptionCountMap;
	}

	/**
	 * Manage biometrics list based on exceptions.
	 */
	public void manageBiometricsListBasedOnExceptions() {

		if (getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
				.getBiometricExceptionDTO() != null
				|| !getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
						.getBiometricExceptionDTO().isEmpty()) {
			int leftSlapCount = 0;
			int rightSlapCount = 0;
			int thumbCount = 0;
			int irisCount = 0;
			biometricTypecombo.getItems().clear();
			populateBiometrics();

			Map<String, Integer> exceptionCount = exceptionFingersCount(leftSlapCount, rightSlapCount, thumbCount,
					irisCount);
			int excepCount = exceptionCount.get(RegistrationConstants.EXCEPTIONCOUNT);

			if ((RegistrationConstants.DISABLE.equalsIgnoreCase(
					getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG)) && excepCount == 2)
					|| (RegistrationConstants.DISABLE
							.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))
							&& excepCount == 10)
					|| excepCount == 12) {
				bioValue = RegistrationUIConstants.SELECT;
				biometricBox.setVisible(true);
				bioProgress.setProgress(1);
				biometricTypecombo.setVisible(false);
				thresholdBox.setVisible(false);
				scanBtn.setText(RegistrationUIConstants.TAKE_PHOTO);
				duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
				retryBox.setVisible(false);
				if (getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().getFace()
						.getFace() == null) {
					scanBtn.setDisable(false);
					continueBtn.setDisable(true);
					updateBiometric(RegistrationUIConstants.PHOTO, RegistrationConstants.IMAGE_PATH, "",
							String.valueOf(RegistrationConstants.PARAM_ZERO));
				} else {
					continueBtn.setDisable(false);
				}
			} else {
				biometricTypecombo.setVisible(true);
				if (bioValue.equalsIgnoreCase(RegistrationUIConstants.SELECT)) {
					biometricBox.setVisible(false);
				} else {
					biometricBox.setVisible(true);
				}
				thresholdBox.setVisible(true);
				scanBtn.setText(RegistrationUIConstants.SCAN);
				duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
				photoAlert.setVisible(false);
				if (exceptionCount.get(RegistrationConstants.LEFTSLAPCOUNT) == 4) {
					modifyBiometricType(RegistrationUIConstants.LEFT_SLAP);
				}
				if (exceptionCount.get(RegistrationConstants.RIGHTSLAPCOUNT) == 4) {
					modifyBiometricType(RegistrationUIConstants.RIGHT_SLAP);
				}
				if (exceptionCount.get(RegistrationConstants.THUMBCOUNT) == 2) {
					modifyBiometricType(RegistrationUIConstants.THUMBS);
				}
				if (anyIrisException(RegistrationConstants.LEFT)) {
					modifyBiometricType(RegistrationUIConstants.LEFT_IRIS);
				}
				if (anyIrisException(RegistrationConstants.RIGHT)) {
					modifyBiometricType(RegistrationUIConstants.RIGHT_IRIS);
				}
				List<String> bioList = new ArrayList<>();
				biometricTypecombo.getItems().forEach(bio -> bioList.add(bio.getName()));
				if (!bioList.contains(bioValue)) {
					bioValue = RegistrationUIConstants.SELECT;
					clearCapturedBioData();
				}
			}

		}
		biometricTypecombo.getSelectionModel().clearSelection();
		biometricTypecombo.setPromptText(bioValue);
		if (bioProgress.getProgress() != 0) {
			if (!scanBtn.getText().equalsIgnoreCase(RegistrationUIConstants.TAKE_PHOTO)) {
				scanBtn.setDisable(true);
			}
			continueBtn.setDisable(false);
		} else {
			continueBtn.setDisable(true);
		}
	}

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

	private boolean validateFingerprint(List<FingerprintDetailsDTO> fingerprintDetailsDTOs) {
		AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId());
		authenticationValidatorDTO.setFingerPrintDetails(fingerprintDetailsDTOs);
		authenticationValidatorDTO.setAuthValidationType("multiple");
		boolean isValid = authenticationService.authValidator("Fingerprint", authenticationValidatorDTO);
		if (null != getValueFromApplicationContext("IDENTY_SDK")) {
			isValid = false;
		}
		return isValid;

	}

	private void updateByAttempt(String bioType, int attempt) {

		double qualityScoreValue = bioService.getBioQualityScores(bioType, attempt);
		String qualityScore = getQualityScore(qualityScoreValue);

		if (qualityScore != null) {
			Image streamImage = convertBytesToImage(bioService.getBioStreamImage(bioType, attempt));
			// Set Stream image
			biometricImage.setImage(streamImage);

			// Quality Label
			qualityText.setText(qualityScore);

			// Progress BAr
			bioProgress.setProgress(qualityScoreValue / 100);

			// Progress Bar Quality Score
			this.qualityScore.setText(String.valueOf((int) qualityScoreValue).concat(RegistrationConstants.PERCENTAGE));

		}

	}

	// public void addBioStreamImage(byte[] streamImageBytes) {
	//
	// STREAM_IMAGES.put(String.format("%s_%s", currentSubType, currentModality),
	// streamImageBytes);
	// }

	public void addBioStreamImage(String subType, String modality, String attempt, Image streamImage) {

		STREAM_IMAGES.put(String.format("%s_%s_%s", subType, modality, attempt), streamImage);
	}

	public Image getBioStreamImage(String subType, String modality, String attempt) {

		return STREAM_IMAGES.get(String.format("%s_%s_%s", subType, modality, attempt));
	}

	public void refreshContinueButton() {

		// get bio attributes by subType ffrom schema
		List<String> bioAttributes = getBioAttributesBySubType(getListOfBiometricSubTypess().get(currentPosition));

		boolean isAllBiometricsCaptured = true;

		List<String> leftHandBioAttributes = getContainsAllElements(RegistrationConstants.leftHandUiAttributes,
				bioAttributes);
		List<String> rightHandBioAttributes = getContainsAllElements(RegistrationConstants.rightHandUiAttributes,
				bioAttributes);
		List<String> twoThumbsBioAttributes = getContainsAllElements(RegistrationConstants.twoThumbsUiAttributes,
				bioAttributes);
		List<String> faceBioAttributes = getContainsAllElements(Arrays.asList(RegistrationConstants.FACE),
				bioAttributes);
		List<String> irisBioAttributes = getContainsAllElements(RegistrationConstants.eyesUiAttributes, bioAttributes);

		isAllBiometricsCaptured = isBiometricsCaptured(leftHandBioAttributes,
				getThresholdScoreInInt(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD))
				&& isBiometricsCaptured(rightHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD))
				&& isBiometricsCaptured(twoThumbsBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD))
				&& isBiometricsCaptured(irisBioAttributes, getThresholdScoreInInt(RegistrationConstants.IRIS_THRESHOLD))
				&& isBiometricsCaptured(faceBioAttributes, getThresholdScoreInInt(RegistrationConstants.FACE));

		if (isAllBiometricsCaptured) {
			continueBtn.setDisable(false);
		} else {
			// disable continue button
			continueBtn.setDisable(true);
		}

	}

	private boolean isBiometricsCaptured(List<String> bioAttributes, int thresholdScore) {

		boolean isCaptured = false;

		if (bioAttributes != null && !bioAttributes.isEmpty()) {

			int qualityScore = 0;

			int exceptionBioCount = 0;

			boolean isForceCaptured = false;
			for (String bioAttribute : bioAttributes) {

				BiometricsDto biometricDTO = getRegistrationDTOFromSession().getBiometric(currentSubType, bioAttribute);
				if (biometricDTO != null) {

					/* Captures check */
					qualityScore += biometricDTO.getQualityScore();

					isCaptured = true;
					isForceCaptured = biometricDTO.isForceCaptured();

				} else if (getRegistrationDTOFromSession().isBiometricExceptionAvailable(currentSubType,
						bioAttribute)) {

					/* Exception bio check */
					isCaptured = true;
					++exceptionBioCount;

				} else {
					isCaptured = false;
					break;
				}
			}

			/* quality score check and force capture check */
			if (isCaptured && !isForceCaptured) {
				isCaptured = qualityScore / bioAttributes.size() - exceptionBioCount >= thresholdScore;
			}
		} else {

			/* If no bio attributes */
			isCaptured = true;
		}
		return isCaptured;
	}
}