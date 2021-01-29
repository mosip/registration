/**
 * 
 */
package io.mosip.registration.util.control;

import java.util.List;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

/**
 * 
 * Control Type will give high level controls for fields TextField,CheckBox,
 * DropDown,DropDown, Buttons, document type, Biometric Type
 * 
 * It also provides a features to copy,disable and visible
 * 
 * @author YASWANTH S
 *
 */
public abstract class FxControl extends Node {

	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "AbstractControlType";

	protected UiSchemaDTO uiSchemaDTO;

	protected FxControl control;

	protected Pane parentPane;
	public Node node;

	protected DemographicDetailController demographicDetailController;

	public void refreshFields() {

	}

	/**
	 * Build Error code, title and fx Element Set Listeners Set Actione events
	 * 
	 * @param uiSchemaDTO field information
	 * @param parentPane  field to be placed
	 */
	public abstract FxControl build(UiSchemaDTO uiSchemaDTO);

	/**
	 * Copy the value from source node to target nodes
	 * 
	 * @param srcNode     copy from
	 * @param targetNodes copy to
	 */
	public abstract void copyTo(Node srcNode, List<Node> targetNodes);

	/**
	 * 
	 * Set Data into fx element
	 * 
	 * @param data value
	 */
	public abstract void setData(Object data);

	/**
	 * Get Value from fx element
	 * 
	 * @return Value
	 */
	public abstract Object getData(Node node);

	/**
	 * Check value is valid or not
	 * 
	 * @return boolean is valid or not
	 */
	public abstract boolean isValid(Node node);

	/**
	 * Disable the field
	 */
	public void disable(Node node, boolean isDisable) {

		node.setDisable(isDisable);

	}

	/**
	 * Hide the field
	 */
	public void visible(Node node, boolean isVisible) {

		node.setVisible(isVisible);

	}

	protected String getMandatorySuffix(UiSchemaDTO schema) {
		String mandatorySuffix = RegistrationConstants.EMPTY;
		RegistrationDTO registrationDTO = demographicDetailController.getRegistrationDTOFromSession();
		String categeory = registrationDTO.getRegistrationCategory();
		switch (categeory) {
		case RegistrationConstants.PACKET_TYPE_UPDATE:
			if (registrationDTO.getUpdatableFields().contains(schema.getId())) {
				mandatorySuffix = schema.isRequired() ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
			}
			break;

		case RegistrationConstants.PACKET_TYPE_NEW:
			mandatorySuffix = schema.isRequired() ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
			break;
		}
		return mandatorySuffix;
	}

	protected Label getLabel(String id, String titleText, String styleClass, boolean isVisible, double prefWidth) {
		/** Field Title */
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		label.setPrefWidth(prefWidth);
		return label;
	}

	protected Node getField(String id) {
		return node.lookup(RegistrationConstants.HASH + uiSchemaDTO.getId());
	}

	public abstract UiSchemaDTO getUiSchemaDTO();

	public abstract void setListener(Node node);

	public abstract Node getNode();
}
