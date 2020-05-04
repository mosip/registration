package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.util.Arrays;
import org.bridj.cpp.std.list;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.Validator;
import io.mosip.registration.dto.demographic.DocumentDetailsDTO;
import io.mosip.registration.dto.mastersync.BlacklistedWordsDto;
import io.mosip.registration.entity.BlacklistedWords;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * Class for validation of the Registration Field
 * 
 * @author Taleev.Aalam
 * @author Balaji
 * @since 1.0.0
 *
 */
@Component
public class Validations extends BaseController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(Validations.class);
	private boolean isChild;
	private ResourceBundle applicationMessageBundle;
	private ResourceBundle localMessageBundle;
	private ResourceBundle applicationLabelBundle;
	private ResourceBundle localLabelBundle;
	private StringBuilder validationMessage;
	private List<String> applicationLanguageblackListedWords;
	private List<String> localLanguageblackListedWords;
	private List<String> noAlert;
	private boolean isLostUIN = false;
	private int maxAge;;

	/**
	 * Instantiates a new validations.
	 */
	public Validations() {
		try {
			noAlert = new ArrayList<>();
			noAlert.add(RegistrationConstants.DD);
			noAlert.add(RegistrationConstants.MM);
			noAlert.add(RegistrationConstants.YYYY);
			noAlert.add(RegistrationConstants.DD + RegistrationConstants.LOCAL_LANGUAGE);
			noAlert.add(RegistrationConstants.MM + RegistrationConstants.LOCAL_LANGUAGE);
			noAlert.add(RegistrationConstants.YYYY + RegistrationConstants.LOCAL_LANGUAGE);
			validationMessage = new StringBuilder();
		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	/**
	 * Sets the resource bundles for Validations, Messages in Application and
	 * Secondary Languages and Labels in Application and Secondary Languages
	 */
	public void setResourceBundle() {
		// validationBundle = ApplicationContext.applicationLanguageValidationBundle();
		applicationMessageBundle = ApplicationContext.applicationMessagesBundle();
		localMessageBundle = ApplicationContext.localMessagesBundle();
		applicationLabelBundle = ApplicationContext.applicationLanguageBundle();
		localLabelBundle = ApplicationContext.localLanguageBundle();
	}

	/**
	 * Iterate the fields to invoke the validate.
	 *
	 * @param pane
	 *            the {@link Pane} containing the fields
	 * @param notTovalidate
	 *            the {@link List} of UI fields not be validated
	 * @param isValid
	 *            the flag indicating whether validation is success or fail
	 * @param isConsolidated
	 *            the flag to indicate for displaying consolidated message
	 * @return true, if successful
	 */
	public boolean validateTheFields(Pane pane, List<String> notTovalidate, boolean isValid) {
		for (Node node : pane.getChildren()) {
			if (node instanceof Pane) {
				if (!validateTheFields((Pane) node, notTovalidate, isValid)) {
					isValid = false;
				}
			} else if (nodeToValidate(notTovalidate, node) && !validateTheNode(pane, node, node.getId(), isValid)) {
				isValid = false;
			}
		}
		return isValid;
	}

	/**
	 * To mark as lost UIN for demographic fields validation.
	 *
	 * @param isLostUIN
	 *            the flag indicating whether work flow is for Lost UIN
	 */
	protected void updateAsLostUIN(boolean isLostUIN) {
		this.isLostUIN = isLostUIN;
	}

	public boolean isLostUIN() {
		return this.isLostUIN;
	}

	/**
	 * To decide whether this node should be validated or not.
	 *
	 * @param notTovalidate
	 *            the {@link list} of fields not be validated
	 * @param node
	 *            the {@link Node} to be checked
	 * @return true, if successful
	 */
	private boolean nodeToValidate(List<String> notTovalidate, Node node) {
		return !(node.getId() == null || notTovalidate.contains(node.getId()) || node instanceof ImageView
				|| node instanceof Button || node instanceof Label || node instanceof Hyperlink);
	}

	/**
	 * Validate the UI fields. Fetch the {@link BlacklistedWords} for application
	 * specific and secondary specific languages.
	 *
	 * @param pane
	 *            the {@link Pane} containing the UI Fields to be validated
	 * @param notTovalidate
	 *            the {@link List} of fields not be validated
	 * @param isValid
	 *            the flag to indicating the status of validation
	 * @param masterSync
	 *            the instance of {@link MasterSyncService} for fetching
	 *            {@link BlacklistedWords}
	 * @return true, if successful
	 */
	public boolean validate(Pane pane, List<String> notTovalidate, boolean isValid, MasterSyncService masterSync) {
		try {
			this.applicationLanguageblackListedWords = masterSync
					.getAllBlackListedWords(ApplicationContext.applicationLanguage()).stream()
					.map(BlacklistedWordsDto::getWord).collect(Collectors.toList());
			this.localLanguageblackListedWords = masterSync.getAllBlackListedWords(ApplicationContext.localLanguage())
					.stream().map(BlacklistedWordsDto::getWord).collect(Collectors.toList());
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		return validateTheFields(pane, notTovalidate, isValid);
	}

	/**
	 * Pass the node to check for the validation, specific validation method will be
	 * called for each field.
	 *
	 * @param parentPane
	 *            the {@link Pane} containing the UI Fields to be validated
	 * @param node
	 *            the {@link Node} to be validated
	 * @param id
	 *            the id of the field to be validated
	 * @param isConsolidated
	 *            the flag to indicate for displaying consolidated message
	 * @return true, if successful
	 */
	public boolean validateTheNode(Pane parentPane, Node node, String id, boolean isPreviousValid) {
		if (node instanceof ComboBox<?>) {
			return validateComboBox(parentPane, (ComboBox<?>) node, id, isPreviousValid);
		}
		return validateTextField(parentPane, (TextField) node, id, isPreviousValid);
	}

	/**
	 * Validate for the TextField.
	 *
	 * @param parentPane
	 *            the {@link Pane} containing the fields
	 * @param node
	 *            the {@link Node} to be validated
	 * @param id
	 *            the id of the UI field
	 * @param isConsolidated
	 *            the flag to indicate for displaying consolidated message
	 * @return true, if successful
	 */
	public boolean validateTextField(Pane parentPane, TextField node, String id, boolean isPreviousValid) {
		if (node.getId().contains(RegistrationConstants.LOCAL_LANGUAGE)) {
			return languageSpecificValidation(parentPane, node, id, localLabelBundle, localMessageBundle,
					localLanguageblackListedWords, isPreviousValid);
		} else {
			return languageSpecificValidation(parentPane, node, id, applicationLabelBundle, applicationMessageBundle,
					applicationLanguageblackListedWords, isPreviousValid);
		}
	}

	/**
	 * Language specific validation of text field
	 *
	 * @param parentPane
	 *            the {@link Pane} containing the fields
	 * @param node
	 *            the {@link Node} to be validated
	 * @param id
	 *            the id of the UI field
	 * @param isConsolidated
	 *            the flag to indicate for displaying consolidated message
	 * @return true, if successful
	 */
	private boolean languageSpecificValidation(Pane parentPane, TextField node, String id, ResourceBundle labelBundle,
			ResourceBundle messageBundle, List<String> blackListedWords, boolean isPreviousValid) {
		boolean isInputValid = true;

		try {
			String label = id.replaceAll(RegistrationConstants.ON_TYPE, RegistrationConstants.EMPTY)
					.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY);
			
			if(isLostUIN && !id.contains("_ontype") ) {
				return true;
			}

			UiSchemaDTO uiSchemaDTO = getUiSchemaDTO(id, isLostUIN);

			String regex = getRegex(uiSchemaDTO, RegistrationUIConstants.REGEX_TYPE);
			if (regex != null) {

				isInputValid = false;
				boolean isMandatory = uiSchemaDTO.isRequired();
				if(isLostUIN)
					isMandatory=false;
				boolean showAlert = (noAlert.contains(node.getId()) && id.contains(RegistrationConstants.ON_TYPE));
				String inputText = node.getText();

				if (node.isDisabled() || (!isMandatory && inputText.isEmpty())) {
					isInputValid = true;
				} else if (isMandatory && (inputText == null || inputText.isEmpty()) && !isLostUIN) {
					generateInvalidValueAlert(parentPane, id,
							labelBundle.getString(label).concat(RegistrationConstants.SPACE)
									.concat(messageBundle.getString(RegistrationConstants.REG_LGN_001)),
							showAlert);
					if (isPreviousValid && !id.contains(RegistrationConstants.ON_TYPE)) {
						node.requestFocus();
						node.getStyleClass().removeIf((s) -> {
							return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
						});
						node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
					}
				} else if (inputText.matches(regex)) {
					isInputValid = validateBlackListedWords(parentPane, node, id, blackListedWords, showAlert,
							String.format("%s %s %s", messageBundle.getString(RegistrationConstants.BLACKLISTED_1),
									labelBundle.getString(label),
									messageBundle.getString(RegistrationConstants.BLACKLISTED_2)),
							messageBundle.getString(RegistrationConstants.BLACKLISTED_ARE),
							messageBundle.getString(RegistrationConstants.BLACKLISTED_IS));
				} else {
					generateInvalidValueAlert(parentPane, id, labelBundle.getString(label) + " "
							+ messageBundle.getString(RegistrationConstants.REG_DDC_004), showAlert);
					if (isPreviousValid && !id.contains(RegistrationConstants.ON_TYPE)) {
						Label nodeLabel = (Label) parentPane.lookup("#" + node.getId() + "Label");
						node.requestFocus();
						node.getStyleClass().removeIf((s) -> {
							return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
						});
						node.getStyleClass().removeIf((s) -> {
							return s.equals("demoGraphicTextFieldOnType");
						});
						nodeLabel.getStyleClass().removeIf((s) -> {
							return s.equals("demoGraphicFieldLabelOnType");
						});
						node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
					}
				}
			}
			if(label.equals("ageField")) {
				maxAge=Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
				if( Integer.parseInt(node.getText())>maxAge) {
					return false;
				}
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}

		return isInputValid;

	}

	private boolean validateBlackListedWords(Pane parentPane, TextField node, String id, List<String> blackListedWords,
			boolean showAlert, String errorMessage, String are, String is) {
		boolean isInputValid = true;
		if (blackListedWords != null && !id.contains(RegistrationConstants.ON_TYPE)) {
			if (blackListedWords.contains(node.getText())) {
				isInputValid = false;
				generateInvalidValueAlert(parentPane, id, String.format("%s %s", node.getText(), errorMessage),
						showAlert);
			} else {

				Set<String> invalidWorlds = blackListedWords.stream().flatMap(l1 -> Stream
						.of(node.getText().split("\\s+")).collect(Collectors.toList()).stream().filter(l2 -> {
							return l1.equalsIgnoreCase(l2);
						})).collect(Collectors.toSet());

				String bWords = String.join(", ", invalidWorlds);
				if (bWords.length() > 0) {
					generateInvalidValueAlert(parentPane, id,
							String.format("%s %s %s", bWords, invalidWorlds.size() > 1 ? are : is, errorMessage),
							showAlert);
					isInputValid = false;
				} else {
					isInputValid = true;
				}
			}
		}
		return isInputValid;
	}

	private void generateInvalidValueAlert(Pane parentPane, String id, String message, boolean showAlert) {
		if (!showAlert)
			generateAlert(parentPane, id, message);
	}

	/**
	 * Validate for the ComboBox type of node
	 */
	private boolean validateComboBox(Pane parentPane, ComboBox<?> node, String id, boolean isPreviousValid) {
		boolean isComboBoxValueValid = false;
		try {
			if (getRegistrationDTOFromSession().getSelectionListDTO() != null && ((id
					.matches(RegistrationConstants.POA_DOCUMENT)
					&& !getRegistrationDTOFromSession().getSelectionListDTO().isAddress())
					|| (id.matches(RegistrationConstants.POI_DOCUMENT)
							&& !getRegistrationDTOFromSession().getSelectionListDTO().isName())
					|| (id.matches(RegistrationConstants.POR_DOCUMENT)
							&& !getRegistrationDTOFromSession().getSelectionListDTO().isParentOrGuardianDetails()))) {
				return true;
			}
			if (getRegistrationDTOFromSession().getSelectionListDTO() == null
					&& id.matches(RegistrationConstants.POR_DOCUMENT) && !isChild)
				return true;

			if (id.matches(RegistrationConstants.DOB_DOCUMENT))
				return true;

			if (id.matches(RegistrationConstants.POE_DOCUMENT))
				return true;

			if (node.isDisabled())
				return true;
			if (isLostUIN)
				return true;

			if (node.getValue() == null) {
				if (!(Arrays
						.asList(new String[] { RegistrationConstants.POA_DOCUMENT, RegistrationConstants.POI_DOCUMENT,
								RegistrationConstants.POR_DOCUMENT, RegistrationConstants.DOB_DOCUMENT })
						.contains(id))) {
					generateAlert(parentPane, id,
							applicationLabelBundle.getString(id).concat(RegistrationConstants.SPACE)
									.concat(applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001)));
					if (isPreviousValid) {
						node.requestFocus();
						node.getStyleClass().removeIf((s) -> {
							return s.equals("demographicCombobox");
						});
						node.getStyleClass().add("demographicComboboxFocused");
					}
				}
			} else {
				if (Arrays
						.asList(new String[] { RegistrationConstants.POA_DOCUMENT, RegistrationConstants.POI_DOCUMENT,
								RegistrationConstants.POR_DOCUMENT, RegistrationConstants.DOB_DOCUMENT })
						.contains(id)) {
					Map<String, DocumentDetailsDTO> documents = getRegistrationDTOFromSession().getDocuments();
					if (documents.containsKey(id) && documents.get(id) != null) {
						isComboBoxValueValid = true;
					}
				} else {
					node.getStyleClass().removeIf((s) -> {
						return s.equals("demographicComboboxFocused");
					});
					node.getStyleClass().add("demographicCombobox");

					isComboBoxValueValid = true;
				}
			}
		} catch (

		RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return isComboBoxValueValid;
	}

	/**
	 * Validates the Parent or Guardian's UIN or RID UI Field
	 * 
	 * @param field
	 *            the {@link TextField} to be validated
	 * @param isChild
	 *            the flag to determine whether the individual or applicant is child
	 * @param uinValidator
	 *            the instance of {@link UinValidator} required to validate the UIN
	 * @param ridValidator
	 *            the instance of {@link RidValidator} required to validate the RID
	 * @return <code>true</code> if UIN or RID is valid, else <code>false</code>
	 */
	public boolean validateUinOrRid(Pane parentPane, TextField uinId, TextField regId, boolean isChild,
			UinValidator<String> uinValidator, RidValidator<String> ridValidator) {
		boolean isIdValid = false;

		if (isChild) {
			if (!uinId.isDisabled()) {
				if ("" == uinId.getText() || uinId.getText().isEmpty()) {
					generateInvalidValueAlert(parentPane, uinId.getId(), applicationLabelBundle.getString(uinId.getId())
							+ " " + applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001), false);

					uinId.getStyleClass().remove(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
					uinId.requestFocus();
				} else {
					try {
						isIdValid = uinValidator.validateId(uinId.getText());
					} catch (InvalidIDException invalidUinException) {

						generateInvalidValueAlert(parentPane, uinId.getId(),
								applicationLabelBundle.getString(uinId.getId()) + " "
										+ applicationMessageBundle.getString(RegistrationConstants.REG_DDC_004),
								false);

						LOGGER.error("UIN VALIDATION FAILED", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
								invalidUinException.getMessage() + ExceptionUtils.getStackTrace(invalidUinException));
						uinId.getStyleClass().remove(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
						uinId.requestFocus();

					}

				}
			} else {
				if (getRegistrationDTOFromSession().getSelectionListDTO() == null && !regId.isDisabled()) {
					if ("" == regId.getText() || regId.getText().isEmpty()) {
						generateInvalidValueAlert(parentPane, regId.getId(),
								applicationLabelBundle.getString(regId.getId()) + " "
										+ applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001),
								false);
						regId.getStyleClass().remove(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
						regId.requestFocus();
					} else {
						try {
							isIdValid = ridValidator.validateId(regId.getText());
						} catch (InvalidIDException invalidRidException) {
							generateInvalidValueAlert(parentPane, regId.getId(),
									applicationLabelBundle.getString(regId.getId()) + " "
											+ applicationMessageBundle.getString(RegistrationConstants.REG_DDC_004),
									false);
							LOGGER.error("RID VALIDATION FAILED", APPLICATION_NAME,
									RegistrationConstants.APPLICATION_ID, invalidRidException.getMessage()
											+ ExceptionUtils.getStackTrace(invalidRidException));
							regId.getStyleClass().remove(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
							regId.requestFocus();

						}
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UIN_INVALID);
				}
			}
		} else {
			isIdValid = true;
		}

		return isIdValid;
	}

	/**
	 * Validate for the single string.
	 *
	 * @param value
	 *            the value to be validated
	 * @param id
	 *            the id of the UI field whose value is provided as input
	 * @return <code>true</code>, if successful, else <code>false</code>
	 */
	public boolean validateSingleString(String value, String id) {

		return value.matches(getRegex(getUiSchemaDTO(id, isLostUIN), RegistrationUIConstants.REGEX_TYPE));
	}

	/**
	 * Check for child.
	 *
	 * @return true, if is child
	 */
	public boolean isChild() {
		return isChild;
	}

	/**
	 * Set for child.
	 *
	 * @param isChild
	 *            the new child
	 */
	public void setChild(boolean isChild) {
		this.isChild = isChild;
	}

	/**
	 * Gets the validation message.
	 *
	 * @return the validation message
	 */
	public StringBuilder getValidationMessage() {
		return validationMessage;
	}

	/**
	 * Sets the validation message.
	 */
	public void setValidationMessage() {
		validationMessage.delete(0, validationMessage.length());
	}

	private UiSchemaDTO getUiSchemaDTO(String id, boolean isLostUIN) {
		UiSchemaDTO uiSchemaDTO = null;

		if (getValidationMap().containsKey(id)) {
			uiSchemaDTO = getValidationMap().get(id);
		}
		return uiSchemaDTO;

	}


	private String getRegex(UiSchemaDTO uiSchemaDTO, String regexType) {

		String regex = null;

		if (uiSchemaDTO != null && uiSchemaDTO.getValidators() != null) {
			for (Validator validator : uiSchemaDTO.getValidators()) {
				if (validator.getType().equalsIgnoreCase(regexType)) {
					return validator.getValidator();
				}
			}
		}

		return regex;
	}
}
