package io.mosip.registration.util.control.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class BiometricFxControl extends FxControl {

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;
		
		GridPane biometricGridPane = create(uiSchemaDTO);

		// TODO Auto-generated method stub
		return null;
	}

	private GridPane create(UiSchemaDTO uiSchemaDTO) {
		
		
		Map<String, List<String>> modalityAttributeMap = new LinkedHashMap<>();
		
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
	public boolean isValid(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public UiSchemaDTO getUiSchemaDTO() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setListener(Node node) {
		// TODO Auto-generated method stub

	}

}
