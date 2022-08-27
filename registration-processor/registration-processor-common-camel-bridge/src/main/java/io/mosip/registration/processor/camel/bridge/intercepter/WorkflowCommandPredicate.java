package io.mosip.registration.processor.camel.bridge.intercepter;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowActionCode;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.json.JsonObject;

public class WorkflowCommandPredicate implements Predicate {

	private static final Logger LOGGER = RegProcessorLogger.getLogger(WorkflowCommandPredicate.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public boolean matches(Exchange exchange) {
		boolean matches = false;

		try {
			String toaddress = (String) exchange.getMessage().getHeader(Exchange.INTERCEPTED_ENDPOINT);

			switch (toaddress) {
			case "workflow-cmd://complete-as-processed":
				processCompleteAsProcessed(exchange);
				matches = true;
				break;
			case "workflow-cmd://complete-as-rejected":
				processCompleteAsRejected(exchange);
				matches = true;
				break;
			case "workflow-cmd://complete-as-failed":
				processCompleteAsFailed(exchange);
				matches = true;
				break;
			case "workflow-cmd://mark-as-reprocess":
				processMarkAsReprocess(exchange);
				matches = true;
				break;
			case "workflow-cmd://pause-and-request-additional-info":
				processPauseAndRequestAdditionalInfo(exchange);
				matches = true;
				break;
			case "workflow-cmd://restart-parent-flow":
				processRestartParentFlow(exchange);
				matches = true;
				break;
			case "workflow-cmd://complete-as-rejected-without-parent-flow":
				processCompleteAsRejectedWithoutParentFlow(exchange);
				matches = true;
				break;
			case "workflow-cmd://anonymous-profile":
				processAnonymousProfile(exchange);
				matches = true;
				break;
			default:
				if (toaddress.startsWith("workflow-cmd://")) {
					matches = true;
					LOGGER.error("Error in  RoutePredicate::matches {}",
							PlatformErrorMessages.RPR_CMB_WORKFLOW_COMMAND_NOT_SUPPORTED.getMessage());
					throw new BaseUncheckedException(
							PlatformErrorMessages.RPR_CMB_WORKFLOW_COMMAND_NOT_SUPPORTED.getCode(),
							PlatformErrorMessages.RPR_CMB_WORKFLOW_COMMAND_NOT_SUPPORTED.getMessage());
				}
				break;

			}
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in  RoutePredicate::matches {}", e.getMessage());
			throw new BaseUncheckedException(e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Error in  RoutePredicate::matches {}", e.getMessage());
			throw new BaseUncheckedException(e.getMessage());
		}
		return matches;
	}

	private void processCompleteAsRejectedWithoutParentFlow(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO
				.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW.toString());
		workflowInternalActionDTO
				.setActionMessage(PlatformSuccessMessages.PACKET_COMPLETE_AS_REJECTED_WITHOUT_PARENT_FLOW.getMessage());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}

	private void processCompleteAsRejected(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_PROCESSING_COMPLETED.getMessage() + WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}

	private void processMarkAsReprocess(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_REPROCESS.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_PROCESSING_COMPLETED.getMessage() + WorkflowInternalActionCode.MARK_AS_REPROCESS.toString());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}

	private void processCompleteAsFailed(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_FAILED.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_PROCESSING_COMPLETED.getMessage() + WorkflowInternalActionCode.COMPLETE_AS_FAILED.toString());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}
	
	private void processAnonymousProfile(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.ANONYMOUS_PROFILE.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_ANONYMOUS_PROFILE.getMessage());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}

	private void processCompleteAsProcessed(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_PROCESSING_COMPLETED.getMessage() + WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}

	private void processRestartParentFlow(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.RESTART_PARENT_FLOW.toString());
		workflowInternalActionDTO.setActionMessage(PlatformSuccessMessages.PACKET_RESTART_PARENT_FLOW.getMessage());
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}

	private void processPauseAndRequestAdditionalInfo(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		workflowInternalActionDTO.setResumeTimestamp(DateUtils.formatToISOString(
				DateUtils.getUTCCurrentDateTime()
						.plusSeconds(Long.parseLong((String) exchange.getProperty(JsonConstant.PAUSE_FOR)))));
		workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
		workflowInternalActionDTO.setDefaultResumeAction(WorkflowActionCode.STOP_PROCESSING.toString());
		workflowInternalActionDTO
				.setActionCode(WorkflowInternalActionCode.PAUSE_AND_REQUEST_ADDITIONAL_INFO.toString());
		workflowInternalActionDTO.setEventTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		workflowInternalActionDTO
				.setActionMessage(PlatformSuccessMessages.PAUSE_AND_REQUEST_ADDITIONAL_INFO.getMessage());
		workflowInternalActionDTO
				.setAdditionalInfoProcess((String) exchange.getProperty(JsonConstant.ADDITIONAL_INFO_PROCESS));
		workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
		workflowInternalActionDTO.setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
	}
}