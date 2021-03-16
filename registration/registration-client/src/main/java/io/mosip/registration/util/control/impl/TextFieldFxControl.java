/**
 * 
 */
package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.dto.mastersync.GenericDto;
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
			if (isValid()) {

				setData(null);

				// handling other handlers
				demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
						uiSchemaDTO.getChangeAction());

			} else {
				getRegistrationDTo().getDemographics().remove(this.uiSchemaDTO.getId());
			}
			// Group level visibility listeners
			refreshFields();
		});

	}

	private VBox create(UiSchemaDTO uiSchemaDTO) {
		String fieldName = uiSchemaDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();

		this.node = simpleTypeVBox;
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		/** Title label */
		Label fieldTitle = getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, simpleTypeVBox.getWidth());

		simpleTypeVBox.getChildren().add(fieldTitle);

		VBox vBox = new VBox();
		List<String> labels = new ArrayList<>();
		switch (this.uiSchemaDTO.getType()) {
			case RegistrationConstants.SIMPLE_TYPE :
				getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
					labels.add(this.uiSchemaDTO.getLabel().get(langCode));
					vBox.getChildren().add(createTextBox(langCode,true));
					vBox.getChildren().add(getLabel(uiSchemaDTO.getId() + langCode + RegistrationConstants.MESSAGE, null,
							RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
				});
				break;
			default:
				getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
							labels.add(this.uiSchemaDTO.getLabel().get(langCode));});
				vBox.getChildren().add(createTextBox(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),false));
				vBox.getChildren().add(getLabel(uiSchemaDTO.getId() +
								getRegistrationDTo().getSelectedLanguagesByApplicant().get(0) + RegistrationConstants.MESSAGE, null,
						RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
				break;
		}

		fieldTitle.setText(String.join(RegistrationConstants.SLASH, labels)	+ getMandatorySuffix(uiSchemaDTO));
		simpleTypeVBox.getChildren().add(vBox);
		return simpleTypeVBox;
	}

	private HBox createTextBox(String langCode, boolean isSimpleType) {
		HBox textFieldHBox = new HBox();
		TextField textField = getTextField(langCode, uiSchemaDTO.getId() + langCode, false);
		textField.setMinWidth(400);
		textFieldHBox.getChildren().add(textField);

		if(isSimpleType) {
			HBox imagesHBox = new HBox();
			imagesHBox.getStyleClass().add(RegistrationConstants.ICONS_HBOX);
			imagesHBox.setPrefWidth(20);
			imagesHBox.setSpacing(5);

			if(this.uiSchemaDTO.isTransliterate()) {
				ImageView translateImageView = new ImageView(new Image(getClass().getResourceAsStream("/images/translate.png")));
				translateImageView.setFitHeight(20);
				translateImageView.setFitWidth(20);
				imagesHBox.getChildren().add(translateImageView);
			}

			VirtualKeyboard keyBoard = new VirtualKeyboard(langCode);
			keyBoard.changeControlOfKeyboard(textField);
			ImageView keyBoardImgView = getKeyBoardImage();
			keyBoardImgView.setId(langCode);
			keyBoardImgView.visibleProperty().bind(textField.visibleProperty());
			keyBoardImgView.managedProperty().bind(textField.visibleProperty());

			if (keyBoardImgView != null) {
				keyBoardImgView.setOnMouseClicked((event) -> {
					setFocusOnField(event, keyBoard, langCode);
				});
			}
			imagesHBox.getChildren().add(keyBoardImgView);
			textFieldHBox.getChildren().add(imagesHBox);
		}

		setListener(textField);
		changeNodeOrientation(textFieldHBox, langCode);
		Validations.putIntoLabelMap(uiSchemaDTO.getId() + langCode, uiSchemaDTO.getLabel().get(langCode));
		return textFieldHBox;
	}


	private TextField getTextField(String langCode, String id, boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(io.mosip.registration.context.ApplicationContext.getInstance()
				.getBundle(langCode, RegistrationConstants.LABELS).getString("language"));
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
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
	public boolean isValid() {
		boolean isValid = true;

		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
			TextField textField = (TextField) getField(uiSchemaDTO.getId() + langCode);
			if (textField == null)  {
				isValid = false;
				break;
			}

			if (validation.validateTextField((Pane) getNode(), textField, uiSchemaDTO.getId(), true, langCode)) {
				FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField, uiSchemaDTO.getId());
			} else {
				FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
				isValid = false;
				break;
			}

			if(!this.uiSchemaDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
				break; //not required to iterate further
			}
		}
		return isValid;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}


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
	 * Setting the focus to specific fields when keyboard loads
	 * @param event
	 * @param keyBoard
	 * @param langCode
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
