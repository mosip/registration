package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.jsonwebtoken.lang.Collections;
import io.mosip.kernel.core.applicanttype.exception.InvalidApplicantArgumentException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
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
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.biometric.BiometricInfoDTO;
import io.mosip.registration.dto.biometric.FaceDetailsDTO;
import io.mosip.registration.dto.demographic.AddressDTO;
import io.mosip.registration.dto.demographic.CBEFFFilePropertiesDTO;
import io.mosip.registration.dto.demographic.DocumentDetailsDTO;
import io.mosip.registration.dto.demographic.LocationDTO;
import io.mosip.registration.dto.demographic.ValuesDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.mastersync.LocationDto;
import io.mosip.registration.entity.Location;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.validator.RegIdObjectMasterDataValidator;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
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
	private ComboBox<GenericDto> region;

	@FXML
	private Label regionMessage;

	@FXML
	private Label regionLabel;

	@FXML
	private ComboBox<GenericDto> regionLocalLanguage;

	@FXML
	private VBox regionLocalLanguagePane;

	@FXML
	private ComboBox<GenericDto> city;

	@FXML
	private Label cityMessage;

	@FXML
	private Label cityLabel;

	@FXML
	private ComboBox<GenericDto> cityLocalLanguage;

	@FXML
	private VBox cityLocalLanguagePane;

	@FXML
	private ComboBox<GenericDto> province;

	@FXML
	private Label provinceLabel;

	@FXML
	private Label provinceMessage;

	@FXML
	private ComboBox<GenericDto> provinceLocalLanguage;

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
	private ComboBox<GenericDto> zone;

	@FXML
	private Label zoneMessage;

	@FXML
	private Label zoneLabel;

	@FXML
	private ComboBox<GenericDto> zoneLocalLanguage;

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
	private GridPane scrollParentPane;
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

	private List<String> orderOfAddress;

	boolean hasToBeTransliterated = true;

	public Map<String, ComboBox<String>> listOfComboBoxWithString;

	public Map<String, ComboBox<GenericDto>> listOfComboBoxWithObject;

	public Map<String, TextField> listOfTextField;

	private int age = 0;

	@Autowired
	private IdentitySchemaService identitySchemaService;

	@Autowired
	private MasterSyncDao masterSyncDao;
	
	private VirtualKeyboard vk;
	
	private HashMap<String, Integer> positionTracker;
	
	int lastPosition;
	
	private ObservableList<Node> parentFlow;
	
	private boolean keyboardVisible=false;
	


	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize()
	 */
	@FXML
	private void initialize() throws IOException {

		
		listOfComboBoxWithString = new HashMap<>();
		listOfComboBoxWithObject = new HashMap<>();
		listOfTextField = new HashMap<>();
		
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Entering the Demographic Details Screen");
		lastPosition=-1;
		positionTracker = new HashMap<>();
		orderOfAddress = new ArrayList<>();
		List<Location> location = masterSyncDao.getLocationDetails(applicationContext.getApplicationLanguage());
		TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();
		location.forEach((l) -> treeMap.put(l.getHierarchyLevel(), l.getHierarchyName()));
		orderOfAddress = treeMap.values().stream().collect(Collectors.toList());
		listOfComboBoxWithString = new HashMap<>();
		listOfComboBoxWithObject = new HashMap<>();
		listOfTextField = new HashMap<>();

		vk = VirtualKeyboard.getInstance();
		keyboardNode = vk.view();
		
		if (ApplicationContext.getInstance().getApplicationLanguage()
				.equals(ApplicationContext.getInstance().getLocalLanguage())) {
			hasToBeTransliterated = false;
		}

		try {
			if (getRegistrationDTOFromSession() == null) {
				validation.updateAsLostUIN(false);
				registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW);
			}

			if (validation.isLostUIN()) {
				registrationNavlabel.setText(applicationLabelBundle.getString("/lostuin"));
				disablePreRegFetch();
			}

			if (getRegistrationDTOFromSession() != null
					&& getRegistrationDTOFromSession().getSelectionListDTO() == null) {
				getRegistrationDTOFromSession().setUpdateUINNonBiometric(false);
				getRegistrationDTOFromSession().setUpdateUINChild(false);
			}
			validation.setChild(false);
			lostUIN = false;
			fxUtils = FXUtils.getInstance();
			fxUtils.setTransliteration(transliteration);
			isChild = false;

			fullNameNodePos = 200.00;
			addressLine1NodePos = 600.00;
			addressLine2NodePos = 705.00;
			addressLine3NodePos = 800.00;
			parentNameNodePos = 1600.00;
			minAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MIN_AGE));
			maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
			applicationLabelBundle = applicationContext.getApplicationLanguageBundle();
			localLabelBundle = applicationContext.getLocalLanguageProperty();
			primaryLanguage = applicationContext.getApplicationLanguage();
			secondaryLanguage = applicationContext.getLocalLanguage();

			Map<String, UiSchemaDTO> schemaMap = validation.getValidationMap();
			parentFlow = parentFlowPane.getChildren();
			int position=parentFlow.size()-1;
			for (Entry<String, UiSchemaDTO> entry : schemaMap.entrySet()) {

				if (isDemographicField(entry.getValue())) {
					GridPane mainGridPane = addContent(entry.getValue());
					parentFlow.add(mainGridPane);
					position++;
					positionTracker.put(mainGridPane.getId(), position);
				}
			}
			addFirstOrderAddress(listOfComboBoxWithObject.get("region"), "region", "");
			addFirstOrderAddress(
					listOfComboBoxWithObject.get("region" + RegistrationConstants.LOCAL_LANGUAGE),
					"region", RegistrationConstants.LOCAL_LANGUAGE);
			populateDropDowns();

			for (int j = 0; j < orderOfAddress.size() - 1; j++) {
				final int k = j;
				try {
					listOfComboBoxWithObject.get(orderOfAddress.get(k)).setOnAction((event) -> {
						configureMethodsForAddress(k, k + 1, orderOfAddress.size());
					});
				} catch (Exception exception) {
					
				}
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_DEMOGRAPHIC_PAGE);

		}
	}

	private void disablePreRegFetch() {
		preRegParentPane.setVisible(false);
		preRegParentPane.setManaged(false);
		preRegParentPane.setDisable(true);
		positionKeyboardForLostUIN();
	}

	public GridPane addContent(UiSchemaDTO schemaDTO) {
		GridPane gridPane = prepareMainGridPane();

		GridPane primary = subGridPane(schemaDTO, "");
		GridPane secondary = subGridPane(schemaDTO, RegistrationConstants.LOCAL_LANGUAGE);

		gridPane.addColumn(0, primary);

		gridPane.addColumn(2, secondary);

		gridPane.setId(schemaDTO.getId());
		
		gridPane.setId(schemaDTO.getId()+"MainGrid");

		return gridPane;
	}
	
	public void addKeyboard(int position) {
	
		
		if(keyboardVisible ) {
			parentFlow.remove(lastPosition);
			keyboardVisible=false;
			lastPosition=position;
		}else {
			GridPane gridPane = prepareMainGridPaneForKeyboard();
			gridPane.addColumn(1, keyboardNode);
			parentFlow.add(position, gridPane);
			lastPosition=position;
			keyboardVisible=true;
		}
	}

	private GridPane prepareMainGridPane() {
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(1000);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(48);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(7);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(45);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
		return gridPane;
	}
	
	private GridPane prepareMainGridPaneForKeyboard() {
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(1000);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(80);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(10);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
		return gridPane;
	}

	@SuppressWarnings("unlikely-arg-type")
	public GridPane subGridPane(UiSchemaDTO schemaDTO, String languageType) {
		GridPane gridPane = new GridPane();

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

		VBox content = null;

		switch (schemaDTO.getControlType()) {
		case RegistrationConstants.DROPDOWN:
			if (orderOfAddress.contains(schemaDTO.getSubType()))
				content = addContentWithComboBoxObject(schemaDTO.getSubType(), schemaDTO,languageType);
			else
				content = addContentWithComboBoxObject(schemaDTO.getId(), schemaDTO,languageType);
			break;
		case RegistrationConstants.AGE_DATE:
			content = addContentForDobAndAge(languageType);
			break;
		case "age":
			// TODO Not yet supported
			break;
		case RegistrationConstants.TEXTBOX:
			content = addContentWithTextField(schemaDTO, schemaDTO.getId(),languageType);
			break;
		}

		gridPane.add(content, 1, 2);

		return gridPane;
	}

	public VBox addContentForDobAndAge(String languageType) {

		VBox vBoxDD = new VBox();

		TextField dd = new TextField();
		dd.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		dd.setId(RegistrationConstants.DD + languageType);
		Label ddLabel = new Label();
		ddLabel.setVisible(false);
		ddLabel.setId(RegistrationConstants.DD + languageType + RegistrationConstants.LABEL);
		ddLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		vBoxDD.getChildren().addAll(ddLabel, dd);

		listOfTextField.put(RegistrationConstants.DD + languageType, dd);

		VBox vBoxMM = new VBox();
		TextField mm = new TextField();
		mm.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		mm.setId(RegistrationConstants.MM + languageType);
		Label mmLabel = new Label();
		mmLabel.setVisible(false);
		mmLabel.setId(RegistrationConstants.MM + languageType + RegistrationConstants.LABEL);
		mmLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		vBoxMM.getChildren().addAll(mmLabel, mm);

		listOfTextField.put(RegistrationConstants.MM + languageType, mm);

		VBox vBoxYYYY = new VBox();
		TextField yyyy = new TextField();
		yyyy.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		yyyy.setId(RegistrationConstants.YYYY + languageType);
		Label yyyyLabel = new Label();
		yyyyLabel.setVisible(false);
		yyyyLabel.setId(RegistrationConstants.YYYY + languageType + RegistrationConstants.LABEL);
		yyyyLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		vBoxYYYY.getChildren().addAll(yyyyLabel, yyyy);

		listOfTextField.put(RegistrationConstants.YYYY + languageType, yyyy);

		Label dobMessage = new Label();
		dobMessage.setId(RegistrationConstants.DOB_MESSAGE + languageType);
		dobMessage.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);

		boolean localLanguage = languageType.equals(RegistrationConstants.LOCAL_LANGUAGE);

		dd.setPromptText(localLanguage ? localLabelBundle.getString(RegistrationConstants.DD) : applicationLabelBundle.getString(RegistrationConstants.DD));
		ddLabel.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.DD) : applicationLabelBundle.getString(RegistrationConstants.DD));
		mm.setPromptText(localLanguage ? localLabelBundle.getString(RegistrationConstants.MM) : applicationLabelBundle.getString(RegistrationConstants.MM));
		mmLabel.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.MM) : applicationLabelBundle.getString(RegistrationConstants.MM));
		dobMessage.setText("");

		yyyy.setPromptText(
				localLanguage ? localLabelBundle.getString(RegistrationConstants.YYYY) : applicationLabelBundle.getString(RegistrationConstants.YYYY));
		yyyyLabel
				.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.YYYY) : applicationLabelBundle.getString(RegistrationConstants.YYYY));

		HBox hB = new HBox();
		hB.setSpacing(10);
		hB.getChildren().addAll(vBoxDD, vBoxMM, vBoxYYYY);

		HBox hB2 = new HBox();
		VBox vboxAgeField = new VBox();
		TextField ageField = new TextField();
		ageField.setId(RegistrationConstants.AGE_FIELD + languageType);
		ageField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		Label ageFieldLabel = new Label();
		ageFieldLabel.setId(RegistrationConstants.AGE_FIELD + languageType + RegistrationConstants.LABEL);
		ageFieldLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		ageFieldLabel.setVisible(false);
		vboxAgeField.getChildren().addAll(ageFieldLabel, ageField);

		listOfTextField.put(RegistrationConstants.AGE_FIELD + languageType, ageField);

		ageField.setPromptText(localLanguage ? localLabelBundle.getString(RegistrationConstants.AGE_FIELD)
				: applicationLabelBundle.getString(RegistrationConstants.AGE_FIELD));
		ageFieldLabel.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.AGE_FIELD)
				: applicationLabelBundle.getString(RegistrationConstants.AGE_FIELD));

		hB.setPrefWidth(250);

		hB2.setSpacing(10);

		Label orLabel = new Label(localLanguage ? localLabelBundle.getString("ageOrDOBField")
				: applicationLabelBundle.getString("ageOrDOBField"));

		VBox orVbox = new VBox();
		orVbox.setPrefWidth(100);
		orVbox.getChildren().addAll(new Label(), orLabel);

		hB2.getChildren().addAll(hB, orVbox, vboxAgeField);

		VBox vB = new VBox();
		vB.getChildren().addAll(hB2, dobMessage);

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

	public VBox addContentWithTextField(UiSchemaDTO schema, String fieldName, String languageType) {

		TextField field = new TextField();
		Label label = new Label();
		Label validationMessage = new Label();

		VBox vbox = new VBox();
		vbox.setId(fieldName + "Parent");
		field.setId(fieldName + languageType);
		field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		label.setId(fieldName + languageType + RegistrationConstants.LABEL);
		label.getStyleClass().add( RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		label.setVisible(false);
		validationMessage.setId(fieldName + languageType + "Message");
		validationMessage.getStyleClass().add("demoGraphicFieldMessageLabel");
		label.setPrefWidth(vbox.getPrefWidth());
		field.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setPrefWidth(vbox.getPrefWidth());
		vbox.setSpacing(5);
		vbox.getChildren().add(label);
		vbox.getChildren().add(field);

		HBox hB = new HBox();
		hB.setSpacing(20);
		hB.setId(fieldName + languageType+"Hbox");

		vbox.getChildren().add(validationMessage);
		if (primaryLanguage.equals(secondaryLanguage)) {
			vbox.setDisable(true);
		}

		if (listOfTextField.get(fieldName) != null)
			fxUtils.populateLocalFieldWithFocus(parentFlowPane, listOfTextField.get(fieldName), field,
					hasToBeTransliterated, validation);

		listOfTextField.put(field.getId(), field);
		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
			field.setPromptText(schema.getLabel().get("secondary"));
			putIntoLabelMap(fieldName+languageType, schema.getLabel().get("secondary"));
			label.setText(schema.getLabel().get("secondary"));
			if (fieldName.matches("phone|email|postalCode|parentOrGuardianRID|parentOrGuardianUIN")) {
				field.setDisable(true);
			} else {
				ImageView imageView = null;
				try {
					imageView = new ImageView(new Image(new FileInputStream(new File(
							"C:\\Users\\M1047962\\git\\registrationNew\\registration\\registration-client\\src\\main\\resources\\images\\keyboard.png"))));
					imageView.setId(fieldName);
					imageView.setFitHeight(20.00);
					imageView.setFitWidth(22.00);
					imageView.setOnMouseClicked((event) -> {
						setFocusonLocalField(event);
					});
					vk.changeControlOfKeyboard(field);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				hB.getChildren().add(imageView);
			}
		} else {
			field.setPromptText(schema.getLabel().get("primary"));
			putIntoLabelMap(fieldName+languageType, schema.getLabel().get("primary"));
			label.setText(schema.getLabel().get("primary"));
		}

		hB.getChildren().add(validationMessage);
		hB.setStyle("-fx-background-color:WHITE");
		vbox.getChildren().add(hB);

		fxUtils.onTypeFocusUnfocusListener(parentFlowPane, field);
		return vbox;
	}

	private void populateDropDowns() {

		try {

			listOfComboBoxWithObject.get("gender").getItems()
					.addAll(masterSyncService.getGenderDtls(ApplicationContext.applicationLanguage()).stream()
							.filter(v -> !v.getCode().equals("OTH")).collect(Collectors.toList()));
			listOfComboBoxWithObject.get("genderLocalLanguage").getItems()
					.addAll(masterSyncService.getGenderDtls(ApplicationContext.localLanguage()).stream()
							.filter(v -> !v.getCode().equals("OTH")).collect(Collectors.toList()));
			listOfComboBoxWithObject.get("residenceStatus").getItems()
					.addAll(masterSyncService.getIndividualType(ApplicationContext.applicationLanguage()));
			listOfComboBoxWithObject.get("residenceStatusLocalLanguage").getItems()
					.addAll(masterSyncService.getIndividualType(ApplicationContext.localLanguage()));
		} catch (RegBaseCheckedException e) {
			e.printStackTrace();
		}
	}

	public <T> VBox addContentWithComboBoxObject(String fieldName, UiSchemaDTO schema,String languageType) {

		ComboBox<GenericDto> field = new ComboBox<GenericDto>();
		Label label = new Label();
		Label validationMessage = new Label();
		StringConverter<T> uiRenderForComboBox = fxUtils.getStringConverterForComboBox();

		VBox vbox = new VBox();
		field.setId(fieldName + languageType);
		field.setPrefWidth(vbox.getPrefWidth());
		if (listOfComboBoxWithObject.get(fieldName) != null) {
			fxUtils.populateLocalComboBox(parentFlowPane, listOfComboBoxWithObject.get(fieldName), field);
		}
		helperMethodForComboBox(field, fieldName, schema, label, validationMessage, vbox, languageType);
		field.setConverter((StringConverter<GenericDto>) uiRenderForComboBox);
		listOfComboBoxWithObject.put(fieldName + languageType, field);
		return vbox;
	}

	private void helperMethodForComboBox(ComboBox<?> field, String fieldName,UiSchemaDTO schema, Label label, Label validationMessage,
			VBox vbox, String languageType) {

		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
			field.setPromptText(schema.getLabel().get("secondary"));
			label.setText(schema.getLabel().get("secondary"));
			field.setDisable(true);
			putIntoLabelMap(fieldName+languageType, schema.getLabel().get("secondary"));
		} else {
			field.setPromptText(schema.getLabel().get("primary"));
			label.setText(schema.getLabel().get("primary"));
			putIntoLabelMap(fieldName+languageType, schema.getLabel().get("primary"));
		}
		// vbox.setStyle("-fx-background-color:BLUE");
		vbox.setPrefWidth(500);
		vbox.setId(fieldName + "Parent");
		label.setId(fieldName + languageType + RegistrationConstants.LABEL);
		label.setVisible(false);
		label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		field.getStyleClass().add("demographicCombobox");
		validationMessage.setId(fieldName + languageType + "Message");
		validationMessage.getStyleClass().add("demoGraphicFieldMessageLabel");
		label.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setVisible(false);
		vbox.setSpacing(5);

		vbox.getChildren().addAll(label, field, validationMessage);

		if (primaryLanguage.equals(secondaryLanguage) && languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
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

	private void disableLocalFieldOnSameLanguage() {
		// if primary and secondary language is same
		if (applicationContext.getApplicationLanguage().equals(applicationContext.getLocalLanguage())) {
			localFullNameBox.setDisable(true);
			localAddressPane.setDisable(true);
			localLanguageParentDetailPane.setDisable(true);

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
						// updateBioPageFlow(RegistrationConstants.FINGERPRINT_DISABLE_FLAG,
						// RegistrationConstants.FINGERPRINT_CAPTURE);
						// updateBioPageFlow(RegistrationConstants.IRIS_DISABLE_FLAG,
						// RegistrationConstants.IRIS_CAPTURE);

						 parentFieldValidation();
					}
					fxUtils.validateOnFocusOut(dobParentPane, ageField, validation, false);
				} else {
					ageField.getStyleClass().remove("demoGraphicTextFieldOnType");
					ageField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
					Label ageFieldLabel = (Label) dobParentPane
							.lookup("#" + ageField.getId() + RegistrationConstants.LABEL);
					ageFieldLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
					ageField.getStyleClass().remove("demoGraphicFieldLabelOnType");
					dobMessage.setText(RegistrationUIConstants.INVALID_AGE + maxAge);
					dobMessage.setVisible(true);

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

	public void positionKeyboardForLostUIN() {
		fullNameNodePos = fullNameNodePos - 80;
		addressLine1NodePos = addressLine1NodePos - 80;
		addressLine2NodePos = addressLine2NodePos - 80;
		addressLine3NodePos = addressLine3NodePos - 80;
		parentNameNodePos = parentNameNodePos - 80;
	}

	private void addFirstOrderAddress(ComboBox<GenericDto> location, String id, String languageType) {
		if (location != null) {
			location.getItems().clear();
			try {

				List<GenericDto> locations = null;

				if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
					locations = masterSync.findLocationByHierarchyCode(
							id, ApplicationContext.localLanguage());
				} else {
					locations = masterSync.findLocationByHierarchyCode(
							id,
							ApplicationContext.applicationLanguage());
				}

				if (locations.isEmpty()) {
					GenericDto lC = new GenericDto();
					lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
					lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
					lC.setLangCode(ApplicationContext.applicationLanguage());
					location.getItems().add(lC);
				} else {
					location.getItems().addAll(locations);
				}
			} catch (Exception e) {

			}

		}
	}

	private List<GenericDto> LocationDtoToComboBoxDto(List<LocationDto> locations) {
		List<GenericDto> listOfValues = new ArrayList<>();
		for (LocationDto locationDto : locations) {
			GenericDto comboBox = new GenericDto();
			comboBox.setCode(locationDto.getCode());
			comboBox.setName(locationDto.getName());
			comboBox.setLangCode(locationDto.getLangCode());
			listOfValues.add(comboBox);
		}
		return listOfValues;
	}

	private void addDemoGraphicDetailsToSession() {

		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();

		List<UiSchemaDTO> schemaFields = null;
		try {
			schemaFields = identitySchemaService.getLatestEffectiveUISchema();
		} catch (RegBaseCheckedException e) {
			e.printStackTrace();
		}
		for (UiSchemaDTO schemaField : schemaFields) {
			if (!(schemaField.getControlType() == null)) {
				switch (schemaField.getType()) {
				case RegistrationConstants.SIMPLE_TYPE:

					if (schemaField.getControlType().equals(RegistrationConstants.DROPDOWN)) {

						if (Arrays.asList(orderOfAddress).contains(schemaField.getId())
								| schemaField.getId().matches("gender|residenceStatus")) {
							ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
							ComboBox<GenericDto> localField = listOfComboBoxWithObject
									.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
							registrationDTO.addDemographicField(schemaField.getId(),
									applicationContext.getApplicationLanguage(), platformField.getValue().getName(),
									applicationContext.getLocalLanguage(), localField.getValue().getName());
						} else {
							ComboBox<String> platformField = listOfComboBoxWithString.get(schemaField.getId());
							ComboBox<String> localField = listOfComboBoxWithString
									.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
							registrationDTO.addDemographicField(schemaField.getId(),
									applicationContext.getApplicationLanguage(), platformField.getValue(),
									applicationContext.getLocalLanguage(), localField.getValue());
						}
					} else {
						TextField platformField = listOfTextField.get(schemaField.getId());
						TextField localField = listOfTextField
								.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
						registrationDTO.addDemographicField(schemaField.getId(),
								applicationContext.getApplicationLanguage(), platformField.getText(),
								applicationContext.getLocalLanguage(), localField.getText());
					}

					break;

				case RegistrationConstants.NUMBER:
				case RegistrationConstants.STRING:
					if (schemaField.getControlType().equalsIgnoreCase(RegistrationConstants.AGE_DATE)) {
						registrationDTO.addDemographicField(schemaField.getId(),
								listOfTextField.get(RegistrationConstants.DD).getText() + "/" + listOfTextField.get(RegistrationConstants.MM).getText() + "/"
										+ listOfTextField.get(RegistrationConstants.YYYY).getText());
					} else {
						registrationDTO.addDemographicField(schemaField.getId(),
								listOfTextField.get(schemaField.getId()).getText());
					}
					break;
				}
			}
		}
	}

	/**
	 * To load the provinces in the selection list based on the language code
	 */
	private void configureMethodsForAddress(int s, int p, int size) {
		try {
			retrieveAndPopulateLocationByHierarchy(listOfComboBoxWithObject.get(orderOfAddress.get(s)),
					listOfComboBoxWithObject.get(orderOfAddress.get(p)),
					listOfComboBoxWithObject.get(orderOfAddress.get(p) + RegistrationConstants.LOCAL_LANGUAGE));

			for (int i = p + 1; i < size; i++) {
				listOfComboBoxWithObject.get(orderOfAddress.get(i)).getItems().clear();
				listOfComboBoxWithObject.get(orderOfAddress.get(i) + RegistrationConstants.LOCAL_LANGUAGE).getItems()
						.clear();
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
	private void addCity(ComboBox<GenericDto> province, ComboBox<GenericDto> city,
			ComboBox<GenericDto> cityLocalLanguage, ComboBox<GenericDto> zone, ComboBox<GenericDto> zoneLocalLanguage,
			TextField postalCode, TextField postalCodeLocalLanguage) {
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
	private void addzone(ComboBox<GenericDto> city, ComboBox<GenericDto> zone, ComboBox<GenericDto> zoneLocalLanguage) {
		try {
			retrieveAndPopulateLocationByHierarchy(city, zone, zoneLocalLanguage);

		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - LOADING FAILED FOR LOCAL ADMIN AUTHORITY SELECTOIN LIST ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	private void populatePincode(ComboBox<GenericDto> zone, TextField postalCode) {
		try {
			GenericDto locationDTO = zone.getSelectionModel().getSelectedItem();

			if (null != locationDTO) {
				if (locationDTO.getCode().equalsIgnoreCase(RegistrationConstants.AUDIT_DEFAULT_USER)) {
					postalCode.setText(RegistrationConstants.AUDIT_DEFAULT_USER);
					postalCodeLocalLanguage.setText(RegistrationConstants.AUDIT_DEFAULT_USER);
				} else {
					List<GenericDto> locationDtos = masterSync.findProvianceByHierarchyCode(locationDTO.getCode(),
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

			Map<String, Object> demographics = registrationDTO.getDemographics();

			if (isChild) {
				osiDataDTO.setIntroducerType(IntroducerType.PARENT.getCode());
			}

			// registrationMetaDataDTO.setParentOrGuardianRID(parentRegId.getText());

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

			disablePreRegFetch();
//
//			clearAllValues();
//			documentScanController.getBioExceptionToggleLabel1().setLayoutX(0);
//			SessionContext.userMap().put(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION, false);
//
//			keyboardNode.setDisable(false);
//			keyboardNode.setManaged(false);
//			RegistrationConstants.CNI_MANDATORY = String.valueOf(true);
//
//			copyPrevious.setDisable(true);
//			autoFillBtn.setVisible(false);
			registrationNavlabel.setText(applicationLabelBundle.getString("uinUpdateNavLbl"));
			// parentFlowPane.setDisable(true);
			// listOfTextField.get("fullNameLocalLanguage").setDisable(false);

			for (Node pane : parentFlowPane.getChildren()) {
				if (pane.getId().equals("fullName"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isName());
				else if (pane.getId().equals("dateOfBirth"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAge());
				else if (pane.getId().equals("residenceStatus"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isForeigner());
				else if (pane.getId().equals("gender"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isGender());
				else if (pane.getId().equals("phone"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isPhone());
				else if (pane.getId().equals("email"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isEmail());
				else if (pane.getId().equals("parentOrGuardianName") | pane.getId().equals("parentOrGuardianRID")
						| pane.getId().equals("parentOrGuardianUIN"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
				else if (pane.getId().equals("addressLine1") || pane.getId().equals("addressLine2")
						|| pane.getId().equals("addressLine2") || pane.getId().equals("region")
						|| pane.getId().equals("province") || pane.getId().equals("city") || pane.getId().equals("zone")
						|| pane.getId().equals("postalCode"))
					pane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
				else
					pane.setDisable(true);
			}

//			fetchBtn.setVisible(false);
//			parentRegIdLabel.setText(applicationLabelBundle.getString("uinIdUinUpdate"));
//			preRegistrationLabel.setText(RegistrationConstants.UIN_LABEL);
//			updateUinId.setVisible(true);
//			updateUinId.setDisable(true);
//			preRegistrationId.setVisible(false);
//			getRegistrationDTOFromSession().getRegistrationMetaDataDTO()
//					.setUin(getRegistrationDTOFromSession().getSelectionListDTO().getUinId());
//			updateUinId.setText(getRegistrationDTOFromSession().getSelectionListDTO().getUinId());
//			applicationFullName.setDisable(false);
//			fullNameKeyboardImage.setDisable(false);
//			localFullName.setDisable(false);
//			applicationAge.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAge());
//			applicationGender.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isGender());
//
//			applicationAddressLine1.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			addressLine1KeyboardImage.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			addressLine2KeyboardImage.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			addressLine3KeyboardImage.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationAddressLine2.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationAddressLine3.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			localAddressLine1.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			localAddressLine2.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			localAddressLine3.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationRegion.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationProvince.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationCity.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationPostalCode.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationzone.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isAddress());
//			applicationMobileNumber.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isPhone());
//			applicationemailIdPane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isEmail());
//
//			residenceParentpane.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isForeigner());
//
//			applicationCniOrPinNumberPane
//					.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isCnieNumber());
//
//			parentDetailPane
//					.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
//			parentDetailPane
//					.setVisible(getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
//			parentDetailPane
//					.setManaged(getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
//			parentNameKeyboardImage
//					.setDisable(!getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails());
//
//			isChild = getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails();
//
//			ResourceBundle localProperties = applicationContext.getApplicationLanguageBundle();
//			Label label = guardianBiometricsController.getGuardianBiometricsLabel();
//
//			if (!isChild) {
//				label.setText(localProperties.getString("applicantbiometrics"));
//			} else {
//				label.setText(localProperties.getString("guardianBiometric"));
//			}
//
//			if (SessionContext.map().get(RegistrationConstants.IS_Child) != null) {
//				isChild = (boolean) SessionContext.map().get(RegistrationConstants.IS_Child);
//				parentDetailPane.setDisable(!isChild);
//				parentDetailPane.setVisible(isChild);
//				parentNameKeyboardImage.setDisable(!isChild);
//
//			}
//
//			enableParentUIN();
//			disableLocalFieldOnSameLanguage();
//		
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
			Map<String, Object> demographics = registrationDTO.getDemographics();

			List<UiSchemaDTO> list;
			try {
				list = identitySchemaService.getLatestEffectiveUISchema();

			} catch (RegBaseCheckedException e) {
				LOGGER.error("REGISTRATION - GETTING SCHEMA FIELDS FAILED", APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(e));
				generateAlert(RegistrationConstants.ERROR, "ID Schema is emtpy"); // TODO
				return;
			}

			for (UiSchemaDTO schemaField : list) {
				switch (schemaField.getType()) {
				case RegistrationConstants.SIMPLE_TYPE:
					List<ValuesDTO> values = demographics.get(schemaField.getId()) != null
							? (List<ValuesDTO>) demographics.get(schemaField.getId())
							: null;

					if (values != null) {
						if (orderOfAddress.contains(schemaField.getId())
								| schemaField.getId().matches("gender|residenceStatus")) {
							populateFieldValue(listOfComboBoxWithObject.get(schemaField.getId()),
									listOfComboBoxWithObject
											.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE),
									values);
						} else
							populateFieldValue(listOfComboBoxWithString.get(schemaField.getId()),
									listOfComboBoxWithString
											.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE),
									values);
					}

					break;

				case RegistrationConstants.NUMBER:
				case RegistrationConstants.STRING:
					String value = demographics.get(schemaField.getId()) != null
							? (String) demographics.get(schemaField.getId())
							: RegistrationConstants.EMPTY;

					if (schemaField.getControlType().equalsIgnoreCase(RegistrationConstants.AGE_DATE)) {
						if (value != null && !value.isEmpty()) {
							String[] date = value.split("/");
							if (date.length == 3) {
								dd.setText(" ");
								dd.setText(date[2]);
								yyyy.setText(date[0]);
								mm.setText(date[1]);
							}
						}
					} else {
						TextField textField = listOfTextField.get(schemaField.getId());
						if (textField != null) {
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
		prepareEditPageContent();
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
	private void setFocusonLocalField(MouseEvent event) {
		try {
			Node node = (Node) event.getSource();
				listOfTextField.get(node.getId()+"LocalLanguage").requestFocus();
				keyboardNode.setVisible(true);
				keyboardNode.setManaged(true);
				//have to remove from the last position as well
				addKeyboard(positionTracker.get((node.getId()+"MainGrid"))+1);

		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - SETTING FOCUS ON LOCAL FIELD FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
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
					getPageByAction(RegistrationConstants.DEMOGRAPHIC_DETAIL, RegistrationConstants.NEXT));
					//addExceptionDTOs();
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
	 * Retrieving and populating the location by hierarchy
	 */
	private void retrieveAndPopulateLocationByHierarchy(ComboBox<GenericDto> srcLocationHierarchy,
			ComboBox<GenericDto> destLocationHierarchy, ComboBox<GenericDto> destLocationHierarchyInLocal) {
		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
				RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
				"Retrieving and populating of location by selected hirerachy started");

		try {

			GenericDto selectedLocationHierarchy = srcLocationHierarchy.getSelectionModel().getSelectedItem();
			if (selectedLocationHierarchy != null) {
				destLocationHierarchy.getItems().clear();
				destLocationHierarchyInLocal.getItems().clear();

				if (selectedLocationHierarchy.getCode().equalsIgnoreCase(RegistrationConstants.AUDIT_DEFAULT_USER)) {
					destLocationHierarchy.getItems().add(selectedLocationHierarchy);
					destLocationHierarchyInLocal.getItems().add(selectedLocationHierarchy);
				} else {

					List<GenericDto> locations = masterSync.findLocationByHierarchyCode(
							selectedLocationHierarchy.getCode(), selectedLocationHierarchy.getLangCode());

					List<GenericDto> locationsSecondary = masterSync.findProvianceByHierarchyCode(
							selectedLocationHierarchy.getCode(), ApplicationContext.localLanguage());

					if (locations.isEmpty()) {
						GenericDto lC = new GenericDto();
						lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
						lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
						lC.setLangCode(ApplicationContext.applicationLanguage());
						destLocationHierarchy.getItems().add(lC);
						destLocationHierarchyInLocal.getItems().add(lC);
					} else {
						destLocationHierarchy.getItems().addAll(locations);
					}

					if (locationsSecondary.isEmpty()) {
						GenericDto lC = new GenericDto();
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

	private boolean isDemographicField(UiSchemaDTO schemaField) {
		return (schemaField.isInputRequired() && !(schemaField.getType().equalsIgnoreCase("biometricsType")
				|| schemaField.getType().equalsIgnoreCase("documentType")));
	}

}
