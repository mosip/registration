package io.mosip.registration.processor.message.sender.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
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
import io.mosip.registration.processor.message.sender.constants.MessageSenderConstant;
import io.mosip.registration.processor.message.sender.constants.NotificationTypeEnum;
import io.mosip.registration.processor.message.sender.dto.MessageSenderDto;
import io.mosip.registration.processor.message.sender.exception.ConfigurationNotFoundException;
import io.mosip.registration.processor.message.sender.exception.EmailIdNotFoundException;
import io.mosip.registration.processor.message.sender.exception.PhoneNumberNotFoundException;
import io.mosip.registration.processor.message.sender.exception.TemplateGenerationFailedException;
import io.mosip.registration.processor.message.sender.exception.TemplateNotFoundException;
import io.mosip.registration.processor.message.sender.util.StatusNotificationTypeMapUtil;
import io.mosip.registration.processor.message.sender.utility.MessageSenderStatusMessage;
import io.mosip.registration.processor.message.sender.utility.NotificationTemplateType;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.code.RegistrationType;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.dto.SyncTypeDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.service.TransactionService;

/**
 * The Class MessageSenderStage.
 *
 * @author M1048358 Alok
 * @since 1.0.0
 */
@RefreshScope

@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.message.sender.config",
		"io.mosip.registration.processor.stages.config", 
		"io.mosip.registrationprocessor.stages.config", 
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", 
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", 
		"io.mosip.kernel.idobjectvalidator.config",
		"io.mosip.registration.processor.core.kernel.beans" })
public class MessageSenderStage extends MosipVerticleAPIManager {
	
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.message.sender.";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(MessageSenderStage.class);

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;
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



	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The notification emails. */
	@Value("${registration.processor.notification.emails}")
	private String notificationEmails;

	@Value("${mosip.notificationtype}")
	private String notificationTypes;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The service. */
	@Autowired
	private MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> service;

	@Autowired
	private TransactionService<TransactionDto> transactionStatusService;

	/** Mosip router for APIs */
	@Autowired
	private MosipRouter router;
	
	@Autowired
	private Environment env;

	@Autowired
	private ObjectMapper mapper;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.message.sender.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;
	
	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationservice;
	
	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}
	
	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		MosipEventBus mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.MESSAGE_SENDER_BUS, messageExpiryTimeLimit);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() {
		router.setRoute(this.postUrl(vertx, MessageBusAddress.MESSAGE_SENDER_BUS, null));
		this.createServer(router.getRouter(), getPort());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#process(
	 * java.lang.Object)
	 */
	@Override
	public MessageDTO process(MessageDTO object) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		object.setMessageBusAddress(MessageBusAddress.MESSAGE_SENDER_BUS);
		boolean isTransactionSuccessful = false;
		object.setInternalError(Boolean.FALSE);
		LogDescription description = new LogDescription();
		String status;
		MessageSenderDto messageSenderDto = new MessageSenderDto();
		String id = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"MessageSenderStage::process()::entry");
		SyncRegistrationEntity regEntity = syncRegistrationservice.findByWorkflowInstanceId(object.getWorkflowInstanceId());
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				id, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
		status = registrationStatusDto.getLatestTransactionTypeCode() + "_"
				+ registrationStatusDto.getLatestTransactionStatusCode();

		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.NOTIFICATION.toString());
		registrationStatusDto.setRegistrationStageName(getStageName());

		try {
			
			String regType = regEntity.getRegistrationType();

			NotificationTemplateType type = null;
			StatusNotificationTypeMapUtil map = new StatusNotificationTypeMapUtil();

			if (registrationStatusDto.getStatusCode().equals(RegistrationStatusCode.PROCESSED.toString())) {
				type = setNotificationTemplateType(registrationStatusDto, type);
			} else {
				type = map.getTemplateType(status);
			}
			if (NotificationTemplateType.DUPLICATE_UIN.equals(type)
					&& registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.LOST.getValue())) {
				isTransactionSuccessful = false;
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				description.setStatusComment(StatusUtil.NOTIFICATION_FAILED_FOR_LOST.getMessage());
				description.setSubStatusCode(StatusUtil.NOTIFICATION_FAILED_FOR_LOST.getCode());
				description.setMessage(PlatformErrorMessages.RPR_NOTIFICATION_FAILED_FOR_LOST.getMessage());
				description.setCode(PlatformErrorMessages.RPR_NOTIFICATION_FAILED_FOR_LOST.getCode());
			} else {
				if (type != null) {
					setTemplateAndSubject(type, regType, messageSenderDto);
				}

				Map<String, Object> attributes = new HashMap<>();
				String[] ccEMailList = null;

				if (isNotificationTypesEmpty()) {
					description.setStatusComment(StatusUtil.TEMPLATE_CONFIGURATION_NOT_FOUND.getMessage());
					description.setSubStatusCode(StatusUtil.TEMPLATE_CONFIGURATION_NOT_FOUND.getCode());
					description.setMessage(PlatformErrorMessages.RPR_TEMPLATE_CONFIGURATION_NOT_FOUND.getMessage());
					description.setCode(PlatformErrorMessages.RPR_TEMPLATE_CONFIGURATION_NOT_FOUND.getCode());
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), object.getRid(),
							PlatformErrorMessages.RPR_TEM_CONFIGURATION_NOT_FOUND.getMessage());
					throw new ConfigurationNotFoundException(
							PlatformErrorMessages.RPR_TEM_CONFIGURATION_NOT_FOUND.getCode());
				}
				String[] allNotificationTypes = notificationTypes.split("\\|");

				if (isNotificationEmailsEmpty()) {
					ccEMailList = notificationEmails.split("\\|");
				}

				boolean isNotificationSuccess = sendNotification(id, registrationStatusDto.getRegistrationType(),
						attributes, ccEMailList, allNotificationTypes, regType, messageSenderDto, description);

				if (isNotificationSuccess) {
					isTransactionSuccessful = true;
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				} else {
					isTransactionSuccessful = false;
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				}
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, "MessageSenderStage::success");
		} catch (EmailIdNotFoundException | PhoneNumberNotFoundException | TemplateGenerationFailedException |

				ConfigurationNotFoundException e) {
			object.setInternalError(Boolean.TRUE);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, e.getMessage() + ExceptionUtils.getStackTrace(e));
			description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage() + e.getMessage()));
			description.setSubStatusCode(StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			description.setMessage(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage());
			description.setCode(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

		} catch (TemplateNotFoundException tnf) {
			object.setInternalError(Boolean.TRUE);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, tnf.getMessage() + ExceptionUtils.getStackTrace(tnf));
			description.setStatusComment(trimExceptionMessage.trimExceptionMessage(
					StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage() + tnf.getMessage()));
			description.setSubStatusCode(StatusUtil.EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			description.setMessage(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getMessage());
			description.setCode(PlatformErrorMessages.RPR_EMAIL_PHONE_TEMPLATE_NOTIFICATION_MISSING.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

		} catch (FSAdapterException e) {
			object.setInternalError(Boolean.TRUE);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), id,
					PlatformErrorMessages.RPR_TEM_PACKET_STORE_NOT_ACCESSIBLE.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			description.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.OBJECT_STORE_EXCEPTION.getMessage() + e.getMessage()));
			description.setSubStatusCode(StatusUtil.OBJECT_STORE_EXCEPTION.getCode());
			description.setMessage(PlatformErrorMessages.RPR_TEM_PACKET_STORE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_TEM_PACKET_STORE_NOT_ACCESSIBLE.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

		} catch (Exception ex) {
			object.setInternalError(Boolean.TRUE);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					id, ex.getMessage() + ExceptionUtils.getStackTrace(ex));
			description.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + ex.getMessage()));
			description.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MESSAGE_SENDER_STAGE_FAILED.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

		} finally {
			object.setIsValid(isTransactionSuccessful);
			if (object.getInternalError()) {
				updateErrorFlags(registrationStatusDto, object);
			}
			registrationStatusDto.setStatusComment(description.getStatusComment());
			registrationStatusDto.setSubStatusCode(description.getSubStatusCode());
			TransactionDto transactionDto = new TransactionDto(UUID.randomUUID().toString(),
					registrationStatusDto.getRegistrationId(),
					registrationStatusDto.getLatestRegistrationTransactionId(),
					registrationStatusDto.getLatestTransactionTypeCode(), "updated registration status record",
					registrationStatusDto.getLatestTransactionStatusCode(), registrationStatusDto.getStatusComment(),
					registrationStatusDto.getSubStatusCode());
			transactionDto.setReferenceId(registrationStatusDto.getRegistrationId());
			transactionDto.setReferenceIdType(MessageSenderConstant.REFERENCE_TYPE_ID);
			transactionStatusService.addRegistrationTransaction(transactionDto);

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

		return object;
	}

	private NotificationTemplateType setNotificationTemplateType(InternalRegistrationStatusDto registrationStatusDto,
			NotificationTemplateType type) {
		if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.LOST.getValue()))
			type = NotificationTemplateType.LOST_UIN;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.NEW.getValue()))
			type = NotificationTemplateType.UIN_CREATED;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.UPDATE.getValue())
		|| registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.RES_UPDATE.getValue()))
			type = NotificationTemplateType.UIN_UPDATE;
		else if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(SyncTypeDto.ACTIVATED.getValue()))
			type = NotificationTemplateType.UIN_UPDATE;
		else if (registrationStatusDto.getRegistrationType()
				.equalsIgnoreCase(SyncTypeDto.DEACTIVATED.getValue()))
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
	 * @param id
	 *            the id
	 * @param attributes
	 *            the attributes
	 * @param ccEMailList
	 *            the cc E mail list
	 * @param allNotificationTypes
	 *            the all notification types
	 * @param regType
	 * @param messageSenderDto
	 * @param description
	 * @throws Exception
	 *             the exception
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
					isSMSSuccess = SendSms(id, process, attributes, regType, messageSenderDto, description);
				} else if (notificationType.equalsIgnoreCase(NotificationTypeEnum.EMAIL.name())
						&& isTemplateAvailable(messageSenderDto)) {
					isEmailSuccess = sendEmail(id, process, attributes, ccEMailList, regType, messageSenderDto, description);
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

	private boolean sendEmail(String id, String process, Map<String, Object> attributes, String[] ccEMailList, String regType,
			MessageSenderDto messageSenderDto, LogDescription description) throws Exception {
		boolean isEmailSuccess = false;
		try {
			ResponseDto emailResponse = service.sendEmailNotification(messageSenderDto.getEmailTemplateCode(),
					id, process, messageSenderDto.getIdType(), attributes, ccEMailList, messageSenderDto.getSubjectCode(), null,
					regType);
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

	private boolean SendSms(String id, String process, Map<String, Object> attributes, String regType,
			MessageSenderDto messageSenderDto, LogDescription description) throws ApisResourceAccessException,
			IOException, io.mosip.registration.processor.core.exception.PacketDecryptionFailureException,
			JSONException {
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
	 * @param templatetype
	 *            the new template and subject
	 * @param regType
	 * @param messageSenderDto
	 */
	private void setTemplateAndSubject(NotificationTemplateType templatetype, String regType,
			MessageSenderDto messageSenderDto) {
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
			|| regType.equalsIgnoreCase(RegistrationType.RES_UPDATE.name())) {
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
	 * @param messageSenderDto
	 *            the template code
	 * @return true, if is template available
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
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

	private void updateErrorFlags(InternalRegistrationStatusDto registrationStatusDto, MessageDTO object) {
		object.setInternalError(true);
		if (registrationStatusDto.getLatestTransactionStatusCode()
				.equalsIgnoreCase(RegistrationTransactionStatusCode.REPROCESS.toString())) {
			object.setIsValid(true);
		} else {
			object.setIsValid(false);
		}
	}

}
