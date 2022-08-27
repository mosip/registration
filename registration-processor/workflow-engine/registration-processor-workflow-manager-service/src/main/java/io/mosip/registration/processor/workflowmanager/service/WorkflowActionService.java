package io.mosip.registration.processor.workflowmanager.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.code.WorkflowActionCode;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.WorkflowActionException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.workflow.dto.WorkflowCompletedEventDTO;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.workflowmanager.util.WebSubUtil;


/**
 * The Class WorkflowActionService.
 */
@Component
public class WorkflowActionService {

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The packet manager service. */
	@Autowired
	private PacketManagerService packetManagerService;

	/** The core audit request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	/** The web sub util. */
	@Autowired
	WebSubUtil webSubUtil;

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";

	/** The resume from beginning stage. */
	@Value("${mosip.regproc.workflow-manager.action.resumefrombeginning.stage}")
	private String resumeFromBeginningStage;

	/** The module name. */
	public static String MODULE_NAME = ModuleName.WORKFLOW_ACTION_SERVICE.toString();

	/** The module id. */
	public static String MODULE_ID = PlatformSuccessMessages.RPR_WORKFLOW_ACTION_SERVICE_SUCCESS.getCode();

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkflowActionService.class);




	/**
	 * Process workflow action.
	 *
	 * @param internalRegistrationStatusDtos the internal registration status dtos
	 * @param workflowAction                 the workflow action
	 * @throws WorkflowActionException the workflow action exception
	 */
	public void processWorkflowAction(List<InternalRegistrationStatusDto> internalRegistrationStatusDtos,
			String workflowAction) throws WorkflowActionException {
		WorkflowActionCode workflowActionCode = null;
		try {
			workflowActionCode = WorkflowActionCode.valueOf(workflowAction);
		} catch (IllegalArgumentException e) {
			throw new WorkflowActionException(PlatformErrorMessages.RPR_WAS_UNKNOWN_WORKFLOW_ACTION.getCode(),
					PlatformErrorMessages.RPR_WAS_UNKNOWN_WORKFLOW_ACTION.getMessage());
		}
		switch (workflowActionCode) {
		case RESUME_PROCESSING:
			processResumeProcessing(internalRegistrationStatusDtos, workflowActionCode);
			break;
		case RESUME_FROM_BEGINNING:
			processResumeFromBeginning(internalRegistrationStatusDtos, workflowActionCode);
			break;
		case STOP_PROCESSING:
			processStopProcessing(internalRegistrationStatusDtos, workflowActionCode);
			break;
		default:
				throw new WorkflowActionException(PlatformErrorMessages.RPR_WAS_UNKNOWN_WORKFLOW_ACTION.getCode(),
						PlatformErrorMessages.RPR_WAS_UNKNOWN_WORKFLOW_ACTION.getMessage());

			}

	}

	/**
	 * Process stop processing.
	 *
	 * @param internalRegistrationStatusDtos the internal registration status dtos
	 * @param workflowActionCode             the workflow action code
	 * @throws WorkflowActionException the workflow action exception
	 */
	private void processStopProcessing(List<InternalRegistrationStatusDto> internalRegistrationStatusDtos,
			WorkflowActionCode workflowActionCode) throws WorkflowActionException {
		regProcLogger.debug("processStopProcessing called for workflowIds {}",
				internalRegistrationStatusDtos.toString());
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;

		if (CollectionUtils.isEmpty(internalRegistrationStatusDtos))
			return;
		for (InternalRegistrationStatusDto internalRegistrationStatusDto : internalRegistrationStatusDtos) {
			String rid = internalRegistrationStatusDto.getRegistrationId();
               try {
            		//addRuleIdsToTag(internalRegistrationStatusDto);
            	     //Pause and Immunity not added for STOP processing
    				internalRegistrationStatusDto = updateRegistrationStatus(internalRegistrationStatusDto,
							RegistrationStatusCode.REJECTED,
							workflowActionCode);
				sendWebSubEvent(internalRegistrationStatusDto);
					description.setMessage(
							String.format(PlatformSuccessMessages.RPR_WORKFLOW_ACTION_SERVICE_SUCCESS.getMessage(),
									workflowActionCode.name()));
					isTransactionSuccessful = true;

			} catch (TablenotAccessibleException e) {
				logAndThrowError(e, e.getErrorCode(), e.getMessage(), rid, description);
			} catch (WebSubClientException e) {
				logAndThrowError(e, ((BaseUncheckedException) e).getErrorCode(),
						((BaseUncheckedException) e).getMessage(),
						rid, description);
			} catch (Exception e) {
				logAndThrowError(e, PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getMessage(), rid, description);

			} finally {
				regProcLogger.debug("WorkflowActionService status for registration id {} {}", rid,
						description.getMessage());
					updateAudit(description, rid, isTransactionSuccessful);
				}

			regProcLogger.debug("processStopProcessing call ended for workflowIds {}",
					internalRegistrationStatusDtos.toString());
		}

	}

	/**
	 * send web sub event.
	 *
	 * @param registrationStatusDto the registration status dto
	 */
	private void sendWebSubEvent(InternalRegistrationStatusDto registrationStatusDto) {
		WorkflowCompletedEventDTO workflowCompletedEventDTO = new WorkflowCompletedEventDTO();
		workflowCompletedEventDTO.setInstanceId(registrationStatusDto.getRegistrationId());
		workflowCompletedEventDTO.setResultCode(registrationStatusDto.getStatusCode());
		workflowCompletedEventDTO.setWorkflowType(registrationStatusDto.getRegistrationType());
		if (registrationStatusDto.getStatusCode().equalsIgnoreCase(RegistrationStatusCode.REJECTED.toString())) {
			workflowCompletedEventDTO.setErrorCode(RegistrationExceptionTypeCode.PACKET_REJECTED.name());
		}

		webSubUtil.publishEvent(workflowCompletedEventDTO);

	}




	/**
	 * Process resume from beginning.
	 *
	 * @param internalRegistrationStatusDtos the internal registration status dtos
	 * @param workflowActionCode             the workflow action code
	 * @throws WorkflowActionException the workflow action exception
	 */
	private void processResumeFromBeginning(List<InternalRegistrationStatusDto> internalRegistrationStatusDtos,
			WorkflowActionCode workflowActionCode) throws WorkflowActionException {
		regProcLogger.debug("processResumeFromBeginning called for workflowIds {}",
				internalRegistrationStatusDtos.toString());
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		if (CollectionUtils.isEmpty(internalRegistrationStatusDtos))
			return;
		for (InternalRegistrationStatusDto internalRegistrationStatusDto : internalRegistrationStatusDtos) {
			String rid = internalRegistrationStatusDto.getRegistrationId();
				try {
					addRuleIdsToTag(internalRegistrationStatusDto);
					if (RegistrationTransactionStatusCode.REPROCESS_FAILED.name()
						.equals(internalRegistrationStatusDto.getLatestTransactionStatusCode())) {
					internalRegistrationStatusDto = updateRegistrationStatus(internalRegistrationStatusDto,
								RegistrationStatusCode.REPROCESS_FAILED, workflowActionCode);
						description.setMessage(PlatformErrorMessages.RPR_WAS_REPROCESS_FAILED.getMessage());
					} else {
					internalRegistrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
					internalRegistrationStatusDto.setRegistrationStageName(resumeFromBeginningStage);
					internalRegistrationStatusDto = updateRegistrationStatus(internalRegistrationStatusDto,
							RegistrationStatusCode.RESUMABLE, workflowActionCode);
						description.setMessage(
								String.format(PlatformSuccessMessages.RPR_WORKFLOW_ACTION_SERVICE_SUCCESS.getMessage(),
										workflowActionCode.name()));
						isTransactionSuccessful = true;
					}

				} 
			catch (TablenotAccessibleException e) {
				logAndThrowError(e, e.getErrorCode(), e.getMessage(), rid, description);
			} catch (ApisResourceAccessException | PacketManagerException | JsonProcessingException e) {
				logAndThrowError(e, ((BaseCheckedException) e).getErrorCode(), ((BaseCheckedException) e).getMessage(),
						rid, description);
			} catch (Exception e) {
					logAndThrowError(e, PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getCode(),
							PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getMessage(), rid, description);

				} finally {
				regProcLogger.debug("WorkflowActionService status for registration id {} {}", rid,
							description.getMessage());
					updateAudit(description, rid, isTransactionSuccessful);
				}

			}
		regProcLogger.debug("processResumeFromBeginning call ended for workflowIds {}",
				internalRegistrationStatusDtos.toString());


	}




	/**
	 * Process resume processing.
	 *
	 * @param internalRegistrationStatusDtos the internal registration status dtos
	 * @param workflowActionCode             the workflow action code
	 * @throws WorkflowActionException the workflow action exception
	 */
	private void processResumeProcessing(List<InternalRegistrationStatusDto> internalRegistrationStatusDtos,
			WorkflowActionCode workflowActionCode) throws WorkflowActionException {
		regProcLogger.debug("processResumeProcessing called for workflowIds {}",
				internalRegistrationStatusDtos.toString());
		boolean isTransactionSuccessful = false;

		LogDescription description = new LogDescription();
		if (CollectionUtils.isEmpty(internalRegistrationStatusDtos))
			return;
		for (InternalRegistrationStatusDto internalRegistrationStatusDto : internalRegistrationStatusDtos) {
			String rid = internalRegistrationStatusDto.getRegistrationId();
				try {
				addRuleIdsToTag(internalRegistrationStatusDto);
				if (RegistrationTransactionStatusCode.REPROCESS_FAILED.name()
								.equals(internalRegistrationStatusDto.getLatestTransactionStatusCode())) {
							internalRegistrationStatusDto = updateRegistrationStatus(internalRegistrationStatusDto,
									RegistrationStatusCode.REPROCESS_FAILED, workflowActionCode);
							description.setMessage(PlatformErrorMessages.RPR_WAS_REPROCESS_FAILED.getMessage());
					} else {
						internalRegistrationStatusDto = updateRegistrationStatus(internalRegistrationStatusDto,
							RegistrationStatusCode.RESUMABLE, workflowActionCode);
							description.setMessage(String.format(
									PlatformSuccessMessages.RPR_WORKFLOW_ACTION_SERVICE_SUCCESS.getMessage(),
								workflowActionCode.name()));
							isTransactionSuccessful = true;
					}

				}
			catch (TablenotAccessibleException e) {
				logAndThrowError(e, e.getErrorCode(), e.getMessage(), rid, description);
			} catch (ApisResourceAccessException | PacketManagerException | JsonProcessingException
			e) {
				logAndThrowError(e, ((BaseCheckedException) e).getErrorCode(), ((BaseCheckedException) e).getMessage(),
						rid, description);
			} catch (Exception e) {
					logAndThrowError(e, PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getCode(),
							PlatformErrorMessages.RPR_WAS_UNKNOWN_EXCEPTION.getMessage(), rid, description);

			} finally {
				regProcLogger.debug("WorkflowActionService status for registration id {} {}", rid,
							description.getMessage());
					updateAudit(description, rid, isTransactionSuccessful);
				}

			}
			regProcLogger.debug("processResumeProcessing call ended for workflowIds {}",
				internalRegistrationStatusDtos.toString());


	}


	private void addRuleIdsToTag(InternalRegistrationStatusDto internalRegistrationStatusDto)
			throws ApisResourceAccessException, JsonProcessingException, PacketManagerException, IOException
			{
		String pauseRuleIds = internalRegistrationStatusDto.getPauseRuleIds();
		if (StringUtils.isEmpty(pauseRuleIds))
			return;

		List<String> tags = new ArrayList<String>();
		tags.add("PAUSE_IMMUNITY_RULE_IDS");
		Map<String, String> tagsPresent=packetManagerService.getTags(internalRegistrationStatusDto.getRegistrationId(), tags);
        String pauseRuleImmunityTag="";
        Set<String> rulesSet=new HashSet<String>();
		if(tagsPresent!=null) {
			pauseRuleImmunityTag=tagsPresent.get("PAUSE_IMMUNITY_RULE_IDS");
			if(!pauseRuleImmunityTag.isEmpty()) {
				rulesSet.addAll(Arrays.asList(pauseRuleImmunityTag.split(", ")));
			}
           }
		String[] pauseRuleIdsArray = internalRegistrationStatusDto.getPauseRuleIds().split(",");
		if (pauseRuleIdsArray.length > 0) {
			for (int i = 0; i < pauseRuleIdsArray.length; i++) {
				if(!rulesSet.contains(pauseRuleIdsArray[i])) {
					rulesSet.add(pauseRuleIdsArray[i]);
				}
			}
		}
        //String.join(", ", rulesSet);
		Map<String,String> tagsToAdd=new HashMap<String,String>();
		tagsToAdd.put("PAUSE_IMMUNITY_RULE_IDS", String.join(", ", rulesSet));
		packetManagerService.addOrUpdateTags(internalRegistrationStatusDto.getRegistrationId(), tagsToAdd);
		regProcLogger.debug("addRuleIdsToTag called for workflowId {}",
				internalRegistrationStatusDto.getRegistrationId());


	}

	/**
	 * Update audit.
	 *
	 * @param description             the description
	 * @param registrationId          the registration id
	 * @param isTransactionSuccessful the is transaction successful
	 */
	private void updateAudit(LogDescription description, String registrationId, boolean isTransactionSuccessful) {

		String moduleId = isTransactionSuccessful
				? MODULE_ID
				: description.getCode();

		String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
		String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
		String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

		auditLogRequestBuilder.createAuditRequestBuilder(description.getMessage(), eventId, eventName, eventType,
				moduleId, MODULE_NAME, registrationId);
	}



	/**
	 * Update registration status.
	 *
	 * @param registrationStatusDto the registration status dto
	 * @param statusCode            the status code
	 * @param workflowActionCode    the workflow action code
	 * @return the internal registration status dto
	 */
	private InternalRegistrationStatusDto updateRegistrationStatus(InternalRegistrationStatusDto registrationStatusDto,
			RegistrationStatusCode statusCode, WorkflowActionCode workflowActionCode) {
		registrationStatusDto.setStatusCode(statusCode.name());
		registrationStatusDto.setStatusComment(String.format(
				PlatformSuccessMessages.RPR_WORKFLOW_ACTION_SERVICE_SUCCESS.getMessage(), workflowActionCode.name()));

		registrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.WORKFLOW_RESUME.toString());
		registrationStatusDto.setSubStatusCode(StatusUtil.WORKFLOW_ACTION_SERVICE_SUCCESS.getCode());

		registrationStatusDto.setUpdatedBy(USER);
		registrationStatusDto.setDefaultResumeAction(null);
		registrationStatusDto.setResumeTimeStamp(null);
		registrationStatusDto.setPauseRuleIds(null);
		registrationStatusService.updateRegistrationStatusForWorkflowEngine(registrationStatusDto, MODULE_ID, MODULE_NAME);
		return registrationStatusDto;
	}

	/**
	 * Log and throw error.
	 *
	 * @param e              the e
	 * @param errorCode      the error code
	 * @param errorMessage   the error message
	 * @param registrationId the registration id
	 * @param description    the description
	 * @throws WorkflowActionException the workflow action exception
	 */
	private void logAndThrowError(Exception e, String errorCode, String errorMessage, String registrationId,
			LogDescription description) throws WorkflowActionException {
		description.setCode(errorCode);
		description.setMessage(errorMessage);
		regProcLogger.error("Error in  processWorkflowAction  for registration id  {} {} {} {}", registrationId,
				errorMessage, e.getMessage(), ExceptionUtils.getStackTrace(e));
		throw new WorkflowActionException(errorCode, errorMessage);
	}

}
