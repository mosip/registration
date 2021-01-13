/**
 * 
 */
package io.mosip.registration.util.controlType;

import java.util.List;

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
public interface ControlType{

	/**
	 * Build Error code, title and fx Element Set Listeners Set Actione events
	 * 
	 * @param uiSchemaDTO field information
	 * @param parentPane field to be placed
	 */
	public Node build(UiSchemaDTO uiSchemaDTO, Pane parentPane);

	/**
	 * Copy the value from source node to target nodes
	 * 
	 * @param srcNode     copy from
	 * @param targetNodes copy to
	 */
	public void copyTo(Node srcNode, List<Node> targetNodes);

	/**
	 * 
	 * Set Data into fx element
	 * 
	 * @param data value
	 */
	public void setData(Object data);

	/**
	 * Get Value from fx element
	 * 
	 * @return Value
	 */
	public Object getData();

	/**
	 * Check value is valid or not
	 * 
	 * @return boolean is valid or not
	 */
	public boolean isValid();

	/**
	 * Disable the field
	 */
	public void disable();

	/**
	 * Hide the field
	 */
	public void visible();

	public UiSchemaDTO getUiSchemaDTO();
}
