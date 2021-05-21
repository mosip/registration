package io.mosip.registration.processor.camel.bridge.intercepter;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.json.JsonObject;

public class WorkflowPredicate implements Predicate {

	private static final Logger LOGGER = RegProcessorLogger.getLogger(WorkflowPredicate.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public boolean matches(Exchange exchange) {
		boolean matches = false;

		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"exchange.getFromEndpoint().toString() " + exchange.getFromEndpoint().toString());
		try {

			String fromAddress = exchange.getFromEndpoint().toString();
			switch (fromAddress) {
			case "workflow-cmd:complete-as-processed":
				processCompleteAsProcessed(exchange);
				matches = true;
				break;
			case "workflow-cmd:complete-as-rejected":
				processCompleteAsRejected(exchange);
				matches = true;
				break;
			case "workflow-cmd:mark-as-failed":
				processMarkAsFailed(exchange);
				matches = true;
				break;
			case "workflow-cmd:mark-as-reprocess":
				processMarkAsReprocess(exchange);
				matches = true;
				break;
			default:
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

		workflowEventDTO.setRid(json.getString("rid"));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_REJECTED.toString());
		// TODO statusMessage update is needed?
		// workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_PAUSED_FOR_ADDITIONAL_INFO.getMessage());
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));

	}

	private void processMarkAsReprocess(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();

		workflowEventDTO.setRid(json.getString("rid"));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_REPROCESS.toString());
		// TODO statusMessage update is needed?
		// workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_PAUSED_FOR_ADDITIONAL_INFO.getMessage());
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
	}

	private void processMarkAsFailed(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();

		workflowEventDTO.setRid(json.getString("rid"));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_FAILED.toString());
		// TODO statusMessage update is needed?
		// workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_PAUSED_FOR_ADDITIONAL_INFO.getMessage());
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));

	}

	private void processCompleteAsProcessed(Exchange exchange) throws JsonProcessingException {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		WorkflowInternalActionDTO workflowEventDTO = new WorkflowInternalActionDTO();

		workflowEventDTO.setRid(json.getString("rid"));
		workflowEventDTO.setActionCode(WorkflowInternalActionCode.COMPLETE_AS_PROCESSED.toString());
		// TODO statusMessage update is needed?
		// workflowEventDTO.setActionMessage(PlatformSuccessMessages.PACKET_PAUSED_FOR_ADDITIONAL_INFO.getMessage());
		exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));

	}


}