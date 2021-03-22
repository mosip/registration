package io.mosip.registration.util.control.impl;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.util.common.Modality;
import javafx.scene.layout.*;
import org.apache.commons.collections4.ListUtils;

import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.device.GenericBiometricsController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;

public class BiometricFxControl extends FxControl {

	protected static final Logger LOGGER = AppConfig.getLogger(BiometricFxControl.class);
	private static final String APPLICANT_SUBTYPE = "applicant";

	private GenericBiometricsController biometricsController;
	private IdentitySchemaService identitySchemaService;
	private Modality currentModality;
	private Map<Modality, List<List<String>>> modalityAttributeMap = new HashMap<>();


	public BiometricFxControl() {
		org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
		this.biometricsController = applicationContext.getBean(GenericBiometricsController.class);
		this.identitySchemaService = applicationContext.getBean(IdentitySchemaService.class);
		this.requiredFieldValidator = applicationContext.getBean(RequiredFieldValidator.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;
		this.node = create();
		return this.node == null ? null : this.control;
	}

	@Override
	public void setData(Object data) {
		if (data != null) {
			getRegistrationDTo().addAllBiometrics(uiSchemaDTO.getSubType(),
					(Map<String, BiometricsDto>) data,
					biometricsController.getThresholdScoreInDouble(biometricsController.getThresholdKeyByBioType(currentModality)),
					biometricsController.getMaxRetryByModality(currentModality));
		}

		//refresh();
		// TODO - remove EOP document if there are no biometric exception
	}

	@Override
	public void fillData(Object data) {

	}

	@Override
	public Object getData() {
		List<String> requiredAttributes = getRequiredBioAttributes(uiSchemaDTO.getSubType());
		List<String> configBioAttributes = ListUtils.intersection(requiredAttributes,
				Modality.getAllBioAttributes(currentModality));
		return getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), configBioAttributes);
	}

	@Override
	public void setListener(Node node) {

	}

	@Override
	public void selectAndSet(Object data) {

	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	private GridPane create() {
		String fieldName = this.uiSchemaDTO.getId();
		List<HBox> modalityList = getModalityButtons();

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
					labels.add(this.uiSchemaDTO.getLabel().get(langCode));
				});

		Label label = new Label();
		label.setText(String.join(RegistrationConstants.SLASH, labels));
		label.getStyleClass().add(RegistrationConstants.QUALITY_BOX_LABEL);

		GridPane gridPane = createGridPane();
		gridPane.setId(fieldName);
		gridPane.add(label,1,0);
		GridPane.setHalignment(label, HPos.CENTER);
		GridPane.setValignment(label, VPos.TOP);

		Node modalityListingNode = null;
		switch (getBiometricFieldLayout()) {
			case "compact":
				modalityListingNode = new HBox();
				modalityListingNode.setId(uiSchemaDTO.getId()+"_listing");
				//((HBox)modalityListingNode).setSpacing(5);
				((HBox)modalityListingNode).getChildren().addAll(modalityList);
				gridPane.add(modalityListingNode,1,1);
				break;
			default:
				modalityListingNode = new VBox();
				modalityListingNode.setId(uiSchemaDTO.getId()+"_listing");
				//((VBox)modalityListingNode).setSpacing(5);
				((VBox)modalityListingNode).getChildren().addAll(modalityList);
				gridPane.add(modalityListingNode,0,1);
				Parent captureDetails = null;
				try {
					captureDetails = BaseController.loadWithNewInstance(getClass().getResource("/fxml/BiometricsCapture.fxml"),
							this.biometricsController);
					GridPane.setHalignment(captureDetails, HPos.CENTER);
					GridPane.setValignment(captureDetails, VPos.TOP);
					gridPane.add(captureDetails,1,1);
				} catch (IOException e) {
					LOGGER.error("Failed to load biometrics capture details page", e);
				}
				break;
		}
		return gridPane;
	}

	private String getBiometricFieldLayout() {
		return this.uiSchemaDTO.getFieldLayout() == null ? "default" : this.uiSchemaDTO.getFieldLayout();
	}

	private GridPane createGridPane() {
		GridPane gridPane = new GridPane();
		
		RowConstraints topRowConstraints = new RowConstraints();
		topRowConstraints.setPercentHeight(5);
		RowConstraints midRowConstraints = new RowConstraints();
		midRowConstraints.setPercentHeight(95);
		gridPane.getRowConstraints().addAll(topRowConstraints,midRowConstraints);

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(15);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(80);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,columnConstraint3);
		return gridPane;
	}

	private List<HBox> getModalityButtons() {
		List<HBox> modalityList = new ArrayList<>();
		List<String> requiredBioAttributes = getRequiredBioAttributes(this.uiSchemaDTO.getSubType());
		//if(!requiredBioAttributes.isEmpty()) {
			for(Modality modality : Modality.values()) {
				List<String> modalityAttributes = ListUtils.intersection(requiredBioAttributes, modality.getAttributes());
				modalityAttributeMap.put(modality, new ArrayList<>());
				modalityAttributeMap.get(modality).add(modalityAttributes);
				modalityAttributeMap.get(modality).add(ListUtils.subtract(modality.getAttributes(), requiredBioAttributes));

				if(!modalityAttributes.isEmpty() || APPLICANT_SUBTYPE.equalsIgnoreCase(this.uiSchemaDTO.getSubType())) {
					HBox modalityView = new HBox();
					modalityView.setSpacing(10);
					modalityView.setId(uiSchemaDTO.getId() + modality);

					modalityView.getChildren().add(addModalityButton(modality));
					modalityList.add(modalityView);

					if(currentModality == null)
						currentModality = modality;

					addRemoveCaptureStatusMark(modalityView, modality);
					displayExceptionPhoto(modalityView, biometricsController.hasApplicantBiometricException());
				}
			}
		//}
		return modalityList;
	}

	private ImageView getImageView(Image image, double height) {
		ImageView imageView = new ImageView(image);
		imageView.setFitHeight(height);
		imageView.setFitWidth(height);
		imageView.setPreserveRatio(true);
		return imageView;
	}

	private Button addModalityButton(Modality modality) {
		Button button = new Button();
		button.setPrefSize(80, 80);

		Image image = new Image(this.getClass().getResourceAsStream(getImageIconPath(modality.name())));
		List<BiometricsDto> capturedData = getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), modality.getAttributes());
		if(!capturedData.isEmpty()) {
			image = biometricsController.getBioStreamImage(uiSchemaDTO.getSubType(), modality,
					capturedData.get(0).getNumOfRetries());
		}
		button.setGraphic(getImageView(image, 80));
		button.getStyleClass().add(RegistrationConstants.MODALITY_BUTTONS);
		Tooltip tooltip = new Tooltip(ApplicationContext.getInstance().getBundle(ApplicationContext.applicationLanguage(),
				RegistrationConstants.LABELS).getString(modality.name()));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		button.setTooltip(tooltip);
		button.setOnAction(getModalityActionHandler(this, modality));
		return button;
	}


	private EventHandler getModalityActionHandler(BiometricFxControl control, Modality modality) {
		return new EventHandler<ActionEvent>() {
			@SneakyThrows
			@Override
			public void handle(ActionEvent event) {
				currentModality = modality;
				scanForModality(control, currentModality);
			}
		};
	}
	
	private void scanForModality(BiometricFxControl control, Modality modality) throws IOException {
		LOGGER.info("Clicked on modality {}", modality);
		List<String> requiredAttributes = getRequiredBioAttributes(uiSchemaDTO.getSubType());
		List<String> configBioAttributes = ListUtils.intersection(requiredAttributes, Modality.getAllBioAttributes(modality));
		List<String> nonConfigBioAttributes = ListUtils.subtract(Modality.getAllBioAttributes(modality), configBioAttributes);

		switch (getBiometricFieldLayout()) {
			case "compact" :
				biometricsController.init(control, uiSchemaDTO.getSubType(), modality,
						configBioAttributes,nonConfigBioAttributes);
				break;
			default :
				biometricsController.initializeWithoutStage(control, uiSchemaDTO.getSubType(), modality,
						configBioAttributes,nonConfigBioAttributes);
		}
	}

	public List<String> getRequiredBioAttributes(String subType) {
		List<String> requiredAttributes = new ArrayList<String>();
		try {
			SchemaDto schema = identitySchemaService.getIdentitySchema(getRegistrationDTo().getIdSchemaVersion());
			List<UiSchemaDTO> fields = schema.getSchema().stream()
					.filter(field -> field.getType() != null
							&& PacketManagerConstants.BIOMETRICS_DATATYPE.equals(field.getType())
							&& field.getSubType() != null && field.getSubType().equals(subType))
					.collect(Collectors.toList());

			for (UiSchemaDTO schemaField : fields) {
				if (requiredFieldValidator.isRequiredField(schemaField, getRegistrationDTo()) && schemaField.getBioAttributes() != null)
					requiredAttributes.addAll(schemaField.getBioAttributes());
			}

			// Reg-client will capture the face of Infant and send it in Packet as part of
			// IndividualBiometrics CBEFF (If Face is captured for the country)
			if ((getRegistrationDTo().isChild() && APPLICANT_SUBTYPE.equals(subType) && requiredAttributes.contains("face"))
					|| (getRegistrationDTo().getRegistrationCategory().equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_UPDATE)
					&& getRegistrationDTo().getUpdatableFieldGroups().contains("GuardianDetails")
					&& APPLICANT_SUBTYPE.equals(subType) && requiredAttributes.contains("face"))) {
				return Arrays.asList("face"); // Only capture face
			}
		}catch (RegBaseCheckedException exception) {
			LOGGER.error("Failed to get required bioattributes", exception);
		}
		return requiredAttributes;
	}

	public String getImageIconPath(String modality) {
		String imageIconPath = RegistrationConstants.DEFAULT_EXCEPTION_IMAGE_PATH;

		if (modality != null) {
			switch (modality) {
				case RegistrationConstants.FACE:
					imageIconPath = RegistrationConstants.FACE_IMG_PATH;
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					imageIconPath = RegistrationConstants.DOUBLE_IRIS_IMG_PATH;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					imageIconPath = RegistrationConstants.RIGHTPALM_IMG_PATH;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					imageIconPath = RegistrationConstants.LEFTPALM_IMG_PATH;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					imageIconPath = RegistrationConstants.THUMB_IMG_PATH;
					break;
			}
		}
		return imageIconPath;
	}

	@Override
	public void refresh() {
		LOGGER.error("Refreshing biometric field {} ", uiSchemaDTO.getId());

		List<HBox> modalityButtons = getModalityButtons();
		if(modalityButtons.isEmpty()) {
			this.node.setVisible(false);
			this.node.setManaged(true);
		}
		else {
			Node listingNode = this.node.lookup(RegistrationConstants.HASH+uiSchemaDTO.getId()+"_listing");
			if(listingNode instanceof HBox) {
				((HBox)listingNode).getChildren().clear();
				((HBox)listingNode).getChildren().addAll(modalityButtons);
			}

			if(listingNode instanceof VBox) {
				((VBox)listingNode).getChildren().clear();
				((VBox)listingNode).getChildren().addAll(modalityButtons);
			}

			this.node.setVisible(true);
			this.node.setManaged(true);
		}
	}


	@Override
	public boolean canContinue() {
		return biometricsController.canContinue(uiSchemaDTO.getSubType(), getRequiredBioAttributes(uiSchemaDTO.getSubType()));
	}

	private void displayExceptionPhoto(HBox hbox, boolean isShow) {
		if(hbox.getId().equals(uiSchemaDTO.getId() + Modality.EXCEPTION_PHOTO)) {
			hbox.setVisible(isShow);
			hbox.setManaged(isShow);
			hbox.getChildren().forEach( child -> {
				child.setVisible(isShow);
				child.setManaged(isShow);
			});
		}
	}


	public void refreshModalityButton(Modality modality) {
		LOGGER.info("Refreshing biometric field {} modality : {}", uiSchemaDTO.getId(), modality);
		HBox modalityView = (HBox) getField(uiSchemaDTO.getId()+modality);
		Button button = (Button) modalityView.getChildren().get(0);
		List<BiometricsDto> capturedData = getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), modality.getAttributes());
		if(!capturedData.isEmpty()) {
			Image image = biometricsController.getBioStreamImage(uiSchemaDTO.getSubType(), modality,
					capturedData.get(0).getNumOfRetries());
			button.setGraphic(getImageView(image, 80));
		}
		addRemoveCaptureStatusMark(modalityView, modality);
		displayExceptionPhoto(modalityView, biometricsController.hasApplicantBiometricException());
	}

	private void addRemoveCaptureStatusMark(HBox pane, Modality modality) {
		if (pane.getChildren().size() > 1) {
			pane.getChildren().remove(1);
		}

		boolean isExceptionsMarked = isAnyExceptions(modality);
		boolean isCaptured = !getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), modality.getAttributes()).isEmpty();
		if(modality.equals(Modality.EXCEPTION_PHOTO) && biometricsController.isBiometricExceptionProofCollected()) {
			pane.getChildren().add(addCompletionImg(getCompletionImgPath(false)));
			return;
		}

		if(isExceptionsMarked) {
			pane.getChildren().add(addCompletionImg(getCompletionImgPath(true)));
			return;
		}

		if(isCaptured) {
			pane.getChildren().add(addCompletionImg(getCompletionImgPath(false)));
			return;
		}
	}


	private String getCompletionImgPath(boolean isAnyException) {
		return isAnyException ? RegistrationConstants.EXCLAMATION_IMG_PATH
				: RegistrationConstants.TICK_CIRICLE_IMG_PATH;
	}

	public boolean isAnyExceptions(Modality modality) {
		//checking with configured set of attributes for the given modality
		return modalityAttributeMap.get(modality).get(0)
				.stream()
				.anyMatch( attr -> biometricsController.isBiometricExceptionAvailable(uiSchemaDTO.getSubType(), attr));
	}

	private ImageView addCompletionImg(String imgPath) {
		ImageView tickImageView = new ImageView(new Image(this.getClass().getResourceAsStream(imgPath)));
		tickImageView.setId(uiSchemaDTO.getId() + currentModality.name() + "PANE");
		tickImageView.setFitWidth(35);
		tickImageView.setFitHeight(35);
		return tickImageView;
	}

}

