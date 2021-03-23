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

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.camel.bridge.model.Setting;
import io.mosip.registration.processor.core.abstractverticle.WorkflowEventDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.vertx.core.json.JsonObject;

public class PauseFlowPredicate implements Predicate {

	private static final Logger LOGGER = RegProcessorLogger.getLogger(PauseFlowPredicate.class);

	@Autowired
	private ObjectMapper objectMapper;

	Setting[] settings = null;

	@Value("${mosip.regproc.camelbridge.pause-settings}")
	private String settingsString;

	@Value("${mosip.regproc.camelbridge.intercept-hotlisted-key}")
	private String hotlistedTagKey;

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
		JsonObject tags = json.getJsonObject("tags");
		LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"exchange.getFromEndpoint().toString() " + exchange.getFromEndpoint().toString());
		LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				" tags.getString(hotlistedTagKey) " + tags.getString(hotlistedTagKey));
		LOGGER.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				" tags.containsKey(hotlistedTagKey) " + tags.containsKey(hotlistedTagKey));
		if (!tags.containsKey(hotlistedTagKey)) {
			return false;
		}
		String fromAddress = exchange.getFromEndpoint().toString();
		for (Setting setting : settings) {
			if (Pattern.matches(setting.getFromAddress(), fromAddress)
					&& tags.getString(hotlistedTagKey).equals(setting.getHotlistedReason())) {
				WorkflowEventDTO workflowEventDTO = new WorkflowEventDTO();
				workflowEventDTO.setResumeTimestamp(DateUtils
						.formatToISOString(DateUtils.getUTCCurrentDateTime().plusSeconds(setting.getPauseFor())));
				workflowEventDTO.setRid(json.getString("rid"));
				workflowEventDTO.setDefaultResumeAction(setting.getDefaultResumeAction());
				workflowEventDTO.setStatusCode(RegistrationStatusCode.PAUSED.toString());
				workflowEventDTO.setEventTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
				workflowEventDTO.setStatusComment(PlatformSuccessMessages.PACKET_PAUSED_HOTLISTED.getMessage());
				try {
					exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
				} catch (JsonProcessingException e) {
					LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
							"RoutePredicate::matches()::exception " + e.getMessage());
				}
				return true;
			} 		}
		return false;
	}

}
