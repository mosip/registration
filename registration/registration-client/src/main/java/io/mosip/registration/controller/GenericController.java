package io.mosip.registration.controller;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.mosip.registration.entity.LocationHierarchy;
import io.mosip.registration.util.control.impl.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.SneakyThrows;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.RegistrationPreviewController;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.ScreenDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.dto.response.UiScreenDTO;
import io.mosip.registration.entity.Location;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import static io.mosip.registration.constants.RegistrationConstants.*;

/**
 * {@code GenericController} is to capture the demographic/demo/Biometric
 * details
 *
 * @author YASWANTH S
 * @since 1.0.0
 *
 */

@Controller
public class GenericController extends BaseController {

	protected static final Logger LOGGER = AppConfig.getLogger(GenericController.class);
	private static final String loggerClassName = "GenericController";
	private static final String CONTROLTYPE_TEXTFIELD = "textbox";
	private static final String CONTROLTYPE_BIOMETRICS = "biometrics";
	private static final String CONTROLTYPE_DOCUMENTS = "fileupload";
	private static final String CONTROLTYPE_DROPDOWN = "dropdown";
	private static final String CONTROLTYPE_CHECKBOX = "checkbox";
	private static final String CONTROLTYPE_BUTTON = "button";
	private static final String CONTROLTYPE_DOB = "date";
	private static final String CONTROLTYPE_DOB_AGE = "ageDate";
	private static final String CONTROLTYPE_HTML = "html";
	private static String CLICKABLE = "paginationLabelFilled";
	private static String NON_CLICKABLE = "paginationLabel";

	/**
	 * Top most Grid pane in FXML
	 */
	@FXML
	private GridPane genericScreen;

	@FXML
	private AnchorPane anchorPane;

	@FXML
	private AnchorPane navigationAnchorPane;

	@FXML
	private Button next;

	@FXML
	private Button previous;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private MasterSyncDao masterSyncDao;

	@Autowired
	private MasterSyncService masterSyncService;

	@Autowired
	private RegistrationPreviewController registrationPreviewController;

	@Autowired
	private RequiredFieldValidator requiredFieldValidator;

	@Autowired
	private PridValidator<String> pridValidatorImpl;

	@Autowired
	private PreRegistrationDataSyncService preRegistrationDataSyncService;

	private ApplicationContext applicationContext = ApplicationContext.getInstance();

	private static TreeMap<Integer, UiScreenDTO> orderedScreens = new TreeMap<>();
	private static Map<String, FxControl> fxControlMap = new HashMap<String, FxControl>();
	private static Map<String, List<String>> fieldMap = new HashMap<String, List<String>>();

	public static Map<String, TreeMap<Integer, String>> hierarchyLevels = new HashMap<String, TreeMap<Integer, String>>();
	public static Map<String, TreeMap<Integer, String>> currentHierarchyMap = new HashMap<String, TreeMap<Integer, String>>();

	public static Map<String, FxControl> getFxControlMap() {
		return fxControlMap;
	}

	private void initialize() {
		orderedScreens.clear();
		fxControlMap.clear();
		fieldMap.clear();
		hierarchyLevels.clear();
		currentHierarchyMap.clear();
		fillHierarchicalLevelsByLanguage();
		anchorPane.prefWidthProperty().bind(genericScreen.widthProperty());
		anchorPane.prefHeightProperty().bind(genericScreen.heightProperty());
	}


	private void fillHierarchicalLevelsByLanguage() {
		//TODO
		/*List<LocationHierarchy> hierarchies = masterSyncDao.getAllLocationHierarchy("eng");
		hierarchies.forEach( hierarchy -> {
			locationHierarchy.put(hierarchy.getHierarchyLevelName(), hierarchy.getHierarchyLevel()+"");
			locationHierarchy.put(hierarchy.getHierarchyLevel()+"", hierarchy.getHierarchyLevelName());
		});*/
		TreeMap<Integer, String> eng_hierarchical = new TreeMap<>();
		eng_hierarchical.put(0, "Country");
		eng_hierarchical.put(1, "Region");
		eng_hierarchical.put(2, "Province");
		eng_hierarchical.put(3, "City");
		eng_hierarchical.put(4, "Zone");
		eng_hierarchical.put(5, "Postal Code");
		hierarchyLevels.put("eng", eng_hierarchical);

		TreeMap<Integer, String> ara_hierarchical = new TreeMap<>();
		ara_hierarchical.put(0, "بلد");
		ara_hierarchical.put(1, "منطقة");
		ara_hierarchical.put(2, "المحافظة");
		ara_hierarchical.put(3, "مدينة");
		ara_hierarchical.put(4, "منطقة");
		ara_hierarchical.put(5, "الرمز البريدي");
		hierarchyLevels.put("ara", ara_hierarchical);

		TreeMap<Integer, String> fra_hierarchical = new TreeMap<>();
		fra_hierarchical.put(0, "Pays");
		fra_hierarchical.put(1, "Région");
		fra_hierarchical.put(2, "Province");
		fra_hierarchical.put(3, "Ville");
		fra_hierarchical.put(4, "Zone");
		fra_hierarchical.put(5, "code postal");
		hierarchyLevels.put("fra", fra_hierarchical);
	}

	private HBox getPreRegistrationFetchComponent() {
		HBox hBox = new HBox();
		hBox.setSpacing(10);
		hBox.setPrefHeight(100);
		hBox.setPrefWidth(200);

		Label label = new Label();
		label.setId("preRegistrationLabel");
		label.setText(applicationContext.getBundle(ApplicationContext.applicationLanguage(), RegistrationConstants.LABELS)
				.getString("search_for_Pre_registration_id"));
		hBox.getChildren().add(label);
		TextField textField = new TextField();
		textField.setId("preRegistrationId");
		hBox.getChildren().add(textField);
		Button button = new Button();
		button.setId("fetchBtn");
		button.setStyle("demoGraphicPaneContentButton");
		button.setText(applicationContext.getBundle(ApplicationContext.applicationLanguage(), RegistrationConstants.LABELS)
				.getString("fetch"));

		button.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ResponseDTO responseDTO = preRegistrationDataSyncService.getPreRegistration(textField.getText(), false);
				
				getRegistrationDTOFromSession().setPreRegistrationId(textField.getText());

				
				try {
					loadPreRegSync(responseDTO);
				} catch (RegBaseCheckedException exception) {
					generateAlertLanguageSpecific(RegistrationConstants.ERROR, responseDTO.getErrorResponseDTOs().get(0).getMessage());
				}
			}
		});

		hBox.getChildren().add(button);
		return hBox;
	}

	private void loadPreRegSync(ResponseDTO responseDTO) throws RegBaseCheckedException{
		SuccessResponseDTO successResponseDTO = responseDTO.getSuccessResponseDTO();
		List<ErrorResponseDTO> errorResponseDTOList = responseDTO.getErrorResponseDTOs();

		
		if (errorResponseDTOList != null && !errorResponseDTOList.isEmpty() || 
				successResponseDTO==null || 
				successResponseDTO.getOtherAttributes() == null || 
				!successResponseDTO.getOtherAttributes().containsKey(RegistrationConstants.REGISTRATION_DTO)) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.PRE_REG_SYNC_FAIL.getErrorCode(),
					RegistrationExceptionConstants.PRE_REG_SYNC_FAIL.getErrorMessage());
		}
			
		RegistrationDTO preRegistrationDTO = (RegistrationDTO) successResponseDTO.getOtherAttributes()
					.get(RegistrationConstants.REGISTRATION_DTO);

			for (Entry<String, List<String>> entry : fieldMap.entrySet()) {

				for (String field : entry.getValue()) {

					FxControl fxControl = getFxControl(field);

					if (fxControl != null) {

						if (preRegistrationDTO.getDemographics().containsKey(field)) {

							fxControl.selectAndSet(preRegistrationDTO.getDemographics().get(field));
						}

						else if (preRegistrationDTO.getDocuments().containsKey(field)) {
							fxControl.selectAndSet(preRegistrationDTO.getDocuments().get(field));

						}
					}
				}
			}

		 
	

		
	}
	private void getScreens(SchemaDto schema) {
		List<UiScreenDTO> screenDTOS = schema.getScreens();

		//screen spec missing, creating default screen
		if(schema.getScreens() == null || schema.getScreens().isEmpty()) {
			UiScreenDTO screen = new UiScreenDTO();
			screen.setName(RegistrationConstants.Resident_Information);

			HashMap<String, String> label = new HashMap<>();
			getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().forEach( langCode -> {
				label.put(langCode, applicationContext
						.getBundle(ApplicationContext.applicationLanguage(), RegistrationConstants.LABELS)
						.getString("defaultHeader"));
			});
			screen.setFields(new ArrayList<>());
			schema.getSchema().forEach(field -> {
				if(field.isInputRequired())
					screen.getFields().add(field.getId());
			});
			screen.setLabel(label);
			screen.setOrder(1);
			screenDTOS = List.of(screen);
		}

		screenDTOS.forEach( dto -> {
			orderedScreens.put(dto.getOrder(), dto);
		});
	}

	private Map<String, List<UiSchemaDTO>> getFieldsBasedOnAlignmentGroup(List<String> screenFields,
																		  SchemaDto schemaDto) {
		Map<String, List<UiSchemaDTO>> groupedScreenFields = new LinkedHashMap<>();

		if(screenFields == null || screenFields.isEmpty())
			return groupedScreenFields;

		if(getRegistrationDTOFromSession().getRegistrationCategory().equals(RegistrationConstants.PACKET_TYPE_UPDATE)) {
			List<String> defaultUpdateFields = new ArrayList<>(); //TODO
			defaultUpdateFields.addAll(getRegistrationDTOFromSession().getUpdatableFields());
			screenFields = ListUtils.intersection(screenFields, defaultUpdateFields);
		}

		screenFields.forEach( fieldId -> {
			Optional<UiSchemaDTO> currentField = schemaDto.getSchema().stream().filter(field -> field.getId().equals(fieldId)).findFirst();
			if(currentField.isPresent()) {
				String alignmentGroup = currentField.get().getAlignmentGroup() == null ? fieldId+"TemplateGroup"
						: currentField.get().getAlignmentGroup();

				if(!groupedScreenFields.containsKey(alignmentGroup))
					groupedScreenFields.put(alignmentGroup, new LinkedList<UiSchemaDTO>());

				groupedScreenFields.get(alignmentGroup).add(currentField.get());
			}
		});
		return groupedScreenFields;
	}

	private GridPane getScreenGridPane(String screenName) {
		GridPane gridPane = new GridPane();
		gridPane.setId(screenName);
		RowConstraints topRowConstraints = new RowConstraints();
		topRowConstraints.setPercentHeight(5);
		RowConstraints midRowConstraints = new RowConstraints();
		midRowConstraints.setPercentHeight(90);
		RowConstraints bottomRowConstraints = new RowConstraints();
		bottomRowConstraints.setPercentHeight(5);
		gridPane.getRowConstraints().addAll(topRowConstraints,midRowConstraints, bottomRowConstraints);

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(5);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(90);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);

		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,
				columnConstraint3);

		return gridPane;
	}

	private GridPane getScreenGroupGridPane(String id, GridPane screenGridPane) {
		GridPane groupGridPane = new GridPane();
		groupGridPane.setId(id);
		groupGridPane.prefWidthProperty().bind(screenGridPane.widthProperty());
		groupGridPane.getColumnConstraints().clear();
		ColumnConstraints columnConstraint = new ColumnConstraints();
		columnConstraint.setPercentWidth(100);
		groupGridPane.getColumnConstraints().add(columnConstraint);
		groupGridPane.setHgap(20);
		groupGridPane.setVgap(20);
		return groupGridPane;
	}

	private void addNavigationButtons() {

		Label navigationLabel = new Label();
		switch (getRegistrationDTOFromSession().getRegistrationCategory()) {
			case PACKET_TYPE_NEW:
				navigationLabel.setText(applicationContext.getApplicationLanguageLabelBundle().getString("/newregistration"));
				break;
			case PACKET_TYPE_UPDATE:
				navigationLabel.setText(applicationContext.getApplicationLanguageLabelBundle().getString("/uinupdate"));
				break;
			case PACKET_TYPE_LOST:
				navigationLabel.setText(applicationContext.getApplicationLanguageLabelBundle().getString("/lostuin"));
				break;
		}
		navigationAnchorPane.getChildren().add(navigationLabel);
		AnchorPane.setTopAnchor(navigationLabel, 5.0);
		AnchorPane.setLeftAnchor(navigationLabel, 10.0);

		/*Button continueButton = new Button();
		continueButton.setId("next");
		continueButton.setText(applicationContext.getApplicationLanguageLabelBundle().getString("continue"));
		continueButton.getStyleClass().add("continueButton");*/
		next.setOnAction(getNextActionHandler());

		/*anchorPane.getChildren().add(continueButton);
		AnchorPane.setBottomAnchor(continueButton, 10.0);
		AnchorPane.setRightAnchor(continueButton, 10.0);*/

		/*Button backButton = new Button();
		backButton.setId("previous");
		backButton.setText(applicationContext.getApplicationLanguageLabelBundle().getString("back"));
		backButton.getStyleClass().add("continueButton");*/
		previous.setOnAction(getPreviousActionHandler());

		/*anchorPane.getChildren().add(backButton);
		AnchorPane.setBottomAnchor(backButton, 10.0);
		AnchorPane.setLeftAnchor(backButton, 10.0);*/
	}

	private String getScreenName(Tab tab) {
		return tab.getId().replace("_tab", EMPTY);
	}

	private boolean isScreenVisible(String screenName) {
		fieldMap.get(screenName).stream().forEach(fieldId -> {
			LOGGER.info("Refreshing Screen: {} fieldId : {}", screenName, fieldId);
			FxControl fxControl = getFxControl(fieldId);
			if(fxControl != null)
				fxControl.refresh();
		});

		boolean atLeastOneVisible = fieldMap.get(screenName)
				.stream()
				.anyMatch( fieldId -> getFxControl(fieldId) != null && getFxControl(fieldId).node.isVisible() );

		LOGGER.info("Screen refreshed, Screen: {} visible : {}", screenName, atLeastOneVisible);
		return atLeastOneVisible;
	}

	private EventHandler getNextActionHandler() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				TabPane tabPane = (TabPane) anchorPane.lookup(HASH+getRegistrationDTOFromSession().getRegistrationId());
				tabPane.getSelectionModel().selectNext();
			}
		};
	}

	private EventHandler getPreviousActionHandler() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				TabPane tabPane = (TabPane) anchorPane.lookup(HASH+getRegistrationDTOFromSession().getRegistrationId());
				tabPane.getSelectionModel().selectPrevious();
			}
		};
	}

	private void setTabSelectionChangeEventHandler(TabPane tabPane) {
		tabPane.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

				if(oldValue == null || newValue.intValue() <= oldValue.intValue())
					return;

				Optional<Tab> result = tabPane.getTabs()
						.stream()
						.filter( t -> t.getId().equals(getInvalidScreenName(tabPane)+"_tab") )
						.findFirst();

				if(result.isPresent()) {
					//refresh the selected screen and set screen visibility
					result.get().setDisable(!isScreenVisible(getScreenName(result.get())));
					tabPane.getSelectionModel().select(result.get());
				}
			}
		});
	}

	private String getInvalidScreenName(TabPane tabPane) {
		String errorScreen = EMPTY;
		for(UiScreenDTO screen : orderedScreens.values()) {
			if(!fieldMap.get(screen.getName())
					.stream()
					.allMatch(fieldId -> getFxControl(fieldId) != null && getFxControl(fieldId).canContinue() == true)) {
				errorScreen = screen.getName();
				break;
			}
		}
		return errorScreen;
	}

	private TabPane createTabPane() {
		TabPane tabPane = new TabPane();
		tabPane.setId(getRegistrationDTOFromSession().getRegistrationId());
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabPane.prefWidthProperty().bind(anchorPane.widthProperty());
		tabPane.prefHeightProperty().bind(anchorPane.heightProperty());

		setTabSelectionChangeEventHandler(tabPane);
		anchorPane.getChildren().add(tabPane);
		addNavigationButtons();
		return tabPane;
	}

	public void populateScreens() {
		LOGGER.debug("Populating Dynamic screens");

		initialize();
		TabPane tabPane = createTabPane();
		SchemaDto schema = getLatestSchema();
		getScreens(schema);

		for(UiScreenDTO screenDTO : orderedScreens.values()) {
			fieldMap.put(screenDTO.getName(), screenDTO.getFields());

			Map<String, List<UiSchemaDTO>> screenFieldGroups = getFieldsBasedOnAlignmentGroup(screenDTO.getFields(), schema);

			List<String> labels = new ArrayList<>();
			getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().forEach(langCode -> {
				labels.add(screenDTO.getLabel().get(langCode));
			});

			Tab screenTab = new Tab();
			screenTab.setId(screenDTO.getName()+"_tab");
			screenTab.setText(String.join(RegistrationConstants.SLASH, labels));

			if(screenFieldGroups == null || screenFieldGroups.isEmpty())
				screenTab.setDisable(true);

			GridPane screenGridPane = getScreenGridPane(screenDTO.getName());
			screenGridPane.prefWidthProperty().bind(tabPane.widthProperty());
			screenGridPane.prefHeightProperty().bind(tabPane.heightProperty());

			int rowIndex = 0;
			GridPane gridPane = getScreenGroupGridPane(screenGridPane.getId()+"_col_1", screenGridPane);

			if(screenDTO.isPreRegFetchRequired() && getRegistrationDTOFromSession().getRegistrationCategory().equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_NEW)) {
				gridPane.add(getPreRegistrationFetchComponent(), 0, rowIndex++);
			}

			for(Entry<String, List<UiSchemaDTO>> groupEntry : screenFieldGroups.entrySet()) {
				FlowPane groupFlowPane = new FlowPane();
				groupFlowPane.prefWidthProperty().bind(gridPane.widthProperty());
				groupFlowPane.setHgap(20);
				groupFlowPane.setVgap(20);

				for(UiSchemaDTO fieldDTO : groupEntry.getValue()) {
					try {
						FxControl fxControl = buildFxElement(fieldDTO);
						groupFlowPane.getChildren().add(fxControl.getNode());
					} catch (Exception exception){
						LOGGER.error("Failed to build control " + fieldDTO.getId(), exception);
					}
				}
				gridPane.add(groupFlowPane, 0, rowIndex++);
			}

			screenGridPane.setStyle("-fx-background-color: white;");
			screenGridPane.add(gridPane, 1, 1);
			final ScrollPane scrollPane = new ScrollPane(screenGridPane);
			scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			screenTab.setContent(scrollPane);
			tabPane.getTabs().add(screenTab);
		}

		addPreviewScreen(tabPane);
		//refresh to reflect the initial visibility configuration
		refreshFields();
	}


	private void addPreviewScreen(TabPane tabPane) {
		List<String> previewLabels = new ArrayList<>();
		List<String> authLabels = new ArrayList<>();
		for (String langCode : getRegistrationDTOFromSession().getSelectedLanguagesByApplicant()) {
			previewLabels.add(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
					.getString(RegistrationConstants.previewHeader));
			authLabels.add(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
					.getString(RegistrationConstants.authentication));
		}

		Tab previewScreen = new Tab();
		previewScreen.setId("PREVIEW");
		previewScreen.setText(String.join(RegistrationConstants.SLASH, previewLabels));
		previewScreen.setContent(registrationPreviewController.getPreviewContent());
		tabPane.getTabs().add(previewScreen);

		//click on continue when current selectedTab is preview should invoke
		//registrationController.goToAuthenticationPage();
	}

	/**
	 * 
	 * Populate the screens with fields
	 */
	/*public void populateScreens() {
		LOGGER.debug("Populating Dynamic screens");
//		flowPane.getChildren().clear();
		fxControlMap.clear();
		fieldMap.clear();

		anchorPane.prefWidthProperty().bind(scrollPane.widthProperty());
		flowPane.prefWidthProperty().bind(anchorPane.widthProperty());

		headerGridPane = registrationController.getNavigationGridPane();
		headerGridPane.getChildren().clear();
		headerGridPane.setHgap(30);

		flowPane.setVgap(10);
		flowPane.setHgap(10);

		*//*preRegGridPane.setVisible(
				getRegistrationDTOFromSession().getRegistrationCategory().equals(RegistrationConstants.PACKET_TYPE_NEW)
						? true
						: false);
		preRegGridPane.setManaged(preRegGridPane.isVisible());*//*

		// convert JSON string to Map
		try {
//			ObjectMapper mapper = new ObjectMapper();
//			Map<String, List<String>> screens = mapper.readValue(dynamicFieldsJsonString, LinkedHashMap.class);
			Map<String, List<String>> screens = new LinkedHashMap<>();
			SchemaDto schema = getLatestSchema();
			headerGridPane.getColumnConstraints().clear();

			for (int index = 0; index < uiScreens.size() + 2; index++) {
				ColumnConstraints columnConstraints = new ColumnConstraints();
				columnConstraints.setPercentWidth(90 / (uiScreens.size() + 2));
				headerGridPane.getColumnConstraints().add(columnConstraints);
			}

			ColumnConstraints columnConstraints = new ColumnConstraints();
			columnConstraints.setPercentWidth(10);
			headerGridPane.getColumnConstraints().add(columnConstraints);

			if (schema != null && uiScreens != null && !uiScreens.isEmpty()) {
				TreeMap<Integer, UiScreenDTO> orderedScreens = getScreensInOrder(uiScreens);
				orderedScreens.forEach((order, screenDTO) -> screens.put(screenDTO.getName(), screenDTO.getFields()));

				screenMap.clear();
				ObservableList<Node> flowPaneNodes = flowPane.getChildren();

				boolean isUpdateUIN = getRegistrationDTOFromSession().getRegistrationCategory()
						.equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_UPDATE);

				List<String> defaultFields = new LinkedList<String>();

				if (isUpdateUIN && !getRegistrationDTOFromSession().isBiometricMarkedForUpdate()) {
					List<UiSchemaDTO> schemaDTOs = getValidationMap().values().stream()
							.filter(schemaDto -> schemaDto.getType()
									.equalsIgnoreCase(PacketManagerConstants.BIOMETRICS_DATATYPE))
							.collect(Collectors.toList());

					for (UiSchemaDTO uiSchemaDTO : schemaDTOs) {

						List<String> configBioAttributes = requiredFieldValidator
								.isRequiredBiometricField(uiSchemaDTO.getSubType(), getRegistrationDTOFromSession());

						if (configBioAttributes != null && !configBioAttributes.isEmpty()) {
							defaultFields.add(uiSchemaDTO.getId());
						}
					}

//						screens.get(screens.size() - 1).addAll(list);

				}

				int columnCount = 0;
				for (Entry<String, List<String>> screenEntry : screens.entrySet()) {

					int count = 0;
					GridPane screenGridPane = new GridPane();
					screenGridPane.prefWidthProperty().bind(flowPane.widthProperty());

					ScreenDTO screenDTO = new ScreenDTO();
					screenDTO.setScreenNames(getScreenLabels(screenEntry.getKey()));
					screenDTO.setScreenNode(screenGridPane);
					screenDTO.setCanContinue(false);
					screenDTO.setVisible(true);

					screenMap.put(screenMap.size() + 1, screenDTO);

					screenGridPane.setVisible(false);
					screenGridPane.setManaged(false);

					screenGridPane.setHgap(15);
					screenGridPane.setVgap(15);

					screenGridPane.getStyleClass().add("-fx-background-color: blue; -fx-grid-lines-visible: true");

					flowPaneNodes.add(screenGridPane);

					List<String> fields = screenEntry.getValue();

					List<String> screenFields = new LinkedList<>();

					if (fields != null && !fields.isEmpty()) {
						Map<String, List<UiSchemaDTO>> templateGroup = getTemplateGroupMap(fields, isUpdateUIN,
								defaultFields);

						addPagination(screenDTO, columnCount++);

						for (Entry<String, List<UiSchemaDTO>> templateGroupEntry : templateGroup.entrySet()) {
							List<UiSchemaDTO> group = templateGroupEntry.getValue();
							List<FxControl> fxControlGroup = new ArrayList<>();
							for (UiSchemaDTO uiSchemaDTO : group) {
								LOGGER.info("Creating control for field : {}", uiSchemaDTO.getId());
								screenFields.add(uiSchemaDTO.getId());
								FxControl fxConrol = buildFxElement(uiSchemaDTO);
								if (fxConrol != null && fxConrol.getNode() != null) {
									fxControlMap.put(uiSchemaDTO.getId(), fxConrol);
									fxControlGroup.add(fxConrol);
								}
							}

							GridPane rowGridPane = new GridPane();

							ColumnConstraints columnConstraint1 = new ColumnConstraints();
							columnConstraint1.setPercentWidth(10);
							ColumnConstraints columnConstraint2 = new ColumnConstraints();
							columnConstraint2.setPercentWidth(80);
							ColumnConstraints columnConstraint3 = new ColumnConstraints();
							columnConstraint3.setPercentWidth(10);

							rowGridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,
									columnConstraint3);

							GridPane middleGridpane = new GridPane();
							middleGridpane.prefWidthProperty().bind(screenGridPane.widthProperty());

							FlowPane primaryLangFlowPane = new FlowPane();
							primaryLangFlowPane.setHgap(15);
//							FlowPane secondaryLangFlowPane = new FlowPane();
//							secondaryLangFlowPane.setHgap(15);

							for (FxControl fxControl : fxControlGroup) {
								if (fxControl != null && fxControl.getNode() != null) {
//									Map<String, Object> nodeMap = fxControl.getNodeMap();
//									if (nodeMap.size() > 1) {
//										secondaryLangFlowPane.getChildren().add((Node) nodeMap
//												.get(ApplicationContext.getInstance().getLocalLanguage()));
//						<!-- </content>
            </ScrollPane>-->			}
									primaryLangFlowPane.getChildren().add(fxControl.getNode());
//									fxControl.setNode(middleGridpane);
								}
							}

							middleGridpane.getColumnConstraints().clear();
							ColumnConstraints middleGridpaneColumnConstraint1 = new ColumnConstraints();
							middleGridpaneColumnConstraint1.setPercentWidth(100);
							middleGridpane.getColumnConstraints().add(middleGridpaneColumnConstraint1);
							middleGridpane.add(primaryLangFlowPane, 0, 0);

							rowGridPane.add(middleGridpane, 1, 0);

							screenGridPane.add(rowGridPane, 0, count++);
						}

						fieldMap.put(screenMap.size(), screenFields);
					}
				}
			}

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Loading Locations");
			for (Entry<String, TreeMap<Integer, FxControl>> locatioEntrySet : locationMap.entrySet()) {

				TreeMap<Integer, FxControl> treeMap = locatioEntrySet.getValue();

				Entry<Integer, FxControl> val = treeMap.firstEntry();
				try {

					Map<String, Object> data = new LinkedHashMap<>();

					String lang = getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0);

					data.put(lang, masterSyncService.findLocationByHierarchyCode(val.getKey(), lang));

					val.getValue().fillData(data);
				} catch (RegBaseCheckedException regBaseCheckedException) {

					LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							ExceptionUtils.getStackTrace(regBaseCheckedException));
				}

			}
			currentPage = 1;

			addPagination(null, 0);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields");
			refreshFields();

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Showing current Page");
			showCurrentPage();
		} catch (RegBaseCheckedException exception) {

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Failed to load dynamic fields " + ExceptionUtils.getStackTrace(exception));
		}

	}*/

	/*private Map<String, String> getScreenLabels(String key) {
		for (UiScreenDTO screen : uiScreens) {
			if (screen.getName().equalsIgnoreCase(key)) {
				return screen.getLabel();
			}
		}
		return null;
	}

	private TreeMap<Integer, UiScreenDTO> getScreensInOrder(List<UiScreenDTO> uiScreenDTO) {
		TreeMap<Integer, UiScreenDTO> orderedScreens = new TreeMap<>();
		for (UiScreenDTO screen : uiScreenDTO) {
			orderedScreens.put(Integer.valueOf(screen.getOrder()), screen);
		}
		return orderedScreens;
	}*/

	/*private void addPagination(ScreenDTO screenDTO, int columnCount) {
		if (screenDTO == null) {

			String previewText = "";
			String authText = "";
			for (String langCode : getRegistrationDTOFromSession().getSelectedLanguagesByApplicant()) {
				previewText = previewText.isEmpty() ? previewText : previewText + RegistrationConstants.SLASH;
				previewText += applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
						.getString(RegistrationConstants.previewHeader);

				authText = authText.isEmpty() ? authText : authText + RegistrationConstants.SLASH;
				authText += applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
						.getString(RegistrationConstants.authentication);

			}

			Label previewLabel = getLabel(String.valueOf(screenMap.size() + 1), previewText,
					RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, 100);
			previewLabel.getStyleClass().addAll(NON_CLICKABLE);

			Label ackLabel = getLabel(String.valueOf(screenMap.size() + 2), authText,
					RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, 150);
			ackLabel.getStyleClass().addAll(NON_CLICKABLE);

			addNavListener(previewLabel);
			addNavListener(ackLabel);
			headerGridPane.add(previewLabel, uiScreens.size(), 0);
			headerGridPane.add(ackLabel, uiScreens.size() + 1, 0);

		} else {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Adding Pagination  for screen : "
					+ screenDTO.getScreenNames().get(ApplicationContext.applicationLanguage()));

			Label label = getLabel(null, getScreenLabel(screenDTO.getScreenNames()),
					RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, 150);
			label.getStyleClass().addAll(NON_CLICKABLE);

			label.setId(String.valueOf(screenMap.size()));

			addNavListener(label);

			headerGridPane.add(label, columnCount, 0);
		}
	}*/

	/*private String getScreenLabel(Map<String, String> screenNames) {
		String labelText = "";

		for (String langCode : getRegistrationDTOFromSession().getSelectedLanguagesByApplicant()) {
			if (screenNames.containsKey(langCode)) {
				labelText = labelText.isEmpty() ? screenNames.get(langCode) : labelText.concat(RegistrationConstants.SLASH).concat(screenNames.get(langCode));
			}
		}
		return labelText;
	}*/

	/*private void addNavListener(Label label) {
		label.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Listener called for page navigation");

				int clickedScreenNumber = Integer.valueOf(label.getId());

				if (clickedScreenNumber == screenMap.size() + 1) {

					boolean isPrevScreenCnt = false;

					for (int screen = screenMap.size(); screen > 0; --screen) {

						isPrevScreenCnt = screenMap.get(screen).isCanContinue();

						if (!isPrevScreenCnt) {
							break;
						}
					}

					if (isPrevScreenCnt) {
						LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
								"Next screen not available showing previw...");
						registrationPreviewController.setUpPreviewContent();
						registrationController.showCurrentPage(RegistrationConstants.GENERIC_DETAIL,
								getPageByAction(RegistrationConstants.GENERIC_DETAIL, RegistrationConstants.NEXT));
						registrationController.showCurrentPage(RegistrationConstants.OPERATOR_AUTHENTICATION,
								getPageByAction(RegistrationConstants.OPERATOR_AUTHENTICATION,
										RegistrationConstants.PREVIOUS));
						setLabelStyles(clickedScreenNumber);
					}
				} else if (clickedScreenNumber == screenMap.size() + 2) {

					if (!registrationPreviewController.getNextButton().isDisable()) {

						registrationController.showCurrentPage(RegistrationConstants.GENERIC_DETAIL,
								getPageByAction(RegistrationConstants.GENERIC_DETAIL, RegistrationConstants.NEXT));

						registrationController.showCurrentPage(RegistrationConstants.REGISTRATION_PREVIEW,
								getPageByAction(RegistrationConstants.REGISTRATION_PREVIEW,
										RegistrationConstants.NEXT));
						registrationController.goToAuthenticationPage();
						setLabelStyles(clickedScreenNumber);
					}

				} else {
					boolean isPrevScreenCnt = false;

					clickedScreenNumber = clickedScreenNumber > 1 ? clickedScreenNumber - 1 : clickedScreenNumber;
					for (int screen = clickedScreenNumber; screen >= 1; --screen) {

						isPrevScreenCnt = screenMap.get(screen).isCanContinue();

						if (!isPrevScreenCnt) {
							break;
						}
					}

					if (isPrevScreenCnt) {

						registrationController.showCurrentPage(RegistrationConstants.OPERATOR_AUTHENTICATION,
								getPageByAction(RegistrationConstants.OPERATOR_AUTHENTICATION,
										RegistrationConstants.PREVIOUS));
						registrationController.showCurrentPage(RegistrationConstants.REGISTRATION_PREVIEW,
								getPageByAction(RegistrationConstants.REGISTRATION_PREVIEW,
										RegistrationConstants.PREVIOUS));

						setLabelStyles(Integer.valueOf(label.getId()));

						LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Navigating page");
						show(Integer.valueOf(label.getId()));
					}
				}

			}
		});

	}*/

	private FxControl buildFxElement(UiSchemaDTO uiSchemaDTO) throws Exception {
		LOGGER.info("Building fxControl for field : {}", uiSchemaDTO.getId());

		FxControl fxControl = null;
		if (uiSchemaDTO.getControlType() != null) {
			switch (uiSchemaDTO.getControlType()) {
				case CONTROLTYPE_TEXTFIELD:
					fxControl = new TextFieldFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_BIOMETRICS:
					fxControl = new BiometricFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_BUTTON:
					fxControl =  new ButtonFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_CHECKBOX:
					fxControl = new CheckBoxFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_DOB:
					fxControl =  new DOBFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_DOB_AGE:
					fxControl =  new DOBAgeFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_DOCUMENTS:
					fxControl =  new DocumentFxControl().build(uiSchemaDTO);
					break;

				case CONTROLTYPE_DROPDOWN:
					fxControl = new DropDownFxControl().build(uiSchemaDTO);
					break;
				case CONTROLTYPE_HTML:
					fxControl = new HtmlFxControl().build(uiSchemaDTO);
					break;
			}
		}

		if(fxControl == null)
			throw  new Exception("Failed to build fxControl");

		fxControlMap.put(uiSchemaDTO.getId(), fxControl);
		return fxControl;
	}

	public void refreshFields() {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields");
		for (Entry<String, List<String>> entrySet : fieldMap.entrySet()) {
			isScreenVisible(entrySet.getKey());
		}
	}


	private FxControl getFxControl(String fieldId) {
		return GenericController.getFxControlMap().get(fieldId);
	}

	/*@FXML
	public void next() {
		LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Next screen requested");
		Node currentNode = screenMap.get(currentPage).getScreenNode();

		Entry<Integer, ScreenDTO> nextScreen = screenMap.higherEntry(currentPage);

		if (nextScreen == null) {

			LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Next screen not available showing previw...");
			registrationPreviewController.setUpPreviewContent();
			registrationController.showCurrentPage(RegistrationConstants.GENERIC_DETAIL,
					getPageByAction(RegistrationConstants.GENERIC_DETAIL, RegistrationConstants.NEXT));

			setLabelStyles(screenMap.size() + 1);
		} else {

			boolean hasElementsinNext = isScreenVisible(fieldMap.get(nextScreen.getKey()));

			if (hasElementsinNext) {
				show(currentNode, nextScreen != null ? nextScreen.getValue().getScreenNode() : null, ++currentPage);

				setLabelStyles(currentPage);
			} else {

				LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Next screen is not visible,, showing next after screen");
				++currentPage;
				next();
			}

		}

	}

	private void showCurrentPage() {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Showing current page : screen No: " + currentPage);

		if (isScreenVisible(fieldMap.get(currentPage))) {
			Node node = screenMap.get(currentPage).getScreenNode();
			node.setVisible(true);
			node.setManaged(true);

			setLabelStyles(currentPage);
		} else {
			++currentPage;
			showCurrentPage();
		}

	}

	private void show(int page) {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Showing current page : screen No: " + currentPage);

		if (isScreenVisible(fieldMap.get(page))) {

			Node node = screenMap.get(page).getScreenNode();

			show(null, node, page);
		}

	}

	private boolean isScreenVisible(List<String> currentScreenFields) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Checking screen visiblity" + currentPage);
		boolean hasElement = false;
		if (currentScreenFields != null && !currentScreenFields.isEmpty()) {

			for (String field : currentScreenFields) {
				FxControl control = getFxControl(field);

				if (control != null && control.getNode().isVisible()) {
					hasElement = true;
				}

				if (hasElement) {
					break;
				}
			}
		}
		return hasElement;
	}

	private void show(Node currentNode, Node nextNode, int updateScreen) {

		if (nextNode != null) {
			nextNode.setVisible(true);
			nextNode.setManaged(true);
			currentPage = updateScreen;

			boolean isVisible = currentPage > 1 ? true : false;
			previous.setVisible(isVisible);

			refreshContinueButton();

			for (Entry<Integer, ScreenDTO> entry : screenMap.entrySet()) {

				if (entry.getKey() != currentPage) {

					Node node = entry.getValue().getScreenNode();
					node.setVisible(false);
					node.setManaged(false);
				}
			}
		}

	}

	@FXML
	public void previous() {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Showing previous page : screen No: " + currentPage);
		Node currentNode = screenMap.get(currentPage).getScreenNode();

		Entry<Integer, ScreenDTO> nextScreen = screenMap.lowerEntry(currentPage);
		boolean hasElementsinNext = isScreenVisible(fieldMap.get(nextScreen.getKey()));

		if (hasElementsinNext) {
			show(currentNode, nextScreen != null ? nextScreen.getValue().getScreenNode() : null, currentPage - 1);

			setLabelStyles(currentPage);
		} else {

			--currentPage;
			previous();
		}

	}

	private FxControl getFxControl(String fieldId) {

		return GenericController.getFxControlMap().get(fieldId);
	}

	public void refreshFields() {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields");

		for (Entry<Integer, List<String>> entrySet : fieldMap.entrySet()) {

			for (String field : entrySet.getValue()) {

				FxControl control = getFxControl(field);

				if (control != null) {
					control.refresh();
				}
			}

			ScreenDTO screenDTO = screenMap.get(entrySet.getKey());
			screenDTO.setVisible(isScreenVisible(entrySet.getValue()));

			if (!screenDTO.isVisible()) {
				screenDTO.setCanContinue(true);
			}

			refreshContinue(entrySet.getKey());

			Label label = (Label) headerGridPane.lookup(RegistrationConstants.HASH + entrySet.getKey());

			if (label != null) {
				label.setVisible(screenDTO.isVisible());
				label.setManaged(screenDTO.isVisible());
			}

		}

		refreshContinueButton();

	}

	public void refreshContinueButton() {

		refreshContinue(currentPage);
		next.setDisable(!screenMap.get(currentPage).isCanContinue());
	}

	private void refreshContinue(int page) {
		boolean canContinue = true;
		List<String> currentScreenFields = fieldMap.get(page);

		if (currentScreenFields != null && !currentScreenFields.isEmpty()) {

			for (String field : currentScreenFields) {
				FxControl control = getFxControl(field);

				if (control != null) {
					canContinue = control.canContinue();
				}

				if (!canContinue) {
					break;
				}
			}
		}

		ScreenDTO screenDTO = screenMap.get(page);

		screenDTO.setCanContinue(canContinue);

//		Label hBox = (Label) headerHBox.lookup((RegistrationConstants.HASH + page));
//
//		if (hBox != null && currentPage != page) {
//			hBox.getStyleClass().add(canContinue ? "" : "paginationLabelGreyedOut");			
//		}

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing continue button");
	}

	protected Label getLabel(String id, String titleText, String styleClass, boolean isVisible, double prefWidth) {
		*//** Field Title *//*
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		label.setMinWidth(Region.USE_COMPUTED_SIZE);
		GridPane.setHalignment(label, HPos.CENTER);
		GridPane.setValignment(label, VPos.BOTTOM);
		label.setTooltip(new Tooltip(titleText));
		return label;
	}

	private void setLabelStyles(int page) {
		for (int index = 1; index <= (screenMap.size() + 2); ++index) {
			Label label = (Label) headerGridPane.lookup((RegistrationConstants.HASH + index));
			if (index == page) {
				label.getStyleClass().add(CLICKABLE);
			} else {
				label.getStyleClass().remove(CLICKABLE);
			}
		}
	}*/

	/*public void fetchPreRegistration() {

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

		ResponseDTO responseDTO = preRegistrationDataSyncService.getPreRegistration(preRegId,false);

		SuccessResponseDTO successResponseDTO = responseDTO.getSuccessResponseDTO();
		List<ErrorResponseDTO> errorResponseDTOList = responseDTO.getErrorResponseDTOs();

		if (successResponseDTO != null && successResponseDTO.getOtherAttributes() != null
				&& successResponseDTO.getOtherAttributes().containsKey(RegistrationConstants.REGISTRATION_DTO)) {
//			SessionContext.map().put(RegistrationConstants.REGISTRATION_DATA,
//					successResponseDTO.getOtherAttributes().get(RegistrationConstants.REGISTRATION_DTO));

			getRegistrationDTOFromSession().setPreRegistrationId(preRegId);

			RegistrationDTO preRegistrationDTO = (RegistrationDTO) successResponseDTO.getOtherAttributes()
					.get(RegistrationConstants.REGISTRATION_DTO);

			for (Entry<Integer, List<String>> entry : fieldMap.entrySet()) {

				for (String field : entry.getValue()) {

					FxControl fxControl = getFxControl(field);

					if (fxControl != null) {

						if (preRegistrationDTO.getDemographics().containsKey(field)) {

							fxControl.selectAndSet(preRegistrationDTO.getDemographics().get(field));
						}

						else if (preRegistrationDTO.getDocuments().containsKey(field)) {
							fxControl.selectAndSet(preRegistrationDTO.getDocuments().get(field));

						}
					}
				}
			}

		} else if (errorResponseDTOList != null && !errorResponseDTOList.isEmpty()) {
			generateAlertLanguageSpecific(RegistrationConstants.ERROR, errorResponseDTOList.get(0).getMessage());
		}
	}*/

	/*private Map<String, List<UiSchemaDTO>> getTemplateGroupMap(List<String> fields, boolean isUpdateUIN,
			List<String> defaultFields) {
		Map<String, List<UiSchemaDTO>> templateGroupMap = new LinkedHashMap<>();
		for (String field : fields) {
			if (!field.isEmpty()
					&& (isUpdateUIN
							? (getRegistrationDTOFromSession().getUpdatableFields().contains(field)
									|| defaultFields.contains(field))
							: true)
					&& getFxControl(field) == null) {
				UiSchemaDTO schemaDto = getValidationMap().get(field);
				if (schemaDto != null) {
					List<UiSchemaDTO> list = templateGroupMap.get(schemaDto.getAlignmentGroup());
					if (list == null) {
						list = new LinkedList<UiSchemaDTO>();
					}
					list.add(schemaDto);
					templateGroupMap.put(schemaDto.getAlignmentGroup() == null ? field + "TemplateGroup"
							: schemaDto.getAlignmentGroup(), list);
				}
			}
		}
		return templateGroupMap;
	}*/
}
