package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.device.BiometricsController;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BiometricFxControl extends FxControl {

	private BiometricsController biometricsController;
	private RequiredFieldValidator requiredFieldValidator;
	private static ResourceBundle applicationBundle = ApplicationContext.getInstance().getApplicationLanguageBundle();

	private String TICK_MARK = "tickMark";
	private String modality = null;
	private Map<String, List<List<String>>> modalityAttributeMap;

	public Map<String, List<List<String>>> getModalityAttributeMap() {
		return modalityAttributeMap;
	}

	public void setModalityAttributeMap(Map<String, List<List<String>>> modalityAttributeMap) {
		this.modalityAttributeMap = modalityAttributeMap;
	}

	private ApplicationContext registrationApplicationContext;

	public BiometricFxControl() {

		org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
		biometricsController = applicationContext.getBean(BiometricsController.class);
		requiredFieldValidator = applicationContext.getBean(RequiredFieldValidator.class);
		demographicDetailController = applicationContext.getBean(DemographicDetailController.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;

		VBox biometricVBox = new VBox();
		biometricVBox.setSpacing(5);

		biometricVBox.getChildren().add(getLabel(null, uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY),
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, biometricVBox.getPrefWidth()));

		biometricVBox.getChildren().add(create(uiSchemaDTO));

		biometricVBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
		this.node = biometricVBox;

		return this.control;
	}

	private HBox create(UiSchemaDTO uiSchemaDTO) {

		modalityAttributeMap = getconfigureAndNonConfiguredBioAttributes(getModalityBasedBioAttributes(),
				uiSchemaDTO.getSubType());

		return loadModalitesToUi(modalityAttributeMap, uiSchemaDTO.getSubType());

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

			if (!modalityEntry.getValue().get(0).isEmpty()) {

				hBox.getChildren().add(getImageVBox(modalityEntry.getKey(), subType, modalityEntry.getValue().get(0),
						modalityEntry.getValue().get(1)));

				rowIndex++;
			}

		}
		if (biometricsController.isApplicant(subType) && rowIndex > 1) {

			GridPane exceptionGridPane = new GridPane();

			hBox.getChildren().add(exceptionGridPane);
			exceptionGridPane.setId(uiSchemaDTO.getId() + RegistrationConstants.EXCEPTION_PHOTO);
			exceptionGridPane.add(getExceptionImageVBox(RegistrationConstants.EXCEPTION_PHOTO), 1, rowIndex);

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
		List<BiometricsDto> biometricsDtos = biometricsController.getBiometrics(subtype, configBioAttributes);

		Image image = null;
		if (biometricsDtos != null && !biometricsDtos.isEmpty()) {

			image = biometricsController.getBioStreamImage(subtype, modality, biometricsDtos.get(0).getNumOfRetries());
		}

		ImageView imageView = new ImageView(image != null ? image
				: new Image(this.getClass().getResourceAsStream(biometricsController.getImageIconPath(modality))));

		imageView.setId(uiSchemaDTO.getId() + RegistrationConstants.IMAGE_VIEW + modality);
		imageView.setFitHeight(80);
		imageView.setFitWidth(85);

		Tooltip tooltip = new Tooltip(applicationBundle.getString(modality));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		// Tooltip.install(hBox, tooltip);
		hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
		hBox.setOnMouseExited(event -> tooltip.hide());
		hBox.getChildren().add(imageView);

		boolean isAllExceptions = isAllExceptions(modality);

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

		vBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);

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
		tickImageView.setFitWidth(40);
		tickImageView.setFitHeight(40);
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

		getField(uiSchemaDTO.getId() + RegistrationConstants.EXCEPTION_PHOTO).setVisible(exceptionPhotoReq);

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

			if (isException) {
				break;
			} else {
				getRegistrationDTo().getDocuments().remove("proofOfException");
			}
		}

		return isException;

	}

	public VBox getExceptionImageVBox(String modality) {

		VBox vBox = new VBox();

		vBox.setAlignment(Pos.BASELINE_LEFT);
		vBox.setId(modality);

		HBox hBox = new HBox();
		hBox.setId(uiSchemaDTO.getId() + modality + RegistrationConstants.HBOX);
		// hBox.setAlignment(Pos.BOTTOM_RIGHT);
		Image image = null;

		if (isBiometricExceptionAvailable()) {

			if (getRegistrationDTo().getDocuments().containsKey("proofOfException")) {
				byte[] documentBytes = getRegistrationDTo().getDocuments().get("proofOfException").getDocument();
				image = biometricsController.convertBytesToImage(documentBytes);

			}
		}

		ImageView imageView = new ImageView(image != null ? image
				: new Image(this.getClass().getResourceAsStream(biometricsController.getImageIconPath(modality))));
		imageView.setId(uiSchemaDTO.getId() + RegistrationConstants.IMAGE_VIEW + modality);
		imageView.setFitHeight(80);
		imageView.setFitWidth(85);

		registrationApplicationContext = registrationApplicationContext.getInstance();
		ResourceBundle applicationLabelBundle = registrationApplicationContext.getApplicationLanguageBundle();
		Tooltip tooltip = new Tooltip(applicationLabelBundle.getString(modality));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		// Tooltip.install(hBox, tooltip);
		hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
		hBox.setOnMouseExited(event -> tooltip.hide());
		hBox.getChildren().add(imageView);

		if (image != null) {
			if (hBox.getChildren().size() == 1) {
				ImageView tickImageView = new ImageView(
						new Image(this.getClass().getResourceAsStream(RegistrationConstants.TICK_CIRICLE_IMG_PATH)));

				tickImageView.setFitWidth(40);
				tickImageView.setFitHeight(40);
				hBox.getChildren().add(tickImageView);
			}
		}

		vBox.getChildren().add(hBox);

		vBox.setOnMouseClicked((event) -> {
			try {
				setModality(vBox.getId());
				biometricsController.init(this, uiSchemaDTO.getSubType(), vBox.getId(), null, null);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		vBox.setFillWidth(true);
		vBox.setMinWidth(100);

		vBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);

		return vBox;
	}
}
