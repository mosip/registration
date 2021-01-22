package io.mosip.registration.util.control;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.UiSchemaDTO;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public abstract class AbstractControlType {

	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "AbstractControlType";

	protected UiSchemaDTO uiSchemaDTO;

	protected AbstractControlType fieldType;

	protected Pane parentPane;

	public void refreshFields() {

	}

	private void setFieldChangeListener(Node node) {
		node.addEventHandler(Event.ANY, event -> {
			if (validateFieldValue(node)) {

//				Object currentValue = this.getData();

				// TODO Translate and set secondary language

				if (uiSchemaDTO != null) {
					LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
							"Invoking external action handler for .... " + uiSchemaDTO.getId());
					actionHandle(parentPane, uiSchemaDTO.getId(), uiSchemaDTO.getChangeAction());
				}
				// Group level visibility listeners
				refreshFields();

			}
		});
	}

	public abstract void actionHandle(Pane parentPane, String fieldId, String changeAction);

	private boolean validateFieldValue(Node field) {
		if (field == null) {
			LOGGER.warn(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Field not found in demographic screen");
			return false;
		}

		// TODO
//		if (this.isValid()) {
//			// Hide Error Label
//
//		} else {
//			// Show Error Label
//		}

		return false;
	}

}
