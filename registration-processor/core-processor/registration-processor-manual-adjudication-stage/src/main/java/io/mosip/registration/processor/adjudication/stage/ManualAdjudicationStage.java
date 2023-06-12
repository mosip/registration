package io.mosip.registration.processor.adjudication.stage;

import java.util.List;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.adjudication.exception.InvalidMessageException;
import io.mosip.registration.processor.adjudication.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.adjudication.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.adjudication.service.ManualAdjudicationService;
import io.mosip.registration.processor.adjudication.util.ManualVerificationRequestValidator;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.QueueConnectionNotFound;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * This class sends message to next stage after successful completion of manual
 * verification.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
@RefreshScope
@Service
@Configuration
@ComponentScan(basePackages = { "${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.core.config",
		"io.mosip.registration.processor.adjudication.config",
		"io.mosip.registration.processor.packet.receiver.config",
		"io.mosip.registration.processor.packet.manager.config",
		"io.mosip.kernel.packetmanager.config",
		"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.adjudication.validators"})
public class ManualAdjudicationStage extends MosipVerticleAPIManager {

	private static final String STAGE_PROPERTY_PREFIX = "mosip.regproc.manual.adjudication.";

	@Autowired
	private ManualAdjudicationService manualAdjudicationService;

	/** The core audit request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The mosip event bus. */
	private MosipEventBus mosipEventBus;

	/** The mosip queue manager. */
	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;

	/**
	 * vertx Cluster Manager Url
	 */
	@Value("${vertx.cluster.configuration}")
	private String clusterManagerUrl;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ManualAdjudicationStage.class);

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	ManualVerificationRequestValidator manualVerificationRequestValidator;

	@Autowired
	ManualVerificationExceptionHandler manualVerificationExceptionHandler;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	private MosipQueue queue;

	/** The mosip connection factory. */
	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	/** The username. */
	@Value("${registration.processor.manual.adjudication.queue.username}")
	private String username;

	/** The password. */
	@Value("${registration.processor.manual.adjudication.queue.password}")
	private String password;

	/** The type of queue. */
	@Value("${registration.processor.manual.adjudication.queue.typeOfQueue}")
	private String typeOfQueue;

	/** The address. */
	@Value("${registration.processor.manual.adjudication.queue.response:adjudication-to-mosip}")
	private String mvResponseAddress;

	@Value("#{'${registration.processor.manual.adjudication.queue.trusted.packages}'.split(',')}")
	private List<String> trustedPackages;

	/** The Constant FAIL_OVER. */
	private static final String FAIL_OVER = "failover:(";

	/** The Constant RANDOMIZE_FALSE. */
	private static final String RANDOMIZE_FALSE = ")?randomize=false";

	private static final String CONFIGURE_MONITOR_IN_ACTIVITY = "?wireFormat.maxInactivityDuration=0";

	/** The url. */
	@Value("${registration.processor.manual.adjudication.queue.url}")
	private String url;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.manual.adjudication.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	private static final String APPLICATION_JSON = "application/json";

	/**
	 * Deploy stage.
	 */
	public void deployVerticle() {
		this.mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.MANUAL_ADJUDICATION_BUS_IN, messageExpiryTimeLimit);
		queue = getQueueConnection();
		if (queue != null) {

			QueueListener listener = new QueueListener() {
				@Override
				public void setListener(Message message) {
					consumerListener(message);
				}
			};

			mosipQueueManager.consume(queue, mvResponseAddress, listener);

		} else {
			throw new QueueConnectionNotFound(PlatformErrorMessages.RPR_PRT_QUEUE_CONNECTION_NULL.getMessage());
		}

	}

	@Override
	public void start() {
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.MANUAL_ADJUDICATION_BUS_IN, MessageBusAddress.MANUAL_ADJUDICATION_BUS_OUT));
		this.createServer(router.getRouter(), getPort());
	}

	@Override
	protected String getPropertyPrefix() {
		return STAGE_PROPERTY_PREFIX;
	}

	public void sendMessage(MessageDTO messageDTO) {
		this.send(this.mosipEventBus, MessageBusAddress.MANUAL_ADJUDICATION_BUS_OUT, messageDTO);
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		return manualAdjudicationService.process(object, queue);
	}

	private MosipQueue getQueueConnection() {
		String failOverBrokerUrl = FAIL_OVER + url + "," + url + RANDOMIZE_FALSE;
		return mosipConnectionFactory.createConnection(typeOfQueue, username, password, failOverBrokerUrl,
				trustedPackages);
	}

	public void consumerListener(Message message) {
		try {
			String response = null;
			if (message instanceof ActiveMQBytesMessage) {
				response = new String(((ActiveMQBytesMessage) message).getContent().data);
			} else if (message instanceof ActiveMQTextMessage) {
				TextMessage textMessage = (TextMessage) message;
				response = textMessage.getText();
			}
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFERENCEID.toString(),
					"Response received from mv system", response);
			if (response == null) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
						PlatformErrorMessages.RPR_INVALID_MESSSAGE.getCode(), PlatformErrorMessages.RPR_INVALID_MESSSAGE.getMessage());
				throw new InvalidMessageException(PlatformErrorMessages.RPR_INVALID_MESSSAGE.getCode(), PlatformErrorMessages.RPR_INVALID_MESSSAGE.getMessage());
			}
			ManualAdjudicationResponseDTO resp = JsonUtil.readValueWithUnknownProperties(response, ManualAdjudicationResponseDTO.class);
			if (resp != null) {
				boolean isProcessingSuccessful = manualAdjudicationService.updatePacketStatus(resp,getStageName(),queue);
				
				if (isProcessingSuccessful)
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
							"", "ManualVerificationStage::processDecision::success");

			}
		} catch (Exception e) {
			regProcLogger.error("","","", ExceptionUtils.getStackTrace(e));
		}

	}
}

