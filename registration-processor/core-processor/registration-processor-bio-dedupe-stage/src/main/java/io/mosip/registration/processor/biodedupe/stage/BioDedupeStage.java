/**
 * 
 */
package io.mosip.registration.processor.biodedupe.stage;

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
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleManager;

/**
 * The Class BioDedupeStage.
 *
 * @author Sowmya
 */
@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.biodedupe.config", "io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.packet.manager.config",
		"io.mosip.kernel.packetmanager.config"})
public class BioDedupeStage extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.bio.dedupe.";
	
	/** The cluster manager url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	@Autowired
	BioDedupeProcessor bioDedupeProcessor;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.bio.dedupe.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	private MosipEventBus mosipEventBus = null;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.BIO_DEDUPE_BUS_IN, 
			MessageBusAddress.BIO_DEDUPE_BUS_OUT, messageExpiryTimeLimit);
	}

	@Override
	public void start(){
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.BIO_DEDUPE_BUS_IN, MessageBusAddress.BIO_DEDUPE_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}
	
	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
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
			return bioDedupeProcessor.process(object, getStageName());
		}

	public void sendMessage(MessageDTO messageDTO) {
		this.send(this.mosipEventBus, MessageBusAddress.BIO_DEDUPE_BUS_OUT, messageDTO);
	}
}
