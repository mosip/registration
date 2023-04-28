package io.mosip.registration.processor.manual.verification.stage;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.abstractverticle.MosipRouter;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.common.rest.dto.BaseRestResponseDTO;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.queue.factory.QueueListener;
import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.manual.verification.constants.ManualVerificationConstants;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.manual.verification.exception.InvalidMessageException;
import io.mosip.registration.processor.manual.verification.exception.handler.ManualVerificationExceptionHandler;
import io.mosip.registration.processor.manual.verification.response.builder.ManualVerificationResponseBuilder;
import io.mosip.registration.processor.manual.verification.response.dto.ManualAdjudicationResponseDTO;
import io.mosip.registration.processor.manual.verification.service.ManualVerificationService;
import io.mosip.registration.processor.manual.verification.util.ManualVerificationRequestValidator;
import io.mosip.registration.processor.packet.storage.exception.QueueConnectionNotFound;
import io.mosip.registration.processor.packet.storage.utils.IdSchemaUtil;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.LinkedHashMap;

/**
 * This class sends message to next stage after successful completion of manual
 * verification.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
@Component
public class ManualVerificationStage extends MosipVerticleAPIManager {


	@Autowired
	private ManualVerificationService manualAdjudicationService;

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
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ManualVerificationStage.class);

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	ManualVerificationRequestValidator manualVerificationRequestValidator;

	@Autowired
	ManualVerificationExceptionHandler manualVerificationExceptionHandler;

	@Autowired
	ManualVerificationResponseBuilder manualVerificationResponseBuilder;

	/** Mosip router for APIs */
	@Autowired
	MosipRouter router;

	private MosipQueue queue;

	/** The mosip connection factory. */
	@Autowired
	private MosipQueueConnectionFactory<MosipQueue> mosipConnectionFactory;

	/** The username. */
	@Value("${registration.processor.queue.username}")
	private String username;

	/** The password. */
	@Value("${registration.processor.queue.password}")
	private String password;

	/** The type of queue. */
	@Value("${registration.processor.queue.typeOfQueue}")
	private String typeOfQueue;

	/** The address. */
	@Value("${registration.processor.queue.manualverification.response:mv-to-mosip}")
	private String mvResponseAddress;

	/** The Constant FAIL_OVER. */
	private static final String FAIL_OVER = "failover:(";

	/** The Constant RANDOMIZE_FALSE. */
	private static final String RANDOMIZE_FALSE = ")?randomize=false";

	private static final String CONFIGURE_MONITOR_IN_ACTIVITY = "?wireFormat.maxInactivityDuration=0";

	/** The url. */
	@Value("${registration.processor.queue.url}")
	private String url;

	/**
	 * server port number
	 */
	@Value("${server.port}")
	private String port;

	/** worker pool size. */
	@Value("${worker.pool.size}")
	private Integer workerPoolSize;

	/** After this time intervel, message should be considered as expired (In seconds). */
	@Value("${mosip.regproc.manual.verification.message.expiry-time-limit}")
	private Long messageExpiryTimeLimit;

	@Value("${server.servlet.path}")
	private String contextPath;

	private static final String APPLICATION_JSON = "application/json";

	/**
	 * Deploy stage.
	 */
	public void deployStage() {
		this.mosipEventBus = this.getEventBus(this, clusterManagerUrl, workerPoolSize);
		this.consume(mosipEventBus, MessageBusAddress.MANUAL_VERIFICATION_BUS_IN, messageExpiryTimeLimit);
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
		router.setRoute(this.postUrl(getVertx(), MessageBusAddress.MANUAL_VERIFICATION_BUS_IN, MessageBusAddress.MANUAL_VERIFICATION_BUS_OUT));
		this.createServer(router.getRouter(), Integer.parseInt(port));
	}


	public void sendMessage(MessageDTO messageDTO) {
		this.send(this.mosipEventBus, MessageBusAddress.MANUAL_VERIFICATION_BUS_OUT, messageDTO);
	}

	@Override
	public MessageDTO process(MessageDTO object) {
		return manualAdjudicationService.process(object, queue);
	}

	private MosipQueue getQueueConnection() {
		String failOverBrokerUrl = FAIL_OVER + url + "," + url + RANDOMIZE_FALSE;
		return mosipConnectionFactory.createConnection(typeOfQueue, username, password, failOverBrokerUrl);
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
			regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
					"Response received from mv system", response);
			if (response == null) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
						PlatformErrorMessages.RPR_INVALID_MESSSAGE.getCode(), PlatformErrorMessages.RPR_INVALID_MESSSAGE.getMessage());
				throw new InvalidMessageException(PlatformErrorMessages.RPR_INVALID_MESSSAGE.getCode(), PlatformErrorMessages.RPR_INVALID_MESSSAGE.getMessage());
			}
			ManualAdjudicationResponseDTO resp = JsonUtil.readValueWithUnknownProperties(response, ManualAdjudicationResponseDTO.class);
			if (resp != null) {
				boolean isProcessingSuccessful = manualAdjudicationService.updatePacketStatus(resp, this.getClass().getSimpleName(),queue);
				
				if (isProcessingSuccessful)
					regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
							"", "ManualVerificationStage::processDecision::success");

			}
		} catch (Exception e) {
			regProcLogger.error("","","", ExceptionUtils.getStackTrace(e));
		}

	}
}

