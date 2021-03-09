package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.control.*;
import org.bridj.cpp.std.list;
import org.springframework.beans.factory.annotation.Autowired;
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
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.Validator;
import io.mosip.registration.dto.mastersync.BlacklistedWordsDto;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.entity.BlacklistedWords;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

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
//	private ResourceBundle applicationLabelBundle;
	private StringBuilder validationMessage;
	private List<String> applicationLanguageblackListedWords;
	private List<String> noAlert;
	private boolean isLostUIN = false;

	@Autowired
	private RequiredFieldValidator requiredFieldValidator;
	@Autowired
	private UinValidator<String> uinValidator;
	@Autowired
	private RidValidator<String> ridValidator;
	@Autowired
	private DateValidation dateValidation;
	@Autowired
	private MasterSyncService masterSync;

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
		applicationMessageBundle = applicationContext.getBundle(ApplicationContext.applicationLanguage(),
				RegistrationConstants.MESSAGES);
		// applicationLabelBundle =
		// applicationContext.getBundle(ApplicationContext.applicationLanguage(),
		// RegistrationConstants.LABELS);
	}

	/**
	 * To mark as lost UIN for demographic fields validation.
	 *
	 * @param isLostUIN the flag indicating whether work flow is for Lost UIN
	 */
	public void updateAsLostUIN(boolean isLostUIN) {
		this.isLostUIN = isLostUIN;
	}

	public boolean isLostUIN() {
		return this.isLostUIN;
	}

	private List<String> getBlackListedWords(String langCode) {
		try {
			return masterSync.getAllBlackListedWords(ApplicationContext.applicationLanguage()).stream()
					.map(BlacklistedWordsDto::getWord).collect(Collectors.toList());
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
		return null;
	}

	private boolean validateButtons(Pane parentPane, Button node, String id) {
		AtomicReference<Boolean> buttonSelected = new AtomicReference<>(false);
		try {
			VBox parent = (VBox) parentPane.getParent();
			String label = parentPane.getId();

			if (node.isDisabled() || isLostUIN) {
				LOGGER.debug(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
						"Ignoring validations as its lostUIN or disabled for field >> " + label);
				return true;
			}

			boolean isMandatory = requiredFieldValidator.isRequiredField(getValidationMap().get(label),
					getRegistrationDTOFromSession());

			if (isMandatory && (node.getId().contains("gender") || node.getId().contains("residence"))) {
				node.getParent().getChildrenUnmodifiable().forEach(child -> {
					if (child instanceof Button && child.getStyleClass().contains("selectedResidence")) {
						buttonSelected.set(true);
					}
				});
			} else {
				buttonSelected.set(true);
			}
			if (!buttonSelected.get()) {
				generateAlert(parent, label, getFromLabelMap(label).concat(RegistrationConstants.SPACE)
						.concat(applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001)));
			}
		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return buttonSelected.get();
	}

	/**
	 * Validate for the TextField.
	 *
	 * @param parentPane      the {@link Pane} containing the fields
	 * @param node            the {@link Node} to be validated
	 * @param id              the id of the UI field
	 * @param isPreviousValid
	 * @return true, if successful
	 */
	public boolean validateTextField(Pane parentPane, TextField node, String id, boolean isPreviousValid,
			String langCode) {

		return languageSpecificValidation(parentPane, node, id, getMessagesBundle(langCode),
				getBlackListedWords(langCode), isPreviousValid, langCode);

	}

	private ResourceBundle getMessagesBundle(String langCode) {
		return applicationContext.getBundle(langCode, RegistrationConstants.MESSAGES);
	}

	/**
	 * Language specific validation of text field
	 *
	 * @param parentPane      the {@link Pane} containing the fields
	 * @param node            the {@link Node} to be validated
	 * @param id              the id of the UI field
	 * @param isPreviousValid
	 * @return true, if successful
	 */
	private boolean languageSpecificValidation(Pane parentPane, TextField node, String id, ResourceBundle messageBundle,
			List<String> blackListedWords, boolean isPreviousValid, String langCode) {
		LOGGER.debug(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
				"started to validate :: " + id);
		boolean isInputValid = true;
		try {
			String label = id.replaceAll(RegistrationConstants.ON_TYPE, RegistrationConstants.EMPTY)
					.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY);

			boolean showAlert = (noAlert.contains(node.getId()) && id.contains(RegistrationConstants.ON_TYPE));
			String ageDateFieldId = getAgeDateFieldId(label);
			UiSchemaDTO uiSchemaDTO = getValidationMap().get(ageDateFieldId == null ? label : ageDateFieldId);

			// During lostUIN and during updateUIN & field is not part of the selection then
			// donot do any validation
			if (isLostUIN && !id.contains(RegistrationConstants.ON_TYPE)) {
				if (node.isVisible() && (node.getText() != null && !node.getText().isEmpty())) {
					isInputValid = checkForValidValue(parentPane, node, label, node.getText(), messageBundle, showAlert,
							isPreviousValid, blackListedWords, uiSchemaDTO, langCode);
				}
				return isInputValid;
			}

			if (uiSchemaDTO != null) {
				if (requiredFieldValidator.isRequiredField(uiSchemaDTO, getRegistrationDTOFromSession())
						&& !isMandatoryFieldFilled(parentPane, uiSchemaDTO, node, node.getText())) {
					generateInvalidValueAlert(parentPane, id, getFromLabelMap(label+langCode).concat(RegistrationConstants.SPACE)
							.concat(messageBundle.getString(RegistrationConstants.REG_LGN_001)), showAlert);
					if (isPreviousValid && !id.contains(RegistrationConstants.ON_TYPE)) {
						addInvalidInputStyleClass(parentPane, node, true);
					}
					isInputValid = false;
				} else { /** Remove Error message for fields */
					Label messageLable = ((Label) (parentPane
							.lookup(RegistrationConstants.HASH + node.getId() + RegistrationConstants.MESSAGE)));
					if (messageLable != null) {
						messageLable.setText(RegistrationConstants.EMPTY);
					}
					isInputValid = true;
				}

				if (node.isVisible() && (node.getText() != null && !node.getText().isEmpty())) {
					isInputValid = checkForValidValue(parentPane, node, label, node.getText(), messageBundle, showAlert,
							isPreviousValid, blackListedWords, uiSchemaDTO, langCode);
				}
			}

		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return isInputValid;
	}

	private boolean checkForValidValue(Pane parentPane, TextField node, String fieldId, String value,
			ResourceBundle messageBundle, boolean showAlert, boolean isPreviousValid, List<String> blackListedWords,
			UiSchemaDTO uiSchemaDTO, String langCode) {

		boolean isLocalLanguageField = node.getId().contains(RegistrationConstants.LOCAL_LANGUAGE);

		if (RegistrationConstants.AGE_DATE.equals(uiSchemaDTO.getControlType())) {
			boolean isValid = dateValidation.validateDate(parentPane, uiSchemaDTO.getId());
			LOGGER.debug(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					"Date validation FAILED :: " + uiSchemaDTO.getId());
			if (!isValid)
				return false;
		}

		boolean isNonBlacklisted = validateBlackListedWords(parentPane, node, node.getId(), blackListedWords, showAlert,
				String.format("%s %s %s", messageBundle.getString(RegistrationConstants.BLACKLISTED_1),
						getFromLabelMap(fieldId), messageBundle.getString(RegistrationConstants.BLACKLISTED_2)),
				messageBundle.getString(RegistrationConstants.BLACKLISTED_ARE),
				messageBundle.getString(RegistrationConstants.BLACKLISTED_IS));

		if (!isNonBlacklisted)
			return false;

		String regex = getRegex(fieldId, RegistrationUIConstants.REGEX_TYPE, langCode);
		if (regex != null && !value.matches(regex)) {
			generateInvalidValueAlert(parentPane, node.getId(),
					getFromLabelMap(fieldId).concat(RegistrationConstants.SPACE)
							.concat(messageBundle.getString(RegistrationConstants.REG_DDC_004)),
					showAlert);
			if (isPreviousValid && !node.getId().contains(RegistrationConstants.ON_TYPE)) {
				addInvalidInputStyleClass(parentPane, node, false);
			}
			return false;
		}

		if (!isLocalLanguageField && uiSchemaDTO != null
				&& Arrays.asList("UIN", "RID").contains(uiSchemaDTO.getSubType())
				&& !validateUinOrRidField(value, getRegistrationDTOFromSession(), uiSchemaDTO)) {
			generateInvalidValueAlert(parentPane, node.getId(),
					getFromLabelMap(fieldId).concat(RegistrationConstants.SPACE)
							.concat(messageBundle.getString(RegistrationConstants.REG_DDC_004)),
					showAlert);
			if (isPreviousValid && !node.getId().contains(RegistrationConstants.ON_TYPE)) {
				addInvalidInputStyleClass(parentPane, node, false);
			}
			return false;
		}

		addValidInputStyleClass(parentPane, node);
		return true;
	}

	private boolean isMandatoryFieldFilled(Pane parentPane, UiSchemaDTO uiSchemaDTO, TextField node, String value) {
		boolean fieldFilled = false;

		if (!node.isVisible()) {
			generateAlert(parentPane, "Mandatory field is hidden : " + uiSchemaDTO.getId(),
					RegistrationConstants.ERROR);
			return fieldFilled;
		}

		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
		switch (registrationDTO.getRegistrationCategory()) {
		case RegistrationConstants.PACKET_TYPE_UPDATE:
			if (node.isDisabled()) {
				return true;
			}
			fieldFilled = doMandatoryCheckOnUpdateUIN(parentPane, value, node.getId(), uiSchemaDTO, true, node,
					registrationDTO);
			break;
		case RegistrationConstants.PACKET_TYPE_NEW:
			fieldFilled = doMandatoryCheckOnNewReg(value, uiSchemaDTO, true);
			break;
		}
		return fieldFilled;
	}

	private void addValidInputStyleClass(Pane parentPane, TextField node) {
		Label nodeLabel = (Label) parentPane.lookup("#" + node.getId() + "Label");

		if (nodeLabel == null && parentPane.getParent() != null && parentPane.getParent().getParent() != null
				&& parentPane.getParent().getParent().getParent() != null) {
			nodeLabel = (Label) parentPane.getParent().getParent().getParent().lookup("#" + node.getId() + "Label");
		}
		// node.requestFocus();
		node.getStyleClass().removeIf((s) -> {
			return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		});
		node.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextFieldOnType");
		});
		nodeLabel.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicFieldLabelOnType");
		});
		node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
	}

	private void addInvalidInputStyleClass(Pane parentPane, Node node, boolean mandatoryCheck) {
		if (mandatoryCheck) {
			node.getStyleClass().removeIf((s) -> {
				return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
			});
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		} else {
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

	private boolean doMandatoryCheckOnNewReg(String inputText, UiSchemaDTO schemaField, boolean isMandatory) {
		if (schemaField != null) {

			if (isMandatory && (inputText == null || inputText.isEmpty())) {
				return false;
			}
		}
		return true;
	}

	private boolean doMandatoryCheckOnUpdateUIN(Pane parentPane, String inputText, String id, UiSchemaDTO schemaField,
			boolean isMandatory, Node node, RegistrationDTO registrationDto) {

		if (schemaField != null && !node.isDisabled()) {
			if (isMandatory && (inputText == null || inputText.isEmpty())) {
				return false;
			}
		}
		return true;
	}

	private String getAgeDateFieldId(String nodeId) {
		String[] parts = nodeId.split("__");
		if (parts.length > 1 && parts[1].matches(RegistrationConstants.DTAE_MONTH_YEAR_REGEX)) {
			return parts[0];
		}
		return null;
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
		{
			boolean isComboBoxValueValid = false;
			try {

				String label = id.replaceAll(RegistrationConstants.ON_TYPE, RegistrationConstants.EMPTY)
						.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY);

				if (node.isDisabled() || isLostUIN) {
					LOGGER.debug(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
							"Ignoring validations as its lostUIN or disabled for field >> " + label);
					return true;
				}

				boolean isMandatory = requiredFieldValidator.isRequiredField(getValidationMap().get(label),
						getRegistrationDTOFromSession());

				if (isMandatory) {
					if (node.getValue() == null) {
						generateAlert(parentPane, id, getFromLabelMap(label).concat(RegistrationConstants.SPACE)
								.concat(applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001)));
						if (isPreviousValid) {
							node.requestFocus();
							node.getStyleClass().removeIf((s) -> {
								return s.equals("demographicCombobox");
							});
							node.getStyleClass().add("demographicComboboxFocused");
						} else {
							node.getStyleClass().add("demographicCombobox");
						}
					} else {
						node.getStyleClass().removeIf((s) -> {
							return s.equals("demographicComboboxFocused");
						});
						node.getStyleClass().add("demographicCombobox");
						if (node.getValue().getClass().getSimpleName().equalsIgnoreCase("DocumentCategoryDto")) {
							DocumentCategoryDto doc = (DocumentCategoryDto) node.getValue();
							if (doc.isScanned()) {
								isComboBoxValueValid = true;
							} else {
								isComboBoxValueValid = false;
							}

						} else {
							isComboBoxValueValid = true;
						}

					}
				} else {
					node.getStyleClass().removeIf((s) -> {
						return s.equals("demographicComboboxFocused");
					});
					node.getStyleClass().add("demographicCombobox");

					isComboBoxValueValid = true;
				}
			} catch (RuntimeException | RegBaseCheckedException runtimeException) {
				LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
						runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			}

			return isComboBoxValueValid;
		}
	}

	private boolean validateUinOrRidField(String inputText, RegistrationDTO registrationDto, UiSchemaDTO schemaField) {
		boolean isValid = true;
		try {
			if ("UIN".equals(schemaField.getSubType())) {
				String updateUIN = RegistrationConstants.PACKET_TYPE_UPDATE
						.equals(registrationDto.getRegistrationCategory())
								? (String) registrationDto.getDemographics().get("UIN")
								: null;

				if (updateUIN != null && inputText.equals(updateUIN))
					isValid = false;

				if (isValid)
					isValid = uinValidator.validateId(inputText);
			}

			if ("RID".equals(schemaField.getSubType())) {
				isValid = ridValidator.validateId(inputText);
			}

		} catch (InvalidIDException invalidRidException) {
			isValid = false;
			LOGGER.error(schemaField.getSubType() + " VALIDATION FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(invalidRidException));
		}
		return isValid;
	}

	/**
	 * Validate for the single string.
	 *
	 * @param value the value to be validated
	 * @param id    the id of the UI field whose value is provided as input
	 * @return <code>true</code>, if successful, else <code>false</code>
	 */
	public boolean validateSingleString(String value, String id, String langCode) {
		String regex = getRegex(id, RegistrationUIConstants.REGEX_TYPE, langCode);
		return regex != null ? value.matches(regex) : true;
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
	 * @param isChild the new child
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

	private String getRegex(String fieldId, String regexType, String langCode) {
		UiSchemaDTO uiSchemaDTO = getValidationMap().get(fieldId);
		if (uiSchemaDTO != null && uiSchemaDTO.getValidators() != null) {

			if (langCode != null) {
				Optional<Validator> validator = uiSchemaDTO.getValidators().stream()
						.filter(v -> v.getType().equalsIgnoreCase(regexType)
								&& (v.getLangCode() != null ? langCode.equalsIgnoreCase(v.getLangCode()) : true))
						.findFirst();
				if (validator.isPresent()) {
					return validator.get().getValidator();
				}
			}

			return uiSchemaDTO.getValidators().get(0).getValidator();

		}
		return null;
	}
}