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
			case "workflow-cmd://resume-parent-flow":
				processResumeParentFlow(exchange);
				matches = true;
				break;
			case "workflow-cmd://restart-parent-flow":
				processRestartParentFlow(exchange);
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

	private void processCompleteAsRejected(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString());
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_COMPLETE_AS_REJECTED.getMessage());
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processMarkAsReprocess(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_REPROCESS.toString());
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_MARK_AS_REPROCESS.getMessage());
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processCompleteAsFailed(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_FAILED.toString());
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_COMPLETE_AS_FAILED.getMessage());
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processCompleteAsProcessed(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_COMPLETE_AS_PROCESSED.getMessage());
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processRestartParentFlow(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.RESTART_PARENT_FLOW.toString());
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_RESTART_PARENT_FLOW.getMessage());
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processResumeParentFlow(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.RESUME_PARENT_FLOW.toString());
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_RESUME_PARENT_FLOW.getMessage());
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processPauseAndRequestAdditionalInfo(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();
		workflowEventDTO.setResumeTimestamp(DateUtils.formatToISOString(
				DateUtils.getUTCCurrentDateTime()
						.plusSeconds(Long.parseLong((String) exchange.getProperty(JsonConstant.PAUSE_FOR)))));
		workflowEventDTO.setRid(json.getString(JsonConstant.RID));
		workflowEventDTO.setDefaultResumeAction(WorkflowActionCode.STOP_PROCESSING.toString());
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.PAUSE_AND_REQUEST_ADDITIONAL_INFO.toString());
		workflowEventDTO.setEventTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		workflowEventDTO.setActionMessage(PlatformSuccessMessages.PAUSE_AND_REQUEST_ADDITIONAL_INFO.getMessage());
		workflowEventDTO.setAdditionalInfoProcess((String) exchange.getProperty(JsonConstant.ADDITIONAL_INFO_PROCESS));
		workflowEventDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
		workflowEventDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
		workflowEventDTO.setSource(json.getString(JsonConstant.SOURCE));
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}
}