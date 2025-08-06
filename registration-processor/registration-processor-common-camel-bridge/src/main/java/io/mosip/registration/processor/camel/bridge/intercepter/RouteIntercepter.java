package io.mosip.registration.processor.camel.bridge.intercepter;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.registration.processor.core.abstractverticle.WorkflowInternalActionDTO;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;

public class RouteIntercepter {

	private static final Logger LOGGER = RegProcessorLogger.getLogger(RouteIntercepter.class);

	@Value("${mosip.regproc.camelbridge.endpoint-prefix}")
	private String endpointPrefix;

	@Autowired
	private PauseFlowPredicate pauseFlowPredicate;

	@Autowired
	private WorkflowCommandPredicate workflowCommandPredicate;

	@Autowired
	private ObjectMapper objectMapper;

	private String workflowInternalActionAddress = MessageBusAddress.WORKFLOW_INTERNAL_ACTION_ADDRESS.getAddress();

	public List<RouteDefinition> intercept(CamelContext camelContext, List<RouteDefinition> routeDefinitions) {
		routeDefinitions.forEach(x -> {
			try {
				x.adviceWith(camelContext, new AdviceWithRouteBuilder() {

					@Override
					public void configure() throws Exception {

						interceptFrom("*").when(pauseFlowPredicate).to(endpointPrefix + workflowInternalActionAddress)
								.stop();
						interceptSendToEndpoint("workflow-cmd:*").when(workflowCommandPredicate)
								.process(exchange -> {
									String newKey = getKey(exchange);
									exchange.getIn().setHeader("kafka.KEY", newKey);
									exchange.getIn().setMessageId(newKey);
								})
								.to(endpointPrefix + workflowInternalActionAddress);
					}
				});
			} catch (Exception e) {
				LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
						"RouteIntercepter::intercept()::exception " + e.getMessage());
			}
		});
		return routeDefinitions;
	}

	/* Add the actionCode to the key specifically for workflow manager because multiple commands
	are being sent to workflow manager for the same RID which result in same key.
	Hence actionCode is added to the key in order to differentiate the same so that it won't cause
	issue in caffeine cache implementation.
	 */
	private String getKey(Exchange exchange) throws JsonProcessingException {
		StringBuilder keyBuilder = new StringBuilder();

		WorkflowInternalActionDTO workflowInternalActionDTO  = objectMapper.readValue(exchange.getMessage().getBody().toString(), new TypeReference<WorkflowInternalActionDTO>() {});
		String currentKey = exchange.getIn().getHeader("RID", String.class);
		keyBuilder.append(currentKey);
		if (workflowInternalActionDTO.getActionCode() != null && !workflowInternalActionDTO.getActionCode().isEmpty()) {
			keyBuilder.append("_").append(workflowInternalActionDTO.getActionCode().toLowerCase());
		}

        return keyBuilder.toString();
	}
}