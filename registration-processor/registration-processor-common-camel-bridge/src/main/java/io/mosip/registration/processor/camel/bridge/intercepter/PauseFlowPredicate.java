package io.mosip.registration.processor.camel.bridge.intercepter;

import java.io.IOException;
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
import io.mosip.registration.processor.core.abstractverticle.WorkflowEventDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
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

		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);

		LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"exchange.getFromEndpoint().toString() " + exchange.getFromEndpoint().toString());

		String fromAddress = exchange.getFromEndpoint().toString();
		for (Setting setting : settings) {
			try {
			JSONArray jsonArray = JsonPath.read(message, setting.getMatchExpression());
			if (Pattern.matches(setting.getFromAddress(), fromAddress)
					&& !jsonArray.isEmpty()) {
				WorkflowEventDTO workflowEventDTO = new WorkflowEventDTO();
				workflowEventDTO.setResumeTimestamp(DateUtils
						.formatToISOString(DateUtils.getUTCCurrentDateTime().plusSeconds(setting.getPauseFor())));
				workflowEventDTO.setRid(json.getString("rid"));
				workflowEventDTO.setDefaultResumeAction(setting.getDefaultResumeAction());
				workflowEventDTO.setStatusCode(RegistrationStatusCode.PAUSED.toString());
				workflowEventDTO.setEventTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
				workflowEventDTO.setStatusComment(PlatformSuccessMessages.PACKET_PAUSED_HOTLISTED.getMessage());
				workflowEventDTO.setResumeRemoveTags(setting.getResumeRemoveTags());
		
					exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
					return true;
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
				
		}
		return false;
	}

}
