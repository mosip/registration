package io.mosip.registration.processor.packet.uploader.stage;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.packet.uploader.service.PacketUploaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The Class PacketUploaderStage.
 *
 * @author Rishabh Keshari
 */
@Component
public class PacketUploaderStage extends MosipVerticleAPIManager {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketUploaderStage.class);

	/** The cluster url. */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** server port number. */
	@Value("${server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/**
	 * The mosip event bus.
	 */
	private MosipEventBus mosipEventBus;

	/** The context path. */
	@Value("${server.servlet.path}")
	private String contextPath;

	/** The packet uploader service. */
	@Autowired
	PacketUploaderService<MessageDTO> packetUploaderService;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	/**
	 * Deploy verticle.
	 */
	public void deployVerticle() {
		this.mosipEventBus = this.getEventBus(this, clusterManagerUrl ,workerPoolSize);
		this.consumeAndSend(mosipEventBus, MessageBusAddress.PACKET_UPLOADER_IN,
				MessageBusAddress.PACKET_UPLOADER_OUT);
	}

	@Override
	public void start(){
		router.setRoute(this.postUrl(mosipEventBus.getEventbus(), MessageBusAddress.PACKET_UPLOADER_IN,
				MessageBusAddress.PACKET_UPLOADER_OUT));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#process(
	 * java.lang.Object)
	 */
	@Override
	public MessageDTO process(MessageDTO messageDTO) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"PacketUploaderStage::processURL()::entry");

		messageDTO.setMessageBusAddress(MessageBusAddress.PACKET_UPLOADER_IN);
		messageDTO.setInternalError(Boolean.FALSE);
		messageDTO.setIsValid(Boolean.FALSE);
		messageDTO = packetUploaderService.validateAndUploadPacket(messageDTO.getRid(),
				this.getClass().getSimpleName());

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				"PacketUploaderStage::processURL()::exit", messageDTO.toString());

		return messageDTO;
	}

}
