package io.mosip.registration.processor.stages.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.TemplateProcessingFailureException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsRequestDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.message.sender.exception.TemplateNotFoundException;
import io.mosip.registration.processor.message.sender.template.TemplateGenerator;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.stages.dto.MessageSenderDTO;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationAdditionalInfoDTO;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;

@Component
public class NotificationUtility {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(NotificationUtility.class);

	/** The Constant BOTH. */
	public static final String BOTH = "BOTH";

	/** The Constant LINE_SEPARATOR. */
	public static final String LINE_SEPARATOR = "" + '\n' + '\n' + '\n';

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	/** The Constant ENCODING. */
	public static final String ENCODING = "UTF-8";

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The re-register subject. */
	@Value("${registration.processor.reregister.subject}")
	private String reregisterSubject;

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

	/** The resclient. */
	@Autowired
	private RestApiClient resclient;

	private static final String SMS_SERVICE_ID = "mosip.registration.processor.sms.id";
	private static final String REG_PROC_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	@Autowired
	private ObjectMapper mapper;

	public void sendNotification(RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO,
			InternalRegistrationStatusDto registrationStatusDto, SyncRegistrationEntity regEntity,
			String[] allNotificationTypes, boolean isProcessingSuccess)
			throws ApisResourceAccessException, IOException {
		String registrationId = registrationStatusDto.getRegistrationId();
		LogDescription description = new LogDescription();
		String regType = regEntity.getRegistrationType();
		MessageSenderDTO messageSenderDTO = new MessageSenderDTO();
		NotificationTemplateType type = null;
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("RID", registrationId);
		if (registrationAdditionalInfoDTO.getName() != null) {
			attributes.put("name_" + primaryLang, registrationAdditionalInfoDTO.getName());
		} else {
			attributes.put("name_" + primaryLang, "");
		}
		
		if (isProcessingSuccess) {
			type = setNotificationTemplateType(registrationStatusDto, type);
		} else {
			type = NotificationTemplateType.TECHNICAL_ISSUE;
		}
		if (type != null) {
			setTemplateAndSubject(type, regType, messageSenderDTO);
		}

		if (allNotificationTypes != null) {
			for (String notificationType : allNotificationTypes) {
				if (notificationType.equalsIgnoreCase("EMAIL")
						&& (registrationAdditionalInfoDTO.getEmail() != null
						&& !registrationAdditionalInfoDTO.getEmail().isEmpty())) {
					sendEmailNotification(registrationId, registrationAdditionalInfoDTO, messageSenderDTO, attributes, description);
				} else if (notificationType.equalsIgnoreCase("SMS") && (registrationAdditionalInfoDTO.getPhone() != null
						&& !registrationAdditionalInfoDTO.getPhone().isEmpty())) {
					sendSMSNotification(registrationStatusDto.getRegistrationId(),
							registrationAdditionalInfoDTO, messageSenderDTO, attributes, description);
				}
			}
		}
	}

	private void sendSMSNotification(String registrationId, RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO,
			MessageSenderDTO messageSenderDTO, Map<String, Object> attributes, LogDescription description) {
		try {
			SmsResponseDto smsResponse = sendSMS(registrationId, registrationAdditionalInfoDTO,
					messageSenderDTO.getSmsTemplateCode().name(), attributes);

			if (smsResponse.getStatus().equals("success")) {
				description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_SMS_SUCCESS.getMessage());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						description.getCode() + description.getMessage());
			} else {
				description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_SMS_FAILED.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getMessage());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						description.getCode() + description.getMessage());
			}
		} catch (IOException | JSONException | ApisResourceAccessException e) {
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_SMS_FAILED.getCode());
			description.setMessage(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description + e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
	}

	private SmsResponseDto sendSMS(String registrationId, RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO,
								   String templateTypeCode, Map<String, Object> attributes) throws ApisResourceAccessException, IOException, JSONException {
		SmsResponseDto response;
		SmsRequestDto smsDto = new SmsRequestDto();
		RequestWrapper<SmsRequestDto> requestWrapper = new RequestWrapper<>();
		ResponseWrapper<?> responseWrapper;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "NotificationUtility::sendSms()::entry");
		try {
			InputStream in = templateGenerator.getTemplate(templateTypeCode, attributes, primaryLang);
			String artifact = IOUtils.toString(in, ENCODING);

			smsDto.setNumber(registrationAdditionalInfoDTO.getPhone());
			smsDto.setMessage(artifact);

			requestWrapper.setId(env.getProperty(SMS_SERVICE_ID));
			requestWrapper.setVersion(env.getProperty(REG_PROC_APPLICATION_VERSION));
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			requestWrapper.setRequesttime(localdatetime);
			requestWrapper.setRequest(smsDto);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "NotificationUtility::sendSms():: SMSNOTIFIER POST service started with request : "
							+ JsonUtil.objectMapperObjectToJson(requestWrapper));

			responseWrapper = (ResponseWrapper<?>) restClientService.postApi(ApiName.SMSNOTIFIER, "", "",
					requestWrapper, ResponseWrapper.class);
			response = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), SmsResponseDto.class);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
					registrationId, "NotificationUtility::sendSms():: SMSNOTIFIER POST service ended with response : "
							+ JsonUtil.objectMapperObjectToJson(response));
		} catch (TemplateNotFoundException | TemplateProcessingFailureException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new TemplateGenerationFailedException(
					PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.getCode(), e);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new ApisResourceAccessException(PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name(), e);
		}
		return response;
	}

	private void sendEmailNotification(String registrationId, RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO,
			MessageSenderDTO messageSenderDTO, Map<String, Object> attributes, LogDescription description) {
		try {
			String subjectTemplateCode;
			if (messageSenderDTO.getSmsTemplateCode().name()
					.equalsIgnoreCase(NotificationTemplateTypeCode.RPR_TEC_ISSUE_SMS.name())) {
				subjectTemplateCode = reregisterSubject;
			} else {
				subjectTemplateCode = messageSenderDTO.getSubjectTemplateCode().name();
			}
			ResponseDto emailResponse = sendEmail(registrationId, registrationAdditionalInfoDTO,
					messageSenderDTO.getEmailTemplateCode().name(), subjectTemplateCode, attributes);
			if (emailResponse.getStatus().equals("success")) {
				description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_EMAIL_SUCCESS.getMessage());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						description.getCode() + description.getMessage());
			} else {
				description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getMessage());
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						description.getCode() + description.getMessage());
			}
		} catch (Exception e) {
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getMessage());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), description.getCode(), registrationId,
					description + e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
	}

	private ResponseDto sendEmail(String registrationId, RegistrationAdditionalInfoDTO registrationAdditionalInfoDTO, String templateTypeCode,
			String subjectTypeCode, Map<String, Object> attributes) throws Exception {
		ResponseDto response = null;

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "NotificationUtility::sendEmail()::entry");
		try {
			InputStream in = templateGenerator.getTemplate(templateTypeCode, attributes, primaryLang);
			String artifact = IOUtils.toString(in, ENCODING);

			InputStream subjectInputStream = templateGenerator.getTemplate(subjectTypeCode, attributes, primaryLang);
			String subjectArtifact = IOUtils.toString(subjectInputStream, ENCODING);

			String mailTo = registrationAdditionalInfoDTO.getEmail();

			response = sendEmail(mailTo, subjectArtifact, artifact);

		} catch (TemplateNotFoundException | TemplateProcessingFailureException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new TemplateGenerationFailedException(
					PlatformErrorMessages.RPR_SMS_TEMPLATE_GENERATION_FAILURE.getCode(), e);
		} catch (ApisResourceAccessException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			throw new ApisResourceAccessException(PlatformErrorMessages.RPR_PGS_API_RESOURCE_NOT_AVAILABLE.name(), e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
				registrationId, "NotificationUtility::sendEmail()::exit");

		return response;
	}

	private ResponseDto sendEmail(String mailTo, String subjectArtifact, String artifact) throws Exception {
		LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		ResponseWrapper<?> responseWrapper;
		ResponseDto responseDto = null;
		String apiHost = env.getProperty(ApiName.EMAILNOTIFIER.name());
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiHost);

		builder.queryParam("mailTo", mailTo);

		builder.queryParam("mailSubject", subjectArtifact);
		builder.queryParam("mailContent", artifact);

		params.add("attachments", null);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"NotificationUtility::sendEmail():: EMAILNOTIFIER POST service started");

		responseWrapper = (ResponseWrapper<?>) resclient.postApi(builder.build().toUriString(),
				MediaType.MULTIPART_FORM_DATA, params, ResponseWrapper.class);

		responseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), ResponseDto.class);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"NotificationUtility::sendEmail():: EMAILNOTIFIER POST service ended with in response : "
						+ JsonUtil.objectMapperObjectToJson(responseDto));

		return responseDto;
	}

	private NotificationTemplateType setNotificationTemplateType(InternalRegistrationStatusDto registrationStatusDto,
			NotificationTemplateType type) {
		if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.LOST.getValue()))
			type = NotificationTemplateType.LOST_UIN;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.NEW.getValue()))
			type = NotificationTemplateType.NEW_REG;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.UPDATE.getValue()))
			type = NotificationTemplateType.UIN_UPDATE;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.RES_REPRINT.getValue()))
			type = NotificationTemplateType.REPRINT_UIN;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.ACTIVATED.getValue()))
			type = NotificationTemplateType.ACTIVATE;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.DEACTIVATED.getValue()))
			type = NotificationTemplateType.DEACTIVATE;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.RES_UPDATE.getValue()))
			type = NotificationTemplateType.RES_UPDATE;
		return type;
	}

	private void setTemplateAndSubject(NotificationTemplateType templatetype, String regType,
			MessageSenderDTO MessageSenderDTO) {
		switch (templatetype) {
		case NEW_REG:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_RPV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_RPV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_RPV_SUC_EMAIL_SUB);
			break;
		case LOST_UIN:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_LPV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_LPV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_LPV_SUC_EMAIL_SUB);
			break;
		case UIN_UPDATE:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_UPV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_UPV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_UPV_SUC_EMAIL_SUB);
			break;
		case REPRINT_UIN:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_PPV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_PPV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_PPV_SUC_EMAIL_SUB);
			break;
		case ACTIVATE:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_APV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_APV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_APV_SUC_EMAIL_SUB);
			break;
		case DEACTIVATE:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_DPV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_DPV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_DPV_SUC_EMAIL_SUB);
			break;
		case RES_UPDATE:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_RUPV_SUC_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_RUPV_SUC_EMAIL);
			MessageSenderDTO.setSubjectTemplateCode(NotificationSubjectCode.RPR_RUPV_SUC_EMAIL_SUB);
			break;
		case TECHNICAL_ISSUE:
			MessageSenderDTO.setSmsTemplateCode(NotificationTemplateTypeCode.RPR_TEC_ISSUE_SMS);
			MessageSenderDTO.setEmailTemplateCode(NotificationTemplateTypeCode.RPR_TEC_ISSUE_EMAIL);
			break;
		default:
			break;
		}
	}
}
