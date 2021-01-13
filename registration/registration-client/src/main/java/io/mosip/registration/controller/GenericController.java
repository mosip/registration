package io.mosip.registration.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Controller;

import io.mosip.registration.dto.UiSchemaDTO;
import javafx.fxml.FXML;
import javafx.scene.Node;
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

		if (screens != null && !screens.isEmpty()) {
			for (Entry<String, List<String>> screenEntry : screens.entrySet()) {

				String screenName = screenEntry.getKey();

				List<String> fields = screenEntry.getValue();

				if (fields != null && !fields.isEmpty()) {

					for (String field : fields) {

						if (!field.isEmpty()) {

							UiSchemaDTO uiSchemaDTO = getValidationMap().get(field);

							Node fieldNode = buildFxElement(uiSchemaDTO);
						}
					}
				}
			}

		}
	}

	private Node buildFxElement(UiSchemaDTO uiSchemaDTO) {

		Node fieldNode = null;
		
//		ControlType controlType= new TextFieldControlType();
		
//		controlType.ge
		switch (uiSchemaDTO.getContactType()) {

		case CONTROLTYPE_TEXTFIELD:
			break;
		case CONTROLTYPE_BIOMETRICS:
			break;
		case CONTROLTYPE_BUTTON:
			break;
		case CONTROLTYPE_CHECKBOX:
			break;
		case CONTROLTYPE_DOB:
			break;
		case CONTROLTYPE_DOB_AGE:
			break;
		case CONTROLTYPE_DOCUMENTS:
			break;
		case CONTROLTYPE_DROPDOWN:
			break;

		}

		return fieldNode;
	}

}
