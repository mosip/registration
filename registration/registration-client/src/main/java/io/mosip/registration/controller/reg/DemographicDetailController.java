package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.applicanttype.exception.InvalidApplicantArgumentException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.builder.Builder;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.IntroducerType;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.VirtualKeyboard;
import io.mosip.registration.controller.device.FaceCaptureController;
import io.mosip.registration.controller.device.GuardianBiometricsController;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.biometric.BiometricInfoDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.demographic.AddressDTO;
import io.mosip.registration.dto.demographic.CBEFFFilePropertiesDTO;
import io.mosip.registration.dto.demographic.DemographicInfoDTO;
import io.mosip.registration.dto.demographic.DocumentDetailsDTO;
import io.mosip.registration.dto.demographic.IndividualIdentity;
import io.mosip.registration.dto.demographic.LocationDTO;
import io.mosip.registration.dto.demographic.ValuesDTO;
import io.mosip.registration.dto.mastersync.LocationDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * {@code DemographicDetailController} is to capture the demographic details
 * 
 * @author Taleev.Aalam
 * @since 1.0.0
 *
 */

@Controller
public class DemographicDetailController extends BaseController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);

	@FXML
	public TextField preRegistrationId;

	@FXML
	private TextField fullName;

	@FXML
	private TextField fullNameLocalLanguage;

	@FXML
	private Label fullNameLocalLanguageLabel;

	@FXML
	private Label fullNameLocalLanguageMessage;

	@FXML
	private Label ageFieldLocalLanguageLabel;

	@FXML
	private Label genderLocalLanguageLabel;

	@FXML
	private Label regionLocalLanguageMessage;

	@FXML
	private Label regionLocalLanguageLabel;

	@FXML
	private Label cityLocalLanguageLabel;

	@FXML
	private Label cityLocalLanguageMessage;

	@FXML
	private Label provinceLocalLanguageLabel;

	@FXML
	private Label provinceLocalLanguageMessage;

	@FXML
	private Label zoneLocalLanguageLabel;

	@FXML
	private Label zoneLocalLanguageMessage;

	@FXML
	private Label postalCodeLocalLanguageLabel;

	@FXML
	private Label mobileNoLocalLanguageLabel;

	@FXML
	private Label emailIdLocalLanguageLabel;

	@FXML
	private Label cniOrPinNumberLocalLanguageLabel;

	@FXML
	private Label parentNameLocalLanguageLabel;

	@FXML
	private Label parentNameLocalLanguageMessage;

	@FXML
	private Label languageLabelLocalLanguage;

	private TextField ageField;

	@FXML
	private TextField ageFieldLocalLanguage;
	@FXML
	private Label uinRidToggleLabel1;
	@FXML
	private Label uinRidToggleLabel2;
	@FXML
	private Label uinRidToggleLabel1LocalLanguage;
	@FXML
	private Label uinRidToggleLabel2LocalLanguage;

	@FXML
	private Label mmLabel;
	@FXML
	private Label ddLabel;
	@FXML
	private Label yyyyLabel;
	@FXML
	private GridPane parentDetailPane;
	@FXML
	private ScrollPane parentScrollPane;
	private SimpleBooleanProperty switchedOnParentUinOrRid;

	@FXML
	private TextField addressLine1;

	@FXML
	private Label addressLine1Label;

	@FXML
	private Label addressLine1Message;

	@FXML
	private TextField addressLine1LocalLanguage;

	@FXML
	private Label addressLine1LocalLanguageLabel;

	@FXML
	private Label addressLine1LocalLanguageMessage;

	@FXML
	private TextField addressLine2;

	@FXML
	private Label addressLine2Label;

	@FXML
	private Label addressLine2Message;

	@FXML
	private TextField addressLine2LocalLanguage;

	@FXML
	private Label addressLine2LocalLanguageLabel;

	@FXML
	private Label addressLine2LocalLanguageMessage;

	@FXML
	private TextField addressLine3;

	@FXML
	private Label addressLine3Label;

	@FXML
	private Label addressLine3Message;

	@FXML
	private Label parentRegIdLabel;

	@FXML
	private TextField addressLine3LocalLanguage;

	@FXML
	private Label addressLine3LocalLanguageLabel;

	@FXML
	private Label addressLine3LocalLanguageMessage;

	@FXML
	private Label postalCodeMessage;

	@FXML
	private Label postalCodeLocalLanguageMessage;

	@FXML
	private Label mobileNoMessage;

	@FXML
	private Label mobileNoLocalLanguageMessage;

	@FXML
	private Label emailIdMessage;

	@FXML
	private Label emailIdLocalLanguageMessage;

	@FXML
	private Label cniOrPinNumberMessage;

	@FXML
	private Label cniOrPinNumberLocalLanguageMessage;

	@FXML
	private TextField emailId;

	@FXML
	private VBox emailIdPane;

	@FXML
	private TextField emailIdLocalLanguage;

	@FXML
	private TextField mobileNo;

	@FXML
	private VBox applicationMobileNumber;

	@FXML
	private VBox applicationAddressLine1;

	@FXML
	private VBox localAddressLine1;

	@FXML
	private VBox localAddressLine2;

	@FXML
	private VBox localAddressLine3;

	@FXML
	private VBox applicationAddressLine2;

	@FXML
	private VBox applicationAddressLine3;

	@FXML
	private VBox applicationRegion;

	@FXML
	private VBox applicationProvince;

	@FXML
	private VBox applicationCity;

	@FXML
	private VBox applicationzone;

	@FXML
	private VBox applicationPostalCode;

	@FXML
	private TextField mobileNoLocalLanguage;

	@FXML
	private ComboBox<LocationDto> region;

	@FXML
	private Label regionMessage;

	@FXML
	private Label regionLabel;

	@FXML
	private ComboBox<LocationDto> regionLocalLanguage;

	@FXML
	private VBox regionLocalLanguagePane;

	@FXML
	private ComboBox<LocationDto> city;

	@FXML
	private Label cityMessage;

	@FXML
	private Label cityLabel;

	@FXML
	private ComboBox<LocationDto> cityLocalLanguage;

	@FXML
	private VBox cityLocalLanguagePane;

	@FXML
	private ComboBox<LocationDto> province;

	@FXML
	private Label provinceLabel;

	@FXML
	private Label provinceMessage;

	@FXML
	private ComboBox<LocationDto> provinceLocalLanguage;

	@FXML
	private VBox provinceLocalLanguagePane;

	@FXML
	private TextField postalCode;

	@FXML
	private TextField postalCodeLocalLanguage;

	@FXML
	private VBox postalCodeLocalLanguagePane;

	@FXML
	private VBox localMobileNumberPane;

	@FXML
	private ComboBox<LocationDto> zone;

	@FXML
	private Label zoneMessage;

	@FXML
	private Label zoneLabel;

	@FXML
	private ComboBox<LocationDto> zoneLocalLanguage;

	@FXML
	private VBox zoneLocalLanguagePane;

	@FXML
	private VBox localEmailIdPane;

	@FXML
	private VBox localCniOrPinPane;

	@FXML
	private TextField cniOrPinNumber;

	@FXML
	private VBox cniOrPinNumberPane;

	@FXML
	private TextField cniOrPinNumberLocalLanguage;

	@FXML
	private TextField parentNameLocalLanguage;

	@FXML
	private TextField parentName;

	@FXML
	private Label parentNameMessage;

	@FXML
	private Label parentNameLabel;

	@FXML
	private Label ageFieldLabel;

	@FXML
	private Label ageFieldLocalLanguageMessage;

	@FXML
	private TextField parentRegId;

	@FXML
	private Label parentRegIdMessage;

	@FXML
	private Label parentRegIdLocalLanguageMessage;

	@FXML
	private TextField parentRegIdLocalLanguage;

	@FXML
	private Label parentRegIdLocalLanguageLabel;

	@FXML
	private TextField parentUinId;

	@FXML
	private Label parentUinIdMessage;

	@FXML
	private Label parentUinIdLocalLanguageMessage;

	@FXML
	private TextField parentUinIdLocalLanguage;

	@FXML
	private Label parentUinIdLocalLanguageLabel;

	@FXML
	private Label parentUinIdLabel;

	private boolean isChild;

	private Node keyboardNode;

	@FXML
	protected Button autoFillBtn;

	@FXML
	protected Button copyPrevious;

	@FXML
	protected Button fetchBtn;

	@FXML
	private HBox dob;

	@FXML
	private HBox dobLocallanguage;

	private TextField dd;

	private TextField mm;

	private TextField yyyy;

	@FXML
	private TextField ddLocalLanguage;

	@FXML
	private Label ddLocalLanguageLabel;

	@FXML
	private Label dobMessage;

	@FXML
	private TextField mmLocalLanguage;

	@FXML
	private Label mmLocalLanguageLabel;

	@FXML
	private TextField yyyyLocalLanguage;

	@FXML
	private Label yyyyLocalLanguageLabel;

	@FXML
	private Label residenceLblLocalLanguage;

	@Autowired
	private PridValidator<String> pridValidatorImpl;
	@Autowired
	private UinValidator<String> uinValidator;
	@Autowired
	private RidValidator<String> ridValidator;
	@Autowired
	private Validations validation;
	@Autowired
	private MasterSyncService masterSync;
	@FXML
	private AnchorPane dateAnchorPane;
	@FXML
	private GridPane residenceParentpane;
	@FXML
	private AnchorPane dateAnchorPaneLocalLanguage;
	@FXML
	private VBox applicationLanguageAddressPane;
	@FXML
	private VBox localLanguageAddressPane;
	@FXML
	private Label preRegistrationLabel;
	@FXML
	private Label fullNameLabel;
	@FXML
	private Label fullNameMessage;
	@FXML
	private Label genderLabel;
	@FXML
	private Label mobileNoLabel;
	@FXML
	private Label emailIdLabel;
	@FXML
	private Label cniOrPinNumberLabel;
	@FXML
	private AnchorPane applicationLanguagePane;
	@FXML
	private AnchorPane localLanguagePane;
	@FXML
	private FlowPane parentFlowPane;
	@FXML
	private Button national;
	@FXML
	private Button male;
	@FXML
	private Button female;
	@FXML
	private Button maleLocalLanguage;
	@FXML
	private Button femaleLocalLanguage;
	@FXML
	private GridPane demographicDetail;
	@FXML
	private GridPane dobParentPane;
	@FXML
	private GridPane demographicParentPane;
	@FXML
	private GridPane fullNameParentPane;
	@FXML
	private GridPane emailIdCniParentPane;
	@FXML
	private GridPane childParentDetail;
	@FXML
	private GridPane genderParentPane;
	@FXML
	private TextField updateUinId;
	@FXML
	private Button foreigner;
	@FXML
	private TextField residence;
	@FXML
	private TextField genderValue;
	@FXML
	private TextField genderValueLocalLanguage;
	@FXML
	private Button nationalLocalLanguage;
	@FXML
	private Button foreignerLocalLanguage;
	@FXML
	private TextField residenceLocalLanguage;
	@FXML
	private VBox applicationFullName;
	@FXML
	private GridPane fullNameGridPane;
	@FXML
	private ImageView fullNameKeyboardImage;
	@FXML
	private ImageView addressLine1KeyboardImage;
	@FXML
	private ImageView addressLine2KeyboardImage;
	@FXML
	private ImageView addressLine3KeyboardImage;
	@FXML
	private ImageView parentNameKeyboardImage;
	@FXML
	private VBox localFullName;
	@FXML
	private GridPane applicationAge;
	@FXML
	private GridPane localAge;
	@FXML
	private VBox localUinIdPane;
	@FXML
	private VBox applicationUinIdPane;
	@FXML
	private AnchorPane localRidOrUinToggle;
	@FXML
	private VBox localRidPane;
	@FXML
	private VBox applicationRidPane;
	@FXML
	private GridPane applicationGender;
	@FXML
	private GridPane localGender;
	@FXML
	private GridPane applicationResidence;
	@FXML
	private GridPane localResidence;
	@FXML
	private GridPane localFullNameBox;
	@FXML
	private GridPane localAddressPane;
	@FXML
	private GridPane localLanguageParentDetailPane;
	@FXML
	private GridPane preRegParentPane;
	@FXML
	private VBox applicationemailIdPane;
	@FXML
	private VBox applicationCniOrPinNumberPane;
	@Autowired
	private DateValidation dateValidation;
	@Autowired
	private PreRegistrationDataSyncService preRegistrationDataSyncService;
	@Autowired
	private RegistrationController registrationController;
	@Autowired
	private DocumentScanController documentScanController;
	@Autowired
	private Transliteration<String> transliteration;

	@Autowired
	private GuardianBiometricsController guardianBiometricsController;
	private FXUtils fxUtils;
	private Date dateOfBirth;
	private int minAge;
	private int maxAge;

	@FXML
	private HBox parentDetailsHbox;
	@FXML
	private HBox localParentDetailsHbox;
	@FXML
	private AnchorPane ridOrUinToggle;

	@Autowired
	private MasterSyncService masterSyncService;
	@FXML
	private GridPane borderToDo;
	@FXML
	private Label registrationNavlabel;
	@FXML
	private AnchorPane keyboardPane;
	private boolean lostUIN = false;
	private ResourceBundle applicationLabelBundle;
	private ResourceBundle localLabelBundle;
	private String textMale;
	private String textFemale;
	private String textMaleLocalLanguage;
	private String textFemaleLocalLanguage;
	private String textMaleCode;
	@FXML
	private Label ageOrDOBLocalLanguageLabel;
	@FXML
	private Label ageOrDOBLabel;
	@Autowired
	private FaceCaptureController faceCaptureController;
	private double fullNameNodePos;
	private double addressLine1NodePos;
	private double addressLine2NodePos;
	private double addressLine3NodePos;
	private double parentNameNodePos;

	private String primaryLanguage;
	private String secondaryLanguage;

	private String orderOfAddress[] = { "region", "province", "city", "zone" };

	boolean hasToBeTransliterated = true;

	private List<Object> listOfFields = new ArrayList<>();

	public Map<String, ComboBox<String>> listOfComboBoxWithString = new HashMap<>();

	public Map<String, ComboBox<LocationDto>> listOfComboBoxWithLocation = new HashMap<>();

	public Map<String, TextField> listOfTextField = new HashMap<>();
	
	private int age = 0;
	
	@Autowired
	private IdentitySchemaService identitySchemaService;


	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize()
	 */
	@FXML
	private void initialize() throws IOException {

		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Entering the Demographic Details Screen");

		if (ApplicationContext.getInstance().getApplicationLanguage()
				.equals(ApplicationContext.getInstance().getApplicationLanguage())) {
			hasToBeTransliterated = false;
		}

		try {
			RegistrationConstants.CNI_MANDATORY = String.valueOf(false);
			if (getRegistrationDTOFromSession() == null) {
				validation.updateAsLostUIN(false);
				registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW);
			}

			if (getRegistrationDTOFromSession() != null
					&& getRegistrationDTOFromSession().getSelectionListDTO() == null) {
				getRegistrationDTOFromSession().setUpdateUINNonBiometric(false);
				getRegistrationDTOFromSession().setUpdateUINChild(false);
			}
//			postalCode.setDisable(true);
//			validation.setChild(false);
//			parentDetailPane.setManaged(false);
//			lostUIN = false;
			fxUtils = FXUtils.getInstance();
//			fxUtils.setTransliteration(transliteration);
//			isChild = false;
			// ageBasedOperation();

			// addListenersFromUiProperties(validation.getValidationMap(), parentFlowPane);

			// TODO Modify Listeners
			// listenerOnFields();

			fullNameNodePos = 200.00;
			addressLine1NodePos = 470.00;
			addressLine2NodePos = 555.00;
			addressLine3NodePos = 630.00;
			parentNameNodePos = 1110.00;
//			if (validation.isLostUIN()) {
//				positionKeyboardForLostUIN();
//			}
			// loadKeyboard();
			// renderComboBoxes();
			minAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MIN_AGE));
			maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
			applicationLabelBundle = applicationContext.getApplicationLanguageBundle();
			localLabelBundle = applicationContext.getLocalLanguageProperty();
			primaryLanguage = applicationContext.getApplicationLanguage();
			secondaryLanguage = applicationContext.getLocalLanguage();

//			genderSettings();
//			if (getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
//					.equals(RegistrationConstants.PACKET_TYPE_LOST)) {
//				preRegParentPane.setVisible(false);
//				preRegParentPane.setManaged(false);
//				national.getStyleClass().addAll("residence", "button");
//				nationalLocalLanguage.getStyleClass().addAll("residence", "button");
//				residence.setText(RegistrationConstants.EMPTY);
//			} else {
//				national.getStyleClass().addAll("selectedResidence", "button");
//				nationalLocalLanguage.getStyleClass().addAll("selectedResidence", "button");
//			}

			loadUIElementsFromSchema();
			Map<String, UiSchemaDTO> schemaMap = getValidationMap();

			ObservableList<Node> parentFlow = parentFlowPane.getChildren();
			for (Entry<String, UiSchemaDTO> entry : schemaMap.entrySet()) {

				if (!entry.getValue().getId().matches("IDSchemaVersion|UIN")) {
					GridPane mainGridPane = mainGridPane(entry.getValue());
					parentFlow.add(mainGridPane);

				}
			}

			addFirstOrderAddress(listOfComboBoxWithLocation.get(orderOfAddress[0]), orderOfAddress[0], "");
			addFirstOrderAddress(listOfComboBoxWithLocation.get(orderOfAddress[0] + "LocalLanguage"), orderOfAddress[0],
					"LocalLanguage");

			for (int j = 0; j < orderOfAddress.length - 1; j++) {
				final int k = j;
				listOfComboBoxWithLocation.get(orderOfAddress[k]).setOnAction((event) -> {
					addProvince(k, k + 1, orderOfAddress.length);
				});
			}

			for (int i = 0; i < orderOfAddress.length; i++) {
				fxUtils.populateLocalComboBox(parentFlowPane, listOfComboBoxWithLocation.get(orderOfAddress[i]),
						listOfComboBoxWithLocation.get(orderOfAddress[i] + "LocalLanguage"));
			}

			fxUtils.populateLocalComboBox(parentFlowPane, listOfComboBoxWithString.get("gender"),
					listOfComboBoxWithString.get("gender" + "LocalLanguage"));

			fxUtils.populateLocalComboBox(parentFlowPane, listOfComboBoxWithString.get("residenceStatus"),
					listOfComboBoxWithString.get("residenceStatus" + "LocalLanguage"));

			for (Entry<String, TextField> tX : listOfTextField.entrySet()) {
				if (!tX.getKey().contains("LocalLanguage")) {
					fxUtils.populateLocalFieldWithFocus(parentFlowPane, tX.getValue(),
							listOfTextField.get(tX.getKey() + "LocalLanguage"), hasToBeTransliterated, validation);
				}
				fxUtils.onTypeFocusUnfocusListener(parentFlowPane, tX.getValue());

			}

		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_DEMOGRAPHIC_PAGE);

		}
	}

	public GridPane mainGridPane(UiSchemaDTO schemaDTO) {
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(1000);
		gridPane.setPrefHeight(100);

		// gridPane.setStyle("-fx-background-color:BLUE");

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(48);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(7);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(45);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		GridPane primary = subGridPane(schemaDTO, "");
		GridPane secondary = subGridPane(schemaDTO, "LocalLanguage");

		gridPane.addColumn(0, primary);

		gridPane.addColumn(2, secondary);

		return gridPane;
	}

	public GridPane subGridPane(UiSchemaDTO schemaDTO, String languageType) {
		GridPane gridPane = new GridPane();
		gridPane.setPrefHeight(200);

		// gridPane.setStyle("-fx-background-color:RED");

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(85);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
		RowConstraints rowConstraint1 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		RowConstraints rowConstraint2 = new RowConstraints();
		columnConstraint1.setPercentWidth(60);
		RowConstraints rowConstraint3 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		rowConstraints.addAll(rowConstraint1, rowConstraint2, rowConstraint3);

		// create content

		VBox content = null;
				
		switch (schemaDTO.getControlType()) {
		case "dropdown":
			if(Arrays.asList(orderOfAddress).contains(schemaDTO.getId()))
				content = addContentWithComboBoxAndLocation(schemaDTO.getId(), languageType);
			else
				content = addContentWithComboBox(new ComboBox<String>(), schemaDTO.getId(), new Label(), new Label(),
						languageType);
			break;
		case "ageDate":
			content = addContentForDobAndAge(languageType);
			break;
		case "age":	
			//TODO Not yet supported
			break;
		case "textbox":	
			content = addContentWithTextField(schemaDTO.getId(), languageType);
			break;
		}

		gridPane.add(content, 1, 2);

		return gridPane;
	}

	public VBox addContentForDobAndAge(String languageType) {

		VBox vBoxDD = new VBox();

		TextField dd = new TextField();
		dd.getStyleClass().add("demoGraphicTextField");
		dd.setId("dd" + languageType);
		Label ddLabel = new Label();
		ddLabel.setVisible(false);
		ddLabel.setId("dd" + languageType + "Label");
		ddLabel.getStyleClass().add("demoGraphicFieldLabel");
		vBoxDD.getChildren().addAll(ddLabel, dd);

		listOfTextField.put("dd" + languageType, dd);
		listOfFields.add(dd);

		VBox vBoxMM = new VBox();
		TextField mm = new TextField();
		mm.getStyleClass().add("demoGraphicTextField");
		mm.setId("mm" + languageType);
		Label mmLabel = new Label();
		mmLabel.setVisible(false);
		mmLabel.setId("mm" + languageType + "Label");
		mmLabel.getStyleClass().add("demoGraphicFieldLabel");
		vBoxMM.getChildren().addAll(mmLabel, mm);

		listOfTextField.put("mm" + languageType, mm);
		listOfFields.add(mm);

		VBox vBoxYYYY = new VBox();
		TextField yyyy = new TextField();
		yyyy.getStyleClass().add("demoGraphicTextField");
		yyyy.setId("yyyy" + languageType);
		Label yyyyLabel = new Label();
		yyyyLabel.setVisible(false);
		yyyyLabel.setId("yyyy" + languageType + "Label");
		yyyyLabel.getStyleClass().add("demoGraphicFieldLabel");
		vBoxYYYY.getChildren().addAll(yyyyLabel, yyyy);

		listOfTextField.put("yyyy" + languageType, yyyy);
		listOfFields.add(yyyy);

		Label dobMessage = new Label();
		dobMessage.setId("dobMessage" + languageType);
		dobMessage.getStyleClass().add("demoGraphicFieldLabel");

		boolean localLanguage = languageType.equals("LocalLanguage");

		dd.setPromptText(localLanguage ? localLabelBundle.getString("dd") : applicationLabelBundle.getString("dd"));
		ddLabel.setText(localLanguage ? localLabelBundle.getString("dd") : applicationLabelBundle.getString("dd"));
		mm.setPromptText(localLanguage ? localLabelBundle.getString("mm") : applicationLabelBundle.getString("mm"));
		mmLabel.setText(localLanguage ? localLabelBundle.getString("mm") : applicationLabelBundle.getString("mm"));
		dobMessage.setText("");

		yyyy.setPromptText(
				localLanguage ? localLabelBundle.getString("yyyy") : applicationLabelBundle.getString("yyyy"));
		yyyyLabel
				.setText(localLanguage ? localLabelBundle.getString("yyyy") : applicationLabelBundle.getString("yyyy"));

		HBox hB = new HBox();
		hB.setSpacing(10);
		hB.getChildren().addAll(vBoxDD, vBoxMM, vBoxYYYY);

		HBox hB2 = new HBox();
		VBox vboxAgeField = new VBox();
		TextField ageField = new TextField();
		ageField.setId("ageField" + languageType);
		ageField.getStyleClass().add("demoGraphicTextField");
		Label ageFieldLabel = new Label();
		ageFieldLabel.setId("ageField" + languageType + "Label");
		ageFieldLabel.getStyleClass().add("demoGraphicFieldLabel");
		ageFieldLabel.setVisible(false);
		vboxAgeField.getChildren().addAll(ageFieldLabel, ageField);

		listOfTextField.put("ageField" + languageType, ageField);
		listOfFields.add(ageField);

		ageField.setPromptText(
				localLanguage ? localLabelBundle.getString("ageField") : applicationLabelBundle.getString("ageField"));
		ageFieldLabel.setText(
				localLanguage ? localLabelBundle.getString("ageField") : applicationLabelBundle.getString("ageField"));

		hB.setPrefWidth(250);

		hB2.setSpacing(10);

		Label orLabel = new Label("OR");

		VBox orVbox = new VBox();
		orVbox.setPrefWidth(100);
		orVbox.getChildren().addAll(new Label(), orLabel);

		hB2.getChildren().addAll(hB, orVbox, vboxAgeField);

		VBox vB = new VBox();
		vB.getChildren().addAll(hB2, dobMessage);

		if (applicationContext.applicationLanguage().equals("eng") && localLanguage)
			vB.setDisable(true);

		ageBasedOperation(vB, ageField, dobMessage, dd, mm, yyyy);

		fxUtils.focusedAction(hB, dd);
		fxUtils.focusedAction(hB, mm);
		fxUtils.focusedAction(hB, yyyy);

		dateValidation.validateDate(parentFlowPane, dd, mm, yyyy, validation, fxUtils, ageField, ageFieldLocalLanguage,
				dobMessage);
		dateValidation.validateMonth(parentFlowPane, dd, mm, yyyy, validation, fxUtils, ageField, ageFieldLocalLanguage,
				dobMessage);
		dateValidation.validateYear(parentFlowPane, dd, mm, yyyy, validation, fxUtils, ageField, ageFieldLocalLanguage,
				dobMessage);

		return vB;
	}

	public VBox addContentWithTextField(String fieldName, String languageType) {

		TextField field = new TextField();
		Label label = new Label();
		Label validationMessage = new Label();

		VBox vbox = new VBox();
		// vbox.setStyle("-fx-background-color:BLUE");
		vbox.setPrefWidth(500);
		vbox.setId(fieldName + "Parent");
		field.setId(fieldName + languageType);
		field.getStyleClass().add("demoGraphicTextField");
		label.setId(fieldName + languageType + "Label");
		label.getStyleClass().add("demoGraphicFieldLabel");
		label.setVisible(false);
		validationMessage.setId(fieldName + languageType + "Message");
		validationMessage.getStyleClass().add("demoGraphicFieldMessageLabel");
		label.setPrefWidth(vbox.getPrefWidth());
		field.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setPrefWidth(vbox.getPrefWidth());
		vbox.setSpacing(5);
		vbox.getChildren().add(label);
		vbox.getChildren().add(field);
		vbox.getChildren().add(validationMessage);
		if (primaryLanguage.equals(secondaryLanguage)) {
			vbox.setDisable(true);
		}

		listOfTextField.put(field.getId(), field);
		listOfFields.add(field);

		if (languageType.equals("LocalLanguage")) {
			field.setPromptText(localLabelBundle.getString(fieldName));
			label.setText(localLabelBundle.getString(fieldName));
			if (fieldName.matches("phone|email|postalCode")) {
				field.setDisable(true);
			}
		} else {
			field.setPromptText(applicationLabelBundle.getString(fieldName));
			label.setText(applicationLabelBundle.getString(fieldName));
		}

		return vbox;
	}

	public VBox addContentWithComboBox(ComboBox<String> field, String fieldName, Label label, Label validationMessage,
			String languageType) {
		VBox vbox = new VBox();
		field.setId(fieldName + languageType);
		field.setPrefWidth(vbox.getPrefWidth());
		listOfComboBoxWithString.put(fieldName + languageType, field);
		listOfFields.add(field);

		try {
			if (fieldName.equals("residenceStatus")) {
				if (!languageType.equals("LocalLanguage"))
					field.getItems()
							.addAll(masterSyncService.getIndividualType(RegistrationConstants.ATTR_NON_FORINGER,
									ApplicationContext.applicationLanguage()).get(0).getName(),
									masterSyncService.getIndividualType(RegistrationConstants.ATTR_FORINGER,
											ApplicationContext.applicationLanguage()).get(0).getName());
				else
					field.getItems()
							.addAll(masterSyncService.getIndividualType(RegistrationConstants.ATTR_NON_FORINGER,
									ApplicationContext.localLanguage()).get(0).getName(),
									masterSyncService.getIndividualType(RegistrationConstants.ATTR_FORINGER,
											ApplicationContext.localLanguage()).get(0).getName());

			} else if (fieldName.equals("gender")) {
				if (!languageType.equals("LocalLanguage")) {
					field.getItems()
							.addAll(masterSyncService.getGenderDtls(ApplicationContext.applicationLanguage()).stream()
									.filter(dto -> dto.getCode().equals(RegistrationConstants.MALE_CODE)).findFirst()
									.get().getGenderName(),
									masterSyncService.getGenderDtls(ApplicationContext.applicationLanguage()).stream()
											.filter(dto -> dto.getCode().equals(RegistrationConstants.FEMALE_CODE))
											.findFirst().get().getGenderName());
				} else {
					field.getItems()
							.addAll(masterSyncService.getGenderDtls(ApplicationContext.localLanguage()).stream()
									.filter(dto -> dto.getCode().equals(RegistrationConstants.MALE_CODE)).findFirst()
									.get().getGenderName(),
									masterSyncService.getGenderDtls(ApplicationContext.localLanguage()).stream()
											.filter(dto -> dto.getCode().equals(RegistrationConstants.FEMALE_CODE))
											.findFirst().get().getGenderName());
				}
			}
		} catch (RegBaseCheckedException e) {
			e.printStackTrace();
		}

		helperMethodForComboBox(field, fieldName, label, validationMessage, vbox, languageType);

		return vbox;
	}

	public <T> VBox addContentWithComboBoxAndLocation(String fieldName, String languageType) {

		ComboBox<LocationDto> field = new ComboBox<LocationDto>();
		Label label = new Label();
		Label validationMessage = new Label();
		StringConverter<T> uiRenderForComboBox = fxUtils.getStringConverterForComboBox();

		VBox vbox = new VBox();
		field.setId(fieldName + languageType);
		field.setPrefWidth(vbox.getPrefWidth());
		listOfComboBoxWithLocation.put(fieldName + languageType, field);
		listOfFields.add(field);
		helperMethodForComboBox(field, fieldName, label, validationMessage, vbox, languageType);
		field.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
		return vbox;
	}

	private void helperMethodForComboBox(ComboBox<?> field, String fieldName, Label label, Label validationMessage,
			VBox vbox, String languageType) {

		if (languageType.equals("LocalLanguage")) {
			field.setPromptText(localLabelBundle.getString(fieldName));
			label.setText(localLabelBundle.getString(fieldName));
			field.setDisable(true);
		} else {
			field.setPromptText(applicationLabelBundle.getString(fieldName));
			label.setText(applicationLabelBundle.getString(fieldName));
		}
		// vbox.setStyle("-fx-background-color:BLUE");
		vbox.setPrefWidth(500);
		vbox.setId(fieldName + "Parent");
		label.setId(fieldName + languageType + "Label");
		label.setVisible(false);
		label.getStyleClass().add("demoGraphicFieldLabel");
		field.getStyleClass().add("demographicCombobox");
		validationMessage.setId(fieldName + languageType + "Message");
		validationMessage.getStyleClass().add("demoGraphicFieldMessageLabel");
		label.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setVisible(false);
		vbox.setSpacing(5);

		vbox.getChildren().addAll(label, field, validationMessage);

		if (primaryLanguage.equals(secondaryLanguage) && languageType.equals("LocalLanguage")) {
			vbox.setDisable(true);
		}
	}

	/**
	 * setting the registration navigation label to lost uin
	 */
	protected void lostUIN() {
		lostUIN = true;
		registrationNavlabel
				.setText(ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));
	}

	/**
	 * Disabe local language fields
	 */
	private void disableLocalFields() {
		localResidence.setDisable(true);
		localGender.setDisable(true);
		regionLocalLanguagePane.setDisable(true);
		provinceLocalLanguagePane.setDisable(true);
		cityLocalLanguagePane.setDisable(true);
		zoneLocalLanguagePane.setDisable(true);
		localEmailIdPane.setDisable(true);
		localCniOrPinPane.setDisable(true);
		postalCodeLocalLanguagePane.setDisable(true);
		localMobileNumberPane.setDisable(true);
		localAge.setDisable(true);
		localUinIdPane.setDisable(true);
		localRidPane.setDisable(true);
		localRidOrUinToggle.setDisable(true);

		disableLocalFieldOnSameLanguage();
	}

	private void disableLocalFieldOnSameLanguage() {
		// if primary and secondary language is same
		if (applicationContext.getApplicationLanguage().equals(applicationContext.getLocalLanguage())) {
			localFullNameBox.setDisable(true);
			localAddressPane.setDisable(true);
			localLanguageParentDetailPane.setDisable(true);

		}
	}

	/**
	 * Toggle functionality between age field and date picker.
	 */
	private void toggleFunctionForParentUinOrRid() {
		try {
			LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Entering into toggle function for parent uin or rid");

			switchedOnParentUinOrRid.addListener((observableValue, oldValue, newValue) -> {
				if (newValue) {
					uinRidToggleLabel1.setLayoutX(0);
					uinRidToggleLabel1LocalLanguage.setLayoutX(0);

					parentRegIdLocalLanguage.clear();
					parentRegId.clear();
					parentUinIdLocalLanguage.clear();
					parentUinId.clear();
					applicationRidPane.setDisable(false);
					applicationUinIdPane.setDisable(true);
					parentRegIdMessage.setVisible(false);
					parentRegIdLocalLanguageMessage.setVisible(false);
					parentUinIdMessage.setVisible(false);
					parentUinIdLocalLanguageMessage.setVisible(false);

				} else {
					uinRidToggleLabel1.setLayoutX(30);
					uinRidToggleLabel1LocalLanguage.setLayoutX(30);

					parentRegIdLocalLanguage.clear();
					parentRegId.clear();
					parentUinIdLocalLanguage.clear();
					parentUinId.clear();
					applicationRidPane.setDisable(true);
					applicationUinIdPane.setDisable(false);
					parentRegIdMessage.setVisible(false);
					parentRegIdLocalLanguageMessage.setVisible(false);
					parentUinIdMessage.setVisible(false);
					parentUinIdLocalLanguageMessage.setVisible(false);

				}

				parentRegId.getStyleClass().remove(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
				parentRegId.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);

				parentUinId.getStyleClass().remove(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
				parentUinId.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);

				parentRegIdLabel.setVisible(false);
				parentRegId.setPromptText(parentRegIdLabel.getText());

				parentUinIdLabel.setVisible(false);
				parentUinId.setPromptText(parentUinIdLabel.getText());

			});

			uinRidToggleLabel1
					.setOnMouseClicked(event -> switchedOnParentUinOrRid.set(!switchedOnParentUinOrRid.get()));
			uinRidToggleLabel2
					.setOnMouseClicked(event -> switchedOnParentUinOrRid.set(!switchedOnParentUinOrRid.get()));
			uinRidToggleLabel1LocalLanguage
					.setOnMouseClicked(event -> switchedOnParentUinOrRid.set(!switchedOnParentUinOrRid.get()));
			uinRidToggleLabel2LocalLanguage
					.setOnMouseClicked(event -> switchedOnParentUinOrRid.set(!switchedOnParentUinOrRid.get()));

			LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Exiting the toggle function for parent uin or rid");
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - TOGGLING OF DOB AND AGE FAILED ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	/**
	 * To restrict the user not to eavcdnter any values other than integer values.
	 */
	private void ageBasedOperation(Pane parentPane, TextField ageField, Label dobMessage, TextField dd, TextField mm,
			TextField yyyy) {
		try {
			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Validating the age given by age field");
			fxUtils.validateLabelFocusOut(parentPane, ageField);
			ageField.focusedProperty().addListener((obsValue, oldValue, newValue) -> {
				if (oldValue) {
					ageValidation(parentPane, ageField, dobMessage, oldValue, dd, mm, yyyy);
				}
			});
			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Validating the age given by age field");
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - AGE FIELD VALIDATION FAILED ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	public void ageValidation(Pane dobParentPane, TextField ageField, Label dobMessage, Boolean oldValue, TextField dd,
			TextField mm, TextField yyyy) {
		if (ageField.getText().matches(RegistrationConstants.NUMBER_OR_NOTHING_REGEX)) {
			if (ageField.getText().matches(RegistrationConstants.NUMBER_REGEX)) {
				if (maxAge >= Integer.parseInt(ageField.getText())) {
					age = Integer.parseInt(ageField.getText());

					// Not to re-calulate DOB and populate DD, MM and YYYY UI fields based on Age,
					// since Age was calculated based on DOB entered by the user. Calculate DOB and
					// populate DD, MM and YYYY UI fields based on user entered Age.
					Calendar defaultDate = Calendar.getInstance();
					defaultDate.set(Calendar.DATE, 1);
					defaultDate.set(Calendar.MONTH, 0);
					defaultDate.add(Calendar.YEAR, -age);

					dateOfBirth = Date.from(defaultDate.toInstant());
					dd.setText(String.valueOf(defaultDate.get(Calendar.DATE)));
					dd.requestFocus();
					mm.setText(String.valueOf(defaultDate.get(Calendar.MONTH + 1)));
					mm.requestFocus();
					yyyy.setText(String.valueOf(defaultDate.get(Calendar.YEAR)));
					yyyy.requestFocus();
					dd.requestFocus();

					if (age <= minAge) {

						if (!isChild == true) {
							clearAllValues();
							clearAllBiometrics();
						}
						if (RegistrationConstants.DISABLE.equalsIgnoreCase(
								getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG))
								&& RegistrationConstants.DISABLE.equalsIgnoreCase(
										getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))) {
							isChild = true;
							validation.setChild(isChild);
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PARENT_BIO_MSG);

						} else {
							updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, true);
							updatePageFlow(RegistrationConstants.FINGERPRINT_CAPTURE, false);
							updatePageFlow(RegistrationConstants.IRIS_CAPTURE, false);

							if (getRegistrationDTOFromSession() != null
									&& getRegistrationDTOFromSession().getSelectionListDTO() != null) {
								enableParentUIN();
							}
						}
					} else {

						if (!isChild == false) {
							clearAllValues();
							clearAllBiometrics();
						}
						if (getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO() != null) {

							getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
									.setFingerprintDetailsDTO(new ArrayList<>());

							getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
									.setIrisDetailsDTO(new ArrayList<>());

							getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
									.setBiometricExceptionDTO(new ArrayList<>());

							getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
									.setExceptionFace(new FaceDetailsDTO());

							getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
									.setFace(new FaceDetailsDTO());

							getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
									.setHasExceptionPhoto(false);

						}

						updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, false);
						updateBioPageFlow(RegistrationConstants.FINGERPRINT_DISABLE_FLAG,
								RegistrationConstants.FINGERPRINT_CAPTURE);
						updateBioPageFlow(RegistrationConstants.IRIS_DISABLE_FLAG, RegistrationConstants.IRIS_CAPTURE);

						// parentFieldValidation();
					}
					fxUtils.validateOnFocusOut(dobParentPane, ageField, validation, false);
				} else {
					ageField.getStyleClass().remove("demoGraphicTextFieldOnType");
					ageField.getStyleClass().add("demoGraphicTextFieldFocused");
					Label ageFieldLabel = (Label) dobParentPane.lookup("#" + ageField.getId() + "Label");
					ageFieldLabel.getStyleClass().add("demoGraphicFieldLabel");
					ageField.getStyleClass().remove("demoGraphicFieldLabelOnType");
					dobMessage.setText(RegistrationUIConstants.INVALID_AGE + maxAge);
					dobMessage.setVisible(true);
					dobMessage.setStyle("-fx-background-color:BLUE");

					generateAlert(dobParentPane, RegistrationConstants.DOB, dobMessage.getText());
					// parentFieldValidation();
				}
			} else {
				updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, false);
				updatePageFlow(RegistrationConstants.FINGERPRINT_CAPTURE, true);
				updatePageFlow(RegistrationConstants.IRIS_CAPTURE, true);
				dd.clear();
				mm.clear();
				yyyy.clear();
			}
		} else {
			ageField.setText(RegistrationConstants.EMPTY);
		}
	}

	private void parentFieldValidation() {
		parentDetailPane.setManaged(false);
		parentDetailPane.setVisible(false);
		parentDetailPane.setDisable(true);
		isChild = false;
		validation.setChild(isChild);
		keyboardNode.setManaged(isChild);
		keyboardNode.setVisible(isChild);
		parentName.clear();
		parentNameLocalLanguage.clear();
		parentRegId.clear();
		parentRegIdLocalLanguage.clear();
		parentRegId.clear();
		parentUinIdLocalLanguage.clear();
		parentUinId.clear();
	}

	/**
	 * Loading the virtual keyboard
	 */
	private void loadKeyboard() {
		try {
			VirtualKeyboard vk = VirtualKeyboard.getInstance();
			keyboardNode = vk.view();
			keyboardNode.setVisible(false);
			keyboardNode.setManaged(false);
			keyboardPane.getChildren().add(keyboardNode);
			vk.changeControlOfKeyboard(fullNameLocalLanguage);
			vk.changeControlOfKeyboard(addressLine1LocalLanguage);
			vk.changeControlOfKeyboard(addressLine2LocalLanguage);
			vk.changeControlOfKeyboard(addressLine3LocalLanguage);
			vk.changeControlOfKeyboard(parentNameLocalLanguage);
			vk.focusListener(fullNameLocalLanguage, fullNameNodePos, keyboardNode);
			vk.focusListener(addressLine1LocalLanguage, addressLine1NodePos, keyboardNode);
			vk.focusListener(addressLine2LocalLanguage, addressLine2NodePos, keyboardNode);
			vk.focusListener(addressLine3LocalLanguage, addressLine3NodePos, keyboardNode);
			vk.focusListener(parentNameLocalLanguage, parentNameNodePos, keyboardNode);
		} catch (NullPointerException exception) {
			LOGGER.error("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
	}

	public void positionKeyboardForLostUIN() {
		fullNameNodePos = fullNameNodePos - 80;
		addressLine1NodePos = addressLine1NodePos - 80;
		addressLine2NodePos = addressLine2NodePos - 80;
		addressLine3NodePos = addressLine3NodePos - 80;
		parentNameNodePos = parentNameNodePos - 80;
	}

	private void addFirstOrderAddress(ComboBox<LocationDto> location, String id, String languageType) {
		location.getItems().clear();
		try {

			List<LocationDto> locations = null;

			if (languageType.equals("LocalLanguage")) {
				locations = masterSync.findLocationByHierarchyCode(
						ApplicationContext.localLanguageBundle().getString(id), ApplicationContext.localLanguage());
			} else {
				locations = masterSync.findLocationByHierarchyCode(
						ApplicationContext.applicationLanguageBundle().getString(id),
						ApplicationContext.applicationLanguage());
			}

			if (locations.isEmpty()) {
				LocationDto lC = new LocationDto();
				lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
				lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
				lC.setLangCode(ApplicationContext.applicationLanguage());
				location.getItems().add(lC);
			} else {
				location.getItems().addAll(locations);
			}
		} catch (RegBaseCheckedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void addDemoGraphicDetailsToSession() {
		// List<Object> fxElements = new LinkedList<>();
		//// fxElements.addAll(listOfFields);
		// fxElements.addAll(listOfComboBoxWithString);
		// fxElements.addAll(listOfComboBoxWithLocation);
		// fxElements.addAll(listOfTextField);
		for (Object fxElement : listOfFields) {
			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
			if (fxElement instanceof TextField) {
				TextField localField = listOfTextField.get(((TextField) fxElement).getId() + "LocalLanguage");
				if (localField.getId().matches("dd|mm|yyyy|ddLocalLanguage|mmLocalLanguage|yyyyLocalLanguage")) {
				
					registrationDTO.addDemographicField("dateOfBirth", "applicationLanguage",
							"dateOfBirth", "localLanguage",
							age+"");
					
					registrationDTO.addDemographicField("age", "applicationLanguage",
							"age", "localLanguage",
							age+"");


				} else {
					registrationDTO.addDemographicField(((TextField) fxElement).getId(), "applicationLanguage",
							((TextField) fxElement).getText(), "localLanguage",
							localField != null ? localField.getText() : null);
				}
			} else if (fxElement instanceof ComboBox<?>) {
				registrationDTO.addDemographicField(((ComboBox<?>) fxElement).getId(),
						((ComboBox<?>) fxElement).getValue());
			}
		}

	}

	/**
	 * To load the provinces in the selection list based on the language code
	 */
	private void addProvince(int s, int p, int size) {
		try {
			retrieveAndPopulateLocationByHierarchy(listOfComboBoxWithLocation.get(orderOfAddress[s]),
					listOfComboBoxWithLocation.get(orderOfAddress[p]),
					listOfComboBoxWithLocation.get(orderOfAddress[p] + "LocalLanguage"));

			for (int i = p + 1; i < size; i++) {
				listOfComboBoxWithLocation.get(orderOfAddress[i]).getItems().clear();
				listOfComboBoxWithLocation.get(orderOfAddress[i] + "LocalLanguage").getItems().clear();
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - LOADING FAILED FOR PROVINCE SELECTION LIST ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}

	}

	/**
	 * To load the cities in the selection list based on the language code
	 */
	private void addCity(ComboBox<LocationDto> province, ComboBox<LocationDto> city,
			ComboBox<LocationDto> cityLocalLanguage, ComboBox<LocationDto> zone,
			ComboBox<LocationDto> zoneLocalLanguage, TextField postalCode, TextField postalCodeLocalLanguage) {
		try {
			retrieveAndPopulateLocationByHierarchy(province, city, cityLocalLanguage);

			zone.getItems().clear();
			zoneLocalLanguage.getItems().clear();
			postalCode.setText(RegistrationConstants.EMPTY);
			postalCodeLocalLanguage.setText(RegistrationConstants.EMPTY);
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - LOADING FAILED FOR CITY SELECTION LIST ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

		}
	}

	/**
	 * To load the localAdminAuthorities selection list based on the language code
	 */
	private void addzone(ComboBox<LocationDto> city, ComboBox<LocationDto> zone,
			ComboBox<LocationDto> zoneLocalLanguage) {
		try {
			retrieveAndPopulateLocationByHierarchy(city, zone, zoneLocalLanguage);

		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - LOADING FAILED FOR LOCAL ADMIN AUTHORITY SELECTOIN LIST ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	private void populatePincode(ComboBox<LocationDto> zone, TextField postalCode) {
		try {
			LocationDto locationDTO = zone.getSelectionModel().getSelectedItem();

			if (null != locationDTO) {
				if (locationDTO.getCode().equalsIgnoreCase(RegistrationConstants.AUDIT_DEFAULT_USER)) {
					postalCode.setText(RegistrationConstants.AUDIT_DEFAULT_USER);
					postalCodeLocalLanguage.setText(RegistrationConstants.AUDIT_DEFAULT_USER);
				} else {
					List<LocationDto> locationDtos = masterSync.findProvianceByHierarchyCode(locationDTO.getCode(),
							locationDTO.getLangCode());

					postalCode.setText(locationDtos.get(0).getName());
					postalCodeLocalLanguage.setText(locationDtos.get(0).getName());
				}
			}

		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
			LOGGER.error("REGISTRATION - Populating of Pin Code Failed ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	/**
	 * 
	 * Saving the detail into concerned DTO'S
	 * 
	 */
	public void saveDetail() {
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Saving the fields to DTO");
		try {
			auditFactory.audit(AuditEvent.SAVE_DETAIL_TO_DTO, Components.REGISTRATION_CONTROLLER,
					SessionContext.userContext().getUserId(), RegistrationConstants.ONBOARD_DEVICES_REF_ID_TYPE);

			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
			
			if (preRegistrationId.getText().isEmpty()) {
				registrationDTO.setPreRegistrationId("");
			}
			OSIDataDTO osiDataDTO = registrationDTO.getOsiDataDTO();
			RegistrationMetaDataDTO registrationMetaDataDTO = registrationDTO.getRegistrationMetaDataDTO();
			String platformLanguageCode = ApplicationContext.applicationLanguage();
			String localLanguageCode = ApplicationContext.localLanguage();

			SessionContext.map().put(RegistrationConstants.IS_Child, isChild);

			addDemoGraphicDetailsToSession();

			if (isChild) {
				osiDataDTO.setIntroducerType(IntroducerType.PARENT.getCode());
			}

			registrationMetaDataDTO.setParentOrGuardianRID(parentRegId.getText());

			osiDataDTO.setOperatorID(SessionContext.userContext().getUserId());

			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Saved the demographic fields to DTO");

		} catch (Exception exception) {
			LOGGER.error("REGISTRATION - SAVING THE DETAILS FAILED ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
	}

	private boolean isParentORGuardian(RegistrationDTO registrationDTO, BiometricInfoDTO introducerBiometric) {
		return !((registrationDTO.getSelectionListDTO() != null && registrationDTO.isUpdateUINChild()
				&& !isCBEFFNotAvailable(introducerBiometric))
				|| (registrationDTO.getSelectionListDTO() == null && !isCBEFFNotAvailable(introducerBiometric)));
	}

	private List<ValuesDTO> buildDemoComboValues(String platformLanguageCode, String localLanguageCode,
			ComboBox<LocationDto> comboField, ComboBox<LocationDto> comboFieldLocalLang, boolean isComboValueRequired) {
		return isComboValueRequired ? null
				: buildValues(platformLanguageCode, localLanguageCode, comboField.getValue().getName(),
						comboFieldLocalLang.getValue().getName());
	}

	private List<ValuesDTO> buildDemoTextValues(String platformLanguageCode, String localLanguageCode,
			TextField demoField, TextField demoFieldLocalLang, boolean isTextFieldNotRequired) {
		return isTextFieldNotRequired ? null
				: buildValues(platformLanguageCode, localLanguageCode, demoField.getText(),
						demoFieldLocalLang.getText());
	}

	@SuppressWarnings("unchecked")
	private List<ValuesDTO> buildValues(String platformLanguageCode, String localLanguageCode, String valueInAppLang,
			String valueInLocalLang) {
		List<ValuesDTO> valuesDTO = (List<ValuesDTO>) Builder.build(LinkedList.class)
				.with(values -> values
						.add(Builder.build(ValuesDTO.class).with(value -> value.setLanguage(platformLanguageCode))
								.with(value -> value.setValue(valueInAppLang)).get()))
				.get();

		if (localLanguageCode != null && !platformLanguageCode.equalsIgnoreCase(localLanguageCode))
			valuesDTO.add(Builder.build(ValuesDTO.class).with(value -> value.setLanguage(localLanguageCode))
					.with(value -> value.setValue(valueInLocalLang)).get());

		return valuesDTO;
	}

	private String buildDemoTextValue(TextField demoField, boolean isTextFieldNotRequired) {
		return isTextFieldNotRequired ? null : demoField.getText();
	}

	private boolean isNameNotRequired(TextField fullName, boolean isNameNotUpdated) {
		return isTextFieldNotRequired(fullName) || isNameNotUpdated;
	}

	private boolean isTextFieldNotRequired(TextField demoField) {
		return demoField.isDisabled() || demoField.getText().isEmpty();
	}

	private boolean postalCodeFieldValidation(TextField demoField) {
		return demoField.getText().isEmpty();
	}

	private boolean isComboBoxValueNotRequired(ComboBox<?> demoComboBox) {
		return demoComboBox.isDisable() || demoComboBox.getValue() == null;
	}

	private BigInteger buildDemoObjectValue(TextField demoField, boolean isTextFieldNotRequired) {
		return isTextFieldNotRequired ? null : new BigInteger(demoField.getText());
	}

	private DocumentDetailsDTO getDocumentFromMap(String documentCategory, Map<String, DocumentDetailsDTO> documents,
			boolean isDocumentsMapEmpty) {
		return isDocumentsMapEmpty ? null : documents.get(documentCategory);
	}

	private CBEFFFilePropertiesDTO buildCBEFFDTO(boolean isCBEFFNotRequired, String cbeffFileName) {
		return isCBEFFNotRequired ? null
				: (CBEFFFilePropertiesDTO) Builder.build(CBEFFFilePropertiesDTO.class)
						.with(cbeffProperties -> cbeffProperties.setFormat(RegistrationConstants.CBEFF_FILE_FORMAT))
						.with(cbeffProperty -> cbeffProperty.setValue(cbeffFileName
								.replace(RegistrationConstants.XML_FILE_FORMAT, RegistrationConstants.EMPTY)))
						.with(cbeffProperty -> cbeffProperty.setVersion(1.0)).get();
	}

	private boolean isCBEFFNotAvailable(BiometricInfoDTO personBiometric) {
		return personBiometric.getFingerprintDetailsDTO().isEmpty() && personBiometric.getIrisDetailsDTO().isEmpty()
				&& personBiometric.getFace().getFace() == null;
	}

	/**
	 * Method will be called for uin Update
	 *
	 */
	public void uinUpdate() {
		if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {

			clearAllValues();
			documentScanController.getBioExceptionToggleLabel1().setLayoutX(0);
			SessionContext.userMap().put(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION, false);

			keyboardNode.setDisable(false);
			keyboardNode.setManaged(false);
			RegistrationConstants.CNI_MANDATORY = String.valueOf(true);

			copyPrevious.setDisable(true);
			autoFillBtn.setVisible(false);
			registrationNavlabel.setText(applicationLabelBundle.getString("uinUpdateNavLbl"));
			parentFlowPane.setDisable(false);
			fetchBtn.setVisible(false);
			parentRegIdLabel.setText(applicationLabelBundle.getString("uinIdUinUpdate"));
			preRegistrationLabel.setText(RegistrationConstants.UIN_LABEL);
			updateUinId.setVisible(true);
			updateUinId.setDisable(true);
			preRegistrationId.setVisible(false);
			getRegistrationDTOFromSession().getRegistrationMetaDataDTO()
					.setUin(getRegistrationDTOFromSession().getSelectionListDTO().getUinId());
			updateUinId.setText(getRegistrationDTOFromSession().getSelectionListDTO().getUinId());
			applicationFullName.setDisable(false);
			fullNameKeyboardImage.setDisable(false);
			localFullName.setDisable(false);
			applicationAge.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAge());
			applicationGender.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isGender());

			applicationAddressLine1.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			addressLine1KeyboardImage.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			addressLine2KeyboardImage.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			addressLine3KeyboardImage.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationAddressLine2.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationAddressLine3.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			localAddressLine1.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			localAddressLine2.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			localAddressLine3.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationRegion.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationProvince.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationCity.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationPostalCode.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationzone.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
			applicationMobileNumber.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isPhone());
			applicationemailIdPane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isEmail());

			residenceParentpane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isForeigner());

			applicationCniOrPinNumberPane
					.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isCnieNumber());

			parentDetailPane
					.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
			parentDetailPane
					.setVisible(getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
			parentDetailPane
					.setManaged(getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
			parentNameKeyboardImage
					.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());

			isChild = getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails();

			ResourceBundle localProperties = applicationContext.getApplicationLanguageBundle();
			Label label = guardianBiometricsController.getGuardianBiometricsLabel();

			if (!isChild) {
				label.setText(localProperties.getString("applicantbiometrics"));
			} else {
				label.setText(localProperties.getString("guardianBiometric"));
			}

			if (SessionContext.map().get(RegistrationConstants.IS_Child) != null) {
				isChild = (boolean) SessionContext.map().get(RegistrationConstants.IS_Child);
				parentDetailPane.setDisable(!isChild);
				parentDetailPane.setVisible(isChild);
				parentNameKeyboardImage.setDisable(!isChild);

			}

			enableParentUIN();
			disableLocalFieldOnSameLanguage();
		}
	}

	private void enableParentUIN() {
		if (isChild || (null != ageField.getText() && !ageField.getText().isEmpty()
				&& Integer.parseInt(ageField.getText()) <= 5)) {

			applicationUinIdPane.setDisable(false);
			applicationRidPane.setDisable(true);
			applicationRidPane.setVisible(false);
			applicationRidPane.setManaged(false);
			ridOrUinToggle.setVisible(false);
			ridOrUinToggle.setManaged(false);
			localRidPane.setDisable(true);
			localRidPane.setVisible(false);
			localRidPane.setManaged(false);
			localRidOrUinToggle.setVisible(false);
			localRidOrUinToggle.setManaged(false);

			parentDetailsHbox.setAlignment(Pos.CENTER_LEFT);
			localParentDetailsHbox.setAlignment(Pos.CENTER_LEFT);
		}
	}

	/**
	 * This method is to prepopulate all the values for edit operation
	 */
	public void prepareEditPageContent() {
		try {
			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Preparing the Edit page content");

			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
			Map<String, Object>  demographics = registrationDTO.getDemographics();
						
			List<String> locationBasedFields = Arrays.asList(orderOfAddress);
			List<UiSchemaDTO> list;
			try {
				list = identitySchemaService.getLatestEffectiveUISchema();
			} catch(RegBaseCheckedException e) {
				LOGGER.error("REGISTRATION - GETTING SCHEMA FIELDS FAILED", APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(e));
				generateAlert(RegistrationConstants.ERROR, "ID Schema is emtpy"); //TODO
				return;
			}
			
			for(UiSchemaDTO schemaField : list) {
				switch (schemaField.getType()) {
				case "simpleType":	
					List<ValuesDTO> values = demographics.get(schemaField.getId()) != null ? 
							(List<ValuesDTO>)demographics.get(schemaField.getId()) : null;
					
					if(values != null) {
						if(locationBasedFields.contains(schemaField.getId())) {
							populateFieldValue(listOfComboBoxWithLocation.get(schemaField.getId()), 
									listOfComboBoxWithLocation.get(schemaField.getId() + "LocalLanguage"), values);
						}
						else
							populateFieldValue(listOfComboBoxWithString.get(schemaField.getId()), 
									listOfComboBoxWithString.get(schemaField.getId() + "LocalLanguage"), values);
					}				
					
					break;
				
				case "number":
				case "string":	
					String value = demographics.get(schemaField.getId()) != null ? 
							(String)demographics.get(schemaField.getId()) : RegistrationConstants.EMPTY ;
					
					if(schemaField.getControlType().equalsIgnoreCase("ageDate")) {
						if(value != null && !value.isEmpty()) {
							String[] date = value.split("/");
							if (date.length == 3) {
								dd.setText(" ");
								dd.setText(date[2]);
								yyyy.setText(date[0]);
								mm.setText(date[1]);
							}
						}
					}
					else {
						TextField textField = listOfTextField.get(schemaField.getId());
						if(textField != null) {
							textField.setText(value);
						}
					}
										
					break;				
				}
			}
			
			if (SessionContext.map().get(RegistrationConstants.IS_Child) != null) {

				boolean isChild = (boolean) SessionContext.map().get(RegistrationConstants.IS_Child);
				parentDetailPane.setDisable(!isChild);
				parentDetailPane.setVisible(isChild);
			}
			
			preRegistrationId.setText(registrationDTO.getPreRegistrationId());

		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}

	}

	/**
	 * Method to populate the local field value
	 *
	 */
	private void populateFieldValue(Node nodeForPlatformLang, Node nodeForLocalLang, List<ValuesDTO> fieldValues) {
		if (fieldValues != null) {
			String platformLanguageCode = applicationContext.getApplicationLanguage();
			String localLanguageCode = applicationContext.getLocalLanguage();
			String valueInPlatformLang = RegistrationConstants.EMPTY;
			String valueinLocalLang = RegistrationConstants.EMPTY;

			for (ValuesDTO fieldValue : fieldValues) {
				if (fieldValue.getLanguage().equalsIgnoreCase(platformLanguageCode)) {
					valueInPlatformLang = fieldValue.getValue();
				} else if (nodeForLocalLang != null && fieldValue.getLanguage().equalsIgnoreCase(localLanguageCode)) {
					valueinLocalLang = fieldValue.getValue();
				}
			}

			if (nodeForPlatformLang instanceof TextField) {
				((TextField) nodeForPlatformLang).setText(valueInPlatformLang);

				if (nodeForLocalLang != null) {
					((TextField) nodeForLocalLang).setText(valueinLocalLang);
				}
			} else if (nodeForPlatformLang instanceof ComboBox) {
				fxUtils.selectComboBoxValue((ComboBox<?>) nodeForPlatformLang, valueInPlatformLang);
			}
		}
	}

	/**
	 * Method to fetch the pre-Registration details
	 */
	@FXML
	private void fetchPreRegistration() {
		String preRegId = preRegistrationId.getText();

		if (StringUtils.isEmpty(preRegId)) {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PRE_REG_ID_EMPTY);
			return;
		} else {
			try {
				pridValidatorImpl.validateId(preRegId);
			} catch (InvalidIDException invalidIDException) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PRE_REG_ID_NOT_VALID);
				LOGGER.error("PRID VALIDATION FAILED", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						invalidIDException.getMessage() + ExceptionUtils.getStackTrace(invalidIDException));
				return;
			}
		}

		auditFactory.audit(AuditEvent.REG_DEMO_PRE_REG_DATA_FETCH, Components.REG_DEMO_DETAILS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW);
		documentScanController.clearDocSection();

		ResponseDTO responseDTO = preRegistrationDataSyncService.getPreRegistration(preRegId);

		SuccessResponseDTO successResponseDTO = responseDTO.getSuccessResponseDTO();
		List<ErrorResponseDTO> errorResponseDTOList = responseDTO.getErrorResponseDTOs();

		if (successResponseDTO != null && successResponseDTO.getOtherAttributes() != null
				&& successResponseDTO.getOtherAttributes().containsKey(RegistrationConstants.REGISTRATION_DTO)) {
			SessionContext.map().put(RegistrationConstants.REGISTRATION_DATA,
					successResponseDTO.getOtherAttributes().get(RegistrationConstants.REGISTRATION_DTO));
			prepareEditPageContent();

		} else if (errorResponseDTOList != null && !errorResponseDTOList.isEmpty()) {
			generateAlertLanguageSpecific(RegistrationConstants.ERROR, errorResponseDTOList.get(0).getMessage());
		}
	}

	/**
	 * 
	 * Loading the address detail from previous entry
	 * 
	 */
	@FXML
	private void loadAddressFromPreviousEntry() {
		try {
			LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Loading address from previous entry");

			if (SessionContext.map().get(RegistrationConstants.ADDRESS_KEY) == null) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PREVIOUS_ADDRESS);
				LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "Previous registration details not available.");

			} else {
				LocationDTO locationDto = ((AddressDTO) SessionContext.map().get(RegistrationConstants.ADDRESS_KEY))
						.getLocationDTO();
				if (locationDto.getRegion() != null) {
					fxUtils.selectComboBoxValue(region, locationDto.getRegion());
					retrieveAndPopulateLocationByHierarchy(region, province, provinceLocalLanguage);
				}
				if (locationDto.getProvince() != null) {
					fxUtils.selectComboBoxValue(province, locationDto.getProvince());
					retrieveAndPopulateLocationByHierarchy(province, city, cityLocalLanguage);
				}
				if (locationDto.getCity() != null) {
					fxUtils.selectComboBoxValue(city, locationDto.getCity());
					retrieveAndPopulateLocationByHierarchy(city, zone, zoneLocalLanguage);
				}
				if (locationDto.getLocalAdministrativeAuthority() != null) {
					fxUtils.selectComboBoxValue(zone, locationDto.getLocalAdministrativeAuthority());
				}

				if (locationDto.getPostalCode() != null) {
					postalCode.setText(locationDto.getPostalCode());
				}
				LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "Loaded address from previous entry");
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - LOADING ADDRESS FROM PREVIOUS ENTRY FAILED ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	/**
	 * 
	 * Setting the focus to specific fields when keyboard loads
	 * 
	 */
	@FXML
	private void setFocusonLocalField(MouseEvent event) {
		try {
			keyboardNode.setLayoutX(fullNameGridPane.getWidth());
			Node node = (Node) event.getSource();

			if (node.getId().equals(RegistrationConstants.ADDRESS_LINE1)) {
				addressLine1LocalLanguage.requestFocus();
				keyboardNode.setLayoutY(addressLine1NodePos);
				keyboardNode.setManaged(true);
			}

			if (node.getId().equals(RegistrationConstants.ADDRESS_LINE2)) {
				addressLine2LocalLanguage.requestFocus();
				keyboardNode.setLayoutY(addressLine2NodePos);
				keyboardNode.setManaged(true);

			}

			if (node.getId().equals(RegistrationConstants.ADDRESS_LINE3)) {
				addressLine3LocalLanguage.requestFocus();
				keyboardNode.setLayoutY(addressLine3NodePos);
				keyboardNode.setManaged(true);

			}

			if (node.getId().equals(RegistrationConstants.FULL_NAME)) {
				fullNameLocalLanguage.requestFocus();
				keyboardNode.setLayoutY(fullNameNodePos);
				keyboardNode.setManaged(true);

			}

			if (node.getId().equals(RegistrationConstants.PARENT_NAME)) {
				parentNameLocalLanguage.requestFocus();
				keyboardNode.setLayoutY(parentNameNodePos);
				keyboardNode.setManaged(true);

			}
			keyboardNode.setVisible(!keyboardNode.isVisible());
			keyboardNode.visibleProperty().addListener((abs, old, newValue) -> {
				if (old) {
					keyboardPane.maxHeight(parentFlowPane.getHeight());
					fullNameLocalLanguage.requestFocus();
				}
			});

		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - SETTING FOCUS ON LOCAL FIELD FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	public void clickMe() {
		validation.setValidationMessage();
		fullName.setText("أيوب توفيق");
		int age = 27;
		ageField.setText("" + age);
		addressLine1.setText("٣٠ ر أم عربية");
		addressLine2.setText("عربية");
		if (!region.getItems().isEmpty()) {
			region.getSelectionModel().select(0);
			retrieveAndPopulateLocationByHierarchy(region, province, provinceLocalLanguage);
		}
		if (!province.getItems().isEmpty()) {
			province.getSelectionModel().select(0);
			retrieveAndPopulateLocationByHierarchy(province, city, cityLocalLanguage);
		}
		if (!city.getItems().isEmpty()) {
			city.getSelectionModel().select(0);
			retrieveAndPopulateLocationByHierarchy(city, zone, zoneLocalLanguage);
		}
		if (!zone.getItems().isEmpty()) {
			zone.getSelectionModel().select(0);
		}
		mobileNo.setText("9965625706");
		emailId.setText("ayoub.toufiq@gmail.com");
		cniOrPinNumber.setText("4545343123");
		registrationController.displayValidationMessage(validation.getValidationMessage().toString());
	}

	/**
	 * Method to go back to previous page
	 */
	@FXML
	private void back() {
		try {
			if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
				Parent uinUpdate = BaseController.load(getClass().getResource(RegistrationConstants.UIN_UPDATE));
				getScene(uinUpdate);
			} else {
				goToHomePageFromRegistration();
			}
		} catch (IOException exception) {
			LOGGER.error("COULD NOT LOAD HOME PAGE", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
	}

	/**
	 * Method to go back to next page
	 */
	@FXML
	private void next() throws InvalidApplicantArgumentException, ParseException {

		if (preRegistrationId.getText().isEmpty()) {
			preRegistrationId.clear();
		}

		if (getRegistrationDTOFromSession().getSelectionListDTO() != null
				&& parentUinId.getText().equals(getRegistrationDTOFromSession().getSelectionListDTO().getUinId())) {
			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.UPDATE_UIN_INDIVIDUAL_AND_PARENT_SAME_UIN_ALERT);
		} else {
			if (validateThisPane()) {
				saveDetail();

				/*
				 * SessionContext.map().put("demographicDetail", false);
				 * SessionContext.map().put("documentScan", true);
				 */

				documentScanController.populateDocumentCategories();

				auditFactory.audit(AuditEvent.REG_DEMO_NEXT, Components.REG_DEMO_DETAILS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

				// Set Exception Photo Type Description
				boolean isParentOrGuardianBiometricsCaptured = getRegistrationDTOFromSession().isUpdateUINChild()
						|| (SessionContext.map().get(RegistrationConstants.IS_Child) != null
								&& (boolean) SessionContext.map().get(RegistrationConstants.IS_Child));
				documentScanController.setExceptionDescriptionText(isParentOrGuardianBiometricsCaptured);
				faceCaptureController.setExceptionFaceDescriptionText(isParentOrGuardianBiometricsCaptured);

				if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
					SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DEMOGRAPHICDETAIL, false);
					if (RegistrationConstants.ENABLE
							.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.DOC_DISABLE_FLAG))
							|| (RegistrationConstants.ENABLE.equalsIgnoreCase(
									getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG))
									|| RegistrationConstants.ENABLE.equalsIgnoreCase(
											getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG)))) {
						SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN, true);
					} else {
						updateUINMethodFlow();
					}
					registrationController.showUINUpdateCurrentPage();
				} else {
					registrationController.showCurrentPage(RegistrationConstants.DEMOGRAPHIC_DETAIL,
							getPageDetails(RegistrationConstants.DEMOGRAPHIC_DETAIL, RegistrationConstants.NEXT));
					addExceptionDTOs();

				}
			}
		}
	}

	/**
	 * Disables the messages once the pane is validated
	 */
	private void disableTheMessages() {
		fullNameMessage.setVisible(false);
		fullNameLocalLanguageMessage.setVisible(false);
		dobMessage.setVisible(false);
		addressLine1Message.setVisible(false);
		addressLine1LocalLanguageMessage.setVisible(false);
		addressLine2Message.setVisible(false);
		addressLine2LocalLanguageMessage.setVisible(false);
		addressLine3Message.setVisible(false);
		addressLine3LocalLanguageMessage.setVisible(false);
		postalCodeMessage.setVisible(false);
		postalCodeLocalLanguageMessage.setVisible(false);
		mobileNoMessage.setVisible(false);
		mobileNoLocalLanguageMessage.setVisible(false);
		emailIdMessage.setVisible(false);
		emailIdLocalLanguageMessage.setVisible(false);
		cniOrPinNumberMessage.setVisible(false);
		cniOrPinNumberLocalLanguageMessage.setVisible(false);
		parentNameMessage.setVisible(false);
		parentNameLocalLanguageMessage.setVisible(false);
		parentRegIdMessage.setVisible(false);
		parentUinIdMessage.setVisible(false);
	}

	/**
	 * Method to validate the details entered
	 */
	public boolean validateThisPane() {
		boolean isValid = true;
		isValid = registrationController.validateDemographicPane(parentFlowPane);
//		if (isValid)
//			isValid = validation.validateUinOrRid(parentFlowPane, parentUinId, parentRegId, isChild, uinValidator,
//					ridValidator);

		return isValid;

	}

	private boolean validateDateOfBirth(boolean isValid) {
		int age;
		if (getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
				.equals(RegistrationConstants.PACKET_TYPE_LOST) && dd.getText().isEmpty() && mm.getText().isEmpty()
				&& yyyy.getText().isEmpty()) {
			return true;
		}
		LocalDate date = null;
		try {
			date = LocalDate.of(Integer.parseInt(yyyy.getText()), Integer.parseInt(mm.getText()),
					Integer.parseInt(dd.getText()));
		} catch (NumberFormatException | DateTimeException exception) {
			if (exception.getMessage().contains("Invalid value for DayOfMonth")) {
				dobMessage.setText(RegistrationUIConstants.INVALID_DATE);
			} else if (exception.getMessage().contains("Invalid value for MonthOfYear")) {
				dobMessage.setText(RegistrationUIConstants.INVALID_MONTH);
			} else {
				dobMessage.setText(RegistrationUIConstants.INVALID_YEAR);
			}
			if (dd.getText().isEmpty()) {
				dobMessage.setText(dd.getPromptText() + " " + RegistrationUIConstants.REG_LGN_001.split("#")[0]);
			} else if (mm.getText().isEmpty()) {
				dobMessage.setText(mm.getPromptText() + " " + RegistrationUIConstants.REG_LGN_001.split("#")[0]);
			} else if (yyyy.getText().isEmpty()) {
				dobMessage.setText(yyyy.getPromptText() + " " + RegistrationUIConstants.REG_LGN_001.split("#")[0]);
			}
			dobMessage.setVisible(true);
			return false;
		}
		LocalDate localDate = LocalDate.now();

		if (localDate.compareTo(date) >= 0) {

			age = Period.between(date, localDate).getYears();
			if (age <= maxAge) {
				ageField.setText(age + "");
				ageFieldLocalLanguage.setText(age + "");
			} else {
				dobMessage.setText(RegistrationUIConstants.INVALID_AGE + maxAge);
				dobMessage.setVisible(true);
				isValid = false;
			}

		} else {
			ageField.clear();
			ageFieldLocalLanguage.clear();
			dobMessage.setText(RegistrationUIConstants.FUTURE_DOB);
			dobMessage.setVisible(true);
			isValid = false;

		}
		return isValid;
	}

	/**
	 * Fetching the combobox details
	 */
	@SuppressWarnings("unchecked")
	private <T> void renderComboBoxes() {
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RENDER_COMBOBOXES", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Rendering of comboboxes started");

		try {
			StringConverter<T> uiRenderForComboBox = fxUtils.getStringConverterForComboBox();

			region.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			province.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			city.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			zone.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			regionLocalLanguage.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			provinceLocalLanguage.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			cityLocalLanguage.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
			zoneLocalLanguage.setConverter((StringConverter<LocationDto>) uiRenderForComboBox);
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.REGISTRATION_CONTROLLER,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException), runtimeException);
		}
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RENDER_COMBOBOXES", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Rendering of comboboxes ended");
	}

	private void selectionInStringComboBox(ComboBox<String> src, ComboBox<String> location) {
	}

	/**
	 * Retrieving and populating the location by hierarchy
	 */
	private void retrieveAndPopulateLocationByHierarchy(ComboBox<LocationDto> srcLocationHierarchy,
			ComboBox<LocationDto> destLocationHierarchy, ComboBox<LocationDto> destLocationHierarchyInLocal) {
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
				RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
				"Retrieving and populating of location by selected hirerachy started");

		try {

			LocationDto selectedLocationHierarchy = srcLocationHierarchy.getSelectionModel().getSelectedItem();
			if (selectedLocationHierarchy != null) {
				destLocationHierarchy.getItems().clear();
				destLocationHierarchyInLocal.getItems().clear();

				if (selectedLocationHierarchy.getCode().equalsIgnoreCase(RegistrationConstants.AUDIT_DEFAULT_USER)) {
					destLocationHierarchy.getItems().add(selectedLocationHierarchy);
					destLocationHierarchyInLocal.getItems().add(selectedLocationHierarchy);
				} else {
					List<LocationDto> locations = masterSync.findLocationByHierarchyCode(
							selectedLocationHierarchy.getCode(), selectedLocationHierarchy.getLangCode());

					List<LocationDto> locationsSecondary = masterSync.findProvianceByHierarchyCode(
							selectedLocationHierarchy.getCode(), ApplicationContext.localLanguage());

					if (locations.isEmpty()) {
						LocationDto lC = new LocationDto();
						lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
						lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
						lC.setLangCode(ApplicationContext.applicationLanguage());
						destLocationHierarchy.getItems().add(lC);
						destLocationHierarchyInLocal.getItems().add(lC);
					} else {
						destLocationHierarchy.getItems().addAll(locations);
					}

					if (locationsSecondary.isEmpty()) {
						LocationDto lC = new LocationDto();
						lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
						lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
						lC.setLangCode(ApplicationContext.localLanguage());
						destLocationHierarchyInLocal.getItems().add(lC);
					} else {
						destLocationHierarchyInLocal.getItems().addAll(locationsSecondary);
					}
				}
			}
		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
			LOGGER.error("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
					APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
				RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
				"Retrieving and populating of location by selected hirerachy ended");
	}

	protected String getSelectedNationalityCode() {
		return residence.getText() != null ? residence.getId() : null;

	}

	private void updateBioPageFlow(String flag, String pageId) {
		if (RegistrationConstants.DISABLE.equalsIgnoreCase(String.valueOf(ApplicationContext.map().get(flag)))) {
			updatePageFlow(pageId, false);
		} else {
			updatePageFlow(pageId, true);
		}
	}

	private void addListenersFromUiProperties(Map<String, UiSchemaDTO> uiSchemaProperties, Pane pane) {
		for (Node node : pane.getChildren()) {
			if (node instanceof Pane) {
				addListenersFromUiProperties(uiSchemaProperties, (Pane) node);
			} else {
				if (uiSchemaProperties.containsKey(node.getId())) {

					System.out.println("********** " + node.getId());
					// Add Listener
				}
			}
		}

	}
}
