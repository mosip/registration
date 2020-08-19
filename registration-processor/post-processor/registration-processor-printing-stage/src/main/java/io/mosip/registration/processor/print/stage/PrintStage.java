package io.mosip.registration.processor.print.stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.jms.Message;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.CardType;
import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.TemplateProcessingFailureException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValue;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.queue.impl.exception.ConnectionUnavailableException;
import io.mosip.registration.processor.core.spi.print.service.PrintService;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.IdentityIteratorUtil;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.print.exception.PrintGlobalExceptionHandler;
import io.mosip.registration.processor.print.exception.QueueConnectionNotFound;
import io.mosip.registration.processor.print.service.dto.PrintQueueDTO;
import io.mosip.registration.processor.print.service.exception.PDFSignatureException;
import io.mosip.registration.processor.print.service.impl.PrintPostServiceImpl;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * The Class PrintStage.
 * 
 * @author M1048358 Alok
 * @author Ranjitha Siddegowda
 */
@RefreshScope
@Service
public class PrintStage extends MosipVerticleAPIManager {

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/** The Constant UIN_CARD_PDF. */
	private static final String UIN_CARD_PDF = "uinPdf";

	/** The Constant UIN_TEXT_FILE. */
	private static final String UIN_TEXT_FILE = "textFile";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PrintStage.class);

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** Enable proxy postal service response. */
	@Value("${registration.processor.enable.proxy.postalservice}")
	private boolean isProxyAbisEnabled;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The global exception handler. */
	@Autowired
	public PrintGlobalExceptionHandler globalExceptionHandler;

	@Autowired
	public Utilities utilities;

	/** The port. */
	@Value("${server.port}")
	private String port;

	/** The mosip event bus. */
	private MosipEventBus mosipEventBus;

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The mosip queue manager. */
	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	/** The mosip connection factory. */
	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	/** The print service. */
	@Autowired
	private PrintService<Map<String, byte[]>> printService;

	/** The print post service. */
	@Autowired
	private PrintPostServiceImpl printPostService;

	/** The username. */
	@Value("${registration.processor.queue.username}")
	private String username;

	/** The password. */
	@Value("${registration.processor.queue.password}")
	private String password;

	/** The url. */
	@Value("${registration.processor.queue.url}")
	private String url;

	/** The type of queue. */
	@Value("${registration.processor.queue.typeOfQueue}")
	private String typeOfQueue;

	/** The address. */
	@Value("${registration.processor.queue.address}")
	private String address;

	/** The print & postal service provider address. */
	/** The address. */
	@Value("${registration.processor.queue.printpostaladdress}")
	private String printPostalAddress;

	@Value("${server.servlet.path}")
	private String contextPath;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Autowired
	private UinValidator<String> uinValidatorImpl;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	boolean isConnection = false;

	private static final String SUCCESS = "Success";

	private static final String RESEND = "Resend";

	private static final String CLASSNAME = "PrintStage";

	private static final String SEPERATOR = "::";

	/** The Constant FAIL_OVER. */
	private static final String FAIL_OVER = "failover:(";

	/** The Constant RANDOMIZE_FALSE. */
	private static final String RANDOMIZE_FALSE = ")?randomize=false";

	private static final String CONFIGURE_MONITOR_IN_ACTIVITY = "?wireFormat.maxInactivityDuration=0";

	private MosipQueue queue;

	InternalRegistrationStatusDto registrationStatusDto;

	private static final String UIN_CARD_TEMPLATE = "RPR_UIN_CARD_TEMPLATE";

	private static final String MASKED_UIN_CARD_TEMPLATE = "RPR_MASKED_UIN_CARD_TEMPLATE";

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {

		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.PRINTING_BUS);

		queue = getQueueConnection();
		if (queue != null) {

			QueueListener listener = new QueueListener() {
				@Override
				public void setListener(Message message) {
					consumerListener(message);
				}
			};

			mosipQueueManager.consume(queue, printPostalAddress, listener);

		} else {
			throw new QueueConnectionNotFound(PlatformErrorMessages.RPR_PRT_QUEUE_CONNECTION_NULL.getMessage());
		}

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
		TrimExceptionMessage trimeExpMessage = new TrimExceptionMessage();
		object.setMessageBusAddress(MessageBusAddress.PRINTING_BUS);
		object.setInternalError(Boolean.FALSE);
		LogDescription description = new LogDescription();

		boolean isTransactionSuccessful = false;

		String regId = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "PrintStage::process()::entry");

		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(regId);
			if (RegistrationType.RES_REPRINT.toString().equals(object.getReg_type().toString())) {
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
			}
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());
			registrationStatusDto.setRegistrationStageName(this.getClass().getSimpleName());

			IdType idType = IdType.RID;

			String idValue = regId;
			String cardType = "UIN";
			if (io.mosip.registration.processor.status.code.RegistrationType.RES_REPRINT.toString()
					.equalsIgnoreCase(object.getReg_type().toString())) {

				PacketMetaInfo packetMetaInfo = utilities.getPacketMetaInfo(regId);
				Identity identity = packetMetaInfo.getIdentity();
				List<FieldValue> metadataList = identity.getMetaData();
				IdentityIteratorUtil identityIteratorUtil = new IdentityIteratorUtil();
				cardType = identityIteratorUtil.getFieldValue(metadataList, JsonConstant.CARDTYPE);

				if (cardType.equalsIgnoreCase(CardType.MASKED_UIN.toString())) {
					idType = IdType.VID;
					idValue = identityIteratorUtil.getFieldValue(metadataList, JsonConstant.VID);

				} else {
					idType = IdType.UIN;
					JSONObject jsonObject = utilities.retrieveUIN(regId);
					idValue = JsonUtil.getJSONValue(jsonObject, IdType.UIN.toString());

				}
			}
			Map<String, byte[]> documentBytesMap = printService.getDocuments(idType, idValue, cardType, false);

			boolean isAddedToQueue = sendToQueue(queue, documentBytesMap, 0, regId);

			if (isAddedToQueue) {
				object.setIsValid(Boolean.TRUE);
				isTransactionSuccessful = true;
				description.setMessage(PlatformSuccessMessages.RPR_PRINT_STAGE_SENT_QUEUE_SUCCESS.getMessage());
				description.setCode(PlatformSuccessMessages.RPR_PRINT_STAGE_SENT_QUEUE_SUCCESS.getCode());
				registrationStatusDto.setStatusComment(
						trimeExpMessage.trimExceptionMessage(StatusUtil.PDF_ADDED_TO_QUEUE_SUCCESS.getMessage()));
				registrationStatusDto.setSubStatusCode(StatusUtil.PDF_ADDED_TO_QUEUE_SUCCESS.getCode());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
				registrationStatusDto
						.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());

			} else {
				object.setIsValid(Boolean.FALSE);
				isTransactionSuccessful = false;
				description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_NOT_ADDED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PRT_PDF_NOT_ADDED.getCode());

				registrationStatusDto.setStatusComment(
						trimeExpMessage.trimExceptionMessage(StatusUtil.PDF_ADDED_TO_QUEUE_FAILED.getMessage()));
				registrationStatusDto.setSubStatusCode(StatusUtil.PDF_ADDED_TO_QUEUE_FAILED.getCode());
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());
				registrationStatusDto
						.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.PRINT_SERVICE.toString());

			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, description.getMessage());
			registrationStatusDto.setUpdatedBy(USER);

			if (isProxyAbisEnabled)
				printPostService.generatePrintandPostal(regId, queue, mosipQueueManager);

		} catch (PDFGeneratorException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(trimeExpMessage.trimExceptionMessage(
					StatusUtil.PDF_GENERATION_FAILED.getMessage() + regId + SEPERATOR + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PDF_GENERATION_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getCode());
			object.setInternalError(Boolean.TRUE);
		}

		catch (PDFSignatureException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(trimeExpMessage.trimExceptionMessage(
					StatusUtil.PDF_SIGNTURED_FAILED.getMessage() + regId + SEPERATOR + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.PDF_SIGNTURED_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_SIGNATURE_EXCEPTION.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (TemplateProcessingFailureException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(trimeExpMessage.trimExceptionMessage(
					StatusUtil.TEMPLATE_PROCESSING_FAILED.getMessage() + regId + SEPERATOR + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.TEMPLATE_PROCESSING_FAILED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_TEM_PROCESSING_FAILURE.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (QueueConnectionNotFound e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId,
					PlatformErrorMessages.RPR_PRT_QUEUE_CONNECTION_NULL.name() + ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(trimeExpMessage.trimExceptionMessage(
					StatusUtil.QUEUE_CONNECTION_NOT_FOUND.getMessage() + regId + SEPERATOR + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.QUEUE_CONNECTION_NOT_FOUND.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_QUEUE_CONNECTION_NULL.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_QUEUE_CONNECTION_NULL.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (ConnectionUnavailableException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(trimeExpMessage.trimExceptionMessage(
					StatusUtil.QUEUE_CONNECTION_UNAVAILABLE.getMessage() + regId + SEPERATOR + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.QUEUE_CONNECTION_UNAVAILABLE.getCode());
			description.setMessage(PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getCode());
			object.setInternalError(Boolean.TRUE);
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			registrationStatusDto.setStatusComment(
					trimeExpMessage.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());
			description.setMessage(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_PRT_PDF_GENERATION_FAILED.getCode());
			object.setInternalError(Boolean.TRUE);
		} finally {
			String eventId = "";
			String eventName = "";
			String eventType = "";
			eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_PRINT_STAGE_SENT_QUEUE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.PRINT_STAGE.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, regId);

		}

		return object;
	}

	/**
	 * Send to queue.
	 *
	 * @param queue
	 *            the queue
	 * @param documentBytesMap
	 *            the document bytes map
	 * @param count
	 *            the count
	 * @param uin
	 *            the uin
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean sendToQueue(MosipQueue queue, Map<String, byte[]> documentBytesMap, int count, String regId)
			throws IOException {
		boolean isAddedToQueue = false;
		try {
			PrintQueueDTO queueDto = new PrintQueueDTO();
			queueDto.setPdfBytes(documentBytesMap.get(UIN_CARD_PDF));
			queueDto.setTextBytes(documentBytesMap.get(UIN_TEXT_FILE));
			queueDto.setRegId(regId);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(queueDto);
			oos.flush();
			byte[] printQueueBytes = bos.toByteArray();
			isAddedToQueue = mosipQueueManager.send(queue, printQueueBytes, address);

		} catch (ConnectionUnavailableException e) {
			if (count < 5) {
				sendToQueue(queue, documentBytesMap, count + 1, regId);
			} else {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.name() + e.getMessage()
								+ ExceptionUtils.getStackTrace(e));
				throw new ConnectionUnavailableException(
						PlatformErrorMessages.RPR_MQI_UNABLE_TO_SEND_TO_QUEUE.getCode());
			}
		}

		return isAddedToQueue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() {
		router.setRoute(this.postUrl(vertx, MessageBusAddress.PRINTING_BUS, null));
		this.routes(router);
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	/**
	 * contains all the routes in the stage.
	 *
	 * @param router
	 *            the router
	 */
	private void routes(MosipRouter router) {
		router.post(contextPath + "/resend");
		router.handler(this::reSendPrintPdf, this::failure);

	}

	/**
	 * This is for failure handler
	 * 
	 * @param routingContext
	 */
	private void failure(RoutingContext routingContext) {
		this.setResponse(routingContext, globalExceptionHandler.handler(routingContext.failure()));
	}

	/**
	 * Re send print pdf.
	 *
	 * @param ctx
	 *            the ctx
	 */
	public void reSendPrintPdf(RoutingContext ctx) {
		boolean isValidUin = false;
		JsonObject object = ctx.getBodyAsJson();
		MessageDTO messageDTO = new MessageDTO();
		try {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					object.getString("regId"), "PrintStage::reSendPrintPdf()::entry");
			messageDTO.setRid(object.getString("regId"));
			String uin = object.getString("uin");
			String status = object.getString("status");
			isValidUin = uinValidatorImpl.validateId(uin);
			if (isValidUin && status.equalsIgnoreCase(RESEND)) {
				MessageDTO responseMessageDto = resendQueueResponse(messageDTO, status);
				if (!responseMessageDto.getIsValid()) {
					this.setResponse(ctx, RegistrationStatusCode.PROCESSED);
				} else {
					this.setResponse(ctx, "Caught internal error in messageDto");
				}
			} else {
				this.setResponse(ctx, "Invalid request");
			}

		} catch (Exception e) {
			this.setResponse(ctx, "Invalid request");
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", PlatformErrorMessages.RPR_BDD_UNKNOWN_EXCEPTION.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				object.getString("regId"), "PrintStage::reSendPrintPdf()::exit");
	}

	public void consumerListener(Message message) {

		String registrationId = null;
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		try {

			String response = new String(((ActiveMQBytesMessage) message).getContent().data);
			JSONObject jsonObject = JsonUtil.objectMapperReadValue(response, JSONObject.class);
			String status = JsonUtil.getJSONValue(jsonObject, "Status");
			registrationId = JsonUtil.getJSONValue(jsonObject, "RegId");
			if (registrationId != null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"PrintStage::consumerListener()::entry");
				InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
						.getRegistrationStatus(registrationId);
				if (status.equalsIgnoreCase(SUCCESS)) {
					isTransactionSuccessful = true;
					description.setMessage(PlatformSuccessMessages.RPR_PRINT_STAGE_SUCCESS.getMessage());
					description.setCode(PlatformSuccessMessages.RPR_PRINT_STAGE_SUCCESS.getCode());

					registrationStatusDto.setStatusComment(StatusUtil.PRINT_POST_COMPLETED.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.PRINT_POST_COMPLETED.getCode());
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
					registrationStatusDto.setStatusCode(RegistrationTransactionStatusCode.PROCESSED.toString());
					registrationStatusDto.setLatestTransactionTypeCode(
							RegistrationTransactionTypeCode.PRINT_POSTAL_SERVICE.toString());
					registrationStatusDto.setUpdatedBy(USER);
					/** Module-Id can be Both Success/Error code */
					String moduleId = description.getCode();
					String moduleName = ModuleName.PRINT_STAGE.toString();
					registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
				} else if (status.equalsIgnoreCase(RESEND)) {
					MessageDTO messageDTO = new MessageDTO();
					messageDTO.setReg_type(RegistrationType.valueOf(registrationStatusDto.getRegistrationType()));
					messageDTO.setRid(registrationId);
					description.setMessage(PlatformErrorMessages.RPR_PRT_RESEND_UIN_CARD.getMessage());
					description.setCode(PlatformErrorMessages.RPR_PRT_RESEND_UIN_CARD.getCode());

					registrationStatusDto.setStatusComment(StatusUtil.RESEND_UIN_CARD.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.RESEND_UIN_CARD.getCode());
					registrationStatusDto.setLatestTransactionStatusCode(RESEND);
					registrationStatusDto.setLatestTransactionTypeCode(
							RegistrationTransactionTypeCode.PRINT_POSTAL_SERVICE.toString());
					registrationStatusDto.setUpdatedBy(USER);
					String moduleId = description.getCode();
					String moduleName = ModuleName.PRINT_STAGE.toString();
					registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
					this.send(mosipEventBus, MessageBusAddress.PRINTING_BUS, messageDTO);
				}

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, description.getMessage());
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId,
						"PrintStage::consumerListener()::exit");
			} else {
				description.setMessage(PlatformErrorMessages.RPR_PRT_PRINT_POST_ACK_FAILED.getMessage());
				description.setCode(PlatformErrorMessages.RPR_PRT_PRINT_POST_ACK_FAILED.getCode());

				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), "",
						PlatformErrorMessages.RPR_PRT_PRINT_POST_ACK_FAILED.name());
			}

		} catch (IOException e) {
			isTransactionSuccessful = false;
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, PlatformErrorMessages.RPR_PRT_PRINT_POST_ACK_FAILED.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
		} finally {
			String eventId = "";
			String eventName = "";
			String eventType = "";
			eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			eventName = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventName.UPDATE.toString()
					: EventName.EXCEPTION.toString();
			eventType = eventId.equalsIgnoreCase(EventId.RPR_402.toString()) ? EventType.BUSINESS.toString()
					: EventType.SYSTEM.toString();
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful ? PlatformSuccessMessages.RPR_PRINT_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.PRINT_STAGE.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}

	}

	private MosipQueue getQueueConnection() {
		// url = url + CONFIGURE_MONITOR_IN_ACTIVITY;
		String failOverBrokerUrl = FAIL_OVER + url + "," + url + RANDOMIZE_FALSE;
		return mosipConnectionFactory.createConnection(typeOfQueue, username, password, failOverBrokerUrl);
	}

	@SuppressWarnings("unchecked")
	private MessageDTO resendQueueResponse(MessageDTO messageDto, String status) {
		JSONObject response = new JSONObject();

		try {
			response.put("RegId", messageDto.getRid());
			response.put("Status", status);
			mosipQueueManager.send(queue, response.toString().getBytes("UTF-8"), printPostalAddress);
			messageDto.setIsValid(Boolean.FALSE);
		} catch (UnsupportedEncodingException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					messageDto.getRid(), PlatformErrorMessages.RPR_CMB_UNSUPPORTED_ENCODING.name() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
		}
		return messageDto;
	}

}
