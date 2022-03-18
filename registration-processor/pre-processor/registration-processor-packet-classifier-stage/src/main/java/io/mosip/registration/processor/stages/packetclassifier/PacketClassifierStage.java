package io.mosip.registration.processor.stages.packetclassifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;

/**
 * The Class PacketClassiferStage will deploy the stage, consumes and processes events 
 * from event bus and exposes required REST APIs
 *
 * @author Vishwanath V
 */
@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.config", 
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", 
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", 
		"io.mosip.kernel.idobjectvalidator.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.stages.packetclassifier.tagging.impl" })
public class PacketClassifierStage extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.packet.classifier.";

	/** Packet Classification Processor which holds the business logic of packet classification */
	@Autowired
	PacketClassificationProcessor packetClassificationProcessor;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/** Vertx cluster configuration file URL, which ensures all verticle joins the same cluster */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** worker pool size is the maximum number of worker threads that will be used by the Vert.x instance */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	@Value("${mosip.regproc.packet.classifier.eventbus.port}")
	private String eventBusPort;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.packet.classifier.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	/**
	 * Deploys the vertx verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.PACKET_CLASSIFIER_BUS_IN,
				MessageBusAddress.PACKET_CLASSIFIER_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	public void start(){
		router.setRoute(this.postUrl(getVertx(),
			MessageBusAddress.PACKET_CLASSIFIER_BUS_IN, MessageBusAddress.PACKET_CLASSIFIER_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#process(
	 * java.lang.Object)
	 */

	@Override
	public MessageDTO process(MessageDTO object) {
		return packetClassificationProcessor.process(object, getStageName());
	}
	
	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

}
