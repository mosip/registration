package io.mosip.registration.processor.message.sender.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONTokener;
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

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.VidType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.TemplateProcessingFailureException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.idrepo.dto.VidInfoDTO;
import io.mosip.registration.processor.core.idrepo.dto.VidsInfosDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsRequestDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.JsonValue;
import io.mosip.registration.processor.core.spi.message.sender.MessageNotificationService;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.LanguageUtility;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.IDRepoResponseNull;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.message.sender.exception.TemplateNotFoundException;
import io.mosip.registration.processor.message.sender.template.TemplateGenerator;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.storage.exception.IdRepoAppException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.status.code.RegistrationType;
import io.mosip.registration.processor.status.dto.RegistrationAdditionalInfoDTO;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.SyncRegistrationService;

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

	@Value("${mosip.notification.language-type}")
	private String languageType;

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	@Autowired
	private Decryptor decryptor;
	/** The template generator. */
	@Autowired
	private TemplateGenerator templateGenerator;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/** The utility. */
	@Autowired
	private Utilities utility;

	@Autowired
	private LanguageUtility languageUtility;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Value("${mosip.default.template-languages:#{null}}")
	private String defaultTemplateLanguages;

	@Value("${mosip.default.user-preferred-language-attribute:#{null}}")
	private String userPreferredLanguageAttribute;

	@Value("#{${registration.processor.notification.additional-process.category-mapping:{:}}}")
	private Map<String,String> additionalProcessCategoryForNotification;

	/** The resclient. */
	@Autowired
	private RestApiClient resclient;

	private static final String SMS_SERVICE_ID = "mosip.registration.processor.sms.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	@Autowired
	private ObjectMapper mapper;

	private List<String> mapperJsonKeys = null;

	private JSONObject mapperIdentity=null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.message.sender.
	 * MessageNotificationService#sendSmsNotification(java.lang.String,
	 * java.lang.String, io.mosip.registration.processor.core.constant.IdType,
	 * java.util.Map)
	 */
	@Override
	public SmsResponseDto sendSmsNotification(String templateTypeCode, String id, String process, IdType idType,
			Map<String, Object> attributes, String regType) throws ApisResourceAccessException, IOException,
			JSONException, PacketDecryptionFailureException {
		SmsResponseDto response=new SmsResponseDto();
		SmsRequestDto smsDto = new SmsRequestDto();
		RequestWrapper<SmsRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<?> responseWrapper;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
				"MessageNotificationServiceImpl::sendSmsNotification()::entry");
		try {
			List<String> preferredLanguages= getPreferredLanguages(id,process);
			String artifact="";
			for(String lang: preferredLanguages) {
				StringBuilder emailId = new StringBuilder();
				StringBuilder phoneNumber = new StringBuilder();
				Map<String, Object> attributesLang=new HashMap<>(attributes);
				setAttributes(id, process,lang, idType, attributesLang, regType, phoneNumber, emailId);
				InputStream stream = templateGenerator.getTemplate(templateTypeCode, attributesLang, lang);
				if(artifact.isBlank()) {
				 artifact = IOUtils.toString(stream, ENCODING);
				}else {
				artifact = artifact + LINE_SEPARATOR + IOUtils.toString(stream, ENCODING);;
				}
				if (phoneNumber == null || phoneNumber.length() == 0) {
					throw new PhoneNumberNotFoundException(PlatformErrorMessages.RPR_SMS_PHONE_NUMBER_NOT_FOUND.getCode());
				}
				smsDto.setNumber(phoneNumber.toString());
			}

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

		} catch (TemplateNotFoundException | TemplateProcessingFailureException | PacketManagerException | JsonProcessingException  e) {
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
		} catch (JsonParseException | JsonMappingException | io.mosip.kernel.core.exception.IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));


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
	public ResponseDto sendEmailNotification(String templateTypeCode, String id, String process, IdType idType,
			Map<String, Object> attributes, String[] mailCc, String subjectCode, MultipartFile[] attachment, String regType)
			throws Exception {
		ResponseDto response = null;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), id,
				"MessageNotificationServiceImpl::sendEmailNotification()::entry");
		try {
			List<String> preferredLanguages= getPreferredLanguages(id,process);

			String artifact="";
			String subject="";
			for(String lang: preferredLanguages) {
				StringBuilder emailId = new StringBuilder();
				StringBuilder phoneNumber = new StringBuilder();
				Map<String, Object> attributesLang=new HashMap<>(attributes);
				setAttributes(id, process,lang, idType, attributesLang, regType, phoneNumber, emailId);
				InputStream stream = templateGenerator.getTemplate(templateTypeCode, attributesLang, lang);

				artifact = IOUtils.toString(stream, ENCODING);

				InputStream subStream = templateGenerator.getTemplate(subjectCode, attributesLang, lang);

				subject=IOUtils.toString(subStream, ENCODING);
				if (emailId == null || emailId.length() == 0) {
					throw new EmailIdNotFoundException(PlatformErrorMessages.RPR_EML_EMAILID_NOT_FOUND.getCode());
				}
				String[] mailTo = { emailId.toString() };

				response = sendEmail(mailTo, mailCc, subject, artifact, attachment);
			}



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

	private List<String> getPreferredLanguages(String id, String process) throws ApisResourceAccessException,
	PacketManagerException, JsonProcessingException, IOException {
		if(userPreferredLanguageAttribute!=null && !userPreferredLanguageAttribute.isBlank()) {
			try {
				String preferredLang=packetManagerService.getField(id, userPreferredLanguageAttribute, process,
						ProviderStageName.MESSAGE_SENDER);
				if(preferredLang!=null && !preferredLang.isBlank()) {
					List<String> codes=new ArrayList<>();
					for(String lang:preferredLang.split(",")) {
						String langCode=languageUtility.getLangCodeFromNativeName(lang);
						if(langCode!=null &&!langCode.isBlank())
							codes.add(langCode);
					}
					if(!codes.isEmpty())return codes;
				}
			}catch(ApisResourceAccessException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
						id, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
								+ ExceptionUtils.getStackTrace(e));
			}

		}
		if(defaultTemplateLanguages!=null && !defaultTemplateLanguages.isBlank()) {
			return List.of(defaultTemplateLanguages.split(","));
		}
		Map<String,String> idValuesMap=packetManagerService.getAllFieldsByMappingJsonKeys(id, process,
						ProviderStageName.MESSAGE_SENDER);
		List<String> idValues=new ArrayList<>();
		for(Entry<String, String> entry: idValuesMap.entrySet()) {
		    if(entry.getValue()!=null && !entry.getValue().isBlank()) {
		       	idValues.add(entry.getValue());
		    }
		}
		Set<String> langSet=new HashSet<>();
		for( String idValue:idValues) {
			if(idValue!=null&& !idValue.isBlank()  ) {
				if(isJSONArrayValid(idValue)) {
					JSONArray array=mapper.readValue(idValue, JSONArray.class);
					for(Object obj:array) {
						JSONObject json= new JSONObject( (Map) obj);
						langSet.add( (String) json.get("language"));
					}
				}
			}
		}
		return new ArrayList<>(langSet);
	}



	public boolean isJSONArrayValid(String jsonArrayString) {
	        try {
	            new org.json.JSONArray(jsonArrayString);
	        } catch (JSONException ex) {
	            return false;
	        }
	    return true;
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
			params.add("mailTo", item);
		}

		if (mailCc != null) {
			for (String item : mailCc) {
				params.add("mailCc", item);
			}
		}

		params.add("mailSubject", subject);
		params.add("mailContent", artifact);

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
	 * @param lang
	 * @param idType     the id type
	 * @param attributes the attributes
	 * @param regType    the reg typesetAttributes
	 * @return the template json
	 * @throws IOException                           Signals that an I/O exception
	 *                                               has occurred.
	 * @throws ApisResourceAccessException
	 * @throws                                       io.mosip.kernel.core.exception.IOException
	 * @throws io.mosip.kernel.core.exception.IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws PacketDecryptionFailureException
	 * @throws RegistrationProcessorCheckedException
	 * @throws IdRepoAppException
	 */
	private Map<String, Object> setAttributes(String id, String process, String lang, IdType idType, Map<String, Object> attributes, String regType,
			StringBuilder phoneNumber, StringBuilder emailId) throws IOException, ApisResourceAccessException,
			JsonProcessingException, PacketManagerException, JSONException, PacketDecryptionFailureException, JsonParseException, JsonMappingException, io.mosip.kernel.core.exception.IOException {

		String uin = "";
		if (idType.toString().equalsIgnoreCase(UIN)) {
			JSONObject jsonObject = utility.idrepoRetrieveIdentityByRid(id);
			uin = JsonUtil.getJSONValue(jsonObject, UIN);
			attributes.put("RID", id);
			attributes.put("UIN", uin);
			attributes.put("VID", getVid(uin));
		} else {
			attributes.put("RID", id);
		}
		String internalProcess = utility.getInternalProcess(additionalProcessCategoryForNotification, regType);


		if (idType.toString().equalsIgnoreCase(UIN) && (regType.equalsIgnoreCase(RegistrationType.ACTIVATED.name())
				|| regType.equalsIgnoreCase(RegistrationType.DEACTIVATED.name())
				|| regType.equalsIgnoreCase(RegistrationType.UPDATE.name())
				|| regType.equalsIgnoreCase(RegistrationType.RES_UPDATE.name())
				|| regType.equalsIgnoreCase(RegistrationType.LOST.name())
				|| internalProcess.equalsIgnoreCase(RegistrationType.UPDATE.name()))) {
			setAttributesFromIdRepo(uin, attributes, regType,lang, phoneNumber, emailId);
		} else {
			setAttributesFromIdJson(id, process, attributes, regType,lang, phoneNumber, emailId);
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
	 * @param lang
	 * @return the map
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, Object> setAttributesFromIdRepo(String uin, Map<String, Object> attributes, String regType,
			String lang,StringBuilder phoneNumber, StringBuilder emailId) throws IOException {
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(uin);
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
						LoggerFileConstant.REGISTRATIONID.toString(), uin,
						PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.name());
				throw new IDRepoResponseNull(PlatformErrorMessages.RPR_PRT_IDREPO_RESPONSE_NULL.getCode());
			}

			String jsonString = new JSONObject((Map) response.getResponse().getIdentity()).toString();
			setAttributes(jsonString, attributes, regType,lang, phoneNumber, emailId);

		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					uin,
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
	 * @param lang
	 * @return the keysand values
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> setAttributes(String idJsonString, Map<String, Object> attribute, String regType,
			String lang, StringBuilder phoneNumber, StringBuilder emailId) throws IOException {
		JSONObject demographicIdentity = null;
			demographicIdentity = JsonUtil.objectMapperReadValue(idJsonString, JSONObject.class);

        if(mapperJsonKeys==null) {
        	String mapperJsonString = Utilities.getJson(utility.getConfigServerFileStorageURL(),
    				utility.getGetRegProcessorIdentityJson());
        	JSONObject mapperJson = JsonUtil.objectMapperReadValue(mapperJsonString, JSONObject.class);
		    mapperIdentity = JsonUtil.getJSONObject(mapperJson, utility.getGetRegProcessorDemographicIdentity());
		   mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
        }
		getMappedIdentity(attribute, lang, demographicIdentity);

		setEmailAndPhone(demographicIdentity, phoneNumber, emailId);

		return attribute;
	}

	private void getMappedIdentity(Map<String, Object> attribute, String lang, JSONObject demographicIdentity) throws com.fasterxml.jackson.core.JsonProcessingException {
		for (String key : mapperJsonKeys) {
			JSONObject jsonValue = JsonUtil.getJSONObject(mapperIdentity, key);
			if (jsonValue.get(VALUE) != null && !jsonValue.get(VALUE).toString().isBlank()) {
				String[] valueArray = jsonValue.get(VALUE).toString().split(",");
				for (String val : valueArray) {
					Object object = JsonUtil.getJSONValue(demographicIdentity, val);
					if (object instanceof ArrayList) {
						List jsonList = new ArrayList<>();
						JSONArray node = JsonUtil.getJSONArray(demographicIdentity, val);
						for (int i = 0; i < node.size(); i++) {
							Object obj = node.get(i);
							if (obj instanceof String) {
								jsonList.add(obj);
							} else {
								JsonValue jsonValueObj = JsonUtil.mapJsonNodeToJavaObject(JsonValue.class,  new JSONObject((Map) obj));
								if (jsonValueObj.getLanguage().equalsIgnoreCase(lang)) {
									attribute.put(val + "_" + lang, jsonValueObj.getValue());
								}
							}
						}
						if (!jsonList.isEmpty()) {
							attribute.putIfAbsent(val, jsonList);
						}
					} else if (object instanceof LinkedHashMap) {
						JSONObject json = JsonUtil.getJSONObject(demographicIdentity, val);
						attribute.put(val, json.get(VALUE));
					} else {
						attribute.put(val, object);
					}
				}
			}
		}
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

		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String email = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL),MappingJsonConstants.VALUE);
		String phone = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE),MappingJsonConstants.VALUE);

		String emailValue = JsonUtil.getJSONValue(demographicIdentity, email);
		String phoneNumberValue = JsonUtil.getJSONValue(demographicIdentity, phone);
		if (emailValue != null) {
			emailId.append(emailValue);
		}
		if (phoneNumberValue != null) {
			phoneNumber.append(phoneNumberValue);
		}

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> setAttributesFromIdJson(String id, String process, Map<String, Object> attribute,
			String regType, String lang, StringBuilder phoneNumber, StringBuilder emailId)
			throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException, JSONException, PacketDecryptionFailureException, JsonParseException, JsonMappingException, io.mosip.kernel.core.exception.IOException {

		if (mapperJsonKeys == null) {
			String mapperJsonString = Utilities.getJson(utility.getConfigServerFileStorageURL(),
					utility.getGetRegProcessorIdentityJson());
			JSONObject mapperJson = JsonUtil.objectMapperReadValue(mapperJsonString, JSONObject.class);
			mapperIdentity = JsonUtil.getJSONObject(mapperJson, utility.getGetRegProcessorDemographicIdentity());
			mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
		}
		List<String> mapperJsonValues = new ArrayList<>();
		for (String key : mapperJsonKeys) {
			JSONObject jsonValue = JsonUtil.getJSONObject(mapperIdentity, key);
			if (jsonValue.get(VALUE) != null && !jsonValue.get(VALUE).toString().isBlank()) {
				String[] valueArray = jsonValue.get(VALUE).toString().split(",");
				mapperJsonValues.addAll(new ArrayList(Arrays.asList(valueArray)));
			}
		}
		Map<String, String> fieldMap =null;
		try {
		 fieldMap = packetManagerService.getFields(id, mapperJsonValues, process, ProviderStageName.MESSAGE_SENDER);
		}catch(ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		}
		if(fieldMap!=null) {
			getMappedAttribute(attribute, lang, fieldMap);
			JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
			String email = JsonUtil.getJSONValue(
					JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.EMAIL),
					MappingJsonConstants.VALUE);
			String phone = JsonUtil.getJSONValue(
					JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.PHONE),
					MappingJsonConstants.VALUE);
			String emailValue = fieldMap.get(email);
			String phoneNumberValue = fieldMap.get(phone);
			if (emailValue != null) {
				emailId.append(emailValue);
			}
			if (phoneNumberValue != null) {
				phoneNumber.append(phoneNumberValue);
			}
			}
			else {
				attribute=setAttributesFromSync(id, process, attribute, regType, lang, phoneNumber, emailId);
			}
			return attribute;
		}

	private void getMappedAttribute(Map<String, Object> attribute, String lang, Map<String, String> fieldMap) throws JSONException, com.fasterxml.jackson.core.JsonProcessingException {
		for (Entry e : fieldMap.entrySet()) {
			if (e.getValue() != null) {
				String value = e.getValue().toString();
				if (StringUtils.isNotEmpty(value)) {
					Object json = new JSONTokener(value).nextValue();
					if (json instanceof org.json.JSONObject) {
						HashMap<String, Object> hashMap = mapper.readValue(value, HashMap.class);
						attribute.putIfAbsent(e.getKey().toString(), hashMap.get(VALUE));
					}
					else if (json instanceof org.json.JSONArray) {
						org.json.JSONArray jsonArray = new org.json.JSONArray(value);
						List jsonList = new ArrayList<>();
						for (int i = 0; i < jsonArray.length(); i++) {
							Object obj = jsonArray.get(i);
							if (obj instanceof String){
								jsonList.add(obj);
							}
							else {
								JsonValue jsonValue = mapper.readValue(obj.toString(), JsonValue.class);
								if (jsonValue.getLanguage().equalsIgnoreCase(lang)) {
									attribute.putIfAbsent(e.getKey().toString() + "_" + lang, jsonValue.getValue());
								}
							}
						}
						if (!jsonList.isEmpty()) {
							attribute.putIfAbsent(e.getKey().toString(), jsonList);
						}
					} else
						attribute.putIfAbsent(e.getKey().toString(), value);
				} else
					attribute.put(e.getKey().toString(), e.getValue());
			}

		}
	}

	private Map<String, Object> setAttributesFromSync(String id, String process, Map<String, Object> attribute,
				String regType, String lang, StringBuilder phoneNumber, StringBuilder emailId) throws PacketDecryptionFailureException, ApisResourceAccessException, IOException, JsonParseException, JsonMappingException, io.mosip.kernel.core.exception.IOException {
			SyncRegistrationEntity regEntity = syncRegistrationService.findByRegistrationId(id).get(0);
			if (regEntity.getOptionalValues() != null) {
				InputStream inputStream = new ByteArrayInputStream(regEntity.getOptionalValues());
				InputStream decryptedInputStream = decryptor.decrypt(
						id,
						utility.getRefId(id, regEntity.getReferenceId()),
						inputStream);
				String decryptedData = IOUtils.toString(decryptedInputStream, "UTF-8");
				RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO = (RegistrationAdditionalInfoDTO) JsonUtils
						.jsonStringToJavaObject(RegistrationAdditionalInfoDTO.class, decryptedData);
				JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		        String nameField = JsonUtil.getJSONValue(
		                JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.NAME),
		                MappingJsonConstants.VALUE);
				attribute.put(nameField, registrationAdditionalInfoDTO.getName());
				if (registrationAdditionalInfoDTO.getEmail() != null) {
					emailId.append(registrationAdditionalInfoDTO.getEmail());
				}
				if (registrationAdditionalInfoDTO.getPhone() != null) {
					phoneNumber.append(registrationAdditionalInfoDTO.getPhone());
				}
			}
			return attribute;

		}

	private String getVid(String uin) throws ApisResourceAccessException {
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(uin);
		String vid = null;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MessageNotificationServiceImpl::getVid():: get GETVIDSBYUIN service call started with request data : "
		);

		VidsInfosDTO vidsInfosDTO =  (VidsInfosDTO) restClientService.getApi(ApiName.GETVIDSBYUIN,
				pathsegments, "", "", VidsInfosDTO.class);

		if (vidsInfosDTO != null) {
			if (vidsInfosDTO.getErrors() != null && !vidsInfosDTO.getErrors().isEmpty()) {
				ServiceError error = vidsInfosDTO.getErrors().get(0);
				throw new BaseUncheckedException(PlatformErrorMessages.RPR_PRT_VID_NOT_AVAILABLE_EXCEPTION.getCode(),
						error.getMessage());

			} else {
				if(vidsInfosDTO.getResponse()!=null && !vidsInfosDTO.getResponse().isEmpty()) {
					for (VidInfoDTO VidInfoDTO : vidsInfosDTO.getResponse()) {
						if (VidType.PERPETUAL.name().equalsIgnoreCase(VidInfoDTO.getVidType())) {
							vid = VidInfoDTO.getVid();
							break;
						}
					}
				}
			}
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
				LoggerFileConstant.REGISTRATIONID.toString(), "",
				"MessageNotificationServiceImpl::getVid():: get GETVIDSBYUIN service call ended");

		return vid;
	}
	}
