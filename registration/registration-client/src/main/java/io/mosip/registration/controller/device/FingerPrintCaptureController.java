package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.UserOnboardParentController;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.biometric.BiometricExceptionDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.mdm.dto.RequestDetail;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.security.AuthenticationService;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * {@code FingerPrintCaptureController} is to capture and display the captured
 * fingerprints.
 * 
 * @author Mahesh Kumar
 * @since 1.0
 */

@Controller
public class FingerPrintCaptureController extends BaseController implements Initializable {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(FingerPrintCaptureController.class);

	/** The finger print capture service impl. */
	@Autowired
	private AuthenticationService authenticationService;

	/** The registration controller. */
	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private ScanPopUpViewController scanPopUpViewController;

	@Autowired
	private Streamer streamer;

	/** The finger print capture pane. */
	@FXML
	private AnchorPane fingerPrintCapturePane;

	/** The left hand palm pane. */
	@FXML
	private GridPane leftHandPalmPane;

	/** The right hand palm pane. */
	@FXML
	private GridPane rightHandPalmPane;

	/** The thumb pane. */
	@FXML
	private GridPane thumbPane;

	/** The left hand palm imageview. */
	@FXML
	private ImageView leftHandPalmImageview;

	/** The right hand palm imageview. */
	@FXML
	private ImageView rightHandPalmImageview;

	/** The thumb imageview. */
	@FXML
	private ImageView thumbImageview;

	/** The left slap quality score. */
	@FXML
	private Label leftSlapQualityScore;

	/** The right slap quality score. */
	@FXML
	private Label rightSlapQualityScore;

	/** The thumbs quality score. */
	@FXML
	private Label thumbsQualityScore;

	/** The left slap threshold score label. */
	@FXML
	private Label leftSlapThresholdScoreLbl;

	/** The right slap threshold score label. */
	@FXML
	private Label rightSlapThresholdScoreLbl;

	/** The thumbs threshold score label. */
	@FXML
	private Label thumbsThresholdScoreLbl;

	/** The duplicate check label. */
	@FXML
	public Label duplicateCheckLbl;

	/** The fp progress. */
	@FXML
	private ProgressBar fpProgress;

	/** The quality text. */
	@FXML
	private Label qualityText;

	/** The fp retry box. */
	@FXML
	private HBox fpRetryBox;

	/** The right slap attempt. */
	@FXML
	private Label rightSlapAttempt;

	/** The thumb slap attempt. */
	@FXML
	private Label thumbSlapAttempt;

	/** The threshold pane 1. */
	@FXML
	private ColumnConstraints thresholdPane1;

	/** The threshold pane 2. */
	@FXML
	private ColumnConstraints thresholdPane2;

	/** The threshold label. */
	@FXML
	private Label thresholdLabel;

	/** The left slap attempt. */
	@FXML
	private Label leftSlapAttempt;

	/** The left slap exception. */
	@FXML
	private Label leftSlapException;

	/** The right slap exception. */
	@FXML
	private Label rightSlapException;

	/** The thumb slap exception. */
	@FXML
	private Label thumbSlapException;

	@FXML
	private ImageView scanImageView;
	@FXML
	private ImageView startOverImageView;
	@FXML
	private Button startOverBtn;

	/** The selected pane. */
	private GridPane selectedPane = null;

	/** The finger print facade. */
	@Autowired
	private BioService bioService;

	/** The iris capture controller. */
	@Autowired
	private IrisCaptureController irisCaptureController;

	/** The face capture controller. */
	@Autowired
	private FaceCaptureController faceCaptureController;

	/** The user onboard parent controller. */
	@Autowired
	private UserOnboardParentController userOnboardParentController;

	/** The scan btn. */
	@FXML
	private Button scanBtn;

	/** The continue btn. */
	@FXML
	private Button continueBtn;

	/** The back btn. */
	@FXML
	private Button backBtn;

	/** The registration navlabel. */
	@FXML
	private Label registrationNavlabel;

	@FXML
	private AnchorPane lefhPalmTrackerImg;
	@FXML
	private AnchorPane rightPalmTrackerImg;
	@FXML
	private AnchorPane thumbTrackerImg;
	@FXML
	private ImageView backImageView;
	@FXML
	private Label dedupeMessage;
	@FXML
	private Label captureTimeValue;

	/** The left slap count. */
	private int leftSlapCount;

	private int leftSlapExceptionCount;

	/** The right slap count. */
	private int rightSlapCount;

	private int rightSlapExceptionCount;

	/** The thumb count. */
	private int thumbCount;

	private int thumbExceptionCount;

	/** Left Hand Exceptions */
	private List<String> leftHandExceptions = new ArrayList<String>();

	/** Right Hand Exceptions */
	private List<String> rightHandExceptions = new ArrayList<String>();

	/** Thumbs Hand Exceptions */
	private List<String> thumbsExceptions = new ArrayList<String>();

	private List<String> exception;

	private String requestedScore;

	@Autowired
	MosipBioDeviceManager mosipBioDeviceManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Loading of FingerprintCapture screen started");

		setImagesOnHover();
		initializeCaptureCount();
		try {
			if (getRegistrationDTOFromSession() != null
					&& getRegistrationDTOFromSession().getSelectionListDTO() != null) {
				registrationNavlabel.setText(ApplicationContext.applicationLanguageBundle()
						.getString(RegistrationConstants.UIN_UPDATE_UINUPDATENAVLBL));
			}
			if (getRegistrationDTOFromSession() != null
					&& getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory() != null
					&& getRegistrationDTOFromSession().getRegistrationMetaDataDTO().getRegistrationCategory()
							.equals(RegistrationConstants.PACKET_TYPE_LOST)) {
				registrationNavlabel.setText(
						ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.LOSTUINLBL));
			}

			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				for (int retry = 0; retry < Integer.parseInt(
						getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)); retry++) {
					Label label = new Label();
					label.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
					label.setId(RegistrationConstants.RETRY_ATTEMPT_ID + (retry + 1));
					label.setVisible(true);
					label.setText(String.valueOf(retry + 1));
					label.setAlignment(Pos.CENTER);
					fpRetryBox.getChildren().add(label);
				}

				String threshold = getValueFromApplicationContext(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);

				thresholdLabel.setAlignment(Pos.CENTER);
				thresholdLabel.setText(RegistrationUIConstants.THRESHOLD.concat("  ").concat(threshold)
						.concat(RegistrationConstants.PERCENTAGE));
				thresholdPane1.setPercentWidth(Double.parseDouble(threshold));
				thresholdPane2.setPercentWidth(100.00 - (Double.parseDouble(threshold)));
			}

			continueBtn.setDisable(true);

			scanBtn.setDisable(true);

			EventHandler<Event> mouseClick = event -> {
				if (event.getSource() instanceof GridPane) {
					GridPane sourcePane = (GridPane) event.getSource();
					sourcePane.requestFocus();
					selectedPane = sourcePane;
					scanBtn.setDisable(true);
					duplicateCheckLbl.setText(RegistrationConstants.EMPTY);

					final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
						public void handle(final MouseEvent mouseEvent) {

							LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"Mouse Event by attempt Started");

							if (bioService.isMdmEnabled()) {
								FingerprintDetailsDTO fingerprintDetailsDTO = getFingerprintBySelectedPane().findFirst()
										.orElse(null);

								if (fingerprintDetailsDTO != null) {
									String eventString = mouseEvent.toString();
									int index = eventString.indexOf(RegistrationConstants.RETRY_ATTEMPT_ID);

									if (index == -1) {
										String text = "Text[text=";
										index = eventString.indexOf(text) + text.length() + 1;

									} else {
										index = index + RegistrationConstants.RETRY_ATTEMPT_ID.length();
									}

									if (index != -1) {
										ImageView streamImage;
										Label qualityTextLabel;
										ProgressBar progressBar;
										Label qualityScore;
										if (fingerprintDetailsDTO.getFingerType()
												.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {

											// Set Stream image
											streamImage = leftHandPalmImageview;
											// Quality Label
											qualityTextLabel = leftSlapQualityScore;
										} else if (fingerprintDetailsDTO.getFingerType()
												.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
											// Set Stream image
											streamImage = rightHandPalmImageview;
											// Quality Label
											qualityTextLabel = rightSlapQualityScore;
										} else {
											// Set Stream image
											streamImage = thumbImageview;
											// Quality Label
											qualityTextLabel = thumbsQualityScore;
										}

										// Progress BAr
										progressBar = fpProgress;
										// Progress Bar Quality Score
										qualityScore = qualityText;

										try {
											updateByAttempt(fingerprintDetailsDTO.getFingerType(),
													Character.getNumericValue(eventString.charAt(index)), streamImage,
													qualityTextLabel, progressBar, qualityScore);

											LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME,
													APPLICATION_ID, "Mouse Event by attempt Ended");
										} catch (RuntimeException runtimeException) {
											LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME,
													APPLICATION_ID, runtimeException.getMessage()
															+ ExceptionUtils.getStackTrace(runtimeException));

										}
									}

								}
							}

						}
					};
					if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
						fpProgress.setProgress(0);
						for (int attempt = 0; attempt < Integer.parseInt(getValueFromApplicationContext(
								RegistrationConstants.FINGERPRINT_RETRIES_COUNT)); attempt++) {
							fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + (attempt + 1)).getStyleClass()
									.clear();
							fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + (attempt + 1)).getStyleClass()
									.add(RegistrationConstants.QUALITY_LABEL_GREY);
							fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + (attempt + 1))
									.setOnMouseClicked(mouseEventHandler);
						}

						String fpThreshold = RegistrationConstants.EMPTY;
						if (leftHandPalmPane.getId().equals(selectedPane.getId())) {
							lefhPalmTrackerImg.setVisible(true);
							rightPalmTrackerImg.setVisible(false);
							thumbTrackerImg.setVisible(false);
							fpThreshold = getValueFromApplicationContext(
									RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
							thresholdLabel.setText(RegistrationConstants.EMPTY);
						} else if (rightHandPalmPane.getId().equals(selectedPane.getId())) {
							rightPalmTrackerImg.setVisible(true);
							lefhPalmTrackerImg.setVisible(false);
							thumbTrackerImg.setVisible(false);
							fpThreshold = getValueFromApplicationContext(
									RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD);
							thresholdLabel.setText(RegistrationConstants.EMPTY);
						} else if (thumbPane.getId().equals(selectedPane.getId())) {
							thumbTrackerImg.setVisible(true);
							rightPalmTrackerImg.setVisible(false);
							lefhPalmTrackerImg.setVisible(false);
							fpThreshold = getValueFromApplicationContext(
									RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD);
							thresholdLabel.setText(RegistrationConstants.EMPTY);
						}
						thresholdPane1.setPercentWidth(Double.parseDouble(fpThreshold));
						thresholdPane2.setPercentWidth(100.00 - (Double.parseDouble(fpThreshold)));
						thresholdLabel.setAlignment(Pos.CENTER);
						thresholdLabel.setText(RegistrationUIConstants.THRESHOLD.concat("  ").concat(fpThreshold)
								.concat(RegistrationConstants.PERCENTAGE));

					}

					exceptionFingersCount();

					// Get the Fingerprint from RegistrationDTO based on
					// selected Fingerprint Pane
					FingerprintDetailsDTO fpDetailsDTO = getFingerprintBySelectedPane().findFirst().orElse(null);

					if ((leftHandPalmPane.getId().equals(selectedPane.getId()) && leftSlapExceptionCount < 4
							&& leftSlapCount < Integer.parseInt(
									getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)))
							&& enableCapture(fpDetailsDTO, RegistrationConstants.FINGERPRINT_SLAB_LEFT,
									RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD)
							|| (rightHandPalmPane.getId().equals(selectedPane.getId()) && rightSlapExceptionCount < 4
									&& rightSlapCount < Integer.parseInt(getValueFromApplicationContext(
											RegistrationConstants.FINGERPRINT_RETRIES_COUNT)))
									&& (enableCapture(fpDetailsDTO, RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
											RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD))
							|| (thumbPane.getId().equals(selectedPane.getId()) && thumbExceptionCount < 2
									&& thumbCount < Integer.parseInt(getValueFromApplicationContext(
											RegistrationConstants.FINGERPRINT_RETRIES_COUNT)))
									&& (enableCapture(fpDetailsDTO, RegistrationConstants.FINGERPRINT_SLAB_THUMBS,
											RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD))) {
						scanBtn.setDisable(false);
					}
					if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {

						if (fpDetailsDTO != null) {

							double qualityScore = findQualityScore(fpDetailsDTO);

							fpProgress.setProgress(fpDetailsDTO != null ? qualityScore / 100 : 0);

							qualityText.setText(getQualityScore(qualityScore));

							String fingerprintThreshold = getThresholdKeyByBioType(fpDetailsDTO.getFingerType());

							Label qualityScoreLabel = fpDetailsDTO.getFingerType()
									.equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)
											? leftSlapQualityScore
											: fpDetailsDTO.getFingerType()
													.equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)
															? rightSlapQualityScore
															: thumbsQualityScore;

							qualityScoreLabel.setText(getQualityScore(qualityScore));
							updateRetryBox(fpDetailsDTO,
									Double.parseDouble(getValueFromApplicationContext(fingerprintThreshold)));
						} else {
							qualityText.setText(RegistrationConstants.EMPTY);
						}
					}
				}
			};

			// Add event handler object to mouse click event
			leftHandPalmPane.setOnMouseClicked(mouseClick);
			rightHandPalmPane.setOnMouseClicked(mouseClick);
			thumbPane.setOnMouseClicked(mouseClick);

			leftSlapThresholdScoreLbl.setText(getQualityScore(Double.parseDouble(
					getValueFromApplicationContext(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD))));

			rightSlapThresholdScoreLbl.setText(getQualityScore(Double.parseDouble(
					getValueFromApplicationContext(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD))));

			thumbsThresholdScoreLbl.setText(getQualityScore(Double
					.parseDouble(getValueFromApplicationContext(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD))));

			loadingImageFromSessionContext();

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Loading of FingerprintCapture screen ended");
		} catch (

		RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"%s -> Exception while initializing Fingerprint Capture page for user registration  %s",
							RegistrationConstants.USER_REG_FINGERPRINT_PAGE_LOAD_EXP,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException)));

			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_FINGERPRINT_PAGE_LOAD_EXP,
					String.format("Exception while initializing Fingerprint Capture page for user registration  %s",
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException)));
		}
	}

	private double findQualityScore(FingerprintDetailsDTO fpDetailsDTO) {
		if (bioService.isMdmEnabled()) {
			if (bioService.getBioQualityScores(fpDetailsDTO.getFingerType(), fpDetailsDTO.getNumRetry()) != null)
				return bioService.getBioQualityScores(fpDetailsDTO.getFingerType(), fpDetailsDTO.getNumRetry());
			return fpDetailsDTO.getQualityScore();
		}
		return fpDetailsDTO.getQualityScore();
	}

	private double findQualityScore(FingerprintDetailsDTO fpDetailsDTO, int attempt) {
		if (bioService.isMdmEnabled()) {
			if (bioService.getBioQualityScores(fpDetailsDTO.getFingerType(), attempt) != null)
				return bioService.getBioQualityScores(fpDetailsDTO.getFingerType(), attempt);
			return fpDetailsDTO.getQualityScore();
		}
		return fpDetailsDTO.getQualityScore();
	}

	private void setImagesOnHover() {
		Image backInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK_FOCUSED));
		Image backImage = new Image(getClass().getResourceAsStream(RegistrationConstants.BACK));
		Image scanInWhite = new Image(getClass().getResourceAsStream(RegistrationConstants.SCAN_FOCUSED));
		Image scanImage = new Image(getClass().getResourceAsStream(RegistrationConstants.SCAN));

		backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				backImageView.setImage(backInWhite);
			} else {
				backImageView.setImage(backImage);
			}
		});
		scanBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				scanImageView.setImage(scanInWhite);
			} else {
				scanImageView.setImage(scanImage);
			}
		});
		startOverBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				startOverImageView.setImage(scanInWhite);
			} else {
				startOverImageView.setImage(scanImage);
			}
		});
	}

	/**
	 * Enable capture.
	 *
	 * @param fpDetailsDTO
	 *            the fp details DTO
	 * @param palm
	 *            the palm
	 * @param fpthreshold
	 *            the fpthreshold
	 * @return true, if successful
	 */
	private boolean enableCapture(FingerprintDetailsDTO fpDetailsDTO, String palm, String fpthreshold) {
		return fpDetailsDTO == null
				|| (fpDetailsDTO.getFingerType().equals(palm) && fpDetailsDTO.getNumRetry() < Integer
						.parseInt(getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)));
	}

	private void updateRetryBox(FingerprintDetailsDTO fpDetailsDTO, double threshold) {

		int retries = fpDetailsDTO.getNumRetry();

		for (int attempt = 1; attempt <= retries; attempt++) {
			if (findQualityScore(fpDetailsDTO, attempt) >= threshold) {
				clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_GREEN, attempt);
				fpProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
				fpProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
				qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
				qualityText.getStyleClass().add(RegistrationConstants.LABEL_GREEN);

			} else {
				clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_RED, attempt);
				fpProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
				fpProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
				qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);
				qualityText.getStyleClass().add(RegistrationConstants.LABEL_RED);
			}
		}
	}

	/**
	 * Clear image.
	 */
	@SuppressWarnings("unchecked")
	public void clearImage() {

		exceptionFingersCount();
		if (leftSlapExceptionCount == 4) {
			removeFingerPrint(RegistrationConstants.FINGERPRINT_SLAB_LEFT, leftHandPalmImageview, leftSlapQualityScore,
					RegistrationConstants.LEFTPALM_IMG_PATH, leftSlapAttempt);

		}
		if (rightSlapExceptionCount == 4) {
			removeFingerPrint(RegistrationConstants.FINGERPRINT_SLAB_RIGHT, rightHandPalmImageview,
					rightSlapQualityScore, RegistrationConstants.RIGHTPALM_IMG_PATH, rightSlapAttempt);

		}
		if (thumbExceptionCount == 2) {
			removeFingerPrint(RegistrationConstants.FINGERPRINT_SLAB_THUMBS, thumbImageview, thumbsQualityScore,
					RegistrationConstants.THUMB_IMG_PATH, thumbSlapAttempt);

		}
		List<BiometricExceptionDTO> tempExceptionList = (List<BiometricExceptionDTO>) SessionContext.map()
				.get(RegistrationConstants.NEW_BIOMETRIC_EXCEPTION);

		if ((tempExceptionList == null || tempExceptionList.isEmpty())
				&& !(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER) && (Boolean) SessionContext
						.userContext().getUserMap().get(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION)) {
			leftHandPalmImageview.setImage(
					new Image(getClass().getResource(RegistrationConstants.LEFTPALM_IMG_PATH).toExternalForm()));
			leftSlapQualityScore.setText(RegistrationConstants.EMPTY);
			leftSlapAttempt.setText(RegistrationConstants.HYPHEN);
			rightHandPalmImageview.setImage(
					new Image(getClass().getResource(RegistrationConstants.RIGHTPALM_IMG_PATH).toExternalForm()));
			rightSlapQualityScore.setText(RegistrationConstants.EMPTY);
			rightSlapAttempt.setText(RegistrationConstants.HYPHEN);
			thumbImageview
					.setImage(new Image(getClass().getResource(RegistrationConstants.THUMB_IMG_PATH).toExternalForm()));
			thumbsQualityScore.setText(RegistrationConstants.EMPTY);
			thumbSlapAttempt.setText(RegistrationConstants.HYPHEN);
		}
		List<BiometricExceptionDTO> bioExceptionList = (List<BiometricExceptionDTO>) SessionContext.map()
				.get(RegistrationConstants.OLD_BIOMETRIC_EXCEPTION);
		if (bioExceptionList == null || bioExceptionList.isEmpty()) {
			if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				findingExceptionDifference(tempExceptionList, bioExceptionList);
			}
			bioExceptionList = tempExceptionList;
		} else {
			findingExceptionDifference(tempExceptionList, bioExceptionList);

		}
		SessionContext.map().put(RegistrationConstants.OLD_BIOMETRIC_EXCEPTION, tempExceptionList);

		if (leftSlapExceptionCount == 4 && rightSlapExceptionCount == 4 && thumbExceptionCount == 2) {
			continueBtn.setDisable(false);
		} else
			populateException();
	}

	public void initializeCaptureCount() {
		leftSlapCount = 0;
		rightSlapCount = 0;
		thumbCount = 0;
	}

	private void findingExceptionDifference(List<BiometricExceptionDTO> tempExceptionList,
			List<BiometricExceptionDTO> bioExceptionList) {
		List<String> bioList1 = null;
		List<String> bioList = bioExceptionList.stream().map(bio -> bio.getMissingBiometric())
				.collect(Collectors.toList());
		if (null != tempExceptionList) {
			bioList1 = tempExceptionList.stream().map(bio -> bio.getMissingBiometric()).collect(Collectors.toList());
		}

		List<String> changedException = (List<String>) CollectionUtils.disjunction(bioList, bioList1);

		changedException.forEach(biometricException -> {
			if (biometricException.contains(RegistrationConstants.LEFT.toLowerCase())
					&& !biometricException.contains(RegistrationConstants.THUMB)
					&& !biometricException.contains(RegistrationConstants.EYE)) {
				removeFingerPrint(RegistrationConstants.LEFTPALM, leftHandPalmImageview, leftSlapQualityScore,
						RegistrationConstants.LEFTPALM_IMG_PATH, leftSlapAttempt);
			} else if (biometricException.contains(RegistrationConstants.RIGHT.toLowerCase())
					&& !biometricException.contains(RegistrationConstants.THUMB)
					&& !biometricException.contains(RegistrationConstants.EYE)) {
				removeFingerPrint(RegistrationConstants.RIGHTPALM, rightHandPalmImageview, rightSlapQualityScore,
						RegistrationConstants.RIGHTPALM_IMG_PATH, rightSlapAttempt);
			} else if (biometricException.contains(RegistrationConstants.THUMB)) {
				removeFingerPrint(RegistrationConstants.THUMBS, thumbImageview, thumbsQualityScore,
						RegistrationConstants.THUMB_IMG_PATH, thumbSlapAttempt);
			}
		});
	}

	/**
	 * Single biomteric capture check.
	 */
	private void singleBiomtericCaptureCheck() {

		if (!validateFingerPrints()) {
			continueBtn.setDisable(true);
		}

		long irisCountApplicant = 0;
		long irisCountIntroducer = 0;

		if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getBiometricDTO() != null) {

			irisCountApplicant = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
					.getBiometricExceptionDTO().stream()
					.filter(bio -> bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.IRIS)).count();

			irisCountIntroducer = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getBiometricExceptionDTO().stream()
					.filter(bio -> bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.IRIS)).count();

		}

		if (!RegistrationConstants.DISABLE
				.equalsIgnoreCase(getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))
				&& getRegistrationDTOFromSession() != null
				&& getRegistrationDTOFromSession().getSelectionListDTO() != null
				&& !getRegistrationDTOFromSession().getSelectionListDTO().isBiometrics() && irisCountApplicant < 2
				&& irisCountIntroducer < 2) {
			continueBtn.setDisable(false);
		}
	}

	/**
	 * Removes the finger print.
	 *
	 * @param handSlap
	 *            the hand slap
	 * @param handSlapImageView
	 *            the hand slap image view
	 * @param handSlapQualityScoreLabel
	 *            the hand slap quality score label
	 * @param HandSlapImagePath
	 *            the hand slap image path
	 */
	private void removeFingerPrint(String handSlap, ImageView handSlapImageView, Label handSlapQualityScoreLabel,
			String handSlapImagePath, Label attemptLabel) {
		Iterator<FingerprintDetailsDTO> iterator;

		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			iterator = getBiometricDTOFromSession().getOperatorBiometricDTO().getFingerprintDetailsDTO().iterator();
		} else if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			iterator = getRegistrationDTOFromSession() != null
					? getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
							.getFingerprintDetailsDTO().iterator()
					: null;
		} else {
			iterator = getRegistrationDTOFromSession() != null
					? getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
							.getFingerprintDetailsDTO().iterator()
					: null;
		}

		while (iterator != null && iterator.hasNext()) {
			FingerprintDetailsDTO value = iterator.next();
			if (value.getFingerType().contains(handSlap)) {
				iterator.remove();
				break;
			}
		}

		handSlapImageView.setImage(new Image(getClass().getResource(handSlapImagePath).toExternalForm()));
		handSlapQualityScoreLabel.setText(RegistrationConstants.EMPTY);
		attemptLabel.setText(RegistrationConstants.HYPHEN);
		clearingProgressBar();
	}

	/**
	 * Clear finger print DTO.
	 */
	public void clearFingerPrintDTO() {

		// Clear All fingerprints images/scores
		clearBiometrics(RegistrationConstants.FINGERPRINT);

		initializeCaptureCount();
		removeFingerPrint(RegistrationConstants.FINGERPRINT_SLAB_LEFT, leftHandPalmImageview, leftSlapQualityScore,
				RegistrationConstants.LEFTPALM_IMG_PATH, leftSlapAttempt);
		removeFingerPrint(RegistrationConstants.FINGERPRINT_SLAB_RIGHT, rightHandPalmImageview, rightSlapQualityScore,
				RegistrationConstants.RIGHTPALM_IMG_PATH, rightSlapAttempt);
		removeFingerPrint(RegistrationConstants.FINGERPRINT_SLAB_THUMBS, thumbImageview, thumbsQualityScore,
				RegistrationConstants.THUMB_IMG_PATH, thumbSlapAttempt);

		clearingProgressBar();
		singleBiomtericCaptureCheck();
		faceCaptureController.clearExceptionImage();
	}

	private void clearingProgressBar() {
		if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			fpProgress.setProgress(0);
			fpProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
			fpProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);

			qualityText.setText(RegistrationConstants.EMPTY);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);

			for (int attempt = 0; attempt < Integer.parseInt(
					getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)); attempt++) {
				fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + (attempt + 1)).getStyleClass().clear();
				fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + (attempt + 1)).getStyleClass()
						.add(RegistrationConstants.QUALITY_LABEL_GREY);
			}
		}
	}

	/**
	 * Exception fingers count.
	 */
	private void exceptionFingersCount() {
		leftSlapExceptionCount = 0;
		rightSlapExceptionCount = 0;
		thumbExceptionCount = 0;
		leftHandExceptions.clear();
		rightHandExceptions.clear();
		thumbsExceptions.clear();

		List<BiometricExceptionDTO> biometricExceptionDTOs;
		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			biometricExceptionDTOs = getBiometricDTOFromSession().getOperatorBiometricDTO().getBiometricExceptionDTO();
		} else if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			biometricExceptionDTOs = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getBiometricExceptionDTO();
		} else {
			biometricExceptionDTOs = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
					.getBiometricExceptionDTO();
		}
		for (BiometricExceptionDTO biometricExceptionDTO : biometricExceptionDTOs) {

			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.LEFT.toLowerCase())
					&& biometricExceptionDTO.isMarkedAsException())
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)) {
				getExceptionIdentifier(leftHandExceptions, biometricExceptionDTO.getMissingBiometric());
				leftSlapExceptionCount++;
			}
			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.RIGHT.toLowerCase())
					&& biometricExceptionDTO.isMarkedAsException())
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
					&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)) {
				getExceptionIdentifier(rightHandExceptions, biometricExceptionDTO.getMissingBiometric());
				rightSlapExceptionCount++;
			}
			if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
					&& biometricExceptionDTO.isMarkedAsException())) {
				getExceptionIdentifier(thumbsExceptions, biometricExceptionDTO.getMissingBiometric());
				thumbExceptionCount++;
			}
		}
	}

	/**
	 * Loading image from session context.
	 */
	private void loadingImageFromSessionContext() {
		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			if (null != getBiometricDTOFromSession()) {
				loadImage(getBiometricDTOFromSession().getOperatorBiometricDTO().getFingerprintDetailsDTO());
			}
		} else if (null != getRegistrationDTOFromSession()
				&& getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			loadImage(getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getFingerprintDetailsDTO());
		} else {
			if (null != getRegistrationDTOFromSession()) {
				loadImage(getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
						.getFingerprintDetailsDTO());
			}
		}
	}

	/**
	 * Load image.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 */
	private void loadImage(List<FingerprintDetailsDTO> fingerprintDetailsDTO) {
		fingerprintDetailsDTO.forEach(item -> {
			if (item.getFingerType().equals(RegistrationConstants.LEFTPALM)) {
				leftHandPalmImageview.setImage(new Image(new ByteArrayInputStream(item.getFingerPrint())));
				leftSlapQualityScore.setText(getQualityScore(item.getQualityScore()));
			} else if (item.getFingerType().equals(RegistrationConstants.RIGHTPALM)) {
				rightHandPalmImageview.setImage(new Image(new ByteArrayInputStream(item.getFingerPrint())));
				rightSlapQualityScore.setText(getQualityScore(item.getQualityScore()));
			} else if (item.getFingerType().equals(RegistrationConstants.THUMBS)) {
				thumbImageview.setImage(new Image(new ByteArrayInputStream(item.getFingerPrint())));
				thumbPane.getStyleClass().add(RegistrationConstants.FINGERPRINT_PANES_SELECTED);
				thumbsQualityScore.setText(getQualityScore(item.getQualityScore()));
			}
		});
	}

	/**
	 * Scan.
	 */
	public void scan() {
		try {
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to capture fingerprint for user registration");
			FingerprintDetailsDTO fpDetailsDTO = getFingerprintBySelectedPane().findFirst().orElse(null);
			ImageView imageView = null;
			if ((fpDetailsDTO == null || fpDetailsDTO.getNumRetry() < Integer
					.parseInt(getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)))
					|| ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))) {

				auditFactory.audit(getAuditEventForScan(selectedPane.getId()), Components.REG_BIOMETRICS,
						SessionContext.userId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
				String FingerType = "";
				if (selectedPane.getId() == leftHandPalmPane.getId()) {
					exception = leftHandExceptions;
					FingerType = RegistrationConstants.LEFTPALM;
					imageView = leftHandPalmImageview;
					requestedScore = getValueFromApplicationContext(
							RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
				} else if (selectedPane.getId() == rightHandPalmPane.getId()) {
					exception = rightHandExceptions;
					FingerType = RegistrationConstants.RIGHTPALM;
					imageView = rightHandPalmImageview;
					requestedScore = getValueFromApplicationContext(
							RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD);
				} else {
					exception = thumbsExceptions;
					FingerType = RegistrationConstants.THUMBS;
					imageView = thumbImageview;
					requestedScore = getValueFromApplicationContext(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD);
				}
				scanPopUpViewController.init(this, RegistrationUIConstants.FINGERPRINT);
				if (bioService.isMdmEnabled()) {
					LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Starting the stream....");
					streamer.startStream(findFingerPrintType(FingerType), scanPopUpViewController.getScanImage(),
							imageView);
				}
			}

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Scanning of fingers ended");

		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, String.format(
					"%s -> Exception while Opening pop-up screen to capture fingerprint for user registration  %s",
					RegistrationConstants.USER_REG_FINGERPRINT_CAPTURE_POPUP_LOAD_EXP,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.UNABLE_LOAD_FINGERPRINT_SCAN_POPUP);
		}
	}

	private AuditEvent getAuditEventForScan(String selectedPaneId) {
		AuditEvent auditEvent;
		if (StringUtils.containsIgnoreCase(selectedPane.getId(), RegistrationConstants.LEFT)) {
			auditEvent = AuditEvent.REG_BIO_LEFT_SLAP_SCAN;
		} else if (StringUtils.containsIgnoreCase(selectedPane.getId(), RegistrationConstants.RIGHT)) {
			auditEvent = AuditEvent.REG_BIO_RIGHT_SLAP_SCAN;
		} else {
			auditEvent = AuditEvent.REG_BIO_THUMBS_SCAN;
		}

		return auditEvent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.controller.BaseController#scan(javafx.stage.Stage)
	 */
	@Override
	public void scan(Stage popupStage) {

		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			operatorBiometricScan(popupStage);
		} else {
			applicantBiometricScan(popupStage);
		}

	}

	/**
	 * Operator biometric scan.
	 *
	 * @param popupStage
	 *            the popup stage
	 */
	private void operatorBiometricScan(Stage popupStage) {
		try {
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Entering into operator Biometric Scan");

			FingerprintDetailsDTO detailsDTO = null;

			List<FingerprintDetailsDTO> fingerprintDetailsDTOs = getBiometricDTOFromSession().getOperatorBiometricDTO()
					.getFingerprintDetailsDTO();

			if (fingerprintDetailsDTOs == null || fingerprintDetailsDTOs.isEmpty()) {
				fingerprintDetailsDTOs = new ArrayList<>(3);
				getBiometricDTOFromSession().getOperatorBiometricDTO().setFingerprintDetailsDTO(fingerprintDetailsDTOs);
			}

			if (selectedPane.getId() == leftHandPalmPane.getId()) {

				scanFingers(fingerprintDetailsDTOs, RegistrationConstants.LEFTPALM,
						RegistrationConstants.LEFTHAND_SEGMNTD_FILE_PATHS_USERONBOARD, leftHandPalmImageview,
						leftSlapQualityScore, popupStage, leftHandPalmPane,
						Double.parseDouble(
								getValueFromApplicationContext(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD)),
						leftSlapAttempt);

			} else if (selectedPane.getId() == rightHandPalmPane.getId()) {

				scanFingers(fingerprintDetailsDTOs, RegistrationConstants.RIGHTPALM,

						RegistrationConstants.RIGHTHAND_SEGMNTD_FILE_PATHS_USERONBOARD, rightHandPalmImageview,

						rightSlapQualityScore, popupStage, rightHandPalmPane,
						Double.parseDouble(
								getValueFromApplicationContext(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD)),
						rightSlapAttempt);

			} else if (selectedPane.getId() == thumbPane.getId()) {

				scanFingers(fingerprintDetailsDTOs, RegistrationConstants.THUMBS,

						RegistrationConstants.THUMBS_SEGMNTD_FILE_PATHS_USERONBOARD, thumbImageview, thumbsQualityScore,
						popupStage, thumbPane,
						Double.parseDouble(
								getValueFromApplicationContext(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD)),
						thumbSlapAttempt);

			}

		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"Exception while getting the scanned Finger details for user registration: %s caused by %s",
							runtimeException.getMessage(),
							runtimeException.getCause() + ExceptionUtils.getStackTrace(runtimeException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGERPRINT_SCANNING_ERROR);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, String.format(
					"Exception while getting the scanned Finger details for user registration: %s caused by %s",
					regBaseCheckedException.getMessage(),
					regBaseCheckedException.getCause() + ExceptionUtils.getStackTrace(regBaseCheckedException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGERPRINT_SCANNING_ERROR);
		}
		LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scanning Finger has ended");
	}

	protected String getOnboardFingertype(String fingerType) {
		if (bioService.isMdmEnabled()) {
			fingerType = fingerType + "_onboard";
		}
		return fingerType;
	}

	/**
	 * Applicant biometric scan.
	 *
	 * @param popupStage
	 *            the popup stage
	 */
	private void applicantBiometricScan(Stage popupStage) {
		try {

			FingerprintDetailsDTO detailsDTO = null;

			List<FingerprintDetailsDTO> fingerprintDetailsDTOs;

			if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
				fingerprintDetailsDTOs = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
						.getFingerprintDetailsDTO();
			} else {
				fingerprintDetailsDTOs = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
						.getFingerprintDetailsDTO();
			}

			if (fingerprintDetailsDTOs == null || fingerprintDetailsDTOs.isEmpty()) {
				fingerprintDetailsDTOs = new ArrayList<>(3);
				if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
					getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
							.setFingerprintDetailsDTO(fingerprintDetailsDTOs);
				} else {
					getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
							.setFingerprintDetailsDTO(fingerprintDetailsDTOs);
				}
			}

			if (selectedPane.getId() == leftHandPalmPane.getId()) {
				scanFingers(fingerprintDetailsDTOs, RegistrationConstants.FINGERPRINT_SLAB_LEFT,
						RegistrationConstants.LEFTHAND_SEGMNTD_FILE_PATHS, leftHandPalmImageview, leftSlapQualityScore,
						popupStage, leftHandPalmPane,
						Double.parseDouble(
								getValueFromApplicationContext(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD)),
						leftSlapAttempt);

			} else if (selectedPane.getId() == rightHandPalmPane.getId()) {
				if (SessionContext.map().containsKey(RegistrationConstants.DUPLICATE_FINGER)) {
					scanFingers(fingerprintDetailsDTOs, RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
							RegistrationConstants.RIGHTHAND_SEGMNTD_FILE_PATHS, rightHandPalmImageview,
							rightSlapQualityScore, popupStage, rightHandPalmPane,
							Double.parseDouble(getValueFromApplicationContext(
									RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD)),
							rightSlapAttempt);
				} else {
					scanFingers(fingerprintDetailsDTOs, RegistrationConstants.FINGERPRINT_SLAB_RIGHT,
							RegistrationConstants.RIGHTHAND_SEGMNTD_DUPLICATE_FILE_PATHS, rightHandPalmImageview,
							rightSlapQualityScore, popupStage, rightHandPalmPane,
							Double.parseDouble(getValueFromApplicationContext(
									RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD)),
							rightSlapAttempt);
				}

			} else if (selectedPane.getId() == thumbPane.getId()) {
				scanFingers(fingerprintDetailsDTOs, RegistrationConstants.FINGERPRINT_SLAB_THUMBS,
						RegistrationConstants.THUMBS_SEGMNTD_FILE_PATHS, thumbImageview, thumbsQualityScore, popupStage,
						thumbPane,
						Double.parseDouble(
								getValueFromApplicationContext(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD)),
						thumbSlapAttempt);

			}

		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"Exception while getting the scanned Finger details for user registration: %s caused by %s",
							runtimeException.getMessage(),
							runtimeException.getCause() + ExceptionUtils.getStackTrace(runtimeException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGERPRINT_SCANNING_ERROR);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, String.format(
					"Exception while getting the scanned Finger details for user registration: %s caused by %s",
					regBaseCheckedException.getMessage(),
					regBaseCheckedException.getCause() + ExceptionUtils.getStackTrace(regBaseCheckedException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGERPRINT_SCANNING_ERROR);
		}
		LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scanning Finger has ended");

	}

	/**
	 * Scan fingers.
	 *
	 * @param detailsDTO
	 *            the details DTO
	 * @param fingerprintDetailsDTOs
	 *            the fingerprint details DT os
	 * @param fingerType
	 *            the finger type
	 * @param segmentedFingersPath
	 *            the segmented fingers path
	 * @param fingerImageView
	 *            the finger image view
	 * @param scoreLabel
	 *            the score label
	 * @param popupStage
	 *            the popup stage
	 * @param parentPane
	 *            the parent pane
	 * @param thresholdValue
	 *            the threshold value
	 * @param attemptSlap
	 *            the attempt slap
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private void scanFingers(List<FingerprintDetailsDTO> fingerprintDetailsDTOs, String fingerType,
			String[] segmentedFingersPath, ImageView fingerImageView, Label scoreLabel, Stage popupStage,
			GridPane parentPane, Double thresholdValue, Label attemptSlap) throws RegBaseCheckedException {

		LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Entering Scan Fingers method");

		ImageView imageView = fingerImageView;
		Label qualityScoreLabel = scoreLabel;
		int attempt = 0;
		Instant start = null;
		Instant end = null;

		if (fingerprintDetailsDTOs != null) {

			for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
				if (fingerprintDetailsDTO.getFingerType() != null
						&& fingerprintDetailsDTO.getSegmentedFingerprints() != null
						&& fingerprintDetailsDTO.getFingerType().equals(fingerType)) {
					attempt = fingerprintDetailsDTO.getNumRetry();

					break;
				}
			}

		}

		attempt = attempt != 0 ? attempt + 1 : 1;

		FingerprintDetailsDTO detailsDTO;
		try {

			// passing the object with details like type, count, exception, requestedScore,
			// timout,
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Calling FingerPrint ImageAs DTO");
			start = Instant.now();

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Capturing Fingerprints for attempt : " + attempt);

			detailsDTO = bioService.getFingerPrintImageAsDTO(new RequestDetail(findFingerPrintType(fingerType),
					getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT), 1, requestedScore,
					exception), attempt);
			end = Instant.now();
			streamer.stop();
			bioService.segmentFingerPrintImage(detailsDTO, segmentedFingersPath, fingerType);
		} catch (RegBaseCheckedException | IOException exception) {
			streamer.stop();

			generateAlert(RegistrationConstants.ALERT_INFORMATION,
					RegistrationUIConstants.getMessageLanguageSpecific(exception.getMessage().substring(0, 3)
							+ RegistrationConstants.UNDER_SCORE + RegistrationConstants.MESSAGE.toUpperCase()));

			LOGGER.error(LOG_REG_GUARDIAN_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s Exception while getting the scanned finger details for user registration: %s ",
							exception.getMessage(), ExceptionUtils.getStackTrace(exception)));
			return;
		}

		LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Validating Captured FingerPrints");
		if (detailsDTO.isCaptured() && bioService.isValidFingerPrints(detailsDTO,false)) {
			
			captureTimeValue.setText(Duration.between(start, end).toString().replace("PT", ""));

			boolean isNotMatched = true;

			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {

				LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Verifying Local Deduplication check of captured fingerprints against Operator Biometrics");
				isNotMatched = generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.FP_CAPTURE_SUCCESS, ()->{return bioService.validateBioDeDup(detailsDTO.getSegmentedFingerprints());}, scanPopUpViewController);
			}

			popupStage.close();
			if (!isNotMatched) {

				LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Failure :Local Deduplication found on captured fingerprints against Operator Biometrics");
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.FINGERPRINT_DUPLICATION_ALERT);
				return;
			}

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Updating captured fingerprints in session");
			updateFingerBiometricsSession(fingerprintDetailsDTOs, detailsDTO);

			if (!bioService.isMdmEnabled()) {
				scanPopUpViewController.getScanImage().setImage(convertBytesToImage(detailsDTO.getFingerPrint()));
				imageView.setImage(convertBytesToImage(detailsDTO.getFingerPrint()));
			} else {
				detailsDTO.setFingerPrint(streamer.imageBytes);
				// Add Bio Stream image
				streamer.setBioStreamImages(null, detailsDTO.getFingerType(), attempt);

				if (detailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {

					leftHandPalmImageview.setImage((bioService.getBioStreamImage(detailsDTO.getFingerType(), attempt)));

				} else if (detailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {

					rightHandPalmImageview
							.setImage((bioService.getBioStreamImage(detailsDTO.getFingerType(), attempt)));
				} else {

					thumbImageview.setImage((bioService.getBioStreamImage(detailsDTO.getFingerType(), attempt)));

				}
			}

			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {

				LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Updating progress Bar,Text and attempts Box in UI");

				double qualityScore = findQualityScore(detailsDTO);

				fpProgress.setProgress(qualityScore / 100);
				qualityText.setText(getQualityScore(qualityScore));
				qualityScoreLabel.setText(getQualityScore(qualityScore));
				attemptSlap.setText(String.valueOf(detailsDTO.getNumRetry()));

				if (qualityScore >= thresholdValue) {
					clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_GREEN, detailsDTO.getNumRetry());
					fpProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
					fpProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
					qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
					qualityText.getStyleClass().add(RegistrationConstants.LABEL_GREEN);
				} else {
					clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_RED, detailsDTO.getNumRetry());
					fpProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
					fpProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
					qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);
					qualityText.getStyleClass().add(RegistrationConstants.LABEL_RED);
				}
			}

			parentPane.getStyleClass().add(RegistrationConstants.FINGERPRINT_PANES_SELECTED);

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Verifying whether all Non Exception FingerPrints Captured or Not");
			if (bioService.isAllNonExceptionFingerprintsCaptured()) {

				LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Verifying whether all Non Exception FingerPrints Captured or Not :  Success");
				continueBtn.setDisable(false);
			} else {

				LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Verifying whether all Non Exception FingerPrints Captured or Not :  Failure");
				continueBtn.setDisable(true);
			}
			
		} else {

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating Captured FingerPrints Failed");
			
			streamer.stop();
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.FINGERPRINT_SCANNING_ERROR);
		}

	}

	private void updateFingerBiometricsSession(List<FingerprintDetailsDTO> fingerprintDetailsDTOs,
			FingerprintDetailsDTO detailsDTO) {

		FingerprintDetailsDTO fingerprintDetails = null;
		for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
			if (fingerprintDetailsDTO.getFingerType() != null

					&& fingerprintDetailsDTO.getFingerType().equals(detailsDTO.getFingerType())) {

				fingerprintDetails = fingerprintDetailsDTO;
				break;
			}
		}

		if (fingerprintDetails != null) {
			fingerprintDetailsDTOs.remove(fingerprintDetails);
		}
		fingerprintDetailsDTOs.add(detailsDTO);
	}

	/**
	 * Helper method to find the finger type mapping
	 * 
	 * @param fingerType
	 * @return String
	 */
	private String findFingerPrintType(String fingerType) {
		switch (fingerType) {
		case RegistrationConstants.LEFTPALM:
			fingerType = RegistrationConstants.FINGERPRINT_SLAB_LEFT;
			break;
		case RegistrationConstants.RIGHTPALM:
			fingerType = RegistrationConstants.FINGERPRINT_SLAB_RIGHT;
			break;
		case RegistrationConstants.THUMBS:
			fingerType = RegistrationConstants.FINGERPRINT_SLAB_THUMBS;
			break;
		default:
			break;
		}
		return fingerType;
	}

	/**
	 * {@code saveBiometricDetails} is to check the deduplication of captured finger
	 * prints.
	 */
	public void goToNextPage() {
		try {
			auditFactory.audit(AuditEvent.REG_BIO_FINGERPRINT_NEXT, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Navigating to Iris capture page for user registration started");

			exceptionFingersCount();
			if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				irisCaptureController.clearIrisBasedOnExceptions();
				userOnboardParentController.showCurrentPage(RegistrationConstants.FINGERPRINT_CAPTURE,
						getOnboardPageDetails(RegistrationConstants.FINGERPRINT_CAPTURE, RegistrationConstants.NEXT));
			} else {
				lefhPalmTrackerImg.setVisible(false);
				rightPalmTrackerImg.setVisible(false);
				thumbTrackerImg.setVisible(true);
				if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
					SessionContext.map().remove(RegistrationConstants.DUPLICATE_FINGER);

					SessionContext.map().put(RegistrationConstants.UIN_UPDATE_FINGERPRINTCAPTURE, false);
					if (RegistrationConstants.ENABLE.equalsIgnoreCase(
							getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))) {
						irisCaptureController.clearIrisBasedOnExceptions();
						SessionContext.map().put(RegistrationConstants.UIN_UPDATE_IRISCAPTURE, true);
					} else if (!RegistrationConstants.DISABLE.equalsIgnoreCase(
							getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG))) {
						SessionContext.map().put(RegistrationConstants.UIN_UPDATE_FACECAPTURE, true);
					} else {
						SessionContext.map().put(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW, true);
						faceCaptureController.checkForException();
						registrationPreviewController.setUpPreviewContent();
					}
					registrationController.showUINUpdateCurrentPage();
				} else {
					SessionContext.map().remove(RegistrationConstants.DUPLICATE_FINGER);
					irisCaptureController.clearIrisBasedOnExceptions();
					faceCaptureController.checkForException();

					registrationController.showCurrentPage(RegistrationConstants.FINGERPRINT_CAPTURE,
							getPageDetails(RegistrationConstants.FINGERPRINT_CAPTURE, RegistrationConstants.NEXT));
				}
			}
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Navigating to Iris capture page for user registration ended");
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format("%s -> Exception while navigating to Iris capture page for user registration  %s",
							RegistrationConstants.USER_REG_FINGERPRINT_CAPTURE_NEXT_SECTION_LOAD_EXP,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException)));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.FINGERPRINT_NAVIGATE_NEXT_SECTION_ERROR);
		}

	}

	/**
	 * {@code saveBiometricDetails} is to check the deduplication of captured finger
	 * prints.
	 */
	public void goToPreviousPage() {
		try {
			auditFactory.audit(AuditEvent.REG_BIO_FINGERPRINT_BACK, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Navigating to Demographic capture page for user registration started");

			exceptionFingersCount();
			if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				userOnboardParentController.showCurrentPage(RegistrationConstants.FINGERPRINT_CAPTURE,
						getOnboardPageDetails(RegistrationConstants.FINGERPRINT_CAPTURE,
								RegistrationConstants.PREVIOUS));
			} else {
				SessionContext.getInstance().getMapObject().remove(RegistrationConstants.DUPLICATE_FINGER);
				if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
					SessionContext.map().put(RegistrationConstants.FINGERPRINT_CAPTURE, false);

					if ((boolean) SessionContext.getInstance().getUserContext().getUserMap()
							.get(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION)) {
						SessionContext.map().put(RegistrationConstants.UIN_UPDATE_BIOMETRICEXCEPTION, true);
					} else {
						SessionContext.map().put(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN, true);
					}
					registrationController.showUINUpdateCurrentPage();

				} else {
					registrationController.showCurrentPage(RegistrationConstants.FINGERPRINT_CAPTURE,
							getPageDetails(RegistrationConstants.FINGERPRINT_CAPTURE, RegistrationConstants.PREVIOUS));
				}
			}
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Navigating to Demographic capture page for user registration is ended");
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					String.format(
							"%s -> Exception while navigating to Demographic capture page for user registration  %s",
							RegistrationConstants.USER_REG_FINGERPRINT_CAPTURE_PREV_SECTION_LOAD_EXP,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException)));

			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.FINGERPRINT_NAVIGATE_PREVIOUS_SECTION_ERROR);
		}
	}

	/**
	 * Validating finger prints.
	 *
	 * @return true, if successful
	 */
	private boolean validateFingerPrints() {
		try {
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating captured Fingerprints started");

			List<FingerprintDetailsDTO> segmentedFingerprintDetailsDTOs = new ArrayList<>();
			boolean isValid = false;
			boolean isleftHandSlapCaptured = false;
			boolean isrightHandSlapCaptured = false;
			boolean isthumbsCaptured = false;

			List<FingerprintDetailsDTO> fingerprintDetailsDTOs;

			if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				fingerprintDetailsDTOs = getBiometricDTOFromSession().getOperatorBiometricDTO()
						.getFingerprintDetailsDTO();
			} else if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
				fingerprintDetailsDTOs = getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
						.getFingerprintDetailsDTO();
			} else {
				fingerprintDetailsDTOs = getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
						.getFingerprintDetailsDTO();
			}

			for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
				if (fingerprintDetailsDTO.getSegmentedFingerprints() != null) {
					for (FingerprintDetailsDTO segmentedFingerprintDetailsDTO : fingerprintDetailsDTO
							.getSegmentedFingerprints()) {
						segmentedFingerprintDetailsDTOs.add(segmentedFingerprintDetailsDTO);
					}
				}
			}

			for (FingerprintDetailsDTO fingerprintDetailsDTO : fingerprintDetailsDTOs) {
				if (fingerprintDetailsDTO.getFingerType() != null) {

					if (validateQualityScore(fingerprintDetailsDTO)
							|| (boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
						if (fingerprintDetailsDTO.getFingerType().equalsIgnoreCase(
								RegistrationConstants.FINGERPRINT_SLAB_LEFT) || leftSlapExceptionCount >= 4) {
							isleftHandSlapCaptured = true;
						}
						if (fingerprintDetailsDTO.getFingerType().equalsIgnoreCase(
								RegistrationConstants.FINGERPRINT_SLAB_RIGHT) || rightSlapExceptionCount >= 4) {
							isrightHandSlapCaptured = true;

						}
						if (fingerprintDetailsDTO.getFingerType().equalsIgnoreCase(
								RegistrationConstants.FINGERPRINT_SLAB_THUMBS) || thumbExceptionCount >= 2) {
							isthumbsCaptured = true;
						}
					} else {
						return isValid;
					}

				}
			}

			if (fingerprintDetailsDTOs.isEmpty() && leftSlapExceptionCount >= 4 && rightSlapExceptionCount >= 4
					&& thumbExceptionCount >= 2) {
				isleftHandSlapCaptured = true;
				isrightHandSlapCaptured = true;
				isthumbsCaptured = true;
			}

			if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getSelectionListDTO() != null

					&& ((getRegistrationDTOFromSession().getSelectionListDTO().isBiometrics() && isleftHandSlapCaptured
							&& isrightHandSlapCaptured && isthumbsCaptured)

							|| !getRegistrationDTOFromSession().getSelectionListDTO().isBiometrics()
									&& (isleftHandSlapCaptured || isrightHandSlapCaptured || isthumbsCaptured))) {

				isValid = fingerdeduplicationCheck(segmentedFingerprintDetailsDTOs, isValid, fingerprintDetailsDTOs);

			} else {
				if (isleftHandSlapCaptured && isrightHandSlapCaptured && isthumbsCaptured) {
					try {
						if (dedupeMessage != null)
							dedupeMessage.setVisible(true);
						isValid = fingerdeduplicationCheck(segmentedFingerprintDetailsDTOs, isValid,
								fingerprintDetailsDTOs);
						if (dedupeMessage != null)
							dedupeMessage.setVisible(false);
					} catch (Exception exception) {
						isValid = false;
						if (dedupeMessage != null)
							dedupeMessage.setVisible(false);
					}
				}
			}
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating captured Fingerprints ended");
			return isValid;
		} catch (RuntimeException runtimeException) {
			runtimeException.printStackTrace();
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_FINGERPRINT_VALIDATION_EXP,
					String.format("Exception while validating the captured fingerprints of individual: %s caused by %s",
							runtimeException.getMessage(), runtimeException.getCause()));
		}
	}

	/**
	 * Fingerdeduplication check.
	 *
	 * @param segmentedFingerprintDetailsDTOs
	 *            the segmented fingerprint details DTO's
	 * @param isValid
	 *            the isvalid flag
	 * @param fingerprintDetailsDTOs
	 *            the fingerprint details DT os
	 * @return true, if successful
	 */
	private boolean fingerdeduplicationCheck(List<FingerprintDetailsDTO> segmentedFingerprintDetailsDTOs,
			boolean isValid, List<FingerprintDetailsDTO> fingerprintDetailsDTOs) {
		if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			if (validateFingerprint(segmentedFingerprintDetailsDTOs)) {
				isValid = true;
			} else {
				duplicateCheckLbl.setText(RegistrationUIConstants.FINGERPRINT_DUPLICATION_ALERT);
			}
		} else {
			isValid = true;
		}
		return isValid;
	}

	/**
	 * Validating quality score of captured fingerprints.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @return true, if successful
	 */
	protected boolean validateQualityScore(FingerprintDetailsDTO fingerprintDetailsDTO) {
		try {
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating quality score of captured fingerprints started");
			if (fingerprintDetailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
				return validate(fingerprintDetailsDTO, RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
			} else if (fingerprintDetailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				return validate(fingerprintDetailsDTO, RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD);
			} else if (fingerprintDetailsDTO.getFingerType().equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
				return validate(fingerprintDetailsDTO, RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD);
			}
			LOGGER.info(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Validating quality score of captured fingerprints ended");
			return false;
		} catch (RuntimeException runtimeException) {
			LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

			throw new RegBaseUncheckedException(RegistrationConstants.USER_REG_FINGERPRINT_SCORE_VALIDATION_EXP,
					String.format(
							"Exception while validating the quality score of captured Fingerprints: %s caused by %s",
							runtimeException.getMessage(), runtimeException.getCause()));
		}
	}

	/**
	 * Validates QualityScore.
	 *
	 * @param fingerprintDetailsDTO
	 *            the fingerprint details DTO
	 * @param handThreshold
	 *            the hand threshold
	 * @return boolean
	 */
	private Boolean validate(FingerprintDetailsDTO fingerprintDetailsDTO, String handThreshold) {

		double qualityScore;
		if (!bioService.isMdmEnabled()) {
			qualityScore = fingerprintDetailsDTO.getQualityScore();
		} else {
			qualityScore = bioService.getHighQualityScoreByBioType(fingerprintDetailsDTO.getFingerType());
		}
		return qualityScore >= Double.parseDouble(getValueFromApplicationContext(handThreshold))
				|| (qualityScore < Double.parseDouble(getValueFromApplicationContext(handThreshold))
						&& fingerprintDetailsDTO.getNumRetry() == Integer.parseInt(
								getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_RETRIES_COUNT)))
				|| fingerprintDetailsDTO.isForceCaptured();
	}

	/**
	 * Gets the fingerprint by selected pane.
	 *
	 * @return the fingerprint by selected pane
	 */
	private Stream<FingerprintDetailsDTO> getFingerprintBySelectedPane() {
		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			return getSelectedPane(getBiometricDTOFromSession().getOperatorBiometricDTO().getFingerprintDetailsDTO());
		} else if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			return getSelectedPane(getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.getFingerprintDetailsDTO());
		} else {
			return getSelectedPane(getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
					.getFingerprintDetailsDTO());
		}
	}

	/**
	 * Gets the selected pane.
	 *
	 * @param fingerPrintDetails
	 *            the finger print details
	 * @return the selected pane
	 */
	private Stream<FingerprintDetailsDTO> getSelectedPane(List<FingerprintDetailsDTO> fingerPrintDetails) {
		return fingerPrintDetails.stream().filter(fingerprint -> {
			String fingerType;
			if (StringUtils.containsIgnoreCase(selectedPane.getId(), leftHandPalmPane.getId())) {
				fingerType = RegistrationConstants.FINGERPRINT_SLAB_LEFT;
			} else {
				if (StringUtils.containsIgnoreCase(selectedPane.getId(), rightHandPalmPane.getId())) {
					fingerType = RegistrationConstants.FINGERPRINT_SLAB_RIGHT;
				} else {
					fingerType = RegistrationConstants.FINGERPRINT_SLAB_THUMBS;
				}
			}
			return fingerprint.getFingerType() != null ? fingerprint.getFingerType().contains(fingerType) : false;
		});
	}

	/**
	 * Clear attempts box.
	 *
	 * @param styleClass
	 *            the style class
	 * @param retries
	 *            the retries
	 */
	private void clearAttemptsBox(String styleClass, int retries) {
		fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries).getStyleClass().clear();
		fpRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries).getStyleClass().add(styleClass);
	}

	/**
	 * Populate exception.
	 */
	private void populateException() {
		leftSlapException.setText(RegistrationConstants.HYPHEN);
		rightSlapException.setText(RegistrationConstants.HYPHEN);
		thumbSlapException.setText(RegistrationConstants.HYPHEN);

		StringBuilder leftSlapExceptionFingers = new StringBuilder();
		StringBuilder rightSlapExceptionFingers = new StringBuilder();
		StringBuilder thumbSlapExceptionFingers = new StringBuilder();

		if ((boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
			if (getBiometricDTOFromSession() != null && getBiometricDTOFromSession().getOperatorBiometricDTO() != null
					&& getBiometricDTOFromSession().getOperatorBiometricDTO().getBiometricExceptionDTO() != null) {
				getBiometricDTOFromSession().getOperatorBiometricDTO().getBiometricExceptionDTO()
						.sort((a, b) -> a.getMissingBiometric().compareTo(b.getMissingBiometric()));
				getBiometricDTOFromSession().getOperatorBiometricDTO().getBiometricExceptionDTO().stream()
						.forEach(bio -> findExceptionFinger(leftSlapExceptionFingers, rightSlapExceptionFingers,
								thumbSlapExceptionFingers, bio));
			}
		} else if (getRegistrationDTOFromSession().isUpdateUINNonBiometric()) {
			if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getBiometricDTO() != null
					&& getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO() != null
					&& getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
							.getBiometricExceptionDTO() != null) {
				getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().getBiometricExceptionDTO()
						.sort((a, b) -> a.getMissingBiometric().compareTo(b.getMissingBiometric()));
				getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO().getBiometricExceptionDTO()
						.stream().forEach(bio -> findExceptionFinger(leftSlapExceptionFingers,
								rightSlapExceptionFingers, thumbSlapExceptionFingers, bio));
			}
		} else {
			if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getBiometricDTO() != null
					&& getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO() != null
					&& getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO()
							.getBiometricExceptionDTO() != null) {
				getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO().getBiometricExceptionDTO()
						.sort((a, b) -> a.getMissingBiometric().compareTo(b.getMissingBiometric()));
				getRegistrationDTOFromSession().getBiometricDTO().getApplicantBiometricDTO().getBiometricExceptionDTO()
						.stream().forEach(bio -> findExceptionFinger(leftSlapExceptionFingers,
								rightSlapExceptionFingers, thumbSlapExceptionFingers, bio));
			}
		}
		if (leftSlapExceptionFingers.length() > 0) {
			leftSlapException.setText(
					(leftSlapExceptionFingers.deleteCharAt(leftSlapExceptionFingers.length() - 1)).toString() + " "
							+ ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.FINGER));

		}
		if (rightSlapExceptionFingers.length() > 0) {
			rightSlapException.setText(
					(rightSlapExceptionFingers.deleteCharAt(rightSlapExceptionFingers.length() - 1)).toString() + " "
							+ ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.FINGER));
		}
		if (thumbSlapExceptionFingers.length() > 0) {
			thumbSlapException.setText(
					(thumbSlapExceptionFingers.deleteCharAt(thumbSlapExceptionFingers.length() - 1)).toString() + " "
							+ ApplicationContext.applicationLanguageBundle().getString(RegistrationConstants.FINGER));
		}
		singleBiomtericCaptureCheck();
	}

	private void findExceptionFinger(StringBuilder leftSlapExceptionFingers, StringBuilder rightSlapExceptionFingers,
			StringBuilder thumbSlapExceptionFingers, BiometricExceptionDTO bio) {
		if (bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT)
				&& bio.getMissingBiometric().contains(RegistrationConstants.LEFT.toLowerCase())
				&& !bio.getMissingBiometric().contains(RegistrationConstants.THUMB)) {
			String str = (bio.getMissingBiometric()).replace(RegistrationConstants.LEFT.toLowerCase(),
					RegistrationConstants.EMPTY);
			str = ApplicationContext.applicationLanguageBundle().getString(str);
			leftSlapExceptionFingers.append(str.concat(RegistrationConstants.COMMA));
		} else if (bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT)
				&& bio.getMissingBiometric().contains(RegistrationConstants.RIGHT.toLowerCase())
				&& !bio.getMissingBiometric().contains(RegistrationConstants.THUMB)) {
			String str = (bio.getMissingBiometric()).replace(RegistrationConstants.RIGHT.toLowerCase(),
					RegistrationConstants.EMPTY);
			str = ApplicationContext.applicationLanguageBundle().getString(str);
			rightSlapExceptionFingers.append(str.concat(RegistrationConstants.COMMA));
		} else if (bio.getBiometricType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT)
				&& bio.getMissingBiometric().contains(RegistrationConstants.THUMB)) {
			String str = (bio.getMissingBiometric());
			str = (str.contains(RegistrationConstants.LEFT.toLowerCase())
					? str.replace(RegistrationConstants.LEFT.toLowerCase(), RegistrationConstants.LEFT)
					: str.replace(RegistrationConstants.RIGHT.toLowerCase(), RegistrationConstants.RIGHT));
			str = ApplicationContext.applicationLanguageBundle().getString(str);
			thumbSlapExceptionFingers.append(str.concat(RegistrationConstants.COMMA));
		}
	}

	private boolean validateFingerprint(List<FingerprintDetailsDTO> fingerprintDetailsDTOs) {
		AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId(SessionContext.userContext().getUserId());
		authenticationValidatorDTO.setFingerPrintDetails(fingerprintDetailsDTOs);
		authenticationValidatorDTO.setAuthValidationType("multiple");
		boolean isValid = !authenticationService.authValidator("Fingerprint", authenticationValidatorDTO);
		if (null != getValueFromApplicationContext("IDENTY_SDK")) {
			isValid = false;
		}
		return isValid;

	}

}
