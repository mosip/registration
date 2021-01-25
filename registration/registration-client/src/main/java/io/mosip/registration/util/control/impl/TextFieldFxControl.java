/**
 * 
 */
package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author YASWANTH S
 *
 */
@Component
public class TextFieldFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);

	private static String loggerClassName = " Text Field Control Type Class";

	@Autowired
	private BaseController baseController;

	@Autowired
	private DemographicDetailController demographicDetailController;
	@Autowired
	private ResourceLoader resourceLoader;
	@Autowired
	private Validations validation;
	@Autowired
	private DemographicChangeActionHandler demographicChangeActionHandler;

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		VBox primaryLangVBox = create(uiSchemaDTO, "");

		HBox hBox = new HBox();
		hBox.getChildren().add(primaryLangVBox);

		if (baseController.isLocalLanguageAvailable() && !baseController.isAppLangAndLocalLangSame()) {

			VBox secondaryLangVBox = create(uiSchemaDTO, RegistrationConstants.LOCAL_LANGUAGE);

			hBox.getChildren().add(secondaryLangVBox);

		}

		this.node = hBox;

		setListener((TextField) getField(RegistrationConstants.HASH + uiSchemaDTO.getId()));

		return this.control;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {

		// TODO Throw Reg Check based exception if src or target nodes were not present
		if (srcNode != null && targetNodes != null && !targetNodes.isEmpty()) {
			TextField srctextField = (TextField) srcNode;

			for (Node targetNode : targetNodes) {

				TextField targetTextField = (TextField) targetNode;
				targetTextField.setText(srctextField.getText());
			}
		}
	}

	@Override
	public void setData(Object data) {

		// TODO Set Data in registration DTO
	}

	@Override
	public UiSchemaDTO getUiSchemaDTO() {

		return this.uiSchemaDTO;
	}

	@Override
	public void setListener(Node node) {
		FXUtils.getInstance().onTypeFocusUnfocusListener(getNode(), (TextField) node);

		TextField textField = (TextField) node;
		textField.addEventHandler(Event.ANY, event -> {
			if (isValid(textField)) {

				Object object = getData(node);
				setData(object);

				// handling other handlers
				UiSchemaDTO uiSchemaDTO = validation.getValidationMap()
						.get(node.getId().replaceAll(RegistrationConstants.ON_TYPE, RegistrationConstants.EMPTY)
								.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY));
				if (uiSchemaDTO != null) {
					LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
							"Invoking external action handler for .... " + uiSchemaDTO.getId());
					demographicChangeActionHandler.actionHandle(getNode(), node.getId(), uiSchemaDTO.getChangeAction());
				}
				// Group level visibility listeners
				refreshFields();
			}
		});

	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String languageType) {

		String fieldName = uiSchemaDTO.getId();

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + languageType + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		String titleText = (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
				? uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY)
				: uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY)) + mandatorySuffix;

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(fieldName + languageType + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);
		simpleTypeVBox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = getTextField(fieldName + languageType, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth,
				languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
						&& !uiSchemaDTO.getType().equals(RegistrationConstants.SIMPLE_TYPE) ? true : false);
		simpleTypeVBox.getChildren().add(textField);

		/** Validation message (Invalid/wrong,,etc,.) */
		Label validationMessage = getLabel(fieldName + languageType + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, prefWidth);
		simpleTypeVBox.getChildren().add(validationMessage);

		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE) && !textField.isDisable()) {
			// If Local Language and simpleType : Set KeyBoard

			addKeyBoard(simpleTypeVBox, validationMessage, textField);

		}

		return simpleTypeVBox;
	}

	private void addKeyBoard(VBox simpleTypeVBox, Label validationMessage, TextField textField) {
		ImageView keyBoardImgView = getKeyBoardImage();

		if (keyBoardImgView != null) {
			keyBoardImgView.setOnMouseClicked((event) -> {
				demographicDetailController.setFocusonLocalField(event);
			});

			demographicDetailController.getVirtualKeyBoard().changeControlOfKeyboard(textField);

			HBox keyBoardHBox = new HBox();
			keyBoardHBox.setSpacing(20);
			keyBoardHBox.getChildren().add(keyBoardImgView);
			keyBoardHBox.getChildren().add(validationMessage);
			keyBoardHBox.setStyle("-fx-background-color:WHITE");
			simpleTypeVBox.getChildren().add(keyBoardHBox);
		}
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	private Label getLabel(String id, String titleText, String styleClass, boolean isVisible, double prefWidth) {
		/** Field Title */
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		label.setPrefWidth(prefWidth);
		return label;
	}

	private ImageView getKeyBoardImage() {
		ImageView imageView = null;
		try {
			imageView = new ImageView(new Image(
					resourceLoader.getResource(RegistrationConstants.KEYBOARD_WITH_CLASSPATH).getInputStream()));
			imageView.setId(uiSchemaDTO.getId());
			imageView.setFitHeight(20.00);
			imageView.setFitWidth(22.00);
		} catch (IOException runtimeException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"keyboard.png image not found in resource folder" + runtimeException.getMessage()
							+ ExceptionUtils.getStackTrace(runtimeException));

		}

		return imageView;
	}

	private String getMandatorySuffix(UiSchemaDTO schema) {
		String mandatorySuffix = RegistrationConstants.EMPTY;
		RegistrationDTO registrationDTO = baseController.getRegistrationDTOFromSession();
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

	@Override
	public Object getData(Node node) {

		// TODO get value form registration DTIO

		// TODO move logic to set data
		if (this.uiSchemaDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {

			List<SimpleDto> simpleDtos = new LinkedList<>();

			// TODO set Simple DTO
			SimpleDto primaryLangSimpleDto = null;

			simpleDtos.add(primaryLangSimpleDto);

			if (baseController.isLocalLanguageAvailable() && !baseController.isAppLangAndLocalLangSame()) {
				// TODO set Simple DTO
				SimpleDto secondaryLangSimpleDto = null;

				simpleDtos.add(secondaryLangSimpleDto);
			}
			return simpleDtos;
		} else {
			return ((TextField) node).getText();
		}

	}

	@Override
	public boolean isValid(Node node) {

		boolean isValid;
		if (node == null) {
			LOGGER.warn(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Field not found in demographic screen");
			return false;
		}

		TextField field = (TextField) node;
		if (validation.validateTextField(getNode(), field, field.getId(), true)) {

			FXUtils.getInstance().setTextValidLabel(getNode(), field);
			isValid = true;
		} else {

			FXUtils.getInstance().showErrorLabel(field, getNode());
			return false;
		}
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"validating text field secondary");

		TextField localField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);
		if (localField != null) {
			if (validation.validateTextField(getNode(), field, field.getId(), true)) {

				FXUtils.getInstance().setTextValidLabel(getNode(), localField);
				isValid = true;
			} else {

				FXUtils.getInstance().showErrorLabel(localField, getNode());
				return false;
			}
		}
		return isValid;

	}

	@Override
	public VBox getNode() {
		return (VBox) this.node;
	}

	private Node getField(String id) {
		return node.lookup(RegistrationConstants.HASH + uiSchemaDTO.getId());
	}
}
