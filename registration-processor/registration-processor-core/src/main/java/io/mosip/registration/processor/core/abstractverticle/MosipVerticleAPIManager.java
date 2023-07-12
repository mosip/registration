package io.mosip.registration.processor.core.abstractverticle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Tracing;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.processor.core.constant.HealthConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.tracing.VertxWebTracingLocal;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;

/**
 * @author Mukul Puspam
 *
 */
public abstract class MosipVerticleAPIManager extends MosipVerticleManager {

	@Value("${registration.processor.signature.isEnabled}")
	Boolean isEnabled;

	@Autowired
	DigitalSignatureUtility digitalSignatureUtility;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	@Autowired
	private Tracing tracing;
	
	@Autowired
	private Environment environment;

	@Autowired(required = false)
    private VirusScanner virusScanner;

	/** The mosip queue manager. */
	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	private static final String PROMETHEUS_ENDPOINT = "/actuator/prometheus";


	private static Logger regProcLogger = RegProcessorLogger.getLogger(MosipVerticleAPIManager.class);

	@Value("${mosip.regproc.health-check.handler-timeout:2000}")
	private long healthCheckTimeOut;


	/**
	 * This method creates a body handler for the routes
	 *
	 * @param vertx
	 * @return
	 */
	public Router postUrl(Vertx vertx, MessageBusAddress consumeAddress, MessageBusAddress sendAddress) {
		Router router = Router.router(vertx);

		VertxWebTracingLocal vertxWebTracing = VertxWebTracingLocal.create(tracing);
		Handler<RoutingContext> routingContextHandler = vertxWebTracing.routingContextHandler();
		router.route()
				.order(-1) //applies before routes
				.handler(routingContextHandler)
				.failureHandler(routingContextHandler);

		router.route().handler(BodyHandler.create());
		String servletPath = getServletPath();
		router.get(servletPath + PROMETHEUS_ENDPOINT).handler(PrometheusScrapingHandler.create());
		if (consumeAddress == null && sendAddress == null)
			configureHealthCheckEndpoint(vertx, router, servletPath, null,
					null);
		else if (consumeAddress == null)
			configureHealthCheckEndpoint(vertx, router, servletPath, null,
					sendAddress.getAddress());
		else if (sendAddress == null)
			configureHealthCheckEndpoint(vertx, router, servletPath,
					consumeAddress.getAddress(), null);
		else
			configureHealthCheckEndpoint(vertx, router, servletPath,
					consumeAddress.getAddress(), sendAddress.getAddress());
		return router;
	}

	public void configureHealthCheckEndpoint(Vertx vertx, Router router, final String servletPath,
			String consumeAddress, String sendAddress) {
		StageHealthCheckHandler healthCheckHandler = new StageHealthCheckHandler(vertx, null, objectMapper,
                virusScanner, environment);
		router.get(servletPath + HealthConstant.HEALTH_ENDPOINT).handler(healthCheckHandler);
		if (servletPath.contains("packetreceiver")) {
			healthCheckHandler.register("virusscanner", healthCheckTimeOut, healthCheckHandler::virusScanHealthChecker);
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Verticle",
					healthCheckTimeOut,
					future -> healthCheckHandler.senderHealthHandler(future, vertx, super.mosipEventBus, sendAddress));
		}
		if (servletPath.contains("uploader")) {
			healthCheckHandler.register("virusscanner", healthCheckTimeOut, healthCheckHandler::virusScanHealthChecker);
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Consume",
					healthCheckTimeOut, future -> {
						healthCheckHandler.consumerHealthHandler(future, vertx, super.mosipEventBus, consumeAddress);
					});
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Verticle",
					healthCheckTimeOut,
					future -> healthCheckHandler.senderHealthHandler(future, vertx, super.mosipEventBus, sendAddress));
		}
		if (checkServletPath(servletPath)) {
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Send",
					healthCheckTimeOut, future -> {
						healthCheckHandler.senderHealthHandler(future, vertx, super.mosipEventBus, sendAddress);
					});
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Consume",
					healthCheckTimeOut,
					future -> {
						healthCheckHandler.consumerHealthHandler(future, vertx, super.mosipEventBus, consumeAddress);
					});
		}
		if (servletPath.contains("manual")) {
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Verticle",
					healthCheckTimeOut,
					future -> healthCheckHandler.senderHealthHandler(future, vertx, super.mosipEventBus, sendAddress));
		}
		if (servletPath.contains("abismiddleware")) {
			healthCheckHandler.register("queuecheck", healthCheckTimeOut,
					future -> healthCheckHandler.queueHealthChecker(future, mosipQueueManager, mosipConnectionFactory));
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Verticle",
					healthCheckTimeOut,
					future -> healthCheckHandler.consumerHealthHandler(future, vertx, super.mosipEventBus,
							consumeAddress));
		}
		if (servletPath.contains("sender")) {
			healthCheckHandler.register(
					servletPath.substring(servletPath.lastIndexOf("/") + 1, servletPath.length()) + "Verticle",
					healthCheckTimeOut,
					future -> healthCheckHandler.consumerHealthHandler(future, vertx, super.mosipEventBus,
							consumeAddress));
		}

		healthCheckHandler.register("diskSpace", healthCheckTimeOut, healthCheckHandler::dispSpaceHealthChecker);
		if (!servletPath.contains("camel")) {
		healthCheckHandler.register("db", healthCheckTimeOut, healthCheckHandler::databaseHealthChecker);
		}
	}

	private boolean checkServletPath(String servletPath) {
		return servletPath.contains("packetvalidator") || servletPath.contains("osi") || servletPath.contains("demo")
				|| servletPath.contains("bio") || servletPath.contains("uin") || servletPath.contains("quality")
				|| servletPath.contains("abishandler") || servletPath.contains("securezone")
				|| servletPath.contains("print") || servletPath.contains("cmd") || servletPath.contains("operator")
				|| servletPath.contains("supervisor") || servletPath.contains("introducer")
				|| servletPath.contains("final") || servletPath.contains("biometric")
				|| servletPath.contains("packetclassifier") || servletPath.contains("bioauth")
				|| servletPath.contains("external");
	}

	/**
	 * This method creates server for vertx web application
	 *
	 * @param router
	 * @param port
	 */
	public void createServer(Router router, int port) {
		vertx.createHttpServer().requestHandler(router::accept).listen(port);
	}

	/**
	 * This method returns a response to the routing context
	 *
	 * @param ctx
	 * @param object
	 */
	public void setResponse(RoutingContext ctx, Object object) {
		ctx.response().putHeader("content-type", "text/plain").putHeader("Access-Control-Allow-Origin", "*")
				.putHeader("Access-Control-Allow-Methods", "GET, POST").setStatusCode(200)
				.end(Json.encodePrettily(object));
	};

	/**
	 * This method returns a response to the routing context
	 *
	 * @param ctx
	 * @param object
	 * @param contentType
	 */
	public void setResponseWithDigitalSignature(RoutingContext ctx, Object object, String contentType) {
		HttpServerResponse response = ctx.response();

		String res=null;
		try {
			res = objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			regProcLogger.error("Error while processing response",e);

		}

		if (isEnabled)
			response.putHeader("Response-Signature",
					digitalSignatureUtility.getDigitalSignature(res));
		response.putHeader("content-type", contentType).putHeader("Access-Control-Allow-Origin", "*")
				.putHeader("Access-Control-Allow-Methods", "GET, POST").setStatusCode(200)
				.end(res);
	}

	// Added this method to cast all the stages to this class and invoke the deployVerticle method 
	// to start the stage by configuration, since we don't want to test all the stages now, not marking this as
	// an abstract method, but later this need to be marked as abstract
	public void deployVerticle() {

	}
	

	/**
	 * Gets the stage name.
	 *
	 * @return the stage name
	 */
	protected String getStageName() {
		return ClassUtils.getUserClass(this.getClass()).getSimpleName();
	}
}