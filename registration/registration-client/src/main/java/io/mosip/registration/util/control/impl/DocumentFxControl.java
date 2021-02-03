package io.mosip.registration.util.control.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.util.control.FxControl;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DocumentFxControl extends FxControl {

	private DocumentScanController documentScanController;

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		// TODO Auto-generated method stub

		VBox comboField = create(uiSchemaDTO);

		HBox hBox = new HBox();
		hBox.setSpacing(20);
		hBox.getChildren().add(comboField);

		VBox docRefField = createDocRef(uiSchemaDTO.getId());
		hBox.getChildren().add(docRefField);

		GridPane scanButtonGridPane = createScanButton(uiSchemaDTO);

		return null;
	}

	private GridPane createScanButton(UiSchemaDTO uiSchemaDTO) {

		Button scanButton = new Button();
		scanButton.setText(RegistrationUIConstants.SCAN);
		scanButton.setId(uiSchemaDTO.getId() + RegistrationConstants.BUTTON);
		scanButton.getStyleClass().add(RegistrationConstants.DOCUMENT_CONTENT_BUTTON);
		scanButton.setGraphic(new ImageView(
				new Image(this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));
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
				auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

				Button clickedBtn = (Button) event.getSource();
				clickedBtn.getId();
				// TODO Check the scan option

				scanDocument((ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId()), uiSchemaDTO.getSubType());

//				uiSchemaDTO.getSub
//				scanDocument(comboBox, documentVBox, documentCategory.getSubType(),
//						RegistrationUIConstants.PLEASE_SELECT + RegistrationConstants.SPACE
//								+ documentCategory.getSubType() + " " + RegistrationUIConstants.DOCUMENT);

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

		GridPane scanButtonGridPane = new GridPane();
//		gridPane.setId(docCategoryCode + "RefNumGridPane");
//		scanButtonGridPane.setVgap(10);
//		scanButtonGridPane.setHgap(10);
		scanButtonGridPane.setPrefWidth(80);
		scanButtonGridPane.add(scanButton, 0, 0);

		return null;
	}

	private void scanDocument(ComboBox<DocumentCategoryDto> comboBox, String subType) {

//		String poeDocValue = getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);
		String poeDocValue = null;

		if (comboBox.getValue() == null) {
			// TODO Generate Alert
			comboBox.requestFocus();
		}

		if (null != comboBox.getValue() && poeDocValue != null && comboBox.getValue().getCode().matches(poeDocValue)) {

			documentScanController.scanDocument(this, uiSchemaDTO.getId(), comboBox.getValue().getCode());

		}

	}

	public void save(List<BufferedImage> bufferedImages) throws IOException {

		// TODO doc size
		String documentSize = documentScanController.getValueFromApplicationContext(RegistrationConstants.DOC_SIZE);
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

			ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());

			DocumentDto documentDto = documentScanController.getRegistrationDTOFromSession().getDocuments()
					.get(uiSchemaDTO.getId());

			if (documentDto == null) {
				documentDto = new DocumentDto();
				documentDto.setDocument(byteArray);
				documentDto.setType(comboBox.getValue().getCode());

				String docType = documentScanController.getValueFromApplicationContext(RegistrationConstants.DOC_TYPE);

				documentDto.setFormat(docType);
				documentDto.setCategory(uiSchemaDTO.getFieldCategory());
				documentDto.setOwner("Applicant");
				documentDto.setValue(uiSchemaDTO.getFieldCategory().concat(RegistrationConstants.UNDER_SCORE)
						.concat(comboBox.getValue().getCode()));
			} else {

				documentDto.setDocument(byteArray);
			}

			TextField textField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);

			documentDto.setRefNumber(textField.getText());

			documentScanController.getRegistrationDTOFromSession().addDocument(uiSchemaDTO.getId(), documentDto);
		}

	}

	private VBox createDocRef(String id) {
		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL,
				RegistrationUIConstants.REF_NUMBER, RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, true, prefWidth);

		simpleTypeVBox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = getTextField(id + RegistrationConstants.DOC_TEXT_FIELD,
				RegistrationUIConstants.REF_NUMBER, RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, true);
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
	public void setData() {
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

	@Override
	public Node getNode() {
		// TODO Auto-generated method stub
		return null;
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
}
