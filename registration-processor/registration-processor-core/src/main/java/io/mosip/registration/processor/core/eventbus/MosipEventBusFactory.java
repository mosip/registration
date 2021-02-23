package io.mosip.registration.processor.core.eventbus;

import brave.Tracing;
import io.mosip.registration.processor.core.tracing.EventTracingHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    //The below properties of the kafka should never be directly configured from the config server, 
    //since this factory is defined in a library. The configurations should always be piped through
    //bootstrap.properties file of each app that uses this library
    @Value("${mosip.regproc.eventbus.kafka.bootstrap.servers:}")
    private String kafkaBootstrapServers;
    
    @Value("${mosip.regproc.eventbus.kafka.group.id:}")
    private String kafkaGroupId;
    
    @Value("${mosip.regproc.eventbus.kafka.commit.type:}")
    private String kafkaCommitType;

    @Value("${mosip.regproc.eventbus.kafka.max.poll.records:}")
    String maxPollRecords;
    
    @Value("${mosip.regproc.eventbus.kafka.poll.frequency:0}")
    int pollFrequency;

    @Autowired
    private Tracing tracing;

    /**
     * Instantiate and return event bus of a particular type
     * @param vertx The vertx instance to which this event bus object should be attached
     * @param eventBusType String representation of event bus types, currently supports vertx and amqp
     * @return Any one implementation of MosipEventBus interface
     * @throws UnsupportedEventBusTypeException - Will be thrown when the eventBusType is not recognized
     */
    public MosipEventBus getEventBus(Vertx vertx, String eventBusType) throws UnsupportedEventBusTypeException {
        EventTracingHandler eventTracingHandler = new EventTracingHandler(tracing, eventBusType);
        switch (eventBusType) {
            case "vertx":
                return new VertxMosipEventBus(vertx, eventTracingHandler);
            case "kafka":
                return new KafkaMosipEventBus(vertx, kafkaBootstrapServers, kafkaGroupId, 
                    kafkaCommitType, maxPollRecords, pollFrequency, eventTracingHandler);
            /*case "amqp":
                return new AmqpMosipEventBus(vertx);*/
            default:
                throw new UnsupportedEventBusTypeException();
        }
    }

    public Tracing getTracing() {
        return tracing;
    }

    public void setTracing(Tracing tracing) {
        this.tracing = tracing;
    }
}
