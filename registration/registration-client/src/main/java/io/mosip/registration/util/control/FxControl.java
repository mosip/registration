/**
 * 
 */
package io.mosip.registration.util.control;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 
 * Control Type will give high level controls for fields TextField,CheckBox,
 * DropDown,DropDown, Buttons, document type, Biometric Type
 * 
 * It also provides a features to copy,disable and visible
 * 
 * @author YASWANTH S
 *
 */
public abstract class FxControl  {

	protected static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "FxControl";

	protected UiSchemaDTO uiSchemaDTO;
	protected FxControl control;
	public Node node;

	protected AuditManagerService auditFactory;
	protected RequiredFieldValidator requiredFieldValidator;


	/**
	 * Build Error code, title and fx Element Set Listeners Set Actione events
	 * 
	 * @param uiSchemaDTO field information
	 */
	public abstract FxControl build(UiSchemaDTO uiSchemaDTO);

	/**
	 *
	 * @param node
	 */
	public abstract void setListener(Node node);

	/**
	 * 
	 * Set Data into Registration DTO
	 * 
	 * @param data value
	 */
	public abstract void setData(Object data);

	/**
	 * 
	 * Fill Data into fx element
	 * 
	 * @param data value
	 */
	public abstract void fillData(Object data);

	/**
	 * Get Value from fx element
	 * 
	 * @return Value
	 */
	public abstract Object getData();

	/**
	 * Check value is valid or not
	 * 
	 * @return boolean is valid or not
	 */
	//public abstract boolean isValid(Node node);

	/**
	 *
	 * @param data
	 */
	public abstract void selectAndSet(Object data);

	/**
	 * Check value is valid or not
	 *
	 * @return boolean is valid or not
	 */
	public abstract boolean isValid();

	/**
	 *
	 * @param langCode
	 * @return
	 */
	public abstract List<GenericDto> getPossibleValues(String langCode);


	/**
	 * Disable the field
	 */
	public void disable(Node node, boolean isDisable) {

		node.setDisable(isDisable);

	}

	/**
	 * Refresh the field
	 */
	public void refresh() {
		if (isFieldVisible(uiSchemaDTO)) {
			visible(this.node, true);
			this.node.setManaged(true);
		} else {
			visible(this.node, false);
			this.node.setManaged(false);
		}
	}

	/**
	 * Hide the field
	 */
	public void visible(Node node, boolean isVisible) {

		node.setVisible(isVisible);

	}

	/**
	 *
	 */
	public void refreshFields() {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields from fx control");
		GenericController genericController = Initialization.getApplicationContext().getBean(GenericController.class);
		genericController.refreshFields();
	}

	/**
	 *
	 * @return
	 */
	public boolean canContinue() {
		//field is not visible, ignoring valid value and isRequired check
		if(!isFieldVisible(this.uiSchemaDTO)) {
			return true;
		}

		if (requiredFieldValidator == null) {
			requiredFieldValidator = Initialization.getApplicationContext().getBean(RequiredFieldValidator.class);
		}

		try {
			boolean isValid = isValid();
			LOGGER.debug("canContinue check on field  : {}, status {} : " ,uiSchemaDTO.getId(), isValid);

			if(isValid) //empty values should be ignored, its fxControl's responsibility
				return true;

			//required and with valid value
			if(isValid && requiredFieldValidator.isRequiredField(this.uiSchemaDTO, getRegistrationDTo()))
				return true;

			if(getRegistrationDTo().getRegistrationCategory().equals(RegistrationConstants.PACKET_TYPE_UPDATE)
				&& !getRegistrationDTo().getUpdatableFields().contains(this.uiSchemaDTO.getId()) && !isValid) {
				LOGGER.error("canContinue check on, {} is non-updatable ignoring", uiSchemaDTO.getId());
				return true;
			}

		} catch (Exception exception) {
			LOGGER.error("Error checking RequiredOn for field : " + uiSchemaDTO.getId(), exception);
		}
		return false;
	}

	/**
	 *
	 * @param schema
	 * @return
	 */
	protected String getMandatorySuffix(UiSchemaDTO schema) {
		String mandatorySuffix = RegistrationConstants.EMPTY;
		RegistrationDTO registrationDTO = getRegistrationDTo();
		String categeory = registrationDTO.getRegistrationCategory();
		switch (categeory) {
		case RegistrationConstants.PACKET_TYPE_UPDATE:
			if (registrationDTO.getUpdatableFields().contains(schema.getId())) {
				mandatorySuffix = schema.isRequired() ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
			}
			break;

		case RegistrationConstants.PACKET_TYPE_NEW:
			mandatorySuffix = schema.isRequired() ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
			break;
		}
		return mandatorySuffix;
	}

	/**
	 *
	 * @param id
	 * @param titleText
	 * @param styleClass
	 * @param isVisible
	 * @param prefWidth
	 * @return
	 */
	protected Label getLabel(String id, String titleText, String styleClass, boolean isVisible, double prefWidth) {
		/** Field Title */
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		label.setWrapText(true);
		// label.setPrefWidth(prefWidth);
		return label;
	}

	protected RegistrationDTO getRegistrationDTo() {
		RegistrationDTO registrationDTO = null;
		if (SessionContext.map() != null || !SessionContext.map().isEmpty()) {
			registrationDTO = (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
		}
		return registrationDTO;
	}

	protected boolean isFieldVisible(UiSchemaDTO schemaDTO) {
		if (requiredFieldValidator == null) {
			requiredFieldValidator = Initialization.getApplicationContext().getBean(RequiredFieldValidator.class);
		}
		try {
			boolean isVisibleAccordingToSpec = requiredFieldValidator.isFieldVisible(schemaDTO, getRegistrationDTo());

			switch (getRegistrationDTo().getRegistrationCategory()) {
				case RegistrationConstants.PACKET_TYPE_UPDATE:
					return (getRegistrationDTo().getUpdatableFields().contains(schemaDTO.getId())) ? isVisibleAccordingToSpec : false;
				case RegistrationConstants.PACKET_TYPE_NEW: return isVisibleAccordingToSpec;
				case RegistrationConstants.PACKET_TYPE_LOST: return isVisibleAccordingToSpec;
			}
		} catch (Exception exception) {
			LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return true;
	}

	protected void changeNodeOrientation(Node node, String langCode) {

		if (ApplicationContext.getInstance().isLanguageRightToLeft(langCode)) {
			node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		}
	}

	protected void addValidationMessage(VBox vBox, String id, String langCode, String styleClass, boolean isVisible) {
		Label validationMessage = getLabel(id + langCode + RegistrationConstants.MESSAGE, null,
				styleClass, isVisible, 0);
		validationMessage.setWrapText(false);
		vBox.getChildren().add(validationMessage);

		HBox validationHBox = new HBox();
		validationHBox.setSpacing(20);
		validationHBox.getChildren().add(validationMessage);
		validationHBox.setStyle("-fx-background-color:WHITE");
		vBox.getChildren().add(validationHBox);
	}

	protected Node getField(String id) {
		return node.lookup(RegistrationConstants.HASH + id);
	}

	protected FxControl getFxControl(String fieldId) {
		return GenericController.getFxControlMap().get(fieldId);
	}

	public UiSchemaDTO getUiSchemaDTO() {
		return uiSchemaDTO;
	}

	public Node getNode() {
		return this.node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

}
