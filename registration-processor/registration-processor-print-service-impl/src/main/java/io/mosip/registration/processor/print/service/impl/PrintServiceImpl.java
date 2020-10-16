package io.mosip.registration.processor.print.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.pdfgenerator.itext.constant.PDFGeneratorExceptionCodeConstant;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.CardType;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.UinCardType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.TemplateProcessingFailureException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO1;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.packet.dto.vid.VidRequestDto;
import io.mosip.registration.processor.core.packet.dto.vid.VidResponseDTO;
import io.mosip.registration.processor.core.spi.print.service.PrintService;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.spi.uincardgenerator.UinCardGenerator;
import io.mosip.registration.processor.core.util.CbeffToBiometricUtil;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.message.sender.template.TemplateGenerator;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.exception.ParsingException;
import io.mosip.registration.processor.packet.storage.exception.VidCreationException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.print.service.exception.IDRepoResponseNull;
import io.mosip.registration.processor.print.service.exception.PDFSignatureException;
import io.mosip.registration.processor.print.service.exception.UINNotFoundInDatabase;
import io.mosip.registration.processor.print.service.utility.PrintUtility;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class PrintServiceImpl.
 * 
 * @author M1048358 Alok
 * @author Girish Yarru
 */
@Service
public class PrintServiceImpl implements PrintService<Map<String, byte[]>> {

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/** The primary lang. */
	@Value("${mosip.primary-language}")
	private String primaryLang;

	/** The secondary lang. */
	@Value("${mosip.secondary-language}")
	private String secondaryLang;

	/** The un masked length. */
	@Value("${registration.processor.unMaskedUin.length}")
	private int unMaskedLength;

	/** The uin length. */
	@Value("${mosip.kernel.uin.length}")
	private int uinLength;

	@Value("${mosip.print.uin.header.length}")
	private int headerLength;
	/** The Constant UIN_CARD_TEMPLATE. */
	private static final String UIN_CARD_TEMPLATE = "RPR_UIN_CARD_TEMPLATE";

	/** The Constant MASKED_UIN_CARD_TEMPLATE. */
	private static final String MASKED_UIN_CARD_TEMPLATE = "RPR_MASKED_UIN_CARD_TEMPLATE";

	/** The Constant FACE. */
	private static final String FACE = "Face";

	/** The Constant UIN_CARD_PDF. */
	private static final String UIN_CARD_PDF = "uinPdf";

	/** The Constant UIN_TEXT_FILE. */
	private static final String UIN_TEXT_FILE = "textFile";

	/** The Constant APPLICANT_PHOTO. */
	private static final String APPLICANT_PHOTO = "ApplicantPhoto";

	/** The Constant QRCODE. */
	private static final String QRCODE = "QrCode";

	/** The Constant UINCARDPASSWORD. */
	private static final String UINCARDPASSWORD = "mosip.registration.processor.print.service.uincard.password";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PrintServiceImpl.class);

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The template generator. */
	@Autowired
	private TemplateGenerator templateGenerator;

	/** The utilities. */
	@Autowired
	private Utilities utilities;

	/** The uin card generator. */
	@Autowired
	private UinCardGenerator<byte[]> uinCardGenerator;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The qr code generator. */
	@Autowired
	private QrCodeGenerator<QrVersion> qrCodeGenerator;

	/** The Constant INDIVIDUAL_BIOMETRICS. */
	private static final String INDIVIDUAL_BIOMETRICS = "individualBiometrics";

	/** The Constant VID_CREATE_ID. */
	public static final String VID_CREATE_ID = "registration.processor.id.repo.generate";

	/** The Constant REG_PROC_APPLICATION_VERSION. */
	public static final String REG_PROC_APPLICATION_VERSION = "registration.processor.id.repo.vidVersion";

	/** The Constant DATETIME_PATTERN. */
	public static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	private static final String NAME = "name";

	public static final String VID_TYPE = "registration.processor.id.repo.vidType";

	/** The cbeffutil. */
	@Autowired
	private CbeffUtil cbeffutil;

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;

	@Autowired
	private PrintUtility printUtility;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.print.service.PrintService#
	 * getDocuments(io.mosip.registration.processor.core.constant.IdType,
	 * java.lang.String, java.lang.String, boolean)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Map<String, byte[]> getDocuments(IdType idType, String idValue, String cardType,
			boolean isPasswordProtected) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getDocuments()::entry");

		Map<String, byte[]> byteMap = new HashMap<>();
		String uin = null;
		LogDescription description = new LogDescription();
		String vid = null;
		Map<String, Object> attributes = new LinkedHashMap<>();
		boolean isTransactionSuccessful = false;
		IdResponseDTO1 response = null;
		String template = UIN_CARD_TEMPLATE;
		try {
			if (idType.toString().equalsIgnoreCase(IdType.UIN.toString())) {
				uin = idValue;
				response = getIdRepoResponse(idType.toString(), idValue);
			} else if (idType.toString().equalsIgnoreCase(IdType.VID.toString())) {
				vid = idValue;
				uin = utilities.getUinByVid(idValue);
				response = getIdRepoResponse(IdType.UIN.toString(), uin);
			}

			else {
				JSONObject jsonObject = utilities.retrieveUIN(idValue);
				uin = JsonUtil.getJSONValue(jsonObject, IdType.UIN.toString());
				if (uin == null) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), null,
							PlatformErrorMessages.RPR_PRT_UIN_NOT_FOUND_IN_DATABASE.name());
					throw new UINNotFoundInDatabase(PlatformErrorMessages.RPR_PRT_UIN_NOT_FOUND_IN_DATABASE.getCode());
				}

				response = getIdRepoResponse(idType.toString(), idValue);
			}

			boolean isPhotoSet = setApplicantPhoto(response, attributes);
			if (!isPhotoSet) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), uin,
						PlatformErrorMessages.RPR_PRT_APPLICANT_PHOTO_NOT_SET.name());
			}
			String jsonString = new JSONObject((Map) response.getResponse().getIdentity()).toString();
			setTemplateAttributes(jsonString, attributes);
			attributes.put(IdType.UIN.toString(), uin);

			byte[] textFileByte = createTextFile(jsonString);
			byteMap.put(UIN_TEXT_FILE, textFileByte);

			boolean isQRcodeSet = setQrCode(textFileByte, attributes);
			if (!isQRcodeSet) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), uin,
						PlatformErrorMessages.RPR_PRT_QRCODE_NOT_SET.name());
			}

			template = setTemplateForMaskedUIN(cardType, uin, vid, attributes, template);

			// getting template and placing original values
			InputStream uinArtifact = templateGenerator.getTemplate(template, attributes, primaryLang);
			if (uinArtifact == null) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), idType.toString(),
						PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.name());
				throw new TemplateProcessingFailureException(
						PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.getCode());
			}

			String password = null;
			if (isPasswordProtected) {
				password = getPassword(uin);
			}

			// generating pdf
			byte[] pdfbytes = uinCardGenerator.generateUinCard(uinArtifact, UinCardType.PDF, password);
		
			byteMap.put(UIN_CARD_PDF, pdfbytes);

			byte[] uinbyte = attributes.get(IdType.UIN.toString()).toString().getBytes();
			byteMap.put(IdType.UIN.toString(), uinbyte);

			isTransactionSuccessful = true;

		} catch (VidCreationException e) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_VID_CREATION_ERROR.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_VID_CREATION_ERROR.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(), PlatformErrorMessages.RPR_PRT_QRCODE_NOT_GENERATED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(e.getErrorCode(), e.getErrorText());

		}

		catch (QrcodeGenerationException e) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_QR_CODE_GENERATION_ERROR.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_QR_CODE_GENERATION_ERROR.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(),
					PlatformErrorMessages.RPR_PRT_QRCODE_NOT_GENERATED.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getErrorText());

		} catch (UINNotFoundInDatabase e) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_UIN_NOT_FOUND_IN_DATABASE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_UIN_NOT_FOUND_IN_DATABASE.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(),
					PlatformErrorMessages.RPR_PRT_UIN_NOT_FOUND_IN_DATABASE.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getErrorText());

		} catch (TemplateProcessingFailureException e) {
			description.setMessage(PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(),
					PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.name() + ExceptionUtils.getStackTrace(e));
			throw new TemplateProcessingFailureException(PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.getMessage());

		} catch (PDFGeneratorException e) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_NOT_GENERATED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_NOT_GENERATED.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(),
					PlatformErrorMessages.RPR_PRT_PDF_NOT_GENERATED.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getErrorText());

		} catch (PDFSignatureException e) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(),
					PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFSignatureException(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getMessage());

		} catch (ApisResourceAccessException | IOException | ParseException
				| io.mosip.kernel.core.exception.IOException e) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_NOT_GENERATED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_NOT_GENERATED.getCode());

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(),
					PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.name() + ExceptionUtils.getStackTrace(e));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					e.getMessage());

		} catch (Exception ex) {
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idType.toString(), description + ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			throw new PDFGeneratorException(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorCode(),
					ex.getMessage() + ExceptionUtils.getStackTrace(ex));

		} finally {
			String eventId = "";
			String eventName = "";
			String eventType = "";
			if (isTransactionSuccessful) {
				description.setMessage(PlatformSuccessMessages.RPR_PRINT_SERVICE_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_PRINT_SERVICE_SUCCESS.getCode());

				eventId = EventId.RPR_402.toString();
				eventName = EventName.UPDATE.toString();
				eventType = EventType.BUSINESS.toString();
			} else {
				description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getCode());

				eventId = EventId.RPR_405.toString();
				eventName = EventName.EXCEPTION.toString();
				eventType = EventType.SYSTEM.toString();
			}
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_PUM_PACKET_UPLOADER.getCode()
					: description.getCode();
			String moduleName = ModuleName.PRINT_SERVICE.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, uin);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getDocuments()::exit");

		return byteMap;
	}

	private String setTemplateForMaskedUIN(String cardType, String uin, String vid, Map<String, Object> attributes,
			String template) throws ApisResourceAccessException, VidCreationException, IOException {
		if (cardType.equalsIgnoreCase(CardType.MASKED_UIN.toString())) {
			template = MASKED_UIN_CARD_TEMPLATE;
			if (vid == null) {
				vid = getVid(uin);
			}
			attributes.put(IdType.VID.toString(), vid);
			String maskedUin = maskString(uin, uinLength - unMaskedLength, '*');
			attributes.put(IdType.UIN.toString(), maskedUin);
		}
		return template;
	}

	/**
	 * Gets the id repo response.
	 *
	 * @param idType
	 *            the id type
	 * @param idValue
	 *            the id value
	 * @return the id repo response
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	private IdResponseDTO1 getIdRepoResponse(String idType, String idValue) throws ApisResourceAccessException {
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(idValue);
		String queryParamName = "type";
		String queryParamValue = "all";
		IdResponseDTO1 response;
		if (idType.equalsIgnoreCase(IdType.UIN.toString())) {
			response = (IdResponseDTO1) restClientService.getApi(ApiName.IDREPOGETIDBYUIN, pathsegments, queryParamName,
					queryParamValue, IdResponseDTO1.class);
		} else {
			response = (IdResponseDTO1) restClientService.getApi(ApiName.RETRIEVEIDENTITYFROMRID, pathsegments,
					queryParamName, queryParamValue, IdResponseDTO1.class);
		}

		if (response == null || response.getResponse() == null) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					idValue, PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.name());
			throw new IDRepoResponseNull(PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.getCode());
		}

		return response;
	}

	/**
	 * Creates the text file.
	 *
	 * @param jsonString
	 *            the attributes
	 * @return the byte[]
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private byte[] createTextFile(String jsonString) throws IOException {

		LinkedHashMap<String, String> printTextFileMap = new LinkedHashMap<>();
		JSONObject demographicIdentity = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
		if (demographicIdentity == null)
			throw new IdentityNotFoundException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
		String printTextFileJson = Utilities.getJson(utilities.getConfigServerFileStorageURL(),
				utilities.getRegistrationProcessorPrintTextFile());

		JSONObject printTextFileJsonObject = JsonUtil.objectMapperReadValue(printTextFileJson, JSONObject.class);
		Set<String> printTextFileJsonKeys = printTextFileJsonObject.keySet();
		for (String key : printTextFileJsonKeys) {
			String printTextFileJsonString = JsonUtil.getJSONValue(printTextFileJsonObject, key);
			for (String value : printTextFileJsonString.split(",")) {
				Object object = demographicIdentity.get(value);
				if (object instanceof ArrayList) {
					JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
					JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
					for (JsonValue jsonValue : jsonValues) {
						if (jsonValue.getLanguage().equals(primaryLang))
							printTextFileMap.put(value + "_" + primaryLang, jsonValue.getValue());
						if (jsonValue.getLanguage().equals(secondaryLang))
							printTextFileMap.put(value + "_" + secondaryLang, jsonValue.getValue());

					}

				} else if (object instanceof LinkedHashMap) {
					JSONObject json = JsonUtil.getJSONObject(demographicIdentity, value);
					printTextFileMap.put(value, (String) json.get(VALUE));
				} else {
					printTextFileMap.put(value, (String) object);

				}
			}

		}

		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		String printTextFileString = gson.toJson(printTextFileMap);
		return printTextFileString.getBytes();
	}

	/**
	 * Sets the qr code.
	 *
	 * @param textFileByte
	 *            the text file byte
	 * @param attributes
	 *            the attributes
	 * @return true, if successful
	 * @throws QrcodeGenerationException
	 *             the qrcode generation exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean setQrCode(byte[] textFileByte, Map<String, Object> attributes)
			throws QrcodeGenerationException, IOException {
		String qrString = new String(textFileByte);
		boolean isQRCodeSet = false;
		String digitalSignaturedQrData = digitalSignatureUtility.getDigitalSignature(qrString);
		JSONObject textFileJson = JsonUtil.objectMapperReadValue(qrString, JSONObject.class);
		textFileJson.put("digitalSignature", digitalSignaturedQrData);

		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		String printTextFileString = gson.toJson(textFileJson);

		byte[] qrCodeBytes = qrCodeGenerator.generateQrCode(printTextFileString, QrVersion.V30);
		if (qrCodeBytes != null) {
			String imageString = CryptoUtil.encodeBase64String(qrCodeBytes);
			attributes.put(QRCODE, "data:image/png;base64," + imageString);
			isQRCodeSet = true;
		}

		return isQRCodeSet;
	}

	/**
	 * Sets the applicant photo.
	 *
	 * @param response
	 *            the response
	 * @param attributes
	 *            the attributes
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	private boolean setApplicantPhoto(IdResponseDTO1 response, Map<String, Object> attributes) throws Exception {
		String value = null;
		boolean isPhotoSet = false;

		if (response == null || response.getResponse() == null) {
			return Boolean.FALSE;
		}
		if (response.getResponse().getDocuments() != null) {
			List<Documents> documents = response.getResponse().getDocuments();
			for (Documents doc : documents) {
				if (doc.getCategory().equals(INDIVIDUAL_BIOMETRICS)) {
					value = doc.getValue();
					break;
				}
			}
		}
		if (value != null) {
			CbeffToBiometricUtil util = new CbeffToBiometricUtil(cbeffutil);
			List<String> subtype = new ArrayList<>();
			byte[] photoByte = util.getImageBytes(value, FACE, subtype);
			if (photoByte != null) {
				String data = Base64.getEncoder().encodeToString(printUtility.extractFaceImageData(photoByte));
				attributes.put(APPLICANT_PHOTO, "data:image/png;base64," + data);
				isPhotoSet = true;
			}
		}

		return isPhotoSet;
	}

	/**
	 * Gets the artifacts.
	 *
	 * @param idJsonString
	 *            the id json string
	 * @param attribute
	 *            the attribute
	 * @return the artifacts
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	private void setTemplateAttributes(String idJsonString, Map<String, Object> attribute) throws IOException {
		try {
			JSONObject demographicIdentity = JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class);
			if (demographicIdentity == null)
				throw new IdentityNotFoundException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());

			String mapperJsonString = Utilities.getJson(utilities.getConfigServerFileStorageURL(),
					utilities.getGetRegProcessorIdentityJson());
			JSONObject mapperJson = JsonUtil.objectMapperReadValue(mapperJsonString, JSONObject.class);
			JSONObject mapperIdentity = JsonUtil.getJSONObject(mapperJson,
					utilities.getGetRegProcessorDemographicIdentity());

			List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
			for (String key : mapperJsonKeys) {
				LinkedHashMap<String, String> jsonObject = JsonUtil.getJSONValue(mapperIdentity, key);
				String values = jsonObject.get(VALUE);
				for (String value : values.split(",")) {
					Object object = demographicIdentity.get(value);
					if (object instanceof ArrayList) {
						JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
						JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
						for (JsonValue jsonValue : jsonValues) {
							if (jsonValue.getLanguage().equals(primaryLang))
								attribute.put(value + "_" + primaryLang, jsonValue.getValue());
							if (jsonValue.getLanguage().equals(secondaryLang))
								attribute.put(value + "_" + secondaryLang, jsonValue.getValue());

						}

					} else if (object instanceof LinkedHashMap) {
						JSONObject json = JsonUtil.getJSONObject(demographicIdentity, value);
						attribute.put(value, (String) json.get(VALUE));
					} else {
						attribute.put(value, String.valueOf(object));
					}
				}
			}

		} catch (JsonParseException | JsonMappingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					null, "Error while parsing Json file" + ExceptionUtils.getStackTrace(e));
			throw new ParsingException(PlatformErrorMessages.RPR_SYS_JSON_PARSING_EXCEPTION.getMessage(), e);
		}
	}

	/**
	 * Mask string.
	 *
	 * @param uin
	 *            the uin
	 * @param maskLength
	 *            the mask length
	 * @param maskChar
	 *            the mask char
	 * @return the string
	 */
	private String maskString(String uin, int maskLength, char maskChar) {
		if (uin == null || "".equals(uin))
			return "";

		if (maskLength == 0)
			return uin;

		StringBuilder sbMaskString = new StringBuilder(maskLength);

		for (int i = 0; i < maskLength; i++) {
			sbMaskString.append(maskChar);
		}

		return sbMaskString.toString() + uin.substring(0 + maskLength);
	}

	/**
	 * Gets the vid.
	 *
	 * @param uin
	 *            the uin
	 * @return the vid
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws VidCreationException
	 *             the vid creation exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private String getVid(String uin) throws ApisResourceAccessException, VidCreationException, IOException {
		String vid;
		VidRequestDto vidRequestDto = new VidRequestDto();
		RequestWrapper<VidRequestDto> request = new RequestWrapper<>();
		VidResponseDTO vidResponse;
		vidRequestDto.setUIN(uin);
		vidRequestDto.setVidType(env.getProperty(VID_TYPE));
		request.setId(env.getProperty(VID_CREATE_ID));
		request.setRequest(vidRequestDto);
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		request.setRequesttime(localdatetime);
		request.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getVid():: post CREATEVID service call started with request data : "
						+ JsonUtil.objectMapperObjectToJson(vidRequestDto));

		vidResponse = (VidResponseDTO) restClientService.postApi(ApiName.CREATEVID, "", "", request,
				VidResponseDTO.class);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PrintServiceImpl::getVid():: post CREATEVID service call ended successfully");

		if (vidResponse.getErrors() != null && !vidResponse.getErrors().isEmpty()) {
			throw new VidCreationException(PlatformErrorMessages.RPR_PRT_VID_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_PRT_VID_EXCEPTION.getMessage());

		} else {
			vid = vidResponse.getResponse().getVid();
		}

		return vid;
	}

	/**
	 * Gets the password.
	 *
	 * @param uin
	 *            the uin
	 * @return the password
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private String getPassword(String uin) throws ApisResourceAccessException, IOException {
		JSONObject jsonObject = utilities.retrieveIdrepoJson(uin);

		String[] attributes = env.getProperty(UINCARDPASSWORD).split("\\|");
		List<String> list = new ArrayList<>(Arrays.asList(attributes));

		Iterator<String> it = list.iterator();
		String uinCardPd = "";

		while (it.hasNext()) {
			String key = it.next().trim();

			Object object = JsonUtil.getJSONValue(jsonObject, key);
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(jsonObject, key);
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				String value = getParameter(jsonValues, primaryLang);
				if (value != null) {
					uinCardPd = uinCardPd.concat(value);
				}

			} else if (object instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(jsonObject, key);
				uinCardPd = uinCardPd.concat((String) json.get(VALUE));
			} else {
				uinCardPd = uinCardPd.concat((String) object);
			}

		}

		return uinCardPd;
	}

	/**
	 * Gets the parameter.
	 *
	 * @param jsonValues
	 *            the json values
	 * @param langCode
	 *            the lang code
	 * @return the parameter
	 */
	private String getParameter(JsonValue[] jsonValues, String langCode) {

		String parameter = null;
		if (jsonValues != null) {
			for (int count = 0; count < jsonValues.length; count++) {
				String lang = jsonValues[count].getLanguage();
				if (langCode.contains(lang)) {
					parameter = jsonValues[count].getValue();
					break;
				}
			}
		}
		return parameter;
	}



}
