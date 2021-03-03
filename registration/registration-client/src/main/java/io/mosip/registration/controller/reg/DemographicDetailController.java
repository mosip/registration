package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.mosip.registration.util.common.DemographicChangeActionHandler;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.kernel.core.util.StringUtils;
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
import io.mosip.registration.controller.device.BiometricsController;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RequiredOnExpr;
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
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;

/**
 * {@code DemographicDetailController} is to capture the demographic details
 *
 * @author Taleev.Aalam
 * @since 1.0.0
 *
 */

//@Controller
public class DemographicDetailController extends BaseController {
//
//	/**
//	 * Instance of {@link Logger}
//	 */
//	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
//	private static final String loggerClassName = "DemographicDetailController";
//
//	@Autowired
//	private PridValidator<String> pridValidatorImpl;
//	@Autowired
//	private Validations validation;
//	@Autowired
//	private MasterSyncService masterSync;
//	@Autowired
//	private DateValidation dateValidation;
//	@Autowired
//	private RegistrationController registrationController;
//	@Autowired
//	private DocumentScanController documentScanController;
//	@Autowired
//	private Transliteration<String> transliteration;
//	@Autowired
//	private PreRegistrationDataSyncService preRegistrationDataSyncService;
//	@Autowired
//	private MasterSyncService masterSyncService;
//	@Autowired
//	private IdentitySchemaService identitySchemaService;
//	@Autowired
//	private MasterSyncDao masterSyncDao;
//	@Autowired
//	private BiometricsController guardianBiometricsController;
//	@Autowired
//	private ResourceLoader resourceLoader;
//	@Autowired
//	private DemographicChangeActionHandler demographicChangeActionHandler;
//
//	@FXML
//	private FlowPane parentFlowPane;
//	@FXML
//	private GridPane scrollParentPane;
//	@FXML
//	private GridPane preRegParentPane;
//	@FXML
//	private GridPane borderToDo;
//	@FXML
//	private Label registrationNavlabel;
//	@FXML
//	private AnchorPane keyboardPane;
//	@FXML
//	public TextField preRegistrationId;
//	@FXML
//	public Label languageLabelLocalLanguage;
//
//	private boolean isChild;
//	private Node keyboardNode;
//	private FXUtils fxUtils;
//	private int minAge;
//	private int maxAge;
//	private boolean lostUIN = false;
//	private ResourceBundle applicationLabelBundle;
//	private ResourceBundle localLabelBundle;
//	private String primaryLanguage;
//	private String secondaryLanguage;
//	boolean hasToBeTransliterated = true;
//	public Map<String, CheckBox> listOfCheckboxes;
//	public Map<String, ComboBox<GenericDto>> listOfComboBoxWithObject;
//	public Map<String, List<Button>> listOfButtons;
//	public Map<String, TextField> listOfTextField;
//	private int age = 0;
//	private VirtualKeyboard vk;
//	private HashMap<String, Integer> positionTracker;
//	int lastPosition;
//	private ObservableList<Node> parentFlow;
//	private boolean keyboardVisible = false;
//	private Map<String, TreeMap<Integer, String>> orderOfAddressMapByGroup = new HashMap<>();
//	private Map<String, List<String>> orderOfAddressListByGroup = new LinkedHashMap<>();
//	Map<String, List<UiSchemaDTO>> templateGroup = null;
//	private Node previousNode;
//
//
//	/*
//	 * (non-Javadoc)
//	 *
//	 * @see javafx.fxml.Initializable#initialize()
//	 */
//	@FXML
//	private void initialize() {
//		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
//				RegistrationConstants.APPLICATION_ID, "Entering the Demographic Details Screen");
//
//		listOfComboBoxWithObject = new HashMap<>();
//		listOfButtons = new HashMap<>();
//		listOfTextField = new HashMap<>();
//		listOfCheckboxes = new HashMap<>();
//		lastPosition = -1;
//		positionTracker = new HashMap<>();
//		fillOrderOfLocation();
//		primaryLanguage = applicationContext.getApplicationLanguage();
//		secondaryLanguage = applicationContext.getLocalLanguage();
//
//		ResourceBundle localProperties = ApplicationContext.localLanguageProperty();
//
//		String localLanguageTextVal = isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()
//				? localProperties.getString("language")
//				: RegistrationConstants.EMPTY;
//		languageLabelLocalLanguage.setText(localLanguageTextVal);
//
//		if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//			vk = VirtualKeyboard.getInstance();
//			keyboardNode = vk.view();
//		}
//		if (ApplicationContext.getInstance().getApplicationLanguage()
//				.equals(ApplicationContext.getInstance().getLocalLanguage())) {
//			hasToBeTransliterated = false;
//		}
//
//		try {
//			applicationLabelBundle = applicationContext.getApplicationLanguageBundle();
//			if (getRegistrationDTOFromSession() == null) {
//				validation.updateAsLostUIN(false);
//				registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW);
//			}
//
//			if (validation.isLostUIN()) {
//				registrationNavlabel.setText(applicationLabelBundle.getString("/lostuin"));
//				disablePreRegFetch();
//			}
//
//			if (getRegistrationDTOFromSession() != null
//					&& getRegistrationDTOFromSession().getSelectionListDTO() == null) {
//				getRegistrationDTOFromSession().setUpdateUINNonBiometric(false);
//				getRegistrationDTOFromSession().setUpdateUINChild(false);
//			}
//			validation.setChild(false);
//			lostUIN = false;
//			fxUtils = FXUtils.getInstance();
//			fxUtils.setTransliteration(transliteration);
//			isChild = false;
//			minAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MIN_AGE));
//			maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
//			localLabelBundle = applicationContext.getLocalLanguageProperty();
//			parentFlow = parentFlowPane.getChildren();
//			int position = parentFlow.size() - 1;
//
//			templateGroup = getTemplateGroupMap();
//			for (Entry<String, List<UiSchemaDTO>> templateGroupEntry : templateGroup.entrySet()) {
//
//				List<UiSchemaDTO> list = templateGroupEntry.getValue();
//				if (list.size() <= 4) {
//					addGroupInUI(list, position, templateGroupEntry.getKey() + position);
//				} else {
//					for (int index = 0; index <= list.size() / 4; index++) {
//
//						int toIndex = ((index * 4) + 3) <= list.size() - 1 ? ((index * 4) + 4) : list.size();
//						List<UiSchemaDTO> subList = list.subList(index * 4, toIndex);
//						addGroupInUI(subList, position, templateGroupEntry.getKey() + position);
//					}
//				}
//			}
//
//			populateDropDowns();
//			for (Entry<String, List<String>> orderOfAdd : orderOfAddressListByGroup.entrySet()) {
//				List<String> orderOfAddress = orderOfAdd.getValue();
//				addFirstOrderAddress(listOfComboBoxWithObject.get(orderOfAddress.get(0)), 1,
//						applicationContext.getApplicationLanguage());
//
//				if (isLocalLanguageAvailable() || !isAppLangAndLocalLangSame()) {
//
//					addFirstOrderAddress(
//							listOfComboBoxWithObject.get(orderOfAddress.get(0) + RegistrationConstants.LOCAL_LANGUAGE),
//							1, applicationContext.getLocalLanguage());
//				}
//
//				for (int j = 0; j < orderOfAddress.size() - 1; j++) {
//					final int k = j;
//
//					try {
//						listOfComboBoxWithObject.get(orderOfAddress.get(k)).setOnAction((event) -> {
//							configureMethodsForAddress(k, k + 1, orderOfAddress.size(), orderOfAddress);
//						});
//					} catch (Exception runtimeException) {
//						LOGGER.info(orderOfAddress.get(k) + " is not a valid field", APPLICATION_NAME,
//								RegistrationConstants.APPLICATION_ID,
//								runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//					}
//				}
//			}
//
//			refreshDemographicGroups();
//
//			auditFactory.audit(AuditEvent.REG_DEMO_CAPTURE, Components.REGISTRATION_CONTROLLER,
//					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
//		} catch (RuntimeException runtimeException) {
//			LOGGER.error("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_DEMOGRAPHIC_PAGE);
//
//		}
//	}
//
//	private void addGroupInUI(List subList, int position, String gridPaneId) {
//		GridPane groupGridPane = new GridPane();
//		groupGridPane.setId(gridPaneId);
//
//		addGroupContent(subList, groupGridPane);
//		parentFlowPane.getChildren().add(groupGridPane);
//
//		//parentFlow.add(groupGridPane);
//		//position++;
//		//positionTracker.put(groupGridPane.getId(), position);
//	}
//
//	private void fillOrderOfLocation() {
//		List<Location> locations = masterSyncDao.getLocationDetails(applicationContext.getApplicationLanguage());
//		Map<Integer, String> treeMap = new TreeMap<Integer, String>();
//
//		Collection<UiSchemaDTO> fields = validation.getValidationMap().values();
//		for (Location location : locations) {
//			List<UiSchemaDTO> matchedfield = fields.stream()
//					.filter(field -> isDemographicField(field) && field.getSubType() != null
//							&& RegistrationConstants.DROPDOWN.equals(field.getControlType())
//							&& field.getSubType().equalsIgnoreCase(location.getHierarchyName()))
//					.collect(Collectors.toList());
//
//			if (matchedfield != null && !matchedfield.isEmpty()) {
//				for (UiSchemaDTO uiSchemaDTO : matchedfield) {
//
//					if (orderOfAddressMapByGroup.containsKey(uiSchemaDTO.getGroup())) {
//
//						if (!orderOfAddressMapByGroup.get(uiSchemaDTO.getGroup())
//								.containsKey(location.getHierarchyLevel())) {
//							TreeMap<Integer, String> hirearchyMap = orderOfAddressMapByGroup
//									.get(uiSchemaDTO.getGroup());
//							hirearchyMap.put(location.getHierarchyLevel(), uiSchemaDTO.getId());
//
//							orderOfAddressMapByGroup.put(uiSchemaDTO.getGroup(), hirearchyMap);
//						}
//					} else {
//
//						TreeMap<Integer, String> hirearchyMap = new TreeMap<>();
//						hirearchyMap.put(location.getHierarchyLevel(), uiSchemaDTO.getId());
//
//						orderOfAddressMapByGroup.put(uiSchemaDTO.getGroup(), hirearchyMap);
//					}
//				}
//			}
//
//		}
//
//		for (Entry<String, TreeMap<Integer, String>> entry : orderOfAddressMapByGroup.entrySet()) {
//
//			orderOfAddressListByGroup.put(entry.getKey(),
//					entry.getValue().values().stream().collect(Collectors.toList()));
//
//		}
//	}
//
//	private void disablePreRegFetch() {
//		preRegParentPane.setVisible(false);
//		preRegParentPane.setManaged(false);
//		preRegParentPane.setDisable(true);
//	}
//
//	public boolean isAppLangAndLocalLangSame() {
//
//		return secondaryLanguage != null && primaryLanguage.equals(secondaryLanguage);
//	}
//
//	public boolean isLocalLanguageAvailable() {
//
//		return secondaryLanguage != null && !secondaryLanguage.isEmpty();
//	}
//
//	public void addKeyboard(int position) {
//
//		if (keyboardVisible) {
//			parentFlow.remove(lastPosition);
//			keyboardVisible = false;
//			lastPosition = position;
//		} else {
//			GridPane gridPane = prepareMainGridPaneForKeyboard();
//			gridPane.addColumn(1, keyboardNode);
//			parentFlow.add(position, gridPane);
//			lastPosition = position;
//			keyboardVisible = true;
//		}
//	}
//
//	private GridPane prepareMainGridPane() {
//		GridPane gridPane = new GridPane();
//		gridPane.setPrefWidth(1000);
//
//		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
//		ColumnConstraints columnConstraint1 = new ColumnConstraints();
//		columnConstraint1.setPercentWidth(48);
//		ColumnConstraints columnConstraint2 = new ColumnConstraints();
//		columnConstraint2.setPercentWidth(7);
//		ColumnConstraints columnConstraint3 = new ColumnConstraints();
//		columnConstraint3.setPercentWidth(45);
//		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
//		return gridPane;
//	}
//
//	private GridPane prepareMainGridPaneForKeyboard() {
//		GridPane gridPane = new GridPane();
//		gridPane.setPrefWidth(1000);
//
//		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
//		ColumnConstraints columnConstraint1 = new ColumnConstraints();
//		columnConstraint1.setPercentWidth(10);
//		ColumnConstraints columnConstraint2 = new ColumnConstraints();
//		columnConstraint2.setPercentWidth(80);
//		ColumnConstraints columnConstraint3 = new ColumnConstraints();
//		columnConstraint3.setPercentWidth(10);
//		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
//		return gridPane;
//	}
//
//	@SuppressWarnings("unlikely-arg-type")
//	public GridPane subGridPane(UiSchemaDTO schemaDTO, String languageType, int noOfItems) {
//		GridPane gridPane = new GridPane();
//
//		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
//		ColumnConstraints columnConstraint1 = new ColumnConstraints();
//		columnConstraint1.setPercentWidth(noOfItems * 5);
//		ColumnConstraints columnConstraint2 = new ColumnConstraints();
//		columnConstraint2.setPercentWidth(90);
//		ColumnConstraints columnConstraint3 = new ColumnConstraints();
//		columnConstraint3.setPercentWidth(5);
//		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
//
//		ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
//		RowConstraints rowConstraint1 = new RowConstraints();
//		RowConstraints rowConstraint2 = new RowConstraints();
//		RowConstraints rowConstraint3 = new RowConstraints();
//		rowConstraints.addAll(rowConstraint1, rowConstraint2, rowConstraint3);
//
//		VBox content = null;
//		switch (schemaDTO.getControlType()) {
//			case RegistrationConstants.DROPDOWN:
//				content = addContentWithComboBoxObject(schemaDTO.getId(), schemaDTO, languageType);
//				break;
//			case RegistrationConstants.AGE_DATE:
//				content = addContentForDobAndAge(schemaDTO, languageType);
//				break;
//			case RegistrationConstants.TEXTBOX:
//				content = addContentWithTextField(schemaDTO, schemaDTO.getId(), languageType);
//				break;
//			case RegistrationConstants.CHECKBOX:
//				content = addContentWithCheckbox(schemaDTO.getId(), schemaDTO, languageType);
//				break;
//			case RegistrationConstants.BUTTON:
//				content = addContentWithButtons(schemaDTO.getId(), schemaDTO, languageType);
//				break;
//		}
//
//		gridPane.add(content, 1, 2);
//		gridPane.visibleProperty().bind(content.visibleProperty());
//		gridPane.managedProperty().bind(content.visibleProperty());
//		return gridPane;
//	}
//
//	private VBox addDateTextField(UiSchemaDTO schema, String type, String languageType, String mandatorySuffix) {
//		VBox vBoxParent = new VBox();
//		TextField textField = new TextField();
//		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
//		textField.setId(schema.getId() + "__" + type + languageType);
//		Label label = new Label();
//		label.setVisible(false);
//		label.setId(schema.getId() + "__" + type + languageType + RegistrationConstants.LABEL);
//		label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
//		vBoxParent.getChildren().addAll(label, textField);
//
//		boolean localLanguage = languageType.equals(RegistrationConstants.LOCAL_LANGUAGE);
//		label.setText(localLanguage ? localLabelBundle.getString(type)
//				: applicationLabelBundle.getString(type) + mandatorySuffix);
//		textField.setPromptText(label.getText());
//
////		textField.textProperty().addListener((ob, ov, nv) -> {
////			fxUtils.showLabel(parentFlowPane, textField);
////			if(!dateValidation.isNewValueValid(nv, type)) {
////				textField.setText(ov);
////			}
////			boolean isValid = RegistrationConstants.AGE_FIELD.equalsIgnoreCase(type) ?
////					dateValidation.validateAge(parentFlowPane, textField) :
////					dateValidation.validateDate(parentFlowPane, schema.getId());
////			if(isValid) {
////				refreshDemographicGroups();
////			}
////		});
//
//		putIntoLabelMap(schema.getId() + "__" + type + languageType,
//				schema.getLabel().get(RegistrationConstants.LOCAL_LANGUAGE.equals(languageType) ?
//						RegistrationConstants.SECONDARY : RegistrationConstants.PRIMARY));
//		listOfTextField.put(schema.getId() + "__" + type + languageType, textField);
//		return vBoxParent;
//	}
//
//	public VBox addContentForDobAndAge(UiSchemaDTO schema, String languageType) {
//		String mandatorySuffix = getMandatorySuffix(schema);
//		boolean localLanguage = languageType.equals(RegistrationConstants.LOCAL_LANGUAGE);
//		VBox vBoxDD = addDateTextField(schema, RegistrationConstants.DD, languageType, mandatorySuffix);
//		VBox vBoxMM = addDateTextField(schema, RegistrationConstants.MM, languageType, mandatorySuffix);
//		VBox vBoxYYYY = addDateTextField(schema, RegistrationConstants.YYYY, languageType, mandatorySuffix);
//		VBox vboxAgeField = addDateTextField(schema, RegistrationConstants.AGE_FIELD, languageType, mandatorySuffix);
//
//		Label dobMessage = new Label();
//		dobMessage.setId(schema.getId() + "__" + RegistrationConstants.DOB_MESSAGE + languageType);
//		dobMessage.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
//		dobMessage.setText("");
//
//		HBox dateHbox = new HBox();
//		dateHbox.setSpacing(10);
//		dateHbox.setPrefWidth(250);
//		String dateFormat = ApplicationContext.getDateFormat();
//		String[] parts = dateFormat.split("/|-");
//		if(parts.length > 0) {
//			for(String part : parts) {
//				switch (part.toLowerCase()) {
//					case "dd" : dateHbox.getChildren().add(vBoxDD);	break;
//					case "mm": dateHbox.getChildren().add(vBoxMM);	break;
//					case "yyyy": dateHbox.getChildren().add(vBoxYYYY); break;
//				}
//			}
//		}
//		else {
//			dateHbox.getChildren().addAll(vBoxDD, vBoxMM, vBoxYYYY);
//		}
//
//		Label orLabel = new Label(localLanguage ? localLabelBundle.getString("ageOrDOBField")
//				: applicationLabelBundle.getString("ageOrDOBField"));
//
//		VBox orVbox = new VBox();
//		orVbox.setPrefWidth(100);
//		orVbox.getChildren().addAll(new Label(), orLabel);
//
//		HBox dateAgeHbox = new HBox();
//		dateAgeHbox.setSpacing(10);
//		dateAgeHbox.getChildren().addAll(dateHbox, orVbox, vboxAgeField);
//
//		VBox finalVbox = new VBox();
//		finalVbox.setId(schema.getId());
//		finalVbox.getChildren().addAll(dateAgeHbox, dobMessage);
//		//NOTE: by default local/secondary language DOB fields are disabled
//		finalVbox.setDisable(languageType.equals(RegistrationConstants.LOCAL_LANGUAGE));
//		return finalVbox;
//	}
//
//
//
//	public VBox addContentWithTextField(UiSchemaDTO schema, String fieldName, String languageType) {
//		TextField field = new TextField();
//		Label label = new Label();
//		Label validationMessage = new Label();
//
//		VBox vbox = new VBox();
//		vbox.setId(fieldName + languageType + RegistrationConstants.Parent);
//		field.setId(fieldName + languageType);
//		field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
//		label.setId(fieldName + languageType + RegistrationConstants.LABEL);
//		label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
//		label.setVisible(false);
//		validationMessage.setId(fieldName + languageType + RegistrationConstants.MESSAGE);
//		validationMessage.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
//		label.setPrefWidth(vbox.getPrefWidth());
//		field.setPrefWidth(vbox.getPrefWidth());
//		validationMessage.setPrefWidth(vbox.getPrefWidth());
//		vbox.setSpacing(5);
//
//		vbox.getChildren().add(label);
//		vbox.getChildren().add(field);
//		vbox.getChildren().add(validationMessage);
//		listOfTextField.put(field.getId(), field);
//		setFieldChangeListener(field);
//
//		HBox hB = new HBox();
//		hB.setSpacing(20);
//
//		String mandatorySuffix = getMandatorySuffix(schema);
//		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
//			label.setText(schema.getLabel().get(RegistrationConstants.SECONDARY) + mandatorySuffix);
//			field.setPromptText(label.getText());
//			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.SECONDARY));
//
//			if (!schema.getType().equals(RegistrationConstants.SIMPLE_TYPE)) {
//				field.setDisable(true);
//			} else {
//				ImageView imageView = null;
//				try {
//					imageView = new ImageView(
//							new Image(resourceLoader.getResource("classpath:images/keyboard.png").getInputStream()));
//					imageView.setId(fieldName);
//					imageView.setFitHeight(20.00);
//					imageView.setFitWidth(22.00);
//					imageView.visibleProperty().bind(field.visibleProperty());
//					imageView.managedProperty().bind(field.visibleProperty());
//					imageView.setOnMouseClicked((event) -> {
//						setFocusonLocalField(event);
//					});
//
//					if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//						vk.changeControlOfKeyboard(field);
//					}
//				} catch (IOException runtimeException) {
//					LOGGER.error("keyboard.png image not found in resource folder", APPLICATION_NAME,
//							RegistrationConstants.APPLICATION_ID,
//							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//
//				}
//				hB.getChildren().add(imageView);
//			}
//		} else {
//			label.setText(schema.getLabel().get(RegistrationConstants.PRIMARY) + mandatorySuffix);
//			field.setPromptText(label.getText());
//			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.PRIMARY));
//		}
//
//		hB.getChildren().add(validationMessage);
//		hB.setStyle("-fx-background-color:WHITE");
//		vbox.getChildren().add(hB);
//		changeNodeOrientation(vbox, languageType);
//		fxUtils.onTypeFocusUnfocusListener(parentFlowPane, field);
//
//		field.managedProperty().bind(field.visibleProperty());
//		validationMessage.visibleProperty().bind(field.visibleProperty());
//		validationMessage.managedProperty().bind(field.visibleProperty());
//		hB.visibleProperty().bind(field.visibleProperty());
//		hB.managedProperty().bind(field.visibleProperty());
//		vbox.visibleProperty().bind(field.visibleProperty());
//		vbox.managedProperty().bind(field.visibleProperty());
//
//		return vbox;
//	}
//
//	private void changeNodeOrientation(Node node, String langType) {
//		String langCode = langType.equalsIgnoreCase(RegistrationConstants.LOCAL_LANGUAGE) ?
//				ApplicationContext.secondaryLanguageLocal() : ApplicationContext.primaryLanguageLocal();
//		String langauages = (String) ApplicationContext.map().getOrDefault(RegistrationConstants.RIGHT_TO_LEFT_ORIENTATION_LANGUAGES, "ar");
//		if(langauages.contains(langCode)) {
//			node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
//		}
//	}
//
//	private void populateDropDowns() {
//		try {
//			for (String k : listOfComboBoxWithObject.keySet()) {
//				if (k.endsWith(RegistrationConstants.LOCAL_LANGUAGE)) {
//					if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//						listOfComboBoxWithObject.get(k).getItems().addAll(masterSyncService.getFieldValues(
//								k.replace(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY), ApplicationContext.localLanguage()));
//					}
//				} else {
//					listOfComboBoxWithObject.get(k).getItems()
//							.addAll(masterSyncService.getFieldValues(k, ApplicationContext.applicationLanguage()));
//				}
//			}
//		} catch (RegBaseCheckedException e) {
//			LOGGER.error(APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,"populateDropDowns",
//					ExceptionUtils.getStackTrace(e));
//		}
//	}
//
//	public <T> VBox addContentWithCheckbox(String fieldName, UiSchemaDTO schema, String languageType) {
//		CheckBox field = new CheckBox();
//		Label label = new Label();
//		Label validationMessage = new Label();
//		VBox vbox = new VBox();
//		vbox.setId(fieldName + RegistrationConstants.Parent + RegistrationConstants.Parent);
//		field.setId(fieldName + languageType);
//
//		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
//			label.setText(schema.getLabel().get(RegistrationConstants.SECONDARY));
//			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.SECONDARY));
//		} else {
//			label.setText(schema.getLabel().get(RegistrationConstants.PRIMARY));
//			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.PRIMARY));
//		}
//		label.setId(fieldName + languageType + RegistrationConstants.LABEL);
//		label.setVisible(true);
//		label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
//		validationMessage.setId(fieldName + languageType + RegistrationConstants.MESSAGE);
//		validationMessage.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
//		label.setPrefWidth(vbox.getPrefWidth());
//		validationMessage.setPrefWidth(vbox.getPrefWidth());
//		validationMessage.setVisible(false);
//
//		if (applicationContext.getApplicationLanguage().equals(applicationContext.getLocalLanguage())
//				&& languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
//			vbox.setDisable(true);
//		}
//		HBox hBox = new HBox();
//		hBox.setSpacing(10);
//		hBox.setId(fieldName + RegistrationConstants.Parent);
//		hBox.getChildren().add(field);
//		hBox.getChildren().add(label);
//		listOfCheckboxes.put(fieldName + languageType, field);
//		vbox.getChildren().addAll(hBox, validationMessage);
//		setFieldChangeListener(field);
//		return vbox;
//	}
//
//	public <T> VBox addContentWithComboBoxObject(String fieldName, UiSchemaDTO schema, String languageType) {
//		ComboBox<GenericDto> field = new ComboBox<GenericDto>();
//		Label label = new Label();
//		Label validationMessage = new Label();
//		StringConverter<T> uiRenderForComboBox = fxUtils.getStringConverterForComboBox();
//		VBox vbox = new VBox();
//		field.setId(fieldName + languageType);
//		field.setPrefWidth(vbox.getPrefWidth());
//		helperMethodForComboBox(field, fieldName, schema, label, validationMessage, vbox, languageType);
//		field.setConverter((StringConverter<GenericDto>) uiRenderForComboBox);
//		listOfComboBoxWithObject.put(fieldName + languageType, field);
//		fxUtils.populateLocalComboBox(parentFlowPane, listOfComboBoxWithObject.get(fieldName), field);
//		setFieldChangeListener(field);
//		changeNodeOrientation(vbox, languageType);
//
//		field.managedProperty().bind(field.visibleProperty());
//		validationMessage.visibleProperty().bind(field.visibleProperty());
//		validationMessage.managedProperty().bind(field.visibleProperty());
//		vbox.visibleProperty().bind(field.visibleProperty());
//		vbox.managedProperty().bind(field.visibleProperty());
//
//		return vbox;
//	}
//
//	public <T> VBox addContentWithButtons(String fieldName, UiSchemaDTO schema, String languageType) {
//		Label label = new Label();
//		Label validationMessage = new Label();
//
//		VBox vbox = new VBox();
//		vbox.setId(fieldName + RegistrationConstants.Parent);
//
//		String mandatorySuffix = getMandatorySuffix(schema);
//		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
//			label.setText(schema.getLabel().get(RegistrationConstants.SECONDARY) + mandatorySuffix);
//			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.SECONDARY));
//		} else {
//			label.setText(schema.getLabel().get(RegistrationConstants.PRIMARY) + mandatorySuffix);
//			putIntoLabelMap(fieldName + languageType, schema.getLabel().get(RegistrationConstants.PRIMARY));
//		}
//		vbox.setPrefWidth(500);
//		label.setId(fieldName + languageType + RegistrationConstants.LABEL);
//		label.setVisible(true);
//		label.getStyleClass().add("buttonsLabel");
//		validationMessage.setId(fieldName + languageType + RegistrationConstants.MESSAGE);
//		validationMessage.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
//		label.setPrefWidth(vbox.getPrefWidth());
//		validationMessage.setPrefWidth(vbox.getPrefWidth());
//		validationMessage.setVisible(false);
//
//		if (applicationContext.getApplicationLanguage().equals(applicationContext.getLocalLanguage())
//				&& languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
//			vbox.setDisable(true);
//		}
//
//		HBox hBox = new HBox();
//		hBox.getChildren().add(label);
//
//		populateButtons(fieldName, languageType);
//
//		listOfButtons.get(fieldName + languageType).forEach(localButton -> {
//			hBox.setId(fieldName + languageType);
//			hBox.setSpacing(10);
//			hBox.setPadding(new Insets(10, 10, 10, 10));
//			localButton.setPrefWidth(vbox.getPrefWidth());
//			localButton.getStyleClass().addAll("residence", "button");
//			setFieldChangeListener(localButton);
//			hBox.getChildren().add(localButton);
//		});
//		vbox.getChildren().addAll(hBox, validationMessage);
//		changeNodeOrientation(vbox, languageType);
//
//		return vbox;
//	}
//
//	public String getMandatorySuffix(UiSchemaDTO schema) {
//		String mandatorySuffix = RegistrationConstants.EMPTY;
//		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
//		String categeory = registrationDTO.getRegistrationCategory();
//		switch (categeory) {
//			case RegistrationConstants.PACKET_TYPE_UPDATE:
//				if (registrationDTO.getUpdatableFields().contains(schema.getId())) {
//					mandatorySuffix = schema.isRequired() ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
//				}
//				break;
//
//			case RegistrationConstants.PACKET_TYPE_NEW:
//				mandatorySuffix = schema.isRequired() ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
//				break;
//		}
//		return mandatorySuffix;
//	}
//
//	private void populateButtons(String key, String languageType) {
//		try {
//			List<GenericDto> values = masterSyncService.getFieldValues(key, languageType.equals(RegistrationConstants.LOCAL_LANGUAGE) ?
//					ApplicationContext.localLanguage() : ApplicationContext.applicationLanguage());
//
//			if(values != null) {
//				values.forEach( genericDto -> {
//					Button button = new Button(genericDto.getName());
//					button.setId(key + genericDto.getCode() + languageType);
//					if(listOfButtons.get(key + languageType) == null) {
//						listOfButtons.put(key + languageType, new ArrayList<>());
//					}
//					listOfButtons.get(key + languageType).add(button);
//
//					if(!languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
//						button.addEventHandler(ActionEvent.ACTION, event -> {
//							if (button.getStyleClass().contains("residence")) {
//								resetButtons(button);
//								if (!isAppLangAndLocalLangSame()) {
//									Node localButton = getFxElement(button.getId()+RegistrationConstants.LOCAL_LANGUAGE);
//									if(localButton != null) {
//										resetButtons((Button) localButton);
//									}
//								}
//							}
//							fxUtils.toggleUIField(parentFlowPane, button.getParent().getId() + RegistrationConstants.MESSAGE, false);
//							if (!isAppLangAndLocalLangSame()) {
//								Node localButton = getFxElement(button.getId()+RegistrationConstants.LOCAL_LANGUAGE);
//								if(localButton != null) {
//									fxUtils.toggleUIField(parentFlowPane, localButton.getParent().getId() + RegistrationConstants.MESSAGE, false);
//								}
//							}
//						});
//					}
//				});
//			}
//		} catch (RegBaseCheckedException e) {
//			LOGGER.error(APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, "populateButtons failed >> " + key,
//					ExceptionUtils.getStackTrace(e));
//		}
//	}
//
//	private void resetButtons(Button button) {
//		button.getStyleClass().clear();
//		button.getStyleClass().addAll("selectedResidence", "button");
//		button.getParent().getChildrenUnmodifiable().forEach(node -> {
//			if (node instanceof Button && !node.getId().equals(button.getId())) {
//				node.getStyleClass().clear();
//				node.getStyleClass().addAll("residence", "button");
//			}
//		});
//	}
//
//	/**
//	 * setting the registration navigation label to lost uin
//	 */
//	protected void lostUIN() {
//		lostUIN = true;
//		registrationNavlabel
//				.setText(ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));
//	}
//
//	private void addFirstOrderAddress(ComboBox<GenericDto> location, int id, String languageType) {
//
//		if (location != null) {
//			location.getItems().clear();
//			try {
//				List<GenericDto> locations = null;
//				locations = masterSync.findLocationByHierarchyCode(id, languageType);
//
//				if (locations.isEmpty()) {
//					GenericDto lC = new GenericDto();
//					lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
//					lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
//					lC.setLangCode(ApplicationContext.applicationLanguage());
//					location.getItems().add(lC);
//				} else {
//					location.getItems().addAll(locations);
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			new ComboBoxAutoComplete<GenericDto>(location);
//		}
//	}
//
//	private List<GenericDto> LocationDtoToComboBoxDto(List<LocationDto> locations) {
//		List<GenericDto> listOfValues = new ArrayList<>();
//		for (LocationDto locationDto : locations) {
//			GenericDto comboBox = new GenericDto();
//			comboBox.setCode(locationDto.getCode());
//			comboBox.setName(locationDto.getName());
//			comboBox.setLangCode(locationDto.getLangCode());
//			listOfValues.add(comboBox);
//		}
//		return listOfValues;
//	}
//
//	private void addDemoGraphicDetailsToSession() {
//		try {
//			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
//			for (UiSchemaDTO schemaField : validation.getValidationMap().values()) {
//				if (schemaField.getControlType() == null || !schemaField.isInputRequired())
//					continue;
//
//				if (registrationDTO.getRegistrationCategory().equals(RegistrationConstants.PACKET_TYPE_UPDATE)
//						&& !registrationDTO.getUpdatableFields().contains(schemaField.getId()))
//					continue;
//
//				addFieldValueToSession(schemaField);
//			}
//		} catch (Exception exception) {
//			LOGGER.error("addDemoGraphicDetailsToSession", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
//		}
//	}
//
//	private void addFieldValueToSession(UiSchemaDTO schemaField) {
//		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
//		switch (schemaField.getType()) {
//			case RegistrationConstants.SIMPLE_TYPE:
//				switch (schemaField.getControlType()) {
//					case RegistrationConstants.DROPDOWN:
//						ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
//						ComboBox<GenericDto> localField = listOfComboBoxWithObject.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//						registrationDTO.addDemographicField(schemaField.getId(),
//								applicationContext.getApplicationLanguage(),
//								platformField == null ? null
//										: platformField.getValue() != null ? platformField.getValue().getName() : null,
//								applicationContext.getLocalLanguage(), localField == null ? null
//										: localField.getValue() != null ? localField.getValue().getName() : null);
//						break;
//					case RegistrationConstants.BUTTON:
//						List<Button> platformFieldButtons = listOfButtons.get(schemaField.getId());
//						List<Button> localFieldButtons = listOfButtons.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//						platformFieldButtons.forEach(button -> {
//							if (button.getStyleClass().contains("selectedResidence")) {
//								registrationDTO.addDemographicField(schemaField.getId(),
//										applicationContext.getApplicationLanguage(), button.getText(),
//										applicationContext.getLocalLanguage(),
//										getLocalFieldButtonText(button, localFieldButtons));
//							}
//						});
//						break;
//					default:
//						TextField platformTextField = listOfTextField.get(schemaField.getId());
//						TextField localTextField = listOfTextField.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//						registrationDTO.addDemographicField(schemaField.getId(), applicationContext.getApplicationLanguage(),
//								platformTextField.getText() == null ? null : platformTextField.getText(), applicationContext.getLocalLanguage(),
//								(localTextField == null || localTextField.getText() == null) ? null : localTextField.getText());
//						break;
//				}
//				break;
//			case RegistrationConstants.NUMBER:
//			case RegistrationConstants.STRING:
//				switch (schemaField.getControlType()) {
//					case RegistrationConstants.AGE_DATE:
//						registrationDTO.setDateField(schemaField.getId(),
//								listOfTextField.get(schemaField.getId() + "__" + RegistrationConstants.DD).getText(),
//								listOfTextField.get(schemaField.getId() + "__" + RegistrationConstants.MM).getText(),
//								listOfTextField.get(schemaField.getId() + "__" + RegistrationConstants.YYYY).getText());
//						break;
//					case RegistrationConstants.CHECKBOX:
//						CheckBox checkBox = listOfCheckboxes.get(schemaField.getId());
//						registrationDTO.addDemographicField(schemaField.getId(), checkBox == null ? "N"
//								: checkBox.isSelected() ? "Y" : "N");
//						break;
//					case RegistrationConstants.DROPDOWN:
//						ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
//						registrationDTO.addDemographicField(schemaField.getId(), platformField == null ? null
//								: platformField.getValue() != null ? platformField.getValue().getName() : null);
//						break;
//					default:
//						registrationDTO.addDemographicField(schemaField.getId(), listOfTextField.get(schemaField.getId()) != null ?
//								listOfTextField.get(schemaField.getId()).getText() : null);
//						break;
//				}
//				break;
//			default:
//				break;
//		}
//	}
//
//
//	private String getLocalFieldButtonText(Button button, List<Button> localFieldButtons) {
//		if (!isLocalLanguageAvailable() || isAppLangAndLocalLangSame()) {
//			return null;
//		}
//		String buttonText = null;
//		Optional<Button> localButton = localFieldButtons.stream().filter(btn -> btn.getId().contains(button.getId()))
//				.findAny();
//		if (localButton.isPresent()) {
//			buttonText = localButton.get().getText();
//		}
//		return buttonText;
//	}
//
//	/**
//	 * To load the provinces in the selection list based on the language code
//	 */
//	private void configureMethodsForAddress(int s, int p, int size, List<String> orderOfAddress) {
//		try {
//			retrieveAndPopulateLocationByHierarchy(listOfComboBoxWithObject.get(orderOfAddress.get(s)),
//					listOfComboBoxWithObject.get(orderOfAddress.get(p)),
//					listOfComboBoxWithObject.get(orderOfAddress.get(p) + RegistrationConstants.LOCAL_LANGUAGE));
//
//			for (int i = p + 1; i < size; i++) {
//				listOfComboBoxWithObject.get(orderOfAddress.get(i)).getItems().clear();
//
//				if (!isAppLangAndLocalLangSame()) {
//					listOfComboBoxWithObject.get(orderOfAddress.get(i) + RegistrationConstants.LOCAL_LANGUAGE)
//							.getItems().clear();
//				}
//			}
//		} catch (RuntimeException runtimeException) {
//			LOGGER.error(" falied due to invalid field", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//		}
//
//	}
//
//	/**
//	 *
//	 * Saving the detail into concerned DTO'S
//	 *
//	 */
//	public void saveDetail() {
//		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
//				RegistrationConstants.APPLICATION_ID, "Saving the fields to DTO");
//		try {
//			auditFactory.audit(AuditEvent.SAVE_DETAIL_TO_DTO, Components.REGISTRATION_CONTROLLER,
//					SessionContext.userContext().getUserId(), RegistrationConstants.ONBOARD_DEVICES_REF_ID_TYPE);
//
//			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
//
//			addDemoGraphicDetailsToSession();
//
//			SessionContext.map().put(RegistrationConstants.IS_Child, registrationDTO.isChild());
//
//			registrationDTO.getOsiDataDTO().setOperatorID(SessionContext.userContext().getUserId());
//
//			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
//					RegistrationConstants.APPLICATION_ID, "Saved the demographic fields to DTO");
//
//		} catch (Exception exception) {
//			LOGGER.error("REGISTRATION - SAVING THE DETAILS FAILED ", APPLICATION_NAME,
//					RegistrationConstants.APPLICATION_ID,
//					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
//		}
//	}
//
//	public void uinUpdate() {
//		List<String> selectionList = getRegistrationDTOFromSession().getUpdatableFields();
//		if (selectionList != null) {
//			disablePreRegFetch();
//			registrationNavlabel.setText(applicationLabelBundle.getString("uinUpdateNavLbl"));
//		}
//		for (Entry<String, UiSchemaDTO> selectionField : validation.getValidationMap().entrySet()) {
//
//			updateDemographicScreen(selectionField.getKey(), selectionList, false);
//			updateDemographicScreen(selectionField.getKey() + RegistrationConstants.LOCAL_LANGUAGE, selectionList,
//					false);
//
//			if (getRegistrationDTOFromSession().getDefaultUpdatableFields().contains(selectionField.getKey())) {
//				updateDemographicScreen(selectionField.getKey(), selectionList, true);
//				updateDemographicScreen(selectionField.getKey() + RegistrationConstants.LOCAL_LANGUAGE, selectionList,
//						true);
//
//			}
//		}
//
//	}
//
//	private void updateDemographicScreen(String key, List<String> selectionList, boolean isDefault) {
//
//		if (getFxElement(key) != null) {
//			if (getFxElement(key).getParent() != null) {
//
//				boolean isDisable = true;
//
//				if (selectionList.contains(key.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, "")) || isDefault) {
//					isDisable = false;
//				}
//
//				getFxElement(key).getParent().getParent().setDisable(isDisable);
//
//			}
//		}
//	}
//
//	/**
//	 * This method is to prepopulate all the values for edit operation
//	 */
//	public void prepareEditPageContent() {
//		try {
//			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
//					RegistrationConstants.APPLICATION_ID, "Preparing the Edit page content");
//
//			Map<String, Object> demographics = new HashMap<>();
//			demographics.putAll(getRegistrationDTOFromSession().getDemographics());
//
//			for (UiSchemaDTO schemaField : validation.getValidationMap().values()) {
//				Object value = demographics.get(schemaField.getId());
//				if (value == null)
//					continue;
//
//				switch (schemaField.getType()) {
//					case RegistrationConstants.SIMPLE_TYPE:
//						switch (schemaField.getControlType()) {
//							case RegistrationConstants.DROPDOWN:
//								populateFieldValue(listOfComboBoxWithObject.get(schemaField.getId()),
//										listOfComboBoxWithObject.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE),
//										(List<SimpleDto>) value);
//								break;
//							case RegistrationConstants.BUTTON:
//								String platformLanguageCode = applicationContext.getApplicationLanguage();
//								List<Button> platformFieldButtons = listOfButtons.get(schemaField.getId());
//								List<Button> localLangFieldButtons = listOfButtons.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//								for (SimpleDto fieldValue : (List<SimpleDto>) value) {
//									Optional<Button> button = fieldValue.getLanguage().equalsIgnoreCase(platformLanguageCode) ?
//											platformFieldButtons.stream().filter(b -> b.getId().equals(schemaField.getId() + fieldValue.getValue())).findFirst() :
//											localLangFieldButtons.stream().filter(b -> b.getId().equals(schemaField.getId() + fieldValue.getValue() +
//													RegistrationConstants.LOCAL_LANGUAGE)).findFirst();
//									if(button.isPresent())
//										resetButtons(button.get());
//								}
//								break;
//							default:
//								populateFieldValue(listOfTextField.get(schemaField.getId()),
//										listOfTextField.get(schemaField.getId() + RegistrationConstants.LOCAL_LANGUAGE),
//										(List<SimpleDto>) value);
//								break;
//						}
//						break;
//					case RegistrationConstants.NUMBER:
//					case RegistrationConstants.STRING:
//						switch (schemaField.getControlType()) {
//							case RegistrationConstants.AGE_DATE:
//								String[] dateParts = ((String) value).split("/");
//								if (dateParts.length == 3) {
//									listOfTextField.get(schemaField.getId() + "__" + "dd").setText(dateParts[2]);
//									listOfTextField.get(schemaField.getId() + "__" + "mm").setText(dateParts[1]);
//									listOfTextField.get(schemaField.getId() + "__" + "yyyy").setText(dateParts[0]);
//								}
//								break;
//							case RegistrationConstants.DROPDOWN:
//								ComboBox<GenericDto> platformField = listOfComboBoxWithObject.get(schemaField.getId());
//								if (platformField != null) {
//									platformField.setValue(new GenericDto((String) value, (String) value, applicationContext.getApplicationLanguage()));
//								}
//								break;
//							case RegistrationConstants.CHECKBOX:
//								CheckBox checkBox = listOfCheckboxes.get(schemaField.getId());
//								if(checkBox != null) {
//									checkBox.setSelected(((String) value).equalsIgnoreCase("Y") ? true : false);
//								}
//								break;
//							default:
//								TextField textField = listOfTextField.get(schemaField.getId());
//								if (textField != null) {
//									textField.setText((String) value);
//								}
//								break;
//						}
//						break;
//				}
//			}
//
//			preRegistrationId.setText(getRegistrationDTOFromSession().getPreRegistrationId());
//
//		} catch (RuntimeException runtimeException) {
//			LOGGER.error(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
//					RegistrationConstants.APPLICATION_ID,
//					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//		}
//
//	}
//
//	/**
//	 * Method to populate the local field value
//	 *
//	 */
//	private void populateFieldValue(Node nodeForPlatformLang, Node nodeForLocalLang, List<SimpleDto> fieldValues) {
//		if (fieldValues != null) {
//			String platformLanguageCode = applicationContext.getApplicationLanguage();
//			String localLanguageCode = applicationContext.getLocalLanguage();
//			String valueInPlatformLang = RegistrationConstants.EMPTY;
//			String valueinLocalLang = RegistrationConstants.EMPTY;
//
//			for (SimpleDto fieldValue : fieldValues) {
//				if (fieldValue.getLanguage().equalsIgnoreCase(platformLanguageCode)) {
//					valueInPlatformLang = fieldValue.getValue();
//				} else if (nodeForLocalLang != null && fieldValue.getLanguage().equalsIgnoreCase(localLanguageCode)) {
//					valueinLocalLang = fieldValue.getValue();
//				}
//			}
//
//			if (nodeForPlatformLang instanceof TextField) {
//				((TextField) nodeForPlatformLang).setText(valueInPlatformLang);
//
//				if (nodeForLocalLang != null) {
//					((TextField) nodeForLocalLang).setText(valueinLocalLang);
//				}
//			} else if (nodeForPlatformLang instanceof ComboBox) {
//				fxUtils.selectComboBoxValue((ComboBox<?>) nodeForPlatformLang, valueInPlatformLang);
//			}
//		}
//	}
//
//	/**
//	 * Method to fetch the pre-Registration details
//	 */
//	@FXML
//	private void fetchPreRegistration() {
//
//		String preRegId = preRegistrationId.getText();
//
//		if (StringUtils.isEmpty(preRegId)) {
//			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PRE_REG_ID_EMPTY);
//			return;
//		} else {
//			try {
//				pridValidatorImpl.validateId(preRegId);
//			} catch (InvalidIDException invalidIDException) {
//				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PRE_REG_ID_NOT_VALID);
//				LOGGER.error("PRID VALIDATION FAILED", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//						invalidIDException.getMessage() + ExceptionUtils.getStackTrace(invalidIDException));
//				return;
//			}
//		}
//
//		auditFactory.audit(AuditEvent.REG_DEMO_PRE_REG_DATA_FETCH, Components.REG_DEMO_DETAILS, SessionContext.userId(),
//				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
//
//		registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW);
//		documentScanController.clearDocSection();
//
//		ResponseDTO responseDTO = preRegistrationDataSyncService.getPreRegistration(preRegId);
//
//		SuccessResponseDTO successResponseDTO = responseDTO.getSuccessResponseDTO();
//		List<ErrorResponseDTO> errorResponseDTOList = responseDTO.getErrorResponseDTOs();
//
//		if (successResponseDTO != null && successResponseDTO.getOtherAttributes() != null
//				&& successResponseDTO.getOtherAttributes().containsKey(RegistrationConstants.REGISTRATION_DTO)) {
//			SessionContext.map().put(RegistrationConstants.REGISTRATION_DATA,
//					successResponseDTO.getOtherAttributes().get(RegistrationConstants.REGISTRATION_DTO));
//			getRegistrationDTOFromSession().setPreRegistrationId(preRegId);
//			prepareEditPageContent();
//
//		} else if (errorResponseDTOList != null && !errorResponseDTOList.isEmpty()) {
//			generateAlertLanguageSpecific(RegistrationConstants.ERROR, errorResponseDTOList.get(0).getMessage());
//		}
//	}
//
//	/**
//	 *
//	 * Setting the focus to specific fields when keyboard loads
//	 *
//	 */
//	public void setFocusonLocalField(MouseEvent event) {
//		try {
//			Node node = (Node) event.getSource();
//			if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//				node.requestFocus();
//				keyboardNode.setVisible(true);
//				keyboardNode.setManaged(true);
//				//addKeyboard(positionTracker.get((node.getId() + "ParentGridPane")) + 1);
//				Node parentNode = node.getParent().getParent();
//				if (keyboardVisible) {
//					if(previousNode != null) {
//						((VBox)previousNode).getChildren().remove(lastPosition - 1);
//					}
//					keyboardVisible = false;
//				} else {
//					listOfTextField.get(node.getId() + "LocalLanguage").requestFocus();
//					GridPane gridPane = prepareMainGridPaneForKeyboard();
//					gridPane.addColumn(1, keyboardNode);
//					((VBox)parentNode).getChildren().add(gridPane);
//					previousNode = parentNode;
//					keyboardVisible = true;
//					lastPosition = ((VBox)parentNode).getChildren().size();
//				}
//			}
//		} catch (RuntimeException runtimeException) {
//			LOGGER.error("REGISTRATION - SETTING FOCUS ON LOCAL FIELD FAILED", APPLICATION_NAME,
//					RegistrationConstants.APPLICATION_ID,
//					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//		}
//	}
//
//	/**
//	 * Method to go back to previous page
//	 */
//	@FXML
//	private void back() {
//		try {
//			if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
//				Parent uinUpdate = BaseController.load(getClass().getResource(RegistrationConstants.UIN_UPDATE));
//				getScene(uinUpdate);
//			} else {
//				goToHomePageFromRegistration();
//			}
//		} catch (IOException exception) {
//			LOGGER.error("COULD NOT LOAD HOME PAGE", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
//		}
//	}
//
//	/**
//	 * Method to go back to next page
//	 */
//	@FXML
//	private void next() {
//		if (preRegistrationId.getText().isEmpty()) {
//			preRegistrationId.clear();
//		}
//		// Its required to save before validation as, on spot check for values during
//		// MVEL validation
//		saveDetail();
//
//		if (registrationController.validateDemographicPane(parentFlowPane)) {
//			guardianBiometricsController.populateBiometricPage(false, false);
//			documentScanController.populateDocumentCategories();
//			auditFactory.audit(AuditEvent.REG_DEMO_NEXT, Components.REG_DEMO_DETAILS, SessionContext.userId(),
//					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
//			registrationController.showCurrentPage(RegistrationConstants.DEMOGRAPHIC_DETAIL,
//					getPageByAction(RegistrationConstants.DEMOGRAPHIC_DETAIL, RegistrationConstants.NEXT));
//		}
//	}
//
//	/**
//	 * Retrieving and populating the location by hierarchy
//	 */
//	private void retrieveAndPopulateLocationByHierarchy(ComboBox<GenericDto> srcLocationHierarchy,
//														ComboBox<GenericDto> destLocationHierarchy, ComboBox<GenericDto> destLocationHierarchyInLocal) {
//		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
//				RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
//				"Retrieving and populating of location by selected hirerachy started");
//
//		try {
//
//			GenericDto selectedLocationHierarchy = srcLocationHierarchy.getSelectionModel().getSelectedItem();
//			if (selectedLocationHierarchy != null) {
//				destLocationHierarchy.getItems().clear();
//
//				if (isLocalLanguageAvailable() && destLocationHierarchyInLocal != null) {
//					destLocationHierarchyInLocal.getItems().clear();
//				}
//				if (selectedLocationHierarchy.getCode().equalsIgnoreCase(RegistrationConstants.AUDIT_DEFAULT_USER)) {
//					destLocationHierarchy.getItems().add(selectedLocationHierarchy);
//
//					if (isLocalLanguageAvailable() && destLocationHierarchyInLocal != null) {
//						destLocationHierarchyInLocal.getItems().add(selectedLocationHierarchy);
//					}
//				} else {
//
//					List<GenericDto> locations = masterSync.findProvianceByHierarchyCode(
//							selectedLocationHierarchy.getCode(), selectedLocationHierarchy.getLangCode());
//
//					if (locations.isEmpty()) {
//						GenericDto lC = new GenericDto();
//						lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
//						lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
//						lC.setLangCode(ApplicationContext.applicationLanguage());
//						destLocationHierarchy.getItems().add(lC);
//
//						if (isLocalLanguageAvailable() && destLocationHierarchyInLocal != null) {
//							destLocationHierarchyInLocal.getItems().add(lC);
//						}
//					} else {
//						destLocationHierarchy.getItems().addAll(locations);
//					}
//					new ComboBoxAutoComplete<GenericDto>(destLocationHierarchy);
//					if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//						List<GenericDto> locationsSecondary = masterSync.findProvianceByHierarchyCode(
//								selectedLocationHierarchy.getCode(), ApplicationContext.localLanguage());
//
//						if (locationsSecondary.isEmpty()) {
//							GenericDto lC = new GenericDto();
//							lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
//							lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
//							lC.setLangCode(ApplicationContext.localLanguage());
//							destLocationHierarchyInLocal.getItems().add(lC);
//
//						} else {
//							destLocationHierarchyInLocal.getItems().addAll(locationsSecondary);
//						}
//						new ComboBoxAutoComplete<GenericDto>(destLocationHierarchyInLocal);
//					}
//				}
//			}
//		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
//			LOGGER.error("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
//					APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//		}
//		LOGGER.info("REGISTRATION - INDIVIDUAL_REGISTRATION - RETRIEVE_AND_POPULATE_LOCATION_BY_HIERARCHY",
//				RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
//				"Retrieving and populating of location by selected hirerachy ended");
//	}
//
//	private boolean isLocationField(String fieldId) {
//		boolean isLocationField = false;
//		for (Entry<String, List<String>> entry : orderOfAddressListByGroup.entrySet()) {
//			if (entry.getValue().contains(fieldId)) {
//				isLocationField = true;
//				break;
//			}
//
//		}
//		return isLocationField;
//
//	}
//
//	private void addGroupContent(List<UiSchemaDTO> uiSchemaDTOs, GridPane groupGridPane) {
//
//		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, "Adding group contents");
//
//		GridPane horizontalRowGridPane = null;
//		if (uiSchemaDTOs != null && !uiSchemaDTOs.isEmpty()) {
//
//			if (uiSchemaDTOs.size() > 0) {
//				LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//						"Requesting prepare grid pane for horizontal layout");
//
//				horizontalRowGridPane = prepareRowGridPane(uiSchemaDTOs.size());
//			}
//
//			for (int index = 0; index < uiSchemaDTOs.size(); index++) {
//
//				LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//						"Adding ui schema of application language");
//				GridPane applicationLanguageGridPane = subGridPane(uiSchemaDTOs.get(index), "", uiSchemaDTOs.size());
//
//				GridPane rowGridPane = horizontalRowGridPane == null ? prepareRowGridPane(1) : horizontalRowGridPane;
//
//				rowGridPane.addColumn(horizontalRowGridPane == null ? 0 : index, applicationLanguageGridPane);
//
//				if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()
//						&& uiSchemaDTOs.get(index).getType().equals(RegistrationConstants.SIMPLE_TYPE)) {
//
//					LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//							"Adding ui schema of local language");
//
//					GridPane localLanguageGridPane = subGridPane(uiSchemaDTOs.get(index),
//							RegistrationConstants.LOCAL_LANGUAGE,
//							uiSchemaDTOs.size() > 1 ? uiSchemaDTOs.size() + 4 : uiSchemaDTOs.size());
//
//					if (localLanguageGridPane != null) {
//						rowGridPane.addColumn(horizontalRowGridPane == null ? 2 : uiSchemaDTOs.size() + index,
//								localLanguageGridPane);
//					}
//				}
//
//				if (horizontalRowGridPane == null) {
//					LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//							"Setting vertical row gridpane for : " + uiSchemaDTOs.get(index).getId());
//					groupGridPane.getChildren().add(rowGridPane);
//				}
//
//			}
//
//		}
//
//		if (horizontalRowGridPane != null) {
//			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					"Setting horizontal row gridpane for group : ");
//			groupGridPane.getChildren().add(horizontalRowGridPane);
//		}
//	}
//
//	private GridPane prepareRowGridPane(int noOfItems) {
//		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//				"Preparing grid pane for items : " + noOfItems);
//		GridPane gridPane = new GridPane();
//		gridPane.setPrefWidth(1200);
//		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
//
//		if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					"Preparing grid pane for items : " + noOfItems + " left hand side");
//
//			// Left Hand Side
//			setColumnConstraints(columnConstraints, 50, noOfItems);
//
//			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					"Preparing grid pane for items : " + noOfItems + " right hand side");
//			// Right Hand Side
//			setColumnConstraints(columnConstraints, 50, noOfItems);
//
//		} else {
//			setColumnConstraints(columnConstraints, 100, noOfItems);
//		}
//		return gridPane;
//	}
//
//	private void setColumnConstraints(ObservableList<ColumnConstraints> columnConstraints, int currentPaneWidth,
//									  int noOfItems) {
//		if (columnConstraints != null) {
//			for (int index = 1; index <= noOfItems; ++index) {
//				ColumnConstraints columnConstraint = new ColumnConstraints();
//				columnConstraint.setPercentWidth(currentPaneWidth / noOfItems);
//				columnConstraints.add(columnConstraint);
//			}
//		}
//	}
//
//	private void setFieldChangeListener(Node node) {
//		node.addEventHandler(Event.ANY, event -> {
//			if (validateFieldValue(node)) {
//				// Group level visibility listeners
//				refreshDemographicGroups();
//				//handling other handlers
//				UiSchemaDTO uiSchemaDTO = validation.getValidationMap().get(node.getId().replaceAll(RegistrationConstants.ON_TYPE,
//						RegistrationConstants.EMPTY).replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY));
//				if(uiSchemaDTO != null) {
//					LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//							"Invoking external action handler for .... " + uiSchemaDTO.getId());
//					demographicChangeActionHandler.actionHandle(parentFlowPane, node.getId(), uiSchemaDTO.getChangeAction());
//				}
//				// Group level visibility listeners
//				refreshDemographicGroups();
//			}
//		});
//	}
//
//	private boolean validateFieldValue(Node field) {
//		if(field == null) {
//			LOGGER.warn(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					"Field not found in demographic screen");
//			return false;
//		}
//
//		if(field instanceof TextField) {
//			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					"validating text field");
//			if (!isInputTextValid((TextField) field, field.getId())) {
//				fxUtils.showErrorLabel((TextField) field, parentFlowPane);
//				return false;
//			}
//			else
//				fxUtils.setTextValidLabel(parentFlowPane, (TextField) field);
//
//			LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//					"validating text field secondary");
//
//			TextField localField = (TextField) getFxElement(field.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//			if (localField != null) {
//				//on valid value of primary set secondary language value
//				setSecondaryLangText((TextField) field, localField, hasToBeTransliterated);
//
//				if (!isInputTextValid(localField, localField.getId())) {
//					fxUtils.showErrorLabel(localField, parentFlowPane);
//					return false;
//				}
//				else
//					fxUtils.setTextValidLabel(parentFlowPane, (TextField) localField);
//			}
//		}
//		return true;
//	}
//
//
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	private void refreshDemographicGroups() {
//		addDemoGraphicDetailsToSession();
//		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//				"Refreshing demographic groups");
//		Map<String, Map<String, Object>> context = new HashMap();
//		context.put("identity", getRegistrationDTOFromSession() == null ?  new HashMap<>():
//				getRegistrationDTOFromSession().getMVELDataContext());
//
//		validation.getValidationMap().values().stream().filter(f -> isDemographicField(f)).forEach(field -> {
//			try {
//				if (field.getVisible() != null && RegistrationConstants.MVEL_TYPE.equalsIgnoreCase(field.getVisible().getEngine())) {
//					VariableResolverFactory resolverFactory = new MapVariableResolverFactory(context);
//					Object required = MVEL.eval(field.getVisible().getExpr(), resolverFactory);
//					updateFields(Arrays.asList(field), required != null ? (boolean) required : false);
//				}
//			} catch (Throwable t) {
//				LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//						ExceptionUtils.getStackTrace(t));
//			}
//		});
//	}
//
//	private void updateFields(List<UiSchemaDTO> fields, boolean isVisible) {
//		for (UiSchemaDTO field : fields) {
//			Node node = getFxElement(field.getId());
//			Node localLangNode = getFxElement(field.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//			Node labelNode = getFxElement(field.getId()+RegistrationConstants.LABEL);
//			Node localLabelNode = getFxElement(field.getId()+ RegistrationConstants.LOCAL_LANGUAGE+RegistrationConstants.LABEL);
//
//			manageFieldVisibility(node, labelNode, isVisible);
//			manageFieldVisibility(localLangNode, localLabelNode, isVisible);			
//		}
//	}
//	
//	private void manageFieldVisibility(Node node, Node labelNode, boolean isVisible) {
//		if(!isVisible) { clearFieldValue(node); }
//
//		if(node != null) {
//			if(labelNode != null) {
//				labelNode.setVisible(isVisible);
//				labelNode.setManaged(isVisible);
//			}
//			node.setVisible(isVisible);
//		}
//	}
//
//	private void clearFieldValue(Node node) {
//		if(node == null) { return; }
//		node.setDisable(false);
//		getRegistrationDTOFromSession().removeDemographicField(node.getId());
//
//		if(node instanceof TextField)
//			((TextField) node).setText("");
//
//		else if (node instanceof ComboBox)
//			((ComboBox<?>) node).getSelectionModel().clearSelection();
//
//		else if(node instanceof Button)
//			((Button) node).setDefaultButton(false);
//
//		else if(node instanceof CheckBox)
//			((CheckBox) node).setSelected(false);
//	}
//
//	private Node getFxElement(String fieldId) {
//		return parentFlowPane.lookup(RegistrationConstants.HASH + fieldId);
//	}
//
//	private boolean isInputTextValid(TextField textField, String id) {
//		return validation.validateTextField(parentFlowPane, textField, id, true);
//	}
//
//	private void setSecondaryLangText(TextField primaryField, TextField secondaryField, boolean haveToTransliterate) {
//		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//				"Setting secondary language for field : " + primaryField.getId());
//		if (secondaryField != null) {
//			if (haveToTransliterate) {
//				try {
//					secondaryField.setText(transliteration.transliterate(ApplicationContext.applicationLanguage(),
//							ApplicationContext.localLanguage(), primaryField.getText()));
//				} catch (RuntimeException runtimeException) {
//					LOGGER.error(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//							"Exception occured while transliterating secondary language for field : "
//									+ primaryField.getId()  + " due to >>>> " + runtimeException.getMessage());
//					secondaryField.setText(primaryField.getText());
//				}
//			} else {
//				LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
//						"Not transliteration into local language" + primaryField.getId());
//				secondaryField.setText(primaryField.getText());
//			}
//		}
//	}
//
//	private Map<String, List<UiSchemaDTO>> getTemplateGroupMap() {
//
//		Map<String, List<UiSchemaDTO>> templateGroupMap = new LinkedHashMap<>();
//
//		for (Entry<String, UiSchemaDTO> entry : validation.getValidationMap().entrySet()) {
//			if (isDemographicField(entry.getValue())) {
//
//				List<UiSchemaDTO> list = templateGroupMap.get(entry.getValue().getAlignmentGroup());
//				if (list == null) {
//					list = new LinkedList<UiSchemaDTO>();
//				}
//				list.add(entry.getValue());
//				templateGroupMap.put(entry.getValue().getAlignmentGroup() == null ? entry.getKey() + "TemplateGroup"
//						: entry.getValue().getAlignmentGroup(), list);
//			}
//		}
//		return templateGroupMap;
//
//	}
}
