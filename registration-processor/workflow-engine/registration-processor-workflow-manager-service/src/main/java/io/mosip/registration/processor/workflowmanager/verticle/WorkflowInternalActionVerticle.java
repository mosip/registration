package io.mosip.registration.processor.workflowmanager.verticle;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
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
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.code.WorkflowActionCode;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.WorkflowInternalActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.AdditionalInfoRequestDto;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.core.workflow.dto.WorkflowPausedForAdditionalInfoEventDTO;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.AdditionalInfoRequestService;
import io.mosip.registration.processor.status.service.AnonymousProfileService;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.service.WorkflowActionService;
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

    @Value("${mosip.regproc.workflow-manager.internal.action.max-allowed-iteration}")
    private int defaultMaxAllowedIteration;
    
    @Value("${mosip.anonymous.profile.eventbus.address}")
	private String anonymousProfileBusAddress;

	@Autowired
	MosipRouter router;

	@Autowired
	AdditionalInfoRequestService additionalInfoRequestService;

	@Autowired
	private WorkflowActionService workflowActionService;

	@Autowired
	private AnonymousProfileService anonymousProfileService;

	private MosipEventBus mosipEventBus = null;
	
	public static String MODULE_NAME = ModuleName.WORKFLOW_INTERNAL_ACTION.toString();

	public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode();

	/** The web sub util. */
	@Autowired
	private WebSubUtil webSubUtil;

	@Autowired
	private Utilities utility;
	
	@Autowired
	private IdSchemaUtil idSchemaUtil;

	@Autowired
	private PacketManagerService packetManagerService;

	@Autowired
	private Environment env;

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
			case PAUSE_AND_REQUEST_ADDITIONAL_INFO:
				processPauseAndRequestAdditionalInfo(workflowInternalActionDTO);
				break;
			case RESTART_PARENT_FLOW:
				processRestartParentFlow(workflowInternalActionDTO);
				break;
			case COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW:
				processCompleteAsRejectedWithoutParentFlow(workflowInternalActionDTO);
				break;
			case ANONYMOUS_PROFILE:
				processAnonymousProfile(workflowInternalActionDTO);
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

	private void processAnonymousProfile(WorkflowInternalActionDTO workflowInternalActionDTO)
			throws IOException, JSONException, BaseCheckedException {

		String json = null;
		String registrationId = workflowInternalActionDTO.getRid();
		String registrationType = workflowInternalActionDTO.getReg_type();

		regProcLogger.info("processAnonymousProfile called for registration id {}", registrationId);

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				registrationId, registrationType, workflowInternalActionDTO.getIteration(),
				workflowInternalActionDTO.getWorkflowInstanceId());
		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String idSchemaVersionValue = JsonUtil.getJSONValue(JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.IDSCHEMA_VERSION), MappingJsonConstants.VALUE);
		String schemaVersion = packetManagerService.getFieldByMappingJsonKey(registrationId,
				idSchemaVersionValue, registrationType, ProviderStageName.WORKFLOW_MANAGER);
		Map<String,String> fieldTypeMap = idSchemaUtil.getIdSchemaFieldTypes(
				Double.parseDouble(schemaVersion));
		Map<String, String> fieldMap = packetManagerService.getFields(registrationId,
				idSchemaUtil.getDefaultFields(Double.valueOf(schemaVersion)), registrationType,
				ProviderStageName.WORKFLOW_MANAGER);
		Map<String, String> metaInfoMap = packetManagerService.getMetaInfo(registrationId, registrationType,
				ProviderStageName.WORKFLOW_MANAGER);
		BiometricRecord biometricRecord = packetManagerService.getBiometrics(registrationId,
				MappingJsonConstants.INDIVIDUAL_BIOMETRICS, registrationType, ProviderStageName.WORKFLOW_MANAGER);
		json = anonymousProfileService.buildJsonStringFromPacketInfo(biometricRecord, fieldMap, fieldTypeMap,
				metaInfoMap, registrationStatusDto.getStatusCode(), registrationStatusDto.getRegistrationStageName());
		anonymousProfileService.saveAnonymousProfile(registrationId, registrationStatusDto.getRegistrationStageName(), json);
		
		this.send(this.mosipEventBus, new MessageBusAddress(anonymousProfileBusAddress), workflowInternalActionDTO);

		regProcLogger.info("processAnonymousProfile ended for registration id {}", registrationId);
	}

	private void processCompleteAsRejectedWithoutParentFlow(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
		registrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
	}

	private void processMarkAsReprocess(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
			.getRegistrationStatus(workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REPROCESS.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
	}

	private void processCompleteAsFailed(WorkflowInternalActionDTO workflowInternalActionDTO)
			throws WorkflowActionException, ApisResourceAccessException, PacketManagerException,
			JsonProcessingException, IOException {
		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration(workflowInternalActionDTO.getRid(),
						workflowInternalActionDTO.getReg_type(), workflowInternalActionDTO.getIteration());

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
			.getRegistrationStatus(workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
		if (additionalInfoRequestDto != null) {
			Map<String, String> tags = new HashMap<String, String>();
			tags.put(workflowInternalActionDTO.getReg_type() + "_FLOW_STATUS",
					RegistrationStatusCode.FAILED.toString());
			packetManagerService.addOrUpdateTags(workflowInternalActionDTO.getRid(),
					tags);
			InternalRegistrationStatusDto mainFlowregistrationStatusDto = registrationStatusService
					.getRegistrationStatus(null, null, null, additionalInfoRequestDto.getWorkflowInstanceId());
			List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
			internalRegistrationStatusDtos.add(mainFlowregistrationStatusDto);
			workflowActionService.processWorkflowAction(internalRegistrationStatusDtos,
					WorkflowActionCode.RESUME_PROCESSING.toString());
		} else {
			sendWorkflowCompletedWebSubEvent(registrationStatusDto);
		}

	}

	private void processCompleteAsRejected(WorkflowInternalActionDTO workflowInternalActionDTO)
			throws WorkflowActionException, ApisResourceAccessException, PacketManagerException,
			JsonProcessingException, IOException {
		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration(workflowInternalActionDTO.getRid(),
						workflowInternalActionDTO.getReg_type(), workflowInternalActionDTO.getIteration());
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
			.getRegistrationStatus(workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
						workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
		
		if (additionalInfoRequestDto != null) {
			Map<String, String> tags = new HashMap<String, String>();
			tags.put(workflowInternalActionDTO.getReg_type() + "_FLOW_STATUS",
					RegistrationStatusCode.REJECTED.toString());
			packetManagerService.addOrUpdateTags(workflowInternalActionDTO.getRid(), tags);
			InternalRegistrationStatusDto mainFlowregistrationStatusDto = registrationStatusService
				.getRegistrationStatus(null, null, null, additionalInfoRequestDto.getWorkflowInstanceId());
			List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
			internalRegistrationStatusDtos.add(mainFlowregistrationStatusDto);
			workflowActionService.processWorkflowAction(internalRegistrationStatusDtos,
					WorkflowActionCode.RESUME_PROCESSING.toString());
		} else {
			sendWorkflowCompletedWebSubEvent(registrationStatusDto);
		}

	}

	private void processCompleteAsProcessed(WorkflowInternalActionDTO workflowInternalActionDTO)
			throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException,
			WorkflowActionException {
		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration(workflowInternalActionDTO.getRid(),
						workflowInternalActionDTO.getReg_type(), workflowInternalActionDTO.getIteration());
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
			.getRegistrationStatus(workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
		if (additionalInfoRequestDto != null) {
			Map<String, String> tags = new HashMap<String, String>();
			tags.put(workflowInternalActionDTO.getReg_type() + "_FLOW_STATUS",
					RegistrationStatusCode.PROCESSED.toString());
			packetManagerService.addOrUpdateTags(workflowInternalActionDTO.getRid(), tags);
			InternalRegistrationStatusDto mainFlowregistrationStatusDto = registrationStatusService
					.getRegistrationStatus(null, null, null, additionalInfoRequestDto.getWorkflowInstanceId());
			mainFlowregistrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
			internalRegistrationStatusDtos.add(mainFlowregistrationStatusDto);
			workflowActionService.processWorkflowAction(internalRegistrationStatusDtos,
					WorkflowActionCode.RESUME_PROCESSING.toString());
		} else {
			sendWorkflowCompletedWebSubEvent(registrationStatusDto);
		}

	}

	private void processPacketForPaused(WorkflowInternalActionDTO workflowInternalActionDTO) {
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
			.getRegistrationStatus(workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PAUSED.toString());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setDefaultResumeAction(workflowInternalActionDTO.getDefaultResumeAction());
		if (workflowInternalActionDTO.getResumeTimestamp() != null) {
			LocalDateTime resumeTimeStamp = DateUtils
					.parseToLocalDateTime(workflowInternalActionDTO.getResumeTimestamp());
			registrationStatusDto.setResumeTimeStamp(resumeTimeStamp);
		}
		registrationStatusDto.setUpdatedBy(USER);
		String pauseRuleIds="";
		for(String matchedRuleId:workflowInternalActionDTO.getMatchedRuleIds()) {
			if(pauseRuleIds.isEmpty())
				pauseRuleIds=matchedRuleId;
			else
			pauseRuleIds=pauseRuleIds+","+matchedRuleId;
		}
		registrationStatusDto.setPauseRuleIds(pauseRuleIds);
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);

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

	private void processRestartParentFlow(WorkflowInternalActionDTO workflowInternalActionDTO)
			throws WorkflowActionException, ApisResourceAccessException, PacketManagerException,
			JsonProcessingException, IOException {
		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration(workflowInternalActionDTO.getRid(),
						workflowInternalActionDTO.getReg_type(), workflowInternalActionDTO.getIteration());
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
		registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSED.toString());
		registrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
		if (additionalInfoRequestDto != null) {
			Map<String, String> tags = new HashMap<String, String>();
			tags.put(workflowInternalActionDTO.getReg_type() + "_FLOW_STATUS",
					RegistrationStatusCode.PROCESSED.toString());
			packetManagerService.addOrUpdateTags(workflowInternalActionDTO.getRid(), tags);

			InternalRegistrationStatusDto mainFlowregistrationStatusDto = registrationStatusService
					.getRegistrationStatus(null, null, null, additionalInfoRequestDto.getWorkflowInstanceId());

			List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
			internalRegistrationStatusDtos.add(mainFlowregistrationStatusDto);
			workflowActionService.processWorkflowAction(internalRegistrationStatusDtos,
					WorkflowActionCode.RESUME_FROM_BEGINNING.toString());
		} else {
			regProcLogger.error(
					"Error in  WorkflowEventUpdateVerticle:processRestartParentFlow for registration id {} {}",
					workflowInternalActionDTO.getRid(),
					PlatformErrorMessages.RPR_WIA_ADDITIONALINFOPROCESS_NOT_FOUND.getMessage());
		}
	}

	private void processPauseAndRequestAdditionalInfo(WorkflowInternalActionDTO workflowInternalActionDTO)
			throws WorkflowActionException, ApisResourceAccessException, PacketManagerException,
			JsonProcessingException, IOException {
		AdditionalInfoRequestDto additionalInfoRequestDto = additionalInfoRequestService
				.getAdditionalInfoRequestByRegIdAndProcessAndIteration(workflowInternalActionDTO.getRid(),
						workflowInternalActionDTO.getReg_type(), workflowInternalActionDTO.getIteration());
		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService.getRegistrationStatus(
				workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getReg_type(),
				workflowInternalActionDTO.getIteration(), workflowInternalActionDTO.getWorkflowInstanceId());
		if (additionalInfoRequestDto != null) {
			regProcLogger.warn("Info in WorkflowEventUpdateVerticle:processRestartParentFlow for registration id {} {}",
					workflowInternalActionDTO.getRid(),
					PlatformErrorMessages.RPR_WIA_ADDITIONALINFOPROCESS_CANNOT_REQUEST.getMessage());
			registrationStatusDto
					.setStatusComment(PlatformErrorMessages.RPR_WIA_ADDITIONALINFOPROCESS_CANNOT_REQUEST.getMessage());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.toString());
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
			registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
			registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
			Map<String, String> tags = new HashMap<String, String>();
			tags.put(workflowInternalActionDTO.getReg_type() + "_FLOW_STATUS",
					RegistrationStatusCode.FAILED.toString());
			packetManagerService.addOrUpdateTags(workflowInternalActionDTO.getRid(), tags);
			InternalRegistrationStatusDto mainFlowregistrationStatusDto = registrationStatusService
					.getRegistrationStatus(null, null, null, additionalInfoRequestDto.getWorkflowInstanceId());
			mainFlowregistrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			List<InternalRegistrationStatusDto> internalRegistrationStatusDtos = new ArrayList<InternalRegistrationStatusDto>();
			internalRegistrationStatusDtos.add(mainFlowregistrationStatusDto);
			workflowActionService.processWorkflowAction(internalRegistrationStatusDtos,
					WorkflowActionCode.RESUME_PROCESSING.toString());
		} else {
			List<AdditionalInfoRequestDto> additionalInfoRequestDtos = additionalInfoRequestService.
					getAdditionalInfoRequestByRegIdAndProcess(
						workflowInternalActionDTO.getRid(), workflowInternalActionDTO.getAdditionalInfoProcess());
			String iteration=env.getProperty("mosip.regproc.workflow-manager.internal.action.max-allowed-iteration." + workflowInternalActionDTO.getAdditionalInfoProcess());
			int maxAllowedIteration;
			if(iteration!=null) {
				maxAllowedIteration=Integer.parseInt(iteration);
			}else {
				maxAllowedIteration=defaultMaxAllowedIteration;
			}
			if (additionalInfoRequestDtos != null && !additionalInfoRequestDtos.isEmpty() && additionalInfoRequestDtos.get(0).getAdditionalInfoIteration()>=maxAllowedIteration) {
				workflowInternalActionDTO.setActionMessage(StatusUtil.WORKFLOW_INTERNAL_ACTION_REJECTED_ITERATIONS_EXCEEDED_LIMIT.getMessage());
				processCompleteAsRejected(workflowInternalActionDTO);
			}else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PAUSED_FOR_ADDITIONAL_INFO.toString());
			registrationStatusDto.setStatusComment(workflowInternalActionDTO.getActionMessage());
			registrationStatusDto.setDefaultResumeAction(workflowInternalActionDTO.getDefaultResumeAction());
			if (workflowInternalActionDTO.getResumeTimestamp() != null) {
				LocalDateTime resumeTimeStamp = DateUtils
						.parseToLocalDateTime(workflowInternalActionDTO.getResumeTimestamp());
				registrationStatusDto.setResumeTimeStamp(resumeTimeStamp);
			}
			registrationStatusDto.setUpdatedBy(USER);
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.INTERNAL_WORKFLOW_ACTION.toString());
			registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_INTERNAL_ACTION_SUCCESS.getCode());
			registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
			String additionalRequestId = createAdditionalInfoRequest(workflowInternalActionDTO,additionalInfoRequestDtos);
			sendWorkflowPausedForAdditionalInfoEvent(registrationStatusDto, additionalRequestId,
					workflowInternalActionDTO.getAdditionalInfoProcess());
			}
		}
	}

	private String createAdditionalInfoRequest(WorkflowInternalActionDTO workflowInternalActionDTO,List<AdditionalInfoRequestDto> additionalInfoRequestDtos) {
		int iteration = 0;
		if (additionalInfoRequestDtos != null && !additionalInfoRequestDtos.isEmpty()) {
			iteration = additionalInfoRequestDtos.get(0).getAdditionalInfoIteration() + 1;
		} else {
			iteration = 1;
		}
		String additionalRequestId = createAdditionalRequestId(workflowInternalActionDTO, iteration);
		AdditionalInfoRequestDto additionalInfoRequestDto = new AdditionalInfoRequestDto();
		additionalInfoRequestDto.setAdditionalInfoReqId(additionalRequestId);
		additionalInfoRequestDto.setWorkflowInstanceId(workflowInternalActionDTO.getWorkflowInstanceId());
		additionalInfoRequestDto.setRegId(workflowInternalActionDTO.getRid());
		additionalInfoRequestDto.setAdditionalInfoProcess(workflowInternalActionDTO.getAdditionalInfoProcess());
		additionalInfoRequestDto.setAdditionalInfoIteration(iteration);
		additionalInfoRequestDto.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
		additionalInfoRequestService.addAdditionalInfoRequest(additionalInfoRequestDto);
		return additionalRequestId;
	}

	private String createAdditionalRequestId(WorkflowInternalActionDTO workflowInternalActionDTO, int iteration) {
		String additionalRequestId = workflowInternalActionDTO.getRid() + "-"
				+ workflowInternalActionDTO.getAdditionalInfoProcess() + "-" + iteration;
		return additionalRequestId;
	}

	private void sendWorkflowPausedForAdditionalInfoEvent(InternalRegistrationStatusDto registrationStatusDto,
			String additonalInfoRequestId, String additionalInfoProcess) {
		WorkflowPausedForAdditionalInfoEventDTO workflowPausedForAdditionalInfoEventDTO = new WorkflowPausedForAdditionalInfoEventDTO();
		workflowPausedForAdditionalInfoEventDTO.setInstanceId(registrationStatusDto.getRegistrationId());
		workflowPausedForAdditionalInfoEventDTO.setWorkflowType(registrationStatusDto.getRegistrationType());
		workflowPausedForAdditionalInfoEventDTO.setAdditionalInfoProcess(additionalInfoProcess);
		workflowPausedForAdditionalInfoEventDTO.setAdditionalInfoRequestId(additonalInfoRequestId);
		webSubUtil.publishEvent(workflowPausedForAdditionalInfoEventDTO);

	}
}
