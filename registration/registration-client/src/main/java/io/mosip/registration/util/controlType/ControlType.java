/**
 * 
 */
package io.mosip.registration.util.controlType;

import java.util.List;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.UiSchemaDTO;
import javafx.scene.Node;
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
public abstract class ControlType extends Node {

	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "AbstractControlType";

	protected UiSchemaDTO uiSchemaDTO;

	protected ControlType fieldType;

	protected Pane parentPane;

	public void refreshFields() {

	}

	/**
	 * Build Error code, title and fx Element Set Listeners Set Actione events
	 * 
	 * @param uiSchemaDTO field information
	 * @param parentPane  field to be placed
	 */
	public abstract Node build(UiSchemaDTO uiSchemaDTO, String languageType);

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
	public abstract Object getData();

	/**
	 * Check value is valid or not
	 * 
	 * @return boolean is valid or not
	 */
	public abstract boolean isValid();

	/**
	 * Disable the field
	 */
	public abstract void disable();

	/**
	 * Hide the field
	 */
	public abstract void visible();

	public abstract UiSchemaDTO getUiSchemaDTO();

	public abstract void setListener();
}
