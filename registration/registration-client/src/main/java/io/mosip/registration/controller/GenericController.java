package io.mosip.registration.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

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
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

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
	private GridPane headerGridPane;

	@FXML
	private GridPane footerGridPane;

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
	public static Map<String, TreeMap<Integer, FxControl>> locationMap = new LinkedHashMap<String, TreeMap<Integer, FxControl>>();
	
	private static Map<String, FxControl> fxControlMap = new HashMap<String, FxControl>();

	@Autowired
	private RegistrationPreviewController registrationPreviewController;

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

	/**
	 * 
	 * Populate the screens with fields
	 * 
	 * @param screens screenName, and list of fields has to be in the screen
	 */
	public void populateScreens(Map<String, List<String>> screens) {

		flowPane.setVgap(10);
		flowPane.setHgap(10);

		screenMap.clear();
		flowPane.prefWidthProperty().bind(layOutGridPane.widthProperty());
		
		ObservableList<Node> flowPaneNodes = flowPane.getChildren();
		if (screens != null && !screens.isEmpty()) {
			for (Entry<String, List<String>> screenEntry : screens.entrySet()) {
				int count = 0;
				GridPane screenGridPane = new GridPane();

				ScreenDTO screenDTO = new ScreenDTO();
				screenDTO.setScreenName(screenEntry.getKey());
				screenDTO.setScreenNode(screenGridPane);
				screenMap.put(screenMap.size() + 1, screenDTO);

				screenGridPane.setVisible(false);
				screenGridPane.setManaged(false);

				screenGridPane.setHgap(15);
				screenGridPane.setVgap(15);

				screenGridPane.getStyleClass().add("-fx-background-color: blue; -fx-grid-lines-visible: true");

				flowPaneNodes.add(screenGridPane);
				String screenName = screenEntry.getKey();

				List<String> fields = screenEntry.getValue();

				if (fields != null && !fields.isEmpty()) {

					for (String field : fields) {

						if (!field.isEmpty()) {

							UiSchemaDTO uiSchemaDTO = getValidationMap().get(field);

							FxControl fxConrol = null;
							if (uiSchemaDTO != null) {
								fxConrol = (FxControl) buildFxElement(uiSchemaDTO);
							}

							if (fxConrol != null && fxConrol.getNode() != null) {
								Node node = fxConrol.getNode();

//								GridPane groupGridPane = (GridPane) screenGridPane.lookup(RegistrationConstants.HASH+uiSchemaDTO.getGroup());
//								
//								if(groupGridPane!=null) {
//									
//								}

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
								gridPane.setAlignment(Pos.CENTER);

								screenGridPane.add(gridPane, 0, count++);
							}
						}
					}
				}

			}

			currentPage = 1;
			Node node = screenMap.get(currentPage).getScreenNode();
			node.setVisible(true);
			node.setManaged(true);

			for (Entry<String, TreeMap<Integer, FxControl>> locatioEntrySet : locationMap.entrySet()) {

				TreeMap<Integer, FxControl> treeMap = locatioEntrySet.getValue();

				Entry<Integer, FxControl> val = treeMap.firstEntry();
				try {

					Map<String, Object> data = new LinkedHashMap<>();

					data.put(RegistrationConstants.PRIMARY, masterSyncService.findLocationByHierarchyCode(val.getKey(),
							ApplicationContext.applicationLanguage()));
					data.put(RegistrationConstants.SECONDARY, masterSyncService
							.findLocationByHierarchyCode(val.getKey(), ApplicationContext.localLanguage()));

					val.getValue().fillData(data);
				} catch (RegBaseCheckedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	private Node buildFxElement(UiSchemaDTO uiSchemaDTO) {

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
			} catch (RegBaseCheckedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				buttonFxControl.fillData(buttonsData);
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
				} catch (RegBaseCheckedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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

//		currentPage = currentPage == 0 ? ++currentPage : currentPage;
		Node currentNode = screenMap.get(currentPage).getScreenNode();

		Entry<Integer, ScreenDTO> nextScreen = screenMap.higherEntry(currentPage);

		if (nextScreen == null) {

			registrationPreviewController.setUpPreviewContent();
			registrationController.showCurrentPage(RegistrationConstants.GENERIC_DETAIL,
					getPageByAction(RegistrationConstants.GENERIC_DETAIL, RegistrationConstants.NEXT));
		} else {
			show(currentNode, nextScreen != null ? nextScreen.getValue().getScreenNode() : null, 1);
		}

	}

	private void show(Node currentNode, Node nextNode, int updateScreen) {

		if (nextNode != null) {
			currentNode.setVisible(false);
			currentNode.setManaged(false);
			nextNode.setVisible(true);
			nextNode.setManaged(true);
			currentPage += updateScreen;

			boolean isVisible = currentPage > 1 ? true : false;
			previous.setVisible(isVisible);

		}
	}

	@FXML
	public void previous() {

		Node currentNode = screenMap.get(currentPage).getScreenNode();

		Entry<Integer, ScreenDTO> nextScreen = screenMap.lowerEntry(currentPage);

		show(currentNode, nextScreen != null ? nextScreen.getValue().getScreenNode() : null, -1);

	}

}
