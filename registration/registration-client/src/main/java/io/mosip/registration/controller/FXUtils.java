package io.mosip.registration.controller;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.mastersync.GenericDto;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

/**
 * Class for JavaFx utilities operation
 * 
 * @author Taleev.Aalam
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
public class FXUtils {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RegistrationController.class);
	private Transliteration<String> transliteration;
	private static FXUtils fxUtils = null;

	private FXUtils() {

	}

	/**
	 * Method to get the instance of {@link FXUtils}. If instance does not exists,
	 * instantiates a new object of {@link FXUtils} and returns the same
	 * 
	 * @return the instance of the {@link FXUtils}
	 */
	public static FXUtils getInstance() {
		if (fxUtils == null) {
			fxUtils = new FXUtils();
		}
		return fxUtils;
	}

	/**
	 * Listener to change the style when field is selected for.
	 *
	 * @param parentOrGuardian the {@link CheckBox}
	 * @param biometrics
	 */
	public void listenOnSelectedCheckBoxParentOrGuardian(CheckBox parentOrGuardian, CheckBox biometrics) {

		parentOrGuardian.selectedProperty().addListener((obsValue, oldValue, newValue) -> {
			if (newValue) {
				parentOrGuardian.getStyleClass().remove("updateUinCheckBox");
				parentOrGuardian.getStyleClass().add("updateUinCheckBoxSelected");
				biometrics.getStyleClass().remove("updateUinCheckBoxSelected");
				biometrics.getStyleClass().add("updateUinCheckBox");
				biometrics.setSelected(false);
			} else {
				parentOrGuardian.getStyleClass().remove("updateUinCheckBoxSelected");
				parentOrGuardian.getStyleClass().add("updateUinCheckBox");
			}
		});
		biometrics.selectedProperty().addListener((obsValue, oldValue, newValue) -> {
			if (newValue) {
				biometrics.getStyleClass().remove("updateUinCheckBox");
				biometrics.getStyleClass().add("updateUinCheckBoxSelected");
				parentOrGuardian.getStyleClass().remove("updateUinCheckBoxSelected");
				parentOrGuardian.getStyleClass().add("updateUinCheckBox");
				parentOrGuardian.setSelected(false);
			} else {
				biometrics.getStyleClass().remove("updateUinCheckBoxSelected");
				biometrics.getStyleClass().add("updateUinCheckBox");
			}
		});
	}

	/**
	 * Listener to change the style when field is selected for.
	 *
	 * @param field the {@link CheckBox}
	 */
	public void listenOnSelectedCheckBox(CheckBox field) {

		field.selectedProperty().addListener((obsValue, oldValue, newValue) -> {
			if (newValue) {
				field.getStyleClass().remove("updateUinCheckBox");
				field.getStyleClass().add("updateUinCheckBoxSelected");
			} else {
				field.getStyleClass().remove("updateUinCheckBoxSelected");
				field.getStyleClass().add("updateUinCheckBox");
			}
		});
	}

	/**
	 * Sets the instance of {@link Transliteration}.
	 *
	 * @param transliteration the transliteration to set
	 */
	public void setTransliteration(Transliteration<String> transliteration) {
		this.transliteration = transliteration;
	}

	/**
	 * Validates the value of field during on-type event. If validation fails,
	 * retain the previous value and display error message.
	 *
	 * @param parentPane the {@link Pane} in which {@link TextField} is present
	 * @param field      the {@link TextField} to be validated
	 * @param validation the instance of {@link Validations}
	 */
	public void validateOnType(Pane parentPane, TextField field, Validations validation) {
		field.textProperty().addListener((obsValue, oldValue, newValue) -> {
			if (!isInputTextValid(parentPane, field, field.getId().concat(RegistrationConstants.ON_TYPE), validation)) {
				field.setText(oldValue);
			} else {
				field.getStyleClass().removeIf((s) -> {
					return s.equals("demoGraphicTextFieldError");
				});
				field.getStyleClass().add("demoGraphicTextField");
			}
		});
	}

	/**
	 * Validates the value of the {@link TextField}
	 * 
	 * @param parentPane the {@link Pane} containing the {@link TextField}
	 * @param field      the {@link TextField} value to be validated
	 * @param fieldId    the id of the {@link TextField} to be validated
	 * @param validation the instance of {@link Validations}
	 * @return <code>true</code> if input is valid, else <code>false</code>
	 */
	private boolean isInputTextValid(Pane parentPane, TextField field, String fieldId, Validations validation) {
		return validation.validateTextField(parentPane, field, fieldId, true);
	}

	/**
	 * Populate local or secondary language combo box based on the application or
	 * primary language. The value in the local or secondary language
	 * {@link ComboBox} will be selected based on the code of the value selected in
	 * application or secondary language {@link ComboBox}.
	 *
	 * @param parentPane       the {@link Pane} in which {@link TextField} is
	 *                         present
	 * @param applicationField the {@link ComboBox} in application or primary
	 *                         language
	 * @param localField       the {@link ComboBox} in local or secondary language
	 */
	public void populateLocalComboBox(Pane parentPane, ComboBox<?> applicationField, ComboBox<?> localField) {
		applicationField.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			selectComboBoxValueByCode(localField, applicationField.getValue(), applicationField);

			toggleUIField(parentPane, applicationField.getId() + RegistrationConstants.LABEL, true);

			toggleUIField(parentPane, applicationField.getId() + RegistrationConstants.MESSAGE, false);
			if (!isAppLangAndLocalLangSame()) {

				toggleUIField(parentPane, localField.getId() + RegistrationConstants.LABEL, true);

				toggleUIField(parentPane, localField.getId() + RegistrationConstants.MESSAGE, false);
			}
		});
	}

	/**
	 * Toggle the visibility of the UI field based on the input visibility
	 * 
	 * @param parentPane the {@link Pane} containing the UI Field
	 * @param uiFieldId  the id of the UI Field for which visibility has to be
	 *                   toggled
	 * @param visibility the visibility property value
	 */
    public void toggleUIField(Pane parentPane, String uiFieldId, boolean visibility) {
		try {
			((Label) parentPane.lookup(RegistrationConstants.HASH.concat(uiFieldId))).setVisible(visibility);
		} catch (RuntimeException runtimeException) {
			LOGGER.info("ID NOT FOUND", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage());
		}
	}

	/**
	 * Validates the value of field during on-type event. If validation is
	 * successful, populate the local or secondary language field (transliterate, if
	 * required) if present. Else retain the previous value and display error
	 * message.
	 *
	 * @param parentPane          the {@link Pane} in which {@link TextField} is
	 *                            present
	 * @param field               the {@link TextField} to be validated
	 * @param validation          the instance of {@link Validations}
	 * @param haveToTransliterate the flag to know whether the field value has to be
	 *                            transliterated
	 */
	public void validateOnType(Pane parentPane, TextField field, Validations validation, boolean haveToTransliterate) {

		focusAction(parentPane, field);
		field.textProperty().addListener((obsValue, oldValue, newValue) -> {
			showLabel(parentPane, field);
			if (isInputTextValid(parentPane, field, field.getId().concat(RegistrationConstants.ON_TYPE), validation)) {
				setTextValidLabel(parentPane, field);
			} else {
				if (!field.getText().equals(RegistrationConstants.EMPTY))
					field.setText(oldValue);
			}
		});

	}

	public void setTextValidLabel(Pane parentPane, TextField field) {
		field.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextField");
		});
		field.getStyleClass().add("demoGraphicTextFieldOnType");
		if (field.isFocused()) {
			Label fieldLabel = (Label) parentPane.lookup("#" + field.getId() + "Label");
			fieldLabel.getStyleClass().add("demoGraphicFieldLabelOnType");
			fieldLabel.getStyleClass().remove("demoGraphicFieldLabel");

			if (field.getId().equals("ageField")) {
				((RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA))
						.setAgeCalculatedByDOB(false);
			}
		}
		hideErrorMessageLabel(parentPane, field);
	}

	/**
	 * Validates the value of field during focus-out event. If validation is
	 * successful, populate the local or secondary language field (transliterate, if
	 * required) if present. Else retain the previous value and display error
	 * message.
	 *
	 * @param parentPane          the {@link Pane} in which {@link TextField} is
	 *                            present
	 * @param field               the {@link TextField} to be validated
	 * @param validation          the instance of {@link Validations}
	 * @param haveToTransliterate the flag to know whether the field value has to be
	 *                            transliterated
	 */
	public void validateOnFocusOut(Pane parentPane, TextField field, Validations validation,
			boolean haveToTransliterate) {

		field.focusedProperty().addListener((obsValue, oldValue, newValue) -> {
			validateOnFocusOut(parentPane, field, validation, haveToTransliterate, oldValue);
		});

		if (parentPane != null && field != null) {
			focusedAction(parentPane, field);
		}

		validateLabelFocusOut(parentPane, field);

	}

	public void populateLocalFieldWithFocus(Pane parentPane, TextField field, TextField localField,
			boolean haveToTransliterate, Validations validation) {

		field.focusedProperty().addListener((obsValue, oldValue, newValue) -> {
			if (!field.isFocused()) {

				if (isInputTextValid(parentPane, field, field.getId() + "_ontype", validation)) {

					if (localField != null) {
						if (haveToTransliterate) {
							try {
								localField
										.setText(transliteration.transliterate(ApplicationContext.applicationLanguage(),
												ApplicationContext.localLanguage(), field.getText()));
							} catch (RuntimeException runtimeException) {
								LOGGER.error("REGISTRATION - TRANSLITERATION ERROR ", APPLICATION_NAME,
										RegistrationConstants.APPLICATION_ID, runtimeException.getMessage());
								localField.setText(field.getText());
							}
						} else {
							localField.setText(field.getText());
						}
					}
				} else {
					showErrorLabel(field, parentPane);
				}

				Label fieldLabel = (Label) parentPane.lookup("#" + field.getId() + "Label");
				fieldLabel.getStyleClass().removeIf((s) -> {
					return s.equals("demoGraphicFieldLabelOnType");
				});
				fieldLabel.getStyleClass().add("demoGraphicFieldLabel");
			}
		});

	}

	public void showErrorLabel(TextField field, Pane parentPane) {
		field.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextFieldOnType");
		});
		field.getStyleClass().add("demoGraphicTextFieldFocused");
		toggleUIField(parentPane, field.getId() + RegistrationConstants.MESSAGE, true);
	}

	public void focusedAction(Pane parentPane, TextField field) {

		if (parentPane != null && field != null) {
			field.focusedProperty().addListener((obsValue, oldValue, newValue) -> {
				if (!field.isFocused()) {
					Label fieldLabel = (Label) parentPane.lookup("#" + field.getId() + "Label");
					fieldLabel.getStyleClass().removeIf((s) -> {
						return s.equals("demoGraphicFieldLabelOnType");
					});
					fieldLabel.getStyleClass().add("demoGraphicFieldLabel");
				}
			});
		}
	}

	public void validateLabelFocusOut(Pane parentPane, TextField field) {

		if (field != null) {
			onTypeFocusUnfocusListener(parentPane, field);
			onTypeFocusUnfocusForLabel(parentPane, field);
		}
	}

	public void validateOnFocusOut(Pane parentPane, TextField field, Validations validation,
			boolean haveToTransliterate, Boolean oldValue) {
		if (oldValue) {
			if (isInputTextValid(parentPane, field, field.getId() + "_ontype", validation)) {
				field.getStyleClass().removeIf((s) -> {
					return s.equals("demoGraphicTextFieldFocused");
				});
				field.getStyleClass().add("demoGraphicTextField");
				hideLabel(parentPane, field);
				hideErrorMessageLabel(parentPane, field);
			} else {
				field.getStyleClass().removeIf((s) -> {
					return s.equals("demoGraphicTextFieldOnType");
				});
				field.getStyleClass().add("demoGraphicTextFieldFocused");
				toggleUIField(parentPane, field.getId() + RegistrationConstants.MESSAGE, true);
			}
			Label fieldLabel = (Label) parentPane.lookup("#" + field.getId() + "Label");
			fieldLabel.getStyleClass().removeIf((s) -> {
				return s.equals("demoGraphicFieldLabelOnType");
			});
			fieldLabel.getStyleClass().add("demoGraphicFieldLabel");
		} else {
			showLabel(parentPane, field);
		}
	}

	/**
	 * Display the secondary or local language's {@link Label}, {@link TextField}
	 * Prompt Text and Error Message {@link Label} based on the {@link TextField}
	 * change event.
	 * 
	 * @param parentPane the {@link Pane} in which secondary or local language's
	 *                   Label, Field and Error Message Label is present
	 * @param field      the secondary or local {@link TextField}
	 */
	public void onTypeFocusUnfocusListener(Pane parentPane, TextField field) {

		if (field != null) {
			field.textProperty().addListener((obsValue, oldValue, newValue) -> {
				field.getStyleClass().removeIf((s) -> {
					return s.equals("demoGraphicField");
				});
				field.getStyleClass().add("demoGraphicTextFieldOnType");
				if (field.isFocused()) {
					Label fieldLabel = (Label) parentPane.lookup("#" + field.getId() + "Label");
					fieldLabel.getStyleClass().clear();
					fieldLabel.getStyleClass().add("demoGraphicFieldLabelOnType");
				}
				if (newValue != null) {
					if (newValue.isEmpty()) {
						hideLabel(parentPane, field);
					} else {
						hideErrorMessageLabel(parentPane, field);
						showLabel(parentPane, field);
					}
				}
			});

		}
	}

	/**
	 * Display the {@link Label}, {@link TextField}
	 * 
	 * @param parentPane the {@link Pane} in which secondary or local language's
	 *                   Label, Field and Error Message Label is present
	 * @param field      the secondary or local {@link TextField}
	 */
	public void onTypeFocusUnfocusForLabel(Pane parentPane, TextField field) {
		if (field != null) {
			field.textProperty().addListener((obsValue, oldValue, newValue) -> {
				if (newValue != null) {
					if (newValue.isEmpty()) {
						hideLabel(parentPane, field);
					} else {
						showLabel(parentPane, field);
					}
				}
			});
		}
	}

	/**
	 * Display the secondary or local language's Label, Field's Prompt Text and
	 * Error Message Label based on the focus in or focus out event.
	 * 
	 * @param parentPane the {@link Pane} in which secondary or local language's
	 *                   Label, Field and Error Message Label is present
	 * @param field      the primary or application {@link TextField}
	 * @param localField the secondary or local {@link TextField}
	 */
	public void focusUnfocusListener(Pane parentPane, TextField field, TextField localField) {
		focusAction(parentPane, field);

		if (!isAppLangAndLocalLangSame()) {
			focusAction(parentPane, localField);
		}
	}

	public void focusAction(Pane parentPane, TextField field) {
		if (field != null) {
			field.focusedProperty().addListener((obsValue, oldValue, newValue) -> {
				if (newValue) {
					showLabel(parentPane, field);
				} else {
					hideLabel(parentPane, field);
				}
			});
		}
	}

	/**
	 * If the value of field is empty, the label will be hidden and prompt text will
	 * be displayed for the corresponding field
	 * 
	 * @param parentPane the {@link Pane} containing the {@link TextField}
	 * @param field      the {@link TextField}
	 */
	public void hideLabel(Pane parentPane, TextField field) {
		if (field == null || field.getText().isEmpty()) {
			try {
				Label label = ((Label) parentPane
						.lookup(RegistrationConstants.HASH + field.getId() + RegistrationConstants.LABEL));
				label.setVisible(false);
				((TextField) parentPane.lookup(RegistrationConstants.HASH + field.getId()))
						.setPromptText(label.getText());
			} catch (RuntimeException runtimeException) {
				LOGGER.info("ID NOT FOUND", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						runtimeException.getMessage());
			}
		}
	}

	/**
	 * Show the {@link Label} and remove Prompt Text corresponding to the input
	 * {@link TextField}
	 * 
	 * @param parentPane the {@link Pane} containing the {@link TextField}
	 * @param field      the {@link TextField} for which Prompt Text has to be
	 *                   removed and show its corresponding {@link Label}
	 */
	public void showLabel(Pane parentPane, TextField field) {
		toggleUIField(parentPane, field.getId() + RegistrationConstants.LABEL, true);
		((TextField) parentPane.lookup(RegistrationConstants.HASH + field.getId())).setPromptText(null);
	}

	/**
	 * Hide the {@link Label} corresponding to the input {@link TextField}
	 * 
	 * @param parentPane the {@link Pane} containing the {@link TextField}
	 * @param field      the {@link TextField} whose {@link Label} has to be removed
	 *                   or hidden
	 */
	public void hideErrorMessageLabel(Pane parentPane, TextField field) {
		String[] parts = field.getId().split("__");
		if (parts.length > 1 && parts[1].matches(RegistrationConstants.DTAE_MONTH_YEAR_REGEX)) {
			toggleUIField(parentPane, parts[0]+"__"+RegistrationConstants.DOB_MESSAGE, false);
		} else {
			toggleUIField(parentPane, field.getId() + RegistrationConstants.MESSAGE, false);
		}
	}

	/**
	 * Adds the Listener for text change event
	 * 
	 * @param field           the {@link TextField} for which listener has to be set
	 * @param fieldToPopulate the {@link TextField} whose value has to be changed
	 *                        based on the input field
	 * @param regex           the regular expression pattern to validate the input
	 *                        of field
	 */
	public void dobListener(TextField field, TextField fieldToPopulate, TextField localFieldToPopulate, String regex) {
		field.textProperty().addListener((obsValue, oldValue, newValue) -> {
			if (field.getText().matches(regex)) {
				int year = Integer.parseInt(field.getText());
				int age = LocalDate.now().getYear() - year;
				if (age > 0) {
					fieldToPopulate.setText(RegistrationConstants.EMPTY + age);
					if (!isAppLangAndLocalLangSame()) {

						localFieldToPopulate.setText(RegistrationConstants.EMPTY + age);
					}
				} else {
					fieldToPopulate.setText("1");
					if (!isAppLangAndLocalLangSame()) {

						localFieldToPopulate.setText("1");
					}
				}
			}
		});
	}

	/**
	 * To display the selected date in the date picker in specific
	 * format("dd-mm-yyyy").
	 *
	 * @param ageDatePicker the age date picker
	 */
	public void dateFormatter(DatePicker ageDatePicker) {
		try {
			LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Validating the date format");

			String pattern = "dd-MM-yyyy";
			ageDatePicker.setPromptText(pattern.toLowerCase());

			ageDatePicker.setConverter(new StringConverter<LocalDate>() {
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

				@Override
				public String toString(LocalDate date) {
					return date != null ? dateFormatter.format(date) : RegistrationConstants.EMPTY;
				}

				@Override
				public LocalDate fromString(String string) {
					if (string != null && !string.isEmpty()) {
						return LocalDate.parse(string, dateFormatter);
					} else {
						return null;
					}
				}
			});
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - DATE FORMAT VALIDATION FAILED ", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, runtimeException.getMessage());
		}
	}

	/**
	 * Disabling the future days in the date picker calendar.
	 *
	 * @param ageDatePicker the age date picker
	 */
	public void disableFutureDays(DatePicker ageDatePicker) {
		try {
			LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Disabling future dates");

			ageDatePicker.setDayCellFactory(picker -> new DateCell() {
				@Override
				public void updateItem(LocalDate date, boolean empty) {
					super.updateItem(date, empty);
					LocalDate today = LocalDate.now();

					setDisable(empty || date.compareTo(today) > 0);
				}
			});

			LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Future dates disabled");
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - DISABLE FUTURE DATE FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, runtimeException.getMessage());
		}
	}

	public void selectComboBoxValueByCode(ComboBox<?> localComboBox, Object selectedOption, ComboBox<?> ComboBox) {
		if(localComboBox == null)
			return;

		ObservableList<?> localComboBoxValues = localComboBox.getItems();
		ObservableList<?> comboBoxValues = ComboBox.getItems();

		if (!localComboBoxValues.isEmpty() && selectedOption != null) {
			IntPredicate findIndexOfSelectedItem = null;
			if (localComboBoxValues.get(0) instanceof GenericDto && selectedOption instanceof GenericDto) {
				findIndexOfSelectedItem = index -> ((GenericDto) localComboBoxValues.get(index)).getCode()
						.equals(((GenericDto) selectedOption).getCode());
			} else if (localComboBoxValues.get(0) instanceof DocumentCategoryDto
					&& selectedOption instanceof DocumentCategoryDto) {
				findIndexOfSelectedItem = index -> ((DocumentCategoryDto) localComboBoxValues.get(index)).getCode()
						.equals(((DocumentCategoryDto) selectedOption).getCode());
			} else if (localComboBoxValues.get(0) instanceof String && selectedOption instanceof String) {
				findIndexOfSelectedItem = index -> ((String) comboBoxValues.get(index))
						.equals(((String) ComboBox.getSelectionModel().getSelectedItem()));
				OptionalInt indexOfSelectedLocation = getIndexOfSelectedItem(comboBoxValues, findIndexOfSelectedItem);
				if (indexOfSelectedLocation.isPresent()) {
					localComboBox.getSelectionModel().select(indexOfSelectedLocation.getAsInt());
				}
				return;
			}
			OptionalInt indexOfSelectedLocation = getIndexOfSelectedItem(localComboBoxValues, findIndexOfSelectedItem);

			if (indexOfSelectedLocation.isPresent()) {
				localComboBox.getSelectionModel().select(indexOfSelectedLocation.getAsInt());
			}
		}
	}

	/**
	 * Shows the selected value in the combo-box
	 * 
	 * @param comboBox      the combo-box from which selected value has to be shown
	 * @param selectedValue the selected value from the combo-box
	 */
	public void selectComboBoxValue(ComboBox<?> comboBox, String selectedValue) {
		ObservableList<?> comboBoxValues = comboBox.getItems();
		if (!comboBoxValues.isEmpty()) {
			IntPredicate findIndexOfSelectedItem = null;
			if (comboBoxValues.get(0) instanceof DocumentCategoryDto) {
				findIndexOfSelectedItem = index -> ((DocumentCategoryDto) comboBoxValues.get(index)).getCode()
						.equals(selectedValue);
				OptionalInt indexOfSelectedLocation = getIndexOfSelectedItem(comboBoxValues, findIndexOfSelectedItem);
				if (indexOfSelectedLocation.isPresent()) {
					((DocumentCategoryDto) comboBoxValues.get(indexOfSelectedLocation.getAsInt())).setScanned(true);
					comboBox.getSelectionModel().select(indexOfSelectedLocation.getAsInt());
				}

			} else if (comboBoxValues.get(0) instanceof GenericDto) {
				findIndexOfSelectedItem = index -> ((GenericDto) comboBoxValues.get(index)).getCode()
						.equals(selectedValue);
				OptionalInt indexOfSelectedLocation = getIndexOfSelectedItem(comboBoxValues, findIndexOfSelectedItem);
				if (indexOfSelectedLocation.isPresent()) {
					comboBox.getSelectionModel().select(indexOfSelectedLocation.getAsInt());
				}
			}
		}
	}

	private OptionalInt getIndexOfSelectedItem(ObservableList<?> comboBoxValues, IntPredicate lambdaExpression) {
		return IntStream.range(0, comboBoxValues.size()).filter(lambdaExpression).findFirst();
	}

	/**
	 * The custom {@link StringConverter} for displaying only the name in the
	 * combo-box based on the combo-box type
	 * 
	 * @return the custom {@link StringConverter}
	 */
	public <T> StringConverter<T> getStringConverterForComboBox() {
		return new StringConverter<T>() {
			@Override
			public String toString(T object) {
				String value = null;
				if (object instanceof DocumentCategoryDto) {
					value = ((DocumentCategoryDto) object).getName();
				} else if (object instanceof GenericDto) {
					value = ((GenericDto) object).getName();
					// this section is for combobox of Guardian biometric screen
				} else if (object instanceof Entry<?, ?>) {
					value = ((Entry<String, String>) object).getValue();
				}
				return value;
			}

			@Override
			public T fromString(String string) {
				return null;
			}
		};
	}

	private boolean isAppLangAndLocalLangSame() {

		return ApplicationContext.getInstance().getApplicationLanguage()
				.equals(ApplicationContext.getInstance().getLocalLanguage());
	}

}
