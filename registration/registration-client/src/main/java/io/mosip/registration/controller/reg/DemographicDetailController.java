package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
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
import io.mosip.kernel.packetmanager.dto.SimpleDto;
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
import io.mosip.registration.controller.VirtualKeyboard;
import io.mosip.registration.controller.device.FaceCaptureController;
import io.mosip.registration.controller.device.GuardianBiometricsController;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.mastersync.LocationDto;
import io.mosip.registration.entity.Location;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
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
	private GridPane parentDetailPane;
	@FXML
	private ScrollPane parentScrollPane;
	private boolean isChild;
	private Node keyboardNode;
	@Autowired
	private PridValidator<String> pridValidatorImpl;
	
	@Autowired
	private Validations validation;
	@Autowired
	private MasterSyncService masterSync;
	@FXML
	private FlowPane parentFlowPane;
	@FXML
	private GridPane scrollParentPane;
	@FXML
	private GridPane preRegParentPane;
	@Autowired
	private DateValidation dateValidation;
	@Autowired
	private RegistrationController registrationController;
	@Autowired
	private DocumentScanController documentScanController;
	@Autowired
	private Transliteration<String> transliteration;
	@Autowired
	private PreRegistrationDataSyncService preRegistrationDataSyncService;

	private FXUtils fxUtils;
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
	@Autowired
	private FaceCaptureController faceCaptureController;
	private String primaryLanguage;
	private String secondaryLanguage;
	private List<String> orderOfAddress;
	boolean hasToBeTransliterated = true;
	// public Map<String, ComboBox<String>> listOfComboBoxWithString;
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
	private boolean keyboardVisible = false;

	@Autowired
	private GuardianBiometricsController guardianBiometricsController;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize()
	 */
	@FXML
	private void initialize() throws IOException {

		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Entering the Demographic Details Screen");

		// listOfComboBoxWithString = new HashMap<>();
		listOfComboBoxWithObject = new HashMap<>();
		listOfTextField = new HashMap<>();
		lastPosition = -1;
		positionTracker = new HashMap<>();
		fillOrderOfLocation();
		vk = VirtualKeyboard.getInstance();
		keyboardNode = vk.view();
		if (ApplicationContext.getInstance().getApplicationLanguage()
				.equals(ApplicationContext.getInstance().getLocalLanguage())) {
			hasToBeTransliterated = false;
		}

		try {
			applicationLabelBundle = applicationContext.getApplicationLanguageBundle();
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
			minAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MIN_AGE));
			maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
			localLabelBundle = applicationContext.getLocalLanguageProperty();
			primaryLanguage = applicationContext.getApplicationLanguage();
			secondaryLanguage = applicationContext.getLocalLanguage();
			parentFlow = parentFlowPane.getChildren();
			int position = parentFlow.size() - 1;

			for (Entry<String, UiSchemaDTO> entry : validation.getValidationMap().entrySet()) {

				if (isDemographicField(entry.getValue())) {
					GridPane mainGridPane = addContent(entry.getValue());
					parentFlow.add(mainGridPane);
					position++;
					positionTracker.put(mainGridPane.getId(), position);
				}
			}

			addFirstOrderAddress(listOfComboBoxWithObject.get(orderOfAddress.get(0)), 1,
					applicationContext.getApplicationLanguage());
			addFirstOrderAddress(
					listOfComboBoxWithObject.get(orderOfAddress.get(0) + RegistrationConstants.LOCAL_LANGUAGE), 1,
					applicationContext.getLocalLanguage());

			populateDropDowns();

			for (int j = 0; j < orderOfAddress.size() - 1; j++) {
				final int k = j;
				try {
					listOfComboBoxWithObject.get(orderOfAddress.get(k)).setOnAction((event) -> {
						configureMethodsForAddress(k, k + 1, orderOfAddress.size());
					});
				} catch (Exception runtimeException) {
					LOGGER.info(orderOfAddress.get(k) + " is not a valid field", APPLICATION_NAME,
							RegistrationConstants.APPLICATION_ID,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

				}
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_DEMOGRAPHIC_PAGE);

		}
	}

	private void fillOrderOfLocation() {
		List<Location> locations = masterSyncDao.getLocationDetails(applicationContext.getApplicationLanguage());
		Map<Integer, String> treeMap = new TreeMap<Integer, String>();

		Collection<UiSchemaDTO> fields = validation.getValidationMap().values();
		for (Location location : locations) {
			List<UiSchemaDTO> matchedfield = fields.stream()
					.filter(field -> isDemographicField(field) && field.getSubType() != null
							&& RegistrationConstants.DROPDOWN.equals(field.getControlType())
							&& field.getSubType().equalsIgnoreCase(location.getHierarchyName()))
					.collect(Collectors.toList());

			if (matchedfield != null && !matchedfield.isEmpty()) {
				treeMap.put(location.getHierarchyLevel(), matchedfield.get(0).getId());
				LOGGER.info("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						"location.getHierarchyLevel() >>> " + location.getHierarchyLevel()
								+ " matchedfield.get(0).getId() >>> " + matchedfield.get(0).getId());
			}
		}
		orderOfAddress = treeMap.values().stream().collect(Collectors.toList());
	}

	private void disablePreRegFetch() {
		preRegParentPane.setVisible(false);
		preRegParentPane.setManaged(false);
		preRegParentPane.setDisable(true);
	}

	public GridPane addContent(UiSchemaDTO schemaDTO) {
		GridPane gridPane = prepareMainGridPane();

		GridPane primary = subGridPane(schemaDTO, "");
		GridPane secondary = subGridPane(schemaDTO, RegistrationConstants.LOCAL_LANGUAGE);

		gridPane.addColumn(0, primary);

		gridPane.addColumn(2, secondary);

		gridPane.setId(schemaDTO.getId() + "ParentGridPane");

		return gridPane;
	}

	public void addKeyboard(int position) {

		if (keyboardVisible) {
			parentFlow.remove(lastPosition);
			keyboardVisible = false;
			lastPosition = position;
		} else {
			GridPane gridPane = prepareMainGridPaneForKeyboard();
			gridPane.addColumn(1, keyboardNode);
			parentFlow.add(position, gridPane);
			lastPosition = position;
			keyboardVisible = true;
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
			content = addContentWithComboBoxObject(schemaDTO.getId(), schemaDTO, languageType);
			break;
		case RegistrationConstants.AGE_DATE:
			content = addContentForDobAndAge(schemaDTO.getId(), languageType);
			break;
		case "age":
			// TODO Not yet supported
			break;
		case RegistrationConstants.TEXTBOX:
			content = addContentWithTextField(schemaDTO, schemaDTO.getId(), languageType);
			break;
		}

		gridPane.add(content, 1, 2);

		return gridPane;
	}

	public VBox addContentForDobAndAge(String fieldId, String languageType) {

		VBox vBoxDD = new VBox();

		TextField dd = new TextField();
		dd.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		dd.setId(fieldId + "__" + RegistrationConstants.DD + languageType);
		Label ddLabel = new Label();
		ddLabel.setVisible(false);
		ddLabel.setId(fieldId + "__" + RegistrationConstants.DD + languageType + RegistrationConstants.LABEL);
		ddLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		vBoxDD.getChildren().addAll(ddLabel, dd);

		listOfTextField.put(fieldId + "__" + RegistrationConstants.DD + languageType, dd);

		VBox vBoxMM = new VBox();
		TextField mm = new TextField();
		mm.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		mm.setId(fieldId + "__" + RegistrationConstants.MM + languageType);
		Label mmLabel = new Label();
		mmLabel.setVisible(false);
		mmLabel.setId(fieldId + "__" + RegistrationConstants.MM + languageType + RegistrationConstants.LABEL);
		mmLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		vBoxMM.getChildren().addAll(mmLabel, mm);

		listOfTextField.put(fieldId + "__" + RegistrationConstants.MM + languageType, mm);

		VBox vBoxYYYY = new VBox();
		TextField yyyy = new TextField();
		yyyy.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		yyyy.setId(fieldId + "__" + RegistrationConstants.YYYY + languageType);
		Label yyyyLabel = new Label();
		yyyyLabel.setVisible(false);
		yyyyLabel.setId(fieldId + "__" + RegistrationConstants.YYYY + languageType + RegistrationConstants.LABEL);
		yyyyLabel.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		vBoxYYYY.getChildren().addAll(yyyyLabel, yyyy);

		listOfTextField.put(fieldId + "__" + RegistrationConstants.YYYY + languageType, yyyy);

		Label dobMessage = new Label();
		dobMessage.setId(fieldId + "__" + RegistrationConstants.DOB_MESSAGE + languageType);
		dobMessage.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);

		boolean localLanguage = languageType.equals(RegistrationConstants.LOCAL_LANGUAGE);

		dd.setPromptText(localLanguage ? localLabelBundle.getString(RegistrationConstants.DD)
				: applicationLabelBundle.getString(RegistrationConstants.DD));
		ddLabel.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.DD)
				: applicationLabelBundle.getString(RegistrationConstants.DD));
		mm.setPromptText(localLanguage ? localLabelBundle.getString(RegistrationConstants.MM)
				: applicationLabelBundle.getString(RegistrationConstants.MM));
		mmLabel.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.MM)
				: applicationLabelBundle.getString(RegistrationConstants.MM));
		dobMessage.setText("");

		yyyy.setPromptText(localLanguage ? localLabelBundle.getString(RegistrationConstants.YYYY)
				: applicationLabelBundle.getString(RegistrationConstants.YYYY));
		yyyyLabel.setText(localLanguage ? localLabelBundle.getString(RegistrationConstants.YYYY)
				: applicationLabelBundle.getString(RegistrationConstants.YYYY));

		HBox hB = new HBox();
		hB.setSpacing(10);
		hB.getChildren().addAll(vBoxDD, vBoxMM, vBoxYYYY);

		HBox hB2 = new HBox();
		VBox vboxAgeField = new VBox();
		TextField ageField = new TextField();
		ageField.setId(fieldId + "__" + RegistrationConstants.AGE_FIELD + languageType);
		ageField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		Label ageFieldLabel = new Label();
		ageFieldLabel
				.setId(fieldId + "__" + RegistrationConstants.AGE_FIELD + languageType + RegistrationConstants.LABEL);
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

		dateValidation.validateDate(parentFlowPane, dd, mm, yyyy, validation, fxUtils, ageField, null, dobMessage);
		dateValidation.validateMonth(parentFlowPane, dd, mm, yyyy, validation, fxUtils, ageField, null, dobMessage);
		dateValidation.validateYear(parentFlowPane, dd, mm, yyyy, validation, fxUtils, ageField, null, dobMessage);

		vB.setDisable(languageType.equals(RegistrationConstants.LOCAL_LANGUAGE));

		return vB;
	}

	@Autowired
	ResourceLoader resourceLoader;

	public VBox addContentWithTextField(UiSchemaDTO schema, String fieldName, String languageType) {
		TextField field = new TextField();
		Label label = new Label();
		Label validationMessage = new Label();

		VBox vbox = new VBox();
		vbox.setId(fieldName + RegistrationConstants.Parent);
		field.setId(fieldName + languageType);
		field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		label.setId(fieldName + languageType + RegistrationConstants.LABEL);
		label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		label.setVisible(false);
		validationMessage.setId(fieldName + languageType + RegistrationConstants.MESSAGE);
		validationMessage.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
		label.setPrefWidth(vbox.getPrefWidth());
		field.setPrefWidth(vbox.getPrefWidth());
		validationMessage.setPrefWidth(vbox.getPrefWidth());
		vbox.setSpacing(5);
		vbox.getChildren().add(label);
		vbox.getChildren().add(field);

		HBox hB = new HBox();
		hB.setSpacing(20);

		vbox.getChildren().add(validationMessage);
		if (primaryLanguage.equals(secondaryLanguage)) {
			vbox.setDisable(true);
		}

		if (listOfTextField.get(fieldName) != null)
			fxUtils.populateLocalFieldWithFocus(parentFlowPane, listOfTextField.get(fieldName), field,
					hasToBeTransliterated, validation);

		listOfTextField.put(field.getId(), field);
		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
			field.setPromptText(schema.getLabel().get(RegistrationConstants.SECONDARY));
			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.SECONDARY));
			label.setText(schema.getLabel().get(RegistrationConstants.SECONDARY));
			if (!schema.getType().equals(RegistrationConstants.SIMPLE_TYPE)) {
				field.setDisable(true);
			} else {
				ImageView imageView = null;
				try {
					imageView = new ImageView(
							new Image(resourceLoader.getResource("classpath:images/keyboard.png").getInputStream()));
					imageView.setId(fieldName);
					imageView.setFitHeight(20.00);
					imageView.setFitWidth(22.00);
					imageView.setOnMouseClicked((event) -> {
						setFocusonLocalField(event);
					});
					vk.changeControlOfKeyboard(field);
				} catch (IOException runtimeException) {
					LOGGER.error("keyboard.png image not found in resource folder", APPLICATION_NAME,
							RegistrationConstants.APPLICATION_ID,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

				}
				hB.getChildren().add(imageView);
			}
		} else {
			field.setPromptText(schema.getLabel().get(RegistrationConstants.PRIMARY));
			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.PRIMARY));
			label.setText(schema.getLabel().get(RegistrationConstants.PRIMARY));
		}

		hB.getChildren().add(validationMessage);
		hB.setStyle("-fx-background-color:WHITE");
		vbox.getChildren().add(hB);

		fxUtils.onTypeFocusUnfocusListener(parentFlowPane, field);
		return vbox;
	}

	private void populateDropDowns() {
		try {			
			for(String k : listOfComboBoxWithObject.keySet()) {
				switch (k.toLowerCase()) {
				case "gender":
					listOfComboBoxWithObject.get("gender").getItems()
					.addAll(masterSyncService.getGenderDtls(ApplicationContext.applicationLanguage()).stream()
							.filter(v -> !v.getCode().equals("OTH")).collect(Collectors.toList()));
					listOfComboBoxWithObject.get("genderLocalLanguage").getItems()
					.addAll(masterSyncService.getGenderDtls(ApplicationContext.localLanguage()).stream()
							.filter(v -> !v.getCode().equals("OTH")).collect(Collectors.toList()));			
					break;

				case "residencestatus":	
					listOfComboBoxWithObject.get("residenceStatus").getItems()
					.addAll(masterSyncService.getIndividualType(ApplicationContext.applicationLanguage()));
					listOfComboBoxWithObject.get("residenceStatusLocalLanguage").getItems()
					.addAll(masterSyncService.getIndividualType(ApplicationContext.localLanguage()));
					break;
					
				default:
					//TODO
					break;
				}
			}			
		} catch (RegBaseCheckedException e) {
			LOGGER.error("populateDropDowns", APPLICATION_NAME,
							RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(e));
		}
	}

	public <T> VBox addContentWithComboBoxObject(String fieldName, UiSchemaDTO schema, String languageType) {

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

	/**
	 * setting the registration navigation label to lost uin
	 */
	protected void lostUIN() {
		lostUIN = true;
		registrationNavlabel
				.setText(ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));
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
				if (!getAge(yyyy.getText(), mm.getText(), dd.getText()).equals(ageField.getText())) {
					if (oldValue) {
						ageValidation(parentPane, ageField, dobMessage, oldValue, dd, mm, yyyy);
					}
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
	
	/**
	 * Gets the age.
	 *
	 * @param year the year
	 * @param month the month
	 * @param date the date
	 * @return the age
	 */
	String getAge(String year, String month, String date) {
		if (year != null && !year.isEmpty() && month != null && !month.isEmpty() && date != null && !date.isEmpty()) {
			LocalDate birthdate = new LocalDate(Integer.parseInt(year), Integer.parseInt(month),
					Integer.parseInt(date)); // Birth
												// date
			LocalDate now = new LocalDate(); // Today's date

			Period period = new Period(birthdate, now, PeriodType.yearMonthDay());
			return String.valueOf(period.getYears());
		} else {
			return "";
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

					dd.setText(String.valueOf(defaultDate.get(Calendar.DATE)));
					dd.requestFocus();
					mm.setText(String.valueOf(defaultDate.get(Calendar.MONTH + 1)));
					mm.requestFocus();
					yyyy.setText(String.valueOf(defaultDate.get(Calendar.YEAR)));
					yyyy.requestFocus();
					dd.requestFocus();

					// TODO NOT REQUIRED NOW
					/*
					 * if (age <= minAge) {
					 * 
					 * if (!isChild == true) { clearAllValues(); clearAllBiometrics(); } if
					 * (RegistrationConstants.DISABLE.equalsIgnoreCase(
					 * getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG
					 * )) && RegistrationConstants.DISABLE.equalsIgnoreCase(
					 * getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))) {
					 * isChild = true; validation.setChild(isChild);
					 * generateAlert(RegistrationConstants.ERROR,
					 * RegistrationUIConstants.PARENT_BIO_MSG);
					 * 
					 * } else { updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, true);
					 * updatePageFlow(RegistrationConstants.FINGERPRINT_CAPTURE, false);
					 * updatePageFlow(RegistrationConstants.IRIS_CAPTURE, false);
					 * 
					 * } } else {
					 * 
					 * if (!isChild == false) { clearAllValues(); clearAllBiometrics(); } if
					 * (getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO(
					 * ) != null) {
					 * 
					 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					 * .setFingerprintDetailsDTO(new ArrayList<>());
					 * 
					 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					 * .setIrisDetailsDTO(new ArrayList<>());
					 * 
					 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					 * .setBiometricExceptionDTO(new ArrayList<>());
					 * 
					 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					 * .setExceptionFace(new FaceDetailsDTO());
					 * 
					 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					 * .setFace(new FaceDetailsDTO());
					 * 
					 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					 * .setHasExceptionPhoto(false);
					 * 
					 * }
					 * 
					 * updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, false); //
					 * updateBioPageFlow(RegistrationConstants.FINGERPRINT_DISABLE_FLAG, //
					 * RegistrationConstants.FINGERPRINT_CAPTURE); //
					 * updateBioPageFlow(RegistrationConstants.IRIS_DISABLE_FLAG, //
					 * RegistrationConstants.IRIS_CAPTURE);
					 * 
					 * }
					 */
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
				// updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, false);
				// updatePageFlow(RegistrationConstants.FINGERPRINT_CAPTURE, true);
				// updatePageFlow(RegistrationConstants.IRIS_CAPTURE, true);
				dd.clear();
				mm.clear();
				yyyy.clear();
			}
		} else {
			ageField.setText(RegistrationConstants.EMPTY);
		}
	}

	private void addFirstOrderAddress(ComboBox<GenericDto> location, int id, String languageType) {
		if (location != null) {
			location.getItems().clear();
			try {
				List<GenericDto> locations = null;
				locations = masterSync.findLocationByHierarchyCode(id, languageType);

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
				e.printStackTrace();
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
		try {
			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
			for (UiSchemaDTO schemaField : validation.getValidationMap().values()) {
				if (schemaField.getControlType() == null)
					continue;			

				if (registrationDTO.getRegistrationCategory().equals(RegistrationConstants.PACKET_TYPE_UPDATE)
						&& !registrationDTO.getUpdatableFields().contains(schemaField.getId()))
					continue;

				switch (schemaField.getType()) {
				case RegistrationConstants.SIMPLE_TYPE:
					if (schemaField.getControlType().equals(RegistrationConstants.DROPDOWN)) {
						ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
						ComboBox<GenericDto> localField = listOfComboBoxWithObject
								.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
						registrationDTO.addDemographicField(schemaField.getId(),
								applicationContext.getApplicationLanguage(),
								platformField == null ? null
										: platformField.getValue() != null ? platformField.getValue().getName() : null,
								applicationContext.getLocalLanguage(), localField == null ? null
										: localField.getValue() != null ? localField.getValue().getName() : null);
					} else {
						TextField platformField = listOfTextField.get(schemaField.getId());
						TextField localField = listOfTextField
								.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
						registrationDTO.addDemographicField(schemaField.getId(),
								applicationContext.getApplicationLanguage(), platformField.getText(),
								applicationContext.getLocalLanguage(),
								localField == null ? null : localField.getText());
					}
					break;
				case RegistrationConstants.NUMBER:
				case RegistrationConstants.STRING:
					if (schemaField.getControlType().equalsIgnoreCase(RegistrationConstants.AGE_DATE))
						registrationDTO.setDateField(schemaField.getId(),
								listOfTextField.get(schemaField.getId() + "__" + RegistrationConstants.DD).getText(),
								listOfTextField.get(schemaField.getId() + "__" + RegistrationConstants.MM).getText(),
								listOfTextField.get(schemaField.getId() + "__" + RegistrationConstants.YYYY).getText());
					else {
						if (schemaField.getControlType().equals(RegistrationConstants.DROPDOWN)) {
							ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
							registrationDTO.addDemographicField(schemaField.getId(), platformField == null ? null
									: platformField.getValue() != null ? platformField.getValue().getName() : null);
						} else {
							TextField platformField = listOfTextField.get(schemaField.getId());
							registrationDTO.addDemographicField(schemaField.getId(),
									platformField != null ? platformField.getText() : null);
						}
					}
					break;

				default:
					break;
				}
			}
		} catch (Exception exception) {
			LOGGER.error("addDemoGraphicDetailsToSession", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
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
			LOGGER.error(" falied due to invalid field", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
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
			/*
			 * if (preRegistrationId.getText() == null &&
			 * preRegistrationId.getText().isEmpty()) {
			 * registrationDTO.setPreRegistrationId(""); }
			 */

			addDemoGraphicDetailsToSession();

			SessionContext.map().put(RegistrationConstants.IS_Child, registrationDTO.isChild());

			registrationDTO.getOsiDataDTO().setOperatorID(SessionContext.userContext().getUserId());

			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Saved the demographic fields to DTO");

		} catch (Exception exception) {
			LOGGER.error("REGISTRATION - SAVING THE DETAILS FAILED ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
	}

	public void uinUpdate() {
		List<String> selectionList = getRegistrationDTOFromSession().getUpdatableFields();
		if (selectionList != null) {
			disablePreRegFetch();
			registrationNavlabel.setText(applicationLabelBundle.getString("uinUpdateNavLbl"));
			for (Node pane : parentFlowPane.getChildren()) {
				if (!pane.getId().matches("preRegParentPane|languageLabelParentPane")) {
					String fieldId = pane.getId().replace("ParentGridPane", "");
					if (selectionList.contains(fieldId)) {
						pane.setDisable(false);
					} else {
						UiSchemaDTO schemaField = getValidationMap().get(fieldId);
						pane.setDisable(schemaField != null && "name".equalsIgnoreCase(schemaField.getSubType()) ? false
								: true);
					}
				}
			}
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

			for (UiSchemaDTO schemaField : validation.getValidationMap().values()) {
				Object value = demographics.get(schemaField.getId());
				if (value == null)
					continue;

				switch (schemaField.getType()) {
				case RegistrationConstants.SIMPLE_TYPE:
					if (schemaField.getControlType().equals(RegistrationConstants.DROPDOWN)
							|| Arrays.asList(orderOfAddress).contains(schemaField.getId())) {
						populateFieldValue(listOfComboBoxWithObject.get(schemaField.getId()),
								listOfComboBoxWithObject.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE),
								(List<SimpleDto>) value);
					} else
						populateFieldValue(listOfTextField.get(schemaField.getId()),
								listOfTextField.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE),
								(List<SimpleDto>) value);
					break;

				case RegistrationConstants.NUMBER:
				case RegistrationConstants.STRING:
					if(RegistrationConstants.AGE_DATE.equalsIgnoreCase(schemaField.getControlType())) {
						String[] dateParts = ((String)value).split("/");
						if (dateParts.length == 3) {
							listOfTextField.get(schemaField.getId() + "__" + "dd").setText(dateParts[2]);
							listOfTextField.get(schemaField.getId() + "__" + "mm").setText(dateParts[1]);
							listOfTextField.get(schemaField.getId() + "__" + "yyyy").setText(dateParts[0]);
						}
					}
					else if (RegistrationConstants.DROPDOWN.equalsIgnoreCase(schemaField.getControlType()) || 
							Arrays.asList(orderOfAddress).contains(schemaField.getId())) {
						ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
						if(platformField != null) {
							platformField.setValue(new GenericDto((String) value, (String) value, "eng"));
						}
					}
					else {
						TextField textField = listOfTextField.get(schemaField.getId());
						if (textField != null)
							textField.setText((String) value);
					}
				}
			}

			/*
			 * if (SessionContext.map().get(RegistrationConstants.IS_Child) != null) {
			 * boolean isChild = (boolean)
			 * SessionContext.map().get(RegistrationConstants.IS_Child);
			 * parentDetailPane.setDisable(!isChild); parentDetailPane.setVisible(isChild);
			 * }
			 */

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
	private void populateFieldValue(Node nodeForPlatformLang, Node nodeForLocalLang, List<SimpleDto> fieldValues) {
		if (fieldValues != null) {
			String platformLanguageCode = applicationContext.getApplicationLanguage();
			String localLanguageCode = applicationContext.getLocalLanguage();
			String valueInPlatformLang = RegistrationConstants.EMPTY;
			String valueinLocalLang = RegistrationConstants.EMPTY;

			for (SimpleDto fieldValue : fieldValues) {
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
			getRegistrationDTOFromSession().setPreRegistrationId(preRegId);
			prepareEditPageContent();

		} else if (errorResponseDTOList != null && !errorResponseDTOList.isEmpty()) {
			generateAlertLanguageSpecific(RegistrationConstants.ERROR, errorResponseDTOList.get(0).getMessage());
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
			listOfTextField.get(node.getId() + "LocalLanguage").requestFocus();
			keyboardNode.setVisible(true);
			keyboardNode.setManaged(true);
			addKeyboard(positionTracker.get((node.getId() + "ParentGridPane")) + 1);

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

		//Its required to save before validation as on spot check for values during MVEL validation
		saveDetail();

		if (registrationController.validateDemographicPane(parentFlowPane)) {
			//saveDetail();

			guardianBiometricsController.populateBiometricPage(false);
			/*
			 * SessionContext.map().put("demographicDetail", false);
			 * SessionContext.map().put("documentScan", true);
			 */

			documentScanController.populateDocumentCategories();

			auditFactory.audit(AuditEvent.REG_DEMO_NEXT, Components.REG_DEMO_DETAILS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			registrationController.showCurrentPage(RegistrationConstants.DEMOGRAPHIC_DETAIL,
					getPageByAction(RegistrationConstants.DEMOGRAPHIC_DETAIL, RegistrationConstants.NEXT));

		}
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

					List<GenericDto> locations = masterSync.findProvianceByHierarchyCode(
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

	/*
	 * private void updateBioPageFlow(String flag, String pageId) { if
	 * (RegistrationConstants.DISABLE.equalsIgnoreCase(String.valueOf(
	 * ApplicationContext.map().get(flag)))) { updatePageFlow(pageId, false); } else
	 * { updatePageFlow(pageId, true); } }
	 */

}
