package io.mosip.registration.processor.reprocessor.stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.abstractverticle.WorkflowEventDTO;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.json.JsonObject;
@Component
public class WorkflowEventUpdateVerticle extends MosipVerticleAPIManager {

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowEventUpdateVerticle.class);

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The audit log request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.workflow.eventupdate.server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.workflow.eventupdate.eventbus.port}")
	private String eventBusPort;

	@Autowired
	MosipRouter router;

	private MosipEventBus mosipEventBus = null;
	
	public static String MODULE_NAME = ModuleName.WORKFLOW_EVENT_UPDATE.toString();

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.WORKFLOW_EVENT_UPDATE_ADDRESS);
	}

	@Override
	public void start() {
		router.setRoute(
				this.postUrl(getVertx(), MessageBusAddress.WORKFLOW_EVENT_UPDATE_ADDRESS, null));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	@Override
	public Integer getEventBusPort() {
		return Integer.parseInt(eventBusPort);
	}
	@Override
	public void consume(MosipEventBus mosipEventBus, MessageBusAddress fromAddress) {
		mosipEventBus.consume(fromAddress, (msg, handler) -> {

			Map<String, String> mdc = MDC.getCopyOfContextMap();
			vertx.executeBlocking(future -> {
				MDC.setContextMap(mdc);
				JsonObject jsonObject = (JsonObject) msg.getBody();
				WorkflowEventDTO workflowEventDTO = jsonObject.mapTo(WorkflowEventDTO.class);
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
		WorkflowEventDTO workflowEventDto = (WorkflowEventDTO) object;
		String registrationId = workflowEventDto.getRid();
		regProcLogger.debug("WorkflowEventUpdateVerticle called for registration id {}", registrationId);
		try {

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
					.getRegistrationStatus(registrationId);

		registrationStatusDto.setStatusCode(workflowEventDto.getStatusCode());
		registrationStatusDto.setStatusComment(workflowEventDto.getStatusComment());
		registrationStatusDto.setDefaultResumeAction(workflowEventDto.getDefaultResumeAction());
		LocalDateTime resumeTimeStamp = DateUtils.parseToLocalDateTime(workflowEventDto.getResumeTimestamp());
		LocalDateTime updateTimeStamp = DateUtils.parseToLocalDateTime(workflowEventDto.getEventTimestamp());
		registrationStatusDto.setResumeTimeStamp(resumeTimeStamp);
		registrationStatusDto.setUpdateDateTime(updateTimeStamp);
		registrationStatusDto.setUpdatedBy(USER);

			String moduleId = PlatformSuccessMessages.RPR_WORKFLOW_EVENT_UPDATE_SUCCESS.getCode();
			String moduleName = ModuleName.WORKFLOW_EVENT_UPDATE.toString();

			registrationStatusService.updateRegistrationStatusForWorkflow(registrationStatusDto, moduleId, moduleName);

			isTransactionSuccessful = true;
			description.setMessage(PlatformSuccessMessages.RPR_WORKFLOW_EVENT_UPDATE_SUCCESS.getMessage());
			description
					.setCode(PlatformSuccessMessages.RPR_WORKFLOW_EVENT_UPDATE_SUCCESS.getCode());
			regProcLogger.debug("WorkflowEventUpdateVerticle call ended for registration id {}", registrationId);

		} catch (DateTimeParseException e) {
			updateDTOsAndLogError(description, registrationId, PlatformErrorMessages.RPR_WFE_DATE_TIME_EXCEPTION, e);

		} catch (TablenotAccessibleException e) {

			updateDTOsAndLogError(description, registrationId,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);

		} catch (Exception e) {

			updateDTOsAndLogError(description, registrationId, PlatformErrorMessages.RPR_WORKFLOW_EVENT_UPDATE_FAILED,
					e);

		} finally {
			regProcLogger.info("WorkflowEventUpdateVerticle status for registration id {} {}", registrationId,
					description.getMessage());

			updateAudit(description, isTransactionSuccessful, registrationId);
		}
		return object;

	}

	private void updateAudit(LogDescription description, boolean isTransactionSuccessful, String registrationId) {
		String moduleId = isTransactionSuccessful
				? PlatformSuccessMessages.RPR_WORKFLOW_EVENT_UPDATE_SUCCESS.getCode()
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

}
