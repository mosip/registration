package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CheckBoxFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "CheckBox FxControl";
	public static final String HASH = "#";

	public CheckBoxFxControl() {
		ApplicationContext applicationContext = Initialization.getApplicationContext();
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		VBox primaryLangVBox = create(uiSchemaDTO, "");

		HBox hBox = new HBox();
		hBox.getChildren().add(primaryLangVBox);

		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(),
				primaryLangVBox);

//		setNodeMap(nodeMap);
//		if (demographicDetailController.isLocalLanguageAvailable()
//				&& !demographicDetailController.isAppLangAndLocalLangSame()) {
//			VBox secondaryLangVBox = create(uiSchemaDTO, RegistrationConstants.LOCAL_LANGUAGE);
//
//			hBox.getChildren().addAll(secondaryLangVBox);
//		}

		this.node = hBox;

		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String languageType) {
		String fieldName = uiSchemaDTO.getId();

		/** Container holds title, fields */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + languageType + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		String titleText = (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
				? uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY)
				: uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY));

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Text Field */
		CheckBox checkBox = getCheckBox(fieldName + languageType, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth,
				languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
						&& !uiSchemaDTO.getType().equals(RegistrationConstants.SIMPLE_TYPE) ? true : false);
		setListener(checkBox);

		simpleTypeVBox.getChildren().add(checkBox);

		return simpleTypeVBox;
	}

	private CheckBox getCheckBox(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {
		CheckBox checkBox = new CheckBox(titleText);
		checkBox.setId(id);
		// checkBox.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		checkBox.setPrefWidth(prefWidth);
		checkBox.setDisable(isDisable);

		return checkBox;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getData() {
		// TODO Auto-generated method stub
		return null;
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
		CheckBox checkBox = (CheckBox) node;
		checkBox.selectedProperty().addListener((options, oldValue, newValue) -> {
			if (uiSchemaDTO != null) {
				LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						"Invoking external action handler for .... " + uiSchemaDTO.getId());
				handleCopyAction(node.getId(), uiSchemaDTO.getChangeAction());
			}
			// Group level visibility listeners
			refreshFields();
		});

	}

	private void handleCopyAction(String sourceId, String handleStr) {
		if (handleStr != null) {
			String[] parts = handleStr.split(":");
			handle(sourceId, parts.length > 1 ? parts[1].split(",") : new String[] {});
		}
	}

	private void handle(String source, String[] args) {
		Node copyEnabledFlagNode = this.node.lookup(HASH.concat(source));
		boolean enabled = ((CheckBox) copyEnabledFlagNode).isSelected() ? true : false;

		for (String arg : args) {
			String[] parts = arg.split("=");
			if (parts.length == 2) {
				if (getFxControl(parts[0]) != null && getFxControl(parts[1]) != null) {
					Pane fromParentNode = (Pane) getFxControl(parts[0]).getNode();
					Pane toParentNode = (Pane) getFxControl(parts[1]).getNode();

					Node fromNode = fromParentNode.lookup(HASH.concat(parts[0]));
					Node toNode = toParentNode.lookup(HASH.concat(parts[1]));

					if (!isValidNode(fromNode) || !isValidNode(toNode))
						continue;

					if (fromNode instanceof TextField) {
						copy(fromParentNode, (TextField) fromNode, (TextField) toNode, enabled);
					} else if (fromNode instanceof ComboBox) {
						copy((ComboBox) fromNode, (ComboBox) toNode, enabled);
					}
				}
			}
		}
	}

	private void copy(Pane parentPane, TextField fromNode, TextField toNode, boolean isEnabled) {
		if (isEnabled) {
			toNode.setText(fromNode.getText());
			toNode.setDisable(true);
			Node localLangNode = parentPane.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
			if (isValidNode(localLangNode) && localLangNode instanceof TextField) {
				((TextField) localLangNode).setText(fromNode.getText());
				localLangNode.setDisable(true);
			}
		} else {
			toNode.setDisable(false);
			Node localLangNode = parentPane.lookup(HASH.concat(toNode.getId()).concat("LocalLanguage"));
			if (isValidNode(localLangNode) && localLangNode instanceof TextField) {
				localLangNode.setDisable(false);
			}
		}
	}

	private void copy(ComboBox fromNode, ComboBox toNode, boolean isEnabled) {
		if (isEnabled) {
			toNode.getSelectionModel().select(fromNode.getSelectionModel().getSelectedItem());
			toNode.setDisable(true);
		} else {
			toNode.setDisable(false);
		}
	}

	private boolean isValidNode(Node node) {
		return (node != null && node.isVisible()) ? true : false;
	}

	@Override
	public void fillData(Object data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectAndSet(Object data) {
		// TODO Auto-generated method stub

	}

}
