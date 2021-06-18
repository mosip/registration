package io.mosip.registration.processor.stages.packetclassifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
public class PacketClassifierStage extends MosipVerticleAPIManager {

	/** Packet Classification Processor which holds the business logic of packet classification */
	@Autowired
	PacketClassificationProcessor packetClassificationProcessor;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/** Vertx cluster configuration file URL, which ensures all verticle joins the same cluster */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number on which REST APIs are exposed */
	@Value("${mosip.regproc.packet.classifier.server.port}")
	private String port;

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
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	@Override
	public Integer getEventBusPort() {
		return Integer.parseInt(eventBusPort);
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
		return packetClassificationProcessor.process(object, this.getClass().getSimpleName());
	}

}
