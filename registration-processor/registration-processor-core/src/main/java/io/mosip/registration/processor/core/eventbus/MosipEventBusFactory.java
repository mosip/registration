package io.mosip.registration.processor.core.eventbus;

import org.springframework.stereotype.Component;

import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.exception.UnsupportedEventBusTypeException;
import io.vertx.core.Vertx;

/**
 * Factory class responsible to instantiate and provide event bus objects of different types
 * 
 * @author Vishwanath V
 */
@Component
public class MosipEventBusFactory {

    /**
     * Instantiate and return event bus of a particular type
     * @param vertx The vertx instance to which this event bus object should be attached
     * @param eventBusType String representation of event bus types, currently supports vertx and amqp
     * @return Any one implementation of MosipEventBus interface
     * @throws UnsupportedEventBusTypeException - Will be thrown when the eventBusType is not recognized
     */
    public MosipEventBus getEventBus(Vertx vertx, String eventBusType) throws UnsupportedEventBusTypeException {
        switch (eventBusType) {
            case "vertx":
                return new VertxMosipEventBus(vertx);
            /*case "amqp":
                return new AmqpMosipEventBus(vertx);*/
            default:
                throw new UnsupportedEventBusTypeException();
        }
    }
}
