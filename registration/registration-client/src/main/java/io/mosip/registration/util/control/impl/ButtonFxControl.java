package io.mosip.registration.util.control.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.util.control.FxControl;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

	public ButtonFxControl() {
		ApplicationContext applicationContext = Initialization.getApplicationContext();
		fxUtils = FXUtils.getInstance();
		regApplicationContext = io.mosip.registration.context.ApplicationContext.getInstance();
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;

		HBox hBox = new HBox();

		VBox primaryLangVBox = create(uiSchemaDTO, "");

		hBox.getChildren().add(primaryLangVBox);
		HBox.setHgrow(primaryLangVBox, Priority.ALWAYS);

		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(),
				primaryLangVBox);

//		if (demographicDetailController.isLocalLanguageAvailable()
//				&& !demographicDetailController.isAppLangAndLocalLangSame()) {
//			VBox secondaryLangVBox = create(uiSchemaDTO, RegistrationConstants.LOCAL_LANGUAGE);
//
//			Region region = new Region();
//			HBox.setHgrow(region, Priority.ALWAYS);
//
//			HBox.setHgrow(secondaryLangVBox, Priority.ALWAYS);
//
//			hBox.getChildren().addAll(region, secondaryLangVBox);
//
//			nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getLocalLanguage(),
//					secondaryLangVBox);
//		}
//
//		setNodeMap(nodeMap);
		this.node = hBox;

		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String languageType) {
		String fieldName = uiSchemaDTO.getId();

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + languageType + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		HBox hbox = new HBox();
		hbox.setId(fieldName + languageType + RegistrationConstants.HBOX);

		String titleText = (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
				? uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY)
				: uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY)) + mandatorySuffix;

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(fieldName + languageType + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.BUTTONS_LABEL, true, prefWidth);

		hbox.getChildren().add(fieldTitle);

		simpleTypeVBox.getChildren().add(hbox);

		/** Validation message (Invalid/wrong,,etc,.) */
		Label validationMessage = getLabel(fieldName + languageType + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, prefWidth);
		simpleTypeVBox.getChildren().add(validationMessage);

		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
			simpleTypeVBox.setDisable(true);
			Validations.putIntoLabelMap(fieldName + languageType,
					uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY));
		} else {
			Validations.putIntoLabelMap(fieldName + languageType,
					uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY));
		}

		changeNodeOrientation(simpleTypeVBox, languageType);

		return simpleTypeVBox;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {
//		HBox primaryHbox = (HBox) getField(uiSchemaDTO.getId() + RegistrationConstants.HBOX);
//		HBox secondaryHBox = (HBox) getField(
//				uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE + RegistrationConstants.HBOX);
//		getRegistrationDTo().addDemographicField(uiSchemaDTO.getId(), regApplicationContext.getApplicationLanguage(),
//				getSelectedValue(primaryHbox), regApplicationContext.getLocalLanguage(),
//				getSelectedValue(secondaryHBox));
	}

	private String getSelectedValue(HBox hBox) {
		if (hBox != null) {
			for (Node node : hBox.getChildren()) {
				if (node instanceof Button) {
					Button button = (Button) node;
					if (button.getStyleClass().contains(selectedResidence)) {
						return button.getText();
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
					val.get(RegistrationConstants.PRIMARY), "");
			setItems(
					(HBox) getField(
							uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE + RegistrationConstants.HBOX),
					val.get(RegistrationConstants.SECONDARY), RegistrationConstants.LOCAL_LANGUAGE);
		}
	}

	private void setItems(HBox hBox, List<GenericDto> val, String languageType) {
		if (hBox != null && val != null && !val.isEmpty()) {
			val.forEach(genericDto -> {
				Button button = new Button(genericDto.getName());
				button.setId(uiSchemaDTO.getId() + genericDto.getCode() + languageType);
				hBox.setSpacing(10);
				hBox.setPadding(new Insets(10, 10, 10, 10));
				button.getStyleClass().addAll(residence, buttonStyle);
				hBox.getChildren().add(button);

				if (!languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
					setListener(button);
				}
			});

		}
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setListener(Node node) {
		Button button = (Button) node;
		button.addEventHandler(ActionEvent.ACTION, event -> {
			if (button.getStyleClass().contains(residence)) {
				resetButtons(button);
//				if (demographicDetailController.isLocalLanguageAvailable()
//						&& !demographicDetailController.isAppLangAndLocalLangSame()) {
//					Node localButton = getField(button.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//					if (localButton != null) {
//						resetButtons((Button) localButton);
//					}
//				}
				setData(null);
			}
			fxUtils.toggleUIField((Pane) this.node, button.getParent().getId() + RegistrationConstants.MESSAGE, false);
//			if (demographicDetailController.isLocalLanguageAvailable()
//					&& !demographicDetailController.isAppLangAndLocalLangSame()) {
//				Node localButton = getField(button.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//				if (localButton != null) {
//					fxUtils.toggleUIField((Pane) this.node,
//							localButton.getParent().getId() + RegistrationConstants.MESSAGE, false);
//				}
//			}
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

					if (simpleDto.getLanguage().equalsIgnoreCase(
							io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage())) {

						HBox appHBox = (HBox) getField(uiSchemaDTO.getId() + RegistrationConstants.HBOX);

						for (Node node : appHBox.getChildren()) {

							if (node instanceof Button
									&& ((Button) node).getText().equalsIgnoreCase(simpleDto.getValue())) {

								((Button) node).fire();
							}
						}

					} /*
						 * else if (simpleDto.getLanguage().equalsIgnoreCase(
						 * io.mosip.registration.context.ApplicationContext.getInstance().
						 * getLocalLanguage())) {
						 * 
						 * HBox langHBox = (HBox) getField(uiSchemaDTO.getId() +
						 * RegistrationConstants.LOCAL_LANGUAGE + RegistrationConstants.HBOX);
						 * 
						 * for (Node node : langHBox.getChildren()) {
						 * 
						 * if (node instanceof Button && ((Button)
						 * node).getText().equalsIgnoreCase(simpleDto.getValue())) {
						 * 
						 * ((Button) node).fire(); } } }
						 */
				}

			}
		}
	}

}
