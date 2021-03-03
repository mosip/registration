package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.device.GenericBiometricsController;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BiometricFxControl extends FxControl {

	protected static final Logger LOGGER = AppConfig.getLogger(BiometricFxControl.class);
	private static final String loggerClassName = "BiometricFxControl";
	private GenericBiometricsController biometricsController;

	private String TICK_MARK = "tickMark";
	private String modality = null;
	private Map<String, List<List<String>>> modalityAttributeMap;

	public Map<String, List<List<String>>> getModalityAttributeMap() {
		return modalityAttributeMap;
	}

	public void setModalityAttributeMap(Map<String, List<List<String>>> modalityAttributeMap) {
		this.modalityAttributeMap = modalityAttributeMap;
	}

	public BiometricFxControl() {

		org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
		biometricsController = applicationContext.getBean(GenericBiometricsController.class);
		this.requiredFieldValidator = applicationContext.getBean(RequiredFieldValidator.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;

		this.node = create(null, uiSchemaDTO);

		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(), this.node);

//		setNodeMap(nodeMap);

		return this.control;
	}

	private VBox create(VBox biometricVBox, UiSchemaDTO uiSchemaDTO) {
		biometricVBox = biometricVBox == null ? new VBox() : biometricVBox;
		biometricVBox.getChildren().clear();
		biometricVBox.setSpacing(5);
		this.node = biometricVBox;
		biometricVBox.getChildren().add(getLabel(null, uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY),
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, biometricVBox.getPrefWidth()));

		modalityAttributeMap = getconfigureAndNonConfiguredBioAttributes(getModalityBasedBioAttributes(),
				uiSchemaDTO.getSubType());
		biometricVBox.getChildren().add(loadModalitesToUi(modalityAttributeMap, uiSchemaDTO.getSubType()));

//		biometricVBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
		if (!isRequired()) {

			remove(RegistrationConstants.leftHandUiAttributes);
			remove(RegistrationConstants.rightHandUiAttributes);
			remove(RegistrationConstants.twoThumbsUiAttributes);
			remove(RegistrationConstants.faceUiAttributes);
			remove(RegistrationConstants.eyesUiAttributes);
		}

		biometricVBox.setVisible(isRequired());
		biometricVBox.setManaged(true);

		return biometricVBox;

	}

	private void remove(List<String> attributes) {
		for (String attribute : attributes) {
			getRegistrationDTo().removeBiometric(this.uiSchemaDTO.getSubType(), attribute);
			getRegistrationDTo().removeBiometricException(this.uiSchemaDTO.getSubType(), attribute);

		}

	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {

		if (data != null) {
			Map<String, BiometricsDto> biometricsMap = (Map<String, BiometricsDto>) data;
			getRegistrationDTo().addAllBiometrics(uiSchemaDTO.getSubType(), biometricsMap,
					biometricsController
							.getThresholdScoreInDouble(biometricsController.getThresholdKeyByBioType(modality)),
					biometricsController.getMaxRetryByModality(modality));
			addTickMark(modality);
		} else {

			removeTickMark(modality);
		}

		refreshFields();
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), getBioAttributes(modality));
	}

	@Override
	public boolean isValid(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setListener(Node node) {
		// TODO Auto-generated method stub

	}

	private List<Entry<String, List<String>>> getModalityBasedBioAttributes() {
		return Arrays.asList(
				biometricsController.getValue(RegistrationConstants.FINGERPRINT_SLAB_LEFT,
						RegistrationConstants.leftHandUiAttributes),
				biometricsController.getValue(RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
						RegistrationConstants.rightHandUiAttributes),
				biometricsController.getValue(RegistrationConstants.FINGERPRINT_SLAB_THUMBS,
						RegistrationConstants.twoThumbsUiAttributes),
				biometricsController.getValue(RegistrationConstants.IRIS_DOUBLE,
						RegistrationConstants.eyesUiAttributes),
				biometricsController.getValue(RegistrationConstants.FACE, RegistrationConstants.faceUiAttributes));
	}

	private List<String> getBioAttributes(String modality) {
		switch (modality) {
		case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
			return RegistrationConstants.leftHandUiAttributes;

		case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
			return RegistrationConstants.rightHandUiAttributes;
		case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
			return RegistrationConstants.twoThumbsUiAttributes;
		case RegistrationConstants.IRIS_DOUBLE:
			return RegistrationConstants.eyesUiAttributes;

		case RegistrationConstants.FACE:
			return RegistrationConstants.faceUiAttributes;
		}

		return null;
	}

	protected Map<String, List<List<String>>> getconfigureAndNonConfiguredBioAttributes(
			List<Entry<String, List<String>>> entryListConstantAttributes, String subType) {

		Map<String, List<List<String>>> modalityMap = new HashMap<>();

		try {

			/** Fetch Bio Attributes */
			List<String> configuredBioAttributes = requiredFieldValidator.isRequiredBiometricField(subType,
					getRegistrationDTo());

			if (configuredBioAttributes != null && !configuredBioAttributes.isEmpty()) {
				for (Entry<String, List<String>> constantAttributes : entryListConstantAttributes) {
					List<String> nonConfigBiometrics = new LinkedList<>();
					List<String> configBiometrics = new LinkedList<>();
					String slabType = constantAttributes.getKey();
					for (String attribute : constantAttributes.getValue()) {
						if (!configuredBioAttributes.contains(attribute)) {
							nonConfigBiometrics.add(attribute);
						} else {
							configBiometrics.add(attribute);
						}
					}
					modalityMap.put(slabType, Arrays.asList(configBiometrics, nonConfigBiometrics));
				}
			}
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}

		return modalityMap;
	}

	private HBox loadModalitesToUi(Map<String, List<List<String>>> modalityMap, String subType) {

		HBox hBox = new HBox();
		hBox.setSpacing(20);
		int rowIndex = 0;

		for (Entry<String, List<List<String>>> modalityEntry : modalityMap.entrySet()) {

			if (modalityEntry.getValue().size() > 1) {
				for (String nonConfig : modalityEntry.getValue().get(1)) {

					getRegistrationDTo().removeBiometric(uiSchemaDTO.getSubType(), nonConfig);
				}
			}

			if (!modalityEntry.getValue().get(0).isEmpty()) {

				hBox.getChildren().add(getImageVBox(modalityEntry.getKey(), subType, modalityEntry.getValue().get(0),
						modalityEntry.getValue().get(1)));

				rowIndex++;
			} else {
				remove(modalityEntry.getValue().get(1));
			}

		}

		if (hBox.getChildren().size() == 0) {
			visible(this.node, false);
		}

		if (biometricsController.isApplicant(subType) && rowIndex > 1) {

			GridPane exceptionGridPane = new GridPane();

			hBox.getChildren().add(exceptionGridPane);
			exceptionGridPane.setId(uiSchemaDTO.getId() + RegistrationConstants.EXCEPTION_PHOTO);
			exceptionGridPane.add(
					getImageVBox(RegistrationConstants.EXCEPTION_PHOTO, uiSchemaDTO.getSubType(), null, null), 1,
					rowIndex);

			visible(exceptionGridPane, isBiometricExceptionAvailable());

		}

		return hBox;
	}

	private VBox getImageVBox(String modality, String subtype, List<String> configBioAttributes,
			List<String> nonConfigBioAttributes) {

		VBox vBox = new VBox();

		vBox.setAlignment(Pos.BASELINE_LEFT);
		vBox.setId(modality);

		HBox hBox = new HBox();
		hBox.setId(uiSchemaDTO.getId() + modality + RegistrationConstants.HBOX);

		Image image = null;
		if (biometricsController.isExceptionPhoto(modality)) {
			if (isBiometricExceptionAvailable()) {

				if (getRegistrationDTo().getDocuments().containsKey("proofOfException")) {
					byte[] documentBytes = getRegistrationDTo().getDocuments().get("proofOfException").getDocument();
					image = biometricsController.convertBytesToImage(documentBytes);

				}
			}
		} else {
			List<BiometricsDto> biometricsDtos = biometricsController.getBiometrics(subtype, configBioAttributes);

			if (biometricsDtos != null && !biometricsDtos.isEmpty()) {

				image = biometricsController.getBioStreamImage(subtype, modality,
						biometricsDtos.get(0).getNumOfRetries());
			}
		}

		VBox imgVBox = new VBox();

		ImageView imageView = new ImageView(image != null ? image
				: new Image(this.getClass().getResourceAsStream(biometricsController.getImageIconPath(modality))));

		imageView.setId(uiSchemaDTO.getId() + RegistrationConstants.IMAGE_VIEW + modality);
		imageView.setFitHeight(80);
		imageView.setFitWidth(85);

		imgVBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
		imgVBox.getChildren().add(imageView);

		ApplicationContext applicationContext = ApplicationContext.getInstance();
		ResourceBundle applicationBundle = applicationContext.getBundle(applicationContext.getApplicationLanguage(),
				RegistrationConstants.LABELS);
		Tooltip tooltip = new Tooltip(applicationBundle.getString(modality));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		// Tooltip.install(hBox, tooltip);
		hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
		hBox.setOnMouseExited(event -> tooltip.hide());
		hBox.getChildren().add(imgVBox);

		boolean isAllExceptions = biometricsController.isExceptionPhoto(modality) ? false : isAllExceptions(modality);

		if (image != null || isAllExceptions) {
			if (hBox.getChildren().size() == 1) {

				hBox.getChildren().add(addCompletionImg(getCompletionImgPath(isAllExceptions)));
			}
		}

		vBox.getChildren().add(hBox);

		// vBox.getChildren().add(imageView);

		vBox.setOnMouseClicked((event) -> {

			try {
				setModality(vBox.getId());
				biometricsController.init(this, uiSchemaDTO.getSubType(), vBox.getId(), configBioAttributes,
						nonConfigBioAttributes);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		vBox.setFillWidth(true);
		vBox.setMinWidth(100);

		return vBox;
	}

	private String getCompletionImgPath(boolean isAllExceptions) {
		return isAllExceptions ? RegistrationConstants.EXCLAMATION_IMG_PATH
				: RegistrationConstants.TICK_CIRICLE_IMG_PATH;

	}

	public boolean isAllExceptions(String modality) {

		if (biometricsController.isExceptionPhoto(modality)) {
			return getRegistrationDTo().getDocuments().get("proofOfException") == null;

		}
		boolean isAllExceptions = true;
		for (String configBioAttribute : modalityAttributeMap.get(modality).get(0)) {

			isAllExceptions = biometricsController.isBiometricExceptionAvailable(uiSchemaDTO.getSubType(),
					configBioAttribute) ? isAllExceptions : false;

			if (!isAllExceptions) {
				break;
			}
		}
		return isAllExceptions;
	}

	private ImageView addCompletionImg(String imgPath) {

		ImageView tickImageView = new ImageView(new Image(this.getClass().getResourceAsStream(imgPath)));

		tickImageView.setId(uiSchemaDTO.getId() + modality + TICK_MARK);
		tickImageView.setFitWidth(35);
		tickImageView.setFitHeight(35);
		return tickImageView;
	}

	private void setModality(String modality) {
		this.modality = modality;
	}

	@Override
	public void fillData(Object data) {
		// TODO Auto-generated method stub

	}

	private void addTickMark(String modality) {

		HBox hBox = getModalityHBox();
		if (hBox.getChildren().size() > 1) {

			hBox.getChildren().remove(1);
		}
		hBox.getChildren().add(addCompletionImg(getCompletionImgPath(isAllExceptions(modality))));
	}

	private HBox getModalityHBox() {
		return ((HBox) getField(uiSchemaDTO.getId() + modality + RegistrationConstants.HBOX));
	}

	private void removeTickMark(String modality) {

		HBox hBox = getModalityHBox();

		if (hBox.getChildren().size() > 1) {
			hBox.getChildren().remove(1);
		}
	}

	public void refreshExceptionMarks() {

		if (biometricsController.isExceptionPhoto(modality)) {
			DocumentDto documentDto = getRegistrationDTo().getDocuments().get("proofOfException");
			if (documentDto != null) {
				addTickMark(modality);
			} else {
				removeTickMark(modality);
			}
		} else if (isAllExceptions(modality) || !biometricsController
				.getBiometrics(uiSchemaDTO.getSubType(), modalityAttributeMap.get(modality).get(0)).isEmpty()) {
			addTickMark(modality);
		} else {
			removeTickMark(modality);
		}

		boolean exceptionPhotoReq = isBiometricExceptionAvailable();

		Node exceptionModality = getField(uiSchemaDTO.getId() + RegistrationConstants.EXCEPTION_PHOTO);

		if (exceptionModality != null) {
			exceptionModality.setVisible(exceptionPhotoReq);
		}

		fillImgView();

	}

	public void fillImgView() {

		Image image = null;
		if (biometricsController.isExceptionPhoto(modality)) {
			DocumentDto documentDto = getRegistrationDTo().getDocuments().get("proofOfException");
			if (documentDto != null) {
				image = biometricsController.convertBytesToImage(documentDto.getDocument());

			}

		} else {
			// Image modalityImage = getImage(modality);
			List<BiometricsDto> biometricsDtos = biometricsController.getBiometrics(uiSchemaDTO.getSubType(),
					modalityAttributeMap.get(modality).get(0));

			if (biometricsDtos != null && !biometricsDtos.isEmpty()) {

				image = biometricsController.getBioStreamImage(uiSchemaDTO.getSubType(), modality,
						biometricsDtos.get(0).getNumOfRetries());
			}

		}

		image = image != null ? image
				: new Image(this.getClass().getResourceAsStream(biometricsController.getImageIconPath(modality)));
		((ImageView) getField(uiSchemaDTO.getId() + RegistrationConstants.IMAGE_VIEW + modality)).setImage(image);

	}

	private boolean isBiometricExceptionAvailable() {

		boolean isException = false;
		for (Entry<String, List<List<String>>> entry : modalityAttributeMap.entrySet()) {

			for (String bioAttribute : entry.getValue().get(0)) {

				if (getRegistrationDTo().isBiometricExceptionAvailable(uiSchemaDTO.getSubType(), bioAttribute)) {
					isException = true;
					break;
				}
			}

		}

		if (!isException) {

			getRegistrationDTo().getDocuments().remove("proofOfException");
		}
		return isException;

	}

	private boolean isRequired() {
		try {
			List<String> configBioAttributes = getBioAttributes();

			return configBioAttributes != null && !configBioAttributes.isEmpty();
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Error is Required  for field : "
					+ uiSchemaDTO.getId() + " " + ExceptionUtils.getStackTrace(regBaseCheckedException));

		}

		return false;
	}

	@Override
	public void refresh() {

		LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refresh " + uiSchemaDTO.getId());

		VBox vBox = null;
		if (node instanceof VBox) {
			vBox = (VBox) node;
		} else {
			vBox = (VBox) ((FlowPane) ((GridPane) this.node).getChildren().get(0)).getChildren().get(0);
		}
		create(vBox, this.uiSchemaDTO);

	}

	private List<String> getBioAttributes() throws RegBaseCheckedException {

		return requiredFieldValidator.isRequiredBiometricField(uiSchemaDTO.getSubType(), getRegistrationDTo());

	}

	@Override
	public boolean canContinue() {

		try {

			List<String> bioAttributes = getBioAttributes();

			return biometricsController.canContinue(uiSchemaDTO.getSubType(), bioAttributes);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Error While checking on-continue  for field : " + uiSchemaDTO.getId() + " "
							+ ExceptionUtils.getStackTrace(regBaseCheckedException));

			return true;
		}
	}

	@Override
	public void selectAndSet(Object data) {

		/** Nothing to set */
	}
}
