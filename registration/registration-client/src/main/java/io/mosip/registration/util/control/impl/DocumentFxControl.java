package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DemographicDetailController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DocumentFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DemographicDetailController.class);

	private static String loggerClassName = " Text Field Control Type Class";

	private DocumentScanController documentScanController;

	private MasterSyncService masterSyncService;

	private String TICK_MARK_ID = "tickMark";

	private String CLEAR_ID = "clear";

	private DemographicDetailController demographicDetailController;

	public DocumentFxControl() {

		org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
		this.demographicDetailController = applicationContext.getBean(DemographicDetailController.class);

		documentScanController = applicationContext.getBean(DocumentScanController.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {

		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;

		HBox hBox = new HBox();
		hBox.setSpacing(20);

		// DROP-DOWN
		hBox.getChildren().add(create(uiSchemaDTO));

		// REF-FIELD
		hBox.getChildren().add(createDocRef(uiSchemaDTO.getId()));

		// TICK-MARK
		hBox.getChildren().add(getImageGridPane(TICK_MARK_ID, RegistrationConstants.DONE_IMAGE_PATH));

		// CLEAR IMAGE
		GridPane clearGridPane = getImageGridPane(CLEAR_ID, RegistrationConstants.CLOSE_IMAGE_PATH);
		clearGridPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			getRegistrationDTo().getDocuments().remove(this.uiSchemaDTO.getId());
			getField(uiSchemaDTO.getId() + TICK_MARK_ID).setVisible(false);
			getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(false);
			getField(uiSchemaDTO.getId() + TICK_MARK_ID).setManaged(false);
			getField(uiSchemaDTO.getId() + CLEAR_ID).setManaged(false);
		});
		hBox.getChildren().add(clearGridPane);

		// SCAN-BUTTON
		hBox.getChildren().add(createScanButton(uiSchemaDTO));

		Map<String, Object> nodeMap = new LinkedHashMap<String, Object>();
		nodeMap.put(io.mosip.registration.context.ApplicationContext.getInstance().getApplicationLanguage(), hBox);

//		setNodeMap(nodeMap);

		this.node = hBox;
		setListener(getField(uiSchemaDTO.getId() + RegistrationConstants.BUTTON));

		try {
			fillData(masterSyncService.getDocumentCategories(uiSchemaDTO.getSubType(),
					ApplicationContext.applicationLanguage()));
		} catch (RegBaseCheckedException regBaseCheckedException) {
			// TODO log it
			regBaseCheckedException.printStackTrace();
		}

		return this.control;
	}

	private GridPane getImageGridPane(String id, String imagePath) {
		VBox imageVBox = new VBox();
		imageVBox.setId(uiSchemaDTO.getId() + id);
		ImageView imageView = new ImageView(
				(new Image(this.getClass().getResourceAsStream(imagePath), 25, 25, true, true)));

		boolean isVisible = getData() != null ? true : false;

		imageVBox.setVisible(isVisible);

		imageVBox.getChildren().add(imageView);

		GridPane gridPane = new GridPane();
		RowConstraints rowConstraint1 = new RowConstraints();
		RowConstraints rowConstraint2 = new RowConstraints();
		rowConstraint1.setPercentHeight(45);
		rowConstraint2.setPercentHeight(55);
		gridPane.getRowConstraints().addAll(rowConstraint1, rowConstraint2);
		gridPane.add(imageVBox, 0, 1);

		return gridPane;
	}

	private GridPane createScanButton(UiSchemaDTO uiSchemaDTO) {

		Button scanButton = new Button();
		scanButton.setText(RegistrationUIConstants.SCAN);
		scanButton.setId(uiSchemaDTO.getId() + RegistrationConstants.BUTTON);
		scanButton.getStyleClass().add(RegistrationConstants.DOCUMENT_CONTENT_BUTTON);
		scanButton.setGraphic(new ImageView(
				new Image(this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));

		GridPane scanButtonGridPane = new GridPane();
		RowConstraints rowConstraint1 = new RowConstraints();
		RowConstraints rowConstraint2 = new RowConstraints();
		rowConstraint1.setPercentHeight(35);
		rowConstraint2.setPercentHeight(65);
		scanButtonGridPane.getRowConstraints().addAll(rowConstraint1, rowConstraint2);
		scanButtonGridPane.setPrefWidth(80);
		scanButtonGridPane.add(scanButton, 0, 1);

		return scanButtonGridPane;
	}

	private void scanDocument(ComboBox<DocumentCategoryDto> comboBox, String subType) {

		if (isValid(comboBox)) {
			documentScanController.scanDocument(this, uiSchemaDTO.getId(), comboBox.getValue().getCode());

		} else {
			documentScanController.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.PLEASE_SELECT
					+ RegistrationConstants.SPACE + uiSchemaDTO.getSubType() + " " + RegistrationUIConstants.DOCUMENT);
		}

	}

	private VBox createDocRef(String id) {
		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);
		simpleTypeVBox.getStyleClass().add(RegistrationConstants.SCAN_VBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL,
				RegistrationUIConstants.REF_NUMBER, RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, prefWidth);

		simpleTypeVBox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = getTextField(id + RegistrationConstants.DOC_TEXT_FIELD,
				RegistrationUIConstants.REF_NUMBER, RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);
		simpleTypeVBox.getChildren().add(textField);

		if (ApplicationContext.getInstance().isPrimaryLanguageRightToLeft()) {

			textField.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			fieldTitle.setAlignment(Pos.CENTER_RIGHT);
		}
		return simpleTypeVBox;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO) {

		String fieldName = uiSchemaDTO.getId();

		// Get Mandatory Astrix
		String mandatorySuffix = getMandatorySuffix(uiSchemaDTO);

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);
		String titleText = uiSchemaDTO.getLabel().get(RegistrationConstants.PRIMARY) + mandatorySuffix;

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);
		simpleTypeVBox.getChildren().add(fieldTitle);

		/** comboBox Field */
		ComboBox<DocumentCategoryDto> comboBox = getComboBox(fieldName, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);
		simpleTypeVBox.getChildren().add(comboBox);

//		/** Validation message (Invalid/wrong,,etc,.) */
//		Label validationMessage = getLabel(fieldName + RegistrationConstants.MESSAGE, null,
//				RegistrationConstants.DemoGraphicFieldMessageLabel, false, prefWidth);
//		simpleTypeVBox.getChildren().add(validationMessage);

		if (ApplicationContext.getInstance().isPrimaryLanguageRightToLeft()) {
			comboBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			fieldTitle.setAlignment(Pos.CENTER_RIGHT);
		}

		return simpleTypeVBox;
	}

	@Override
	public void copyTo(Node srcNode, List<Node> targetNodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setData(Object data) {

		try {

			if (data == null) {

				getField(uiSchemaDTO.getId() + TICK_MARK_ID).setVisible(false);
				getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(false);
			} else {
				List<BufferedImage> bufferedImages = (List<BufferedImage>) data;

				if (bufferedImages != null && !bufferedImages.isEmpty()) {
					String documentSize = documentScanController
							.getValueFromApplicationContext(RegistrationConstants.DOC_SIZE);
					int docSize = Integer.parseInt(documentSize) / (1024 * 1024);
					if (bufferedImages == null || bufferedImages.isEmpty()) {
						documentScanController.generateAlert(RegistrationConstants.ERROR,
								RegistrationUIConstants.SCAN_DOCUMENT_EMPTY);
						return;
					}
					byte[] byteArray = documentScanController.getScannedPagesToBytes(bufferedImages);

					if (byteArray == null) {
						documentScanController.generateAlert(RegistrationConstants.ERROR,
								RegistrationUIConstants.SCAN_DOCUMENT_CONVERTION_ERR);
						return;
					}

					if (docSize <= (byteArray.length / (1024 * 1024))) {
						bufferedImages.clear();
						documentScanController.generateAlert(RegistrationConstants.ERROR,
								RegistrationUIConstants.SCAN_DOC_SIZE.replace("1", Integer.toString(docSize)));
					} else {

						ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(
								uiSchemaDTO.getId());

						DocumentDto documentDto = documentScanController.getRegistrationDTOFromSession().getDocuments()
								.get(uiSchemaDTO.getId());

						if (documentDto == null) {
							documentDto = new DocumentDto();
							documentDto.setDocument(byteArray);
							documentDto.setType(comboBox.getValue().getCode());

							String docType = documentScanController
									.getValueFromApplicationContext(RegistrationConstants.DOC_TYPE);

							documentDto.setFormat(docType);
							documentDto.setCategory(uiSchemaDTO.getFieldCategory());
							documentDto.setOwner("Applicant");
							documentDto.setValue(uiSchemaDTO.getSubType()
									.concat(RegistrationConstants.UNDER_SCORE).concat(comboBox.getValue().getCode()));
						} else {

							documentDto.setDocument(byteArray);
						}

						TextField textField = (TextField) getField(
								uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);

						documentDto.setRefNumber(textField.getText());

						documentScanController.getRegistrationDTOFromSession().addDocument(uiSchemaDTO.getId(),
								documentDto);

						getField(uiSchemaDTO.getId() + TICK_MARK_ID).setVisible(true);
						getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(true);
					}
				}
			}
		} catch (IOException regBaseCheckedException) {

			LOGGER.error("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					"Unable to parse the buffered images to byte array " + regBaseCheckedException.getMessage()
							+ ExceptionUtils.getStackTrace(regBaseCheckedException));
			getField(uiSchemaDTO.getId() + TICK_MARK_ID).setVisible(false);
			getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(false);
			documentScanController.generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.UNABLE_LOAD_REG_PAGE);
		}

		refreshFields();
	}

	@Override
	public Object getData() {

		return documentScanController.getRegistrationDTOFromSession().getDocuments().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid(Node node) {

		String poeDocValue = documentScanController
				.getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) node;
		if (comboBox.getValue() == null) {
			comboBox.requestFocus();
			return false;
		} else if (comboBox.getValue().getCode().equalsIgnoreCase(poeDocValue)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void setListener(Node node) {

		Button scanButton = (Button) node;
		scanButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				AuditEvent auditEvent = null;
				try {
					auditEvent = AuditEvent
							.valueOf(String.format("REG_DOC_%S_SCAN", ((Button) event.getSource()).getId()));
				} catch (Exception exception) {

					auditEvent = AuditEvent.REG_DOC_SCAN;

				}
//				auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
//						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

				Button clickedBtn = (Button) event.getSource();
				clickedBtn.getId();
				// TODO Check the scan option

				documentScanController.setFxControl(control);
//				documentScanController.scanWindow();
				scanDocument((ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId()), uiSchemaDTO.getSubType());

			}

		});
		scanButton.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				scanButton.setGraphic(new ImageView(new Image(
						this.getClass().getResourceAsStream(RegistrationConstants.SCAN_FOCUSED), 12, 12, true, true)));
			} else {
				scanButton.setGraphic(new ImageView(new Image(
						this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));
			}
		});

	}

	private <T> ComboBox<DocumentCategoryDto> getComboBox(String id, String titleText, String styleClass,
			double prefWidth, boolean isDisable) {
		ComboBox<DocumentCategoryDto> field = new ComboBox<DocumentCategoryDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		VBox vbox = new VBox();
		field.setId(id);
		field.setPrefWidth(prefWidth);
		field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.setConverter((StringConverter<DocumentCategoryDto>) uiRenderForComboBox);
		field.getStyleClass().add(styleClass);
		field.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			getField(uiSchemaDTO.getId() + RegistrationConstants.LABEL).setVisible(true);
		});

		if (ApplicationContext.getInstance().isPrimaryLanguageRightToLeft()) {
			field.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

		}

		return field;
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	@Override
	public void fillData(Object data) {

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());

		if (data != null) {

			List<DocumentCategoryDto> vals = (List<DocumentCategoryDto>) data;
			comboBox.getItems().addAll(vals);
		}

	}

	public boolean canContinue() {

		boolean canContinue;

		if (getRegistrationDTo().getDocuments().get(this.uiSchemaDTO.getId()) != null || getRegistrationDTo()
				.getRegistrationCategory().equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_LOST)) {
			return true;
		} else {

			if (requiredFieldValidator == null) {
				requiredFieldValidator = Initialization.getApplicationContext().getBean(RequiredFieldValidator.class);
			}

			try {
				boolean isRequired = requiredFieldValidator.isRequiredField(this.uiSchemaDTO, getRegistrationDTo());
				canContinue = !isRequired;
			} catch (RegBaseCheckedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

				canContinue = false;
			}
		}

		return canContinue;
	}

	@Override
	public void selectAndSet(Object data) {

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());

		if (comboBox != null) {
			comboBox.getSelectionModel().selectFirst();

			DocumentDto documentDto = (DocumentDto) data;

			getRegistrationDTo().addDocument(this.uiSchemaDTO.getId(), documentDto);

			TextField textField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);

			textField.setText(documentDto.getRefNumber());

			getField(uiSchemaDTO.getId() + TICK_MARK_ID).setVisible(true);
			getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(true);
		}
	}
}
