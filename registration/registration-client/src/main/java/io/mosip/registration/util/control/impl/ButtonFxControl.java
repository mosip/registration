package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.control.FxControl;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ButtonFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);

	private static String loggerClassName = " Button Control Type Class";

	private FXUtils fxUtils;

	private io.mosip.registration.context.ApplicationContext regApplicationContext;

	private String selectedResidence = "genderSelectedButton";

	private String residence = "genderButton";

	private String buttonStyle = "button";

	private MasterSyncService masterSyncService;

	public ButtonFxControl() {
		ApplicationContext applicationContext = Initialization.getApplicationContext();
		fxUtils = FXUtils.getInstance();
		regApplicationContext = io.mosip.registration.context.ApplicationContext.getInstance();
		masterSyncService = applicationContext.getBean(MasterSyncService.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		this.node = create(uiSchemaDTO);
		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO) {
		String fieldName = uiSchemaDTO.getId();

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();

		HBox hbox = new HBox();
		hbox.setId(fieldName + RegistrationConstants.HBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, null, RegistrationConstants.BUTTONS_LABEL,
				true, prefWidth);

		String labelText = "";
		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {

			String label = uiSchemaDTO.getLabel().get(langCode);
			labelText = labelText.isEmpty() ? labelText : labelText + RegistrationConstants.SLASH;
			labelText += label;
		}

		fieldTitle.setText(labelText + mandatorySuffix);

		hbox.getChildren().add(fieldTitle);

		simpleTypeVBox.getChildren().add(hbox);

		changeNodeOrientation(simpleTypeVBox, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		return simpleTypeVBox;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {
		HBox primaryHbox = (HBox) getField(uiSchemaDTO.getId() + RegistrationConstants.HBOX);

		Button selectedButton = getSelectedButton(primaryHbox);

		String code = selectedButton.getId().replaceAll(uiSchemaDTO.getId(), "");

		List<SimpleDto> values = new ArrayList<SimpleDto>();
		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {

			Optional<GenericDto> result;
			try {
				result = masterSyncService.getFieldValues(uiSchemaDTO.getId(), langCode).stream()
						.filter(b -> b.getCode().equalsIgnoreCase(code)).findFirst();
				if (result.isPresent()) {

					values.add(new SimpleDto(langCode, result.get().getName()));
				}
			} catch (RegBaseCheckedException exception) {
				LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						ExceptionUtils.getStackTrace(exception));
			}
		}

		getRegistrationDTo().addDemographicField(uiSchemaDTO.getId(), values);
	}

	private Button getSelectedButton(HBox hBox) {
		if (hBox != null) {
			for (Node node : hBox.getChildren()) {
				if (node instanceof Button) {
					Button button = (Button) node;
					if (button.getStyleClass().contains(selectedResidence)) {
						return button;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void fillData(Object data) {
		if (data != null) {
			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;
			setItems((HBox) getField(uiSchemaDTO.getId() + RegistrationConstants.HBOX),
					val.get(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));
		}
	}

	private void setItems(HBox hBox, List<GenericDto> val) {
		if (hBox != null && val != null && !val.isEmpty()) {
			val.forEach(genericDto -> {
				Button button = new Button(genericDto.getName());
				button.setId(uiSchemaDTO.getId() + genericDto.getCode());
				hBox.setSpacing(10);
				hBox.setPadding(new Insets(10, 10, 10, 10));
				button.getStyleClass().addAll(residence, buttonStyle);
				hBox.getChildren().add(button);

				setListener(button);
			});

		}
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid(Node node) {
		Button button = (Button) node;

		return button.getStyleClass().contains(residence);
	}

	@Override
	public void setListener(Node node) {
		Button button = (Button) node;
		button.addEventHandler(ActionEvent.ACTION, event -> {
			if (isValid(node)) {
				resetButtons(button);
				setData(null);
			}

		});
	}

	private void resetButtons(Button button) {
		button.getStyleClass().clear();
		button.getStyleClass().addAll(selectedResidence, buttonStyle);
		button.getParent().getChildrenUnmodifiable().forEach(node -> {
			if (node instanceof Button && !node.getId().equals(button.getId())) {
				node.getStyleClass().clear();
				node.getStyleClass().addAll(residence, buttonStyle);
			}
		});
	}

	@Override
	public void selectAndSet(Object data) {
		if (data != null) {

			Object val = null;

			if (val instanceof List) {

				List<SimpleDto> list = (List<SimpleDto>) val;

				for (SimpleDto simpleDto : list) {

					if (getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)
							.equalsIgnoreCase(simpleDto.getLanguage())) {

						HBox appHBox = (HBox) getField(uiSchemaDTO.getId() + RegistrationConstants.HBOX);

						for (Node node : appHBox.getChildren()) {

							if (node instanceof Button
									&& ((Button) node).getText().equalsIgnoreCase(simpleDto.getValue())) {

								((Button) node).fire();
							}
						}

						break;
					}

				}

			}
		}
	}

}
