/**
 * 
 */
package io.mosip.registration.processor.stages.packet.validator;

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
 * The Class PacketValidatorStage.
 *
 * @author M1022006
 * @author Girish Yarru
 */

@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.stages.config", 
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.rest.client.config", 
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.packet.manager.config", 
		"io.mosip.kernel.idobjectvalidator.config",
		"io.mosip.registration.processor.core.kernel.beans" })
public class PacketValidatorStage extends MosipVerticleAPIManager {
	
	private static final String MOSIP_REGPROC_PACKET_VALIDATOR = "mosip.regproc.packet.validator.";


	/** Paacket validate Processor */
	@Autowired
	PacketValidateProcessor packetvalidateprocessor;

	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** The mosip event bus. */
	MosipEventBus mosipEventBus = null;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.PACKET_VALIDATOR_BUS_IN,
				MessageBusAddress.PACKET_VALIDATOR_BUS_OUT);
	}

	@Override
	public void start(){
		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), MessageBusAddress.PACKET_VALIDATOR_BUS_IN,
				MessageBusAddress.PACKET_VALIDATOR_BUS_OUT));
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
		return packetvalidateprocessor.process(object, this.getClass().getSimpleName());
	}
	
	@Override
	protected String getPropertyPrefix() {
		return MOSIP_REGPROC_PACKET_VALIDATOR;
	}

}
