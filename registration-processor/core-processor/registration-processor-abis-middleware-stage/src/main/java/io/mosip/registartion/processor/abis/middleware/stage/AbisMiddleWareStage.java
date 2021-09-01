package io.mosip.registartion.processor.abis.middleware.stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.assertj.core.util.Arrays;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.AbisStatusCode;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorUnCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.abis.AbisCommonResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidatesDto;
import io.mosip.registration.processor.core.packet.dto.abis.RegBioRefDto;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.entity.AbisRequestEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisRequestPKEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisResponseDetEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisResponseDetPKEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisResponseEntity;
import io.mosip.registration.processor.packet.storage.entity.AbisResponsePKEntity;
import io.mosip.registration.processor.packet.storage.repository.BasePacketRepository;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.entity.RegistrationStatusEntity;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.utilities.RegistrationUtility;

/**
 * 
 * @author Girish Yarru
 * @since v1.0
 *
 */
@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "io.mosip.registration.processor.abis.handler.config",
        "io.mosip.registration.processor.status.config",
        "io.mosip.registration.processor.rest.client.config",
        "io.mosip.registration.processor.packet.storage.config",
        "io.mosip.registration.processor.core.config",
        "io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.stages.config",
		"io.mosip.registartion.processor.abis.middleware.validators"})
public class AbisMiddleWareStage extends MosipVerticleAPIManager {
	private static Logger regProcLogger = RegProcessorLogger.getLogger(AbisMiddleWareStage.class);
	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.abis.middleware.";

	/** The mosip queue manager. */
	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Autowired
	private BasePacketRepository<AbisRequestEntity, String> abisRequestRepositary;

	@Autowired
	private BasePacketRepository<AbisResponseEntity, String> abisResponseRepositary;

	@Autowired
	private BasePacketRepository<AbisResponseDetEntity, String> abisResponseDetailRepositary;

	@Autowired
	private Utilities utility;

	@Autowired
	private RegistrationStatusDao registrationStatusDao;

	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private PacketInfoDao packetInfoDao;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;
	
	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.abis.middleware.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	/** Message Format. */
	@Value("${activemq.message.format}")
	private String messageFormat;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/** The url. */
	private static final String SYSTEM = "SYSTEM";
	private List<AbisQueueDetails> abisQueueDetails;
	private static final String REQUESTID = "requestId";
	private static final String DEMOGRAPHIC_VERIFICATION = "DEMOGRAPHIC_VERIFICATION";
	private static final String BIOGRAPHIC_VERIFICATION = "BIOGRAPHIC_VERIFICATION";
	private static final String ABIS_QUEUE_NOT_FOUND = "ABIS_QUEUE_NOT_FOUND";
	private static final String TEXT_MESSAGE = "text";

	/**
	 * Get all the abis queue details,register listener to outbound queue's
	 */
	public void deployVerticle() {
		try {
			
			mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
			this.consume(mosipEventBus, MessageBusAddress.ABIS_MIDDLEWARE_BUS_IN, messageExpiryTimeLimit);
			abisQueueDetails = utility.getAbisQueueDetails();
			for (AbisQueueDetails abisQueue : abisQueueDetails) {
				String abisInBoundaddress = abisQueue.getInboundQueueName();
				int inboundMessageTTL = abisQueue.getInboundMessageTTL();
				MosipQueue queue = abisQueue.getMosipQueue();
				QueueListener listener = new QueueListener() {
					@Override
					public void setListener(Message message) {
						try {
							consumerListener(message, abisInBoundaddress, queue, mosipEventBus,
								inboundMessageTTL);
						} catch (Exception e) {

							regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
									LoggerFileConstant.REGISTRATIONID.toString(), "", ExceptionUtils.getStackTrace(e));

						}
					}
				};
				mosipQueueManager.consume(queue, abisQueue.getOutboundQueueName(), listener);
			}

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", ExceptionUtils.getStackTrace(e));
			throw new RegistrationProcessorUnCheckedException(PlatformErrorMessages.UNKNOWN_EXCEPTION_OCCURED.getCode(),
					PlatformErrorMessages.UNKNOWN_EXCEPTION_OCCURED.getMessage(), e);

		}
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.ABIS_MIDDLEWARE_BUS_IN,
				MessageBusAddress.ABIS_MIDDLEWARE_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}


	@Override
	public MessageDTO process(MessageDTO object) {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		object.setMessageBusAddress(MessageBusAddress.ABIS_MIDDLEWARE_BUS_IN);
		object.setIsValid(false);
		object.setInternalError(false);
		boolean isTransactionSuccessful = false;
		LogDescription description = new LogDescription();
		String registrationId = object.getRid();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "AbisMiddlewareStage::process()::entry");
		InternalRegistrationStatusDto internalRegDto = registrationStatusService.getRegistrationStatus(
					registrationId, object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
		try {
			List<String> abisRefList = packetInfoManager.getReferenceIdByWorkflowInstanceId(object.getWorkflowInstanceId());
			validateNullCheck(abisRefList, "ABIS_REFERENCE_ID_NOT_FOUND");

			String refRegtrnId = getLatestTransactionId(registrationId,
					object.getReg_type(), object.getIteration(), object.getWorkflowInstanceId());
			validateNullCheck(refRegtrnId, "LATEST_TRANSACTION_ID_NOT_FOUND");
			String abisRefId = abisRefList.get(0);
			List<AbisRequestDto> abisInsertIdentifyList = packetInfoManager.getInsertOrIdentifyRequest(abisRefId,
					refRegtrnId);
			validateNullCheck(abisInsertIdentifyList, "IDENTIFY_REQUESTS_NOT_FOUND");
			// get all insert requests(already processed,in progress)
			List<AbisRequestDto> abisInsertRequestList = abisInsertIdentifyList.stream()
					.filter(dto -> dto.getRequestType().equals(AbisStatusCode.INSERT.toString()))
					.collect(Collectors.toList());
			List<AbisRequestDto> abisInprogressInsertRequestList = abisInsertRequestList.stream()
					.filter(dto -> dto.getStatusCode().equals(AbisStatusCode.IN_PROGRESS.toString()))
					.collect(Collectors.toList());
			List<AbisRequestDto> abisAlreadyprocessedInsertRequestList = abisInsertRequestList.stream()
					.filter(dto -> dto.getStatusCode().equals(AbisStatusCode.ALREADY_PROCESSED.toString()))
					.collect(Collectors.toList());
			List<AbisRequestDto> abisIdentifyRequestList = abisInsertIdentifyList.stream()
					.filter(dto -> dto.getRequestType().equals(AbisStatusCode.IDENTIFY.toString()))
					.collect(Collectors.toList());

			processInsertIdentify(abisInsertRequestList, abisIdentifyRequestList, abisInprogressInsertRequestList,
					internalRegDto, abisAlreadyprocessedInsertRequestList);

			object.setIsValid(true);
			object.setInternalError(false);
			isTransactionSuccessful = true;
			description.setMessage(PlatformSuccessMessages.RPR_ABIS_MIDDLEWARE_STAGE_SUCCESS.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_ABIS_MIDDLEWARE_STAGE_SUCCESS.getCode());

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "AbisMiddlewareStage::process()::Abis insertRequests sucessfully sent to Queue");
		} catch (RegistrationProcessorCheckedException e) {
			object.setInternalError(true);
			description.setMessage(PlatformErrorMessages.UNKNOWN_EXCEPTION_OCCURED.getMessage());
			description.setCode(PlatformErrorMessages.UNKNOWN_EXCEPTION_OCCURED.getCode());
			internalRegDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			internalRegDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			internalRegDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			object.setInternalError(true);
			description.setMessage(PlatformErrorMessages.UNKNOWN_EXCEPTION_OCCURED.getMessage());
			description.setCode(PlatformErrorMessages.UNKNOWN_EXCEPTION_OCCURED.getCode());
			internalRegDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			internalRegDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			internalRegDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, ExceptionUtils.getStackTrace(e));
		} finally {
			if (!isTransactionSuccessful) {
				String transactionTypeCode = "";
				if (transactionTypeCode.equalsIgnoreCase(DEMOGRAPHIC_VERIFICATION)) {
					internalRegDto.setRegistrationStageName("DemoDedupeStage");
				} else if (transactionTypeCode.equalsIgnoreCase(BIOGRAPHIC_VERIFICATION)) {
					internalRegDto.setRegistrationStageName("BioDedupeStage");
				}
				updateErrorFlags(internalRegDto, object);
				String moduleId = description.getCode();
				String moduleName = ModuleName.ABIS_MIDDLEWARE.toString();
				registrationStatusService.updateRegistrationStatus(internalRegDto, moduleId, moduleName);
			}

			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_ABIS_MIDDLEWARE_STAGE_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.ABIS_MIDDLEWARE.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}
		return object;
	}

	private void processInsertIdentify(List<AbisRequestDto> abisInsertRequestList,
			List<AbisRequestDto> abisIdentifyRequestList, List<AbisRequestDto> abisInprogressInsertRequestList,
			InternalRegistrationStatusDto internalRegDto, List<AbisRequestDto> abisAlreadyprocessedInsertRequestList)
			throws RegistrationProcessorCheckedException {
		// If all insert request are null then send all identify requests.
		if (abisInsertRequestList.isEmpty()) {
			for (AbisRequestDto abisIdentifyRequest : abisIdentifyRequestList) {
				List<AbisQueueDetails> abisQueue = abisQueueDetails.stream()
						.filter(dto -> dto.getName().equals(abisIdentifyRequest.getAbisAppCode()))
						.collect(Collectors.toList());
				validateNullCheck(abisQueue, ABIS_QUEUE_NOT_FOUND);
				byte[] reqBytearray = abisIdentifyRequest.getReqText();

				boolean isAddedToQueue = sendToQueue(abisQueue.get(0).getMosipQueue(), new String(reqBytearray),
						abisQueue.get(0).getInboundQueueName(), abisQueue.get(0).getInboundMessageTTL());

				updateAbisRequest(isAddedToQueue, abisIdentifyRequest, internalRegDto);
			}

		}
		// send in progress insert requests to queue
		for (AbisRequestDto abisInprogressRequest : abisInprogressInsertRequestList) {
			List<AbisQueueDetails> abisQueue = abisQueueDetails.stream()
					.filter(dto -> dto.getName().equals(abisInprogressRequest.getAbisAppCode()))
					.collect(Collectors.toList());
			validateNullCheck(abisQueue, ABIS_QUEUE_NOT_FOUND);

			byte[] reqBytearray = abisInprogressRequest.getReqText();

			boolean isAddedToQueue = sendToQueue(abisQueue.get(0).getMosipQueue(), new String(reqBytearray),
					abisQueue.get(0).getInboundQueueName(), abisQueue.get(0).getInboundMessageTTL());

			updateAbisRequest(isAddedToQueue, abisInprogressRequest, internalRegDto);
		}
		// send all identify requests for already processed insert requests
		for (AbisRequestDto abisAlreadyProcessedInsertRequest : abisAlreadyprocessedInsertRequestList) {
			List<AbisQueueDetails> abisQueue = abisQueueDetails.stream()
					.filter(dto -> dto.getName().equals(abisAlreadyProcessedInsertRequest.getAbisAppCode()))
					.collect(Collectors.toList());
			validateNullCheck(abisQueue, ABIS_QUEUE_NOT_FOUND);
			List<AbisRequestDto> identifyRequest = abisIdentifyRequestList.stream()
					.filter(dto -> dto.getAbisAppCode().equals(abisAlreadyProcessedInsertRequest.getAbisAppCode()))
					.collect(Collectors.toList());
			byte[] reqBytearray = identifyRequest.get(0).getReqText();
			boolean isAddedToQueue = sendToQueue(abisQueue.get(0).getMosipQueue(), new String(reqBytearray),
					abisQueue.get(0).getInboundQueueName(), abisQueue.get(0).getInboundMessageTTL());
			updateAbisRequest(isAddedToQueue, identifyRequest.get(0), internalRegDto);

		}
	}

	public void consumerListener(Message message, String abisInBoundAddress, MosipQueue queue,
			MosipEventBus eventBus, int inboundMessageTTL)
			throws RegistrationProcessorCheckedException {
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		InternalRegistrationStatusDto internalRegStatusDto = null;
		String registrationId = null;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisMiddlewareStage::consumerListener()::entry");
		String moduleId = "";
		String moduleName = ModuleName.ABIS_MIDDLEWARE.toString();
		boolean isTransactionSuccessful = true;
		String response = null;
		LogDescription description = new LogDescription();
		try {
			if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE)) {
				TextMessage textMessage = (TextMessage) message;
				response =textMessage.getText();
			} else
				response = new String(((ActiveMQBytesMessage) message).getContent().data);
			JSONObject inserOrIdentifyResponse = JsonUtil.objectMapperReadValue(response, JSONObject.class);
			String requestId = JsonUtil.getJSONValue(inserOrIdentifyResponse, REQUESTID);
			String batchId = packetInfoManager.getBatchIdByRequestId(requestId);
			validateNullCheck(batchId, "ABIS_BATCH_ID_NOT_FOUND");
			List<String> bioRefId = packetInfoManager.getReferenceIdByBatchId(batchId);
			validateNullCheck(bioRefId, "ABIS_REFERENCE_ID_NOT_FOUND");

			List<RegBioRefDto> regBioRefist = packetInfoManager.getRegBioRefDataByBioRefIds(bioRefId);
			RegBioRefDto regBioRefDto = regBioRefist.get(0);
			registrationId = regBioRefDto.getRegId();
			internalRegStatusDto = registrationStatusService.getRegistrationStatus(registrationId,
					regBioRefDto.getProcess(), regBioRefDto.getIteration(), regBioRefDto.getWorkflowInstanceId());
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"AbisMiddlewareStage::consumerListener()::response from abis for requestId ::" + requestId);

			AbisRequestDto abisCommonRequestDto = packetInfoManager.getAbisRequestByRequestId(requestId);
			// check for insert response,if success send corresponding identify request to
			// queue
			if (abisCommonRequestDto.getRequestType().equals(AbisStatusCode.INSERT.toString())) {
				if (AbisStatusCode.SENT.toString().equals(abisCommonRequestDto.getStatusCode())) {

				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"AbisMiddlewareStage::consumerListener()::Insert Response received from abis ::"
								+ inserOrIdentifyResponse);
				AbisInsertResponseDto abisInsertResponseDto = JsonUtil.objectMapperReadValue(response,
						AbisInsertResponseDto.class);

				if (abisInsertResponseDto.getFailureReason() != null && abisInsertResponseDto.getFailureReason().equalsIgnoreCase("10"))
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
							"Received failure response from abis.",
							"Reference id already present for requestId : " + requestId +". Regproc will pass this insert response.");

				updateAbisResponseEntity(abisInsertResponseDto, response);
				updteAbisRequestProcessed(abisInsertResponseDto, abisCommonRequestDto);
				if (isInsertSuccess(abisInsertResponseDto)) {
					List<String> transactionIdList = packetInfoManager.getAbisTransactionIdByRequestId(requestId);
					validateNullCheck(transactionIdList, "LATEST_TRANSACTION_ID_NOT_FOUND");
					List<AbisRequestDto> abisIdentifyRequestList = packetInfoManager.getIdentifyReqListByTransactionId(
							transactionIdList.get(0), AbisStatusCode.IDENTIFY.toString());
					List<AbisRequestDto> abisIdentifyRequest = abisIdentifyRequestList.stream()
							.filter(dto1 -> dto1.getAbisAppCode().equals(abisCommonRequestDto.getAbisAppCode()))
							.collect(Collectors.toList());
					validateNullCheck(abisIdentifyRequest, "IDENTIFY_REQUESTS_NOT_FOUND");
					AbisRequestDto abisIdentifyRequestDto = abisIdentifyRequest.get(0);
					boolean isAddedToQueue = sendToQueue(queue, new String(abisIdentifyRequestDto.getReqText()),
							abisInBoundAddress, inboundMessageTTL);
					updateAbisRequest(isAddedToQueue, abisIdentifyRequestDto, internalRegStatusDto);
				} else {
					internalRegStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
					internalRegStatusDto.setStatusComment(
							StatusUtil.INSERT_RESPONSE_FAILED.getMessage() + abisCommonRequestDto.getId());
					internalRegStatusDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
					moduleId = PlatformErrorMessages.SYSTEM_EXCEPTION_OCCURED.getCode();
					registrationStatusService.updateRegistrationStatus(internalRegStatusDto, moduleId, moduleName);
				}

				} else {
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
							"",
							"AbisMiddlewareStage::consumerListener()::Duplicate Insert Response received from abis for same request id ::"
									+ requestId + " response " + inserOrIdentifyResponse);
					isTransactionSuccessful = false;
					description.setMessage(PlatformErrorMessages.DUPLICATE_INSERT_RESPONSE.getMessage() + requestId);
					description.setCode(PlatformErrorMessages.DUPLICATE_INSERT_RESPONSE.getCode());

				}
			}
			// check if identify response,then if all identify requests are processed send
			// to abis handler
			if (abisCommonRequestDto.getRequestType().equals(AbisStatusCode.IDENTIFY.toString())) {
				if (AbisStatusCode.SENT.toString().equals(abisCommonRequestDto.getStatusCode())) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"AbisMiddlewareStage::consumerListener()::Identify Response received from abis ::"
								+ inserOrIdentifyResponse);

				AbisIdentifyResponseDto abisIdentifyResponseDto = JsonUtil.readValueWithUnknownProperties(response,
						AbisIdentifyResponseDto.class);
				if (!abisIdentifyResponseDto.getReturnValue().equalsIgnoreCase("1")){
					internalRegStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
					internalRegStatusDto.setStatusComment(
							StatusUtil.IDENTIFY_RESPONSE_FAILED.getMessage() + abisCommonRequestDto.getId());
					internalRegStatusDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
					moduleId = PlatformErrorMessages.SYSTEM_EXCEPTION_OCCURED.getCode();
					registrationStatusService.updateRegistrationStatus(internalRegStatusDto, moduleId, moduleName);
				}
				AbisResponseDto abisResponseDto = updateAbisResponseEntity(abisIdentifyResponseDto, response);
				if (abisIdentifyResponseDto.getCandidateList() != null) {
					CandidatesDto[] candidatesDtos = abisIdentifyResponseDto.getCandidateList().getCandidates();
					if (!Arrays.isNullOrEmpty(candidatesDtos)) {
						saveCandiateDtos(candidatesDtos, abisResponseDto, bioRefId.get(0));
					}
				}
				updteAbisRequestProcessed(abisIdentifyResponseDto, abisCommonRequestDto);

				if (checkAllIdentifyRequestsProcessed(batchId)) {

					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
							"",
							"AbisMiddlewareStage::consumerListener()::All identify are requests processed sending to Abis handler");

					sendToAbisHandler(eventBus, bioRefId, registrationId, internalRegStatusDto.getRegistrationType(),
							internalRegStatusDto.getIteration(), internalRegStatusDto.getWorkflowInstanceId());

					}
				} else {
					regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(),
							"",
							"AbisMiddlewareStage::consumerListener()::Duplicate Identify Response received from abis for same request id ::"
									+ requestId + " response " + inserOrIdentifyResponse);
					isTransactionSuccessful = false;
					description.setMessage(PlatformErrorMessages.DUPLICATE_IDENTITY_RESPONSE.getMessage() + requestId);
					description.setCode(PlatformErrorMessages.DUPLICATE_IDENTITY_RESPONSE.getCode());

				}
			}

		} catch (IOException e) {
			if (internalRegStatusDto != null) {
				internalRegStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				internalRegStatusDto.setStatusComment(trimExceptionMessage
						.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
				internalRegStatusDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
				moduleId = PlatformErrorMessages.SYSTEM_EXCEPTION_OCCURED.getCode();
				registrationStatusService.updateRegistrationStatus(internalRegStatusDto, moduleId, moduleName);
			}
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, ExceptionUtils.getStackTrace(e));
			throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
		} catch (Exception e) {
			if (internalRegStatusDto != null) {
				internalRegStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
				internalRegStatusDto.setStatusComment(trimExceptionMessage
						.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
				internalRegStatusDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
				moduleId = PlatformErrorMessages.SYSTEM_EXCEPTION_OCCURED.getCode();
				registrationStatusService.updateRegistrationStatus(internalRegStatusDto, moduleId, moduleName);
			}

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, ExceptionUtils.getStackTrace(e));
		} finally {
			if (!isTransactionSuccessful) {
				String eventId = EventId.RPR_405.toString();
				String eventName = EventName.EXCEPTION.toString();
				String eventType = EventType.SYSTEM.toString();
				moduleId = description.getCode();
				moduleName = ModuleName.ABIS_MIDDLEWARE.toString();
				auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName,
						eventType, moduleId, moduleName, registrationId);
			}

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisMiddlewareStage::consumerListener()::Exit()");
	}

	private void validateNullCheck(Object obj, String errorMessage) {
		if (obj == null) {
			throw new RegistrationProcessorUnCheckedException(PlatformErrorMessages.valueOf(errorMessage).getCode(),
					PlatformErrorMessages.valueOf(errorMessage).getMessage());
		}
		if (obj instanceof Collection) {
			List<?> genericList = new ArrayList<>((Collection<?>) obj);
			if (genericList.isEmpty()) {
				throw new RegistrationProcessorUnCheckedException(PlatformErrorMessages.valueOf(errorMessage).getCode(),
						PlatformErrorMessages.valueOf(errorMessage).getMessage());
			}
		}

	}

	private boolean sendToQueue(MosipQueue queue, String abisReqTextString, String abisQueueAddress,
			int messageTTL) throws RegistrationProcessorCheckedException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisMiddlewareStage::sendToQueue()::Entry");
		boolean isAddedToQueue;
		try {
			if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
				isAddedToQueue = mosipQueueManager.send(queue, abisReqTextString,
					abisQueueAddress, messageTTL);
			else
				isAddedToQueue = mosipQueueManager.send(queue, abisReqTextString.getBytes(),
					abisQueueAddress, messageTTL);

			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"AbisMiddlewareStage:: sent to abis queue ::" + abisReqTextString);

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", ExceptionUtils.getStackTrace(e));
			throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
					PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisMiddlewareStage::sendToQueue()::Exit");
		return isAddedToQueue;
	}

	private void updateAbisRequest(boolean isAddedToQueue, AbisRequestDto abisRequestDto,
			InternalRegistrationStatusDto internalRegDto) {
		AbisRequestEntity abisReqEntity = convertAbisRequestDtoToAbisRequestEntity(abisRequestDto);

		if (isAddedToQueue) {

			abisReqEntity.setStatusCode(AbisStatusCode.SENT.toString());
			abisReqEntity.setStatusComment(
					StatusUtil.INSERT_IDENTIFY_REQUEST_SUCCESS.getMessage() + abisRequestDto.getAbisAppCode());
		} else {
			abisReqEntity.setStatusCode(AbisStatusCode.FAILED.toString());
			abisReqEntity.setStatusComment(
					StatusUtil.INSERT_IDENTIFY_REQUEST_FAILED.getMessage() + abisRequestDto.getAbisAppCode());
			internalRegDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			internalRegDto.setStatusComment(
					StatusUtil.INSERT_IDENTIFY_REQUEST_FAILED.getMessage() + abisRequestDto.getAbisAppCode());
			internalRegDto.setSubStatusCode(StatusUtil.SYSTEM_EXCEPTION_OCCURED.getCode());
		}
		abisRequestRepositary.save(abisReqEntity);

	}

	private void updteAbisRequestProcessed(AbisCommonResponseDto abisCommonResponseDto,
			AbisRequestDto abisCommonRequestDto) {
		AbisRequestEntity abisReqEntity = new AbisRequestEntity();
		AbisRequestPKEntity abisReqPKEntity = new AbisRequestPKEntity();
		abisReqPKEntity.setId(abisCommonResponseDto.getRequestId());
		abisReqEntity.setId(abisReqPKEntity);
		abisReqEntity.setStatusCode(isInsertSuccess(abisCommonResponseDto) ? AbisStatusCode.PROCESSED.toString()
				: AbisStatusCode.FAILED.toString());
		abisReqEntity.setStatusComment(
				abisCommonResponseDto.getReturnValue().equalsIgnoreCase("1") ? StatusUtil.INSERT_IDENTIFY_RESPONSE_SUCCESS.getMessage()
						: io.mosip.registartion.processor.abis.middleware.constants.FailureReason.getValueFromKey(abisCommonResponseDto.getFailureReason()));
		abisReqEntity.setAbisAppCode(abisCommonRequestDto.getAbisAppCode());
		abisReqEntity.setRequestType(abisCommonRequestDto.getRequestType());
		abisReqEntity.setRequestDtimes(abisCommonRequestDto.getRequestDtimes());
		abisReqEntity.setReqBatchId(abisCommonRequestDto.getReqBatchId());
		abisReqEntity.setLangCode(abisCommonRequestDto.getLangCode());
		abisReqEntity.setCrBy(SYSTEM);
		abisReqEntity.setCrDtimes(abisCommonRequestDto.getCrDtimes());
		abisReqEntity.setBioRefId(abisCommonRequestDto.getBioRefId());
		abisReqEntity.setRefRegtrnId(abisCommonRequestDto.getRefRegtrnId());
		abisReqEntity.setReqText(abisCommonRequestDto.getReqText());
		abisRequestRepositary.save(abisReqEntity);
	}

	private AbisRequestEntity convertAbisRequestDtoToAbisRequestEntity(AbisRequestDto abisRequestDto) {
		AbisRequestEntity abisReqEntity = new AbisRequestEntity();
		AbisRequestPKEntity abisReqPKEntity = new AbisRequestPKEntity();
		abisReqPKEntity.setId(abisRequestDto.getId());
		abisReqEntity.setId(abisReqPKEntity);
		abisReqEntity.setAbisAppCode(abisRequestDto.getAbisAppCode());
		abisReqEntity.setBioRefId(abisRequestDto.getBioRefId());
		abisReqEntity.setCrBy(abisRequestDto.getCrBy());
		if (abisRequestDto.getCrDtimes() == null) {
			abisReqEntity.setCrDtimes(LocalDateTime.now(ZoneId.of("UTC")));
		} else {
			abisReqEntity.setCrDtimes(abisRequestDto.getCrDtimes());
		}
		abisReqEntity.setIsDeleted(false);
		abisReqEntity.setLangCode(abisRequestDto.getLangCode());
		abisReqEntity.setRefRegtrnId(abisRequestDto.getRefRegtrnId());
		abisReqEntity.setReqBatchId(abisRequestDto.getReqBatchId());
		abisReqEntity.setReqText(abisRequestDto.getReqText());
		abisReqEntity.setRequestDtimes(abisRequestDto.getRequestDtimes());
		abisReqEntity.setRequestType(abisRequestDto.getRequestType());
		abisReqEntity.setStatusCode(abisRequestDto.getStatusCode());
		abisReqEntity.setStatusComment(abisRequestDto.getStatusComment());
		abisReqEntity.setUpdBy(abisRequestDto.getUpdBy());
		abisReqEntity.setUpdDtimes(LocalDateTime.now(ZoneId.of("UTC")));
		return abisReqEntity;

	}

	private AbisResponseEntity convertAbisResponseDtoToAbisResponseEntity(AbisResponseDto abisResponseDto) {
		AbisResponseEntity abisResponseEntity = new AbisResponseEntity();
		AbisResponsePKEntity abisResponsePKEntity = new AbisResponsePKEntity();
		abisResponsePKEntity.setId(abisResponseDto.getId());
		abisResponseEntity.setId(abisResponsePKEntity);
		abisResponseEntity.setAbisRequest(abisResponseDto.getAbisRequest());
		abisResponseEntity.setRespText(abisResponseDto.getRespText());
		abisResponseEntity.setStatusCode(abisResponseDto.getStatusCode());
		abisResponseEntity.setStatusComment(abisResponseDto.getStatusComment());
		abisResponseEntity.setLangCode(abisResponseDto.getLangCode());
		abisResponseEntity.setCrBy(abisResponseDto.getCrBy());
		if (abisResponseDto.getCrDtimes() == null) {
			abisResponseEntity.setCrDtimes(LocalDateTime.now(ZoneId.of("UTC")));
		} else {
			abisResponseEntity.setCrDtimes(abisResponseDto.getCrDtimes());
		}
		abisResponseEntity.setUpdBy(abisResponseDto.getUpdBy());
		abisResponseEntity.setUpdDtimes(abisResponseDto.getUpdDtimes());
		abisResponseEntity.setIsDeleted(abisResponseDto.getIsDeleted());
		abisResponseEntity.setRespDtimes(LocalDateTime.now(ZoneId.of("UTC")));

		return abisResponseEntity;

	}

	private AbisResponseDto updateAbisResponseEntity(AbisCommonResponseDto abisCommonResponseDto, String response) {
		AbisResponseDto abisResponseDto = new AbisResponseDto();

		abisResponseDto.setId(RegistrationUtility.generateId());
		abisResponseDto.setRespText(response.getBytes());

		abisResponseDto.setStatusCode(isInsertSuccess(abisCommonResponseDto) ?
				AbisStatusCode.SUCCESS.toString() : AbisStatusCode.FAILED.toString());
		abisResponseDto.setStatusComment(io.mosip.registartion.processor.abis.middleware.constants.FailureReason.getValueFromKey(abisCommonResponseDto.getFailureReason()));
		abisResponseDto.setLangCode("eng");
		abisResponseDto.setCrBy(SYSTEM);
		abisResponseDto.setUpdBy(SYSTEM);
		abisResponseDto.setUpdDtimes(LocalDateTime.now(ZoneId.of("UTC")));
		abisResponseDto.setIsDeleted(false);
		abisResponseDto.setAbisRequest(abisCommonResponseDto.getRequestId());
		abisResponseRepositary.save(convertAbisResponseDtoToAbisResponseEntity(abisResponseDto));

		return abisResponseDto;
	}

	private void updateAbisResponseDetail(CandidatesDto candidatesDto, AbisResponseDto abisResponseDto,
			String bioRefId) {

		if (!candidatesDto.getReferenceId().equalsIgnoreCase(bioRefId)) {

				String candidateRegId = packetInfoDao.getRegIdByBioRefId(candidatesDto.getReferenceId().toLowerCase());
				if (candidateRegId == null || candidateRegId.isEmpty())
					return;
				AbisResponseDetEntity abisResponseDetEntity = new AbisResponseDetEntity();
				AbisResponseDetPKEntity abisResponseDetPKEntity = new AbisResponseDetPKEntity();
				abisResponseDetPKEntity.setAbisRespId(abisResponseDto.getId());
				abisResponseDetPKEntity.setMatchedBioRefId(candidatesDto.getReferenceId().toLowerCase());
				abisResponseDetEntity.setId(abisResponseDetPKEntity);
				abisResponseDetEntity.setCrBy(SYSTEM);
				abisResponseDetEntity.setUpdBy(SYSTEM);
				abisResponseDetEntity.setIsDeleted(false);
				abisResponseDetEntity.setCrDtimes(LocalDateTime.now(ZoneId.of("UTC")));
				abisResponseDetEntity.setUpdDtimes(LocalDateTime.now(ZoneId.of("UTC")));
				abisResponseDetailRepositary.save(abisResponseDetEntity);

		}

	}

	private String getLatestTransactionId(String registrationId, String process, int iteration, String workflowInstanceId) {
		RegistrationStatusEntity entity = registrationStatusDao.find(registrationId, process, iteration, workflowInstanceId);
		return entity != null ? entity.getLatestRegistrationTransactionId() : null;

	}

	private boolean checkAllIdentifyRequestsProcessed(String batchId) {
		List<String> batchStatus = packetInfoManager.getBatchStatusbyBatchId(batchId);
		if (batchStatus != null) {
			boolean flag = batchStatus.stream().allMatch(status -> status.equals(AbisStatusCode.PROCESSED.toString()));
			if (flag)
				return true;
		}
		return false;
	}

	private void sendToAbisHandler(MosipEventBus eventBus, List<String> bioRefId,
								   String regId, String regType, int iteration, String workflowInstanceId) {
		if (bioRefId != null) {
			MessageDTO messageDto = new MessageDTO();
			messageDto.setRid(regId);
			messageDto.setReg_type(regType);
			messageDto.setIsValid(Boolean.TRUE);
			messageDto.setInternalError(Boolean.FALSE);
			messageDto.setIteration(iteration);
			messageDto.setWorkflowInstanceId(workflowInstanceId);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"AbisMiddlewareStage::consumerListener()::sending to Abis handler");
			this.send(eventBus, MessageBusAddress.ABIS_MIDDLEWARE_BUS_OUT, messageDto);
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"AbisMiddlewareStage::consumerListener()::sent to Abis handler");
		}

	}

	private void saveCandiateDtos(CandidatesDto[] candidatesDtos, AbisResponseDto abisResponseDto, String bioRefId) {
		for (CandidatesDto candidatesDto : candidatesDtos) {
			updateAbisResponseDetail(candidatesDto, abisResponseDto, bioRefId);
		}
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

	/**
	 * If return value is 1 or failure reason is 10(which means ABIS has already processed the
	 * reference id) then return true
	 * @param abisInsertResponseDto
	 * @return boolean
	 */
	private boolean isInsertSuccess(AbisCommonResponseDto abisInsertResponseDto) {
		return abisInsertResponseDto.getReturnValue().equalsIgnoreCase("1")
				|| (abisInsertResponseDto.getFailureReason() != null
				&& abisInsertResponseDto.getFailureReason().equalsIgnoreCase("10"));
	}

}