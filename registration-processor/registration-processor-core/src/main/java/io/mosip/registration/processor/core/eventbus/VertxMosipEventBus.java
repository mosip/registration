package io.mosip.registration.processor.core.eventbus;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.HealthCheckDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.constant.HealthConstant;
import io.mosip.registration.processor.core.exception.MessageExpiredException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.mosip.registration.processor.core.tracing.EventTracingHandler;
import io.mosip.registration.processor.core.tracing.MDCHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;



/**
 * Implementation of MosipEventBus interface for Vertx navtive event bus
 *
 * @author Vishwanath V
 */
public class VertxMosipEventBus implements MosipEventBus {

	/** The logger. */
	private Logger logger = RegProcessorLogger.getLogger(VertxMosipEventBus.class);

	/** The vertx instance that will be used by this event bus */
	private Vertx vertx = null;

	private EventTracingHandler eventTracingHandler;

	/**
	 * Instantiates a new vertx mosip event bus.
	 *
	 * @param vertx
	 *            The vertx instance
	 */
	public VertxMosipEventBus(Vertx vertx, EventTracingHandler eventTracingHandler) {
		this.vertx = vertx;
		this.eventTracingHandler = eventTracingHandler;
		this.eventTracingHandler.writeHeaderOnProduce(vertx.eventBus());
		this.eventTracingHandler.readHeaderOnConsume(vertx.eventBus());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * getEventbus()
	 */
	@Override
	public Vertx getEventbus() {
		return this.vertx;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * consume()
	 */
	@Override
	public void consume(MessageBusAddress fromAddress,
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
		vertx.eventBus().consumer(fromAddress.getAddress(), msg -> {
			EventDTO eventDTO = new EventDTO();
			eventDTO.setBody(new JsonObject((String)msg.body()));
			eventHandler.handle(eventDTO, res -> {
				if (!res.succeeded() && res.cause() instanceof MessageExpiredException) {
					logger.warn("Event handling failed {}", res.cause().getMessage());
				} else if (!res.succeeded()) {
					logger.error("Event handling failed {}",res.cause());
				}
				MDCHelper.clearMDC();
			});
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * consumeAndSend()
	 */
	@Override
	public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress, 
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler) {
		vertx.eventBus().consumer(fromAddress.getAddress(), msg -> {
			EventDTO eventDTO = new EventDTO();
			eventDTO.setBody(new JsonObject((String) msg.body()));
			eventHandler.handle(eventDTO, res -> {
				if (!res.succeeded() && res.cause() instanceof MessageExpiredException) {
					logger.warn("Event handling failed {}", res.cause().getMessage());
				} else if (!res.succeeded()) {
					logger.error("Event handling failed {}", res.cause());
				} else {
					MessageDTO messageDTO = res.result();
					MessageBusAddress messageBusToAddress = new MessageBusAddress(toAddress, messageDTO.getReg_type());
					JsonObject jsonObject = JsonObject.mapFrom(messageDTO);
					vertx.eventBus().send(messageBusToAddress.getAddress(), jsonObject.toString());
				}
				MDCHelper.clearMDC();
			});
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.core.spi.eventbus.MosipEventbusFactory#
	 * send()
	 */
	@Override
	public void send(MessageBusAddress toAddress, MessageDTO message) {
		MessageBusAddress messageBusAddress = new MessageBusAddress(toAddress, message.getReg_type());
		JsonObject jsonObject = JsonObject.mapFrom(message);
		logger.debug("send called with toAddress {} for message {}",messageBusAddress.getAddress(), jsonObject.toString());
		this.vertx.eventBus().send(messageBusAddress.getAddress(), jsonObject.toString());
	}

	@Override
	public void consumerHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
		Boolean isRegistered = vertx.eventBus().consumer(address).isRegistered();
		HealthCheckDTO healthCheckDTO = new HealthCheckDTO();
		healthCheckDTO.setEventBusConnected(isRegistered);
		eventHandler.handle(healthCheckDTO);

	}

	@Override
	public void senderHealthCheck(Handler<HealthCheckDTO> eventHandler, String address) {
		HealthCheckDTO healthCheckDTO = new HealthCheckDTO();
		try {
			vertx.eventBus().send(address, HealthConstant.PING);
			healthCheckDTO.setEventBusConnected(true);
			eventHandler.handle(healthCheckDTO);
		} catch (Exception e) {
			healthCheckDTO.setEventBusConnected(false);
			healthCheckDTO.setFailureReason(e.getMessage());
			eventHandler.handle(healthCheckDTO);
		}
	}
}
