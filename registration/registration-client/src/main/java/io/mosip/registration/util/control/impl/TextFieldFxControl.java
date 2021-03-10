/**
 * 
 */
package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.controller.FXComponents;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.VirtualKeyboard;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
	
	private Node keyboardNode;
	
	private int lastPosition;
	
	private boolean keyboardVisible = false;
	
	private Node previousNode;
	
	private String previousLangCode;
	
	private Stage keyBoardStage;
	
	private FXComponents fxComponents;

	public TextFieldFxControl() {

		ApplicationContext applicationContext = Initialization.getApplicationContext();

		validation = applicationContext.getBean(Validations.class);
		fxComponents = applicationContext.getBean(FXComponents.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;
		create(uiSchemaDTO);
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

		this.node = simpleTypeVBox;
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Title label */
		Label fieldTitle = getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, simpleTypeVBox.getWidth());
		simpleTypeVBox.getChildren().add(fieldTitle);

		boolean isCreated = false;
		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {

			String label = uiSchemaDTO.getLabel().get(langCode);
			labelText = labelText.isEmpty() ? label : labelText.concat(RegistrationConstants.SLASH).concat(label);

			boolean isFieldReqd = this.uiSchemaDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE) ? true
					: !isCreated;
			if (isFieldReqd) {

				VBox vBox = new VBox();
				/** Text Field */
				HBox textFieldHBox = new HBox();
				TextField textField = getTextField(langCode, fieldName + langCode, label + mandatorySuffix,
						RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, simpleTypeVBox.getWidth(), false);
				textField.setPrefWidth(300);
				
				VirtualKeyboard keyBoard = new VirtualKeyboard(langCode);
				keyBoard.changeControlOfKeyboard(textField);
				
				ImageView translateImageView = new ImageView(new Image(getClass().getResourceAsStream("/images/translate.png")));
				translateImageView.setFitHeight(20);
				translateImageView.setFitWidth(20);
				
				ImageView keyBoardImgView = getKeyBoardImage();
				
				keyBoardImgView.setId(langCode);
				keyBoardImgView.visibleProperty().bind(textField.visibleProperty());
				keyBoardImgView.managedProperty().bind(textField.visibleProperty());

				if (keyBoardImgView != null) {
					keyBoardImgView.setOnMouseClicked((event) -> {
						setFocusOnField(event, keyBoard, langCode);
					});
				}
				
				HBox imagesHBox = new HBox();
				
				imagesHBox.getStyleClass().add(RegistrationConstants.ICONS_HBOX);
			
				imagesHBox.setPrefWidth(20);
				imagesHBox.setSpacing(5);
				imagesHBox.getChildren().addAll(translateImageView, keyBoardImgView);
				
				if (io.mosip.registration.context.ApplicationContext.getInstance().isLanguageRightToLeft(langCode)) {
					textFieldHBox.getChildren().addAll(imagesHBox, textField);
				} else {
					textFieldHBox.getChildren().addAll(textField, imagesHBox);
				}
							
				vBox.getChildren().add(textFieldHBox);

				/** Validation message (Invalid/wrong,,etc,.) */
				Label validationMessage = getLabel(fieldName + langCode + RegistrationConstants.MESSAGE, null,
						RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getWidth());
				validationMessage.setWrapText(false);
				vBox.getChildren().add(validationMessage);
				addValidationMessage(vBox, validationMessage, textField, langCode);
				
				changeNodeOrientation(textField, langCode);
				changeNodeOrientation(imagesHBox, langCode);

				simpleTypeVBox.getChildren().add(vBox);
				Validations.putIntoLabelMap(fieldName + langCode, uiSchemaDTO.getLabel().get(langCode));

				setListener(textField);

				isCreated = true;
			}
		}

		fieldTitle.setText(labelText + mandatorySuffix);

//		if (!this.uiSchemaDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
//
//			TextField textField = (TextField) getField(
//					uiSchemaDTO.getId() + getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
//			textField.setPromptText(fieldTitle.getText());
//		}

		return simpleTypeVBox;
	}

	private void addValidationMessage(VBox vBox, Label validationMessage, TextField textField, String langCode) {
		HBox validationHBox = new HBox();
		validationHBox.setSpacing(20);
		validationHBox.getChildren().add(validationMessage);
		validationHBox.setStyle("-fx-background-color:WHITE");
		vBox.getChildren().add(validationHBox);
	}

	private TextField getTextField(String langCode, String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(io.mosip.registration.context.ApplicationContext.getInstance()
				.getBundle(langCode, RegistrationConstants.LABELS).getString("language"));
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

				if (validation.validateTextField((Pane) getNode(), textField, uiSchemaDTO.getId(), true, langCode)) {

					FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField, uiSchemaDTO.getId());
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
	 * @param keyBoard2 
	 * @param langCode
	 *
	 */
	public void setFocusOnField(MouseEvent event, VirtualKeyboard keyBoard, String langCode) {
		try {
			Node node = (Node) event.getSource();
			node.requestFocus();
			Node parentNode = node.getParent().getParent().getParent();
			if (keyboardVisible) {
				if(previousNode != null) {
					((VBox) previousNode).getChildren().remove(lastPosition - 1);
				}
//				keyBoardStage.close();
				keyboardVisible = false;
				if (!langCode.equalsIgnoreCase(previousLangCode)) {
					openKeyBoard(keyBoard, langCode, parentNode);
				}
			} else {
//				keyboardNode = keyBoard.view();
//				keyboardNode.setVisible(true);
//				keyboardNode.setManaged(true);
//				getField(node.getId()).requestFocus();
//				openKeyBoardPopUp();
//				keyboardVisible = true;
				openKeyBoard(keyBoard, langCode, parentNode);
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - SETTING FOCUS ON LOCAL FIELD FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}
	
	private void openKeyBoard(VirtualKeyboard keyBoard, String langCode, Node parentNode) {
		keyboardNode = keyBoard.view();
		keyboardNode.setVisible(true);
		keyboardNode.setManaged(true);
		getField(node.getId()).requestFocus();
		GridPane gridPane = prepareMainGridPaneForKeyboard();
		gridPane.addColumn(1, keyboardNode);
		((VBox) parentNode).getChildren().add(gridPane);
		previousNode = parentNode;
		previousLangCode = langCode;
		keyboardVisible = true;
		lastPosition = ((VBox)parentNode).getChildren().size();
	}

	private GridPane prepareMainGridPaneForKeyboard() {
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(740);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(80);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(10);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
		return gridPane;
	}
	
	private void openKeyBoardPopUp() {
		try {
			keyBoardStage = new Stage();
			keyBoardStage.initModality(Modality.WINDOW_MODAL);
			keyBoardStage.initOwner(fxComponents.getStage());
//			stage.setX(400);
//			stage.setY(200);
			GridPane gridPane = prepareMainGridPaneForKeyboard();
			gridPane.addColumn(1, keyboardNode);
			Scene scene = new Scene(gridPane);
			keyBoardStage.setScene(scene);
			keyBoardStage.show();
		} catch (Exception exception) {
			LOGGER.error("REGISTRATION - OPENING - KEYBOARD - POPUP", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			validation.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP);
		}
	}
}
