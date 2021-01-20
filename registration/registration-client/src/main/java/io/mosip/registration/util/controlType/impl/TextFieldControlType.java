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

		Node textFieldNode = null;

		// TODO Create Label
		// Create text field
		// Create error label

		createField(uiSchemaDTO, languageType);
		setListener();

		return null;
	}

	private void createField(UiSchemaDTO uiSchemaDTO, String languageType) {

		String fieldName = uiSchemaDTO.getId();

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Container holds title, fields and validation message elements */
		VBox vbox = new VBox();
		vbox.setId(fieldName + languageType + RegistrationConstants.VBOX);
		vbox.setSpacing(5);

		String titleText = (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
				? uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY)
				: uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY)) + mandatorySuffix;
		/** Field Title */
		Label fieldTitle = new Label();
		fieldTitle.setId(fieldName + languageType + RegistrationConstants.LABEL);
		fieldTitle.setText(titleText);
		fieldTitle.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
		fieldTitle.setVisible(false);
		fieldTitle.setPrefWidth(vbox.getPrefWidth());
		vbox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(fieldName + languageType);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setPrefWidth(vbox.getPrefWidth());
		textField.setDisable(languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
				&& !uiSchemaDTO.getType().equals(RegistrationConstants.SIMPLE_TYPE) ? true : false);
		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE) && !textField.isDisable()) {
			// If Local Language and simpleType : Set KeyBoard

			ImageView keyBoardImgView = getKeyBoardImage();

			if (keyBoardImgView != null) {
				keyBoardImgView.setOnMouseClicked((event) -> {
					demographicDetailController.setFocusonLocalField(event);
				});

				demographicDetailController.getVirtualKeyBoard().changeControlOfKeyboard(textField);
			}

		}
		vbox.getChildren().add(textField);

		/** Validation message (Invalid/wrong,,etc,.) */
		Label validationMessage = new Label();
		validationMessage.setId(fieldName + languageType + RegistrationConstants.MESSAGE);
		validationMessage.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
		validationMessage.setPrefWidth(vbox.getPrefWidth());
		vbox.getChildren().add(validationMessage);

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
	public void setListener() {
		// TODO Auto-generated method stub

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
