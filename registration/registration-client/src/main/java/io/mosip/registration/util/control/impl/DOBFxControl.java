package io.mosip.registration.util.control.impl;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.springframework.context.ApplicationContext;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DateValidation;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DOBFxControl extends FxControl {
	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DOBAgeFxControl.class);

	private static String loggerClassName = "DOB Age Control Type Class";

	private Validations validation;
	private DemographicChangeActionHandler demographicChangeActionHandler;

	private io.mosip.registration.context.ApplicationContext regApplicationContext;
	private ResourceBundle localLabelBundle;

	private ResourceBundle applicationLabelBundle;

	private FXUtils fxUtils;

	private DateValidation dateValidation;

	public DOBFxControl() {

		fxUtils = FXUtils.getInstance();
		regApplicationContext = io.mosip.registration.context.ApplicationContext.getInstance();
//		localLabelBundle = regApplicationContext.localLanguageBundle();
		applicationLabelBundle = regApplicationContext.getApplicationLanguageLabelBundle();
		ApplicationContext applicationContext = Initialization.getApplicationContext();

//		resourceLoader = applicationContext.getBean(ResourceLoader.class);
		this.validation = applicationContext.getBean(Validations.class);
		this.demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		this.dateValidation = applicationContext.getBean(DateValidation.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		VBox appLangDateVBox = create(uiSchemaDTO, "");

		HBox hBox = new HBox();
		hBox.setSpacing(30);
		hBox.getChildren().add(appLangDateVBox);
		HBox.setHgrow(appLangDateVBox, Priority.ALWAYS);

		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(),
				appLangDateVBox);

//		if (demographicDetailController.isLocalLanguageAvailable()
//				&& !demographicDetailController.isAppLangAndLocalLangSame()) {
//			VBox secondaryLangVBox = create(uiSchemaDTO, RegistrationConstants.LOCAL_LANGUAGE);
//
//			HBox.setHgrow(secondaryLangVBox, Priority.ALWAYS);
//			hBox.getChildren().add(secondaryLangVBox);
//
//			nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getLocalLanguage(),
//					appLangDateVBox);
//		}
//
//		setNodeMap(nodeMap);

		this.node = hBox;

		setListener(hBox);

		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String languageType) {

		HBox dobHBox = new HBox();
		dobHBox.setId(uiSchemaDTO.getId() + RegistrationConstants.HBOX);
		dobHBox.setSpacing(10);

		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Add Date */
		dobHBox.getChildren()
				.add(addDateTextField(uiSchemaDTO, RegistrationConstants.DD, languageType, mandatorySuffix));
		/** Add Month */
		dobHBox.getChildren()
				.add(addDateTextField(uiSchemaDTO, RegistrationConstants.MM, languageType, mandatorySuffix));
		/** Add Year */
		dobHBox.getChildren()
				.add(addDateTextField(uiSchemaDTO, RegistrationConstants.YYYY, languageType, mandatorySuffix));

		VBox ageVBox = new VBox();
		ageVBox.setPrefWidth(390);
		ageVBox.getChildren().add(dobHBox);
		dobHBox.prefWidthProperty().bind(ageVBox.widthProperty());

		/** Validation message (Invalid/wrong,,etc,.) */
		ageVBox.getChildren().add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, ageVBox.getPrefWidth()));

		changeNodeOrientation(ageVBox, languageType);
		return ageVBox;
	}

	private VBox addDateTextField(UiSchemaDTO uiSchemaDTO, String dd, String languageType, String mandatorySuffix) {

		VBox dateVBox = new VBox();
		dateVBox.setId(uiSchemaDTO.getId() + dd + languageType + RegistrationConstants.VBOX);

		double prefWidth = dateVBox.getPrefWidth();
		boolean localLanguage = languageType.equals(RegistrationConstants.LOCAL_LANGUAGE);

		/** DOB Label */
		dateVBox.getChildren()
				.add(getLabel(uiSchemaDTO.getId() + dd + RegistrationConstants.LABEL,
						localLanguage ? localLabelBundle.getString(dd)
								: applicationLabelBundle.getString(dd) + mandatorySuffix,
						RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, prefWidth));

		/** DOB Text Field */
		dateVBox.getChildren()
				.add(getTextField(uiSchemaDTO.getId() + dd + RegistrationConstants.TEXT_FIELD + languageType,
						(localLanguage ? localLabelBundle.getString(dd) : applicationLabelBundle.getString(dd))
								+ mandatorySuffix,
						RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, localLanguage));

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public UiSchemaDTO getUiSchemaDTO() {
		// TODO Auto-generated method stub
		return null;
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

	}

	private void addListener(TextField textField, String dateTyep) {
		textField.textProperty().addListener((ob, ov, nv) -> {
			fxUtils.showLabel((Pane) node, textField);
			if (!dateValidation.isNewValueValid(nv, dateTyep)) {
				textField.setText(ov);
			}
			boolean isValid = dateValidation.validateDate((Pane) node, uiSchemaDTO.getId());
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
