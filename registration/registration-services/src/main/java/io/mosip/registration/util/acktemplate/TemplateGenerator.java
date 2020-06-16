package io.mosip.registration.util.acktemplate;

import static io.mosip.registration.constants.LoggerConstants.LOG_TEMPLATE_GENERATOR;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.kernel.packetmanager.constants.Biometric;
import io.mosip.kernel.packetmanager.dto.BiometricsDto;
import io.mosip.kernel.packetmanager.dto.DocumentDto;
import io.mosip.kernel.packetmanager.dto.SimpleDto;
import io.mosip.kernel.packetmanager.dto.metadata.BiometricsException;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.biometric.BiometricExceptionDTO;
import io.mosip.registration.dto.demographic.ValuesDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.impl.IdentitySchemaServiceImpl;

/**
 * Generates Velocity Template for the creation of acknowledgement
 * 
 * @author Himaja Dhanyamraju
 *
 */
@Controller
public class TemplateGenerator extends BaseService {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(TemplateGenerator.class);

	@Autowired
	private QrCodeGenerator<QrVersion> qrCodeGenerator;
	
	@Autowired
	private IdentitySchemaServiceImpl identitySchemaServiceImpl;

	private String consentText;

	private String guidelines;

	public String getGuidelines() {
		return guidelines;
	}

	public void setGuidelines(String guidelines) {
		this.guidelines = guidelines;
	}

	public String getConsentText() {
		return consentText;
	}

	public void setConsentText(String consentText) {
		this.consentText = consentText;
	}

	/**
	 * This method generates the Registration Preview / Acknowledgement page by
	 * mapping all the applicant details including demographic details, documents,
	 * biometrics and photos that are captured as a part of registration to the
	 * place-holders given in the html template.
	 * 
	 * <p>
	 * Returns the {@link ResponseDTO} object.
	 * </p>
	 * 
	 * <p>
	 * If all the data is mapped successfully to the html template,
	 * {@link SuccessResponseDTO} will be set in {@link ResponseDTO} object. The
	 * generated template is stored in the success response which will be used
	 * further to display the Registration Preview / Acknowledgement.
	 * </p>
	 * 
	 * <p>
	 * If any exception occurs, {@link ErrorResponseDTO} will be set in
	 * {@link ResponseDTO} object
	 * </p>
	 * 
	 * @param templateText
	 *            - string which contains the data of template that is used to
	 *            generate acknowledgement
	 * @param registration
	 *            - RegistrationDTO to display required fields on the template
	 * @param templateManagerBuilder
	 *            - The Builder which generates template by mapping values to
	 *            respective place-holders in template
	 * @param templateType
	 *            - The type of template that is required (like
	 *            email/sms/acknowledgement)
	 * @return {@link ResponseDTO} which specifies either success response or error
	 *         response after the generation of Registration Preview /
	 *         Acknowledgement
	 * @throws RegBaseCheckedException 
	 */
	public ResponseDTO generateTemplate(String templateText, RegistrationDTO registration,
			TemplateManagerBuilder templateManagerBuilder, String templateType) throws RegBaseCheckedException {

		ResponseDTO response = new ResponseDTO();

		try {
			LOGGER.info(LOG_TEMPLATE_GENERATOR, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					"generateTemplate had been called for preparing Acknowledgement Template.");

			ResourceBundle localProperties = ApplicationContext.localLanguageProperty();
			ResourceBundle applicationLanguageProperties = ApplicationContext.applicationLanguageBundle();

			InputStream is = new ByteArrayInputStream(templateText.getBytes());
			Map<String, Object> templateValues = new WeakHashMap<>();

			String documentDisableFlag = String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.DOC_DISABLE_FLAG));
			String fingerPrintDisableFlag = String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.FINGERPRINT_DISABLE_FLAG));
			String irisDisableFlag = String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.IRIS_DISABLE_FLAG));
			String faceDisableFlag = String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.FACE_DISABLE_FLAG));
			
			boolean isChild = registration.isChild();

			int leftSlapCount = 0;
			int rightSlapCount = 0;
			int thumbCount = 0;
			int irisCount = 0;
			
			List<UiSchemaDTO> schemaFields = getSchemaFields(registration.getIdSchemaVersion());

			boolean parentPhotoCaptured = false;

			Map<String, Integer> exceptionCount = exceptionFingersCount(registration, leftSlapCount, rightSlapCount,
					thumbCount, irisCount);
			int excepCount = exceptionCount.isEmpty() ? 0 : exceptionCount.get(RegistrationConstants.EXCEPTIONCOUNT);

			if ((RegistrationConstants.DISABLE.equalsIgnoreCase(fingerPrintDisableFlag) && excepCount == 2)
					|| (RegistrationConstants.DISABLE.equalsIgnoreCase(irisDisableFlag) && excepCount == 10)
					|| excepCount == 12) {
				parentPhotoCaptured = true;
			}

			if (templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE)) {
				/* Set-up Registration Acknowledgement related content */
				setUpAcknowledgementContent(registration, templateValues, response, applicationLanguageProperties,
						fingerPrintDisableFlag, irisDisableFlag);
				/* Set-up important guidelines that are configured by the country */
				setUpImportantGuidelines(templateValues, guidelines);
			} else {
				/* Set-up Registration Preview related content */
				setUpPreviewContent(registration, templateValues, isChild, response, applicationLanguageProperties,
						fingerPrintDisableFlag);
			}

			if (registration.getSelectionListDTO() != null) {
				if (!registration.getBiometrics().isEmpty()) {
					templateValues = countMissingIrises(templateValues, registration, isChild, templateType,response);
				} else {
					if (!RegistrationConstants.ENABLE.equalsIgnoreCase(faceDisableFlag) || registration
							.getBiometricDTO().getApplicantBiometricDTO().getExceptionFace().getFace() == null) {
						templateValues.put(RegistrationConstants.TEMPLATE_IRIS_DISABLED,
								RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					}
					templateValues.put(RegistrationConstants.TEMPLATE_IRIS_ENABLED,
							RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
				}
			} else {
				templateValues = countMissingIrises(templateValues, registration, isChild, templateType,response);
			}

			/* Set-up demographic information related content */
			setUpDemographicInfo(registration, templateValues, isChild, applicationLanguageProperties, schemaFields);

			/* Set-up the list of documents submitted by the applicant */
			setUpDocuments(templateValues, applicationLanguageProperties, registration.getDocuments(), documentDisableFlag);
			
			//setupBiometricDataList(registration, templateValues, schemaFields, applicationLanguageProperties, response);

			/* Set-up captured biometrics count */
			setUpBiometricsCount(templateValues, registration, applicationLanguageProperties, fingerPrintDisableFlag,
					irisDisableFlag, faceDisableFlag, isChild);

			/* Set-up captured images of applicant */
			setUpCapturedImages(templateValues, registration, isChild, applicationLanguageProperties, localProperties,
					faceDisableFlag, parentPhotoCaptured);

			/* Set-up Biometrics related content */
			setUpBiometricContent(templateValues, registration, isChild, applicationLanguageProperties, localProperties,
					fingerPrintDisableFlag, irisDisableFlag, faceDisableFlag, parentPhotoCaptured);

			/* Set-up Registration Office and Officer related content */
			setUpROContent(templateValues, registration, applicationLanguageProperties);

			Writer writer = new StringWriter();
			try {
				LOGGER.debug(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
						"merge method of TemplateManager had been called for preparing Acknowledgement Template.");
				
				TemplateManager templateManager = templateManagerBuilder.build();
				InputStream inputStream = templateManager.merge(is, templateValues);
				String defaultEncoding = null;
				IOUtils.copy(inputStream, writer, defaultEncoding);
			} catch (IOException ioException) {
				setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
				LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
						ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			}
			LOGGER.debug(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					"generateTemplate method has been ended for preparing Acknowledgement Template.");

			Map<String, Object> responseMap = new WeakHashMap<>();
			responseMap.put(RegistrationConstants.TEMPLATE_NAME, writer);
			setSuccessResponse(response, RegistrationConstants.SUCCESS, responseMap);
		} catch (RuntimeException runtimeException) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return response;
	}

	private void setUpImportantGuidelines(Map<String, Object> templateValues, String guidelines) {
		String[] importantGuidelines = guidelines.split(RegistrationConstants.SPLIT_DELIMITOR);
		StringBuilder formattedGuidelines = new StringBuilder();
		for (String importantGuideline : importantGuidelines) {
			formattedGuidelines.append(RegistrationConstants.LIST_ITEM_OPENING_TAG).append(importantGuideline)
					.append(RegistrationConstants.LIST_ITEM_CLOSING_TAG);
		}
		templateValues.put(RegistrationConstants.TEMPLATE_GUIDELINES, formattedGuidelines.toString());
	}

	private void setUpROContent(Map<String, Object> templateValues, RegistrationDTO registration,
			ResourceBundle applicationLanguageProperties) {
		templateValues.put(RegistrationConstants.TEMPLATE_RO_IMAGE, RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME_USER_LANG_LABEL,
				applicationLanguageProperties.getString("ro_name"));
		templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("ro_name"));
		templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME,
				getValue(registration.getOsiDataDTO().getOperatorID()));
		templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME_LOCAL_LANG, RegistrationConstants.EMPTY);
		templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER_USER_LANG_LABEL,
				applicationLanguageProperties.getString("registrationcenter"));
		templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("registrationcenter"));
		templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER,
				SessionContext.userContext().getRegistrationCenterDetailDTO().getRegistrationCenterName());
		templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER_LOCAL_LANG, RegistrationConstants.EMPTY);
		templateValues.put(RegistrationConstants.TEMPLATE_IMPORTANT_GUIDELINES,
				applicationLanguageProperties.getString("importantguidelines"));
	}

	private void setUpCapturedImages(Map<String, Object> templateValues, RegistrationDTO registration, boolean isChild,
			ResourceBundle applicationLanguageProperties, ResourceBundle localProperties, String faceDisableFlag,
			boolean parentPhotoCaptured) {
		if (!parentPhotoCaptured) {
			templateValues.put(RegistrationConstants.PARENT_PHOTO_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}

		boolean isParentOrGuardianBiometricsCaptured = registration.isUpdateUINChild()
				|| (SessionContext.map().get(RegistrationConstants.IS_Child) != null
						&& (boolean) SessionContext.map().get(RegistrationConstants.IS_Child));

		if (isChild || registration.isUpdateUINNonBiometric()) {
			if (!registration.getBiometricExceptions().isEmpty()) {
				Optional<Entry<String, DocumentDto>> exceptionImage = registration.getDocuments().entrySet().stream()
						.filter(obj -> obj.getValue().getCategory().equalsIgnoreCase("POE")).findAny();
				if (exceptionImage.isPresent()) {
					byte[] exceptionImageBytes = exceptionImage.get().getValue().getDocument();
					setUpExceptionPhoto(exceptionImageBytes, templateValues, applicationLanguageProperties,
							localProperties, isParentOrGuardianBiometricsCaptured);
				}
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_WITHOUT_EXCEPTION, null);
				templateValues.put(RegistrationConstants.TEMPLATE_WITH_EXCEPTION,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
		} else if (!registration.getBiometricExceptions().isEmpty()) {
			byte[] exceptionImageBytes = null;
			Optional<Entry<String, DocumentDto>> exceptionImage = registration.getDocuments().entrySet().stream()
					.filter(obj -> obj.getValue().getCategory().equalsIgnoreCase("POE")).findAny();
			if (exceptionImage.isPresent()) {
				exceptionImageBytes = exceptionImage.get().getValue().getDocument();
				setUpExceptionPhoto(exceptionImageBytes, templateValues, applicationLanguageProperties,
						localProperties, isParentOrGuardianBiometricsCaptured);
			}
			setUpExceptionPhoto(exceptionImageBytes, templateValues, applicationLanguageProperties, localProperties,
					isParentOrGuardianBiometricsCaptured);
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_WITHOUT_EXCEPTION, null);
			templateValues.put(RegistrationConstants.TEMPLATE_WITH_EXCEPTION,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}

		if (RegistrationConstants.ENABLE.equalsIgnoreCase(faceDisableFlag)) {
			templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_USER_LANG,
					applicationLanguageProperties.getString("individualphoto"));
			templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_LOCAL_LANG,
					getSecondaryLanguageLabel("individualphoto"));
			byte[] applicantImageBytes;
			if (registration.isUpdateUINNonBiometric() && !registration.isUpdateUINChild()) {
				applicantImageBytes = registration.getBiometricDTO().getIntroducerBiometricDTO().getFace().getFace();
			} else {
				applicantImageBytes = registration.getBiometricDTO().getApplicantBiometricDTO().getFace().getFace();
			}

			String applicantImageEncodedBytes = StringUtils
					.newStringUtf8(Base64.encodeBase64(applicantImageBytes, false));
			templateValues.put(RegistrationConstants.TEMPLATE_APPLICANT_IMAGE_SOURCE,
					RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + applicantImageEncodedBytes);
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_FACE_CAPTURE_ENABLED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}
	}

	private void setUpExceptionPhoto(byte[] exceptionImageBytes, Map<String, Object> templateValues,
			ResourceBundle applicationLanguageProperties, ResourceBundle localProperties,
			boolean isParentOrGuardianExceptionPhotoCapture) {
		templateValues.put(RegistrationConstants.TEMPLATE_WITHOUT_EXCEPTION,
				RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		templateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_PHOTO_USER_LANG_LABEL,
				getExceptionPhotoLabel(isParentOrGuardianExceptionPhotoCapture,
						applicationLanguageProperties.getString("exceptionphoto"), applicationLanguageProperties));
		templateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_PHOTO_LOCAL_LANG_LABEL, getExceptionPhotoLabel(
				isParentOrGuardianExceptionPhotoCapture, getSecondaryLanguageLabel("exceptionphoto"), localProperties));
		
		
		
		String exceptionImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(exceptionImageBytes, false));
		templateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_IMAGE_SOURCE,
				RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + exceptionImageEncodedBytes);
	}

	private String getExceptionPhotoLabel(boolean isParentOrGuardianExceptionPhotoCapture, String exceptionPhotoLabel,
			ResourceBundle resourceBundle) {
		String exceptionFaceDescription = exceptionPhotoLabel;

		if (isParentOrGuardianExceptionPhotoCapture) {
			exceptionFaceDescription = resourceBundle.getString("parentOrGuardian").concat(" ")
					.concat(exceptionFaceDescription.toLowerCase());
		}

		return exceptionFaceDescription;
	}

	private void setUpBiometricContent(Map<String, Object> templateValues, RegistrationDTO registration,
			boolean isChild, ResourceBundle applicationLanguageProperties, ResourceBundle localProperties,
			String fingerPrintDisableFlag, String irisDisableFlag, String faceDisableFlag,
			boolean parentPhotoCaptured) {
		boolean exceptionWithParentPhoto = false;
		// iris is configured
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(irisDisableFlag)
				&& ((registration.getSelectionListDTO() == null && !isChild)
						|| (registration.getSelectionListDTO() == null && isChild)
						|| (registration.getSelectionListDTO() != null &&
							 registration.getSelectionListDTO().get("biometrics")==null))) {
			if (isChild || registration.isUpdateUINNonBiometric()) {
				if (registration.getBiometricDTO().getIntroducerBiometricDTO().getFace() == null
						|| (registration.getBiometricDTO().getIntroducerBiometricDTO().getFace() != null && registration
								.getBiometricDTO().getIntroducerBiometricDTO().getFace().getFace() == null)
								&& registration.getBiometricDTO().getIntroducerBiometricDTO().getIrisDetailsDTO()
										.isEmpty()) {
					if (!RegistrationConstants.ENABLE.equalsIgnoreCase(faceDisableFlag) || registration
							.getBiometricDTO().getIntroducerBiometricDTO().getExceptionFace().getFace() == null) {
						templateValues.put(RegistrationConstants.TEMPLATE_IRIS_DISABLED,
								RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					} else {
						exceptionWithParentPhoto = true;
					}
					templateValues.put(RegistrationConstants.TEMPLATE_IRIS_ENABLED,
							RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
				} else {
					if (registration.getBiometricDTO().getIntroducerBiometricDTO().getExceptionFace()
							.getFace() != null) {
						if (registration.getBiometricDTO().getIntroducerBiometricDTO().getFace() != null && registration
								.getBiometricDTO().getIntroducerBiometricDTO().getFace().getFace() != null) {
							templateValues.put(RegistrationConstants.IRIS_WITH_EXCEPTION,
									RegistrationConstants.TEMPLATE_IRIS);
						} else {
							templateValues.put(RegistrationConstants.IRIS_WITH_EXCEPTION,
									RegistrationConstants.IRIS_WITH_EXCEPTION_STYLE);
						}
					} else {
						templateValues.put(RegistrationConstants.IRIS_STYLE,
								RegistrationConstants.IRIS_WITHOUT_EXCEPTION_STYLE);
					}
				}
			} else {
				templateValues.put(RegistrationConstants.IRIS_STYLE, RegistrationConstants.IRIS_WITHOUT_EXCEPTION);
				templateValues.put(RegistrationConstants.IRIS_WITH_EXCEPTION, RegistrationConstants.TEMPLATE_IRIS);
			}
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_USER_LANG_LABEL,
					applicationLanguageProperties.getString("lefteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("lefteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_USER_LANG_LABEL,
					applicationLanguageProperties.getString("righteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("righteye"));
			if (!exceptionWithParentPhoto) {
				templateValues.put(RegistrationConstants.TEMPLATE_IRIS_DISABLED,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
		} else {
			if (!RegistrationConstants.ENABLE.equalsIgnoreCase(faceDisableFlag)
					|| (((isChild || registration.isUpdateUINNonBiometric()) && registration.getBiometricDTO()
							.getIntroducerBiometricDTO().getExceptionFace().getFace() == null)
							|| ((!isChild && !registration.isUpdateUINNonBiometric()) && registration.getBiometricDTO()
									.getApplicantBiometricDTO().getExceptionFace().getFace() == null))) {
				templateValues.put(RegistrationConstants.TEMPLATE_IRIS_DISABLED,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
			templateValues.put(RegistrationConstants.TEMPLATE_IRIS_ENABLED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}

		if (parentPhotoCaptured) {
			templateValues.put(RegistrationConstants.PARENT_PHOTO_NOT_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			templateValues.put(RegistrationConstants.PARENT_PHOTO_PRIMARY_LANG,
					applicationLanguageProperties.getString("parentPhoto"));
			templateValues.put(RegistrationConstants.PARENT_PHOTO_LOCAL_LANG, getSecondaryLanguageLabel("parentPhoto"));
			byte[] parentImageBytes = registration.getBiometricDTO().getIntroducerBiometricDTO().getFace().getFace();
			String parentImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(parentImageBytes, false));
			templateValues.put(RegistrationConstants.PARENT_IMAGE_SOURCE,
					RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + parentImageEncodedBytes);
		}

		if ((registration.getSelectionListDTO() != null && registration.getBiometrics()!=null)
						|| (registration.getSelectionListDTO() == null && !isChild)
						|| (registration.getSelectionListDTO() == null && isChild)) {
			templateValues.put(RegistrationConstants.TEMPLATE_FINGERPRINTS_CAPTURED, null);
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_USER_LANG_LABEL,
					applicationLanguageProperties.getString("lefthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("lefthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_USER_LANG_LABEL,
					applicationLanguageProperties.getString("righthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("righthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_USER_LANG_LABEL,
					applicationLanguageProperties.getString("thumbs"));
			templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("thumbs"));
			if (isChild || registration.isUpdateUINNonBiometric()) {
				if (parentPhotoCaptured) {
					//TODO need to set parent fingers
					setUpParentFingerprints(registration, templateValues);
				} else {
					templateValues.put(RegistrationConstants.PARENT_PHOTO_CAPTURED,
							RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					if (!registration.getBiometrics()
							.isEmpty()) {
						for (Entry<String, BiometricsDto> fingerprint : registration.getBiometrics().entrySet()) {
							if (fingerprint.getValue().getModalityName().contains(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
								templateValues.put(RegistrationConstants.TEMPLATE_CHILD_LEFT,
										RegistrationConstants.PARENT_STYLE);
								templateValues.put(RegistrationConstants.PARENT_RIGHT_SLAP,
										RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
								templateValues.put(RegistrationConstants.PARENT_THUMBS,
										RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
								templateValues.put(RegistrationConstants.TEMPLATE_LEFT_INDEX_FINGER,
										RegistrationConstants.TEMPLATE_RIGHT_MARK);
							} else if (fingerprint.getValue().getModalityName().contains(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
								templateValues.put(RegistrationConstants.TEMPLATE_CHILD_RIGHT,
										RegistrationConstants.PARENT_STYLE);
								templateValues.put(RegistrationConstants.PARENT_LEFT_SLAP,
										RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
								templateValues.put(RegistrationConstants.PARENT_THUMBS,
										RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
								templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_LITTLE_FINGER,
										RegistrationConstants.TEMPLATE_RIGHT_MARK);
							} else if (fingerprint.getValue().getModalityName().contains(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
								templateValues.put(RegistrationConstants.TEMPLATE_CHILD_THUMBS,
										RegistrationConstants.PARENT_STYLE);
								templateValues.put(RegistrationConstants.PARENT_LEFT_SLAP,
										RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
								templateValues.put(RegistrationConstants.PARENT_RIGHT_SLAP,
										RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
								templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_THUMB_FINGER,
										RegistrationConstants.TEMPLATE_RIGHT_MARK);
							}
						}
						templateValues.put(RegistrationConstants.TEMPLATE_IS_CHILD,
								RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					} else {
						templateValues.put(RegistrationConstants.TEMPLATE_FINGERPRINTS_CAPTURED,
								RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					}
				}
				templateValues.put(RegistrationConstants.TEMPLATE_MISSING_LEFT_FINGERS,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
				templateValues.put(RegistrationConstants.TEMPLATE_MISSING_RIGHT_FINGERS,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
				templateValues.put(RegistrationConstants.TEMPLATE_MISSING_THUMBS,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_CHILD_LEFT,
						RegistrationConstants.TEMPLATE_LEFT_INDEX_FINGER);
				templateValues.put(RegistrationConstants.TEMPLATE_CHILD_RIGHT,
						RegistrationConstants.TEMPLATE_RIGHT_LITTLE_FINGER);
				templateValues.put(RegistrationConstants.TEMPLATE_CHILD_THUMBS,
						RegistrationConstants.TEMPLATE_RIGHT_THUMB_FINGER);
				// get the quality ranking for fingerprints of the applicant
				Map<String, Integer> fingersQuality = getFingerPrintQualityRanking(registration);
				for (Map.Entry<String, Integer> entry : fingersQuality.entrySet()) {
					if (entry.getValue() != 0) {
						if (registration.getRegistrationMetaDataDTO().getRegistrationCategory() != null
								&& registration.getRegistrationMetaDataDTO().getRegistrationCategory()
										.equals(RegistrationConstants.PACKET_TYPE_LOST)) {
							// display tick mark for the captured fingerprints
							templateValues.put(entry.getKey(), RegistrationConstants.TEMPLATE_RIGHT_MARK);
						} else {
							// display rank of quality for the captured fingerprints
							templateValues.put(entry.getKey(), entry.getValue());
						}
					} else {
						// display cross mark for missing fingerprints
						templateValues.put(entry.getKey(), RegistrationConstants.TEMPLATE_CROSS_MARK);
					}
				}
				countMissingFingers(registration, templateValues, applicationLanguageProperties, localProperties);
			}
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_FINGERPRINTS_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}
	}

	private void setUpParentFingerprints(RegistrationDTO registration, Map<String, Object> templateValues) {
		templateValues.put(RegistrationConstants.TEMPLATE_CHILD_LEFT, RegistrationConstants.TEMPLATE_LEFT_INDEX_FINGER);
		templateValues.put(RegistrationConstants.TEMPLATE_CHILD_RIGHT,
				RegistrationConstants.TEMPLATE_RIGHT_LITTLE_FINGER);
		templateValues.put(RegistrationConstants.TEMPLATE_CHILD_THUMBS,
				RegistrationConstants.TEMPLATE_RIGHT_THUMB_FINGER);
		// get the quality ranking for fingerprints of the Introducer
		Map<String, Integer> fingersQuality = new WeakHashMap<>();

		// list of missing fingers
		List<BiometricExceptionDTO> exceptionFingers = registration.getBiometricDTO().getIntroducerBiometricDTO()
				.getBiometricExceptionDTO();

		if (exceptionFingers != null) {
			for (BiometricExceptionDTO exceptionFinger : exceptionFingers) {
				if (exceptionFinger.getBiometricType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT)
						&& exceptionFinger.getReason().equals(RegistrationConstants.MISSING_BIOMETRICS)) {
					fingersQuality.put(exceptionFinger.getMissingBiometric(), 0);
				}
			}
		}

		for (Map.Entry<String, Integer> entry : fingersQuality.entrySet()) {
			// display cross mark for missing fingerprints
			templateValues.put(entry.getKey(), RegistrationConstants.TEMPLATE_CROSS_MARK);
		}
	}

	private void setUpBiometricsCount(Map<String, Object> templateValues, RegistrationDTO registration,
			ResourceBundle applicationLanguageProperties, String fingerPrintDisableFlag, String irisDisableFlag,
			String faceDisableFlag, boolean isChild) {

		List<UiSchemaDTO> uiSchema = null;
		List<String> result = new ArrayList<>();
		try {
			uiSchema = identitySchemaServiceImpl.getUISchema(registration.getIdSchemaVersion());
			uiSchema.stream().forEach(obj -> {
				obj.getLabel().entrySet().forEach(lable -> {
					result.add(lable.getValue());
				});
			});
		} catch (RegBaseCheckedException e) {
			e.printStackTrace();
		}
		Optional<HashMap<String, String>> lableMap = null;
		if (!uiSchema.isEmpty()) {
			Optional<String> optinalLable = registration.getBiometrics().keySet().stream().findFirst();
			if (optinalLable.isPresent() && optinalLable.get().contains("applicant")) {

				lableMap = uiSchema.stream().filter(obj -> obj.getId().equalsIgnoreCase("individualBiometrics"))
						.map(UiSchemaDTO::getLabel).findAny();
			} else {
				lableMap = uiSchema.stream().filter(obj -> obj.getId().equalsIgnoreCase("parentOrGuardianBiometrics"))
						.map(UiSchemaDTO::getLabel).findAny();
			}

		}
//TODO
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_USER_LANG_LABEL,
				lableMap.isPresent() ? lableMap.get().get("primary")
						: applicationLanguageProperties.getString("biometricsHeading"));
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_LOCAL_LANG_LABEL,
				lableMap.isPresent() ? lableMap.get().get("secondary")
						: getSecondaryLanguageLabel("biometricsHeading"));
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_CAPTURED_USER_LANG_LABEL,
				applicationLanguageProperties.getString("biometrics_captured"));
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_CAPTURED_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("biometrics_captured"));

		List<BiometricsDto> capturedFingers;
		List<BiometricsDto> capturedIris;

		if ((registration.getSelectionListDTO() == null && !isChild)
				|| (registration.getSelectionListDTO() != null && !registration.isUpdateUINNonBiometric())) {
			// get the total count of fingerprints captured and irises captured
			capturedFingers = registration.getBiometrics().values().stream()
					.filter(obj -> obj.getModalityName().contains("FINGERPRINT_SLAB")).collect(Collectors.toList());
			capturedIris = registration.getBiometrics().values().stream()
					.filter(obj -> obj.getModalityName().equalsIgnoreCase("IRIS_DOUBLE")).collect(Collectors.toList());
		} else {
			capturedFingers = registration.getBiometrics().values().stream()
					.filter(obj -> obj.getModalityName().contains("FINGERPRINT_SLAB"))
					.collect(Collectors.toList());
			capturedIris = registration.getBiometrics().values().stream()
					.filter(obj -> obj.getModalityName().contains("IRIS_DOUBLE")).collect(Collectors.toList());
		}

		StringBuilder biometricsCaptured = new StringBuilder();
		StringBuilder biometricsCapturedLocalLang = new StringBuilder();

		if (RegistrationConstants.ENABLE.equalsIgnoreCase(fingerPrintDisableFlag)) {

			if (registration.getSelectionListDTO() != null) {
				if (registration.getSelectionListDTO().get("biometrics") != null || registration.getBiometricDTO()
						.getApplicantBiometricDTO().getFingerprintDetailsDTO() != null) {
					addToCapturedBiometrics(biometricsCaptured, biometricsCapturedLocalLang,
							applicationLanguageProperties, "fingersCount", capturedFingers.size());
				}
			} else {
				addToCapturedBiometrics(biometricsCaptured, biometricsCapturedLocalLang, applicationLanguageProperties,
						"fingersCount", capturedFingers.size());
			}
		}
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(irisDisableFlag)) {
			if (registration.getSelectionListDTO() != null) {
				if (registration.getSelectionListDTO().get("biometrics") != null
						|| registration.getBiometricDTO().getApplicantBiometricDTO().getIrisDetailsDTO() != null) {
					addToCapturedBiometrics(biometricsCaptured, biometricsCapturedLocalLang,
							applicationLanguageProperties, "irisCount", capturedIris.size());
				}
			} else {
				addToCapturedBiometrics(biometricsCaptured, biometricsCapturedLocalLang, applicationLanguageProperties,
						"irisCount", capturedIris.size());
			}
		}
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(faceDisableFlag)) {
			if (biometricsCaptured.length() > 1) {
				biometricsCaptured.append(applicationLanguageProperties.getString("comma"));
				biometricsCapturedLocalLang.append(getSecondaryLanguageLabel("comma"));
			}
			biometricsCaptured.append(applicationLanguageProperties.getString("faceCount"));
			biometricsCapturedLocalLang.append(getSecondaryLanguageLabel("faceCount"));
		}

		if (RegistrationConstants.ENABLE.equalsIgnoreCase(fingerPrintDisableFlag)
				|| RegistrationConstants.ENABLE.equalsIgnoreCase(irisDisableFlag)
				|| RegistrationConstants.ENABLE.equalsIgnoreCase(faceDisableFlag)) {

			templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_CAPTURED, biometricsCaptured);
			templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_CAPTURED_LOCAL_LANG,
					biometricsCapturedLocalLang);
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_ENABLED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}
	}

	private String getSecondaryLanguageLabel(String key) {
		ResourceBundle localProperties = ApplicationContext.localLanguageProperty();
		if (ApplicationContext.localLanguage().equalsIgnoreCase(ApplicationContext.applicationLanguage())) {
			return RegistrationConstants.EMPTY;
		} else {
			return localProperties.containsKey(key) ? localProperties.getString(key) : RegistrationConstants.EMPTY;
		}
	}

	private void setUpDocuments(Map<String, Object> templateValues, ResourceBundle applicationLanguageProperties,
			Map<String, DocumentDto> documents, String documentDisableFlag) {
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(documentDisableFlag)) {
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS_USER_LANG_LABEL,
					applicationLanguageProperties.getString("documents"));
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("documents"));
			StringBuilder documentsList = new StringBuilder();

			for (String docName : documents.keySet()) {
				documentsList.append(docName);
				if (documentsList.length() > 0) {
					documentsList.append(", ");
				}
			}
			
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS, documentsList.toString());
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS_LOCAL_LANG, RegistrationConstants.EMPTY);
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS_ENABLED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}
	}

	@SuppressWarnings("unchecked")
	private void setUpDemographicInfo(RegistrationDTO registration, Map<String, Object> templateValues,
			boolean isChild, ResourceBundle applicationLanguageProperties, List<UiSchemaDTO> schemaFields) {
		
		String platformLanguageCode = ApplicationContext.applicationLanguage();
		String localLanguageCode = ApplicationContext.localLanguage();
		
		SimpleDateFormat sdf = new SimpleDateFormat(RegistrationConstants.TEMPLATE_DATE_FORMAT);
		String currentDate = sdf.format(new Date());
		
		// map the respective fields with the values in the registrationDTO
		templateValues.put(RegistrationConstants.TEMPLATE_DATE, currentDate);
		templateValues.put(RegistrationConstants.TEMPLATE_DATE_USER_LANG_LABEL,
				applicationLanguageProperties.getString("date"));
		templateValues.put(RegistrationConstants.TEMPLATE_DATE_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("date"));
		
		templateValues.put(RegistrationConstants.TEMPLATE_DEMO_INFO,
				applicationLanguageProperties.getString("demographicInformation"));
		
		templateValues.put("DemographicInfoSecondary",
				getSecondaryLanguageLabel("demographicInformation"));		
		
		List<Map<String, Object>> demographicsdata = new ArrayList<Map<String, Object>>();
		
		for(UiSchemaDTO field : schemaFields) {
			if("biometricsType".equals(field.getType()) || "documentType".equals(field.getType()) || 
					"UIN".equalsIgnoreCase(field.getId()) || "IDSchemaVersion".equalsIgnoreCase(field.getId()) )
				continue;
			
			Map<String, Object> data = new HashMap<String, Object>();
			String value = getValue(registration.getDemographics().get(field.getId()));
			if(value != null || !value.isEmpty() || !"".equals(value)) {
				data.put("primaryLabel", field.getLabel().get("primary"));
				data.put("secondaryLabel", field.getLabel().containsKey("secondary") ? 
						field.getLabel().get("secondary") : RegistrationConstants.EMPTY);
				data.put("primaryValue", getValueForTemplate(value, platformLanguageCode));
				data.put("secondaryValue", getSecondaryLanguageValue(registration.getDemographics().get(field.getId()), localLanguageCode));
				demographicsdata.add(data);
			}			
		}
			
		templateValues.put("demographicsdata", demographicsdata);
		
		if (!isChild) {
			templateValues.put(RegistrationConstants.TEMPLATE_WITH_PARENT,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}
	}
	
	private void setupBiometricDataList(RegistrationDTO registration, Map<String, Object> templateValues, List<UiSchemaDTO> schemaFields,
			ResourceBundle applicationLanguageProperties, ResponseDTO response) {
		List<UiSchemaDTO> biometricFields = schemaFields.stream().filter(f-> "biometricsType".equals(f.getType()) && f.getSubType() != null)
				.collect(Collectors.toList());	
		
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_USER_LANG_LABEL,
				applicationLanguageProperties.getString("biometricsHeading"));		
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("biometricsHeading"));
		
		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY, applicationLanguageProperties.getString("modify"));
		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_SOURCE, getEncodedImage(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_PATH, 
				response, RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING));
		
		//field -> modality -> attributes
		List<Map<String, Object>> biometricsData = new ArrayList<>();
		
		for(UiSchemaDTO field : biometricFields) {
			Map<String, Object> bioField = new HashMap<>();
			bioField.put("primaryLabel", field.getLabel().get("primary"));
			bioField.put("secondaryLabel", field.getLabel().get("secondary"));
				
			List<Map<String, Object>> modalities = new ArrayList<>();
			for(String bioAttribute : field.getBioAttributes()) {
				BiometricsDto dto = registration.getBiometric(field.getSubType(), bioAttribute);
				boolean isException = registration.isBiometricExceptionAvailable(field.getSubType(), bioAttribute);
				getBiometricData(Biometric.getModalityNameByAttribute(bioAttribute), bioAttribute, dto, isException, 
						modalities, applicationLanguageProperties, response);
			}
			bioField.put("modalities", modalities);
			biometricsData.add(bioField);
		}
		
		templateValues.put("biometricsData", biometricsData);
	}
	
	
	private Map<String, Object> getModalityMap(String modalityName, List<Map<String, Object>> modalities) {
		Optional<Map<String, Object>> modalityMap = modalities.stream().filter(p -> p.get(modalityName) != null).findFirst();
		if(modalityMap.isPresent())
			return modalityMap.get();
		
		Map<String, Object> map = new HashMap<>();
		modalities.add(map);
		return map;
	}
	
	private Map<String, String> getAttributesMap(String bioAttribute, Map<String, Object> modalityMap) {
		if(!modalityMap.containsKey("attributes")) {
			List<Map<String, String>> attributeList = new ArrayList<>();			
			modalityMap.put("attributes", attributeList);			
		}
		
		List<Map<String, String>> attributeList =  (List<Map<String, String>>) modalityMap.get("attributes");
		Optional<Map<String, String>> attributeMap = attributeList.stream().filter(p -> p.get(bioAttribute) != null ).findFirst();
		if(attributeMap.isPresent())
			return attributeMap.get();
		else {
			Map<String, String> map = new HashMap<>();
			attributeList.add(map);
			return map;
		}			
	}
	
	
	private void getBiometricData(String modalityName, String bioAttribute, BiometricsDto dto, boolean isException, 
			List<Map<String, Object>> modalities, ResourceBundle applicationLanguageProperties, 
			ResponseDTO response) {		
		switch (modalityName) {
		case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
			Map<String, Object> leftHandModalityMap = getModalityMap(modalityName, modalities);
			if(!leftHandModalityMap.containsKey(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
				leftHandModalityMap.put(RegistrationConstants.FINGERPRINT_SLAB_LEFT, "captured");
				leftHandModalityMap.put("primaryLabel", applicationLanguageProperties.getString("lefthandpalm"));
				leftHandModalityMap.put("secondaryLabel", getSecondaryLanguageLabel("lefthandpalm"));
				leftHandModalityMap.put("imageSource", getEncodedImage(RegistrationConstants.LEFTPALM_IMG_PATH, response, 
						RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING));
			}
			
			Map<String, String> attributeMap = getAttributesMap(bioAttribute, leftHandModalityMap);
			switch (bioAttribute) {
			case "leftIndex":
				attributeMap.put("class", "leftIndex");
				attributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;	
			case "leftMiddle":	
				attributeMap.put("class", "leftMiddle");
				attributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			case "leftRing":	
				attributeMap.put("class", "leftRing");
				attributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			case "leftLittle":	
				attributeMap.put("class", "leftLittle");
				attributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			}
			
			break;
		case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
			Map<String, Object> rightHandModalityMap = getModalityMap(modalityName, modalities);
			if(!rightHandModalityMap.containsKey(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
				rightHandModalityMap.put(RegistrationConstants.FINGERPRINT_SLAB_RIGHT, "captured");
				rightHandModalityMap.put("primaryLabel", applicationLanguageProperties.getString("righthandpalm"));
				rightHandModalityMap.put("secondaryLabel", getSecondaryLanguageLabel("righthandpalm"));
				rightHandModalityMap.put("imageSource", getEncodedImage(RegistrationConstants.RIGHTPALM_IMG_PATH, response, 
								RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING));
			}
			
			Map<String, String> rightPalmAttributeMap = getAttributesMap(bioAttribute, rightHandModalityMap);
			switch (bioAttribute) {
			case "rightIndex":
				rightPalmAttributeMap.put("class", "rightIndex");
				rightPalmAttributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;	
			case "rightMiddle":	
				rightPalmAttributeMap.put("class", "rightMiddle");
				rightPalmAttributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			case "rightRing":	
				rightPalmAttributeMap.put("class", "rightRing");
				rightPalmAttributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			case "rightLittle":	
				rightPalmAttributeMap.put("class", "rightLittle");
				rightPalmAttributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			}
			
			break;
		case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:	
			Map<String, Object> thumbsModalityMap = getModalityMap(modalityName, modalities);
			if(!thumbsModalityMap.containsKey(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
				thumbsModalityMap.put(RegistrationConstants.FINGERPRINT_SLAB_THUMBS, "captured");
				thumbsModalityMap.put("primaryLabel", applicationLanguageProperties.getString("thumbs"));
				thumbsModalityMap.put("secondaryLabel", getSecondaryLanguageLabel("thumbs"));
				thumbsModalityMap.put("imageSource", getEncodedImage(RegistrationConstants.THUMB_IMG_PATH, response, 
								RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING));
			}
			
			Map<String, String> thumbAttributeMap = getAttributesMap(bioAttribute, thumbsModalityMap);
			switch (bioAttribute) {
			case "rightThumb":
				thumbAttributeMap.put("class", "rightThumb");
				thumbAttributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;				
			case "leftThumb":	
				thumbAttributeMap.put("class", "leftThumb");
				thumbAttributeMap.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
					RegistrationConstants.TEMPLATE_RIGHT_MARK);
				break;
			}
			break;
		
		case RegistrationConstants.IRIS_DOUBLE:	
			Map<String, Object> eyeMap = getModalityMap(bioAttribute, modalities);
			List<Map<String, String>> tempList = new ArrayList<Map<String, String>>();
			Map<String, String> temp = new HashMap<>();
			
			switch (bioAttribute) {
			case "leftEye":
				eyeMap.put("leftEye", "captured");
				eyeMap.put("primaryLabel", applicationLanguageProperties.getString("lefteye"));
				eyeMap.put("secondaryLabel", getSecondaryLanguageLabel("lefteye"));
				eyeMap.put("imageSource", getEncodedImage(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH, response, 
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING));				
				temp.put("class", "leftEye");
				temp.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
						RegistrationConstants.TEMPLATE_RIGHT_MARK);
				tempList.add(temp);
				eyeMap.put("attributes", tempList);
				break;
			case "rightEye":
				eyeMap.put("primaryLabel", applicationLanguageProperties.getString("righteye"));
				eyeMap.put("secondaryLabel", getSecondaryLanguageLabel("righteye"));
				eyeMap.put("imageSource", getEncodedImage(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH, response, 
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING));				
				temp.put("class", "leftEye");
				temp.put("classHeader", isException ? RegistrationConstants.TEMPLATE_CROSS_MARK : 
						RegistrationConstants.TEMPLATE_RIGHT_MARK);
				tempList.add(temp);
				eyeMap.put("attributes", tempList);				
				break;
			}			
			break;
			
		case RegistrationConstants.FACE_FULLFACE:
		case "Face":
			break;
		}
	}
	
	private String getEncodedImage(String imagePath, ResponseDTO response, String encoding) {
		byte[] bytes = null;
		try {
			bytes = IOUtils.toByteArray(this.getClass().getResourceAsStream(imagePath));
			return encoding + StringUtils.newStringUtf8(Base64.encodeBase64(bytes, false));
		} catch (IOException ioException) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}
		return null;
	}

	private void setUpPreviewContent(RegistrationDTO registration, Map<String, Object> templateValues, boolean isChild,
			ResponseDTO response, ResourceBundle applicationLanguageProperties, String fingerPrintDisableFlag) {
		ByteArrayOutputStream byteArrayOutputStream = null;

		templateValues.put(RegistrationConstants.TEMPLATE_ACKNOWLEDGEMENT,
				RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		if (registration.getSelectionListDTO() != null || (registration.getRegistrationMetaDataDTO() != null
				&& registration.getRegistrationMetaDataDTO().getRegistrationCategory() != null
				&& registration.getRegistrationMetaDataDTO().getRegistrationCategory()
						.equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_LOST))) {
			templateValues.put(RegistrationConstants.TEMPLATE_IS_UIN_UPDATE,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_USER_LANG_LABEL, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_LOCAL_LANG_LABEL, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID, RegistrationConstants.EMPTY);
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_USER_LANG_LABEL,
					applicationLanguageProperties.getString("preRegistrationId"));
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("preRegistrationId"));
			if (registration.getPreRegistrationId() != null && !registration.getPreRegistrationId().isEmpty()) {
				templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID, registration.getPreRegistrationId());
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID, "-");
			}
		}

		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY, applicationLanguageProperties.getString("modify"));

		try {
			BufferedImage modifyImage = ImageIO
					.read(this.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_PATH));
			byteArrayOutputStream = new ByteArrayOutputStream();
			ImageIO.write(modifyImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
			byte[] modifyImageBytes = byteArrayOutputStream.toByteArray();
			String modifyImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(modifyImageBytes, false));
			templateValues.put(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_SOURCE,
					RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + modifyImageEncodedBytes);
		} catch (IOException ioException) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		} finally {
			if (byteArrayOutputStream != null) {
				try {
					byteArrayOutputStream.close();
				} catch (IOException exception) {
					setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
					LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
							exception.getMessage() + ExceptionUtils.getStackTrace(exception));
				}
			}
		}
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(fingerPrintDisableFlag)) {
			boolean leftPalmCaptured = false;
			boolean rightPalmCaptured = false;
			boolean thumbsCaptured = false;
			Map<String, BiometricsDto> fingerprintDetailsDTO;
			if (isChild || registration.isUpdateUINNonBiometric()) {
				fingerprintDetailsDTO = registration.getBiometrics();
			} else {
				fingerprintDetailsDTO = registration.getBiometrics();
			}
			
			
			for (Entry<String, BiometricsDto> entry : fingerprintDetailsDTO.entrySet()) {

				if (entry.getValue().getModalityName().equals(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
					leftPalmCaptured = true;
					byte[] leftPalmBytes = null;
					try {
						leftPalmBytes = IOUtils.toByteArray(
								this.getClass().getResourceAsStream(RegistrationConstants.LEFTPALM_IMG_PATH));
					} catch (IOException ioException) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
					}
					String leftPalmEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(leftPalmBytes, false));
					templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_LEFT_SLAP,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + leftPalmEncodedBytes);
				} else if (entry.getValue().getModalityName().equals(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
					rightPalmCaptured = true;
					byte[] rightPalmBytes = null;
					try {
						rightPalmBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream(RegistrationConstants.RIGHTPALM_IMG_PATH)
								);
					} catch (IOException ioException) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
					}
					String rightPalmEncodedBytes = StringUtils
							.newStringUtf8(Base64.encodeBase64(rightPalmBytes, false));
					templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_SLAP,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + rightPalmEncodedBytes);
				} else if (entry.getValue().getModalityName().equals(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
					thumbsCaptured = true;
					byte[] thumbsBytes = null;
					try {
						thumbsBytes = IOUtils.toByteArray(
								this.getClass().getResourceAsStream(RegistrationConstants.THUMB_IMG_PATH));
					} catch (IOException ioException) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
					}
					String thumbsEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(thumbsBytes, false));
					templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_THUMBS,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + thumbsEncodedBytes);
				}

			}
			
			/*for (Map<String, BiometricsDto> fpDetailsDTO : fingerprintDetailsDTO) {
				if (fpDetailsDTO.getFingerType().contains(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
					leftPalmCaptured = true;
					byte[] leftPalmBytes = fpDetailsDTO.getFingerPrint();
					String leftPalmEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(leftPalmBytes, false));
					templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_LEFT_SLAP,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + leftPalmEncodedBytes);
				} else if (fpDetailsDTO.getFingerType().contains(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
					rightPalmCaptured = true;
					byte[] rightPalmBytes = fpDetailsDTO.getFingerPrint();
					String rightPalmEncodedBytes = StringUtils
							.newStringUtf8(Base64.encodeBase64(rightPalmBytes, false));
					templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_SLAP,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + rightPalmEncodedBytes);
				} else if (fpDetailsDTO.getFingerType().contains(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
					thumbsCaptured = true;
					byte[] thumbsBytes = fpDetailsDTO.getFingerPrint();
					String thumbsEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(thumbsBytes, false));
					templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_THUMBS,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + thumbsEncodedBytes);
				}
			}*/
			if (!leftPalmCaptured) {
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_SLAP_CAPTURED,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
			if (!rightPalmCaptured) {
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_SLAP_CAPTURED,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
			if (!thumbsCaptured) {
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_CAPTURED,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
		}

		templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_HEADING,
				applicationLanguageProperties.getString("consentHeading"));
		templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_DATA, consentText);
		templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_YES, applicationLanguageProperties.getString("yes"));
		templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_NO, applicationLanguageProperties.getString("no"));
		if (registration.getRegistrationMetaDataDTO().getConsentOfApplicant() != null) {
			String consent = registration.getRegistrationMetaDataDTO().getConsentOfApplicant();
			if (consent.equalsIgnoreCase(RegistrationConstants.YES)) {
				templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_SELECTED_YES,
						RegistrationConstants.TEMPLATE_CONSENT_CHECKED);
			} else if (consent.equalsIgnoreCase(RegistrationConstants.NO)) {
				templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_SELECTED_NO,
						RegistrationConstants.TEMPLATE_CONSENT_CHECKED);
			}
		}
	}

	private void setUpAcknowledgementContent(RegistrationDTO registration, Map<String, Object> templateValues,
			ResponseDTO response, ResourceBundle applicationLanguageProperties, String fingerPrintDisableFlag,
			String irisDisableFlag) {
		ByteArrayOutputStream byteArrayOutputStream = null;

		templateValues.put(RegistrationConstants.TEMPLATE_PREVIEW, RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		templateValues.put(RegistrationConstants.TEMPLATE_RID_USER_LANG_LABEL,
				applicationLanguageProperties.getString("registrationid"));
		templateValues.put(RegistrationConstants.TEMPLATE_RID_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("registrationid"));
		templateValues.put(RegistrationConstants.TEMPLATE_RID, registration.getRegistrationId());
		if (registration.getRegistrationMetaDataDTO().getUin() != null
				&& !registration.getRegistrationMetaDataDTO().getUin().isEmpty()) {
			templateValues.put(RegistrationConstants.TEMPLATE_HEADER_TABLE,
					RegistrationConstants.TEMPLATE_UIN_HEADER_TABLE);
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_USER_LANG_LABEL,
					applicationLanguageProperties.getString("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN, registration.getRegistrationMetaDataDTO().getUin());
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_HEADER_TABLE,
					RegistrationConstants.TEMPLATE_HEADER_TABLE);
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_UPDATE,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}

		// QR Code Generation
		generateQRCode(registration, templateValues, response, applicationLanguageProperties);

		if (RegistrationConstants.ENABLE.equalsIgnoreCase(irisDisableFlag)) {
			try {
				BufferedImage eyeImage = ImageIO
						.read(this.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH));
				byteArrayOutputStream = new ByteArrayOutputStream();
				ImageIO.write(eyeImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
				byte[] eyeImageBytes = byteArrayOutputStream.toByteArray();
				String eyeImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(eyeImageBytes, false));
				templateValues.put(RegistrationConstants.TEMPLATE_EYE_IMAGE_SOURCE,
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + eyeImageEncodedBytes);
			} catch (IOException ioException) {
				setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
				LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID, ioException.getMessage());
			} finally {
				if (byteArrayOutputStream != null) {
					try {
						byteArrayOutputStream.close();
					} catch (IOException exception) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION,
								null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								exception.getMessage() + ExceptionUtils.getStackTrace(exception));
					}
				}
			}
		}

		if (RegistrationConstants.ENABLE.equalsIgnoreCase(fingerPrintDisableFlag)) {
			try {
				BufferedImage leftPalmImage = ImageIO
						.read(this.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_LEFT_SLAP_IMAGE_PATH));
				byteArrayOutputStream = new ByteArrayOutputStream();
				ImageIO.write(leftPalmImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
				byte[] leftPalmImageBytes = byteArrayOutputStream.toByteArray();
				String leftPalmImageEncodedBytes = StringUtils
						.newStringUtf8(Base64.encodeBase64(leftPalmImageBytes, false));
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_IMAGE_SOURCE,
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + leftPalmImageEncodedBytes);
			} catch (IOException ioException) {
				setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
				LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID, ioException.getMessage());
			} finally {
				if (byteArrayOutputStream != null) {
					try {
						byteArrayOutputStream.close();
					} catch (IOException exception) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION,
								null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								exception.getMessage() + ExceptionUtils.getStackTrace(exception));
					}
				}
			}

			try {
				BufferedImage rightPalmImage = ImageIO.read(
						this.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_RIGHT_SLAP_IMAGE_PATH));
				byteArrayOutputStream = new ByteArrayOutputStream();
				ImageIO.write(rightPalmImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
				byte[] rightPalmImageBytes = byteArrayOutputStream.toByteArray();
				String rightPalmImageEncodedBytes = StringUtils
						.newStringUtf8(Base64.encodeBase64(rightPalmImageBytes, false));
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_IMAGE_SOURCE,
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + rightPalmImageEncodedBytes);
			} catch (IOException ioException) {
				setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
				LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
						ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			} finally {
				if (byteArrayOutputStream != null) {
					try {
						byteArrayOutputStream.close();
					} catch (IOException exception) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION,
								null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								exception.getMessage() + ExceptionUtils.getStackTrace(exception));
					}
				}
			}

			try {
				BufferedImage thumbsImage = ImageIO
						.read(this.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_THUMBS_IMAGE_PATH));
				byteArrayOutputStream = new ByteArrayOutputStream();
				ImageIO.write(thumbsImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
				byte[] thumbsImageBytes = byteArrayOutputStream.toByteArray();
				String thumbsImageEncodedBytes = StringUtils
						.newStringUtf8(Base64.encodeBase64(thumbsImageBytes, false));
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_IMAGE_SOURCE,
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + thumbsImageEncodedBytes);
			} catch (IOException ioException) {
				setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
				LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
						ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			} finally {
				if (byteArrayOutputStream != null) {
					try {
						byteArrayOutputStream.close();
					} catch (IOException exception) {
						setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION,
								null);
						LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
								exception.getMessage() + ExceptionUtils.getStackTrace(exception));
					}
				}
			}
		}
	}

	private void generateQRCode(RegistrationDTO registration, Map<String, Object> templateValues, ResponseDTO response,
			ResourceBundle applicationLanguageProperties) {
		StringBuilder qrCodeString = new StringBuilder();

		qrCodeString.append(applicationLanguageProperties.getString("registrationid")).append(" : ").append("\n")
				.append(registration.getRegistrationId());
		try {
			byte[] qrCodeInBytes = qrCodeGenerator.generateQrCode(qrCodeString.toString(), QrVersion.V4);

			String qrCodeImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(qrCodeInBytes, false));
			templateValues.put(RegistrationConstants.TEMPLATE_QRCODE_SOURCE,
					RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + qrCodeImageEncodedBytes);
		} catch (IOException | QrcodeGenerationException exception) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
		}
	}

	private void addToCapturedBiometrics(StringBuilder biometricsCaptured, StringBuilder biometricsCapturedLocalLang,
			ResourceBundle applicationLanguageProperties, String biometricType, int count) {
		if (biometricsCaptured.length() > 1) {
			biometricsCaptured.append(applicationLanguageProperties.getString("comma"));
			biometricsCapturedLocalLang.append(getSecondaryLanguageLabel("comma"));
		}
		biometricsCaptured.append(MessageFormat.format((String) applicationLanguageProperties.getString(biometricType),
				String.valueOf(count)));
		biometricsCapturedLocalLang
				.append(MessageFormat.format(getSecondaryLanguageLabel(biometricType), String.valueOf(count)));
	}

	private Map<String, Object> countMissingIrises(Map<String, Object> templateValues, RegistrationDTO registration,
			boolean isChild, String templateType,ResponseDTO response) {
		if (RegistrationConstants.ENABLE.equalsIgnoreCase(
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.IRIS_DISABLE_FLAG)))) {
			Map<String, BiometricsDto> irisDetailsDTOs;
			if (isChild || registration.isUpdateUINNonBiometric()) {
				irisDetailsDTOs = registration.getBiometrics();
			} else {
				irisDetailsDTOs = registration.getBiometrics();
			}
			List<Entry<String, BiometricsDto>> listOfIris = irisDetailsDTOs.entrySet().stream()
					.filter(obj -> obj.getValue().getModalityName().contains("IRIS_DOUBLE"))
					.collect(Collectors.toList());
			if (listOfIris.size() == 2) {
				if (templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE)) {
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
							RegistrationConstants.TEMPLATE_RIGHT_MARK);
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
							RegistrationConstants.TEMPLATE_RIGHT_MARK);
				} else {
					for (Entry<String, BiometricsDto> capturedIris : listOfIris) {
						if (capturedIris.getValue().getBioAttribute().equalsIgnoreCase("leftEye")) {
							byte[] leftIrisBytes = null;
							try {
								leftIrisBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream(RegistrationConstants.LEFT_IRIS_IMG_PATH));
							} catch (IOException ioException) {
								setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
								LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
										ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
							}
							String leftIrisEncodedBytes = StringUtils
									.newStringUtf8(Base64.encodeBase64(leftIrisBytes, false));
							templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_LEFT_EYE,
									RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + leftIrisEncodedBytes);
						} else if (capturedIris.getValue().getBioAttribute().equalsIgnoreCase("rightEye")) {
							byte[] rightIrisBytes = null;
							try {
								rightIrisBytes = IOUtils.toByteArray(
										this.getClass().getResourceAsStream(RegistrationConstants.RIGHT_IRIS_IMG_PATH));
							} catch (IOException ioException) {
								setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
								LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
										ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
							}
							String rightIrisEncodedBytes = StringUtils
									.newStringUtf8(Base64.encodeBase64(rightIrisBytes, false));
							templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + rightIrisEncodedBytes);
						}
					}
				}

			}

			else if (listOfIris.size() == 1) {
				Optional<Entry<String, BiometricsDto>> leftIris = listOfIris.stream()
						.filter(obj -> obj.getValue().getBioAttribute().equalsIgnoreCase("leftEye")).findAny();
				if (leftIris.isPresent()) {
					if (templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE)) {
						if (isChild || registration.isUpdateUINNonBiometric()) {
							templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
									RegistrationConstants.TEMPLATE_RIGHT_MARK);
							templateValues.put(RegistrationConstants.PARENT_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
						} else {
							templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
									RegistrationConstants.TEMPLATE_RIGHT_MARK);
							templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_CROSS_MARK);
						}
					} else {
						byte[] leftIrisBytes = null;
						try {
							leftIrisBytes = IOUtils.toByteArray(
									this.getClass().getResourceAsStream(RegistrationConstants.RIGHT_IRIS_IMG_PATH));
						} catch (IOException ioException) {
							setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
							LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
									ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
						}
						String leftIrisEncodedBytes = StringUtils
								.newStringUtf8(Base64.encodeBase64(leftIrisBytes, false));
						templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_LEFT_EYE,
								RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + leftIrisEncodedBytes);
						templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_CAPTURED,
								RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					}

				} else {
					if (templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE)) {
						if (isChild || registration.isUpdateUINNonBiometric()) {
							templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_RIGHT_MARK);
							templateValues.put(RegistrationConstants.PARENT_LEFT_EYE,
									RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
						} else {
							templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
									RegistrationConstants.TEMPLATE_CROSS_MARK);
							templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_RIGHT_MARK);
						}
					} else {
						byte[] rightIrisBytes = null;
						try {
							rightIrisBytes = IOUtils.toByteArray(
									this.getClass().getResourceAsStream(RegistrationConstants.LEFT_IRIS_IMG_PATH));
						} catch (IOException ioException) {
							setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
							LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
									ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
						}
						String rightIrisEncodedBytes = StringUtils
								.newStringUtf8(Base64.encodeBase64(rightIrisBytes, false));
						templateValues.put(RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_EYE,
								RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + rightIrisEncodedBytes);
						templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_CAPTURED,
								RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					}
				}
			}

			else if (listOfIris.isEmpty()) {
				if (templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE)) {
					if (isChild || registration.isUpdateUINNonBiometric()) {
						if (registration.getBiometricDTO().getIntroducerBiometricDTO().getFace() != null && registration
								.getBiometricDTO().getIntroducerBiometricDTO().getFace().getFace() != null) {
							templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
									RegistrationConstants.TEMPLATE_CROSS_MARK);
							templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_CROSS_MARK);
						} else {
							templateValues.put(RegistrationConstants.PARENT_LEFT_EYE,
									RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
							templateValues.put(RegistrationConstants.PARENT_RIGHT_EYE,
									RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
						}
					} else {
						templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
								RegistrationConstants.TEMPLATE_CROSS_MARK);
						templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
								RegistrationConstants.TEMPLATE_CROSS_MARK);
					}
				} else {
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_CAPTURED,
							RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_CAPTURED,
							RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
				}
			}
		}
		return templateValues;
	}

	private void countMissingFingers(RegistrationDTO registration, Map<String, Object> templateValues,
			ResourceBundle applicationLanguageProperties, ResourceBundle localProperties) {
		int missingLeftFingers = 0;
		int missingRightFingers = 0;
		int missingThumbs = 0;
		List<BiometricExceptionDTO> exceptionFingers = registration.getBiometricDTO().getApplicantBiometricDTO()
				.getBiometricExceptionDTO();
		if (exceptionFingers != null) {
			for (BiometricExceptionDTO exceptionFinger : exceptionFingers) {
				if (exceptionFinger.getBiometricType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT)) {
					if (exceptionFinger.getMissingBiometric().toLowerCase()
							.contains(RegistrationConstants.THUMB.toLowerCase())) {
						missingThumbs++;
					} else if (exceptionFinger.getMissingBiometric().toLowerCase()
							.contains(RegistrationConstants.LEFT.toLowerCase())) {
						missingLeftFingers++;
					} else if (exceptionFinger.getMissingBiometric().toLowerCase()
							.contains(RegistrationConstants.RIGHT.toLowerCase())) {
						missingRightFingers++;
					}
				}
			}
			if (missingLeftFingers != 0) {
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_SLAP_EXCEPTION_USER_LANG,
						missingLeftFingers + " " + applicationLanguageProperties.getString("exceptionCount"));
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_SLAP_EXCEPTION_LOCAL_LANG,
						getSecondaryLanguageLabel("exceptionCount") + " " + missingLeftFingers);
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_MISSING_LEFT_FINGERS,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
			if (missingRightFingers != 0) {
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_SLAP_EXCEPTION_USER_LANG,
						missingRightFingers + " " + applicationLanguageProperties.getString("exceptionCount"));
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_SLAP_EXCEPTION_LOCAL_LANG,
						getSecondaryLanguageLabel("exceptionCount") + " " + missingRightFingers);
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_MISSING_RIGHT_FINGERS,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
			if (missingThumbs != 0) {
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_EXCEPTION_USER_LANG,
						missingThumbs + " " + applicationLanguageProperties.getString("exceptionCount"));
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_EXCEPTION_LOCAL_LANG,
						getSecondaryLanguageLabel("exceptionCount") + " " + missingThumbs);
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_MISSING_THUMBS,
						RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}
		}
	}

	/**
	 * This method generates the content that will be sent to the applicant via
	 * email/SMS after a successful registration.
	 * 
	 * <p>
	 * The details that are required to be attached in the email/SMS will be mapped
	 * to the place-holders given in the HTML template and then, the template is
	 * build.
	 * </p>
	 * 
	 * <p>
	 * Returns the generated content in string format.
	 * </p>
	 * 
	 * @param templateText
	 *            - string which contains the data of template that is used to
	 *            generate notification
	 * @param registration
	 *            - RegistrationDTO to display required fields on the template
	 * @param templateManagerBuilder
	 *            - The Builder which generates template by mapping values to
	 *            respective place-holders in template
	 * @return writer - After mapping all the fields into the template, it is
	 *         written into a StringWriter and returned
	 */
	public Writer generateNotificationTemplate(String templateText, RegistrationDTO registration,
			TemplateManagerBuilder templateManagerBuilder) {

		try {
			String applicationLanguageCode = ApplicationContext.applicationLanguage().toLowerCase();
			InputStream is = new ByteArrayInputStream(templateText.getBytes());
			Map<String, Object> values = new LinkedHashMap<>();
			
			SimpleDateFormat sdf = new SimpleDateFormat(RegistrationConstants.TEMPLATE_DATE_FORMAT);
			String currentDate = sdf.format(new Date());
			values.put(RegistrationConstants.TEMPLATE_DATE, currentDate);
			values.put(RegistrationConstants.TEMPLATE_RID, getValue(registration.getRegistrationId()));
			
			for(String key : registration.getDemographics().keySet()) {
				String value = getValueForTemplate(registration.getDemographics().get(key), applicationLanguageCode);
				values.put(key.toUpperCase(), value);
			}			

			Writer writer = new StringWriter();
			try {
				TemplateManager templateManager = templateManagerBuilder.build();
				String defaultEncoding = null;
				InputStream inputStream = templateManager.merge(is, values);
				IOUtils.copy(inputStream, writer, defaultEncoding);
			} catch (IOException exception) {
				LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
						"generateNotificationTemplate method has been ended for preparing Notification Template.");
			}
			return writer;
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationConstants.TEMPLATE_GENERATOR_SMS_EXCEPTION,
					runtimeException.getMessage(), runtimeException);
		}
	}

	/**
	 * @param enrolment
	 *            - EnrolmentDTO to get the biometric details
	 * @return hash map which gives the set of fingerprints captured and their
	 *         respective rankings based on quality score
	 */
	@SuppressWarnings({ "unchecked" })
	private Map<String, Integer> getFingerPrintQualityRanking(RegistrationDTO registration) {
		// for storing the fingerprints captured and their respective quality scores
		Map<String, Double> fingersQuality = new WeakHashMap<>();

		// list of missing fingers
		Map<String, BiometricsException> exceptionFingers = registration.getBiometricExceptions();

		if (exceptionFingers != null) {
			for (Entry<String, BiometricsException> exceptionFinger : exceptionFingers.entrySet()) {
				/*if (exceptionFinger.getValue().getExceptionType().equalsIgnoreCase(RegistrationConstants.FINGERPRINT)
						&& exceptionFinger.getValue().getMissingBiometric().equals(RegistrationConstants.MISSING_BIOMETRICS)) {*/
					fingersQuality.put(exceptionFinger.getValue().getMissingBiometric(), (double) 0);
				//}
			}
		}
		Map<String, BiometricsDto> availableFingers = registration.getBiometrics();
		for (Entry<String, BiometricsDto> availableFinger : availableFingers.entrySet()) {
			fingersQuality.put(availableFinger.getValue().getBioAttribute(),
					availableFinger.getValue().getQualityScore());
		}

		Object[] fingerQualitykeys = fingersQuality.entrySet().toArray();
		Arrays.sort(fingerQualitykeys, new Comparator<Object>() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			public int compare(Object fingetPrintQuality1, Object fingetPrintQuality2) {
				return ((Map.Entry<String, Double>) fingetPrintQuality2).getValue()
						.compareTo(((Map.Entry<String, Double>) fingetPrintQuality1).getValue());
			}
		});

		LinkedHashMap<String, Double> fingersQualitySorted = new LinkedHashMap<>();
		for (Object fingerPrintQualityKey : fingerQualitykeys) {
			String finger = ((Map.Entry<String, Double>) fingerPrintQualityKey).getKey();
			double quality = ((Map.Entry<String, Double>) fingerPrintQualityKey).getValue();
			fingersQualitySorted.put(finger, quality);
		}

		Map<String, Integer> fingersQualityRanking = new WeakHashMap<>();
		int rank = 1;
		double prev = 1.0;
		for (Map.Entry<String, Double> entry : fingersQualitySorted.entrySet()) {
			if (entry.getValue() != 0) {
				if (Double.compare(entry.getValue(), prev) == 0 || Double.compare(prev, 1.0) == 0) {
					fingersQualityRanking.put(entry.getKey(), rank);
				} else {
					fingersQualityRanking.put(entry.getKey(), ++rank);
				}
				prev = entry.getValue();
			} else {
				fingersQualityRanking.put(entry.getKey(), entry.getValue().intValue());
			}
		}
		return fingersQualityRanking;
	}
	
	@SuppressWarnings("unchecked")
	private String getValueForTemplate(Object fieldValue, String lang) {
		String value = RegistrationConstants.EMPTY;

		if (fieldValue instanceof List<?>) {
			Optional<SimpleDto> demoValueInRequiredLang = ((List<SimpleDto>) fieldValue).stream()
					.filter(valueDTO -> valueDTO.getLanguage().equals(lang)).findFirst();

			if (demoValueInRequiredLang.isPresent() && demoValueInRequiredLang.get().getValue() != null) {
				value = demoValueInRequiredLang.get().getValue();
			}
		}
		

		else if (fieldValue instanceof String || fieldValue instanceof Integer || fieldValue instanceof BigInteger
				|| fieldValue instanceof Double) {
			value = String.valueOf(fieldValue);
		}
		return value == null ? RegistrationConstants.EMPTY : value;
	}

	@SuppressWarnings("unchecked")
	private String getValue(Object fieldValue, String lang) {
		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
				"Getting values of demographic fields in given specific language");
		String value = RegistrationConstants.EMPTY;

		if (fieldValue instanceof List<?>) {
			Optional<SimpleDto> demoValueInRequiredLang = ((List<SimpleDto>) fieldValue).stream()
					.filter(valueDTO -> valueDTO.getLanguage().equals(lang)).findFirst();

			if (demoValueInRequiredLang.isPresent() && demoValueInRequiredLang.get().getValue() != null) {
				value = demoValueInRequiredLang.get().getValue();
			}
		}

		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
				"Getting values of demographic fields in given specific language has been completed");
		return value;
	}

	private String getSecondaryLanguageValue(Object fieldValue, String lang) {
		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
				"Getting values of demographic fields in given specific language");
		String value = RegistrationConstants.EMPTY;

		if (!ApplicationContext.applicationLanguage().equalsIgnoreCase(ApplicationContext.localLanguage())) {
			value = getValue(fieldValue, lang);
		}

		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
				"Getting values of demographic fields in given specific language has been completed");
		return value;
	}

	private String getValue(Object fieldValue) {
		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID, "Getting values of demographic fields");
		String value = RegistrationConstants.EMPTY;

		if (fieldValue instanceof String || fieldValue instanceof Integer || fieldValue instanceof BigInteger
				|| fieldValue instanceof Double) {
			value = String.valueOf(fieldValue);
		} else {

			if(null!=fieldValue) {
			@SuppressWarnings("unchecked")
			List<SimpleDto> valueList=(List<SimpleDto>) fieldValue;
			value = valueList.get(0).getValue();
			}
		}

		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
				"Getting values of demographic fields has been completed");
		return value;
	}

	/**
	 * To count the number of exceptions for face/iris/fingerprint
	 */
	private Map<String, Integer> exceptionFingersCount(RegistrationDTO registration, int leftSlapCount,
			int rightSlapCount, int thumbCount, int irisCount) {

		Map<String, Integer> exceptionCountMap = new HashMap<>();
		List<BiometricExceptionDTO> biometricExceptionDTOs;
		if ((registration.getSelectionListDTO() == null
				&& (boolean) SessionContext.map().get(RegistrationConstants.IS_Child))
				|| (registration.getSelectionListDTO() != null
						&& registration.getSelectionListDTO().get("biometrics")!=null)) {
			biometricExceptionDTOs = registration.getBiometricDTO().getIntroducerBiometricDTO()
					.getBiometricExceptionDTO();
			for (BiometricExceptionDTO biometricExceptionDTO : biometricExceptionDTOs) {
				if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.LEFT.toLowerCase())
						&& biometricExceptionDTO.isMarkedAsException())
						&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
						&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)) {
					leftSlapCount++;
				}
				if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.RIGHT.toLowerCase())
						&& biometricExceptionDTO.isMarkedAsException())
						&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
						&& !biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)) {
					rightSlapCount++;
				}
				if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.THUMB)
						&& biometricExceptionDTO.isMarkedAsException())) {
					thumbCount++;
				}
				if ((biometricExceptionDTO.getMissingBiometric().contains(RegistrationConstants.EYE)
						&& biometricExceptionDTO.isMarkedAsException())) {
					irisCount++;
				}
			}
			exceptionCountMap.put(RegistrationConstants.LEFTSLAPCOUNT, leftSlapCount);
			exceptionCountMap.put(RegistrationConstants.RIGHTSLAPCOUNT, rightSlapCount);
			exceptionCountMap.put(RegistrationConstants.THUMBCOUNT, thumbCount);
			exceptionCountMap.put(RegistrationConstants.EXCEPTIONCOUNT,
					leftSlapCount + rightSlapCount + thumbCount + irisCount);
		}

		return exceptionCountMap;
	}
	
	private List<UiSchemaDTO> getSchemaFields(double idVersion) throws RegBaseCheckedException {
		List<UiSchemaDTO> schemaFields;
		try {
			schemaFields = identitySchemaServiceImpl.getUISchema(idVersion);
		} catch (RegBaseCheckedException e) {
			throw e;
		}
		return schemaFields;
	}
}