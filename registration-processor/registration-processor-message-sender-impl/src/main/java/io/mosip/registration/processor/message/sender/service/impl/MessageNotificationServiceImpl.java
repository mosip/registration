package io.mosip.registration.processor.message.sender.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.TemplateProcessingFailureException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsRequestDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.spi.message.sender.MessageNotificationService;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.IDRepoResponseNull;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.message.sender.exception.TemplateNotFoundException;
import io.mosip.registration.processor.message.sender.template.TemplateGenerator;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.status.code.RegistrationType;

/**
 * ServiceImpl class for sending notification.
 * 
 * @author Alok Ranjan
 * 
 * @since 1.0.0
 *
 */
@Service
public class MessageNotificationServiceImpl
		implements MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> {

    /** The Constant BOTH. */
    public static final String BOTH = "BOTH";

    /** The Constant LINE_SEPARATOR. */
    public static final String LINE_SEPARATOR = "" + '\n' + '\n' + '\n';

    /** The Constant VALUE. */
	private static final String VALUE = "value";

	/** The Constant UIN. */
	private static final String UIN = "UIN";

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	/** The Constant ENCODING. */
	public static final String ENCODING = "UTF-8";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(MessageNotificationServiceImpl.class);

	/** The primary language. */
	@Value("${mosip.primary-language}")
	private String primaryLang;

	@Value("${mosip.secondary-language}")
	private String secondaryLang;

	@Value("${mosip.notification.language-type}")
	private String languageType;

	/** The env. */
	@Autowired
	private Environment env;


	/** The template generator. */
	@Autowired
	private TemplateGenerator templateGenerator;

	/** The utility. */
	@Autowired
	private Utilities utility;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The resclient. */
	@Autowired
	private RestApiClient resclient;

	private static final String SMS_SERVICE_ID = "mosip.registration.processor.sms.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	@Autowired
	private ObjectMapper mapper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.message.sender.
	 * MessageNotificationService#sendSmsNotification(java.lang.String,
	 * java.lang.String, io.mosip.registration.processor.core.constant.IdType,
	 * java.util.Map)
	 */
	@Override
	public SmsResponseDto sendSmsNotification(String templateTypeCode, String id, IdType idType,
			Map<String, Object> attributes, String regType) throws ApisResourceAccessException, IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException,
			RegistrationProcessorCheckedException {
		SmsResponseDto response;
		SmsRequestDto smsDto = new SmsRequestDto();
		RequestWrapper<SmsRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<?> responseWrapper;

		StringBuilder emailId = new StringBuilder();
		StringBuilder phoneNumber = new StringBuilder();

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
				"MessageNotificationServiceImpl::sendSmsNotification()::entry");
		try {
			setAttributes(id, idType, attributes, regType, phoneNumber, emailId);
			InputStream in = templateGenerator.getTemplate(templateTypeCode, attributes, primaryLang);
			String artifact = IOUtils.toString(in, ENCODING);
			if(languageType.equalsIgnoreCase(BOTH)){
				InputStream secondaryStream = templateGenerator.getTemplate(templateTypeCode, attributes, secondaryLang);
				String secondaryArtifact = IOUtils.toString(secondaryStream, ENCODING);
				artifact = artifact + LINE_SEPARATOR + secondaryArtifact;
			}

			if (phoneNumber == null || phoneNumber.length() == 0) {
				throw new PhoneNumberNotFoundException(PlatformErrorMessages.RPR_SMS_PHONE_NUMBER_NOT_FOUND.getCode());
			}
			smsDto.setNumber(phoneNumber.toString());
			smsDto.setMessage(artifact);

			requestWrapper.setId(env.getProperty(SMS_SERVICE_ID));
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setRequest(smsDto);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
					"MessageNotificationServiceImpl::sendSmsNotification():: SMSNOTIFIER POST service started with request : "
							+ JsonUtil.objectMapperObjectToJson(requestWrapper));

			responseWrapper = (ResponseWrapper<?>) restClientService.postApi(ApiName.SMSNOTIFIER, "", "",
					requestWrapper, ResponseWrapper.class);
			response = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), SmsResponseDto.class);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
					"MessageNotificationServiceImpl::sendSmsNotification():: SMSNOTIFIER POST service ended with response : "
							+ JsonUtil.objectMapperObjectToJson(response));

		} catch (TemplateNotFoundException | TemplateProcessingFailureException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new TemplateGenerationFailedException(
					PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.getCode(), e);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new ApisResourceAccessException(PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name(), e);
		}

		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.message.sender.
	 * MessageNotificationService#sendEmailNotification(java.lang.String,
	 * java.lang.String, io.mosip.registration.processor.core.constant.IdType,
	 * java.util.Map, java.lang.String[], java.lang.String, java.lang.Object)
	 */
	@Override
	public ResponseDto sendEmailNotification(String templateTypeCode, String id, IdType idType,
			Map<String, Object> attributes, String[] mailCc, String subject, MultipartFile[] attachment, String regType)
			throws Exception {
		ResponseDto response = null;
		StringBuilder emailId = new StringBuilder();
		StringBuilder phoneNumber = new StringBuilder();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
				"MessageNotificationServiceImpl::sendEmailNotification()::entry");
		try {
			setAttributes(id, idType, attributes, regType, phoneNumber, emailId);

			InputStream in = templateGenerator.getTemplate(templateTypeCode, attributes, primaryLang);
			String artifact = IOUtils.toString(in, ENCODING);
			if(languageType.equalsIgnoreCase(BOTH)){
				InputStream secondaryStream = templateGenerator.getTemplate(templateTypeCode, attributes, secondaryLang);
				String secondaryArtifact = IOUtils.toString(secondaryStream, ENCODING);
				artifact = artifact + LINE_SEPARATOR + secondaryArtifact;
			}

			if (emailId == null || emailId.length() == 0) {
				throw new EmailIdNotFoundException(PlatformErrorMessages.RPR_EML_EMAILID_NOT_FOUND.getCode());
			}
			String[] mailTo = { emailId.toString() };

			response = sendEmail(mailTo, mailCc, subject, artifact, attachment);

		} catch (TemplateNotFoundException | TemplateProcessingFailureException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new TemplateGenerationFailedException(
					PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.getCode(), e);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new ApisResourceAccessException(PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name(), e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
				"MessageNotificationServiceImpl::sendEmailNotification()::exit");

		return response;
	}

	/**
	 * Send email.
	 *
	 * @param mailTo
	 *            the mail to
	 * @param mailCc
	 *            the mail cc
	 * @param subject
	 *            the subject
	 * @param artifact
	 *            the artifact
	 * @param attachment
	 *            the attachment
	 * @return the response dto
	 * @throws Exception
	 *             the exception
	 */
	private ResponseDto sendEmail(String[] mailTo, String[] mailCc, String subject, String artifact,
			MultipartFile[] attachment) throws Exception {
		LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		ResponseWrapper<?> responseWrapper;
		ResponseDto responseDto = null;
		String apiHost = env.getProperty(ApiName.EMAILNOTIFIER.name());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiHost);

		for (String item : mailTo) {
			builder.queryParam("mailTo", item);
		}

		if (mailCc != null) {
			for (String item : mailCc) {
				builder.queryParam("mailCc", item);
			}
		}

		builder.queryParam("mailSubject", subject);
		builder.queryParam("mailContent", artifact);

		params.add("attachments", attachment);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"MessageNotificationServiceImpl::sendEmail():: EMAILNOTIFIER POST service started");

		responseWrapper = (ResponseWrapper<?>) resclient.postApi(builder.build().toUriString(),
				MediaType.MULTIPART_FORM_DATA, params, ResponseWrapper.class);

		responseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), ResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"MessageNotificationServiceImpl::sendEmail():: EMAILNOTIFIER POST service ended with in response : "
						+ JsonUtil.objectMapperObjectToJson(responseDto));

		return responseDto;
	}

	/**
	 * Gets the template json.
	 *
	 * @param id         the id
	 * @param idType     the id type
	 * @param attributes the attributes
	 * @param regType    the reg type
	 * @return the template json
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws ApisResourceAccessException
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws PacketDecryptionFailureException
	 * @throws                                       io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 * @throws IdRepoAppException
	 */
	private Map<String, Object> setAttributes(String id, IdType idType, Map<String, Object> attributes, String regType,
			StringBuilder phoneNumber, StringBuilder emailId) throws IOException, ApisResourceAccessException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException,
			RegistrationProcessorCheckedException,
			io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException {

		Long uin = 0l;
		if (idType.toString().equalsIgnoreCase(UIN)) {
			JSONObject jsonObject = utility.retrieveUIN(id);
			uin = JsonUtil.getJSONValue(jsonObject, UIN);
			attributes.put("RID", id);
			attributes.put("UIN", uin);
		} else {
			attributes.put("RID", id);
		}



		if (regType.equalsIgnoreCase(RegistrationType.ACTIVATED.name())
				|| regType.equalsIgnoreCase(RegistrationType.DEACTIVATED.name())
				|| regType.equalsIgnoreCase(RegistrationType.UPDATE.name())
				|| regType.equalsIgnoreCase(RegistrationType.RES_UPDATE.name())
				||regType.equalsIgnoreCase(RegistrationType.LOST.name())) {
			setAttributesFromIdRepo(uin, attributes, regType, phoneNumber, emailId);
		} else {
			setAttributesFromIdJson(id, attributes, regType, phoneNumber, emailId);
		}

		return attributes;
	}

	/**
	 * Sets the attributes from id repo.
	 *
	 * @param uin
	 *            the uin
	 * @param attributes
	 *            the attributes
	 * @param regType
	 *            the reg type
	 * @return the map
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, Object> setAttributesFromIdRepo(Long uin, Map<String, Object> attributes, String regType,
			StringBuilder phoneNumber, StringBuilder emailId) throws IOException {
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(uin.toString());
		IdResponseDTO response;
		try {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"MessageNotificationServiceImpl::setAttributesFromIdRepo():: IDREPOGETIDBYUIN GET service Started ");

			response = (IdResponseDTO) restClientService.getApi(ApiName.IDREPOGETIDBYUIN, pathsegments, "", "",
					IdResponseDTO.class);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"MessageNotificationServiceImpl::setAttributesFromIdRepo():: IDREPOGETIDBYUIN GET service ended successfully");

			if (response == null || response.getResponse() == null) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), uin.toString(),
						PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.name());
				throw new IDRepoResponseNull(PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.getCode());
			}

			String jsonString = new JSONObject((Map) response.getResponse().getIdentity()).toString();
			setAttributes(jsonString, attributes, regType, phoneNumber, emailId);

		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					Long.toString(uin),
					PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.name() + ExceptionUtils.getStackTrace(e));
			throw new IDRepoResponseNull(PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.getCode());
		}

		return attributes;
	}

	/**
	 * Gets the keysand values.
	 *
	 * @param idJsonString
	 *            the id json string
	 * @param attribute
	 *            the attribute
	 * @param regType
	 *            the reg type
	 * @return the keysand values
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> setAttributes(String idJsonString, Map<String, Object> attribute, String regType,
			StringBuilder phoneNumber, StringBuilder emailId) throws IOException {
		JSONObject demographicIdentity = null;

			demographicIdentity = JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class);


		String mapperJsonString = Utilities.getJson(utility.getConfigServerFileStorageURL(),
				utility.getGetRegProcessorIdentityJson());
		JSONObject mapperJson = JsonUtil.objectMapperReadValue(mapperJsonString, JSONObject.class);
		JSONObject mapperIdentity = JsonUtil.getJSONObject(mapperJson, utility.getGetRegProcessorDemographicIdentity());

		List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
		for (String key : mapperJsonKeys) {
			JSONObject jsonValue = JsonUtil.getJSONObject(mapperIdentity, key);
			Object object = JsonUtil.getJSONValue(demographicIdentity, (String) jsonValue.get(VALUE));
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(demographicIdentity, (String) jsonValue.get(VALUE));
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				for (int count = 0; count < jsonValues.length; count++) {
					String lang = jsonValues[count].getLanguage();
					attribute.put(key + "_" + lang, jsonValues[count].getValue());
				}
			} else if (object instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(demographicIdentity, (String) jsonValue.get(VALUE));
				attribute.put(key, json.get(VALUE));
			} else {
				attribute.put(key, object);
			}
		}

		setEmailAndPhone(demographicIdentity, phoneNumber, emailId);

		return attribute;
	}

	/**
	 * Sets the email and phone.
	 *
	 * @param demographicIdentity
	 *            the new email and phone
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void setEmailAndPhone(JSONObject demographicIdentity, StringBuilder phoneNumber, StringBuilder emailId)
			throws IOException {

		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorIdentityJson();
		String email = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL),MappingJsonConstants.VALUE);
		String phone = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE),MappingJsonConstants.VALUE);

		emailId.append(JsonUtil.getJSONValue(demographicIdentity, email).toString());
		phoneNumber.append(JsonUtil.getJSONValue(demographicIdentity, phone).toString());

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> setAttributesFromIdJson(String id, Map<String, Object> attribute,
			String regType, StringBuilder phoneNumber, StringBuilder emailId)
			throws IOException, RegistrationProcessorCheckedException, PacketDecryptionFailureException,
			ApisResourceAccessException, io.mosip.kernel.core.exception.IOException,
			io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException {

		String mapperJsonString = Utilities.getJson(utility.getConfigServerFileStorageURL(),
				utility.getGetRegProcessorIdentityJson());
		JSONObject mapperJson = JsonUtil.objectMapperReadValue(mapperJsonString, JSONObject.class);
		JSONObject mapperIdentity = JsonUtil.getJSONObject(mapperJson, utility.getGetRegProcessorDemographicIdentity());

		List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
		for (String key : mapperJsonKeys) {
			JSONObject jsonValue = JsonUtil.getJSONObject(mapperIdentity, key);
			String value = (String) jsonValue.get(VALUE);
			Object object = JsonUtil.getJSONValue(utility.getDemographicIdentityJSONObject(id,
					value),
					value);
			if (object instanceof ArrayList) {
				JSONArray node = JsonUtil.getJSONArray(utility.getDemographicIdentityJSONObject(id, value), value);
				;
				JsonValue[] jsonValues = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class, node);
				for (int count = 0; count < jsonValues.length; count++) {
					String lang = jsonValues[count].getLanguage();
					attribute.put(key + "_" + lang, jsonValues[count].getValue());
				}
			} else if (object instanceof LinkedHashMap) {
				JSONObject json = JsonUtil.getJSONObject(utility.getDemographicIdentityJSONObject(id, value), value);
				attribute.put(key, json.get(VALUE));
			} else {
				attribute.put(key, object);
			}
		}
		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorIdentityJson();
		String email = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL),
				MappingJsonConstants.VALUE);
		String phone = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE),
				MappingJsonConstants.VALUE);

		emailId.append(JsonUtil.getJSONValue(utility.getDemographicIdentityJSONObject(id, email), email).toString());
		phoneNumber
				.append(JsonUtil.getJSONValue(utility.getDemographicIdentityJSONObject(id, phone), phone).toString());


		return attribute;
	}
}