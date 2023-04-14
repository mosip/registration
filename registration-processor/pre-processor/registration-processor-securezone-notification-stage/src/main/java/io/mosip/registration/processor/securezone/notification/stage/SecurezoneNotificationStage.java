package io.mosip.registration.processor.securezone.notification.stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.entity.SyncRegistrationEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.securezone.notification.config",
		"io.mosip.registration.processor.packet.manager.config", "io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", "io.mosip.registration.processor.core.kernel.beans" })
public class SecurezoneNotificationStage extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.securezone.notification.";

	/**
	 * The reg proc logger.
	 */
	private static final Logger regProcLogger = RegProcessorLogger.getLogger(SecurezoneNotificationStage.class);

	/**
	 * The cluster url.
	 */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/**
	 * worker pool size.
	 */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/**
	 * The mosip event bus.
	 */
	private MosipEventBus mosipEventBus;

	@Value("${securezone.routing.enabled:true}")
	private boolean routingEnabled;

	/**
	 * After this time intervel, message should be considered as expired (In
	 * seconds).
	 */
	@Value("${mosip.regproc.securezone.notification.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	@Value("#{T(java.util.Arrays).asList('${registration.processor.main-processes:}')}")
	private List<String> mainProcesses;

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/**
	 * Mosip router for APIs
	 */
	@Autowired
	private MosipRouter router;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	@Autowired
	private SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> syncRegistrationService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	@Autowired
	private RegistrationExceptionMapperUtil registrationStatusMapperUtil;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		this.mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.SECUREZONE_NOTIFICATION_IN,
				MessageBusAddress.SECUREZONE_NOTIFICATION_OUT, messageExpiryTimeLimit);
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(vertx, MessageBusAddress.SECUREZONE_NOTIFICATION_IN,
				MessageBusAddress.SECUREZONE_NOTIFICATION_OUT));
		this.routes(router);
		this.createServer(router.getRouter(), getPort());
	}

	/**
	 * contains all the routes in this stage
	 *
	 * @param router
	 */
	private void routes(MosipRouter router) {
		router.post(getServletPath() + "/notification");
		router.handler(this::processURL, this::failure);
	}

	/**
	 * method to process the context received.
	 *
	 * @param ctx the ctx
	 */
	public void processURL(RoutingContext ctx) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"SecurezoneNotificationStage::processURL()::entry");

		MessageDTO messageDTO = new MessageDTO();
		try {
			JsonObject obj = ctx.getBodyAsJson();
			messageDTO.setMessageBusAddress(MessageBusAddress.SECUREZONE_NOTIFICATION_IN);
			messageDTO.setInternalError(Boolean.FALSE);
			messageDTO.setRid(obj.getString("rid"));
			messageDTO.setReg_type(obj.getString("reg_type"));
			messageDTO.setIsValid(obj.getBoolean("isValid"));
			messageDTO.setSource(obj.getString("source"));
			messageDTO.setIteration(obj.getInteger("iteration"));
			messageDTO.setWorkflowInstanceId(obj.getString("workflowInstanceId"));
			MessageDTO result = process(messageDTO);
			if (result.getIsValid()) {
				sendMessage(result);
				this.setResponse(ctx,
						"Packet with registrationId '" + result.getRid() + "' has been forwarded to next stage");

				regProcLogger.info(obj.getString("rid"),
						"Packet with registrationId '" + result.getRid() + "' has been forwarded to next stage",
						null, null);
			} else {
				this.setResponse(ctx, "Packet with registrationId '" + obj.getString("rid")
						+ "' has not been uploaded to file System");

				regProcLogger.info(obj.getString("rid"),
						"Packet with registrationId '" + result.getRid() + "' has not been uploaded to file System",
						null, null);
			}
		} catch (Exception e) {
			ctx.fail(e);
		}
	}

	/**
	 * This is for failure handler
	 *
	 * @param routingContext
	 */
	private void failure(RoutingContext routingContext) {
		this.setResponse(routingContext, routingContext.failure().getMessage());
	}

	/**
	 * sends messageDTO to camel bridge.
	 *
	 * @param messageDTO the message DTO
	 */
	public void sendMessage(MessageDTO messageDTO) {
		if (routingEnabled)
			this.send(this.mosipEventBus, MessageBusAddress.SECUREZONE_NOTIFICATION_OUT, messageDTO);
	}

	@Override
	public MessageDTO process(MessageDTO messageDTO) {
		InternalRegistrationStatusDto registrationStatusDto = new InternalRegistrationStatusDto();
		TrimExceptionMessage trimMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		try {
			registrationStatusDto = registrationStatusService.getRegistrationStatus(messageDTO.getRid(),
					messageDTO.getReg_type(), messageDTO.getIteration(), messageDTO.getWorkflowInstanceId());

			boolean isDuplicatePacket = false;
			if (registrationStatusDto != null) {
				registrationStatusDto.setLatestTransactionTypeCode(
						RegistrationTransactionTypeCode.SECUREZONE_NOTIFICATION.toString());
				registrationStatusDto.setRegistrationStageName(getStageName());
				isDuplicatePacket = isDuplicatePacketForSameReqId(messageDTO);
			} else {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
						"No records found in registration table for reg id - " + messageDTO.getRid());
			}

			if (!isDuplicatePacket && registrationStatusDto != null
					&& messageDTO.getRid().equalsIgnoreCase(registrationStatusDto.getRegistrationId())) {
				registrationStatusDto
						.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());
				messageDTO.setIsValid(Boolean.TRUE);
				registrationStatusDto.setStatusComment(StatusUtil.NOTIFICATION_RECEIVED_TO_SECUREZONE.getMessage());
				registrationStatusDto.setSubStatusCode(StatusUtil.NOTIFICATION_RECEIVED_TO_SECUREZONE.getCode());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());

				isTransactionSuccessful = true;
				description.setMessage(PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getMessage() + " -- "
						+ messageDTO.getRid());
				description.setCode(PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getCode());

				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
						description.getCode() + description.getMessage());
			} else if (isDuplicatePacket) {
				isTransactionSuccessful = false;
				messageDTO.setIsValid(Boolean.FALSE);
				registrationStatusDto.setSubStatusCode(StatusUtil.NOTIFICATION_RECEIVED_TO_SECUREZONE.getCode());
				description.setMessage(PlatformErrorMessages.RPR_SECUREZONE_DUPLICATE_PACKET.getMessage());
				description.setCode(PlatformErrorMessages.RPR_SECUREZONE_DUPLICATE_PACKET.getCode());
				registrationStatusDto
						.setStatusComment(PlatformErrorMessages.RPR_SECUREZONE_DUPLICATE_PACKET.getMessage());
				registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.name());
				registrationStatusDto.setLatestTransactionStatusCode(RegistrationStatusCode.REJECTED.name());
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
						PlatformErrorMessages.RPR_SECUREZONE_DUPLICATE_PACKET.getMessage());
			} else {
				isTransactionSuccessful = false;
				messageDTO.setIsValid(Boolean.FALSE);
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
						"Transaction failed. RID not found in registration table.");
			}
		} catch (TablenotAccessibleException e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(
					trimMessage.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(
					registrationStatusMapperUtil.getStatusCode(RegistrationExceptionTypeCode.DATA_ACCESS_EXCEPTION));
			description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					description.getCode() + " -- " + messageDTO.getRid(),
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage() + e.getMessage()
							+ ExceptionUtils.getStackTrace(e));
			messageDTO.setInternalError(Boolean.TRUE);
			messageDTO.setRid(registrationStatusDto.getRegistrationId());
		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					messageDTO.toString(), ExceptionUtils.getStackTrace(e));
			messageDTO.setIsValid(Boolean.FALSE);
			description.setCode(PlatformErrorMessages.RPR_SECUREZONE_FAILURE.getCode());
			description.setMessage(PlatformErrorMessages.RPR_SECUREZONE_FAILURE.getMessage());
		} finally {
			if (messageDTO.getInternalError() != null && messageDTO.getInternalError()) {
				registrationStatusDto.setUpdatedBy(USER);
				int retryCount = registrationStatusDto.getRetryCount() != null
						? registrationStatusDto.getRetryCount() + 1
						: 1;
				registrationStatusDto.setRetryCount(retryCount);
				updateErrorFlags(registrationStatusDto, messageDTO);
			}
			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getCode()
					: description.getCode();
			String moduleName = ModuleName.SECUREZONE_NOTIFICATION.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			if (isTransactionSuccessful)
				description.setMessage(PlatformSuccessMessages.RPR_SEZ_SECUREZONE_NOTIFICATION.getMessage());
			String eventId = isTransactionSuccessful ? EventId.RPR_401.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.GET.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, messageDTO.getRid());
		}
		return messageDTO;
	}

	private boolean isDuplicatePacketForSameReqId(MessageDTO messageDTO) {
		boolean isDuplicate = false;
		SyncRegistrationEntity entity = syncRegistrationService
				.findByWorkflowInstanceId(messageDTO.getWorkflowInstanceId());

		if (entity.getAdditionalInfoReqId() == null && mainProcesses.contains(entity.getRegistrationType())) {
			// find all main process records for same registrationId.
			List<SyncRegistrationEntity> entities = syncRegistrationService
					.findByRegistrationId(entity.getRegistrationId());
			List<SyncRegistrationEntity> mainProcessEntities = entities.stream()
					.filter(e -> e.getAdditionalInfoReqId() == null).collect(Collectors.toList());
			isDuplicate = checkDuplicates(messageDTO, mainProcessEntities);
		} else {
			// find all records for same additionalInfoReqId.
			List<SyncRegistrationEntity> entities = syncRegistrationService
					.findByAdditionalInfoReqId(entity.getAdditionalInfoReqId());
			// if multiple records are present for same additionalInfoReqId then check in
			// registration table how many packets are received
			isDuplicate = checkDuplicates(messageDTO, entities);
		}
		return isDuplicate;
	}

	private boolean checkDuplicates(MessageDTO messageDTO, List<SyncRegistrationEntity> entities) {
		if (!CollectionUtils.isEmpty(entities) && entities.size() > 1) {
			List<String> workflowInstanceIds = entities.stream().map(e -> e.getWorkflowInstanceId())
					.collect(Collectors.toList());
			List<InternalRegistrationStatusDto> dtos = new ArrayList<>();
			for (String workflowInstanceId : workflowInstanceIds) {
				InternalRegistrationStatusDto dto = registrationStatusService.getRegistrationStatus(messageDTO.getRid(),
						messageDTO.getReg_type(), messageDTO.getIteration(), workflowInstanceId);
				if (dto != null)
					dtos.add(dto);
			}
			Optional<InternalRegistrationStatusDto> currentPacketOptional = dtos.stream()
					.filter(d -> d.getWorkflowInstanceId() != null
							&& d.getWorkflowInstanceId().equalsIgnoreCase(messageDTO.getWorkflowInstanceId()))
					.findAny();
			if (currentPacketOptional.isPresent()) {
				InternalRegistrationStatusDto currentPacket = currentPacketOptional.get();
				// remove current packet from list so that it contains only other packets
				// received for same additionalInfoReqId
				dtos.remove(currentPacket);
				for (InternalRegistrationStatusDto otherPacket : dtos) {
					if (otherPacket.getCreateDateTime().isBefore(currentPacket.getCreateDateTime())) {
						messageDTO.setIsValid(Boolean.FALSE);
						regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
								LoggerFileConstant.REGISTRATIONID.toString(), messageDTO.getRid(),
								"Packet already received for same registrationId.");
						return true;
					}
				}
			}
		}
		return false;
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
