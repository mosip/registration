/**
 * 
 */
package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.VirtualKeyboard;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * @author YASWANTH S
 *
 */
public class TextFieldFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(TextFieldFxControl.class);

	private static String loggerClassName = " Text Field Control Type Class";

	private Validations validation;
	private DemographicChangeActionHandler demographicChangeActionHandler;

	public TextFieldFxControl() {

		ApplicationContext applicationContext = Initialization.getApplicationContext();

		validation = applicationContext.getBean(Validations.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		this.node = create(uiSchemaDTO);
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

		RegistrationDTO registrationDTO = getRegistrationDTo();

		if (this.uiSchemaDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
			List<SimpleDto> values = new ArrayList<SimpleDto>();

			for (String langCode : registrationDTO.getSelectedLanguagesByApplicant()) {

				TextField textField = (TextField) getField(uiSchemaDTO.getId() + langCode);

				SimpleDto simpleDto = new SimpleDto(langCode, textField.getText());
				values.add(simpleDto);

			}

			registrationDTO.addDemographicField(uiSchemaDTO.getId(), values);

		} else {
			registrationDTO
					.addDemographicField(uiSchemaDTO.getId(),
							((TextField) getField(
									uiSchemaDTO.getId() + registrationDTO.getSelectedLanguagesByApplicant().get(0)))
											.getText());

		}
	}

	@Override
	public void setListener(Node node) {
		FXUtils.getInstance().onTypeFocusUnfocusListener((Pane) getNode(), (TextField) node);

		TextField textField = (TextField) node;

		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (isValid(textField)) {

				setData(null);

				// handling other handlers
				UiSchemaDTO uiSchemaDTO = validation.getValidationMap()
						.get(node.getId().replaceAll(RegistrationConstants.ON_TYPE, RegistrationConstants.EMPTY)
								.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY));
				if (uiSchemaDTO != null) {
					LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
							"Invoking external action handler for .... " + uiSchemaDTO.getId());
					demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
							uiSchemaDTO.getChangeAction());
				}

			} else {
				getRegistrationDTo().getDemographics().remove(this.uiSchemaDTO.getId());
			}
			// Group level visibility listeners
			refreshFields();
		});

	}

	private VBox create(UiSchemaDTO uiSchemaDTO) {

		String labelText = "";

		String fieldName = uiSchemaDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Title label */
		Label fieldTitle = getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, simpleTypeVBox.getWidth());
		simpleTypeVBox.getChildren().add(fieldTitle);

		List<String> langCodes = new LinkedList<>();

		List<String> selectedLanguages = getRegistrationDTo().getSelectedLanguagesByApplicant();

		if (this.uiSchemaDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
			langCodes.addAll(selectedLanguages);
		} else {
			langCodes.add(selectedLanguages.get(0));
		}
		for (String langCode : langCodes) {

			String label = uiSchemaDTO.getLabel().get(langCode);
			labelText = labelText.isEmpty() ? labelText : labelText + RegistrationConstants.SLASH;
			labelText += label;

			/** Text Field */
			TextField textField = getTextField(fieldName + langCode, label + mandatorySuffix,
					RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, simpleTypeVBox.getWidth(), false);
			simpleTypeVBox.getChildren().add(textField);

			/** Validation message (Invalid/wrong,,etc,.) */
			Label validationMessage = getLabel(fieldName + langCode + RegistrationConstants.MESSAGE, null,
					RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getWidth());
			simpleTypeVBox.getChildren().add(validationMessage);
			addKeyBoard(simpleTypeVBox, validationMessage, textField);
			changeNodeOrientation(simpleTypeVBox, langCode);

			Validations.putIntoLabelMap(fieldName + langCode, uiSchemaDTO.getLabel().get(langCode));

			setListener(textField);
		}

		fieldTitle.setText(labelText + mandatorySuffix);

		return simpleTypeVBox;
	}

	private void addKeyBoard(VBox simpleTypeVBox, Label validationMessage, TextField textField) {
		ImageView keyBoardImgView = getKeyBoardImage();

		if (keyBoardImgView != null) {
			keyBoardImgView.setOnMouseClicked((event) -> {
				setFocusonLocalField(event);
			});

			// TODO Check lang value
			VirtualKeyboard keyBoard = new VirtualKeyboard(null);
			keyBoard.view();
			keyBoard.changeControlOfKeyboard(textField);

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
		// textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	private ImageView getKeyBoardImage() {
		ImageView imageView = null;

		imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/keyboard.png")));
		imageView.setId(uiSchemaDTO.getId() + "KeyBoard");
		imageView.setFitHeight(20.00);
		imageView.setFitWidth(22.00);

		return imageView;
	}

	@Override
	public Object getData() {

		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid(Node node) {

		boolean isValid = true;
		if (node == null) {
			LOGGER.warn(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Field not found in demographic screen");
			return false;
		}

		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {

			TextField textField = (TextField) getField(uiSchemaDTO.getId() + langCode);
			if (textField != null) {

				if (validation.validateTextField((Pane) getNode(), textField, textField.getId(), true, langCode)) {

					FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField);
					isValid = !isValid ? isValid : true;
				} else {

					FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
					isValid = false;
				}
			}

		}

		return isValid;

	}

//	@Override
//	public HBox getNode() {
//		return (HBox) this.node;
//	}

	@Override
	public void fillData(Object data) {

		selectAndSet(data);

	}

	@Override
	public void selectAndSet(Object data) {

		if (data != null) {
			if (data instanceof String) {

				TextField textField = (TextField) getField(
						uiSchemaDTO.getId() + getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

				textField.setText((String) data);
			} else if (data instanceof List) {

				List<SimpleDto> list = (List<SimpleDto>) data;

				for (SimpleDto simpleDto : list) {

					TextField textField = (TextField) getField(uiSchemaDTO.getId() + simpleDto.getLanguage());

					if (textField != null) {
						textField.setText(simpleDto.getValue());
					}
				}

			}
		}

	}

	/**
	 *
	 * Setting the focus to specific fields when keyboard loads
	 *
	 */
	public void setFocusonLocalField(MouseEvent event) {

		// Set foucus field
//		try {
//			Node node = (Node) event.getSource();
//			if (isLocalLanguageAvailable() && !isAppLangAndLocalLangSame()) {
//				node.requestFocus();
//				keyboardNode.setVisible(true);
//				keyboardNode.setManaged(true);
//				//addKeyboard(positionTracker.get((node.getId() + "ParentGridPane")) + 1);
//				Node parentNode = node.getParent().getParent();
//				if (keyboardVisible) {
//					if(previousNode != null) {
//						((VBox)previousNode).getChildren().remove(lastPosition - 1);
//					}
//					keyboardVisible = false;
//				} else {
//					listOfTextField.get(node.getId() + "LocalLanguage").requestFocus();
//					GridPane gridPane = prepareMainGridPaneForKeyboard();
//					gridPane.addColumn(1, keyboardNode);
//					((VBox)parentNode).getChildren().add(gridPane);
//					previousNode = parentNode;
//					keyboardVisible = true;
//					lastPosition = ((VBox)parentNode).getChildren().size();
//				}
//			}
//		} catch (RuntimeException runtimeException) {
//			LOGGER.error("REGISTRATION - SETTING FOCUS ON LOCAL FIELD FAILED", APPLICATION_NAME,
//					RegistrationConstants.APPLICATION_ID,
//					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
//		}
	}
}
