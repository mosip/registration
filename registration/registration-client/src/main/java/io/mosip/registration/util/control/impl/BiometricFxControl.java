package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

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

		Map<String, List<List<String>>> modalityAttributeMap = getconfigureAndNonConfiguredBioAttributes(
				getModalityBasedBioAttributes(), uiSchemaDTO.getSubType());

		return loadModalitesToUi(modalityAttributeMap, uiSchemaDTO.getSubType());

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

				hBox.getChildren().add(getImageVBox(modalityEntry.getKey(), subType, modalityEntry.getValue().get(0)));

				rowIndex++;
			}

		}
		if (biometricsController.isApplicant(subType) && rowIndex > 1) {

			GridPane exceptionGridPane = new GridPane();

			hBox.getChildren().add(exceptionGridPane);
			exceptionGridPane.setId(RegistrationConstants.EXCEPTION_PHOTO);
			exceptionGridPane.add(biometricsController.getExceptionImageVBox(RegistrationConstants.EXCEPTION_PHOTO), 1,
					rowIndex);

		}

		return hBox;
	}

	private VBox getImageVBox(String modality, String subtype, List<String> configBioAttributes) {

		VBox vBox = new VBox();

		vBox.setAlignment(Pos.BASELINE_LEFT);
		vBox.setId(modality);

		// Create Label with modality
		// Label label = new Label();
		// label.setText(applicationLabelBundle.getString(modality));
		// vBox.getChildren().add(label);

		HBox hBox = new HBox();
		// hBox.setAlignment(Pos.BOTTOM_RIGHT);

		// Image modalityImage = getImage(modality);
		List<BiometricsDto> biometricsDtos = biometricsController.getBiometrics(subtype, configBioAttributes);

		Image image = null;
		if (biometricsDtos != null && !biometricsDtos.isEmpty()) {

			image = biometricsController.getBioStreamImage(subtype, modality, biometricsDtos.get(0).getNumOfRetries());
		}

		ImageView imageView = new ImageView(image != null ? image
				: new Image(this.getClass().getResourceAsStream(biometricsController.getImageIconPath(modality))));
		imageView.setFitHeight(80);
		imageView.setFitWidth(85);

		Tooltip tooltip = new Tooltip(applicationBundle.getString(modality));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		// Tooltip.install(hBox, tooltip);
		hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
		hBox.setOnMouseExited(event -> tooltip.hide());
		hBox.getChildren().add(imageView);

		boolean isAllExceptions = true;
		for (String configBioAttribute : configBioAttributes) {

			isAllExceptions = biometricsController.isBiometricExceptionAvailable(subtype, configBioAttribute)
					? isAllExceptions
					: false;

			if (!isAllExceptions) {
				break;
			}
		}
		if (image != null || isAllExceptions) {
			if (hBox.getChildren().size() == 1) {
				ImageView tickImageView;
				if (isAllExceptions) {
					tickImageView = new ImageView(
							new Image(this.getClass().getResourceAsStream(RegistrationConstants.EXCLAMATION_IMG_PATH)));
				} else {
					tickImageView = new ImageView(new Image(
							this.getClass().getResourceAsStream(RegistrationConstants.TICK_CIRICLE_IMG_PATH)));
				}
				tickImageView.setFitWidth(40);
				tickImageView.setFitHeight(40);
				hBox.getChildren().add(tickImageView);
			}
		}

		vBox.getChildren().add(hBox);

		// vBox.getChildren().add(imageView);

		vBox.setOnMouseClicked((event) -> {

//			TODO display biometric
//			displayBiometric(vBox.getId());
		});

		vBox.setFillWidth(true);
		vBox.setMinWidth(100);

		// vBox.setMinHeight(100);
		vBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
		// vBox.setBorder(new Border(
		// new BorderStroke(Color.PINK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
		// BorderWidths.FULL)));

		return vBox;
	}
}
