package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
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

	int maxAge = 0;

	public boolean isNewValueValid(String newValue, String fieldType) {
		if(newValue.isEmpty())
			return true;

		if(newValue.matches(RegistrationConstants.NUMBER_REGEX)) {
			switch (fieldType) {
				case RegistrationConstants.DD:
					return Integer.parseInt(newValue) > RegistrationConstants.DAYS ? false : true;
				case RegistrationConstants.MM:
					return Integer.parseInt(newValue) > RegistrationConstants.MONTH ? false : true;
				case RegistrationConstants.YYYY:
					return newValue.length() > 4 ? false : true;
				case RegistrationConstants.AGE_FIELD:
					int age = Integer.parseInt(newValue);
					return (age < 1 ||
							Integer.parseInt(newValue) > Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE)))
							? false : true;
			}
		}
		return false;
	}

	public boolean validateDate(Pane parentPane, String fieldId) {
		resetFieldStyleClass(parentPane, fieldId, false);

		TextField dd = (TextField) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.DD);
		TextField mm = (TextField) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.MM);
		TextField yyyy = (TextField) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.YYYY);

		boolean isValid = false;
		if(dd.getText().matches(RegistrationConstants.NUMBER_REGEX) &&
				mm.getText().matches(RegistrationConstants.NUMBER_REGEX) &&
				yyyy.getText().matches(RegistrationConstants.NUMBER_REGEX) &&
				yyyy.getText().matches(RegistrationConstants.FOUR_NUMBER_REGEX)) {

			isValid = isValidDate(parentPane, dd.getText(), mm.getText(), yyyy.getText(), fieldId);
			if(isValid) {
				setLocalDateFields(parentPane, dd, mm, yyyy);
				populateAge(parentPane, fieldId);
			}
		}

		resetFieldStyleClass(parentPane, fieldId, !isValid);
		return isValid;
	}

	public boolean validateAge(Pane parentPane, TextField ageField) {
		String fieldId = ageField.getId().split("__")[0];
		resetFieldStyleClass(parentPane, fieldId, false);
		boolean isValid = ageField.getText().matches(RegistrationConstants.NUMBER_REGEX);

		if(isValid) {
			int maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
			try {
				int age = Integer.parseInt(ageField.getText());
				if(age > maxAge)
					isValid = false;
				else {

					Calendar defaultDate = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
					defaultDate.set(Calendar.DATE, 1);
					defaultDate.set(Calendar.MONTH, 0);
					defaultDate.add(Calendar.YEAR, -age);

					LocalDate date = LocalDate.of(defaultDate.get(Calendar.YEAR), defaultDate.get(Calendar.MONTH + 1),
							defaultDate.get(Calendar.DATE));
					isValid = validation.validateSingleString(date.format(DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat())),
							fieldId);

					if(isValid) {
						Node node = getFxElement(parentPane, ageField.getId()+RegistrationConstants.LOCAL_LANGUAGE);
						if(node != null) {  ((TextField)node).setText(String.valueOf(age)); }
						populateDateFields(parentPane, fieldId, age);
					}
				}
			} catch (Exception ex) {
				LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						ExceptionUtils.getStackTrace(ex));
				isValid = false;
			}
		}

		resetFieldStyleClass(parentPane, fieldId, !isValid);
		return isValid;
	}

	private void resetFieldStyleClass(Pane parentPane, String fieldId, boolean isError) {
		TextField dd = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.DD);
		TextField mm = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.MM);
		TextField yyyy = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.YYYY);
		TextField ageField = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.AGE_FIELD);

		Label dobMessage = (Label) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.DOB_MESSAGE);

		setTextFieldStyle(parentPane, dd, isError);
		setTextFieldStyle(parentPane, mm, isError);
		setTextFieldStyle(parentPane, yyyy, isError);
		setTextFieldStyle(parentPane, ageField, isError);

		if(isError) {
			dobMessage.setText(RegistrationUIConstants.INVALID_DATE.concat(" / ")
					.concat(RegistrationUIConstants.INVALID_AGE + getValueFromApplicationContext(RegistrationConstants.MAX_AGE)));
			dobMessage.setVisible(true);
			generateAlert(parentPane, RegistrationConstants.DOB, dobMessage.getText());
		} else {
			dobMessage.setText(RegistrationConstants.EMPTY);
			dobMessage.setVisible(false);
		}
	}

	private void populateAge(Pane parentPane, String fieldId) {
		LocalDate date = getCurrentSetDate(parentPane, fieldId);
		TextField ageField = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.AGE_FIELD);
		String age = String.valueOf(Period.between(date, LocalDate.now(ZoneId.of("UTC"))).getYears());
		if(!age.equals(ageField.getText())) { ageField.setText(age); }
		Node node = getFxElement(parentPane, ageField.getId()+RegistrationConstants.LOCAL_LANGUAGE);
		if(node != null) {
			TextField localTextField = (TextField)node;
			if(!age.equals(localTextField.getText())) { localTextField.setText(age); }
		}
	}

	private void populateDateFields(Pane parentPane, String fieldId, int age) {
		TextField dd = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.DD);
		TextField mm = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.MM);
		TextField yyyy = (TextField)getFxElement(parentPane, fieldId + "__" + RegistrationConstants.YYYY);

		try {
			LocalDate date = LocalDate.of(Integer.valueOf(yyyy.getText()), Integer.valueOf(mm.getText()), Integer.valueOf(dd.getText()));
			if(Period.between(date,LocalDate.now(ZoneId.of("UTC"))).getYears() == age) {
				setLocalDateFields(parentPane, dd, mm, yyyy);
				return;
			}
		} catch (Throwable t) {
			LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, t.getMessage());
		}

		Calendar defaultDate = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
		defaultDate.set(Calendar.DATE, 1);
		defaultDate.set(Calendar.MONTH, 0);
		defaultDate.add(Calendar.YEAR, -age);

		dd.setText(String.valueOf(defaultDate.get(Calendar.DATE)));
		mm.setText(String.valueOf(defaultDate.get(Calendar.MONTH + 1)));
		yyyy.setText(String.valueOf(defaultDate.get(Calendar.YEAR)));

		setLocalDateFields(parentPane, dd, mm, yyyy);
	}

	private boolean isValidDate(Pane parentPane, String dd, String mm, String yyyy, String fieldId) {
		if(isValidValue(dd) && isValidValue(mm) && isValidValue(yyyy)) {
			try {
				LocalDate date = LocalDate.of(Integer.valueOf(yyyy), Integer.valueOf(mm), Integer.valueOf(dd));
				String dob = date.format(DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat()));
				return validation.validateSingleString(dob, fieldId);
			} catch (Exception ex) {
				LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
			}
		}
		return false;
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty();
	}

	private LocalDate getCurrentSetDate(Pane parentPane, String fieldId) {
		TextField dd = (TextField) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.DD);
		TextField mm = (TextField) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.MM);
		TextField yyyy = (TextField) getFxElement(parentPane, fieldId+ "__" + RegistrationConstants.YYYY);

		if(isValidValue(dd.getText()) && isValidValue(mm.getText()) && isValidValue(yyyy.getText())) {
			try {
				return LocalDate.of(Integer.valueOf(yyyy.getText()), Integer.valueOf(mm.getText()), Integer.valueOf(dd.getText()));
			} catch (Throwable ex) {
				LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
			}
		}
		return null;
	}

	private void setLocalDateFields(Pane parentPane, TextField dd, TextField mm, TextField yyyy) {
		Node local_dd = getFxElement(parentPane, dd.getId() + RegistrationConstants.LOCAL_LANGUAGE);
		Node local_mm = getFxElement(parentPane, mm.getId() + RegistrationConstants.LOCAL_LANGUAGE);
		Node local_yyyy = getFxElement(parentPane, yyyy.getId() + RegistrationConstants.LOCAL_LANGUAGE);
		if(local_dd != null)
			((TextField)local_dd).setText(dd.getText());
		if(local_mm != null)
			((TextField)local_mm).setText(mm.getText());
		if(local_yyyy != null)
			((TextField)local_yyyy).setText(yyyy.getText());
	}


	private void setTextFieldStyle(Pane parentPane, TextField node, boolean isError) {
		Node labelNode = getFxElement(parentPane, node.getId()+RegistrationConstants.LABEL);
		if(labelNode == null) { return; }
		Label label = (Label)labelNode;
		if(isError) {
			node.getStyleClass().clear();
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
			label.getStyleClass().clear();
			label.getStyleClass().add("demoGraphicFieldLabelOnType");
		}
		else {
			node.getStyleClass().clear();
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		}
	}


	/**
	 * Validate the date and populate its corresponding local or secondary language
	 * 	 * field if date is valid
	 * @param parentPane
	 * @param date
	 * @param month
	 * @param year
	 * @param fxUtils
	 * @param ageField
	 * @param ageLocalField
	 * @param dobMessage
	 */
	public void validateDate(Pane parentPane, TextField date, TextField month, TextField year,
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
	 * 	 * field if month is valid
	 * @param parentPane
	 * @param date
	 * @param month
	 * @param year
	 * @param fxUtils
	 * @param ageField
	 * @param ageLocalField
	 * @param dobMessage
	 */
	public void validateMonth(Pane parentPane, TextField date, TextField month, TextField year,
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
	 * 	 * field if year is valid
	 * @param parentPane
	 * @param date
	 * @param month
	 * @param year
	 * @param fxUtils
	 * @param ageField
	 * @param ageLocalField
	 * @param dobMessage
	 */
	public void validateYear(Pane parentPane, TextField date, TextField month, TextField year,
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
							return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
						});
						month.getStyleClass().removeIf((s) -> {
							return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
						});
						year.getStyleClass().removeIf((s) -> {
							return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
						});
						date.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
						month.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
						year.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);

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
		date.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		month.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		year.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		dobMessage.setText(RegistrationUIConstants.INVALID_DATE);
		ageField.clear();
		dobMessage.setVisible(true);
	}

	private Node getFxElement(Pane pane, String fieldId) {
		return pane.lookup(RegistrationConstants.HASH + fieldId);
	}
}
