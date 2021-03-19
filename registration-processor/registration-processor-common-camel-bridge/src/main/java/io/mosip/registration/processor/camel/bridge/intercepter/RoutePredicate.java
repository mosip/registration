package io.mosip.registration.processor.camel.bridge.intercepter;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.camel.bridge.model.Setting;
import io.mosip.registration.processor.core.abstractverticle.WorkflowEventDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.vertx.core.json.JsonObject;

@Component
public class RoutePredicate implements Predicate {
	
	private static final Logger LOGGER = RegProcessorLogger.getLogger(RouteIntercepter.class);
	
	@Value("${mosip.regproc.camelbridge.pause-settings}")
	private String settingsString;

	@Value("${mosip.regproc.camelbridge.intercept-hotlisted-key}")
	private String hotlistedTagKey;


	@Autowired
	private ObjectMapper objectMapper;

	Setting[] settings = null;

	@PostConstruct
	private void init() throws JsonParseException, JsonMappingException, IOException {
		settings = objectMapper.readValue(settingsString, Setting[].class);
	}

	@Override
	public boolean matches(Exchange exchange) {
		String message = (String) exchange.getMessage().getBody();
		JsonObject json = new JsonObject(message);
		JsonObject tags = json.getJsonObject("tags");
		if (tags.containsKey(hotlistedTagKey)) {
			String fromAddress = exchange.getFromEndpoint().toString();
			for (Setting setting : settings) {
				if (Pattern.matches(setting.getFromAddress(), fromAddress)
						&& tags.getString(hotlistedTagKey).equals(setting.getHotlistedReason())) {
					WorkflowEventDTO workflowEventDTO = new WorkflowEventDTO();
					workflowEventDTO.setResumeTimestamp(DateUtils
							.toISOString(DateUtils.getUTCCurrentDateTime().plusSeconds(setting.getPauseFor())));
					workflowEventDTO.setRid(json.getString("rid"));
					workflowEventDTO.setDefaultResumeAction(setting.getDefaultResumeAction());
					workflowEventDTO.setStatusCode(RegistrationStatusCode.PAUSED.toString());
					try {
						exchange.getMessage().setBody(objectMapper.writeValueAsString(workflowEventDTO));
					} catch (JsonProcessingException e) {
						LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
								"RouteIntercepter::intercept()::exception " + e.getMessage());
					}
					break;
				}else {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

}
