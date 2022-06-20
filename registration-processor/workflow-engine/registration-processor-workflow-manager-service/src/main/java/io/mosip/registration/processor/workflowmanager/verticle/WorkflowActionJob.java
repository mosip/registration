package io.mosip.registration.processor.workflowmanager.verticle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.constants.WorkflowManagerConstants;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class WorkflowActionJob extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.workflow.action.job.";

	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowActionJob.class);

	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The environment. */
	@Autowired
	Environment environment;

	@Autowired
	WorkflowActionService workflowActionService;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	/** The fetch size. */
	@Value("${mosip.regproc.workflow-manager.action.job.fetchsize}")
	private Integer fetchSize;

	/** The is transaction successful. */
	boolean isTransactionSuccessful;

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration id. */
	private String registrationId = "";

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/** The port. */
	@Value("${server.port}")
	private String port;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl);
		deployScheduler(mosipEventBus.getEventbus());

	}

	/**
	 * This method deploys the chime scheduler
	 *
	 * @param vertx the vertx
	 */
	private void deployScheduler(Vertx vertx) {
		vertx.deployVerticle(WorkflowManagerConstants.CEYLON_SCHEDULER, this::schedulerResult);
	}

	public void schedulerResult(AsyncResult<String> res) {
		if (res.succeeded()) {
			regProcLogger.info("WorkflowActionJob::schedular()::deployed");
			cronScheduling(vertx);
		} else {
			regProcLogger.error("WorkflowActionJob::schedular()::deployment failure");
		}
	}

	/**
	 * This method does the cron scheduling by fetchin cron expression from config
	 * server
	 *
	 * @param vertx the vertx
	 */
	private void cronScheduling(Vertx vertx) {

		EventBus eventBus = vertx.eventBus();
		// listen the timer events
		eventBus.consumer((WorkflowManagerConstants.TIMER_EVENT), message -> {

			process(new MessageDTO());
		});

		// description of timers
		JsonObject timer = (new JsonObject())
				.put(WorkflowManagerConstants.TYPE, environment.getProperty(WorkflowManagerConstants.TYPE_VALUE))
				.put(WorkflowManagerConstants.SECONDS, environment.getProperty(WorkflowManagerConstants.SECONDS_VALUE))
				.put(WorkflowManagerConstants.MINUTES, environment.getProperty(WorkflowManagerConstants.MINUTES_VALUE))
				.put(WorkflowManagerConstants.HOURS, environment.getProperty(WorkflowManagerConstants.HOURS_VALUE))
				.put(WorkflowManagerConstants.DAY_OF_MONTH,
						environment.getProperty(WorkflowManagerConstants.DAY_OF_MONTH_VALUE))
				.put(WorkflowManagerConstants.MONTHS, environment.getProperty(WorkflowManagerConstants.MONTHS_VALUE))
				.put(WorkflowManagerConstants.DAYS_OF_WEEK,
						environment.getProperty(WorkflowManagerConstants.DAYS_OF_WEEK_VALUE));

		// create scheduler
		eventBus.send(WorkflowManagerConstants.CHIME,
				(new JsonObject()).put(WorkflowManagerConstants.OPERATION, WorkflowManagerConstants.OPERATION_VALUE)
						.put(WorkflowManagerConstants.NAME, WorkflowManagerConstants.NAME_VALUE)
						.put(WorkflowManagerConstants.DESCRIPTION, timer),
				ar -> {
					if (ar.succeeded()) {
						regProcLogger.info("WorkflowActionJob::schedular()::started");
					} else {
						regProcLogger.error("WorkflowActionJob::schedular()::failed {} ", ar.cause());
						vertx.close();
					}
				});

	}


	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), null, null));
		this.createServer(router.getRouter(), Integer.parseInt(port));
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
		LogDescription description = new LogDescription();

		regProcLogger.debug("WorkflowActionJob::process()::entry");
		registrationId=object.getRid();
		try {
			processActionablePausedPackets();
			isTransactionSuccessful = true;

		} catch (TablenotAccessibleException e) {
			isTransactionSuccessful = false;
			object.setInternalError(Boolean.TRUE);
			description.setMessage(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(
					"Error in  WorkflowActionJob:process  {} {} {}",
					PlatformErrorMessages.RPR_RGS_REGISTRATION_TABLE_NOT_ACCESSIBLE.getMessage(), e.getMessage(),
					ExceptionUtils.getStackTrace(e));
		} catch (WorkflowActionException e) {
			isTransactionSuccessful = false;
			object.setInternalError(Boolean.TRUE);
			description.setMessage(e.getMessage());
			description.setCode(e.getErrorCode());
			regProcLogger.error("Error in  WorkflowActionJob:process  {} {}", e.getMessage(),
					ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			isTransactionSuccessful = false;
			description.setMessage(PlatformErrorMessages.RPR_WORKFLOW_ACTION_JOB_FAILED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_WORKFLOW_ACTION_JOB_FAILED.getCode());
			regProcLogger.error("Error in  WorkflowActionJob:process  {} {} {}",
					PlatformErrorMessages.RPR_WORKFLOW_ACTION_JOB_FAILED.getMessage(), e.getMessage(),
					ExceptionUtils.getStackTrace(e));
			object.setInternalError(Boolean.TRUE);

		} finally {

			if (isTransactionSuccessful) {
				description.setCode(PlatformSuccessMessages.RPR_WORKFLOW_ACTION_JOB_SUCCESS.getCode());
				description.setMessage(PlatformSuccessMessages.RPR_WORKFLOW_ACTION_JOB_SUCCESS.getMessage());
			}
			regProcLogger.info(description.getMessage());
			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

			/** Module-Id can be Both Success/Error code */
			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_WORKFLOW_ACTION_JOB_SUCCESS.getCode()
					: description.getCode();
			String moduleName = ModuleName.WORKFLOW_ACTION_JOB.toString();
			auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
					moduleId, moduleName, registrationId);
		}

		return object;
	}

	public void processActionablePausedPackets() throws WorkflowActionException {
		List<InternalRegistrationStatusDto> actionablePausedPacketsList = registrationStatusService.getActionablePausedPackets(fetchSize);
		Map<String, List<InternalRegistrationStatusDto>> defaultResumeActionPacketIdsMap = new HashMap<>();
		for (InternalRegistrationStatusDto dto : actionablePausedPacketsList) {
			if (defaultResumeActionPacketIdsMap.containsKey(dto.getDefaultResumeAction())) {
				List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = defaultResumeActionPacketIdsMap
						.get(dto.getDefaultResumeAction());
				internalRegistrationStatusDtos.add(dto);
				defaultResumeActionPacketIdsMap.put(dto.getDefaultResumeAction(), internalRegistrationStatusDtos);
			} else {
				List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
				internalRegistrationStatusDtos.add(dto);
				defaultResumeActionPacketIdsMap.put(dto.getDefaultResumeAction(), internalRegistrationStatusDtos);
			}
		}
		for (Entry<String, List<InternalRegistrationStatusDto>> entry : defaultResumeActionPacketIdsMap.entrySet()) {
			workflowActionService.processWorkflowAction(entry.getValue(), entry.getKey());
		}

	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}
}
