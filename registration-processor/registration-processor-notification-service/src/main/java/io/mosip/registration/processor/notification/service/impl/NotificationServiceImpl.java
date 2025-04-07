package io.mosip.registration.processor.notification.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.JSONException;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.TemplateResponseDto;
import io.mosip.registration.processor.core.spi.message.sender.MessageNotificationService;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.message.sender.exception.ConfigurationNotFoundException;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.message.sender.exception.TemplateNotFoundException;
import io.mosip.registration.processor.message.sender.utility.MessageSenderStatusMessage;
import io.mosip.registration.processor.message.sender.utility.NotificationTemplateType;
import io.mosip.registration.processor.notification.constants.NotificationTypeEnum;
import io.mosip.registration.processor.notification.constants.ResultCode;
import io.mosip.registration.processor.notification.dto.MessageSenderDto;
import io.mosip.registration.processor.notification.service.NotificationService;
import io.mosip.registration.processor.notification.util.StatusNotificationTypeMapUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationType;

/**
 * The contains business logic for selecting template based on metadata and sending notification 
 *
 * @since 1.0.0
 */
@RefreshScope
@Service
public class NotificationServiceImpl implements NotificationService {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(NotificationServiceImpl.class);
	private static final String NOTIFICATION_TEMPLATE_CODE="regproc.notification.template.code.";
	private static final String EMAIL="email";
	private static final String SMS="sms";
	private static final String SUB="sub";
	private static final String LOST_UIN=NOTIFICATION_TEMPLATE_CODE+"lost.uin.";
	private static final String UIN_CREATED=NOTIFICATION_TEMPLATE_CODE+"uin.created.";
	private static final String UIN_NEW=NOTIFICATION_TEMPLATE_CODE+"uin.new.";
	private static final String UIN_ACTIVATE=NOTIFICATION_TEMPLATE_CODE+"uin.activate.";
	private static final String UIN_DEACTIVATE=NOTIFICATION_TEMPLATE_CODE+"uin.deactivate.";
	private static final String UIN_UPDATE=NOTIFICATION_TEMPLATE_CODE+"uin.update.";
	private static final String DUPLICATE_UIN=NOTIFICATION_TEMPLATE_CODE+"duplicate.uin.";
	private static final String TECHNICAL_ISSUE=NOTIFICATION_TEMPLATE_CODE+"technical.issue.";
	private static final String PAUSED_FOR_ADDITIONAL_INFO=NOTIFICATION_TEMPLATE_CODE+"paused.for.additional.info.";
	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	Utilities utilities;

	/** The notification emails. */
	@Value("${registration.processor.notification.emails}")
	private String notificationEmails;

	@Value("${mosip.registration.processor.notification.types}")
	private String notificationTypes;
	
	@Value("${websub.hub.url}")
	private String hubURL;
	
	@Value("${registration.processor.notification_service_subscriber_secret}")
	private String secret;
	
	@Value("${mosip.regproc.workflow.complete.topic}")
	private String topic;
	
	@Value("${registration.processor.notification_service_subscriber_callback_url}")
	private String callbackURL;

	@Value("${registration.processor.notification_service_pausedforadditonalinfo_subscriber_secret}")
	private String pausedForAdditonalInfoSecret;

	@Value("${mosip.regproc.workflow.pausedforadditionalinfo.topic}")
	private String pausedForAdditonalInfoTopic;

	@Value("${registration.processor.notification_service_pausedforadditonalinfo_subscriber_callback_url}")
	private String pausedForAdditonalInfoCallbackURL;


	@Value("#{${registration.processor.notification.additional-process.category-mapping:{:}}}")
	private Map<String,String> additionalProcessCategoryForNotification;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The service. */
	@Autowired
	private MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> service;
	
	@Autowired
	private SubscriptionClient<SubscriptionChangeRequest,UnsubscriptionRequest, SubscriptionChangeResponse> sb;

	@Autowired
	private Environment env;

	// sends init subscribe req to hub
	@Scheduled(fixedDelayString = "${mosip.regproc.websub.resubscription.delay.millisecs:43200000}",
            initialDelayString = "${mosip.regproc.websub.subscriptions-delay-on-startup.millisecs:300000}")
	protected void init() {
		SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
		subscriptionRequest.setCallbackURL(callbackURL);
		subscriptionRequest.setHubURL(hubURL);
		subscriptionRequest.setSecret(secret);
		subscriptionRequest.setTopic(topic);
		sb.subscribe(subscriptionRequest);
		SubscriptionChangeRequest subscriptionRequestPausedForAdditionalInfo = new SubscriptionChangeRequest();
		subscriptionRequestPausedForAdditionalInfo.setCallbackURL(pausedForAdditonalInfoCallbackURL);
		subscriptionRequestPausedForAdditionalInfo.setHubURL(hubURL);
		subscriptionRequestPausedForAdditionalInfo.setSecret(pausedForAdditonalInfoSecret);
		subscriptionRequestPausedForAdditionalInfo.setTopic(pausedForAdditonalInfoTopic);
		sb.subscribe(subscriptionRequestPausedForAdditionalInfo);
	}

	@Override
	public ResponseEntity<Void> process(@RequestBody WorkflowCompletedEventDTO object) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		ResponseEntity<Void> responseEntity = null;
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		MessageSenderDto messageSenderDto = new MessageSenderDto();
		String id = object.getInstanceId();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"MessageSenderStage::process()::entry");
	
		try {

			String resultCode = object.getResultCode();
			String workflowType = object.getWorkflowType();

			NotificationTemplateType type = null;
			StatusNotificationTypeMapUtil map = new StatusNotificationTypeMapUtil();

			if (resultCode.equals(ResultCode.PROCESSED.toString())) {
				type = setNotificationTemplateType(workflowType);
			} else {
				type = map.getTemplateType(object.getErrorCode());
			}
			if (NotificationTemplateType.DUPLICATE_UIN.equals(type)
					&& workflowType.equalsIgnoreCase(RegistrationType.LOST.toString())) {
				isTransactionSuccessful = false;
				description.setStatusComment(StatusUtil.NOTIFICATION_FAILED_FOR_LOST.getMessage());
				description.setSubStatusCode(StatusUtil.NOTIFICATION_FAILED_FOR_LOST.getCode());
				description.setMessage(PlatformErrorMessages.RPR_NOTIFICATION_FAILED_FOR_LOST.getMessage());
				description.setCode(PlatformErrorMessages.RPR_NOTIFICATION_FAILED_FOR_LOST.getCode());
			} else {
				if (type != null) {
					setTemplateAndSubject(type, workflowType, messageSenderDto);
				}

				Map<String, Object> attributes = new HashMap<>();
				String[] ccEMailList = null;

				if (isNotificationTypesEmpty()) {
					description.setStatusComment(StatusUtil.TEMPLATE_CONFIGURATION_NOT_FOUND.getMessage());
					description.setSubStatusCode(StatusUtil.TEMPLATE_CONFIGURATION_NOT_FOUND.getCode());
					description.setMessage(PlatformErrorMessages.RPR_TEMPLATE_CONFIGURATION_NOT_FOUND.getMessage());
					description.setCode(PlatformErrorMessages.RPR_TEMPLATE_CONFIGURATION_NOT_FOUND.getCode());
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), object.getInstanceId(),
							PlatformErrorMessages.RPR_TEM_CONFIGURATION_NOT_FOUND.getMessage());
					throw new ConfigurationNotFoundException(
							PlatformErrorMessages.RPR_TEM_CONFIGURATION_NOT_FOUND.getCode());
				}
				String[] allNotificationTypes = notificationTypes.split("\\|");

				if (isNotificationEmailsEmpty()) {
					ccEMailList = notificationEmails.split("\\|");
				}

				 isTransactionSuccessful = sendNotification(id, workflowType,
						attributes, ccEMailList, allNotificationTypes, workflowType, messageSenderDto, description);

			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, "MessageSenderStage::success");
		} catch (EmailIdNotFoundException | PhoneNumberNotFoundException | TemplateGenerationFailedException |

				ConfigurationNotFoundException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, e.getMessage() + ExceptionUtils.getStackTrace(e));
			description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage() + e.getMessage()));
			description.setSubStatusCode(StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			description.setMessage(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage());
			description.setCode(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			
			responseEntity = new ResponseEntity<Void>(HttpStatus.OK);
		} catch (TemplateNotFoundException tnf) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, tnf.getMessage() + ExceptionUtils.getStackTrace(tnf));
			description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage() + tnf.getMessage()));
			description.setSubStatusCode(StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			description.setMessage(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage());
			description.setCode(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			responseEntity = new ResponseEntity<Void>(HttpStatus.OK);
		} catch (Exception ex) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			description.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			description.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getCode());
			responseEntity = new ResponseEntity<Void>(HttpStatus.OK);
		} finally {
			responseEntity =  new ResponseEntity<Void>(HttpStatus.OK);
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String  eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.MESSAGE_SENDER.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, id);
		}

		return responseEntity;

	}

	private NotificationTemplateType setNotificationTemplateType(String regtype){
		NotificationTemplateType type=null;
		String internalProcess= utilities.getInternalProcess(additionalProcessCategoryForNotification, regtype);
		if (regtype.equalsIgnoreCase(RegistrationType.LOST.toString()))
			type = NotificationTemplateType.LOST_UIN;
        else if (regtype.equalsIgnoreCase(RegistrationType.NEW.toString())||
				internalProcess.equalsIgnoreCase(RegistrationType.NEW.toString()))
			type = NotificationTemplateType.UIN_CREATED;
		else if (regtype.equalsIgnoreCase(RegistrationType.UPDATE.toString())
				|| regtype.equalsIgnoreCase(RegistrationType.RES_UPDATE.toString())
				||internalProcess.equalsIgnoreCase(RegistrationType.UPDATE.toString()))
			type = NotificationTemplateType.UIN_UPDATE;
		else if (regtype.equalsIgnoreCase(RegistrationType.ACTIVATED.toString()))
			type = NotificationTemplateType.UIN_UPDATE;
		else if (regtype.equalsIgnoreCase(RegistrationType.DEACTIVATED.toString()))
			type = NotificationTemplateType.UIN_UPDATE;
		return type;
	}

	private boolean isNotificationEmailsEmpty() {
		return notificationEmails != null && notificationEmails.length() > 0;
	}

	private boolean isNotificationTypesEmpty() {
		return notificationTypes == null || notificationTypes.isEmpty();
	}

	/**
	 * Send notification.
	 *
	 * @param id                   the id
	 * @param attributes           the attributes
	 * @param ccEMailList          the cc E mail list
	 * @param allNotificationTypes the all notification types
	 * @param regType
	 * @param messageSenderDto
	 * @param description
	 * @throws Exception the exception
	 */
	private boolean sendNotification(String id, String process, Map<String, Object> attributes, String[] ccEMailList,
			String[] allNotificationTypes, String regType, MessageSenderDto messageSenderDto,
			LogDescription description) throws Exception {
		boolean isNotificationSuccess = false;
		boolean isSMSSuccess = false, isEmailSuccess = false;
		// if notification is set as none then dont send notification
		if (allNotificationTypes != null && allNotificationTypes.length == 1
				&& allNotificationTypes[0].equalsIgnoreCase(NotificationTypeEnum.NONE.name())) {
			isNotificationSuccess = true;
			description.setMessage(StatusUtil.MESSAGE_SENDER_NOT_CONFIGURED.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode());
			description.setStatusComment(StatusUtil.MESSAGE_SENDER_NOT_CONFIGURED.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_NOT_CONFIGURED.getCode());
			return isNotificationSuccess;
		}
		if (allNotificationTypes != null) {
			for (String notificationType : allNotificationTypes) {
				if (notificationType.equalsIgnoreCase(NotificationTypeEnum.SMS.name())
						&& isTemplateAvailable(messageSenderDto)) {
					isSMSSuccess = sendSms(id, process, attributes, regType, messageSenderDto, description);
				} else if (notificationType.equalsIgnoreCase(NotificationTypeEnum.EMAIL.name())
						&& isTemplateAvailable(messageSenderDto)) {
					isEmailSuccess = sendEmail(id, process, attributes, ccEMailList, regType, messageSenderDto,
							description);
				} else {
					throw new TemplateNotFoundException(MessageSenderStatusMessage.TEMPLATE_NOT_FOUND);
				}
			}
		}

		if (isEmailSuccess && isSMSSuccess) {
			isNotificationSuccess = true;
			description.setMessage(StatusUtil.MESSAGE_SENDER_NOTIF_SUCC.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode());
			description.setStatusComment(StatusUtil.MESSAGE_SENDER_NOTIF_SUCC.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_NOTIF_SUCC.getCode());
		} else if (!isEmailSuccess && !isSMSSuccess) {
			description.setMessage(StatusUtil.MESSAGE_SENDER_NOTIFICATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getCode());
			description.setStatusComment(StatusUtil.MESSAGE_SENDER_NOTIFICATION_FAILED.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_NOTIFICATION_FAILED.getCode());
		} else if (allNotificationTypes.length == 1
				&& ((allNotificationTypes[0].equalsIgnoreCase(NotificationTypeEnum.SMS.name()) && isSMSSuccess)
						|| (allNotificationTypes[0].equalsIgnoreCase(NotificationTypeEnum.EMAIL.name())
								&& isEmailSuccess))) {
			// if only one notification type is set and that is successful
			isNotificationSuccess = true;
		} else if (!isEmailSuccess || !isSMSSuccess) {
			isNotificationSuccess = false;
			String failedMessage = "Failed to send Notification for type : "
					+ (isEmailSuccess ? NotificationTypeEnum.SMS.name() : NotificationTypeEnum.EMAIL.name());
			description.setMessage(failedMessage);
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getCode());
			description.setStatusComment(failedMessage);
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_NOTIFICATION_FAILED.getCode());
		}

		return isNotificationSuccess;
	}

	private boolean sendEmail(String id, String process, Map<String, Object> attributes, String[] ccEMailList,
			String regType, MessageSenderDto messageSenderDto, LogDescription description) throws Exception {
		boolean isEmailSuccess = false;
		try {
			ResponseDto emailResponse = service.sendEmailNotification(messageSenderDto.getEmailTemplateCode(),
					id, process, messageSenderDto.getIdType(), attributes, ccEMailList, messageSenderDto.getSubjectCode(),
					null, regType);
			if (emailResponse.getStatus().equals("success")) {
				isEmailSuccess = true;
				description.setStatusComment(StatusUtil.MESSAGE_SENDER_EMAIL_SUCCESS.getMessage());
				description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_EMAIL_SUCCESS.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_EMAIL_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode());
			} else {
				description.setStatusComment(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getMessage());
				description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getCode());
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), id,
					MessageSenderStatusMessage.EMAIL_NOTIFICATION_SUCCESS);
		} catch (EmailIdNotFoundException e) {
			description.setStatusComment(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getCode());
			description.setMessage(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getCode());
		} catch (TemplateGenerationFailedException | ApisResourceAccessException e) {
			description.setStatusComment(e.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_EMAIL_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_EMAIL_FAILED.getCode());
		}
		return isEmailSuccess;
	}

	private boolean sendSms(String id, String process, Map<String, Object> attributes, String regType,
			MessageSenderDto messageSenderDto, LogDescription description)
			throws ApisResourceAccessException, IOException,
			io.mosip.registration.processor.core.exception.PacketDecryptionFailureException, JSONException {
		boolean isSmsSuccess = false;
		try {
			SmsResponseDto smsResponse = service.sendSmsNotification(messageSenderDto.getSmsTemplateCode(), id,
					process, messageSenderDto.getIdType(), attributes, regType);
			if (smsResponse.getStatus().equals("success")) {
				isSmsSuccess = true;
				description.setStatusComment(StatusUtil.MESSAGE_SENDER_SMS_SUCCESS.getMessage());
				description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_SMS_SUCCESS.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_SMS_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode());
			} else {
				description.setStatusComment(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getMessage());
				description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getCode());
				description.setMessage(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_SMS_FAILED.getCode());
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), id,
					MessageSenderStatusMessage.SMS_NOTIFICATION_SUCCESS);
		} catch (PhoneNumberNotFoundException e) {
			description.setStatusComment(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getCode());
			description.setMessage(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_SMS_FAILED.getCode());
		} catch (TemplateGenerationFailedException | ApisResourceAccessException e) {
			description.setStatusComment(e.getMessage());
			description.setSubStatusCode(StatusUtil.MESSAGE_SENDER_SMS_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_SMS_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_SMS_FAILED.getCode());
		}
		return isSmsSuccess;
	}

	/**
	 * Sets the template and subject.
	 *
	 * @param templatetype     the new template and subject
	 * @param regType
	 * @param messageSenderDto
	 */
	private void setTemplateAndSubject(NotificationTemplateType templatetype, String regType,
			MessageSenderDto messageSenderDto) {
		String internalProcess= utilities.getInternalProcess(additionalProcessCategoryForNotification, regType);
		switch (templatetype) {
		case LOST_UIN:
			messageSenderDto.setSmsTemplateCode(env.getProperty(LOST_UIN+SMS));
			messageSenderDto.setEmailTemplateCode(env.getProperty(LOST_UIN+EMAIL));
			messageSenderDto.setIdType(IdType.UIN);
			messageSenderDto.setSubjectCode(env.getProperty(LOST_UIN+SUB));
			break;
		case UIN_CREATED:
			messageSenderDto.setSmsTemplateCode(env.getProperty(UIN_CREATED+SMS));
			messageSenderDto.setEmailTemplateCode(env.getProperty(UIN_CREATED+EMAIL));
			messageSenderDto.setIdType(IdType.UIN);
			messageSenderDto.setSubjectCode(env.getProperty(UIN_CREATED+SUB));
			break;
		case UIN_UPDATE:
			if (regType.equalsIgnoreCase(RegistrationType.NEW.name())) {
				messageSenderDto.setSmsTemplateCode(env.getProperty(UIN_NEW+SMS));
				messageSenderDto.setEmailTemplateCode(env.getProperty(UIN_NEW+EMAIL));
				messageSenderDto.setIdType(IdType.UIN);
				messageSenderDto.setSubjectCode(env.getProperty(UIN_NEW+SUB));
			} else if (regType.equalsIgnoreCase(RegistrationType.ACTIVATED.name())) {
				messageSenderDto.setSmsTemplateCode(env.getProperty(UIN_ACTIVATE+SMS));
				messageSenderDto.setEmailTemplateCode(env.getProperty(UIN_ACTIVATE+EMAIL));
				messageSenderDto.setIdType(IdType.UIN);
				messageSenderDto.setSubjectCode(env.getProperty(UIN_ACTIVATE+SUB));
			} else if (regType.equalsIgnoreCase(RegistrationType.DEACTIVATED.name())) {
				messageSenderDto.setSmsTemplateCode(env.getProperty(UIN_DEACTIVATE+SMS));
				messageSenderDto.setEmailTemplateCode(env.getProperty(UIN_DEACTIVATE+EMAIL));
				messageSenderDto.setIdType(IdType.UIN);
				messageSenderDto.setSubjectCode(env.getProperty(UIN_DEACTIVATE+SUB));
			} else if (regType.equalsIgnoreCase(RegistrationType.UPDATE.name())
					|| regType.equalsIgnoreCase(RegistrationType.RES_UPDATE.name())
					|| internalProcess.equalsIgnoreCase(RegistrationType.UPDATE.toString())){
				messageSenderDto.setSmsTemplateCode(env.getProperty(UIN_UPDATE+SMS));
				messageSenderDto.setEmailTemplateCode(env.getProperty(UIN_UPDATE+EMAIL));
				messageSenderDto.setIdType(IdType.UIN);
				messageSenderDto.setSubjectCode(env.getProperty(UIN_UPDATE+SMS));
			}
			break;
		case DUPLICATE_UIN:
			messageSenderDto.setSmsTemplateCode(env.getProperty(DUPLICATE_UIN+SMS));
			messageSenderDto.setEmailTemplateCode(env.getProperty(DUPLICATE_UIN+EMAIL));
			messageSenderDto.setIdType(IdType.RID);
			messageSenderDto.setSubjectCode(env.getProperty(DUPLICATE_UIN+SUB));
			break;
		case TECHNICAL_ISSUE:
			messageSenderDto.setSmsTemplateCode(env.getProperty(TECHNICAL_ISSUE+SMS));
			messageSenderDto.setEmailTemplateCode(env.getProperty(TECHNICAL_ISSUE+EMAIL));
			messageSenderDto.setIdType(IdType.RID);
			messageSenderDto.setSubjectCode(env.getProperty(TECHNICAL_ISSUE+SUB));
			break;
		default:
			break;
		}
	}

	/**
	 * Checks if is template available.
	 *
	 * @param messageSenderDto the template code
	 * @return true, if is template available
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws JsonProcessingException
	 * @throws ParseException
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	private boolean isTemplateAvailable(MessageSenderDto messageSenderDto)
			throws ApisResourceAccessException, IOException {

		List<String> pathSegments = new ArrayList<>();
		ResponseWrapper<?> responseWrapper;
		TemplateResponseDto templateResponseDto = null;

		responseWrapper = (ResponseWrapper<?>) restClientService.getApi(ApiName.TEMPLATES, pathSegments, "", "",
				ResponseWrapper.class);
		templateResponseDto = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()),
				TemplateResponseDto.class);

		if (responseWrapper.getErrors() == null) {
			templateResponseDto.getTemplates().forEach(dto -> {
				if (dto.getTemplateTypeCode().equalsIgnoreCase(messageSenderDto.getSmsTemplateCode())) {
					messageSenderDto.setTemplateAvailable(true);
				}
			});
		}
		return messageSenderDto.isTemplateAvailable();
	}

	@Override
	public ResponseEntity<Void> process(WorkflowPausedForAdditionalInfoEventDTO object) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		ResponseEntity<Void> responseEntity = null;
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		MessageSenderDto messageSenderDto = new MessageSenderDto();
		String id = object.getInstanceId();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"NotificationServiceImpl::process()::entry");

		try {
			String workflowType = object.getWorkflowType();
			setTemplateAndSubjectForPausedForAdditionalInfo(messageSenderDto);
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("additionalInfoRequestId", object.getAdditionalInfoRequestId());
			attributes.put("additionalInfoProcess", object.getAdditionalInfoProcess());
				String[] ccEMailList = null;

				if (isNotificationTypesEmpty()) {
					description.setStatusComment(StatusUtil.TEMPLATE_CONFIGURATION_NOT_FOUND.getMessage());
					description.setSubStatusCode(StatusUtil.TEMPLATE_CONFIGURATION_NOT_FOUND.getCode());
					description.setMessage(PlatformErrorMessages.RPR_TEMPLATE_CONFIGURATION_NOT_FOUND.getMessage());
					description.setCode(PlatformErrorMessages.RPR_TEMPLATE_CONFIGURATION_NOT_FOUND.getCode());
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), object.getInstanceId(),
							PlatformErrorMessages.RPR_TEM_CONFIGURATION_NOT_FOUND.getMessage());
					throw new ConfigurationNotFoundException(
							PlatformErrorMessages.RPR_TEM_CONFIGURATION_NOT_FOUND.getCode());
				}
				String[] allNotificationTypes = notificationTypes.split("\\|");

				if (isNotificationEmailsEmpty()) {
					ccEMailList = notificationEmails.split("\\|");
				}

				isTransactionSuccessful = sendNotification(id, workflowType, attributes, ccEMailList,
						allNotificationTypes, workflowType, messageSenderDto, description);


			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, "NotificationServiceImpl::success");
		} catch (TemplateNotFoundException tnf) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, tnf.getMessage() + ExceptionUtils.getStackTrace(tnf));
			description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage() + tnf.getMessage()));
			description.setSubStatusCode(StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			description.setMessage(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage());
			description.setCode(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			responseEntity = new ResponseEntity<Void>(HttpStatus.OK);
		} catch (Exception ex) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			description.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			description.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getCode());
			responseEntity = new ResponseEntity<Void>(HttpStatus.OK);
		} finally {
			responseEntity =  new ResponseEntity<Void>(HttpStatus.OK);
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			String eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_MESSAGE_SENDER_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.MESSAGE_SENDER.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, id);
		}

		return responseEntity;
	}

	private void setTemplateAndSubjectForPausedForAdditionalInfo(MessageSenderDto messageSenderDto) {
		messageSenderDto
				.setSmsTemplateCode(env.getProperty(PAUSED_FOR_ADDITIONAL_INFO+SMS));
		messageSenderDto
				.setEmailTemplateCode(env.getProperty(PAUSED_FOR_ADDITIONAL_INFO+EMAIL));
		messageSenderDto
		.setSubjectCode(env.getProperty(PAUSED_FOR_ADDITIONAL_INFO+SUB));
		messageSenderDto.setIdType(IdType.RID);

	}

}
