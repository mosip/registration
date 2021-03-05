package io.mosip.registration.processor.reprocessor.stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
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
				this.postUrl(mosipEventBus.getEventbus(), MessageBusAddress.WORKFLOW_EVENT_UPDATE_ADDRESS, null));
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
				WorkflowEventDTO workFlowEventDTO = jsonObject.mapTo(WorkflowEventDTO.class);
				MessageDTO result = process(workFlowEventDTO);
				future.complete(result);
			}, false, handler);
			MDC.clear();
		});
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"ReprocessorStage::process()::entry");
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		WorkflowEventDTO workFlowEventDto = (WorkflowEventDTO) object;
		String registrationId = workFlowEventDto.getRid();
		try {

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
					.getRegistrationStatus(registrationId);

		registrationStatusDto.setStatusCode(workFlowEventDto.getStatusCode());
		registrationStatusDto.setStatusComment(workFlowEventDto.getStatusComment());
		registrationStatusDto.setDefaultResumeAction(workFlowEventDto.getDefaultResumeAction());
		LocalDateTime resumeTimeStamp = DateUtils.parseToLocalDateTime(workFlowEventDto.getResumeTimestamp());
		LocalDateTime updateTimeStamp = DateUtils.parseToLocalDateTime(workFlowEventDto.getEventTimestamp());
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
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "ReprocessorStage::process()::exit");

		} catch (DateTimeParseException e) {
			updateDTOsAndLogError(description, registrationId, PlatformErrorMessages.RPR_WFE_DATE_TIME_EXCEPTION, e);

		} catch (TablenotAccessibleException e) {

			updateDTOsAndLogError(description, registrationId,
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE, e);

		} catch (Exception e) {

			updateDTOsAndLogError(description, registrationId, PlatformErrorMessages.RPR_WORKFLOW_EVENT_UPDATE_FAILED,
					e);

		} finally {
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, description.getMessage());

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
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				description.getCode() + " -- " + registrationId,
				platformErrorMessages.getMessage() + e.getMessage()
						+ ExceptionUtils.getStackTrace(e));
	}

}
