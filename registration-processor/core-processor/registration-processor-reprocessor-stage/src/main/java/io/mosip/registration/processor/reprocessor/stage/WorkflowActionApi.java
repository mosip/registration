package io.mosip.registration.processor.reprocessor.stage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class WorkflowActionApi extends MosipVerticleAPIManager {
	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(WorkFlowEventUpdateVerticle.class);

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The audit log request builder. */
	@Autowired
	AuditLogRequestBuilder auditLogRequestBuilder;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${mosip.regproc.workflowaction.server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.workflowaction.eventbus.port}")
	private String eventBusPort;

	@Autowired
	MosipRouter router;

	/**
	 * The context path.
	 */
	@Value("${server.servlet.path}")
	private String contextPath;

	private MosipEventBus mosipEventBus = null;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);


	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), null, null));
		this.routes(router);
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	@Override
	public Integer getEventBusPort() {
		return Integer.parseInt(eventBusPort);
	}

	/**
	 * contains all the routes in this stage
	 *
	 * @param router
	 */
	private void routes(MosipRouter router) {
		router.post(contextPath + "/workflowaction");
		router.handler(this::processURL, this::failure);
	}

	/**
	 * method to process the context received.
	 *
	 * @param ctx the ctx
	 */
	public void processURL(RoutingContext ctx) {
		JsonObject obj = ctx.getBodyAsJson();
		System.out.println("jsonobject" + obj);
		// this.setResponse(ctx,
		// "Packet with registrationId '" + "" + "' has been forwarded to next stage");
	}

	private void failure(RoutingContext routingContext) {
		this.setResponse(routingContext, routingContext.failure().getMessage());
	}
	@Override
	public MessageDTO process(MessageDTO object) {
		return null;
	}

}
