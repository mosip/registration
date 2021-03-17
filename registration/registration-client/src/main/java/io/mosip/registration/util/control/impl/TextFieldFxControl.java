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
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.kernel.transliteration.icu4j.impl.TransliterationImpl;
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
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
	
	private Transliteration<String> transliteration;
	
	private Node keyboardNode;
	
	private boolean keyboardVisible = false;
	
	private static double xPosition;
	private static double yPosition;
	
	private String previousLangCode;
	
	private Stage keyBoardStage;
	
	private FXComponents fxComponents;

	public TextFieldFxControl() {
		ApplicationContext applicationContext = Initialization.getApplicationContext();
		validation = applicationContext.getBean(Validations.class);
		fxComponents = applicationContext.getBean(FXComponents.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		transliteration = (Transliteration<String>) applicationContext.getBean(TransliterationImpl.class);
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
			if (uiSchemaDTO.isTransliterate()) {
				transliterate(textField, textField.getId().substring(textField.getId().length() - RegistrationConstants.LANGCODE_LENGTH, textField.getId().length()));
			}
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
			imagesHBox.setPrefWidth(10);

			VirtualKeyboard keyBoard = new VirtualKeyboard(langCode);
			keyBoard.changeControlOfKeyboard(textField);
			
			ImageView keyBoardImgView = getKeyBoardImage();
			keyBoardImgView.setId(langCode);
			keyBoardImgView.visibleProperty().bind(textField.visibleProperty());
			keyBoardImgView.managedProperty().bind(textField.visibleProperty());

			if (keyBoardImgView != null) {
				keyBoardImgView.setOnMouseClicked((event) -> {
					setFocusOnField(event, keyBoard, langCode, textField);
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
	public boolean isEmpty() {
		return false;
	}

	private void transliterate(TextField textField, String langCode) {
		for (String langCodeToBeTransliterated : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
			if (!langCodeToBeTransliterated.equalsIgnoreCase(langCode)) {
				TextField textFieldToBeTransliterated = (TextField) getField(uiSchemaDTO.getId() + langCodeToBeTransliterated);
				if (textFieldToBeTransliterated != null)  {
					try {
						textFieldToBeTransliterated.setText(transliteration.transliterate(langCode,
								langCodeToBeTransliterated, textField.getText()));
					} catch (RuntimeException runtimeException) {
						LOGGER.error(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
								"Exception occured while transliterating secondary language for field : "
										+ textField.getId()  + " due to >>>> " + runtimeException.getMessage());
					}
				}
			}
		}
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
	 *
	 * Setting the focus to specific fields when keyboard loads
	 * @param event
	 * @param keyBoard
	 * @param langCode
	 * @param textField 
	 *
	 */
	public void setFocusOnField(MouseEvent event, VirtualKeyboard keyBoard, String langCode, TextField textField) {
		try {
			Node node = (Node) event.getSource();
			node.requestFocus();
			Node parentNode = node.getParent().getParent().getParent();
			if (keyboardVisible) {
				keyBoardStage.close();
				keyboardVisible = false;
				if (!langCode.equalsIgnoreCase(previousLangCode)) {
					openKeyBoard(keyBoard, langCode, textField, parentNode);
				}
			} else {
				openKeyBoard(keyBoard, langCode, textField, parentNode);
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}
	
	private void openKeyBoard(VirtualKeyboard keyBoard, String langCode, TextField textField, Node parentNode) {
		if (keyBoardStage != null)  {
			keyBoardStage.close();
		}
		keyboardNode = keyBoard.view();
		keyBoard.setParentStage(fxComponents.getStage());
		keyboardNode.setVisible(true);
		keyboardNode.setManaged(true);
		getField(textField.getId()).requestFocus();
		openKeyBoardPopUp();
		previousLangCode = langCode;
		keyboardVisible = true;
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
			keyBoardStage.setAlwaysOnTop(true);
			keyBoardStage.initStyle(StageStyle.UNDECORATED);
			keyBoardStage.setX(300);
			keyBoardStage.setY(500);
			GridPane gridPane = prepareMainGridPaneForKeyboard();
			gridPane.addColumn(1, keyboardNode);
			Scene scene = new Scene(gridPane);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(validation.getCssName()).toExternalForm());
			gridPane.getStyleClass().add(RegistrationConstants.KEYBOARD_PANE);
			keyBoardStage.setScene(scene);
			makeDraggable(keyBoardStage, gridPane);
			keyBoardStage.show();
		} catch (Exception exception) {
			LOGGER.error(loggerClassName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			validation.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP);
		}
	}
	
	private static void makeDraggable(final Stage stage, final Node node) {
	    node.setOnMousePressed(mouseEvent -> {
	    	// record a distance for the drag and drop operation.
	    	xPosition = stage.getX() - mouseEvent.getScreenX();
	    	yPosition = stage.getY() - mouseEvent.getScreenY();
	    	node.setCursor(Cursor.MOVE);
	    });
	    node.setOnMouseReleased(mouseEvent -> node.setCursor(Cursor.HAND));
	    node.setOnMouseDragged(mouseEvent -> {
	    	stage.setX(mouseEvent.getScreenX() + xPosition);
	    	stage.setY(mouseEvent.getScreenY() + yPosition);
	    });
	    node.setOnMouseEntered(mouseEvent -> {
	    	if (!mouseEvent.isPrimaryButtonDown()) {
	    		node.setCursor(Cursor.HAND);
	    	}
	    });
	    node.setOnMouseExited(mouseEvent -> {
	    	if (!mouseEvent.isPrimaryButtonDown()) {
	    		node.setCursor(Cursor.DEFAULT);
	    	}
	    });
	}
}
