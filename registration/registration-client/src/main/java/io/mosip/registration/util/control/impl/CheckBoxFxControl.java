package io.mosip.registration.util.control.impl;

import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import org.springframework.context.ApplicationContext;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class CheckBoxFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(CheckBoxFxControl.class);
	public static final String HASH = "#";
	private DemographicChangeActionHandler demographicChangeActionHandler;

	public CheckBoxFxControl() {
		ApplicationContext applicationContext = Initialization.getApplicationContext();
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;
		this.node = create(uiSchemaDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String langCode) {
		String fieldName = uiSchemaDTO.getId();

		/** Container holds title, fields */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiSchemaDTO.getLabel().get(lCode));});

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** CheckBox */
		CheckBox checkBox = getCheckBox(fieldName,
				String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiSchemaDTO),
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);

		setListener(checkBox);
		simpleTypeVBox.getChildren().add(checkBox);
		simpleTypeVBox.getChildren().add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
		changeNodeOrientation(simpleTypeVBox, langCode);
		return simpleTypeVBox;
	}

	private CheckBox getCheckBox(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {
		CheckBox checkBox = new CheckBox(titleText);
		checkBox.setId(id);
		// checkBox.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		checkBox.setPrefWidth(prefWidth);
		checkBox.setDisable(isDisable);

		return checkBox;
	}


	@Override
	public void setData(Object data) {
		CheckBox checkBox = (CheckBox) getField(uiSchemaDTO.getId());
		getRegistrationDTo().addDemographicField(uiSchemaDTO.getId(), checkBox == null ? "N"
								: checkBox.isSelected() ? "Y" : "N");
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}


	@Override
	public boolean isValid() {
		if(requiredFieldValidator.isRequiredField(this.uiSchemaDTO, getRegistrationDTo())){
			CheckBox checkBox = (CheckBox) getField(uiSchemaDTO.getId());
			return checkBox == null ? false : checkBox.isSelected() ? true : false;
		}
		return true;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}


	@Override
	public void setListener(Node node) {
		CheckBox checkBox = (CheckBox) node;
		checkBox.selectedProperty().addListener((options, oldValue, newValue) -> {
			// handling other handlers
			demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
					uiSchemaDTO.getChangeAction());
			// Group level visibility listeners
			refreshFields();
		});

	}

	@Override
	public void fillData(Object data) {
		selectAndSet(data);
	}

	@Override
	public void selectAndSet(Object data) {
		CheckBox checkBox = (CheckBox) getField(uiSchemaDTO.getId());
		checkBox.setSelected(data != null && ((String)data).equals("Y") ? true : false);
	}

}
