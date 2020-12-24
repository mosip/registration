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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
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

	public ResponseDTO generateTemplate(String templateText, RegistrationDTO registration, TemplateManagerBuilder
			templateManagerBuilder, String templateType) throws RegBaseCheckedException {
		ResponseDTO response = new ResponseDTO();

		try {
			LOGGER.info(LOG_TEMPLATE_GENERATOR, RegistrationConstants.APPLICATION_NAME,	RegistrationConstants.APPLICATION_ID,
					"generateTemplate had been called for preparing Acknowledgement Template.");

			Map<String, Object> templateValues = new WeakHashMap<>();
			boolean isPrevTemplate = templateType.equals(RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE) ? false : true;
			ResourceBundle applicationLanguageProperties = ApplicationContext.applicationLanguageBundle();
			InputStream is = new ByteArrayInputStream(templateText.getBytes(StandardCharsets.UTF_8));
			List<UiSchemaDTO> schemaFields = getSchemaFields(registration.getIdSchemaVersion());

			//Basic values
			setBasicDetails(templateValues, registration, isPrevTemplate, applicationLanguageProperties, response);

			Map<String, Map<String, Object>> demographicsData = new HashMap<>();
			Map<String, Map<String, Object>> documentsData = new HashMap<>();
			Map<String, Map<String, Object>> biometricsData = new HashMap<>();

			for (UiSchemaDTO field : schemaFields) {
				switch (field.getType()) {
					case "documentType":
						Map<String, Object> doc_data = getDocumentData(registration, field, templateValues);
						if(doc_data != null) { documentsData.put(field.getId(), doc_data); }
						break;

					case "biometricsType":
						Map<String, Object> bio_data = getBiometericData(registration, field, isPrevTemplate, templateValues,
								applicationLanguageProperties);
						if(bio_data != null) { biometricsData.put(field.getId(), bio_data); }
						break;

					default:
						Map<String, Object> demo_data = getDemographicData(registration, field);
						if(demo_data != null) { demographicsData.put(field.getId(), demo_data); }
						break;
				}
			}
			templateValues.put("demographics", demographicsData);
			templateValues.put("documents", documentsData);
			templateValues.put("biometrics", biometricsData);

			LOGGER.debug(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					"merge method of TemplateManager had been called for preparing Acknowledgement Template.");
			Writer writer = new StringWriter();
			TemplateManager templateManager = templateManagerBuilder.build();
			InputStream inputStream = templateManager.merge(is, templateValues);
			IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
			LOGGER.debug(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					"generateTemplate method has been ended for preparing Acknowledgement Template.");

			Map<String, Object> responseMap = new WeakHashMap<>();
			responseMap.put(RegistrationConstants.TEMPLATE_NAME, writer);
			setSuccessResponse(response, RegistrationConstants.SUCCESS, responseMap);

		} catch (RuntimeException | IOException runtimeException) {
			setErrorResponse(response, RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, null);
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return response;
	}

	private Map<String, Object> getBiometericData(RegistrationDTO registration, UiSchemaDTO field, boolean isPrevTemplate,
												  Map<String, Object> templateValues, ResourceBundle applicationLanguageProperties)
			throws RegBaseCheckedException {
		List<BiometricsDto> capturedList = new ArrayList<>();
		for (String attribute : field.getBioAttributes()) {
			String key = String.format("%s_%s", field.getSubType(), attribute);
			if (registration.getBiometrics().containsKey(key))
				capturedList.add(registration.getBiometrics().get(key));
			else if (registration.getBiometricExceptions().containsKey(key)) {
				BiometricsDto dto = new BiometricsDto(attribute, null, 0);
				dto.setModalityName(Biometric.getModalityNameByAttribute(attribute));
				capturedList.add(dto);
			}
		}

		if(capturedList.isEmpty()) { return null; }

		Map<String, Object> bio_data = new HashMap<>();
		List<BiometricsDto> capturedFingers = capturedList.stream()
				.filter(d -> d.getModalityName().toLowerCase().contains("finger")).collect(Collectors.toList());
		List<BiometricsDto> capturedIris = capturedList.stream()
				.filter(d -> d.getModalityName().toLowerCase().contains("iris")).collect(Collectors.toList());
		List<BiometricsDto> capturedFace = capturedList.stream()
				.filter(d -> d.getModalityName().toLowerCase().contains("face")).collect(Collectors.toList());

		bio_data.put("FingerCount", capturedFingers.size());
		bio_data.put("IrisCount", capturedIris.size());
		bio_data.put("FaceCount", capturedFace.size());
		bio_data.put("subType", field.getSubType());
		bio_data.put("primaryLabel", field.getLabel().get("primary"));
		bio_data.put("secondaryLabel", field.getLabel().get("secondary"));

		Optional<BiometricsDto> result = capturedIris.stream()
				.filter(b -> b.getBioAttribute().equalsIgnoreCase("leftEye")).findFirst();
		if (result.isPresent()) {
			BiometricsDto biometricsDto = result.get();
			bio_data.put(RegistrationConstants.TEMPLATE_LEFT_EYE, (biometricsDto.getAttributeISO() != null) ?
					RegistrationConstants.TEMPLATE_RIGHT_MARK : RegistrationConstants.TEMPLATE_CROSS_MARK);
			setBiometricImage(bio_data, RegistrationConstants.TEMPLATE_CAPTURED_LEFT_EYE,
					isPrevTemplate ? null : RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH,
					isPrevTemplate ? getStreamImageBytes(biometricsDto, registration) : null);
		}

		result = capturedIris.stream()
				.filter(b -> b.getBioAttribute().equalsIgnoreCase("rightEye")).findFirst();
		if (result.isPresent()) {
			BiometricsDto biometricsDto = result.get();
			bio_data.put(RegistrationConstants.TEMPLATE_RIGHT_EYE, (biometricsDto.getAttributeISO() != null) ?
					RegistrationConstants.TEMPLATE_RIGHT_MARK : RegistrationConstants.TEMPLATE_CROSS_MARK);
			setBiometricImage(bio_data, RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_EYE,
					isPrevTemplate ? null : RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH,
					isPrevTemplate ? getStreamImageBytes(biometricsDto, registration) : null);
		}

		List<BiometricsDto> resultList = capturedFingers.stream().filter(b -> b.getModalityName().equalsIgnoreCase("FINGERPRINT_SLAB_LEFT"))
				.collect(Collectors.toList());
		if(!resultList.isEmpty()) {
			setFingerRankings(resultList, Biometric.getDefaultAttributes("FINGERPRINT_SLAB_LEFT"), bio_data);
			setBiometricImage(bio_data, RegistrationConstants.TEMPLATE_CAPTURED_LEFT_SLAP,
					isPrevTemplate ? null : RegistrationConstants.LEFTPALM_IMG_PATH,
					isPrevTemplate ? getStreamImageBytes(resultList, registration) : null);
		}

		resultList = capturedFingers.stream().filter(b -> b.getModalityName().equalsIgnoreCase("FINGERPRINT_SLAB_RIGHT"))
				.collect(Collectors.toList());
		if(!resultList.isEmpty()) {
			setFingerRankings(resultList, Biometric.getDefaultAttributes("FINGERPRINT_SLAB_RIGHT"), bio_data);
			setBiometricImage(bio_data, RegistrationConstants.TEMPLATE_CAPTURED_RIGHT_SLAP,
					isPrevTemplate ? null : RegistrationConstants.RIGHTPALM_IMG_PATH,
					isPrevTemplate ? getStreamImageBytes(resultList, registration) : null);
		}

		resultList = capturedFingers.stream().filter(b -> b.getModalityName().toLowerCase().contains("thumb"))
				.collect(Collectors.toList());
		if(!resultList.isEmpty()) {
			setFingerRankings(resultList, Biometric.getDefaultAttributes("FINGERPRINT_SLAB_THUMBS"), bio_data);
			setBiometricImage(bio_data, RegistrationConstants.TEMPLATE_CAPTURED_THUMBS,
					isPrevTemplate ? null : RegistrationConstants.THUMB_IMG_PATH,
					isPrevTemplate ? getStreamImageBytes(resultList, registration) : null);
		}

		if(!capturedFace.isEmpty()) {
			setBiometricImage(bio_data, RegistrationConstants.TEMPLATE_FACE_IMAGE_SOURCE,
					isPrevTemplate ? null : RegistrationConstants.FACE_IMG_PATH,
					isPrevTemplate ? getStreamImageBytes(capturedFace, registration) : null);

			if("applicant".equalsIgnoreCase(capturedFace.get(0).getSubType())) {
				setBiometricImage(templateValues, RegistrationConstants.TEMPLATE_APPLICANT_IMAGE_SOURCE,
						RegistrationConstants.FACE_IMG_PATH,  getStreamImageBytes(capturedFace.get(0), registration));
			}
		}
		return bio_data;
	}

	private void setFingerRankings(List<BiometricsDto> capturedFingers,	List<String> fingers, Map<String, Object> data) {
		Map<String, Double> sortedvalues = capturedFingers.stream()
				.filter(b -> fingers.contains(b.getBioAttribute()) && b.getAttributeISO() != null)
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
			if (result.isPresent()) {
				data.put(finger, result.get().getAttributeISO() == null ? RegistrationConstants.TEMPLATE_CROSS_MARK :
						rankings.get(finger));
			}
		}
	}

	private Map<String, Object> getDocumentData(RegistrationDTO registration, UiSchemaDTO field,
												Map<String, Object> templateValues) {
		Map<String, Object> data = null;
		if(registration.getDocuments().get(field.getId()) != null) {
			data = new HashMap<>();
			data.put("primaryLabel", field.getLabel().get("primary"));
			data.put("secondaryLabel", field.getLabel().get("secondary"));
			data.put("category", registration.getDocuments().get(field.getId()).getCategory());
			data.put("value", registration.getDocuments().get(field.getId()).getValue());
			data.put("format", registration.getDocuments().get(field.getId()).getFormat());
			data.put("refNumber", registration.getDocuments().get(field.getId()).getRefNumber());

			if("POE".equalsIgnoreCase(field.getSubType())) {
				templateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_IMAGE_SOURCE, RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING +
						StringUtils.newStringUtf8(Base64.encodeBase64(registration.getDocuments().get(field.getId()).getDocument(), false)));
			}
		}
		return data;
	}

	private Map<String, Object> getDemographicData(RegistrationDTO registration, UiSchemaDTO field) {
		Map<String, Object> data = null;
		if("UIN".equalsIgnoreCase(field.getId()) || "IDSchemaVersion".equalsIgnoreCase(field.getId()))
			return null;

		String value = getValue(registration.getDemographics().get(field.getId()));
		if (value != null && !value.isEmpty()) {
			data = new HashMap<>();
			data.put("primaryLabel", field.getLabel().get("primary"));
			data.put("secondaryLabel", field.getLabel().get("secondary"));
			data.put("primaryValue", getValueForTemplate(value));
			data.put("secondaryValue", getSecondaryLanguageValue(registration.getDemographics().get(field.getId())));
		}
		return data;
	}

	private void setBasicDetails(Map<String, Object> templateValues, RegistrationDTO registration, boolean isPrevTemplate,
								 ResourceBundle applicationLanguageProperties, ResponseDTO responseDTO) {
		try {
			templateValues.put("isPreview", isPrevTemplate);
			templateValues.put("IDSchemaVersion", registration.getIdSchemaVersion());
			templateValues.put("secLangPresent", isLocalLanguageAvailable());
			templateValues.put(RegistrationConstants.TEMPLATE_RID_USER_LANG_LABEL, applicationLanguageProperties.getString("registrationid"));
			templateValues.put(RegistrationConstants.TEMPLATE_RID_LOCAL_LANG_LABEL,	getSecondaryLanguageLabel("registrationid"));
			templateValues.put(RegistrationConstants.TEMPLATE_RID, registration.getRegistrationId());
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_USER_LANG_LABEL, applicationLanguageProperties.getString("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("uin"));
			templateValues.put(RegistrationConstants.TEMPLATE_UIN, registration.getDemographics().get("UIN"));
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_USER_LANG_LABEL, applicationLanguageProperties.getString("preRegistrationId"));
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("preRegistrationId"));
			templateValues.put(RegistrationConstants.TEMPLATE_PRE_REG_ID, registration.getPreRegistrationId());
			templateValues.put(RegistrationConstants.TEMPLATE_MODIFY, applicationLanguageProperties.getString("modify"));
			templateValues.put(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_SOURCE, getEncodedImage(RegistrationConstants.TEMPLATE_MODIFY_IMAGE_PATH,
					RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING));
			generateQRCode(registration, templateValues, applicationLanguageProperties);
			setUpImportantGuidelines(templateValues, guidelines);
			templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_HEADING, applicationLanguageProperties.getString("consentHeading"));
			templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_DATA, consentText);
			templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_YES, applicationLanguageProperties.getString("yes"));
			templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_NO, applicationLanguageProperties.getString("no"));
			if (registration.getRegistrationMetaDataDTO().getConsentOfApplicant() != null) {
				String consent = registration.getRegistrationMetaDataDTO().getConsentOfApplicant();
				if (consent.equalsIgnoreCase(RegistrationConstants.YES)) {
					templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_SELECTED_YES, RegistrationConstants.TEMPLATE_CONSENT_CHECKED);
				} else if (consent.equalsIgnoreCase(RegistrationConstants.NO)) {
					templateValues.put(RegistrationConstants.TEMPLATE_CONSENT_SELECTED_NO, RegistrationConstants.TEMPLATE_CONSENT_CHECKED);
				}
			}
			LocalDateTime currentTime = OffsetDateTime.now().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			templateValues.put(RegistrationConstants.TEMPLATE_DATE, currentTime.format(DateTimeFormatter.ofPattern(RegistrationConstants.TEMPLATE_DATE_FORMAT)));
			templateValues.put(RegistrationConstants.TEMPLATE_DATE_USER_LANG_LABEL,	applicationLanguageProperties.getString("date"));
			templateValues.put(RegistrationConstants.TEMPLATE_DATE_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("date"));

			templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME_USER_LANG_LABEL, applicationLanguageProperties.getString("ro_name"));
			templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("ro_name"));
			templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME, getValue(registration.getOsiDataDTO().getOperatorID()));
			templateValues.put(RegistrationConstants.TEMPLATE_RO_NAME_LOCAL_LANG, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER_USER_LANG_LABEL, applicationLanguageProperties.getString("registrationcenter"));
			templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("registrationcenter"));
			templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER, SessionContext.userContext().getRegistrationCenterDetailDTO().getRegistrationCenterName());
			templateValues.put(RegistrationConstants.TEMPLATE_REG_CENTER_LOCAL_LANG, RegistrationConstants.EMPTY);
			templateValues.put(RegistrationConstants.TEMPLATE_IMPORTANT_GUIDELINES, applicationLanguageProperties.getString("importantguidelines"));

			templateValues.put(RegistrationConstants.TEMPLATE_DEMO_INFO, applicationLanguageProperties.getString("demographicInformation"));
			templateValues.put("DemographicInfoSecondary", getSecondaryLanguageLabel("demographicInformation"));
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS_USER_LANG_LABEL, applicationLanguageProperties.getString("documents"));
			templateValues.put(RegistrationConstants.TEMPLATE_DOCUMENTS_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("documents"));
			templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_USER_LANG_LABEL, applicationLanguageProperties.getString("biometricsHeading"));
			templateValues.put(RegistrationConstants.TEMPLATE_BIOMETRICS_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("biometricsHeading"));
			templateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_PHOTO_USER_LANG_LABEL, applicationLanguageProperties.getString("exceptionphoto"));
			templateValues.put(RegistrationConstants.TEMPLATE_EXCEPTION_PHOTO_LOCAL_LANG_LABEL,	getSecondaryLanguageLabel("exceptionphoto"));
			templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_USER_LANG, applicationLanguageProperties.getString("individualphoto"));
			templateValues.put(RegistrationConstants.TEMPLATE_PHOTO_LOCAL_LANG, getSecondaryLanguageLabel("individualphoto"));

			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_USER_LANG_LABEL, applicationLanguageProperties.getString("lefteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_EYE_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("lefteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_USER_LANG_LABEL, applicationLanguageProperties.getString("righteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_EYE_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("righteye"));
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_USER_LANG_LABEL, applicationLanguageProperties.getString("lefthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_LEFT_PALM_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("lefthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_USER_LANG_LABEL, applicationLanguageProperties.getString("righthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_RIGHT_PALM_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("righthandpalm"));
			templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_USER_LANG_LABEL, applicationLanguageProperties.getString("thumbs"));
			templateValues.put(RegistrationConstants.TEMPLATE_THUMBS_LOCAL_LANG_LABEL, getSecondaryLanguageLabel("thumbs"));
			templateValues.put("FacePrimLabel",	applicationLanguageProperties.getString("FACE"));
			templateValues.put("FaceSecLabel", getSecondaryLanguageLabel("FACE"));

		} catch (RegBaseCheckedException ex) {
			setErrorResponse(responseDTO, ex.getMessage(), null);
		}
	}

	private void setUpImportantGuidelines(Map<String, Object> templateValues, String guidelines) {
		String[] importantGuidelines = guidelines!=null ?
				guidelines.split(RegistrationConstants.SPLIT_DELIMITOR) : new String[]{};
		templateValues.put(RegistrationConstants.TEMPLATE_GUIDELINES, Arrays.asList(importantGuidelines));
	}

	private byte[] getStreamImageBytes(List<BiometricsDto> biometricsDtos, RegistrationDTO registration) {
		Optional<BiometricsDto> biometricsDto = biometricsDtos.stream().filter(b-> b.getAttributeISO() != null).findFirst();
		if(biometricsDto.isPresent()) {
			return registration.streamImages.get(String.format("%s_%s_%s", biometricsDto.get().getSubType(),
					biometricsDto.get().getModalityName(), biometricsDto.get().getNumOfRetries()));
		}
		return null;
	}

	private byte[] getStreamImageBytes(BiometricsDto biometricsDto, RegistrationDTO registration) {
		return registration.streamImages.get(String.format("%s_%s_%s", biometricsDto.getSubType(),
				biometricsDto.getModalityName(), biometricsDto.getNumOfRetries()));
	}


	private void setBiometricImage(Map<String, Object> templateValues, String key, String imagePath, byte[] streamImage)
			throws RegBaseCheckedException {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();) {
			if (streamImage != null && streamImage.length > 0) {
				String encodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(streamImage, false));
				templateValues.put(key, RegistrationConstants.TEMPLATE_JPG_IMAGE_ENCODING + encodedBytes);
			} else if(imagePath != null) {
				BufferedImage eyeImage = ImageIO.read(this.getClass().getResourceAsStream(imagePath));
				ImageIO.write(eyeImage, RegistrationConstants.IMAGE_FORMAT_PNG, byteArrayOutputStream);
				byte[] eyeImageBytes = byteArrayOutputStream.toByteArray();
				String eyeImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(eyeImageBytes, false));
				templateValues.put(key, RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + eyeImageEncodedBytes);
			}
		} catch (IOException ioException) {
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID, ioException.getMessage());
			throw new RegBaseCheckedException(RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, ioException.getMessage());
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

	private String getEncodedImage(String imagePath, String encoding) throws RegBaseCheckedException {
		try {
			byte[] bytes = IOUtils.toByteArray(this.getClass().getResourceAsStream(imagePath));
			return encoding + StringUtils.newStringUtf8(Base64.encodeBase64(bytes, false));
		} catch (IOException ioException) {
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			throw new RegBaseCheckedException(RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, ioException.getMessage());
		}
	}

	private void generateQRCode(RegistrationDTO registration, Map<String, Object> templateValues,
			ResourceBundle applicationLanguageProperties) throws RegBaseCheckedException {
		try {
			StringBuilder qrCodeString = new StringBuilder();
			qrCodeString.append(applicationLanguageProperties.getString("registrationid")).append(" : ").append("\n")
					.append(registration.getRegistrationId());
			byte[] qrCodeInBytes = qrCodeGenerator.generateQrCode(qrCodeString.toString(), QrVersion.V4);
			String qrCodeImageEncodedBytes = StringUtils.newStringUtf8(Base64.encodeBase64(qrCodeInBytes, false));
			templateValues.put(RegistrationConstants.TEMPLATE_QRCODE_SOURCE,
					RegistrationConstants.TEMPLATE_PNG_IMAGE_ENCODING + qrCodeImageEncodedBytes);
		} catch (QrcodeGenerationException | IOException exception) {
			LOGGER.error(LOG_TEMPLATE_GENERATOR, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(exception));
			throw  new RegBaseCheckedException(RegistrationConstants.TEMPLATE_GENERATOR_ACK_RECEIPT_EXCEPTION, exception.getMessage());
		}
	}


	@SuppressWarnings("unchecked")
	private String getValueForTemplate(Object fieldValue) {
		String lang = ApplicationContext.applicationLanguage();
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
		String value = RegistrationConstants.EMPTY;

		if (fieldValue instanceof List<?>) {
			Optional<SimpleDto> demoValueInRequiredLang = ((List<SimpleDto>) fieldValue).stream()
					.filter(valueDTO -> valueDTO.getLanguage().equals(lang)).findFirst();

			if (demoValueInRequiredLang.isPresent() && demoValueInRequiredLang.get().getValue() != null) {
				value = demoValueInRequiredLang.get().getValue();
			}
		}
		return value;
	}

	private String getSecondaryLanguageValue(Object fieldValue) {
		String lang = ApplicationContext.localLanguage();
		String value = RegistrationConstants.EMPTY;
		if (!ApplicationContext.applicationLanguage().equalsIgnoreCase(lang)) {
			value = getValue(fieldValue, lang);
		}
		return value;
	}

	private String getValue(Object fieldValue) {
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