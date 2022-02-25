package io.mosip.registration.processor.stages.introducervalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;

@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.config", "io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", "io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", "io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.kernel.biosdk.provider.impl" })
public class IntroducerValidatorStage extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.introducer-validator.";

	@Autowired
	IntroducerValidationProcessor introducerValidationProcessor;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/**
	 * Vertx cluster configuration file URL, which ensures all verticle joins the
	 * same cluster
	 */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/**
	 * worker pool size is the maximum number of worker threads that will be used by
	 * the Vert.x instance
	 */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/**
	 * After this time intervel, message should be considered as expired (In
	 * seconds).
	 */
	@Value("${mosip.regproc.introducer-validator.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.INTRODUCER_VALIDATOR_BUS_IN,
				MessageBusAddress.INTRODUCER_VALIDATOR_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.INTRODUCER_VALIDATOR_BUS_IN,
				MessageBusAddress.INTRODUCER_VALIDATOR_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		return introducerValidationProcessor.process(object, getStageName());
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

}
