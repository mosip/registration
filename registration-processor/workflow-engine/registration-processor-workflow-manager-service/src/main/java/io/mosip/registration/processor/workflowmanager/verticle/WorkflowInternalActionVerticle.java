package io.mosip.registration.processor.workflowmanager.verticle;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.exception.WorkflowInternalActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;
import io.vertx.core.json.JsonObject;
@Component
public class WorkflowInternalActionVerticle extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.reprocessor.";

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowInternalActionVerticle.class);

	/** The registration status service. */
	@Autowired
    @Qualifier("workflowStatusServiceImpl")
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The audit log request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.workflow-manager.internal.action.server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.workflow-manager.internal.action.eventbus.port}")
	private String eventBusPort;

	@Autowired
	MosipRouter router;

	private MosipEventBus mosipEventBus = null;
	
	public static String MODULE_NAME = ModuleName.WORKFLOW_INTERNAL_ACTION.toString();

	public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode();

	/** The web sub util. */
	@Autowired
	WebSubUtil webSubUtil;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.WORKFLOW_INTERNAL_ACTION_ADDRESS, 0);
	}

	@Override
	public void start() {
		router.setRoute(
				this.postUrl(getVertx(), MessageBusAddress.WORKFLOW_INTERNAL_ACTION_ADDRESS, null));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	@Override
	public Integer getEventBusPort() {
		return Integer.parseInt(eventBusPort);
	}
	@Override
	public void consume(MosipEventBus mosipEventBus, MessageBusAddress fromAddress, 
			long messageExpiryTimeLimit) {
		mosipEventBus.consume(fromAddress, (msg, handler) -> {

			Map<String, String> mdc = MDC.getCopyOfContextMap();
			vertx.executeBlocking(future -> {
				MDC.setContextMap(mdc);
				JsonObject jsonObject = (JsonObject) msg.getBody();
				WorkflowInternalActionDTO workflowEventDTO = jsonObject.mapTo(WorkflowInternalActionDTO.class);
				MessageDTO result = process(workflowEventDTO);
				future.complete(result);
			}, false, handler);
			MDC.clear();
		});
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		WorkflowInternalActionDTO workflowInternalActionDTO = (WorkflowInternalActionDTO) object;
		String registrationId = workflowInternalActionDTO.getRid();
		regProcLogger.debug("WorkflowInternalActionVerticle called for registration id {}", registrationId);
		WorkflowInternalActionCode workflowInternalActionCode = null;
		try {
			workflowInternalActionCode = WorkflowInternalActionCode.valueOf(workflowInternalActionDTO.getActionCode());
			switch (workflowInternalActionCode) {
			case MARK_AS_PAUSED:
				processPacketForPaused(workflowInternalActionDTO);
				break;
			case COMPLETE_AS_PROCESSED:
				processCompleteAsProcessed(workflowInternalActionDTO);
				break;
			case COMPLETE_AS_REJECTED:
				processCompleteAsRejected(workflowInternalActionDTO);
				break;
			case COMPLETE_AS_FAILED:
				processCompleteAsFailed(workflowInternalActionDTO);
				break;
			case MARK_AS_REPROCESS:
				processMarkAsReprocess(workflowInternalActionDTO);
				break;
			default:
				throw new WorkflowInternalActionException(
						PlatformErrorMessages.RPR_WIA_UNKNOWN_WORKFLOW_ACTION.getCode(),
						PlatformErrorMessages.RPR_WIA_UNKNOWN_WORKFLOW_ACTION.getMessage());

			}
			isTransactionSuccessful = true;
			description.setMessage(PlatformSuccessMessages.RPR_WORKFLOW_INTERNAL_ACTION_SUCCESS.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
			regProcLogger.debug("WorkflowInternalActionVerticle call ended for registration id {}", registrationId);

		} catch (DateTimeParseException e) {
			updateDTOsAndLogError(description, registrationId, PlatformErrorMessages.RPR_WIA_DATE_TIME_EXCEPTION, e);

		} catch (TablenotAccessibleException e) {

			updateDTOsAndLogError(description, registrationId,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);

		} catch (WorkflowInternalActionException e) {

			updateDTOsAndLogError(description, registrationId, PlatformErrorMessages.RPR_WIA_UNKNOWN_WORKFLOW_ACTION,
					e);

		} catch (Exception e) {

			updateDTOsAndLogError(description, registrationId,
					PlatformErrorMessages.RPR_WORKFLOW_INTERNAL_ACTION_FAILED,
					e);

		} finally {
			regProcLogger.info("WorkflowEventUpdateVerticle status for registration id {} {}", registrationId,
					description.getMessage());

			updateAudit(description, isTransactionSuccessful, registrationId);
		}
		return object;

	}

	private void processMarkAsReprocess(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(workflowInternalActionDTO.getRid());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REPROCESS.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, MODULE_ID, MODULE_NAME);
	}

	private void processCompleteAsFailed(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(workflowInternalActionDTO.getRid());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, MODULE_ID, MODULE_NAME);
		sendWorkflowCompletedWebSubEvent(registrationStatusDto);
	}

	private void processCompleteAsRejected(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(workflowInternalActionDTO.getRid());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, MODULE_ID, MODULE_NAME);
		sendWorkflowCompletedWebSubEvent(registrationStatusDto);
	}

	private void processCompleteAsProcessed(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(workflowInternalActionDTO.getRid());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, MODULE_ID, MODULE_NAME);
		sendWorkflowCompletedWebSubEvent(registrationStatusDto);
	}

	private void processPacketForPaused(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(workflowInternalActionDTO.getRid());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PAUSED.toString());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setDefaultResumeAction(workflowInternalActionDTO.getDefaultResumeAction());
		if (workflowInternalActionDTO.getResumeTimestamp() != null) {
			LocalDateTime resumeTimeStamp = DateUtils
					.parseToLocalDateTime(workflowInternalActionDTO.getResumeTimestamp());
			registrationStatusDto.setResumeTimeStamp(resumeTimeStamp);
		}
		registrationStatusDto.setUpdatedBy(USER);
		registrationStatusDto.setResumeRemoveTags(workflowInternalActionDTO.getResumeRemoveTags());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatus(registrationStatusDto, MODULE_ID, MODULE_NAME);

	}

	private void updateAudit(LogDescription description, boolean isTransactionSuccessful, String registrationId) {
		String moduleId = isTransactionSuccessful
				? PlatformSuccessMessages.RPR_WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode()
				: description.getCode();

		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString()
				: EventId.RPR_405.toString();
		String eventName = isTransactionSuccessful ? EventName.UPDATE.toString()
				: EventName.EXCEPTION.toString();
		String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString()
				: EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
				moduleId, MODULE_NAME, registrationId);
	}

	private void updateDTOsAndLogError(LogDescription description, String registrationId,
			PlatformErrorMessages platformErrorMessages, Exception e) {
		description.setMessage(platformErrorMessages.getMessage());
		description.setCode(platformErrorMessages.getCode());
		regProcLogger.error("Error in  WorkflowEventUpdateVerticle  for registration id {} {} {} {}", registrationId,
				platformErrorMessages.getMessage(), e.getMessage(), ExceptionUtils.getStackTrace(e));

	}
	
	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	private void sendWorkflowCompletedWebSubEvent(InternalRegistrationStatusDto registrationStatusDto) {
		WorkflowCompletedEventDTO workflowCompletedEventDTO = new WorkflowCompletedEventDTO();
		workflowCompletedEventDTO.setInstanceId(registrationStatusDto.getRegistrationId());
		workflowCompletedEventDTO.setResultCode(registrationStatusDto.getStatusCode());
		workflowCompletedEventDTO.setWorkflowType(registrationStatusDto.getRegistrationType());
		if (registrationStatusDto.getStatusCode().equalsIgnoreCase(RegistrationStatusCode.REJECTED.toString())) {
			workflowCompletedEventDTO.setErrorCode(RegistrationExceptionTypeCode.PACKET_REJECTED.name());
		}
		if (registrationStatusDto.getStatusCode().equalsIgnoreCase(RegistrationStatusCode.FAILED.toString())) {
			workflowCompletedEventDTO.setErrorCode(RegistrationExceptionTypeCode.PACKET_FAILED.name());
		}

		webSubUtil.publishEvent(workflowCompletedEventDTO);

	}

}
