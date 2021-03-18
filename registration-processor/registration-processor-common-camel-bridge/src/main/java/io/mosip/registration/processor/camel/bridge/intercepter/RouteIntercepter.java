package io.mosip.registration.processor.camel.bridge.intercepter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
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
import io.mosip.registration.processor.camel.bridge.processor.TokenGenerationProcessor;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.WorkflowEventDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.json.JsonObject;

@Component
public class RouteIntercepter {

	private static final Logger LOGGER = RegProcessorLogger.getLogger(TokenGenerationProcessor.class);

	@Value("${mosip.regproc.camelbridge.pause-settings}")
	private String settingsString;

	@Value("${mosip.regproc.camelbridge.intercept-hotlisted-key}")
	private String hotlistedTagKey;

	private String workflowStatusUpdateAddress = MessageBusAddress.WORKFLOW_EVENT_UPDATE_ADDRESS.toString();

	@Autowired
	private ObjectMapper objectMapper;

	Setting[] settings = null;

	@PostConstruct
	private void init() throws JsonParseException, JsonMappingException, IOException {

		settings = objectMapper.readValue(settingsString, Setting[].class);

	}

	public List<RouteDefinition> intercept(CamelContext camelContext, RoutesDefinition routes) {
		List<RouteDefinition> routesDefination = routes.getRoutes();
		routesDefination.forEach(x -> {
			try {
				x.adviceWith(camelContext, new AdviceWithRouteBuilder() {

					@Override
					public void configure() throws Exception {

						interceptFrom("*").when(new Predicate() {

							@Override
							public boolean matches(Exchange exchange) {

								String message = (String) exchange.getMessage().getBody();
								JsonObject json = new JsonObject(message);
								JsonObject tags = json.getJsonObject("tags");
								if (tags.containsKey(hotlistedTagKey)) {
									String fromAddress = exchange.getFromEndpoint().toString();
									for (Setting setting : settings) {
										if (Pattern.matches(setting.getFromAddress(), fromAddress) && tags
												.getString(hotlistedTagKey).equals(setting.getHotlistedReason())) {
											// workflow dto workfloweventdto
											WorkflowEventDTO workflowEventDTO = new WorkflowEventDTO();
											workflowEventDTO.setResumeTimestamp(DateUtils.toISOString(DateUtils
													.getUTCCurrentDateTime().plusSeconds(setting.getPauseFor())));
											workflowEventDTO.setRid(json.getString("rid"));
											workflowEventDTO.setDefaultResumeAction(setting.getDefaultResumeAction());
											workflowEventDTO.setStatusCode("PAUSED");
											try {
												exchange.getMessage()
														.setBody(objectMapper.writeValueAsString(workflowEventDTO));
											} catch (JsonProcessingException e) {
												LOGGER.error(LoggerFileConstant.SESSIONID.toString(),
														LoggerFileConstant.USERID.toString(), "",
														"RouteIntercepter::intercept()::exception " + e.getMessage());
											}
											break;
										}
									}
									return true;
								} else {
									return false;
								}
							}
						}).to(workflowStatusUpdateAddress).stop();
					}
				});

			} catch (Exception e) {
				LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"RouteIntercepter::intercept()::exception " + e.getMessage());
			}
		});
		return routesDefination;
	}
}