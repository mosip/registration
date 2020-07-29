package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.CLMDetectedFace;
import org.openimaj.image.processing.face.detection.CLMFaceDetector;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idgenerator.spi.RidGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.ProcessNames;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.auth.AuthenticationController;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * {@code RegistrationController} for Registration Page Controller
 * 
 * @author Taleev.Aalam
 * @since 1.0.0
 */

@Controller
public class RegistrationController extends BaseController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RegistrationController.class);

	@Autowired
	private DocumentScanController documentScanController;
	@FXML
	private GridPane documentScan;
	@FXML
	private GridPane registrationId;
	@Autowired
	private Validations validation;
	@Autowired
	private MasterSyncService masterSync;
	@Autowired
	private DemographicDetailController demographicDetailController;
	@FXML
	private GridPane demographicDetail;

	@FXML
	private GridPane biometric;
	@FXML
	private GridPane operatorAuthenticationPane;
	@FXML
	public ImageView biometricTracker;
	@FXML
	private GridPane registrationPreview;
	@Autowired
	private AuthenticationController authenticationController;
	@Autowired
	private RidGenerator<String> ridGeneratorImpl;

	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;

	@Autowired
	private IdentitySchemaService identitySchemaService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize()
	 */
	@FXML
	private void initialize() {
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Entering the Registration Controller");
		try {
			if (isEditPage() && getRegistrationDTOFromSession() != null) {
				prepareEditPageContent();
			}
			uinUpdate();

		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_REG_PAGE);
		}
	}

	/**
	 * This method is prepare the screen for uin update
	 */
	private void uinUpdate() {
		if (getRegistrationDTOFromSession().getUpdatableFields() != null) {
			demographicDetailController.uinUpdate();
		}
	}

	public void init(String UIN, HashMap<String, Object> selectionListDTO, Map<String, UiSchemaDTO> selectedFields,
			List<String> selectedFieldGroups) {
		validation.updateAsLostUIN(false);
		createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_UPDATE);
		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
		registrationDTO.setSelectionListDTO(selectionListDTO);
		List<String> fieldIds = new ArrayList<String>(selectedFields.keySet());
		registrationDTO.setUpdatableFields(fieldIds);
		registrationDTO.addDemographicField("UIN", UIN);
		registrationDTO.setUpdatableFieldGroups(selectedFieldGroups);
		registrationDTO.setBiometricMarkedForUpdate(
				selectedFieldGroups.contains(RegistrationConstants.BIOMETRICS_GROUP) ? true : false);
	}

	protected void initializeLostUIN() {
		validation.updateAsLostUIN(true);
		createRegistrationDTOObject(RegistrationConstants.PACKET_TYPE_LOST);
	}

	/**
	 * This method is to prepopulate all the values for edit operation
	 */
	private void prepareEditPageContent() {
		try {
			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Preparing the Edit page content");
			// demographicDetailController.prepareEditPageContent();
			documentScanController.prepareEditPageContent();
			SessionContext.map().put(RegistrationConstants.REGISTRATION_ISEDIT, false);
		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}

	}

	/**
	 * To detect the face from the captured photograph for validation.
	 * 
	 * @param applicantImage
	 *            the image that is captured as applicant photograph
	 * @return BufferedImage the face that is detected from the applicant photograph
	 */
	public BufferedImage detectApplicantFace(BufferedImage applicantImage) {
		BufferedImage detectedFace = null;
		CLMFaceDetector detector = new CLMFaceDetector();
		List<CLMDetectedFace> faces = null;
		faces = detector.detectFaces(ImageUtilities.createFImage(applicantImage));
		if (!faces.isEmpty()) {
			if (faces.size() > 1) {
				return null;
			} else {
				Iterator<CLMDetectedFace> dfi = faces.iterator();
				while (dfi.hasNext()) {
					DetectedFace face = dfi.next();
					FImage image1 = face.getFacePatch();
					detectedFace = ImageUtilities.createBufferedImage(image1);
				}
			}
		}
		return detectedFace;
	}

	/**
	 * To compress the detected face from the image of applicant and store it in DTO
	 * to use it for QR Code generation
	 * 
	 * @param applicantImage
	 *            the image that is captured as applicant photograph
	 */
	/*
	 * private void compressImageForQRCode(BufferedImage detectedFace) { try {
	 * ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	 * 
	 * Iterator<ImageWriter> writers = ImageIO
	 * .getImageWritersByFormatName(RegistrationConstants.WEB_CAMERA_IMAGE_TYPE);
	 * ImageWriter writer = writers.next();
	 * 
	 * ImageOutputStream imageOutputStream =
	 * ImageIO.createImageOutputStream(byteArrayOutputStream);
	 * writer.setOutput(imageOutputStream);
	 * 
	 * ImageWriteParam param = writer.getDefaultWriteParam();
	 * 
	 * param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	 * param.setCompressionQuality(0); // Change the quality value you // prefer
	 * writer.write(null, new IIOImage(detectedFace, null, null), param); byte[]
	 * compressedPhoto = byteArrayOutputStream.toByteArray(); if ((boolean)
	 * SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
	 * ((BiometricDTO)
	 * SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA))
	 * .getOperatorBiometricDTO().getFace().setFace(compressedPhoto); } else {
	 * FaceDetailsDTO faceDetailsDTO =
	 * getRegistrationDTOFromSession().getBiometricDTO()
	 * .getApplicantBiometricDTO().getFace();
	 * faceDetailsDTO.setCompressedFacePhoto(compressedPhoto); }
	 * byteArrayOutputStream.close(); imageOutputStream.close(); writer.dispose(); }
	 * catch (IOException ioException) {
	 * LOGGER.error(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
	 * RegistrationConstants.APPLICATION_ID, ioException.getMessage() +
	 * ExceptionUtils.getStackTrace(ioException)); } }
	 */

	/**
	 * This method is save the biometric details
	 */
	/*
	 * public boolean saveBiometricDetails(BufferedImage applicantBufferedImage,
	 * BufferedImage exceptionBufferedImage, byte[] applicantIso, byte[]
	 * exceptionIso) { LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER,
	 * RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
	 * "saving the details of applicant biometrics"); boolean isValid = true; //
	 * TODO This is not required at this stage as validation is complted during
	 * click of documant continue button /*if (!(boolean)
	 * SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) { isValid =
	 * true; demographicDetailController.validateThisPane();
	 * 
	 * if (isValid && RegistrationConstants.ENABLE
	 * .equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.
	 * DOC_DISABLE_FLAG))) { isValid =
	 * validateDemographicPane(documentScanController.documentScanPane); } }
	 */
	/*
	 * if (isValid) { try { BufferedImage detectedFace =
	 * detectApplicantFace(applicantBufferedImage); if (detectedFace != null) { if
	 * (exceptionBufferedImage != null &&
	 * detectApplicantFace(exceptionBufferedImage) == null) { isValid = false;
	 * generateAlert(RegistrationConstants.ERROR,
	 * RegistrationUIConstants.EXCEPTION_PHOTO_CAPTURE_ERROR); } else {
	 * //compressImageForQRCode(detectedFace); ByteArrayOutputStream
	 * byteArrayOutputStream = new ByteArrayOutputStream();
	 * ImageIO.write(applicantBufferedImage,
	 * RegistrationConstants.WEB_CAMERA_IMAGE_TYPE, byteArrayOutputStream); byte[]
	 * photoInBytes = byteArrayOutputStream.toByteArray(); if (!(boolean)
	 * SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
	 * BiometricInfoDTO biometricDTO = getFaceDetailsDTO(); FaceDetailsDTO
	 * faceDetailsDTO = biometricDTO.getFace(); FaceDetailsDTO
	 * exceptionFaceDetailsDTO = biometricDTO.getExceptionFace(); if
	 * (getRegistrationDTOFromSession().isUpdateUINNonBiometric() &&
	 * !getRegistrationDTOFromSession().isUpdateUINChild()) {
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getFace() .setFace(photoInBytes);
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getFace() .setFaceISO(applicantIso); } else {
	 * faceDetailsDTO.setFace(photoInBytes);
	 * faceDetailsDTO.setFaceISO(applicantIso);
	 * faceDetailsDTO.setPhotographName(RegistrationConstants.
	 * APPLICANT_PHOTOGRAPH_NAME); } byteArrayOutputStream.close(); if
	 * (exceptionBufferedImage != null) { ByteArrayOutputStream outputStream = new
	 * ByteArrayOutputStream(); ImageIO.write(exceptionBufferedImage,
	 * RegistrationConstants.WEB_CAMERA_IMAGE_TYPE, outputStream); byte[]
	 * exceptionPhotoInBytes = outputStream.toByteArray(); if ((boolean)
	 * SessionContext.map().get(RegistrationConstants.IS_Child) ||
	 * (getRegistrationDTOFromSession().isUpdateUINNonBiometric() &&
	 * !getRegistrationDTOFromSession().isUpdateUINChild())) {
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getExceptionFace().setFace(exceptionPhotoInBytes);
	 * getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
	 * .getExceptionFace().setFaceISO(exceptionIso);
	 * biometricDTO.setHasExceptionPhoto(true); } else {
	 * exceptionFaceDetailsDTO.setFace(exceptionPhotoInBytes);
	 * exceptionFaceDetailsDTO.setFaceISO(exceptionIso); exceptionFaceDetailsDTO
	 * .setPhotographName(RegistrationConstants.EXCEPTION_PHOTOGRAPH_NAME);
	 * biometricDTO.setHasExceptionPhoto(true); } outputStream.close(); } else {
	 * biometricDTO.setHasExceptionPhoto(false); }
	 * 
	 * LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER,
	 * RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
	 * "showing demographic preview"); if
	 * (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
	 * SessionContext.map().put("faceCapture", false);
	 * SessionContext.map().put("registrationPreview", true);
	 * registrationPreviewController.setUpPreviewContent();
	 * showUINUpdateCurrentPage(); } else {
	 * showCurrentPage(RegistrationConstants.FACE_CAPTURE,
	 * getPageByAction(RegistrationConstants.FACE_CAPTURE,
	 * RegistrationConstants.NEXT)); }
	 * 
	 * } else { ((BiometricDTO)
	 * SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA))
	 * .getOperatorBiometricDTO().getFace().setFace(photoInBytes); ((BiometricDTO)
	 * SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA))
	 * .getOperatorBiometricDTO().getFace().setFaceISO(applicantIso);
	 * byteArrayOutputStream.close(); } } } else { if ((boolean)
	 * SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
	 * ((BiometricDTO)
	 * SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA))
	 * .getOperatorBiometricDTO().getFace().setFace(null); ((BiometricDTO)
	 * SessionContext.map().get(RegistrationConstants.USER_ONBOARD_DATA))
	 * .getOperatorBiometricDTO().getFace().setFaceISO(null); } isValid = false;
	 * generateAlert(RegistrationConstants.ERROR,
	 * RegistrationUIConstants.FACE_CAPTURE_ERROR); } } catch (IOException
	 * ioException) { LOGGER.error(RegistrationConstants.REGISTRATION_CONTROLLER,
	 * APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
	 * ioException.getMessage() + ExceptionUtils.getStackTrace(ioException)); } }
	 * return isValid; }
	 */

	/**
	 * This method is to go to the operator authentication page
	 */
	public void goToAuthenticationPage() {
		try {
			authenticationController.initData(ProcessNames.PACKET.getType());
		} catch (RegBaseCheckedException ioException) {
			LOGGER.error("REGISTRATION - REGSITRATION_OPERATOR_AUTHENTICATION_PAGE_LOADING_FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}
	}

	/**
	 * This method is to determine if it is edit page
	 */
	private Boolean isEditPage() {
		if (SessionContext.map().get(RegistrationConstants.REGISTRATION_ISEDIT) != null)
			return (Boolean) SessionContext.map().get(RegistrationConstants.REGISTRATION_ISEDIT);
		return false;
	}

	/**
	 * This method will create registration DTO object
	 */
	protected void createRegistrationDTOObject(String registrationCategory) {
		RegistrationDTO registrationDTO = new RegistrationDTO();

		// set id-schema version to be followed for this registration
		try {
			registrationDTO.setIdSchemaVersion(identitySchemaService.getLatestEffectiveSchemaVersion());
		} catch (RegBaseCheckedException e) {
			generateAlert(RegistrationConstants.ERROR, "Published Identity Schema is required"); // TODO
		}

		// Create object for OSIData DTO
		registrationDTO.setOsiDataDTO(new OSIDataDTO());
		registrationDTO.setRegistrationCategory(registrationCategory);

		// Create RegistrationMetaData DTO & set default values in it
		RegistrationMetaDataDTO registrationMetaDataDTO = new RegistrationMetaDataDTO();
		registrationMetaDataDTO.setRegistrationCategory(registrationCategory); // TODO - remove its usage

		/*
		 * registrationMetaDataDTO.setRegClientVersionNumber(softwareUpdateHandler.
		 * getCurrentVersion()); RegistrationCenterDetailDTO registrationCenter =
		 * SessionContext.userContext().getRegistrationCenterDetailDTO(); if
		 * (RegistrationConstants.ENABLE
		 * .equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.
		 * GPS_DEVICE_DISABLE_FLAG))) { registrationMetaDataDTO
		 * .setGeoLatitudeLoc(Double.parseDouble(registrationCenter.
		 * getRegistrationCenterLatitude())); registrationMetaDataDTO
		 * .setGeoLongitudeLoc(Double.parseDouble(registrationCenter.
		 * getRegistrationCenterLongitude())); }
		 * 
		 * registrationMetaDataDTO.setCenterId((String)
		 * ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID));
		 * registrationMetaDataDTO.setMachineId((String)
		 * ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID));
		 * registrationMetaDataDTO.setDeviceId((String)
		 * ApplicationContext.map().get(RegistrationConstants.DONGLE_SERIAL_NUMBER));
		 */
		registrationDTO.setRegistrationMetaDataDTO(registrationMetaDataDTO);

		// Set RID
		String registrationID = ridGeneratorImpl.generateId(
				(String) ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID),
				(String) ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID));
		registrationDTO.setRegistrationId(registrationID);

		LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID,
				"Registration Started for RID  : [ " + registrationDTO.getRegistrationId() + " ] ");

		// Put the RegistrationDTO object to SessionContext Map
		SessionContext.map().put(RegistrationConstants.REGISTRATION_DATA, registrationDTO);
	}

	/**
	 * This method will show uin update current page
	 */
	public void showUINUpdateCurrentPage() {
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Setting Visibility for demo,doc,biometric,preview,auth");
		demographicDetail.setVisible(getVisiblity(RegistrationConstants.UIN_UPDATE_DEMOGRAPHICDETAIL));
		documentScan.setVisible(getVisiblity(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN));

		biometric.setVisible(getVisiblity(RegistrationConstants.UIN_UPDATE_PARENTGUARDIAN_DETAILS));
		registrationPreview.setVisible(getVisiblity(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW));
		operatorAuthenticationPane
				.setVisible(getVisiblity(RegistrationConstants.UIN_UPDATE_OPERATORAUTHENTICATIONPANE));
	}

	/**
	 * This method will determine the visibility of the page
	 */
	private boolean getVisiblity(String page) {
		if (SessionContext.map().get(page) != null) {
			return (boolean) SessionContext.map().get(page);
		}
		return false;
	}

	/**
	 * This method will determine the current page
	 */
	public void showCurrentPage(String notTosShow, String show) {

		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Navigating to next page based on the current page");

		getCurrentPage(registrationId, notTosShow, show);

		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Navigated to next page based on the current page");
	}

	/**
	 * 
	 * Validates the fields of demographic pane1
	 * 
	 */
	public boolean validateDemographicPane(Pane paneToValidate) {
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Validating the fields in demographic pane");

		boolean gotoNext = true;
		List<String> excludedIds = RegistrationConstants.fieldsToExclude();
		/*
		 * if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
		 * excludedIds.remove("cniOrPinNumber");
		 * excludedIds.remove("cniOrPinNumberLocalLanguage"); }
		 */

		if (getRegistrationDTOFromSession().getUpdatableFields() != null
				&& !getRegistrationDTOFromSession().getUpdatableFields().isEmpty()) {
			if (getRegistrationDTOFromSession().isChild()
					&& !getRegistrationDTOFromSession().getUpdatableFieldGroups().contains("GuardianDetails")) {
				gotoNext = false;
				generateAlert(RegistrationConstants.ERROR, "Parent or Guardian should have been selected");
			}
		}

		validation.setValidationMessage();
		gotoNext = validation.validate(paneToValidate, excludedIds, gotoNext, masterSync);
		displayValidationMessage(validation.getValidationMessage().toString());
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Validated the fields");

		return gotoNext;
	}

	/**
	 * Display the validation failure messages
	 */
	public void displayValidationMessage(String validationMessage) {
		LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Showing the validation message");
		if (validationMessage.length() > 0) {
			TextArea view = new TextArea(validationMessage);
			view.setEditable(false);
			Scene scene = new Scene(new StackPane(view), 300, 200);
			Stage primaryStage = new Stage();
			primaryStage.setTitle("Invalid input");
			primaryStage.setScene(scene);
			primaryStage.sizeToScene();
			primaryStage.initModality(Modality.WINDOW_MODAL);
			primaryStage.initOwner(fXComponents.getStage());
			primaryStage.show();

			LOGGER.debug(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Validation message shown successfully");
		}
	}

}
