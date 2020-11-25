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

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
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
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
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
	 * @param templateText           - string which contains the data of template
	 *                               that is used to generate acknowledgement
	 * @param registration           - RegistrationDTO to display required fields on
	 *                               the template
	 * @param templateManagerBuilder - The Builder which generates template by
	 *                               mapping values to respective place-holders in
	 *                               template
	 * @param templateType           - The type of template that is required (like
	 *                               email/sms/acknowledgement)
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

			List<UiSchemaDTO> schemaFields = getSchemaFields(registration.getIdSchemaVersion());

			boolean isAckTemplate = false;

			if (!isLocalLanguageAvailable()) {
				templateValues.put("localLangAvailable", "");
			} else {
				templateValues.put("localLangAvailable", "/");
			}
			if (templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE)) {
				/* Set-up Registration Acknowledgement related content */
				setUpAcknowledgementContent(registration, templateValues, response, applicationLanguageProperties);
				/* Set-up important guidelines that are configured by the country */
				setUpImportantGuidelines(templateValues, guidelines);

				isAckTemplate = true;
			} else {
				/* Set-up Registration Preview related content */
				setUpPreviewContent(registration, templateValues, response, applicationLanguageProperties);
			}

			/* Set-up demographic information related content */
			setUpDemographicInfo(registration, templateValues, applicationLanguageProperties, schemaFields);

			/* Set-up the list of documents submitted by the applicant */
			setUpDocuments(templateValues, applicationLanguageProperties, registration.getDocuments(),
					documentDisableFlag);

			setUpBiometricData(templateValues, registration, applicationLanguageProperties, schemaFields, response,
					isAckTemplate);

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

	private Map<String, List<BiometricsDto>> getBiometricsFields(RegistrationDTO registration,
			List<UiSchemaDTO> fields) {
		Map<String, List<BiometricsDto>> biometricDetails = new HashMap<>();
		List<UiSchemaDTO> biometricFields = fields.stream()
				.filter(field -> PacketManagerConstants.BIOMETRICS_DATATYPE.equals(field.getType())
						&& field.getSubType() != null && field.getBioAttributes() != null)
				.collect(Collectors.toList());

		for (UiSchemaDTO biometricField : biometricFields) {
			List<BiometricsDto> list = new ArrayList<>();
			List<BiometricsDto> exceptionList = new ArrayList<>();
			for (String attribute : biometricField.getBioAttributes()) {
				String key = String.format("%s_%s", biometricField.getSubType(), attribute);
				if (registration.getBiometrics().containsKey(key))
					list.add(registration.getBiometrics().get(key));
				else if (registration.getBiometricExceptions().containsKey(key)) {
					BiometricsDto dto = new BiometricsDto(attribute, null, 0);
					dto.setModalityName(Biometric.getModalityNameByAttribute(attribute));
					exceptionList.add(dto);
				}
			}

			if (list.isEmpty() && exceptionList.isEmpty())
				continue;

			biometricDetails.put(biometricField.getId(), list);
			biometricDetails.put(biometricField.getId() + "_EXCEPTIONS", exceptionList);
		}
		return biometricDetails;
	}

	private void setUpBiometricData(Map<String, Object> templateValues, RegistrationDTO registration,
			ResourceBundle applicationLanguageProperties, List<UiSchemaDTO> fields, ResponseDTO response,
			boolean isAckTemplate) {
		Map<String, List<BiometricsDto>> biometricDetails = getBiometricsFields(registration, fields);

		if (biometricDetails == null || biometricDetails.isEmpty()) {
			templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_ENABLED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			return;
		}

		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_ENABLED, RegistrationConstants.EMPTY);
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_USER_LANG_LABEL,
				applicationLanguageProperties.getString("biometricsHeading"));
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_USER_LANG_LABEL,
				applicationLanguageProperties.getString("biometricsHeading"));
		templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("biometricsHeading"));

		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY, applicationLanguageProperties.getString("modify"));
		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_SOURCE,
				getEncodedImage(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_PATH, response,
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING));

//		templateValues.put("applicantPhoto", RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);

		templateValues.put("biometricsData", new ArrayList<HashMap<String, Object>>());

		for (String fieldId : biometricDetails.keySet()) {
			if (fieldId.endsWith("_EXCEPTIONS"))
				continue;

			Map<String, Object> fieldTemplateValues = new HashMap<String, Object>();
			UiSchemaDTO field = fields.stream().filter(f -> f.getId().equals(fieldId)).findFirst().get();
			fieldTemplateValues.put("BiometricsFieldPrimLabel", field.getLabel().get("primary"));
			fieldTemplateValues.put("BiometricsFieldSecLabel", isLocalLanguageAvailable() ? field.getLabel().get("secondary") : RegistrationConstants.EMPTY);

			List<BiometricsDto> dataCaptured = biometricDetails.get(fieldId);

			List<BiometricsDto> capturedFingers = dataCaptured.stream()
					.filter(d -> d.getModalityName().toLowerCase().contains("finger")).collect(Collectors.toList());

			List<BiometricsDto> capturedIris = dataCaptured.stream()
					.filter(d -> d.getModalityName().toLowerCase().contains("iris")).collect(Collectors.toList());

			List<BiometricsDto> capturedFace = dataCaptured.stream()
					.filter(d -> d.getModalityName().toLowerCase().contains("face")).collect(Collectors.toList());

			StringBuilder biometricsCaptured = new StringBuilder();
			StringBuilder biometricsCapturedLocalLang = new StringBuilder();

			String count = "0";

			if (capturedFingers != null) {
				count = String.valueOf(capturedFingers.size());
				biometricsCaptured.append(MessageFormat.format(
						(String) applicationLanguageProperties.getString("fingersCount"), String.valueOf(count)));
				biometricsCapturedLocalLang
						.append(MessageFormat.format(getSecondaryLanguageLabel("fingersCount"), String.valueOf(count)));
			}

			if (capturedIris != null) {
				count = String.valueOf(capturedIris.size());
				biometricsCaptured
						.append(biometricsCaptured.length() > 0 ? RegistrationConstants.COMMA
								: RegistrationConstants.EMPTY)
						.append(MessageFormat.format((String) applicationLanguageProperties.getString("irisCount"),
								String.valueOf(count)));
				biometricsCapturedLocalLang
						.append(biometricsCapturedLocalLang.length() > 0 ? RegistrationConstants.COMMA
								: RegistrationConstants.EMPTY)
						.append(MessageFormat.format(getSecondaryLanguageLabel("irisCount"), String.valueOf(count)))
						.append(RegistrationConstants.COMMA);
			}

			if (capturedFace != null) {
				count = String.valueOf(capturedFace.size());
				biometricsCaptured
						.append(biometricsCaptured.length() > 0 ? RegistrationConstants.COMMA
								: RegistrationConstants.EMPTY)
						.append(MessageFormat.format((String) applicationLanguageProperties.getString("faceCount"),
								String.valueOf(count)));
				biometricsCapturedLocalLang
						.append(biometricsCaptured.length() > 0 ? RegistrationConstants.COMMA
								: RegistrationConstants.EMPTY)
						.append(MessageFormat.format(getSecondaryLanguageLabel("faceCount"), String.valueOf(count)));
			}

			fieldTemplateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_CAPTURED, biometricsCaptured.toString());
			fieldTemplateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_CAPTURED_LOCAL_LANG,
					biometricsCapturedLocalLang.toString());

			fieldTemplateValues.put(RegistrationConstants.TEMPLATE_WITH_EXCEPTION,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);

			if (biometricDetails.get(fieldId + "_EXCEPTIONS") != null
					&& !biometricDetails.get(fieldId + "_EXCEPTIONS").isEmpty()) {
				byte[] exceptionImageBytes = null;
				Optional<Entry<String, DocumentDto>> exceptionImage = registration.getDocuments().entrySet().stream()
						.filter(obj -> obj.getValue().getCategory().equalsIgnoreCase("POE")).findFirst();
				if (exceptionImage.isPresent()) {
					exceptionImageBytes = exceptionImage.get().getValue().getDocument();
					fieldTemplateValues.put(RegistrationConstants.TEMPLATE_WITH_EXCEPTION, RegistrationConstants.EMPTY);
					fieldTemplateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_PHOTO_USER_LANG_LABEL,
							applicationLanguageProperties.getString("exceptionphoto"));
					fieldTemplateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_PHOTO_LOCAL_LANG_LABEL,
							getSecondaryLanguageLabel("exceptionphoto"));
					String exceptionImageEncodedBytes = StringUtils
							.newStringUtf8(Base64.encodeBase64(exceptionImageBytes, false));
					fieldTemplateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + exceptionImageEncodedBytes);
				}
			}

			setIrisTemplateValues(fieldTemplateValues, capturedIris,
					biometricDetails.get(fieldId + "_EXCEPTIONS") != null
							? biometricDetails.get(fieldId + "_EXCEPTIONS").stream()
									.filter(b -> b.getModalityName().toLowerCase().contains("iris"))
									.collect(Collectors.toList())
							: null,
					applicationLanguageProperties, isAckTemplate, response, registration);

			setFingerTemplateValues(fieldTemplateValues, capturedFingers,
					biometricDetails.get(fieldId + "_EXCEPTIONS") != null
							? biometricDetails.get(fieldId + "_EXCEPTIONS").stream()
									.filter(b -> b.getModalityName().toLowerCase().contains("finger"))
									.collect(Collectors.toList())
							: null,
					applicationLanguageProperties, isAckTemplate, response, registration);

			setFaceTemplateValues(fieldTemplateValues, capturedFace, applicationLanguageProperties, isAckTemplate,
					response, registration);

			if (capturedFace != null && !capturedFace.isEmpty()
					&& "applicant".equalsIgnoreCase(capturedFace.get(0).getSubType())) {
//				templateValues.put("applicantPhoto", RegistrationConstants.EMPTY);
				templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_USER_LANG,
						applicationLanguageProperties.getString("individualphoto"));
				templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_LOCAL_LANG,
						getSecondaryLanguageLabel("individualphoto"));

				setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_APPLICANT_IMAGE_SOURCE,
						RegistrationConstants.FACE_IMG_PATH, response,
						getStreamImageBytes(capturedFace.get(0), registration));
			} else {
				templateValues.put("uinUpdateWithoutBiometrics", RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			}

			((List) templateValues.get("biometricsData")).add(fieldTemplateValues);
		}
	}

	private byte[] getStreamImageBytes(BiometricsDto biometricsDto, RegistrationDTO registration) {
		return registration.streamImages.get(String.format("%s_%s_%s", biometricsDto.getSubType(),
				biometricsDto.getModalityName(), biometricsDto.getNumOfRetries()));
	}

	private void setFaceTemplateValues(Map<String, Object> templateValues, List<BiometricsDto> capturedFace,
			ResourceBundle applicationLanguageProperties, boolean isAckTemplate, ResponseDTO response,
			RegistrationDTO registration) {

		templateValues.put(RegistrationConstants.TEMPLATE_FACE_CAPTURE_ENABLED,
				RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);

		if (capturedFace != null && !capturedFace.isEmpty()) {
			templateValues.put(RegistrationConstants.TEMPLATE_FACE_CAPTURE_ENABLED, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_USER_LANG,
					applicationLanguageProperties.getString("FACE"));
			templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_LOCAL_LANG, getSecondaryLanguageLabel("FACE"));

			if (isAckTemplate) {
				setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_FACE_IMAGE_SOURCE,
						RegistrationConstants.FACE_IMG_PATH, response,
						getStreamImageBytes(capturedFace.get(0), registration));
			} else {
				setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_FACE_IMAGE_SOURCE,
						RegistrationConstants.FACE_IMG_PATH, response,
						getStreamImageBytes(capturedFace.get(0), registration));
			}
		}
	}

	private void setFingerTemplateValues(Map<String, Object> templateValues, List<BiometricsDto> capturedFingers,
			List<BiometricsDto> exceptions, ResourceBundle applicationLanguageProperties, boolean isAckTemplate,
			ResponseDTO response, RegistrationDTO registration) {

		if (capturedFingers == null && capturedFingers.isEmpty() && exceptions == null && exceptions.isEmpty()) {
			templateValues.put(RegistrationConstants.TEMPLATE_FINGERPRINTS_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		} else {

			Map<String, Object> leftSlab = setFingerSlabTemplateValues(capturedFingers, exceptions != null
					? exceptions.stream()
							.filter(b -> b.getModalityName().toLowerCase().contains("finger")
									&& b.getModalityName().toLowerCase().contains("left"))
							.collect(Collectors.toList())
					: null, isAckTemplate, Biometric.getDefaultAttributes("FINGERPRINT_SLAB_LEFT"),
					RegistrationConstants.TEMPLATE_LEFT_SLAP_CAPTURED);

			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_SLAP_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			if (!leftSlab.isEmpty()) {
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_SLAP_CAPTURED, RegistrationConstants.EMPTY);
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_USER_LANG_LABEL,
						applicationLanguageProperties.getString("lefthandpalm"));
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_LOCAL_LANG_LABEL,
						getSecondaryLanguageLabel("lefthandpalm"));
				templateValues.putAll(leftSlab);
				if (isAckTemplate) {
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_LEFT_PALM_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_LEFT_SLAP_IMAGE_PATH, response, null);
				} else {
					byte[] imageBytes = null;
					if (!capturedFingers.isEmpty()) {
						Optional<BiometricsDto> biometrics = capturedFingers.stream()
								.filter(b -> b.getModalityName().equalsIgnoreCase("FINGERPRINT_SLAB_LEFT")).findFirst();
						if (biometrics.isPresent()) {
							imageBytes = getStreamImageBytes(biometrics.get(), registration);
						}
					}
					setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_CAPTURED_LEFT_SLAP,
							RegistrationConstants.LEFTPALM_IMG_PATH, response, imageBytes);
				}
			}

			Map<String, Object> rightSlab = setFingerSlabTemplateValues(capturedFingers, exceptions != null
					? exceptions.stream()
							.filter(b -> b.getModalityName().toLowerCase().contains("finger")
									&& b.getModalityName().toLowerCase().contains("right"))
							.collect(Collectors.toList())
					: null, isAckTemplate, Biometric.getDefaultAttributes("FINGERPRINT_SLAB_RIGHT"),
					RegistrationConstants.TEMPLATE_RIGHT_SLAP_CAPTURED);

			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_SLAP_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			if (!rightSlab.isEmpty()) {
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_SLAP_CAPTURED, RegistrationConstants.EMPTY);
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_USER_LANG_LABEL,
						applicationLanguageProperties.getString("righthandpalm"));
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_LOCAL_LANG_LABEL,
						getSecondaryLanguageLabel("righthandpalm"));
				templateValues.putAll(rightSlab);
				if (isAckTemplate) {
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_RIGHT_PALM_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_RIGHT_SLAP_IMAGE_PATH, response, null);
				} else {
					byte[] imageBytes = null;
					if (!capturedFingers.isEmpty()) {
						Optional<BiometricsDto> biometrics = capturedFingers.stream()
								.filter(b -> b.getModalityName().equalsIgnoreCase("FINGERPRINT_SLAB_RIGHT"))
								.findFirst();
						if (biometrics.isPresent()) {
							imageBytes = getStreamImageBytes(biometrics.get(), registration);
						}
					}
					setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_SLAP,
							RegistrationConstants.RIGHTPALM_IMG_PATH, response, imageBytes);
				}
			}

			Map<String, Object> thumbsSlab = setFingerSlabTemplateValues(capturedFingers, exceptions != null
					? exceptions.stream()
							.filter(b -> b.getModalityName().toLowerCase().contains("finger")
									&& b.getModalityName().toLowerCase().contains("thumb"))
							.collect(Collectors.toList())
					: null, isAckTemplate, Biometric.getDefaultAttributes("FINGERPRINT_SLAB_THUMBS"),
					RegistrationConstants.TEMPLATE_THUMBS_CAPTURED);

			templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			if (!thumbsSlab.isEmpty()) {
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_CAPTURED, RegistrationConstants.EMPTY);
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_USER_LANG_LABEL,
						applicationLanguageProperties.getString("thumbs"));
				templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_LOCAL_LANG_LABEL,
						getSecondaryLanguageLabel("thumbs"));
				templateValues.putAll(thumbsSlab);
				if (isAckTemplate) {
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_THUMBS_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_THUMBS_IMAGE_PATH, response, null);
				} else {
					byte[] imageBytes = null;
					if (!capturedFingers.isEmpty()) {
						Optional<BiometricsDto> biometrics = capturedFingers.stream()
								.filter(b -> b.getModalityName().toLowerCase().contains("thumb")).findFirst();
						if (biometrics.isPresent()) {
							imageBytes = getStreamImageBytes(biometrics.get(), registration);
						}
					}
					setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_CAPTURED_THUMBS,
							RegistrationConstants.THUMB_IMG_PATH, response, imageBytes);
				}
			}
		}
	}

	private Map<String, Object> setFingerSlabTemplateValues(List<BiometricsDto> capturedFingers,
			List<BiometricsDto> exceptions, boolean isAckTemplate, List<String> fingers, String key) {
		Map<String, Object> templateValues = new HashMap<>();
		Map<String, Double> sortedvalues = capturedFingers.stream().filter(b -> fingers.contains(b.getBioAttribute()))
				.sorted(Comparator.comparing(BiometricsDto::getQualityScore))
				.collect(Collectors.toMap(BiometricsDto::getBioAttribute, BiometricsDto::getQualityScore));

		int rank = 0;
		double prev = 0;
		Map<String, Integer> rankings = new HashMap<>();
		for (Entry<String, Double> entry : sortedvalues.entrySet()) {
			rankings.put(entry.getKey(), prev == 0 ? ++rank : entry.getValue() == prev ? rank : ++rank);
			prev = entry.getValue();
		}

		for (String finger : fingers) {
			Optional<BiometricsDto> result = capturedFingers.stream()
					.filter(b -> b.getBioAttribute().equalsIgnoreCase(finger)).findFirst();
			Optional<BiometricsDto> exceptionResult = exceptions.stream()
					.filter(b -> b.getBioAttribute().equalsIgnoreCase(finger)).findFirst();

			if (isAckTemplate) {
				if (result.isPresent()) {
					templateValues.put(finger, rankings.get(finger));
				}

				if (exceptionResult.isPresent()) {
					templateValues.put(finger, RegistrationConstants.TEMPLATE_CROSS_MARK);
				}
			} else { // if its preview template
				templateValues.put(key,
						(result.isPresent() || exceptionResult.isPresent())
								? (exceptions != null && exceptions.size() == fingers.size())
										? RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY
										: RegistrationConstants.EMPTY
								: RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
				break;
			}
		}
		return templateValues;
	}

	private void setIrisTemplateValues(Map<String, Object> templateValues, List<BiometricsDto> capturedIris,
			List<BiometricsDto> exceptions, ResourceBundle applicationLanguageProperties, boolean isAckTemplate,
			ResponseDTO response, RegistrationDTO registration) {

		if (capturedIris == null && capturedIris.isEmpty() && exceptions == null && exceptions.isEmpty()) {
			templateValues.put(RegistrationConstants.TEMPLATE_IRIS_ENABLED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		} else {
			templateValues.put(RegistrationConstants.TEMPLATE_IRIS_ENABLED, RegistrationConstants.EMPTY);

			Optional<BiometricsDto> result = capturedIris.stream()
					.filter(b -> b.getBioAttribute().equalsIgnoreCase("leftEye")).findFirst();
			Optional<BiometricsDto> exceptionResult = exceptions.stream()
					.filter(b -> b.getBioAttribute().equalsIgnoreCase("leftEye")).findFirst();

			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			if (result.isPresent()) {
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_USER_LANG_LABEL,
						applicationLanguageProperties.getString("lefteye"));
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_LOCAL_LANG_LABEL,
						getSecondaryLanguageLabel("lefteye"));
				// templateValues.put(RegistrationConstants.IRIS_STYLE, "irisWithoutException");
				templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_CAPTURED, RegistrationConstants.EMPTY);

				if (isAckTemplate) {
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
							RegistrationConstants.TEMPLATE_RIGHT_MARK);
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_EYE_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH, response, null);
				} else {
					setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_CAPTURED_LEFT_EYE,
							RegistrationConstants.LEFT_IRIS_IMG_PATH, response,
							getStreamImageBytes(capturedIris.stream()
									.filter(b -> b.getBioAttribute().equalsIgnoreCase("leftEye")).findFirst().get(),
									registration));
				}
			}
			if (exceptionResult.isPresent()) {
				if (isAckTemplate) {
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_USER_LANG_LABEL,
							applicationLanguageProperties.getString("lefteye"));
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_LOCAL_LANG_LABEL,
							getSecondaryLanguageLabel("lefteye"));
					// templateValues.put(RegistrationConstants.IRIS_STYLE, "irisWithoutException");
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE,
							RegistrationConstants.TEMPLATE_CROSS_MARK);
					templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_CAPTURED, RegistrationConstants.EMPTY);
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_EYE_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH, response, null);
				}
			}

			result = capturedIris.stream().filter(b -> b.getBioAttribute().equalsIgnoreCase("rightEye")).findFirst();
			exceptionResult = exceptions.stream().filter(b -> b.getBioAttribute().equalsIgnoreCase("rightEye"))
					.findFirst();

			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_CAPTURED,
					RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
			if (result.isPresent()) {
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_USER_LANG_LABEL,
						applicationLanguageProperties.getString("righteye"));
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_LOCAL_LANG_LABEL,
						getSecondaryLanguageLabel("righteye"));
				// templateValues.put(RegistrationConstants.IRIS_STYLE, "irisWithoutException");
				templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_CAPTURED, RegistrationConstants.EMPTY);

				if (isAckTemplate) {
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
							RegistrationConstants.TEMPLATE_RIGHT_MARK);
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_EYE_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH, response, null);
				} else {
					setPreviewBiometricImage(templateValues, RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_EYE,
							RegistrationConstants.RIGHT_IRIS_IMG_PATH, response,
							getStreamImageBytes(capturedIris.stream()
									.filter(b -> b.getBioAttribute().equalsIgnoreCase("rightEye")).findFirst().get(),
									registration));
				}
			}

			if (exceptionResult.isPresent()) {
				if (isAckTemplate) {
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_USER_LANG_LABEL,
							applicationLanguageProperties.getString("righteye"));
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_LOCAL_LANG_LABEL,
							getSecondaryLanguageLabel("righteye"));
					// templateValues.put(RegistrationConstants.IRIS_STYLE, "irisWithoutException");
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE,
							RegistrationConstants.TEMPLATE_CROSS_MARK);
					templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_CAPTURED, RegistrationConstants.EMPTY);
					setACKBiometricImage(templateValues, RegistrationConstants.TEMPLATE_EYE_IMAGE_SOURCE,
							RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH, response, null);
				}
			}
		}
	}

	private void setPreviewBiometricImage(Map<String, Object> templateValues, String key, String imagePath,
			ResponseDTO response, byte[] streamImage) {
		try {
			String encodedBytes = "";
			if (streamImage != null && streamImage.length > 0) {
				encodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(streamImage, false));
			} else
				encodedBytes = StringUtils.newStringUtf8(Base64
						.encodeBase64(IOUtils.toByteArray(this.getClass().getResourceAsStream(imagePath)), false));

			templateValues.put(key, RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + encodedBytes);
		} catch (IOException ioException) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}
	}

	private void setACKBiometricImage(Map<String, Object> templateValues, String key, String imagePath,
			ResponseDTO response, byte[] streamImage) {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();) {
			if (key.equalsIgnoreCase(RegistrationConstants.TEMPLATE_FACE_IMAGE_SOURCE) && streamImage != null) {
				String encodedBytes = "";
				if (streamImage != null && streamImage.length > 0) {
					encodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(streamImage, false));
				} else
					encodedBytes = StringUtils.newStringUtf8(Base64
							.encodeBase64(IOUtils.toByteArray(this.getClass().getResourceAsStream(imagePath)), false));

				templateValues.put(key, RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + encodedBytes);
			} else {
				BufferedImage eyeImage = ImageIO.read(this.getClass().getResourceAsStream(imagePath));
				ImageIO.write(eyeImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
				byte[] eyeImageBytes = byteArrayOutputStream.toByteArray();
				String eyeImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(eyeImageBytes, false));
				templateValues.put(key, RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + eyeImageEncodedBytes);
			}
		} catch (IOException ioException) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID, ioException.getMessage());
		}
	}

	private String getSecondaryLanguageLabel(String key) {
		ResourceBundle localProperties = ApplicationContext.localLanguageProperty();
		if (!isLocalLanguageAvailable()) {
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
			ResourceBundle applicationLanguageProperties, List<UiSchemaDTO> schemaFields) {

		String platformLanguageCode = ApplicationContext.applicationLanguage();
		String localLanguageCode = ApplicationContext.localLanguage();

		SimpleDateFormat sdf = new SimpleDateFormat(RegistrationConstants.TEMPLATE_DATE_FORMAT);
		String currentDate = sdf.format(new Date());

		// map the respective fields with the values in the registrationDTO
		templateValues.put(RegistrationConstants.TEMPLATE_DATE, currentDate);
		templateValues.put(RegistrationConstants.TEMPLATE_DATE_USER_LANG_LABEL,
				applicationLanguageProperties.getString("date"));
		templateValues.put(RegistrationConstants.TEMPLATE_DATE_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("date"));

		Map<String, Object> defaultDemographics = registration.getDefaultDemographics();
		if (defaultDemographics != null && !defaultDemographics.isEmpty()) {
			templateValues.put(RegistrationConstants.TEMPLATE_APPLICANT_NAME_PRIMARY_LABEL,
					applicationLanguageProperties.getString("applicantname"));
			templateValues.put(RegistrationConstants.TEMPLATE_APPLICANT_NAME_SECONDARY_LABEL,
					getSecondaryLanguageLabel("applicantname"));
			StringBuilder applicantNameAppLanguage = new StringBuilder();
			StringBuilder applicantNameLocalLanguage = new StringBuilder();
			for (Map.Entry<String, Object> entry : defaultDemographics.entrySet()) {
				List<SimpleDto> fullNameValues = (List<SimpleDto>) entry.getValue();
				fullNameValues.forEach(simpleDto -> {
					if (simpleDto.getLanguage().equalsIgnoreCase(platformLanguageCode)) {
						applicantNameAppLanguage.append(simpleDto.getValue() + " ");
					} else if (simpleDto.getLanguage().equalsIgnoreCase(localLanguageCode)) {
						applicantNameLocalLanguage.append(simpleDto.getValue() + " ");
					}
				});
			}
			templateValues.put(RegistrationConstants.TEMPLATE_APPLICANT_NAME_PRIMARY_VALUE,
					applicantNameAppLanguage.toString());
			if (!isLocalLanguageAvailable()) {
				templateValues.put(RegistrationConstants.TEMPLATE_APPLICANT_NAME_SECONDARY_VALUE,
						RegistrationConstants.EMPTY);
			} else {
				templateValues.put(RegistrationConstants.TEMPLATE_APPLICANT_NAME_SECONDARY_VALUE, applicantNameLocalLanguage.toString());
			}
		} else {
			templateValues.put("DisplayName", RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		}

		templateValues.put(RegistrationConstants.TEMPLATE_DEMO_INFO,
				applicationLanguageProperties.getString("demographicInformation"));

		templateValues.put("DemographicInfoSecondary", getSecondaryLanguageLabel("demographicInformation"));

		List<Map<String, Object>> demographicsdata = new ArrayList<Map<String, Object>>();

		for (UiSchemaDTO field : schemaFields) {
			if ("biometricsType".equals(field.getType()) || "documentType".equals(field.getType())
					|| "UIN".equalsIgnoreCase(field.getId()) || "IDSchemaVersion".equalsIgnoreCase(field.getId()))
				continue;

			Map<String, Object> data = new HashMap<String, Object>();
			String value = getValue(registration.getDemographics().get(field.getId()));
			if (value != null || !value.isEmpty() || !"".equals(value)) {
				data.put("primaryLabel", field.getLabel().get("primary"));
				data.put("secondaryLabel", field.getLabel().containsKey("secondary") && isLocalLanguageAvailable() ? field.getLabel().get("secondary")
						: RegistrationConstants.EMPTY);
				data.put("primaryValue", getValueForTemplate(value, platformLanguageCode));
				String secondaryVal = getSecondaryLanguageValue(registration.getDemographics().get(field.getId()),
						localLanguageCode);
				data.put("secondaryValue", secondaryVal != null && !secondaryVal.isEmpty() ? "/" + secondaryVal : secondaryVal);
				demographicsdata.add(data);
			}
		}

		templateValues.put("demographicsdata", demographicsdata);
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

	private void setUpPreviewContent(RegistrationDTO registration, Map<String, Object> templateValues,
			ResponseDTO response, ResourceBundle applicationLanguageProperties) {
		// ByteArrayOutputStream byteArrayOutputStream = null;

		templateValues.put(RegistrationConstants.TEMPLATE_ACKNOWLEDGEMENT,
				RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);

		templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG, RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		if (registration.getPreRegistrationId() != null) {
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_USER_LANG_LABEL,
					applicationLanguageProperties.getString("preRegistrationId"));
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_LOCAL_LANG_LABEL,
					getSecondaryLanguageLabel("preRegistrationId"));
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID, registration.getPreRegistrationId());
		}

		templateValues.put(RegistrationConstants.TEMPLATE_UIN_UPDATE,
				RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		if (registration.getDemographics().get("UIN") != null) {
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_UPDATE, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_USER_LANG_LABEL,
					applicationLanguageProperties.getString("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN, registration.getDemographics().get("UIN"));
		}

		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY, applicationLanguageProperties.getString("modify"));
		templateValues.put(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_SOURCE,
				getEncodedImage(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_PATH, response,
						RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING));

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
			ResponseDTO response, ResourceBundle applicationLanguageProperties) {
		templateValues.put(RegistrationConstants.TEMPLATE_PREVIEW, RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		templateValues.put(RegistrationConstants.TEMPLATE_RID_USER_LANG_LABEL,
				applicationLanguageProperties.getString("registrationid"));
		templateValues.put(RegistrationConstants.TEMPLATE_RID_LOCAL_LANG_LABEL,
				getSecondaryLanguageLabel("registrationid"));
		templateValues.put(RegistrationConstants.TEMPLATE_RID, registration.getRegistrationId());

		templateValues.put(RegistrationConstants.TEMPLATE_UIN_UPDATE,
				RegistrationConstants.TEMPLATE_STYLE_HIDE_PROPERTY);
		templateValues.put(RegistrationConstants.TEMPLATE_HEADER_TABLE, RegistrationConstants.TEMPLATE_HEADER_TABLE);

		if (registration.getDemographics().get("UIN") != null) {
			templateValues.put(RegistrationConstants.TEMPLATE_HEADER_TABLE,
					RegistrationConstants.TEMPLATE_UIN_HEADER_TABLE);
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_UPDATE, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_USER_LANG_LABEL,
					applicationLanguageProperties.getString("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN, registration.getDemographics().get("UIN"));
		}

		// QR Code Generation
		generateQRCode(registration, templateValues, response, applicationLanguageProperties);

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
	 * @param templateText           - string which contains the data of template
	 *                               that is used to generate notification
	 * @param registration           - RegistrationDTO to display required fields on
	 *                               the template
	 * @param templateManagerBuilder - The Builder which generates template by
	 *                               mapping values to respective place-holders in
	 *                               template
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
			values.put(RegistrationConstants.TEMPLATE_RID, registration.getRegistrationId());

			for (String key : registration.getDemographics().keySet()) {
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

			if (null != fieldValue) {
				@SuppressWarnings("unchecked")
				List<SimpleDto> valueList = (List<SimpleDto>) fieldValue;
				value = valueList.get(0).getValue();
			}
		}

		LOGGER.info(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
				"Getting values of demographic fields has been completed");
		return value;
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

	private boolean isLocalLanguageAvailable() {
		String platformLanguageCode = ApplicationContext.applicationLanguage();
		String localLanguageCode = ApplicationContext.localLanguage();
		
		if (localLanguageCode != null && !localLanguageCode.isEmpty()) {
			if (!platformLanguageCode.equalsIgnoreCase(localLanguageCode)) {
				return true;
			}
		}
		
		return false;
	}
}