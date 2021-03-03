/**
 * 
 */
package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

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

//		resourceLoader = applicationContext.getBean(ResourceLoader.class);
		validation = applicationContext.getBean(Validations.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		VBox primaryLangVBox = create(uiSchemaDTO, "");

		HBox hBox = new HBox();
		hBox.getChildren().add(primaryLangVBox);
//		HBox.setHgrow(primaryLangVBox, Priority.ALWAYS);
//		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
//		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(),
//				primaryLangVBox);

		// TODO
//		1. Label with slash appended with languages
//		2. create textbox with validation message for all languages
//		3. If type is not a simple type then create only one textbox with validation message

//		this.node = hBox;
//		if (demographicDetailController.isLocalLanguageAvailable()
//				&& !demographicDetailController.isAppLangAndLocalLangSame()) {
//			VBox secondaryLangVBox = create(uiSchemaDTO, RegistrationConstants.LOCAL_LANGUAGE);
//
////			HBox.setHgrow(secondaryLangVBox, Priority.ALWAYS);
//			hBox.getChildren().addAll(secondaryLangVBox);
//
//			nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getLocalLanguage(),
//					secondaryLangVBox);
//			setListener((TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE));
//		}

//		setNodeMap(nodeMap);
		setListener((TextField) getField(uiSchemaDTO.getId()));

//		controlMap.put(RegistrationConstants.FX_CONTROL, this.control);
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

			// 1.Iterate over the selected languages and get textfield
			// 2. prepare simpleDto
			// 3. add to list
			// 4. add to session

			/*
			 * Example List<SimpleDto> values = new ArrayList<SimpleDto>(); if (value !=
			 * null && !value.isEmpty()) values.add(new SimpleDto(applicationLanguage,
			 * value));
			 */

//			TextField appLangTextField = (TextField) getField(uiSchemaDTO.getId());
//			TextField localLangTextField = (TextField) getField(
//					uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);
//			String primaryLang = getAppLanguage();
//			String primaryVal = appLangTextField != null ? appLangTextField.getText() : null;
//			String localLanguage = getLocalLanguage();
//			String localVal = localLangTextField != null ? localLangTextField.getText() : null;
//
//			registrationDTO.addDemographicField(uiSchemaDTO.getId(), primaryLang, primaryVal, localLanguage, localVal);

		} else {
			registrationDTO.addDemographicField(uiSchemaDTO.getId(),
					((TextField) getField(uiSchemaDTO.getId())).getText());

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
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, prefWidth);
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
		if (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)) {
			Validations.putIntoLabelMap(fieldName + languageType,
					uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY));
		} else {
			Validations.putIntoLabelMap(fieldName + languageType,
					uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY));
		}

		changeNodeOrientation(simpleTypeVBox, languageType);

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

		boolean isValid;
		if (node == null) {
			LOGGER.warn(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Field not found in demographic screen");
			return false;
		}

		TextField field = (TextField) getField(uiSchemaDTO.getId());
		if (validation.validateTextField((Pane) getNode(), field, field.getId(), true)) {

			FXUtils.getInstance().setTextValidLabel((Pane) getNode(), field);
			isValid = true;
		} else {

			FXUtils.getInstance().showErrorLabel(field, (Pane) getNode());
			return false;
		}
		LOGGER.debug(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"validating text field secondary");

		TextField localField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);
		if (localField != null) {
			if (uiSchemaDTO.getType().equalsIgnoreCase("string")) {
				localField.setText(field.getText());
			}
			if (validation.validateTextField((Pane) getNode(), localField, field.getId(), true)) {

				FXUtils.getInstance().setTextValidLabel((Pane) getNode(), localField);
				isValid = true;
			} else {

				FXUtils.getInstance().showErrorLabel(localField, (Pane) getNode());
				return false;
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

		TextField appLangTextField = (TextField) getField(uiSchemaDTO.getId());
		TextField localLangTextField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);

		Map<String, String> dataMap = (Map<String, String>) data;
		if (appLangTextField != null) {
			appLangTextField.setText(dataMap.get(RegistrationConstants.PRIMARY));
		}
		if (localLangTextField != null) {
			localLangTextField.setText(dataMap.get(RegistrationConstants.SECONDARY));
		}

	}

	@Override
	public void selectAndSet(Object data) {

		TextField appLangTextField = (TextField) getField(uiSchemaDTO.getId());
		TextField localLangTextField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);

		if (data instanceof String) {

			appLangTextField.setText((String) data);

			if (localLangTextField != null) {

				localLangTextField.setText((String) data);

			}
		}
		if (data instanceof List) {

			List<SimpleDto> list = (List<SimpleDto>) data;

			for (SimpleDto simpleDto : list) {

				// TODO iterate through selected language list,, and set values for what has
				// been fetched from map

//				if (simpleDto.getLanguage().equalsIgnoreCase(
//						io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage())) {
//
//					appLangTextField.setText(simpleDto.getValue());
//				} else if (localLangTextField != null && simpleDto.getLanguage().equalsIgnoreCase(
//						io.mosip.registration.context.ApplicationContext.getInstance().getLocalLanguage())) {
//
//					localLangTextField.setText(simpleDto.getValue());
//				}
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
