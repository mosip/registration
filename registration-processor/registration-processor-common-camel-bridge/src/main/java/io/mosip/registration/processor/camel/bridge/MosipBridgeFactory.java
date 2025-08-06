package io.mosip.registration.processor.camel.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.mosip.registration.processor.core.eventbus.MosipEventBusFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.component.vertx.VertxComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.RoutesDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.camel.bridge.intercepter.RouteIntercepter;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.UnsupportedEventBusTypeException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.camel.CamelBridge;
import io.vertx.camel.CamelBridgeOptions;

/**
 * This class starts Vertx camel bridge.
 *
 * @author Mukul Puspam
 * @author Pranav kumar
 * @since 0.0.1
 */
@Component
public class MosipBridgeFactory extends MosipVerticleAPIManager {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(MosipBridgeFactory.class);

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.camel.bridge.";

	@Value("${mosip.regproc.eventbus.type:vertx}")
	private String eventBusType;

	@Autowired
	ApplicationContext applicationContext;
	@Autowired
	Environment environment;
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	@Autowired
	private MosipEventBusFactory mosipEventBusFactory;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	@Autowired
	private RouteIntercepter routeIntercepter;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	@Value("${mosip.regproc.eventbus.kafka.bootstrap.servers}")
	private String kafkaBootstrapServers;

	@Value("${mosip.regproc.eventbus.kafka.group.id}")
	private String kafkaGroupId;

	/**
	 * Gets the event bus.
	 *
	 * @return the event bus
	 */
	public void getEventBus() {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"camel bridge event bus ", "MosipBridgeFactory::getEventBus()::entry");
		mosipEventBus = this.getEventBus(this, clusterManagerUrl);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"MosipBridgeFactory::getEventBus()::exit", String.valueOf(mosipEventBus));
	}

	public void startCamelBridge() throws Exception {
		String camelRoutesFileName;
		JndiRegistry registry = null;
		CompletableFuture<MosipEventBus> eventBus = CompletableFuture.completedFuture(mosipEventBus);
		CompletableFuture<Void> future = CompletableFuture.allOf(eventBus);
		future.join();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"MosipBridgeFactory::start()::entry::mosipEventBus ", String.valueOf(mosipEventBus));

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"mosipEventBus.getEventbus() ", String.valueOf(mosipEventBus.getEventbus()));

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"router ", String.valueOf(router));

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"router post url ", String.valueOf(this.postUrl(mosipEventBus.getEventbus(), null, null)));

		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), null, null));

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"router set  ", "router.setRoute()");
		this.createServer(router.getRouter(), getPort());
		String[] beanNames = applicationContext.getBeanDefinitionNames();
		if (beanNames != null) {
			Map<String, String> enviroment = new HashMap<>();
			enviroment.put("java.naming.factory.initial", "org.apache.camel.util.jndi.CamelInitialContextFactory");
			registry = new JndiRegistry(enviroment);
			for (String name : beanNames) {
				registry.bind(name, applicationContext.getBean(name));
			}
		}
		String zone = environment.getProperty("registration.processor.zone");
		if (zone != null && zone.equalsIgnoreCase("dmz")) {
			camelRoutesFileName = environment.getProperty("camel.dmz.active.flows.file.names");
		} else {
			camelRoutesFileName = environment.getProperty("camel.secure.active.flows.file.names");
		}
		CamelContext camelContext = new DefaultCamelContext(registry);
		camelContext.setStreamCaching(true);
		if (camelRoutesFileName == null) {
			throw new RuntimeException("Property camel.secure.active.flows.file.names not found");
		}
		List<String> camelRoutesFilesArr = Arrays.asList(camelRoutesFileName.split(","));
		RestTemplate restTemplate = new RestTemplate();
		String camelRoutesBaseUrl = environment.getProperty("camel.routes.url");
		ResponseEntity<Resource> responseEntity;
		RoutesDefinition routes;
		for (String camelRouteFileName : camelRoutesFilesArr) {
			String camelRoutesUrl = camelRoutesBaseUrl + camelRouteFileName;
			responseEntity = restTemplate.exchange(camelRoutesUrl, HttpMethod.GET, null, Resource.class);
			Resource body=responseEntity.getBody();
			if (body == null) {
				throw new RuntimeException("Response for " + camelRoutesUrl + " is null");
			}
			routes = camelContext.loadRoutesDefinition(body.getInputStream());
			camelContext.addRouteDefinitions(routeIntercepter.intercept(camelContext, routes.getRoutes()));
			// camelContext.addRouteDefinitions(routes.getRoutes());
		}
		if (eventBusType.equals("vertx")) {
			VertxComponent vertxComponent = new VertxComponent();
			vertxComponent.setVertx(vertx);
			camelContext.addComponent("eventbus", vertxComponent);
			camelContext.addComponent("workflow-cmd", vertxComponent);
		} else if (eventBusType.equals("kafka")) {
			KafkaComponent kafkaComponent = new KafkaComponent();
			KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
			kafkaConfiguration.setGroupId(kafkaGroupId);
			kafkaConfiguration.setBrokers(kafkaBootstrapServers);
			kafkaConfiguration.setMaxPollRecords(Integer.valueOf(this.mosipEventBusFactory.getMaxPollRecords(STAGE_PROPERTY_PREFIX)));
			kafkaConfiguration.setMaxPollIntervalMs(Long.valueOf(this.mosipEventBusFactory.getMaxPollInterval(STAGE_PROPERTY_PREFIX)));
			kafkaComponent.setConfiguration(kafkaConfiguration);
			camelContext.addComponent("eventbus", kafkaComponent);
			camelContext.addComponent("workflow-cmd", kafkaComponent);
			camelContext.setUseMDCLogging(true);
			camelContext.setUnitOfWorkFactory(CustomMDCUnitOfWork::new);
		} else
			throw new UnsupportedEventBusTypeException(
					PlatformErrorMessages.RPR_CMB_CONFIGURATION_SERVER_FAILURE_EXCEPTION);

		camelContext.start();
		CamelBridge.create(vertx, new CamelBridgeOptions(camelContext)).start();
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}
}
