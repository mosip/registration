/**
 * 
 */
package io.mosip.registration.util.controlType.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.controlType.ControlType;
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
public class TextFieldControlType extends ControlType {

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

	@Override
	public Node build(UiSchemaDTO uiSchemaDTO, String languageType) {
		this.uiSchemaDTO = uiSchemaDTO;

		this.fieldType = this;

		VBox simpleTypeVBox = create(uiSchemaDTO, languageType);

		TextField textField = (TextField) simpleTypeVBox
				.lookup(RegistrationConstants.HASH + uiSchemaDTO.getId() + languageType);

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

	@Override
	public void setListener(Node node) {
		// TODO Auto-generated method stub

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

}
