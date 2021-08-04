package io.mosip.registration.processor.camel.bridge.intercepter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.camel.bridge.model.Setting;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import io.mosip.registration.processor.core.code.WorkflowInternalActionCode;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.vertx.core.json.JsonObject;
import net.minidev.json.JSONArray;

public class PauseFlowPredicate implements Predicate {

	private static final Logger LOGGER = RegProcessorLogger.getLogger(PauseFlowPredicate.class);

	@Autowired
	private ObjectMapper objectMapper;

	Setting[] settings = null;

	@Value("${mosip.regproc.camelbridge.pause-settings}")
	private String settingsString;

	@PostConstruct
	private void init() {
		try {
			settings = objectMapper.readValue(settingsString, Setting[].class);
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
					"RoutePredicate::exception " + e.getMessage());
		}

	}

	@Override
	public boolean matches(Exchange exchange) {
        boolean isMatches=false;
        try {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
        MessageDTO messageDto=objectMapper.readValue(message, MessageDTO.class);
		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"exchange.getFromEndpoint().toString() " + exchange.getFromEndpoint().toString());

		String fromAddress = exchange.getFromEndpoint().toString();
		WorkflowInternalActionDTO workflowInternalActionDTO = new WorkflowInternalActionDTO();
		List<String> matchedRuleIds=new ArrayList<String>();
		String ruleDescription="";
		long pauseFor=0;
		String defaultResumeAction=null;
		Map<String,String> tags=messageDto.getTags();
		for (Setting setting : settings) {
			if(isRuleIdNotPresent(tags,setting.getRuleId())) {

			JSONArray jsonArray = JsonPath.read(message, setting.getMatchExpression());
			if (Pattern.matches(setting.getFromAddress(), fromAddress)
					&& !jsonArray.isEmpty()) {
				     matchedRuleIds.add(setting.getRuleId());
				     if(ruleDescription.isBlank())
				    	 ruleDescription=setting.getRuleDescription();
				     else
				    	 ruleDescription=ruleDescription+","+setting.getRuleDescription();
				     if(setting.getPauseFor()>pauseFor) {
				    	 pauseFor=setting.getPauseFor();
				    	 defaultResumeAction=setting.getDefaultResumeAction();
				     }
					isMatches=true;
				}
			}
		}
		if(isMatches) {
            workflowInternalActionDTO.setRid(json.getString(JsonConstant.RID));
			workflowInternalActionDTO
			.setEventTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
			workflowInternalActionDTO.setActionCode(WorkflowInternalActionCode.MARK_AS_PAUSED.toString());
			workflowInternalActionDTO.setDefaultResumeAction(defaultResumeAction);
			 workflowInternalActionDTO.setResumeTimestamp(DateUtils
						.formatToISOString(DateUtils.getUTCCurrentDateTime().plusSeconds(pauseFor)));
			workflowInternalActionDTO.setMatchedRuleIds(matchedRuleIds);
			workflowInternalActionDTO
			.setActionMessage(PlatformSuccessMessages.PACKET_MARK_AS_PAUSED.getMessage()+"("+ruleDescription+")");
            workflowInternalActionDTO.setReg_type(json.getString(JsonConstant.REGTYPE));
            workflowInternalActionDTO.setIteration(json.getInteger(JsonConstant.ITERATION));
            workflowInternalActionDTO.setSource(json.getString(JsonConstant.SOURCE));
            workflowInternalActionDTO
                    .setWorkflowInstanceId(json.getString(JsonConstant.WORKFLOW_INSTANCE_ID));
			exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowInternalActionDTO));
		}
        }catch (JsonProcessingException e) {
			LOGGER.error("Error in  RoutePredicate::matches {}",
					 e.getMessage());
			throw new BaseUncheckedException(e.getMessage());
			}
			catch (InvalidPathException e) {
			LOGGER.error("Error in  RoutePredicate::matches {}",
						 e.getMessage());
			throw new BaseUncheckedException(e.getMessage());
			}
		catch (Exception e) {
			LOGGER.error("Error in  RoutePredicate::matches {}",
					 e.getMessage());
			throw new BaseUncheckedException(e.getMessage());
		}
		return isMatches;
	}

	private boolean isRuleIdNotPresent(Map<String, String> tags, String  ruleId) {
		boolean isRuleIdNotPresent=true;
		if(tags!=null) {
			String pauseRuleImmunity=tags.get(JsonConstant.PAUSERULEIMMUNITY);
            if(pauseRuleImmunity!=null & pauseRuleImmunity.contains(ruleId)) {
            	isRuleIdNotPresent=false;
           }
		}
		return isRuleIdNotPresent;
	}

}
