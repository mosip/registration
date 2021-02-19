package io.mosip.registration.controller;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.RegistrationPreviewController;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ScreenDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.entity.Location;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.BiometricFxControl;
import io.mosip.registration.util.control.impl.ButtonFxControl;
import io.mosip.registration.util.control.impl.CheckBoxFxControl;
import io.mosip.registration.util.control.impl.DOBAgeFxControl;
import io.mosip.registration.util.control.impl.DOBFxControl;
import io.mosip.registration.util.control.impl.DocumentFxControl;
import io.mosip.registration.util.control.impl.DropDownFxControl;
import io.mosip.registration.util.control.impl.TextFieldFxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

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
	/**
	 * Top most Grid pane in FXML
	 */
	@FXML
	private GridPane genericScreen;

	/**
	 * All Elements created and placed in flowPane
	 */
	@FXML
	private FlowPane flowPane;

	@FXML
	private GridPane layOutGridPane;

	@FXML
	private GridPane footerGridPane;

	@FXML
	private Button next;
	@FXML
	private Button previous;

	@FXML
	private HBox headerHBox;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private MasterSyncDao masterSyncDao;

	@Value("${mosip.registration.dynamic.json}")
	private String dynamicFieldsJsonString;

	@Autowired
	private MasterSyncService masterSyncService;
	public static Map<String, TreeMap<Integer, FxControl>> locationMap = new LinkedHashMap<String, TreeMap<Integer, FxControl>>();

	private static Map<String, FxControl> fxControlMap = new HashMap<String, FxControl>();

	private static Map<Integer, List<String>> fieldMap = new HashMap<Integer, List<String>>();

	@Autowired
	private RegistrationPreviewController registrationPreviewController;

	@Autowired
	private RequiredFieldValidator requiredFieldValidator;

	public static Map<String, FxControl> getFxControlMap() {
		return fxControlMap;
	}

	private int currentPage;

	/**
	 * Key- Integer : is a screen order Value - Node : screenNode {@summary all
	 * screen fields will be inside the screen node}
	 */
	private TreeMap<Integer, ScreenDTO> screenMap = new TreeMap<>();

	private static final String CONTROLTYPE_TEXTFIELD = "textbox";
	private static final String CONTROLTYPE_BIOMETRICS = "biometrics";

	private static final String CONTROLTYPE_DOCUMENTS = "fileupload";
	private static final String CONTROLTYPE_DROPDOWN = "dropdown";
	private static final String CONTROLTYPE_CHECKBOX = "checkbox";
	private static final String CONTROLTYPE_BUTTON = "button";
	private static final String CONTROLTYPE_DOB = "date";
	private static final String CONTROLTYPE_DOB_AGE = "ageDate";

	private static String CLICKABLE = "paginationLabelFilled";

	private static String NON_CLICKABLE = "paginationLabel";

	/**
	 * 
	 * Populate the screens with fields
	 * 
	 * @param screens screenName, and list of fields has to be in the screen
	 */
	public void populateScreens() {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Populating Dynamic screens");
		flowPane.getChildren().clear();

		headerHBox = registrationController.getNavigationHBox();

		headerHBox.setAlignment(Pos.BOTTOM_LEFT);
		headerHBox.getChildren().clear();
		headerHBox.setSpacing(45);

		flowPane.setVgap(10);
		flowPane.setHgap(10);

		flowPane.prefWidthProperty().bind(layOutGridPane.widthProperty());

		// convert JSON string to Map
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, List<String>> screens = mapper.readValue(dynamicFieldsJsonString, LinkedHashMap.class);

			screenMap.clear();
			ObservableList<Node> flowPaneNodes = flowPane.getChildren();

			boolean isUpdateUIN = getRegistrationDTOFromSession().getRegistrationCategory()
					.equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_UPDATE);

			List<String> defaultFields = new LinkedList<String>();
			if (screens != null && !screens.isEmpty()) {

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

//					screens.get(screens.size() - 1).addAll(list);

				}
				for (Entry<String, List<String>> screenEntry : screens.entrySet()) {

					int count = 0;
					GridPane screenGridPane = new GridPane();

					ScreenDTO screenDTO = new ScreenDTO();
					screenDTO.setScreenName(screenEntry.getKey());
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

						addPagination(screenDTO);

						for (String field : fields) {

							if (!field.isEmpty()
									&& (isUpdateUIN
											? (getRegistrationDTOFromSession().getUpdatableFields().contains(field)
													|| defaultFields.contains(field))
											: true)) {

								LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
										"Creating control for field : " + field);
								screenFields.add(field);
								UiSchemaDTO uiSchemaDTO = getValidationMap().get(field);

								FxControl fxConrol = null;
								if (uiSchemaDTO != null) {
									fxConrol = (FxControl) buildFxElement(uiSchemaDTO);
								}

								if (fxConrol != null && fxConrol.getNode() != null) {
									Node node = fxConrol.getNode();

//									GridPane groupGridPane = (GridPane) screenGridPane.lookup(RegistrationConstants.HASH+uiSchemaDTO.getGroup());
//									
//									if(groupGridPane!=null) {
//										
//									}

									fxControlMap.put(field, fxConrol);
									GridPane gridPane = new GridPane();

									ColumnConstraints columnConstraint1 = new ColumnConstraints();
									columnConstraint1.setPercentWidth(15);
									ColumnConstraints columnConstraint2 = new ColumnConstraints();
									columnConstraint2.setPercentWidth(70);
									ColumnConstraints columnConstraint3 = new ColumnConstraints();
									columnConstraint3.setPercentWidth(15);
									gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,
											columnConstraint3);
									gridPane.add(node, 1, 2);

									screenGridPane.add(gridPane, 0, count++);
								}
							}
						}
					}
					fieldMap.put(screenMap.size(), screenFields);

				}

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Loading Locations");
				for (Entry<String, TreeMap<Integer, FxControl>> locatioEntrySet : locationMap.entrySet()) {

					TreeMap<Integer, FxControl> treeMap = locatioEntrySet.getValue();

					Entry<Integer, FxControl> val = treeMap.firstEntry();
					try {

						Map<String, Object> data = new LinkedHashMap<>();

						data.put(RegistrationConstants.PRIMARY, masterSyncService
								.findLocationByHierarchyCode(val.getKey(), ApplicationContext.applicationLanguage()));
						data.put(RegistrationConstants.SECONDARY, masterSyncService
								.findLocationByHierarchyCode(val.getKey(), ApplicationContext.localLanguage()));

						val.getValue().fillData(data);
					} catch (RegBaseCheckedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

			currentPage = 1;

			addPagination(null);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields");
			refreshFields();

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Showing current Page");
			showCurrentPage();
		} catch (JsonProcessingException | RegBaseCheckedException exception) {

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Failed to load dynamic fields " + ExceptionUtils.getStackTrace(exception));
		}

	}

	private void addPagination(ScreenDTO screenDTO) {

		if (screenDTO == null) {
			Label previewLabel = getLabel(String.valueOf(screenMap.size() + 1),
					ApplicationContext.getInstance().getApplicationLanguageBundle().getString("registrationpreview"), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
					100);
			previewLabel.getStyleClass().addAll(NON_CLICKABLE);

			Label ackLabel = getLabel(String.valueOf(screenMap.size() + 2),
					ApplicationContext.getInstance().getApplicationLanguageBundle().getString("authentication"), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
					100);
			ackLabel.getStyleClass().addAll(NON_CLICKABLE);

			addNavListener(previewLabel);
			addNavListener(ackLabel);
			headerHBox.getChildren().add(previewLabel);
			headerHBox.getChildren().add(ackLabel);

		} else {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Adding Pagination  for screen : " + screenDTO.getScreenName());

			Label label = getLabel(null, screenDTO.getScreenName(), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
					50);
			label.getStyleClass().addAll(NON_CLICKABLE);

			label.setId(String.valueOf(screenMap.size()));

			addNavListener(label);

			headerHBox.getChildren().add(label);
		}

	}

	private void addNavListener(Label label) {
		label.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Listener called for page navigation");

				int clickedScreenNumber = Integer.valueOf(label.getId());

				if (clickedScreenNumber == screenMap.size() + 1) {

					boolean isPrevScreenCnt = false;

					for (int screen = screenMap.size() - 1; screen > 0; --screen) {

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

	}

	private Node buildFxElement(UiSchemaDTO uiSchemaDTO) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Build fx element called for field : " + uiSchemaDTO.getId());
		switch (uiSchemaDTO.getControlType()) {

		case CONTROLTYPE_TEXTFIELD:
			return new TextFieldFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_BIOMETRICS:

			return new BiometricFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_BUTTON:
			FxControl buttonFxControl = new ButtonFxControl().build(uiSchemaDTO);
			Map<String, Object> buttonsData = new LinkedHashMap<>();

			try {
				buttonsData.put(RegistrationConstants.PRIMARY, masterSyncService.getFieldValues(uiSchemaDTO.getId(),
						ApplicationContext.applicationLanguage()));
				buttonsData.put(RegistrationConstants.SECONDARY,
						masterSyncService.getFieldValues(uiSchemaDTO.getId(), ApplicationContext.localLanguage()));

				buttonFxControl.fillData(buttonsData);
			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Exception occured while fetching button values : " + uiSchemaDTO.getId() + " "
								+ ExceptionUtils.getStackTrace(regBaseCheckedException));
			}
			return buttonFxControl;
		case CONTROLTYPE_CHECKBOX:
			return new CheckBoxFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_DOB:
			return new DOBFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_DOB_AGE:
			return new DOBAgeFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_DOCUMENTS:
			return new DocumentFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_DROPDOWN:
			FxControl fxControl = new DropDownFxControl().build(uiSchemaDTO);
			if (uiSchemaDTO.getGroup().contains(RegistrationConstants.LOCATION)) {
				List<Location> value = masterSyncDao.getLocationDetails(uiSchemaDTO.getSubType(),
						applicationContext.getApplicationLanguage());

				TreeMap<Integer, FxControl> hirearcyMap = locationMap.get(uiSchemaDTO.getGroup()) != null
						? locationMap.get(uiSchemaDTO.getGroup())
						: new TreeMap<Integer, FxControl>();

				hirearcyMap.put(value.get(0).getHierarchyLevel(), fxControl);

				locationMap.put(uiSchemaDTO.getGroup(), hirearcyMap);

			} else {
				Map<String, Object> data = new LinkedHashMap<>();
				try {
					data.put(RegistrationConstants.PRIMARY, masterSyncService.getFieldValues(uiSchemaDTO.getId(),
							ApplicationContext.applicationLanguage()));
					data.put(RegistrationConstants.SECONDARY,
							masterSyncService.getFieldValues(uiSchemaDTO.getId(), ApplicationContext.localLanguage()));
				} catch (RegBaseCheckedException regBaseCheckedException) {
					LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							"Exception occured while fetching dropdown values : " + uiSchemaDTO.getId() + " "
									+ ExceptionUtils.getStackTrace(regBaseCheckedException));
				} finally {
					fxControl.fillData(data);
				}
			}

			return fxControl;

		}

		return null;
	}

	@FXML
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

			Label hBox = (Label) headerHBox.lookup(RegistrationConstants.HASH + entrySet.getKey());

			if (hBox != null) {
				hBox.setVisible(screenDTO.isVisible());

				hBox.setManaged(screenDTO.isVisible());
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
		/** Field Title */
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		//label.setPrefWidth(prefWidth);
		label.setMinWidth(Region.USE_PREF_SIZE);
		//label.setWrapText(true);
		return label;
	}
	
	private void setLabelStyles(int page) {
		for (int index = 1; index <= (screenMap.size() + 2); ++index) {
			Label label = (Label) headerHBox.lookup((RegistrationConstants.HASH + index));
			if (index == page) {
				label.getStyleClass().add(CLICKABLE);
			} else {
				label.getStyleClass().remove(CLICKABLE);
			}
		}
	}
}
