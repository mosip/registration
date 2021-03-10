/**
 * 
 */
package io.mosip.registration.util.control.impl;

import java.util.List;
import java.util.ResourceBundle;

import org.springframework.context.ApplicationContext;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DateValidation;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author M1044402
 *
 */
public class DOBAgeFxControl extends FxControl {
	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DOBAgeFxControl.class);

	private static String loggerClassName = "DOB Age Control Type Class";
	private FXUtils fxUtils;

	private DateValidation dateValidation;

	public DOBAgeFxControl() {

		fxUtils = FXUtils.getInstance();
		ApplicationContext applicationContext = Initialization.getApplicationContext();

		this.dateValidation = applicationContext.getBean(DateValidation.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		VBox appLangDateVBox = create(uiSchemaDTO);

		HBox hBox = new HBox();
		hBox.setSpacing(30);
		hBox.getChildren().add(appLangDateVBox);
		HBox.setHgrow(appLangDateVBox, Priority.ALWAYS);

		this.node = hBox;

		setListener(hBox);

		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO) {

		HBox dobHBox = new HBox();
		dobHBox.setId(uiSchemaDTO.getId() + RegistrationConstants.HBOX);
		dobHBox.setSpacing(10);

		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		ResourceBundle resourceBundle = io.mosip.registration.context.ApplicationContext.getInstance()
				.getBundle(langCode, RegistrationConstants.LABELS);

		VBox ageVBox = new VBox();
		ageVBox.setPrefWidth(390);

		String labelText = "";
		for (String lang : getRegistrationDTo().getSelectedLanguagesByApplicant()) {

			String label = uiSchemaDTO.getLabel().get(lang);
			labelText = labelText.isEmpty() ? labelText : labelText + RegistrationConstants.SLASH;
			labelText += label;
		}

		/** DOB Label */
		ageVBox.getChildren().add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL,
				labelText + mandatorySuffix, RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, ageVBox.getWidth()));

		/** Add Date */
		dobHBox.getChildren().add(addDateTextField(uiSchemaDTO, RegistrationConstants.DD,
				resourceBundle.getString(RegistrationConstants.DD)));
		/** Add Month */
		dobHBox.getChildren().add(addDateTextField(uiSchemaDTO, RegistrationConstants.MM,
				resourceBundle.getString(RegistrationConstants.MM)));
		/** Add Year */
		dobHBox.getChildren().add(addDateTextField(uiSchemaDTO, RegistrationConstants.YYYY,
				resourceBundle.getString(RegistrationConstants.YYYY)));

//		/** OR Label */
//		dobHBox.getChildren()
//				.add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL,
//						resourceBundle.getString("ageOrDOBField"), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
//						ageVBox.getWidth()));

		/** Add Age Field */
		dobHBox.getChildren().add(addDateTextField(uiSchemaDTO, RegistrationConstants.AGE_FIELD,
				resourceBundle.getString(RegistrationConstants.AGE_FIELD)));

//		/** YEARS Label */
//		dobHBox.getChildren()
//				.add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL,
//						"YEARS, RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
//						ageVBox.getWidth()));
		ageVBox.getChildren().add(dobHBox);

		/** Validation message (Invalid/wrong,,etc,.) */
		ageVBox.getChildren().add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, ageVBox.getPrefWidth()));

		dobHBox.prefWidthProperty().bind(ageVBox.widthProperty());

		changeNodeOrientation(ageVBox, langCode);
		return ageVBox;
	}

	private VBox addDateTextField(UiSchemaDTO uiSchemaDTO, String dd, String text) {

		VBox dateVBox = new VBox();
		dateVBox.setId(uiSchemaDTO.getId() + dd + RegistrationConstants.VBOX);

		double prefWidth = dateVBox.getPrefWidth();

		/** DOB Label */
		dateVBox.getChildren().add(getLabel(uiSchemaDTO.getId() + dd + RegistrationConstants.LABEL, text,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth));

		/** DOB Text Field */
		dateVBox.getChildren().add(getTextField(uiSchemaDTO.getId() + dd + RegistrationConstants.TEXT_FIELD, text,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false));

		return dateVBox;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {

		TextField dd = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

		getRegistrationDTo().setDateField(uiSchemaDTO.getId(), dd.getText(), mm.getText(), yyyy.getText());
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid(Node node) {
		TextField dd = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);
		return dd != null && !dd.getText().isEmpty() && mm != null && !mm.getText().isEmpty() && yyyy != null
				&& !yyyy.getText().isEmpty();
	}

	@Override
	public void setListener(Node node) {

		addListener(
				(TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.DD);
		addListener(
				(TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.MM);
		addListener(
				(TextField) getField(
						uiSchemaDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.YYYY);
		addListener(
				(TextField) getField(
						uiSchemaDTO.getId() + RegistrationConstants.AGE_FIELD + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.AGE_FIELD);

	}

	private void addListener(TextField textField, String dateTyep) {
		textField.textProperty().addListener((ob, ov, nv) -> {

			fxUtils.toggleUIField((Pane) node,
					textField.getId().replaceAll(RegistrationConstants.TEXT_FIELD, "") + RegistrationConstants.LABEL,
					!textField.getText().isEmpty());
			if (!dateValidation.isNewValueValid(nv, dateTyep)) {
				textField.setText(ov);
			}
			boolean isValid = RegistrationConstants.AGE_FIELD.equalsIgnoreCase(dateTyep)
					? dateValidation.validateAge((Pane) node, uiSchemaDTO.getId())
					: dateValidation.validateDate((Pane) node, uiSchemaDTO.getId());
			if (isValid) {
				setData(null);
				refreshFields();
			}
		});
	}

	@Override
	public void fillData(Object data) {
		// TODO Parse and set the date
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

	@Override
	public void selectAndSet(Object data) {

		if (data != null && data instanceof String) {

			String[] dobArray = ((String) data).split("/");

			TextField yyyy = ((TextField) getField(
					this.uiSchemaDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD));

			TextField mm = ((TextField) getField(
					this.uiSchemaDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD));
			TextField dd = ((TextField) getField(
					this.uiSchemaDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD));
			yyyy.setText(dobArray[0]);
			mm.setText(dobArray[1]);
			dd.setText(dobArray[2]);

		}

	}

}
