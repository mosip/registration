package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DropDownFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);
	private static final String loggerClassName = "DropDownFxControl";
	private Validations validation;
	private DemographicChangeActionHandler demographicChangeActionHandler;
	private MasterSyncService masterSyncService;
	private String languageType;

	public DropDownFxControl() {

		ApplicationContext applicationContext = Initialization.getApplicationContext();

		validation = applicationContext.getBean(Validations.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);

		masterSyncService = applicationContext.getBean(MasterSyncService.class);

	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;

		this.control = this;

		HBox hBox = new HBox();

		VBox primaryLangVBox = create(uiSchemaDTO, "");
		hBox.getChildren().add(primaryLangVBox);
//		HBox.setHgrow(primaryLangVBox, Priority.ALWAYS);

		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(),
				primaryLangVBox);
//		if (demographicDetailController.isLocalLanguageAvailable()
//				&& !demographicDetailController.isAppLangAndLocalLangSame()) {

		languageType = RegistrationConstants.LOCAL_LANGUAGE;
		VBox secondaryLangVBox = create(uiSchemaDTO, RegistrationConstants.LOCAL_LANGUAGE);

//			Region region = new Region();
//			HBox.setHgrow(region, Priority.ALWAYS);

//			HBox.setHgrow(secondaryLangVBox, Priority.ALWAYS);
		hBox.getChildren().add(secondaryLangVBox);
//		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getLocalLanguage(),
//				secondaryLangVBox);

//		}
//
//		setNodeMap(nodeMap);
		this.node = hBox;
		setListener(node);
		return this.control;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO, String languageType) {

		String fieldName = uiSchemaDTO.getId();

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + languageType + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);
		String titleText = (languageType.equals(RegistrationConstants.LOCAL_LANGUAGE)
				? uiSchemaDTO.getLabel().get(RegistrationConstants.SECONDARY)
				: uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY)) + mandatorySuffix;

		double prefWidth = 500;

		/** Title label */
		Label fieldTitle = getLabel(fieldName + languageType + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, prefWidth);
		simpleTypeVBox.getChildren().add(fieldTitle);

		/** comboBox Field */
		ComboBox<GenericDto> comboBox = getComboBox(fieldName + languageType, titleText,
				RegistrationConstants.DOC_COMBO_BOX, prefWidth,
				languageType.equals(RegistrationConstants.LOCAL_LANGUAGE) ? true : false);
		simpleTypeVBox.getChildren().add(comboBox);

		/** Validation message (Invalid/wrong,,etc,.) */
		Label validationMessage = getLabel(fieldName + languageType + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, prefWidth);
		simpleTypeVBox.getChildren().add(validationMessage);

		changeNodeOrientation(simpleTypeVBox, languageType);
		return simpleTypeVBox;
	}

	private <T> ComboBox<GenericDto> getComboBox(String id, String titleText, String stycleClass, double prefWidth,
			boolean isDisable) {
		ComboBox<GenericDto> field = new ComboBox<GenericDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		VBox vbox = new VBox();
		field.setId(id);
		// field.setPrefWidth(prefWidth);
		field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_COMBOBOX);
		field.setConverter((StringConverter<GenericDto>) uiRenderForComboBox);

		return field;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiSchemaDTO.getId());
		ComboBox<GenericDto> localComboBox = (ComboBox<GenericDto>) getField(
				uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);

//		String primaryVal = appComboBox != null && appComboBox.getValue() != null ? appComboBox.getValue().getName()
//				: null;
//		String localVal = localComboBox != null && localComboBox.getValue() != null ? localComboBox.getValue().getName()
//				: null;
//
//		getRegistrationDTo().addDemographicField(uiSchemaDTO.getId(), getAppLanguage(), primaryVal, getLocalLanguage(),
//				localVal);
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid(Node node) {
		return true;
	}

	@Override
	public void setListener(Node node) {

//		if (RegistrationConstants.LOCAL_LANGUAGE.equalsIgnoreCase(languageType)) {
//			FXUtils.getInstance().populateLocalComboBox((Pane) getNode(), (ComboBox<?>) getField(uiSchemaDTO.getId()),
//					(ComboBox<?>) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE));
//		}

		((ComboBox<GenericDto>) getField(uiSchemaDTO.getId())).getSelectionModel().selectedItemProperty()
				.addListener((options, oldValue, newValue) -> {
					if (isValid(getField(uiSchemaDTO.getId()))) {

						FXUtils.getInstance().toggleUIField((Pane) getNode(),
								uiSchemaDTO.getId() + RegistrationConstants.LABEL, true);

						FXUtils.getInstance().toggleUIField((Pane) getNode(),
								uiSchemaDTO.getId() + RegistrationConstants.MESSAGE, false);
//						if (!demographicDetailController.isAppLangAndLocalLangSame()) {
						ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiSchemaDTO.getId());
						ComboBox<GenericDto> localComboBox = (ComboBox<GenericDto>) getField(
								uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE);

						FXUtils.getInstance().selectComboBoxValueByCode(localComboBox, appComboBox.getValue(),
								appComboBox);
						FXUtils.getInstance().toggleUIField((Pane) getNode(), uiSchemaDTO.getId()
								+ RegistrationConstants.LOCAL_LANGUAGE + RegistrationConstants.LABEL, true);
						FXUtils.getInstance().toggleUIField((Pane) getNode(), uiSchemaDTO.getId()
								+ RegistrationConstants.LOCAL_LANGUAGE + RegistrationConstants.MESSAGE, false);
//						}

						setData(null);
						Map<Integer, FxControl> hirearchyMap = GenericController.locationMap
								.get(uiSchemaDTO.getGroup());

						if (hirearchyMap != null && !hirearchyMap.isEmpty()) {
							for (Entry<Integer, FxControl> entry : hirearchyMap.entrySet()) {

								if (entry.getValue() == this.control) {

									Entry<Integer, FxControl> nextEntry = GenericController.locationMap
											.get(uiSchemaDTO.getGroup()).higherEntry(entry.getKey());

									if (nextEntry != null) {

										clearHirearchy(nextEntry.getKey());

										FxControl nextLocation = nextEntry.getValue();

										Map<String, Object> data = new LinkedHashMap<>();

										ComboBox<GenericDto> comboBox = (ComboBox<GenericDto>) getField(
												uiSchemaDTO.getId());

										if (comboBox != null && comboBox.getValue() != null) {
											GenericDto genericDto = comboBox.getValue();

											data.put(RegistrationConstants.PRIMARY,
													getLocations(genericDto.getCode(), genericDto.getLangCode()));
//											data.put(RegistrationConstants.SECONDARY, getLocations(genericDto.getCode(),
//													io.mosip.registration.context.ApplicationContext.localLanguage()));
										}

										nextLocation.fillData(data);

									}

									break;
								}
							}
						}

						if (uiSchemaDTO != null) {
							LOGGER.info(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
									"Invoking external action handler for .... " + uiSchemaDTO.getId());
							demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
									uiSchemaDTO.getChangeAction());
						}
						// Group level visibility listeners
						refreshFields();
					}
				});
	}

	private void clearHirearchy(Integer hirearchynumber) {

		if (hirearchynumber != null) {
			FxControl fxControl = GenericController.locationMap.get(uiSchemaDTO.getGroup()).get(hirearchynumber);

			ComboBox<GenericDto> comboBox = (ComboBox<GenericDto>) getField(fxControl.getNode(),
					fxControl.getUiSchemaDTO().getId());

			comboBox.getItems().clear();

			Entry<Integer, FxControl> nextEntry = GenericController.locationMap.get(uiSchemaDTO.getGroup())
					.higherEntry(hirearchynumber);

			if (nextEntry != null) {
				clearHirearchy(nextEntry.getKey());
			}
		}

	}

	private Node getField(Node fieldParentNode, String id) {

		return fieldParentNode.lookup(RegistrationConstants.HASH + id);

	}

	private List<GenericDto> getLocations(String hirearchCode, String langCode) {

		List<GenericDto> locations = null;
		try {
			locations = masterSyncService.findProvianceByHierarchyCode(hirearchCode, langCode);
		} catch (RegBaseCheckedException e) {
			// TODO
			e.printStackTrace();
		} finally {
			if (locations.isEmpty()) {
				GenericDto lC = new GenericDto();
				lC.setCode(RegistrationConstants.AUDIT_DEFAULT_USER);
				lC.setName(RegistrationConstants.AUDIT_DEFAULT_USER);
				lC.setLangCode(langCode);
				locations.add(lC);
			}
		}

		return locations;

	}

	@Override
	public void fillData(Object data) {

		if (data != null) {

			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;

			setItems((ComboBox<GenericDto>) getField(uiSchemaDTO.getId()), val.get(RegistrationConstants.PRIMARY));
			setItems((ComboBox<GenericDto>) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE),
					val.get(RegistrationConstants.SECONDARY));

		}
	}

	private void setItems(ComboBox<GenericDto> comboBox, List<GenericDto> val) {
		if (comboBox != null && val != null && !val.isEmpty()) {
			comboBox.getItems().clear();
			comboBox.getItems().addAll(val);
		}
	}

	@Override
	public void selectAndSet(Object data) {
		if (data != null) {

			if (data instanceof List) {

				List<SimpleDto> list = (List<SimpleDto>) data;

				for (SimpleDto simpleDto : list) {

					if (simpleDto.getLanguage().equalsIgnoreCase(
							io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage())) {

						selectItem((ComboBox<GenericDto>) getField(uiSchemaDTO.getId()), simpleDto.getValue());

					}
//						else if (simpleDto.getLanguage().equalsIgnoreCase(
//							io.mosip.registration.context.ApplicationContext.getInstance().getLocalLanguage())) {
//
//						selectItem(
//								(ComboBox<GenericDto>) getField(
//										uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE),
//								simpleDto.getValue());
//					}
				}

			} else if (data instanceof String) {

				selectItem((ComboBox<GenericDto>) getField(uiSchemaDTO.getId()), (String) data);
				selectItem((ComboBox<GenericDto>) getField(uiSchemaDTO.getId() + RegistrationConstants.LOCAL_LANGUAGE),
						(String) data);
			}
		}
	}

	private void selectItem(ComboBox<GenericDto> field, String val) {
		if (field != null && val != null && !val.isEmpty()) {
			for (GenericDto genericDto : field.getItems()) {

				if (genericDto.getCode().equalsIgnoreCase(val)) {
					field.getSelectionModel().select(genericDto);
					break;
				}
			}
		}
	}
}
