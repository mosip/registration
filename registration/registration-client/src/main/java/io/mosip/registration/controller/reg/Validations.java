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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
	private ResourceBundle localMessageBundle;
	private ResourceBundle applicationLabelBundle;
	private ResourceBundle localLabelBundle;
	private StringBuilder validationMessage;
	private List<String> applicationLanguageblackListedWords;
	private List<String> localLanguageblackListedWords;
	private List<String> noAlert;
	private boolean isLostUIN = false;
	private int maxAge;

	@Autowired
	private RequiredFieldValidator requiredFieldValidator;
	@Autowired
	private UinValidator<String> uinValidator;
	@Autowired
	private RidValidator<String> ridValidator;

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
	 * @param pane           the {@link Pane} containing the fields
	 * @param notTovalidate  the {@link List} of UI fields not be validated
	 * @param isValid        the flag indicating whether validation is success or
	 *                       fail
	 * @param isConsolidated the flag to indicate for displaying consolidated
	 *                       message
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
	 * @param isLostUIN the flag indicating whether work flow is for Lost UIN
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
	 * @param notTovalidate the {@link list} of fields not be validated
	 * @param node          the {@link Node} to be checked
	 * @return true, if successful
	 */
	private boolean nodeToValidate(List<String> notTovalidate, Node node) {
		if (node.getId() != null && (node.getId().contains("gender") || node.getId().contains("residence"))) {
			return !(node.getId() == null || notTovalidate.contains(node.getId()) || node instanceof ImageView
					|| node instanceof Label || node instanceof Hyperlink || node.isDisabled());
		}
		return !(node.getId() == null || notTovalidate.contains(node.getId()) || node instanceof ImageView
				|| node instanceof Button || node instanceof Label || node instanceof Hyperlink || node.isDisabled());
	}

	/**
	 * Validate the UI fields. Fetch the {@link BlacklistedWords} for application
	 * specific and secondary specific languages.
	 *
	 * @param pane          the {@link Pane} containing the UI Fields to be
	 *                      validated
	 * @param notTovalidate the {@link List} of fields not be validated
	 * @param isValid       the flag to indicating the status of validation
	 * @param masterSync    the instance of {@link MasterSyncService} for fetching
	 *                      {@link BlacklistedWords}
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
	 * @param parentPane     the {@link Pane} containing the UI Fields to be
	 *                       validated
	 * @param node           the {@link Node} to be validated
	 * @param id             the id of the field to be validated
	 * @param isConsolidated the flag to indicate for displaying consolidated
	 *                       message
	 * @return true, if successful
	 */
	public boolean validateTheNode(Pane parentPane, Node node, String id, boolean isPreviousValid) {
		if (node instanceof ComboBox<?>) {
			return validateComboBox(parentPane, (ComboBox<?>) node, id, isPreviousValid);
		} else if (node instanceof Button) {
			return validateButtons(parentPane, (Button) node, id);
		}
		return validateTextField(parentPane, (TextField) node, id, isPreviousValid);
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
	 * @param parentPane     the {@link Pane} containing the fields
	 * @param node           the {@link Node} to be validated
	 * @param id             the id of the UI field
	 * @param isConsolidated the flag to indicate for displaying consolidated
	 *                       message
	 * @return true, if successful
	 */
	public boolean validateTextField(Pane parentPane, TextField node, String id, boolean isPreviousValid) {
		if (node.getId().contains(RegistrationConstants.LOCAL_LANGUAGE)) {
			return languageSpecificValidation(parentPane, node, id, localMessageBundle, localLanguageblackListedWords,
					isPreviousValid);
		} else {
			return languageSpecificValidation(parentPane, node, id, applicationMessageBundle,
					applicationLanguageblackListedWords, isPreviousValid);
		}
	}

	/**
	 * Language specific validation of text field
	 *
	 * @param parentPane     the {@link Pane} containing the fields
	 * @param node           the {@link Node} to be validated
	 * @param id             the id of the UI field
	 * @param isConsolidated the flag to indicate for displaying consolidated
	 *                       message
	 * @return true, if successful
	 */
	// TODO validate for UIN and RID based on subtype
	private boolean languageSpecificValidation(Pane parentPane, TextField node, String id, ResourceBundle messageBundle,
			List<String> blackListedWords, boolean isPreviousValid) {
		LOGGER.debug(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
				"started to validate :: " + id);
		boolean isInputValid = true;
		try {
			String label = id.replaceAll(RegistrationConstants.ON_TYPE, RegistrationConstants.EMPTY)
					.replaceAll(RegistrationConstants.LOCAL_LANGUAGE, RegistrationConstants.EMPTY);

			// During lostUIN and during updateUIN & field is not part of the selection then
			// donot do any validation
			if (isLostUIN && !id.contains(RegistrationConstants.ON_TYPE)) {
				return true;
			}

			RegistrationDTO registrationDto = getRegistrationDTOFromSession();
			boolean showAlert = (noAlert.contains(node.getId()) && id.contains(RegistrationConstants.ON_TYPE));
			String inputText = node.getText();
			String ageDateFieldId = getAgeDateFieldId(label);
			UiSchemaDTO uiSchemaDTO = getValidationMap().get(ageDateFieldId == null ? label : ageDateFieldId);

			if (uiSchemaDTO != null) {
				boolean isMandatory = requiredFieldValidator.isRequiredField(uiSchemaDTO, registrationDto);

				switch (registrationDto.getRegistrationCategory()) {
				case RegistrationConstants.PACKET_TYPE_UPDATE:
					isInputValid = doMandatoryCheckOnUpdateUIN(parentPane, inputText, id, uiSchemaDTO, isMandatory,
							node, registrationDto);
					break;
				case RegistrationConstants.PACKET_TYPE_NEW:
					isInputValid = doMandatoryCheckOnNewReg(inputText, uiSchemaDTO, isMandatory);
					if (!isInputValid) {
						generateInvalidValueAlert(parentPane, id,
								getFromLabelMap(label).concat(RegistrationConstants.SPACE)
										.concat(messageBundle.getString(RegistrationConstants.REG_LGN_001)),
								showAlert);
						if (isPreviousValid && !id.contains(RegistrationConstants.ON_TYPE)) {
							addInvalidInputStyleClass(parentPane, node, true);
						}
					} else {

						/** Remove Error message for fields */
						Label messageLable = ((Label) (parentPane
								.lookup(RegistrationConstants.HASH + node.getId() + RegistrationConstants.MESSAGE)));

						if (messageLable != null) {
							messageLable.setText(RegistrationConstants.EMPTY);
						}
					}
					break;
				}

				/** !node.isDisabled() */
				if (isInputValid) {
					String regex = getRegex(id, RegistrationUIConstants.REGEX_TYPE);
					if (regex != null) {
						if (inputText.matches(regex)) {
							isInputValid = validateBlackListedWords(parentPane, node, id, blackListedWords, showAlert,
									String.format("%s %s %s",
											messageBundle.getString(RegistrationConstants.BLACKLISTED_1),
											getFromLabelMap(id),
											messageBundle.getString(RegistrationConstants.BLACKLISTED_2)),
									messageBundle.getString(RegistrationConstants.BLACKLISTED_ARE),
									messageBundle.getString(RegistrationConstants.BLACKLISTED_IS));
						} else {
							isInputValid = false;
						}
					} else if ((inputText != null && !inputText.isEmpty())
							&& Arrays.asList("UIN", "RID").contains(uiSchemaDTO.getSubType())) {
						isInputValid = validateUinOrRidField(inputText, registrationDto, uiSchemaDTO);
					}

					if (!isInputValid) {
						generateInvalidValueAlert(parentPane, node.getId(),
								getFromLabelMap(label).concat(RegistrationConstants.SPACE)
										.concat(messageBundle.getString(RegistrationConstants.REG_DDC_004)),
								showAlert);
						if (isPreviousValid && !id.contains(RegistrationConstants.ON_TYPE)) {
							addInvalidInputStyleClass(parentPane, node, false);
						}
					}
				}
			}

		} catch (RuntimeException | RegBaseCheckedException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}

		if (isInputValid) {
			addValidInputStyleClass(parentPane, node);
		}
		return isInputValid;
	}

	private void addValidInputStyleClass(Pane parentPane, TextField node) {
		Label nodeLabel = (Label) parentPane.lookup("#" + node.getId() + "Label");
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
		if (parts.length < 2)
			return null;

		List<String> ageDateNodeIds = new ArrayList<String>();
		ageDateNodeIds.add("dd");
		ageDateNodeIds.add("mm");
		ageDateNodeIds.add("yyyy");
		ageDateNodeIds.add("ageField");
		return ageDateNodeIds.contains(parts[1]) ? parts[0] : null;
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

	/**
	 * Validates the Parent or Guardian's UIN or RID UI Field
	 * 
	 * @param field        the {@link TextField} to be validated
	 * @param isChild      the flag to determine whether the individual or applicant
	 *                     is child
	 * @param uinValidator the instance of {@link UinValidator} required to validate
	 *                     the UIN
	 * @param ridValidator the instance of {@link RidValidator} required to validate
	 *                     the RID
	 * @return <code>true</code> if UIN or RID is valid, else <code>false</code>
	 */
	/*
	 * public boolean validateUinOrRid(Pane parentPane, TextField uinId, TextField
	 * regId, boolean isChild) { boolean isIdValid = false;
	 * 
	 * if (isChild) { if (!uinId.isDisabled()) { if ("" == uinId.getText() ||
	 * uinId.getText().isEmpty()) { generateInvalidValueAlert(parentPane,
	 * uinId.getId(), applicationLabelBundle.getString(uinId.getId()) + " " +
	 * applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001),
	 * false);
	 * 
	 * uinId.getStyleClass().remove(RegistrationConstants.
	 * DEMOGRAPHIC_TEXTFIELD_FOCUSED); uinId.requestFocus(); } else { try {
	 * isIdValid = uinValidator.validateId(uinId.getText()); } catch
	 * (InvalidIDException invalidUinException) {
	 * 
	 * generateInvalidValueAlert(parentPane, uinId.getId(),
	 * applicationLabelBundle.getString(uinId.getId()) + " " +
	 * applicationMessageBundle.getString(RegistrationConstants.REG_DDC_004),
	 * false);
	 * 
	 * LOGGER.error("UIN VALIDATION FAILED", APPLICATION_NAME,
	 * RegistrationConstants.APPLICATION_ID, invalidUinException.getMessage() +
	 * ExceptionUtils.getStackTrace(invalidUinException));
	 * //uinId.getStyleClass().remove(RegistrationConstants.
	 * DEMOGRAPHIC_TEXTFIELD_FOCUSED); //uinId.requestFocus();
	 * addInvalidInputStyleClass(parentPane, uinId, true); }
	 * 
	 * } } else { if (getRegistrationDTOFromSession().getSelectionListDTO() == null
	 * && !regId.isDisabled()) { if ("" == regId.getText() ||
	 * regId.getText().isEmpty()) { generateInvalidValueAlert(parentPane,
	 * regId.getId(), applicationLabelBundle.getString(regId.getId()) + " " +
	 * applicationMessageBundle.getString(RegistrationConstants.REG_LGN_001),
	 * false); regId.getStyleClass().remove(RegistrationConstants.
	 * DEMOGRAPHIC_TEXTFIELD_FOCUSED); regId.requestFocus(); } else { try {
	 * isIdValid = ridValidator.validateId(regId.getText()); } catch
	 * (InvalidIDException invalidRidException) {
	 * generateInvalidValueAlert(parentPane, regId.getId(),
	 * applicationLabelBundle.getString(regId.getId()) + " " +
	 * applicationMessageBundle.getString(RegistrationConstants.REG_DDC_004),
	 * false); LOGGER.error("RID VALIDATION FAILED", APPLICATION_NAME,
	 * RegistrationConstants.APPLICATION_ID, invalidRidException.getMessage() +
	 * ExceptionUtils.getStackTrace(invalidRidException));
	 * //regId.getStyleClass().remove(RegistrationConstants.
	 * DEMOGRAPHIC_TEXTFIELD_FOCUSED); //regId.requestFocus();
	 * addInvalidInputStyleClass(parentPane, regId, true); } } } else {
	 * generateAlert(RegistrationConstants.ERROR,
	 * RegistrationUIConstants.UIN_INVALID); } } } else { isIdValid = true; }
	 * 
	 * return isIdValid; }
	 */

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
	public boolean validateSingleString(String value, String id) {

		return value.matches(getRegex(id, RegistrationUIConstants.REGEX_TYPE));
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

	/*
	 * private UiSchemaDTO getUiSchemaDTO(String id, boolean isLostUIN) {
	 * UiSchemaDTO uiSchemaDTO = null;
	 * 
	 * if (getValidationMap().containsKey(id)) { uiSchemaDTO =
	 * getValidationMap().get(id); } return uiSchemaDTO;
	 * 
	 * }
	 * 
	 * 
	 * private String getRegex(UiSchemaDTO uiSchemaDTO, String regexType) {
	 * 
	 * String regex = null;
	 * 
	 * if (uiSchemaDTO != null && uiSchemaDTO.getValidators() != null) { for
	 * (Validator validator : uiSchemaDTO.getValidators()) { if
	 * (validator.getType().equalsIgnoreCase(regexType)) { return
	 * validator.getValidator(); } } }
	 * 
	 * return regex; }
	 */

	private String getRegex(String fieldId, String regexType) {
		UiSchemaDTO uiSchemaDTO = getValidationMap().get(fieldId);
		if (uiSchemaDTO != null && uiSchemaDTO.getValidators() != null) {
			Optional<Validator> validator = uiSchemaDTO.getValidators().stream()
					.filter(v -> v.getType().equalsIgnoreCase(regexType)).findFirst();
			if (validator.isPresent())
				return validator.get().getValidator();
		}
		return null;
	}
}
