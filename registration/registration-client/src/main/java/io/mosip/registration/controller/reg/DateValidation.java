package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

/**
 * Class for validating the date fields
 * 
 * @author Taleev.Aalam
 * @author Balaji
 * @since 1.0.0
 *
 */
@Component
public class DateValidation extends BaseController {

	private static final Logger LOGGER = AppConfig.getLogger(DateValidation.class);
	@Autowired
	private Validations validation;
	@Autowired
	private DemographicDetailController demographicDetailController;

	int maxAge = 0;

	/**
	 * Validate the date and populate its corresponding local or secondary language
	 * field if date is valid
	 *
	 * @param parentPane  the {@link Pane} containing the date fields
	 * @param date        the date(dd) {@link TextField}
	 * @param month       the month {@link TextField}
	 * @param year        the year {@link TextField}
	 * @param validations the instance of {@link Validations}
	 * @param fxUtils     the instance of {@link FXUtils}
	 * @param localField  the local field to be populated if input is valid.
	 */
	public void validateDate(Pane parentPane, TextField date, TextField month, TextField year, Validations validations,
			FXUtils fxUtils, TextField ageField, TextField ageLocalField, Label dobMessage) {
		if (maxAge == 0)
			maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
		try {
			fxUtils.validateOnType(parentPane, date, validation, false);
			date.textProperty().addListener((obsValue, oldValue, newValue) -> {
				populateAge(parentPane, date, month, year, ageField, dobMessage);

			});

		} catch (RuntimeException runTimeException) {
			LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runTimeException.getMessage() + ExceptionUtils.getStackTrace(runTimeException));

		}
	}

	/**
	 * Validate the month and populate its corresponding local or secondary language
	 * field if month is valid
	 *
	 * @param parentPane  the {@link Pane} containing the date fields
	 * @param date        the date(dd) {@link TextField}
	 * @param month       the month {@link TextField}
	 * @param year        the year {@link TextField}
	 * @param validations the instance of {@link Validations}
	 * @param fxUtils     the instance of {@link FXUtils}
	 * @param localField  the local field to be populated if input is valid.
	 */
	public void validateMonth(Pane parentPane, TextField date, TextField month, TextField year, Validations validations,
			FXUtils fxUtils, TextField ageField, TextField ageLocalField, Label dobMessage) {
		try {
			fxUtils.validateOnType(parentPane, month, validation, false);
			month.textProperty().addListener((obsValue, oldValue, newValue) -> {
				populateAge(parentPane, date, month, year, ageField, dobMessage);

			});
		} catch (RuntimeException runTimeException) {
			LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runTimeException.getMessage() + ExceptionUtils.getStackTrace(runTimeException));

		}
	}

	/**
	 * Validate the year and populate its corresponding local or secondary language
	 * field if year is valid
	 *
	 * @param parentPane  the {@link Pane} containing the date fields
	 * @param date        the date(dd) {@link TextField}
	 * @param month       the month {@link TextField}
	 * @param year        the year {@link TextField}
	 * @param validations the instance of {@link Validations}
	 * @param fxUtils     the instance of {@link FXUtils}
	 * @param localField  the local field to be populated if input is valid.
	 */
	public void validateYear(Pane parentPane, TextField date, TextField month, TextField year, Validations validations,
			FXUtils fxUtils, TextField ageField, TextField ageLocalField, Label dobMessage) {
		try {
			fxUtils.validateOnType(parentPane, year, validation, false);

			year.textProperty().addListener((obsValue, oldValue, newValue) -> {
				populateAge(parentPane, date, month, year, ageField, dobMessage);
			});
		} catch (RuntimeException runTimeException) {
			LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runTimeException.getMessage() + ExceptionUtils.getStackTrace(runTimeException));

		}
	}

	private void populateAge(Pane parentPane, TextField date, TextField month, TextField year, TextField ageField,
			Label dobMessage) {

		if (date != null && month != null && year != null) {

			if (getFxElement(parentPane, date.getId() + RegistrationConstants.LOCAL_LANGUAGE) != null) {
				((TextField) getFxElement(parentPane, date.getId() + RegistrationConstants.LOCAL_LANGUAGE))

						.setText(date.getText());
			}

			if (getFxElement(parentPane, date.getId() + RegistrationConstants.LOCAL_LANGUAGE) != null) {
				((TextField) getFxElement(parentPane, month.getId() + RegistrationConstants.LOCAL_LANGUAGE))
						.setText(month.getText());

			}

			if (getFxElement(parentPane, year.getId() + RegistrationConstants.LOCAL_LANGUAGE) != null) {
				((TextField) getFxElement(parentPane, year.getId() + RegistrationConstants.LOCAL_LANGUAGE))
						.setText(year.getText());

			}

			if (!date.getText().isEmpty() && !month.getText().isEmpty() && !year.getText().isEmpty()
					&& year.getText().matches(RegistrationConstants.FOUR_NUMBER_REGEX)) {

				try {
					LocalDate givenDate = LocalDate.of(Integer.parseInt(year.getText()),
							Integer.parseInt(month.getText()), Integer.parseInt(date.getText()));
					LocalDate localDate = LocalDate.now();

					if (localDate.compareTo(givenDate) >= 0) {

						int age = Period.between(givenDate, localDate).getYears();

						getRegistrationDTOFromSession().setAgeCalculatedByDOB(true);

						ageField.setText(String.valueOf(age));
						ageField.requestFocus();
						dobMessage.setText(RegistrationConstants.EMPTY);
						dobMessage.setVisible(false);
						date.getStyleClass().removeIf((s) -> {
							return s.equals("demoGraphicTextFieldFocused");
						});
						month.getStyleClass().removeIf((s) -> {
							return s.equals("demoGraphicTextFieldFocused");
						});
						year.getStyleClass().removeIf((s) -> {
							return s.equals("demoGraphicTextFieldFocused");
						});
						date.getStyleClass().add("demoGraphicTextField");
						month.getStyleClass().add("demoGraphicTextField");
						year.getStyleClass().add("demoGraphicTextField");
						// demographicDetailController.ageValidation(false);

						if (getFxElement(parentPane, ageField.getId() + RegistrationConstants.LOCAL_LANGUAGE) != null) {
							((TextField) getFxElement(parentPane,
									ageField.getId() + RegistrationConstants.LOCAL_LANGUAGE))
											.setText(ageField.getText());
						}
					} else {
						ageField.clear();
						dobMessage.setText(RegistrationUIConstants.FUTURE_DOB);
						dobMessage.setVisible(true);
						generateAlert(parentPane, RegistrationConstants.DOB, dobMessage.getText());
					}
				} catch (Throwable exception) {
					setErrorMsg(parentPane, date, month, year, ageField, dobMessage);
					generateAlert(parentPane, RegistrationConstants.DOB, dobMessage.getText());
					LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME,
							RegistrationConstants.APPLICATION_ID,
							exception.getMessage() + ExceptionUtils.getStackTrace(exception));
				}
			} else if ((!date.getText().isEmpty() && Integer.parseInt(date.getText()) > RegistrationConstants.DAYS)
					|| (!month.getText().isEmpty() && Integer.parseInt(month.getText()) > RegistrationConstants.MONTH)
					|| (!year.getText().isEmpty() && (Integer.parseInt(year.getText()) > LocalDate.now().getYear()
							|| (((LocalDate.now().getYear() - Integer.parseInt(year.getText())) > Integer
									.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE)))
									&& year.getText().length() > RegistrationConstants.YEAR)))) {
				setErrorMsg(parentPane, date, month, year, ageField, dobMessage);
				generateAlert(parentPane, RegistrationConstants.DOB, dobMessage.getText());
			}
		}
	}

	private void setErrorMsg(Pane parentPane, TextField date, TextField month, TextField year, TextField ageField,
			Label dobMessage) {
		date.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextFieldOnType");
		});
		month.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextFieldOnType");
		});
		year.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextFieldOnType");
		});
		date.getStyleClass().add("demoGraphicTextFieldFocused");
		month.getStyleClass().add("demoGraphicTextFieldFocused");
		year.getStyleClass().add("demoGraphicTextFieldFocused");
		dobMessage.setText(RegistrationUIConstants.INVALID_DATE);
		ageField.clear();
		dobMessage.setVisible(true);
	}

	private Node getFxElement(Pane pane, String fieldId) {

		return pane.lookup(RegistrationConstants.HASH + fieldId);
	}
}
