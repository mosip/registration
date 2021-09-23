package io.mosip.registration.processor.camel.bridge.intercepter;

import java.util.List;

import org.apache.camel.CamelContext;
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
}