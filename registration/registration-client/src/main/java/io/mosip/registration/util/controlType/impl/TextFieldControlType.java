/**
 * 
 */
package io.mosip.registration.util.controlType.impl;

import java.util.List;

import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.controlType.AbstractControlType;
import io.mosip.registration.util.controlType.ControlType;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * @author YASWANTH S
 *
 */
public class TextFieldControlType extends AbstractControlType {

	@Override
	public Node build(UiSchemaDTO uiSchemaDTO, Pane parentPane) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.fieldType = this;
		createUiElements(parentPane);

		addListener();
		
		return null;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void disable() {
		// TODO Auto-generated method stub

	}

	@Override
	public void visible() {
		// TODO Auto-generated method stub

	}

	@Override
	public UiSchemaDTO getUiSchemaDTO() {
		// TODO Auto-generated method stub
		return null;
	}

	private void createUiElements(Pane parentPane) {

		// TODO Create Label, field , error message
	}

	private void addListener() {

	}

	@Override
	public void actionHandle(Pane parentPane, String fieldId, String changeAction) {
		// TODO Auto-generated method stub
		
	}

}
