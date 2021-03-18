package io.mosip.registration.util.control.impl;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

	}

	@Override
	public void fillData(Object data) {

	}

	@Override
	public Object getData() {
		return null;
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
					scanForModality(this, currentModality);
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
				scanForModality(control, modality);
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
				if (/*requiredFieldValidator.isRequiredField(schemaField, getRegistrationDTo()) &&*/ schemaField.getBioAttributes() != null)
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

	public void displayExceptionPhoto(boolean isShow) {
		Node node = this.getNode().lookup(RegistrationConstants.HASH+ uiSchemaDTO.getId() + Modality.EXCEPTION_PHOTO);
		if(node != null) {
			displayExceptionPhoto( (HBox) node, isShow);
		}
	}

	private void addTickMark(Modality modality) {
		Pane pane = getModalityHBox(modality);
		if (pane.getChildren().size() > 1) {

			pane.getChildren().remove(1);
		}
		pane.getChildren().add(addCompletionImg(getCompletionImgPath(isAnyExceptions(modality))));
	}

	private Pane getModalityHBox(Modality modality) {
		return ((Pane) getField(uiSchemaDTO.getId() + modality.name()));
	}

	private void removeTickMark(Modality modality) {

		Pane pane = getModalityHBox(modality);

		if (pane.getChildren().size() > 1) {
			pane.getChildren().remove(1);
		}
	}

	public void refreshExceptionMarks(Modality currentModality) {
		if (biometricsController.isExceptionPhoto(currentModality)) {
			if (biometricsController.isBiometricExceptionProofCollected()) {
				addTickMark(currentModality);
			} else {
				removeTickMark(currentModality);
			}
		} else if (isAnyExceptions(currentModality) || !biometricsController
				.getBiometrics(uiSchemaDTO.getSubType(), modalityAttributeMap.get(currentModality).get(0)).isEmpty()) {
			addTickMark(currentModality);
		} else {
			removeTickMark(currentModality);
		}

		Node exceptionModality = getField(uiSchemaDTO.getId() + Modality.EXCEPTION_PHOTO);

		if (exceptionModality != null) {
			exceptionModality.setVisible(isBiometricExceptionAvailable());
		}
	}

	private boolean isBiometricExceptionAvailable() {
		boolean isException = false;
		for (Map.Entry<Modality, List<List<String>>> entry : modalityAttributeMap.entrySet()) {
			isException = entry.getValue().get(0)
					.stream()
					.anyMatch( attr -> getRegistrationDTo().isBiometricExceptionAvailable(uiSchemaDTO.getSubType(), attr));

			if(isException)
				break;
		}
		return isException;
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


	/*private VBox create(VBox biometricVBox, UiSchemaDTO uiSchemaDTO) {
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
		return true;
	}

	@Override
	public boolean isValid() {
		return true;
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

	private List<String> getAllBioAttributes(String modality) {
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
	}*/
}

