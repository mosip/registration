package io.mosip.registration.controller;

import static io.mosip.registration.constants.RegistrationConstants.EMPTY;
import static io.mosip.registration.constants.RegistrationConstants.HASH;
import static io.mosip.registration.constants.RegistrationConstants.PACKET_TYPE_LOST;
import static io.mosip.registration.constants.RegistrationConstants.PACKET_TYPE_NEW;
import static io.mosip.registration.constants.RegistrationConstants.PACKET_TYPE_UPDATE;
import static io.mosip.registration.constants.RegistrationConstants.REG_AUTH_PAGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.ProcessNames;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.auth.AuthenticationController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.RegistrationPreviewController;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.dto.response.UiScreenDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.BiometricFxControl;
import io.mosip.registration.util.control.impl.ButtonFxControl;
import io.mosip.registration.util.control.impl.CheckBoxFxControl;
import io.mosip.registration.util.control.impl.DOBAgeFxControl;
import io.mosip.registration.util.control.impl.DOBFxControl;
import io.mosip.registration.util.control.impl.DocumentFxControl;
import io.mosip.registration.util.control.impl.DropDownFxControl;
import io.mosip.registration.util.control.impl.HtmlFxControl;
import io.mosip.registration.util.control.impl.TextFieldFxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.SneakyThrows;

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

	private static final String CONTROLTYPE_TEXTFIELD = "textbox";
	private static final String CONTROLTYPE_BIOMETRICS = "biometrics";
	private static final String CONTROLTYPE_DOCUMENTS = "fileupload";
	private static final String CONTROLTYPE_DROPDOWN = "dropdown";
	private static final String CONTROLTYPE_CHECKBOX = "checkbox";
	private static final String CONTROLTYPE_BUTTON = "button";
	private static final String CONTROLTYPE_DOB = "date";
	private static final String CONTROLTYPE_DOB_AGE = "ageDate";
	private static final String CONTROLTYPE_HTML = "html";

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
	private Button authenticate;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private AuthenticationController authenticationController;

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
	
	private Stage keyboardStage;

	public static Map<String, TreeMap<Integer, String>> hierarchyLevels = new HashMap<String, TreeMap<Integer, String>>();
	public static Map<String, TreeMap<Integer, String>> currentHierarchyMap = new HashMap<String, TreeMap<Integer, String>>();

	public static Map<String, FxControl> getFxControlMap() {
		return fxControlMap;
	}

	private void initialize() {
		orderedScreens.clear();
		fxControlMap.clear();
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
		auditFactory.audit(AuditEvent.REG_DEMO_PRE_REG_DATA_FETCH, Components.REG_DEMO_DETAILS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		
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

		for (UiScreenDTO screenDTO : orderedScreens.values()) {
			for (String field : screenDTO.getFields()) {

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

		next.setOnAction(getNextActionHandler());
		authenticate.setOnAction(getRegistrationAuthActionHandler());
	}

	private String getScreenName(Tab tab) {
		return tab.getId().replace("_tab", EMPTY);
	}

	private boolean refreshScreenVisibility(String screenName) {
		boolean atLeastOneVisible = true;
		Optional<UiScreenDTO> screenDTO = orderedScreens.values()
				.stream()
				.filter(screen -> screen.getName().equals(screenName))
				.findFirst();

		if(screenDTO.isPresent()) {
			LOGGER.info("Refreshing Screen: {}", screenName);
			screenDTO.get().getFields().forEach( fieldId -> {
				FxControl fxControl = getFxControl(fieldId);
				if(fxControl != null)
					fxControl.refresh();
			});

			atLeastOneVisible = screenDTO.get()
					.getFields()
					.stream()
					.anyMatch( fieldId -> getFxControl(fieldId) != null && getFxControl(fieldId).getNode().isVisible() );
		}
		LOGGER.info("Screen refreshed, Screen: {} visible : {}", screenName, atLeastOneVisible);
		return atLeastOneVisible;
	}

	private EventHandler getNextActionHandler() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				TabPane tabPane = (TabPane) anchorPane.lookup(HASH+getRegistrationDTOFromSession().getRegistrationId());
				tabPane.getTabs().size();
				int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
				while(selectedIndex < tabPane.getTabs().size()) {
					selectedIndex++;
					if(!tabPane.getTabs().get(selectedIndex).isDisabled()) {
						tabPane.getSelectionModel().select(selectedIndex);
						break;
					}
				}
			}
		};
	}

	private EventHandler getRegistrationAuthActionHandler() {
		return new EventHandler<ActionEvent>() {
			@SneakyThrows
			@Override
			public void handle(ActionEvent event) {
				TabPane tabPane = (TabPane) anchorPane.lookup(HASH+getRegistrationDTOFromSession().getRegistrationId());
				String incompleteScreen = getInvalidScreenName(tabPane);

				if(incompleteScreen == null) {
					generateAlert(RegistrationConstants.ERROR, incompleteScreen +" Screen with ERROR !");
					return;
				}
				authenticationController.goToNextPage();
			}
		};
	}

	private void setTabSelectionChangeEventHandler(TabPane tabPane) {
		tabPane.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				LOGGER.debug("Old selection : {} New Selection : {}", oldValue, newValue);

				if(newValue.intValue() < 0) { return; }

				final String newScreenName = tabPane.getTabs().get(newValue.intValue()).getId().replace("_tab", EMPTY);

				//Hide continue button in preview page
				next.setVisible(newScreenName.equals("AUTH") ? false : true);
				authenticate.setVisible(newScreenName.equals("AUTH") ? true : false);

				if(newScreenName.equals("AUTH") || newScreenName.equals("PREVIEW")) {
					if(getInvalidScreenName(tabPane).equals(EMPTY)) {
						loadPreviewOrAuthScreen(tabPane, tabPane.getTabs().get(newValue.intValue()));
						return;
					}
					//Not eligible to preview / auth
					tabPane.getSelectionModel().selectPrevious();
					return;
				}

				if(oldValue == null || oldValue.intValue() < 0)
					return;

				//Refresh screen visibility
				tabPane.getTabs().get(newValue.intValue()).setDisable(!refreshScreenVisibility(newScreenName));
				boolean isSelectedDisabledTab = tabPane.getTabs().get(newValue.intValue()).isDisabled();
				if(newValue.intValue() <= oldValue.intValue() && !isSelectedDisabledTab)
					return;

				if(!isScreenValid(tabPane.getTabs().get(oldValue.intValue()).getId())) {
					LOGGER.error("Current screen is not fully valid : {}", oldValue.intValue());
					tabPane.getSelectionModel().selectPrevious();
					return;
				}

				if(isSelectedDisabledTab) {
					LOGGER.error("Current selected new screen is disabled finding new screen");
					tabPane.getSelectionModel().selectPrevious();
				}
			}
		});
	}

	private boolean isScreenValid(final String screenName) {
		Optional<UiScreenDTO> result = orderedScreens.values()
				.stream().filter(screen -> screen.getName().equals(screenName.replace("_tab", EMPTY))).findFirst();

		boolean isValid = true;
		if(result.isPresent()) {
			for(String fieldId : result.get().getFields()) {
				if(getFxControl(fieldId) != null && !getFxControl(fieldId).canContinue()) {
					LOGGER.error("Screen validation , fieldId : {} has invalid value", fieldId);
					isValid = false;
					break;
				}
			}
		}
		if (isValid) {
			auditFactory.audit(AuditEvent.REG_NAVIGATION, Components.REGISTRATION_CONTROLLER,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		}
		return isValid;
	}

	private String getInvalidScreenName(TabPane tabPane) {
		String errorScreen = EMPTY;
		for(UiScreenDTO screen : orderedScreens.values()) {
			LOGGER.error("Started to validate screen : {} ", screen.getName());
			boolean anyInvalidField = screen.getFields()
					.stream()
					.anyMatch( fieldId -> getFxControl(fieldId) != null &&
							getFxControl(fieldId).canContinue() == false );

			if(anyInvalidField) {
				LOGGER.error("Screen validation failed {}", screen.getName());
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

	public void populateScreens() throws Exception {
		LOGGER.debug("Populating Dynamic screens");

		initialize();
		TabPane tabPane = createTabPane();
		SchemaDto schema = getLatestSchema();
		getScreens(schema);

		for(UiScreenDTO screenDTO : orderedScreens.values()) {
			Map<String, List<UiSchemaDTO>> screenFieldGroups = getFieldsBasedOnAlignmentGroup(screenDTO.getFields(), schema);

			List<String> labels = new ArrayList<>();
			getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().forEach(langCode -> {
				labels.add(screenDTO.getLabel().get(langCode));
			});

			Tab screenTab = new Tab();
			screenTab.setId(screenDTO.getName()+"_tab");
			screenTab.setText(labels.get(0));
			screenTab.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, labels)));

			if(screenFieldGroups == null || screenFieldGroups.isEmpty())
				screenTab.setDisable(true);

			GridPane screenGridPane = getScreenGridPane(screenDTO.getName());
			screenGridPane.prefWidthProperty().bind(tabPane.widthProperty());
			screenGridPane.prefHeightProperty().bind(tabPane.heightProperty());

			int rowIndex = 0;
			GridPane gridPane = getScreenGroupGridPane(screenGridPane.getId()+"_col_1", screenGridPane);

			if(screenDTO.isPreRegFetchRequired() && getRegistrationDTOFromSession().getRegistrationCategory().equals(PACKET_TYPE_NEW)) {
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
						if(fxControl.getNode() instanceof GridPane) {
							((GridPane)fxControl.getNode()).prefWidthProperty().bind(groupFlowPane.widthProperty());
						}
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

		//refresh to reflect the initial visibility configuration
		refreshFields();
		addPreviewAndAuthScreen(tabPane);
	}


	private void addPreviewAndAuthScreen(TabPane tabPane) throws Exception {
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
		previewScreen.setText(previewLabels.get(0));
		previewScreen.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, previewLabels)));
		tabPane.getTabs().add(previewScreen);

		Tab authScreen = new Tab();
		authScreen.setId("AUTH");
		authScreen.setText(authLabels.get(0));
		authScreen.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, authLabels)));
		tabPane.getTabs().add(authScreen);
	}

	private void loadPreviewOrAuthScreen(TabPane tabPane, Tab tab) {
		switch (tab.getId()) {
			case "PREVIEW":
				try {
					tab.setContent(getPreviewContent(tabPane));
				} catch (Exception exception) {
					LOGGER.error("Failed to load preview page!!, clearing registration data.");
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_PREVIEW_PAGE);
				}
				break;

			case "AUTH":
				try {
					tab.setContent(loadAuthenticationPage(tabPane));
					authenticationController.initData(ProcessNames.PACKET.getType());
				} catch (Exception exception) {
					LOGGER.error("Failed to load auth page!!, clearing registration data.");
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE);
				}
				break;
		}
	}

	private Node getPreviewContent(TabPane tabPane) throws Exception {
		String content = registrationPreviewController.getPreviewContent();
		if(content != null) {
			final WebView webView = new WebView();
			webView.prefWidthProperty().bind(tabPane.widthProperty());
			webView.prefHeightProperty().bind(tabPane.heightProperty());
			webView.getEngine().loadContent(content);
			final GridPane gridPane = new GridPane();
			gridPane.prefWidthProperty().bind(tabPane.widthProperty());
			gridPane.prefHeightProperty().bind(tabPane.heightProperty());
			gridPane.setAlignment(Pos.TOP_LEFT);
			gridPane.getChildren().add(webView);
			return gridPane;
		}
		throw new RegBaseCheckedException("", "Failed to load preview screen");
	}

	private Node loadAuthenticationPage(TabPane tabPane) throws Exception {
		GridPane gridPane = (GridPane)BaseController.load(getClass().getResource(REG_AUTH_PAGE));
		gridPane.prefWidthProperty().bind(tabPane.widthProperty());
		gridPane.prefHeightProperty().bind(tabPane.heightProperty());

		Node node = gridPane.lookup("#backButton");
		if(node != null) {
			node.setVisible(false);
			node.setDisable(true);
		}

		node = gridPane.lookup("#operatorAuthContinue");
		if(node != null) {
			node.setVisible(false);
			node.setDisable(true);
		}
		return gridPane;
	}


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
		orderedScreens.values().forEach(screen -> { refreshScreenVisibility(screen.getName()); });
	}

	private FxControl getFxControl(String fieldId) {
		return GenericController.getFxControlMap().get(fieldId);
	}
	
	public Stage getKeyboardStage() {		
		return keyboardStage;
	}
	
	public void setKeyboardStage(Stage keyboardStage) {
		this.keyboardStage = keyboardStage;
	}
}