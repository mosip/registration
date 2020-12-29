package io.mosip.registration.processor.core.eventbus;

import io.mosip.registration.processor.core.abstractverticle.EventDTO;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of MosipEventBus interface for Vertx navtive event bus
 *
 * @author Vishwanath V
 */
public class VertxMosipEventBus implements MosipEventBus {

	/** The logger. */
	private Logger logger = LoggerFactory.getLogger(VertxMosipEventBus.class);

	/** The vertx instance that will be used by this event bus */
	private Vertx vertx = null;

	/**
	 * Instantiates a new vertx mosip event bus.
	 *
	 * @param vertx
	 *            The vertx instance
	 */
	public VertxMosipEventBus(Vertx vertx) {
		this.vertx = vertx;
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
			eventDTO.setBody((JsonObject) msg.body());
			eventHandler.handle(eventDTO, res -> {
				if (!res.succeeded()) {
					logger.error("Event handling failed " + res.cause());
				}
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
			eventDTO.setBody((JsonObject) msg.body());
			eventHandler.handle(eventDTO, res -> {
				if (!res.succeeded()) {
					logger.error("Event handling failed " + res.cause());
				} else {
					MessageDTO messageDTO = res.result();
					MessageBusAddress messageBusToAddress = new MessageBusAddress(toAddress, messageDTO.getReg_type());
					JsonObject jsonObject = JsonObject.mapFrom(messageDTO);
					vertx.eventBus().send(messageBusToAddress.getAddress(), jsonObject);
				}
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
		logger.debug("send called with toAddress " + toAddress.getAddress() + " for message " + jsonObject.toString());
		this.vertx.eventBus().send(messageBusAddress.getAddress(), jsonObject);
	}

}
