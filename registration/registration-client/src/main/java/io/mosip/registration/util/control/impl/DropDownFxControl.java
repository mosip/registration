package io.mosip.registration.util.control.impl;

import java.util.*;
import java.util.Map.Entry;

import io.mosip.registration.dao.MasterSyncDao;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.entity.Location;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DropDownFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "DropDownFxControl";
	private int hierarchyLevel;
	private Validations validation;
	private DemographicChangeActionHandler demographicChangeActionHandler;
	private MasterSyncService masterSyncService;
	private MasterSyncDao masterSyncDao;

	public DropDownFxControl() {
		ApplicationContext applicationContext = Initialization.getApplicationContext();
		validation = applicationContext.getBean(Validations.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
		masterSyncDao  = applicationContext.getBean(MasterSyncDao.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;
		this.node = create(uiSchemaDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		//As subType in UI Spec is defined in english we consider using eng to fill initial dropdown
		if(GenericController.hierarchyLevels.get("eng").containsValue(uiSchemaDTO.getSubType())) {
			TreeMap<Integer, String> groupFields = GenericController.currentHierarchyMap.getOrDefault(uiSchemaDTO.getGroup(), new TreeMap<>());
			for (Entry<Integer, String> entry : GenericController.hierarchyLevels.get("eng").entrySet()) {
				if (entry.getValue().equals(uiSchemaDTO.getSubType())) {
					this.hierarchyLevel = entry.getKey();
					groupFields.put(entry.getKey(), uiSchemaDTO.getId());
					GenericController.currentHierarchyMap.put(uiSchemaDTO.getGroup(), groupFields);
					break;
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),
				getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

		//clears & refills items
		fillData(data);
		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String langCode) {
		String fieldName = uiSchemaDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		/** Title label */
		Label fieldTitle = getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getWidth());
		simpleTypeVBox.getChildren().add(fieldTitle);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiSchemaDTO.getLabel().get(lCode));
		});

		String titleText = String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiSchemaDTO);
		ComboBox<GenericDto> comboBox = getComboBox(fieldName, titleText, RegistrationConstants.DOC_COMBO_BOX,
				simpleTypeVBox.getPrefWidth(), false);
		simpleTypeVBox.getChildren().add(comboBox);

		comboBox.setOnMouseExited(event -> {
			getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE).setVisible(false);
			comboBox.getTooltip().hide();
		});

		comboBox.setOnMouseEntered((event -> {
			getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE).setVisible(true);

		}));

		setListener(comboBox);

		fieldTitle.setText(titleText);
		comboBox.setPromptText(fieldTitle.getText());
		simpleTypeVBox.getChildren().add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getPrefWidth()));

		changeNodeOrientation(simpleTypeVBox, langCode);

		return simpleTypeVBox;
	}


	public List<GenericDto> getPossibleValues(String langCode) {
		boolean isHierarchical = false;
		String fieldSubType = uiSchemaDTO.getSubType();

		if(GenericController.currentHierarchyMap.containsKey(uiSchemaDTO.getGroup())) {
			isHierarchical = true;
			Entry<Integer, String> parentEntry = GenericController.currentHierarchyMap.get(uiSchemaDTO.getGroup())
					.lowerEntry(this.hierarchyLevel);
			if(parentEntry == null) { //first parent
				parentEntry = GenericController.hierarchyLevels.get(langCode).lowerEntry(this.hierarchyLevel);
				Assert.assertNotNull(parentEntry);
				List<Location> locations = masterSyncDao.getLocationDetails(parentEntry.getValue(), langCode);
				fieldSubType = locations != null && !locations.isEmpty() ? locations.get(0).getCode() : null;
			}
			else {
				FxControl fxControl = GenericController.getFxControlMap().get(parentEntry.getValue());
				Node comboBox = getField(fxControl.getNode(), parentEntry.getValue());
				GenericDto selectedItem = comboBox != null ?
						((ComboBox<GenericDto>) comboBox).getSelectionModel().getSelectedItem() : null;
				fieldSubType = selectedItem != null ? selectedItem.getCode() : null;
				if(fieldSubType == null)
					return Collections.EMPTY_LIST;
			}
		}
		return masterSyncService.getFieldValues(fieldSubType, langCode, isHierarchical);
	}

	private <T> ComboBox<GenericDto> getComboBox(String id, String titleText, String stycleClass, double prefWidth,
			boolean isDisable) {
		ComboBox<GenericDto> field = new ComboBox<GenericDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		field.setId(id);
		// field.setPrefWidth(prefWidth);
		field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_COMBOBOX);
		field.setConverter((StringConverter<GenericDto>) uiRenderForComboBox);
		return field;
	}


	@Override
	public void setData(Object data) {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiSchemaDTO.getId());
		List<SimpleDto> values = new ArrayList<SimpleDto>();
		String selectedCode = appComboBox.getSelectionModel().getSelectedItem().getCode();
		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
			Optional<GenericDto> result = getPossibleValues(langCode).stream()
					.filter(b -> b.getCode().equals(selectedCode)).findFirst();
			if (result.isPresent()) {
				SimpleDto simpleDto = new SimpleDto(langCode, result.get().getName());
				values.add(simpleDto);
			}
		}

		getRegistrationDTo().addDemographicField(uiSchemaDTO.getId(), values);
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid() {
		//TODO check if its lostUIN, then no validation required
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiSchemaDTO.getId());
		return appComboBox != null && appComboBox.getSelectionModel().getSelectedItem() != null;
	}

	@Override
	public boolean isEmpty() {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiSchemaDTO.getId());
		return appComboBox == null || appComboBox.getSelectionModel().getSelectedItem() == null;
	}

	@Override
	public void setListener(Node node) {
		ComboBox<GenericDto> fieldComboBox = (ComboBox<GenericDto>) node;
		fieldComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			displayFieldLabel();
			if (isValid()) {

				List<String> toolTipText = new ArrayList<>();
				String selectedCode = fieldComboBox.getSelectionModel().getSelectedItem().getCode();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					Optional<GenericDto> result = getPossibleValues(langCode).stream()
							.filter(b -> b.getCode().equals(selectedCode)).findFirst();
					if (result.isPresent()) {
						SimpleDto simpleDto = new SimpleDto(langCode, result.get().getName());

						toolTipText.add(result.get().getName());
					}
				}

				Label messageLabel = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(String.join(RegistrationConstants.SLASH, toolTipText));

				setData(null);
				refreshNextHierarchicalFxControls();
				demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),	uiSchemaDTO.getChangeAction());
				// Group level visibility listeners
				refreshFields();
			} else {
				((Label) getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE)).setVisible(false);
				FXUtils.getInstance().toggleUIField((Pane) getNode(),
						uiSchemaDTO.getId() + RegistrationConstants.MESSAGE, true);
			}
		});
	}

	private void refreshNextHierarchicalFxControls() {
		if(GenericController.currentHierarchyMap.containsKey(uiSchemaDTO.getGroup())) {
			Entry<Integer, String> nextEntry = GenericController.currentHierarchyMap.get(uiSchemaDTO.getGroup())
					.higherEntry(this.hierarchyLevel);

			while (nextEntry != null) {
				FxControl fxControl = GenericController.getFxControlMap().get(nextEntry.getValue());
				Map<String, Object> data = new LinkedHashMap<>();
				data.put(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),
						fxControl.getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

				//clears & refills items
				fxControl.fillData(data);
				nextEntry = GenericController.currentHierarchyMap.get(uiSchemaDTO.getGroup())
						.higherEntry(nextEntry.getKey());
			}
		}
	}

	private void displayFieldLabel() {
		FXUtils.getInstance().toggleUIField((Pane) getNode(), uiSchemaDTO.getId() + RegistrationConstants.LABEL,
				true);
		Label label = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.LABEL);
		label.getStyleClass().add("demoGraphicFieldLabelOnType");
		label.getStyleClass().remove("demoGraphicFieldLabel");
		FXUtils.getInstance().toggleUIField((Pane) getNode(), uiSchemaDTO.getId() + RegistrationConstants.MESSAGE, false);
	}



	private Node getField(Node fieldParentNode, String id) {
		return fieldParentNode.lookup(RegistrationConstants.HASH + id);
	}

	@Override
	public void fillData(Object data) {

		if (data != null) {

			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;

			List<GenericDto> items = val.get(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

			if (items != null && !items.isEmpty()) {
				setItems((ComboBox<GenericDto>) getField(uiSchemaDTO.getId()), items);
			}

		}
	}

	private void setItems(ComboBox<GenericDto> comboBox, List<GenericDto> val) {
		if (comboBox != null && val != null && !val.isEmpty()) {
			comboBox.getItems().clear();
			comboBox.getItems().addAll(val);

			new ComboBoxAutoComplete<GenericDto>(comboBox);
			
			comboBox.hide();

		}
	}

	@Override
	public void selectAndSet(Object data) {
		if (data != null) {

			if (data instanceof List) {

				List<SimpleDto> list = (List<SimpleDto>) data;

				for (SimpleDto simpleDto : list) {

					if (getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)
							.equalsIgnoreCase(simpleDto.getLanguage())) {

						selectItem((ComboBox<GenericDto>) getField(uiSchemaDTO.getId()), simpleDto.getValue());

						break;
					}
				}

			} else if (data instanceof String) {

				selectItem((ComboBox<GenericDto>) getField(uiSchemaDTO.getId()), (String) data);
			}
		}
	}

	private void selectItem(ComboBox<GenericDto> field, String val) {
		if (field != null && val != null && !val.isEmpty()) {
			for (GenericDto genericDto : field.getItems()) {
				if (genericDto.getCode().equals(val)) {
					field.getSelectionModel().select(genericDto);
					break;
				}
			}
		}
	}
}
