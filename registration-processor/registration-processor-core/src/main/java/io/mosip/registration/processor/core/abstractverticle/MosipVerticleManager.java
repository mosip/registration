package io.mosip.registration.processor.core.abstractverticle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.packetmanager.InfoRequestDto;
import io.mosip.registration.processor.core.packet.dto.packetmanager.InfoResponseDto;

import org.slf4j.MDC;
import org.apache.commons.lang3.exception.ExceptionUtils;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.config.Config;
import com.hazelcast.config.UrlXmlConfig;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.eventbus.MosipEventBusFactory;
import io.mosip.registration.processor.core.exception.DeploymentFailureException;
import io.mosip.registration.processor.core.exception.MessageExpiredException;
import io.mosip.registration.processor.core.exception.UnsupportedEventBusTypeException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.http.RequestWrapper;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.spi.eventbus.EventBusManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * This abstract class is Vert.x implementation for MOSIP.
 * 
 * This class provides functionalities to be used by MOSIP verticles.
 * 
 * @author Pranav Kumar
 * @author Mukul Puspam
 * @since 0.0.1
 *
 */
public abstract class MosipVerticleManager extends AbstractVerticle
		implements EventBusManager<MosipEventBus, MessageBusAddress, MessageDTO> {

	/** The logger. */
	private Logger logger = RegProcessorLogger.getLogger(MosipVerticleManager.class);

	private static final String ID = "mosip.commmons.packetmanager";
    private static final String VERSION = "v1";

    @Autowired
    private RegistrationProcessorRestClientService<Object> restApi;

    @Autowired
    private ObjectMapper objectMapper;

	@Value("${mosip.regproc.eventbus.type:vertx}")
	private String eventBusType;

	@Value("${eventbus.port}")
	private String eventBusPort;

	@Value("${mosip.regproc.message.tag.loading.disable:false}")
	private Boolean disableTagLoading;

	@Autowired
	private MosipEventBusFactory mosipEventBusFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#getEventBus
	 * (java.lang.Class, java.lang.String)
	 */
	@Override
	public MosipEventBus getEventBus(Object verticleName, String clusterManagerUrl) {
		return getEventBus(verticleName, clusterManagerUrl, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.core.spi.eventbus.EventBusManager#getEventBus
	 * (java.lang.Class)
	 */
	@Override
	public MosipEventBus getEventBus(Object verticleName, String clusterManagerUrl, int instanceNumber) {
		CompletableFuture<Vertx> eventBus = new CompletableFuture<>();
		MosipEventBus mosipEventBus = null;
		Config config;
		try {
			config = new UrlXmlConfig(clusterManagerUrl);
		} catch (IOException e1) {
			throw new DeploymentFailureException(PlatformErrorMessages.RPR_CMB_MALFORMED_URL_EXCEPTION.getMessage());
		}
		ClusterManager clusterManager = new HazelcastClusterManager(config);
		String address = null;
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			throw new DeploymentFailureException(PlatformErrorMessages.RPR_CMB_MALFORMED_URL_EXCEPTION.getMessage());
		}

		MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
				.setPrometheusOptions(new VertxPrometheusOptions()
						.setEnabled(true))
				.setEnabled(true);

		VertxOptions options = new VertxOptions().setClustered(true).setClusterManager(clusterManager)
				.setHAEnabled(false).setWorkerPoolSize(instanceNumber)
				.setEventBusOptions(new EventBusOptions().setPort(getEventBusPort()).setHost(address))
				.setMetricsOptions(micrometerMetricsOptions);
		Vertx.clusteredVertx(options, result -> {
			if (result.succeeded()) {
				result.result().deployVerticle((Verticle) verticleName,
						new DeploymentOptions().setHa(false).setWorker(true).setWorkerPoolSize(instanceNumber));
				eventBus.complete(result.result());
				logger.debug(verticleName + " deployed successfully");
			} else {
				throw new DeploymentFailureException(PlatformErrorMessages.RPR_CMB_DEPLOYMENT_FAILURE.getMessage());
			}
		});

		try {
			Vertx vert = eventBus.get();
			mosipEventBus = mosipEventBusFactory.getEventBus(vert, getEventBusType());
		} catch (InterruptedException | ExecutionException | UnsupportedEventBusTypeException e) {
			Thread.currentThread().interrupt();
			throw new DeploymentFailureException(PlatformErrorMessages.RPR_CMB_DEPLOYMENT_FAILURE.getMessage(), e);
		}
		return mosipEventBus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.EventBusManager#
	 * consumeAndSend(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void consumeAndSend(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
			MessageBusAddress toAddress, long messageExpiryTimeLimit) {
		mosipEventBus.consumeAndSend(fromAddress, toAddress, (msg, handler) -> {
			logger.debug("consumeAndSend received from {} {}",fromAddress.toString(), msg.getBody());
			Map<String, String> mdc = MDC.getCopyOfContextMap();
			vertx.executeBlocking(future -> {
				MDC.setContextMap(mdc);
				JsonObject jsonObject = (JsonObject) msg.getBody();
				MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
				if(isMessageExpired(messageDTO, messageExpiryTimeLimit)) {
					future.fail(new MessageExpiredException("rid: " + messageDTO.getRid() +
						" lastHopTimestamp " + messageDTO.getLastHopTimestamp()));
					return;
				}
				MessageDTO result = process(messageDTO);
				if(result.getTags() == null)
					addTagsToMessageDTO(result);
				result.setLastHopTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
				future.complete(result);
			}, false, handler);
			MDC.clear();
		});
	}

	/**
	 * Send.
	 *
	 * @param mosipEventBus
	 *            The Eventbus instance for communication
	 * @param toAddress
	 *            The address on which message is to be sent
	 * @param message
	 *            The message that needs to be sent
	 */
	public void send(MosipEventBus mosipEventBus, MessageBusAddress toAddress, MessageDTO message) {
		if(message.getTags() == null)
			addTagsToMessageDTO(message);
		message.setLastHopTimestamp(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		mosipEventBus.send(toAddress, message);
	}

	/**
	 * Consume.
	 *
	 * @param mosipEventBus
	 *            The Eventbus instance for communication
	 * @param fromAddress
	 *            The address from which message needs to be consumed
	 * @param messageExpiryTimeLimit
	 * 			  The time limit in seconds, after which message should considered as expired
	 */
	public void consume(MosipEventBus mosipEventBus, MessageBusAddress fromAddress,
			long messageExpiryTimeLimit) {
		mosipEventBus.consume(fromAddress, (msg, handler) -> {
			logger.debug("Received from {} {}",fromAddress.toString(), msg.getBody());
			Map<String, String> mdc = MDC.getCopyOfContextMap();
			vertx.executeBlocking(future -> {
				MDC.setContextMap(mdc);
				JsonObject jsonObject = (JsonObject) msg.getBody();
				MessageDTO messageDTO = jsonObject.mapTo(MessageDTO.class);
				if(isMessageExpired(messageDTO, messageExpiryTimeLimit)) {
					future.fail(new MessageExpiredException("rid: " + messageDTO.getRid() +
						" lastHopTimestamp " + messageDTO.getLastHopTimestamp()));
					return;
				}
				MessageDTO result = process(messageDTO);
				future.complete(result);
			}, false, handler);
			MDC.clear();
		});
	}

	public Integer getEventBusPort() {
		return Integer.parseInt(eventBusPort);
	}

	public String getEventBusType() {
		return this.eventBusType;
	}

	//TODO Temporarely added for passing the existing unit test case, later to be removed and unit test case to be changed based on SpringRunner
	protected void setMosipEventBusFactory(MosipEventBusFactory mosipEventBusFactory) {
		this.mosipEventBusFactory = mosipEventBusFactory;
	}

	private void addTagsToMessageDTO(MessageDTO messageDTO) {
		if(disableTagLoading) {
			messageDTO.setTags(new HashMap<>());
			return;
		}
		try {
			messageDTO.setTags(getTagsFromPacket(messageDTO.getRid()));
		} catch (ApisResourceAccessException | PacketManagerException |
				JsonProcessingException | IOException e) {
			logger.error(PlatformErrorMessages.RPR_SYS_PACKET_TAGS_COPYING_FAILED.getCode() +
				" -- " + PlatformErrorMessages.RPR_SYS_PACKET_TAGS_COPYING_FAILED.getMessage() +
				e.getMessage() + ExceptionUtils.getStackTrace(e));
			messageDTO.setInternalError(true);
		}
	}

	private Map<String, String> getTagsFromPacket(String id) throws ApisResourceAccessException,
			PacketManagerException, JsonProcessingException, IOException {
        InfoRequestDto infoRequestDto = new InfoRequestDto(id);

        RequestWrapper<InfoRequestDto> request = new RequestWrapper<>();
        request.setId(ID);
        request.setVersion(VERSION);
        request.setRequesttime(DateUtils.getUTCCurrentDateTime());
        request.setRequest(infoRequestDto);
        ResponseWrapper<InfoResponseDto> response = (ResponseWrapper) restApi.postApi(
			ApiName.PACKETMANAGER_INFO, "", "", request, ResponseWrapper.class);

        if (response.getErrors() != null && response.getErrors().size() > 0) {
			throw new PacketManagerException(response.getErrors().get(0).getErrorCode(),
				response.getErrors().get(0).getMessage());
        }

        InfoResponseDto infoResponseDto = objectMapper.readValue(JsonUtils.javaObjectToJsonString(
			response.getResponse()), InfoResponseDto.class);
		return infoResponseDto.getTags();
	}

	private boolean isMessageExpired(MessageDTO messageDTO, long messageExpiryTimeLimit) {
		if(messageExpiryTimeLimit <= 0)
			return false;
		try {
			LocalDateTime lastHopDateTime = DateUtils.parseUTCToLocalDateTime(messageDTO.getLastHopTimestamp());
			LocalDateTime nowDateTime = LocalDateTime.now();
			if(ChronoUnit.SECONDS.between(lastHopDateTime, nowDateTime) <= messageExpiryTimeLimit)
				return false;
			return true;
		} catch(Exception e) {
			logger.error("{} {} {} {}", PlatformErrorMessages.RPR_SYS_PARSING_DATE_EXCEPTION.getCode(),
				PlatformErrorMessages.RPR_SYS_PARSING_DATE_EXCEPTION.getMessage(), e.getMessage(),
				ExceptionUtils.getStackTrace(e));
			return true;
		}
	}

}
