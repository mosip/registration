package io.mosip.registration.controller;

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
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.entity.Location;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.BiometricFxControl;
import io.mosip.registration.util.control.impl.DOBAgeFxControl;
import io.mosip.registration.util.control.impl.DOBFxControl;
import io.mosip.registration.util.control.impl.DocumentFxControl;
import io.mosip.registration.util.control.impl.DropDownFxControl;
import io.mosip.registration.util.control.impl.TextFieldFxControl;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
	private GridPane parentGridPane;

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

	@Autowired
	private Validations validation;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private MasterSyncDao masterSyncDao;

	@Autowired
	private MasterSyncService masterSyncService;
	public static Map<String, TreeMap<Integer, FxControl>> locationMap = new LinkedHashMap<String, TreeMap<Integer, FxControl>>();

	/**
	 * Key- String : is a screen name Value - Node : screenNode {@summary all screen
	 * fields will be inside the screen node}
	 */
	private Map<String, Node> screenMap = new LinkedHashMap<>();

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

		if (getRegistrationDTOFromSession() == null) {
			validation.updateAsLostUIN(false);
			registrationController.createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_NEW);
		}

		flowPane.setVgap(10);
		flowPane.setHgap(10);

		ObservableList<Node> flowPaneNodes = flowPane.getChildren();
		int count = 0;
		if (screens != null && !screens.isEmpty()) {
			for (Entry<String, List<String>> screenEntry : screens.entrySet()) {

				GridPane screenGridPane = new GridPane();

				screenGridPane.setHgap(15);
				screenGridPane.setVgap(15);

				screenGridPane.getStyleClass().add("-fx-background-color: blue; -fx-grid-lines-visible: true");

				flowPane.prefWidthProperty().bind(layOutGridPane.widthProperty());

				flowPaneNodes.add(screenGridPane);
				String screenName = screenEntry.getKey();

				List<String> fields = screenEntry.getValue();

				if (fields != null && !fields.isEmpty()) {

					for (String field : fields) {

						if (!field.isEmpty()) {

							UiSchemaDTO uiSchemaDTO = getValidationMap().get(field);

							FxControl fieldNode = (FxControl) buildFxElement(uiSchemaDTO);

							if (fieldNode != null && fieldNode.getNode() != null) {
								Node node = fieldNode.getNode();

								GridPane gridPane = new GridPane();

								ColumnConstraints columnConstraint1 = new ColumnConstraints();
								columnConstraint1.setPercentWidth(20);
								ColumnConstraints columnConstraint2 = new ColumnConstraints();
								columnConstraint2.setPercentWidth(60);
								ColumnConstraints columnConstraint3 = new ColumnConstraints();
								columnConstraint3.setPercentWidth(20);
								gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,
										columnConstraint3);
								gridPane.add(node, 1, 2);

								screenGridPane.add(gridPane, 0, count++);
							}
						}
					}
				}

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

		}
	}

	private Node buildFxElement(UiSchemaDTO uiSchemaDTO) {

		switch (uiSchemaDTO.getControlType()) {

		case CONTROLTYPE_TEXTFIELD:
			return new TextFieldFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_BIOMETRICS:

			return new BiometricFxControl().build(uiSchemaDTO);
		case CONTROLTYPE_BUTTON:
			break;
		case CONTROLTYPE_CHECKBOX:
			break;
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

}
