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
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Label;

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
public abstract class FxControl extends Node {

	protected static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "FxControl";

	protected UiSchemaDTO uiSchemaDTO;

	protected FxControl control;

	public Node node;

	public void setNode(Node node) {
		this.node = node;
	}

	protected AuditManagerService auditFactory;

	protected RequiredFieldValidator requiredFieldValidator;

//	protected Map<String, Object> nodeMap;
//
//	public Map<String, Object> getNodeMap() {
//		return nodeMap;
//	}
//
//	public void setNodeMap(Map<String, Object> nodeMap) {
//		this.nodeMap = nodeMap;
//	}

	public void refreshFields() {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields from fx control");
		GenericController genericController = Initialization.getApplicationContext().getBean(GenericController.class);
		genericController.refreshFields();

	}

	/**
	 * Build Error code, title and fx Element Set Listeners Set Actione events
	 * 
	 * @param uiSchemaDTO field information
	 * @param parentPane  field to be placed
	 */
	public abstract FxControl build(UiSchemaDTO uiSchemaDTO);

	/**
	 * Copy the value from source node to target nodes
	 * 
	 * @param srcNode     copy from
	 * @param targetNodes copy to
	 */
	public abstract void copyTo(Node srcNode, List<Node> targetNodes);

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
	public abstract boolean isValid(Node node);

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

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing field : " + uiSchemaDTO.getId());
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

	public UiSchemaDTO getUiSchemaDTO() {
		return uiSchemaDTO;
	}

	public abstract void setListener(Node node);

	public Node getNode() {
		return this.node;
	}

	public boolean canContinue() {

		boolean canContinue;

		if (getRegistrationDTo().getDemographics().get(this.uiSchemaDTO.getId()) != null || getRegistrationDTo()
				.getRegistrationCategory().equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_LOST)) {
			return true;
		} else {

			if (requiredFieldValidator == null) {
				requiredFieldValidator = Initialization.getApplicationContext().getBean(RequiredFieldValidator.class);
			}

			try {
				boolean isRequired = requiredFieldValidator.isRequiredField(this.uiSchemaDTO, getRegistrationDTo());
				canContinue = !isRequired;
			} catch (Exception exception) {
				LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Error While checking on-Reuired  for field : " + uiSchemaDTO.getId() + " "
								+ ExceptionUtils.getStackTrace(exception));

				canContinue = false;
			}
		}

		return canContinue;
	}

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

	protected Label getLabel(String id, String titleText, String styleClass, boolean isVisible, double prefWidth) {
		/** Field Title */
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		// label.setPrefWidth(prefWidth);
		return label;
	}

	protected Node getField(String id) {
		return node.lookup(RegistrationConstants.HASH + id);
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
			return requiredFieldValidator.isFieldVisible(schemaDTO, getRegistrationDTo());
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}

		return true;
	}

	protected void changeNodeOrientation(Node node, String langCode) {

		if (ApplicationContext.getInstance().isLanguageRightToLeft(langCode)) {
			node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		}
	}

	protected FxControl getFxControl(String fieldId) {

		return GenericController.getFxControlMap().get(fieldId);
	}

	public abstract void selectAndSet(Object data);

}
