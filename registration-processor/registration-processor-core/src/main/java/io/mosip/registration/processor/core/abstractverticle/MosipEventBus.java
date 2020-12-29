package io.mosip.registration.processor.core.abstractverticle;

import io.mosip.registration.processor.core.spi.eventbus.EventHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Interface that declares public methods that needs to be expose by every event bus implementation
 * Later this interface to be moved to spi package, not done now to avoid changing all the projects depending on this library
 *
 * @author Pranav Kumar
 * @param <T>
 * @since 0.0.1
 */
public interface MosipEventBus {

	/**
	 * Return the vertx instantance associated with event bus
	 * This is depricated because method is called getEventbus but returns a vertx instance
	 * @return Assoicated vertx instance
	 */
	@Deprecated
	public Vertx getEventbus();

	
	/**
	 * Start consuming events from an address and use the handler to process the events
	 * 
	 * @param fromAddress - Address from which event should be consumed
	 * @param eventHandler - Handler that should be called for processing each event
	 */
	public void consume(MessageBusAddress fromAddress, EventHandler<EventDTO, 
			Handler<AsyncResult<MessageDTO>>> eventHandler);

	/**
	 * Start consuming events from an address, use the handler to process the events and s
	 * end an event back to another address on successful completion
	 * 
	 * @param fromAddress - Address from which event should be consumed
	 * @param toAddress - Address to which reply message should be sent after process the event
	 * @param eventHandler - Handler that should be called for processing each event
	 */
	public void consumeAndSend(MessageBusAddress fromAddress, MessageBusAddress toAddress, 
			EventHandler<EventDTO, Handler<AsyncResult<MessageDTO>>> eventHandler);

	/**
	 * Send the message to an address
	 * 
	 * @param toAddress - Address from to which message should be sent
	 * @param message - actual message that needs to be sent
	 */
	public void send(MessageBusAddress toAddress, MessageDTO message);


}
