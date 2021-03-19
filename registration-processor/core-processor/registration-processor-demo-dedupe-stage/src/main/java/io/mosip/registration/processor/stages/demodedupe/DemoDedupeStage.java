package io.mosip.registration.processor.stages.demodedupe;

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
 * The Class DemodedupeStage.
 *
 * @author M1048358 Alok Ranjan
 */

@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.config", "io.mosip.registration.processor.demo.dedupe.config",
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.packet.manager.config",
		"io.mosip.kernel.packetmanager.config"})
public class DemoDedupeStage extends MosipVerticleAPIManager {

	private static final String MOSIP_REGPROC_DEMO_DEDUPE = "mosip.regproc.demo.dedupe.";
	
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;
	
	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	private MosipEventBus mosipEventBus = null;
	@Autowired
	DemodedupeProcessor demodedupeProcessor;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;
	

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.DEMO_DEDUPE_BUS_IN, MessageBusAddress.DEMO_DEDUPE_BUS_OUT);
	}

	@Override
	public void start(){
		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), MessageBusAddress.DEMO_DEDUPE_BUS_IN, MessageBusAddress.DEMO_DEDUPE_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}
	
	@Override
	protected String getPropertyPrefix() {
		return MOSIP_REGPROC_DEMO_DEDUPE;
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
		return demodedupeProcessor.process(object, this.getClass().getSimpleName());

	}

}