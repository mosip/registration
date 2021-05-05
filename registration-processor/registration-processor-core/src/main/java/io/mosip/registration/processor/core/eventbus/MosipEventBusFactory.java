package io.mosip.registration.processor.core.eventbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import brave.Tracing;
import io.mosip.registration.processor.core.abstractverticle.MosipEventBus;
import io.mosip.registration.processor.core.exception.UnsupportedEventBusTypeException;
import io.mosip.registration.processor.core.tracing.EventTracingHandler;
import io.mosip.registration.processor.core.util.PropertiesUtil;
import io.vertx.core.Vertx;

/**
 * Factory class responsible to instantiate and provide event bus objects of different types
 * 
 * @author Vishwanath V
 */
@Component
public class MosipEventBusFactory {

    private static final String EVENTBUS_KAFKA_POLL_FREQUENCY = "eventbus.kafka.poll.frequency";

	private static final String EVENTBUS_KAFKA_MAX_POLL_RECORDS = "eventbus.kafka.max.poll.records";

	private static final String EVENTBUS_KAFKA_GROUP_ID = "eventbus.kafka.group.id";

	private static final String EVENTBUS_KAFKA_COMMIT_TYPE = "eventbus.kafka.commit.type";

	private static final String MOSIP_REGPROC_EVENTBUS_KAFKA_BOOTSTRAP_SERVERS = "mosip.regproc.eventbus.kafka.bootstrap.servers";

	@Autowired
    private Tracing tracing;
    
    @Autowired
    private PropertiesUtil propertiesUtil;

    /**
     * Instantiate and return event bus of a particular type
     * @param vertx The vertx instance to which this event bus object should be attached
     * @param eventBusType String representation of event bus types, currently supports vertx and amqp
     * @param string 
     * @return Any one implementation of MosipEventBus interface
     * @throws UnsupportedEventBusTypeException - Will be thrown when the eventBusType is not recognized
     */
    public MosipEventBus getEventBus(Vertx vertx, String eventBusType, String propertyPrefix) throws UnsupportedEventBusTypeException {
        EventTracingHandler eventTracingHandler = new EventTracingHandler(tracing, eventBusType);
        switch (eventBusType) {
            case "vertx":
                return new VertxMosipEventBus(vertx, eventTracingHandler);
            case "kafka":
                return new KafkaMosipEventBus(vertx, 
                		getKafkaBootstrapServers(), 
                		getKafkaGroupId(propertyPrefix), 
                		getKafkaCommitType(propertyPrefix), 
                		getMaxPollRecords(propertyPrefix), 
                		getPollFrequency(propertyPrefix), 
                		eventTracingHandler);
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
    
    public String getKafkaBootstrapServers() {
		return propertiesUtil.getProperty(MOSIP_REGPROC_EVENTBUS_KAFKA_BOOTSTRAP_SERVERS);
	}
    
    public String getKafkaCommitType(String propertyPrefix) {
		return propertiesUtil.getProperty(propertyPrefix + EVENTBUS_KAFKA_COMMIT_TYPE);
	}
    
    public String getKafkaGroupId(String propertyPrefix) {
		return propertiesUtil.getProperty(propertyPrefix + EVENTBUS_KAFKA_GROUP_ID);
	}
    
    public String getMaxPollRecords(String propertyPrefix) {
		return propertiesUtil.getProperty(propertyPrefix + EVENTBUS_KAFKA_MAX_POLL_RECORDS);
	}
    
	public int getPollFrequency(String propertyPrefix) {
		return propertiesUtil.getProperty(propertyPrefix + EVENTBUS_KAFKA_POLL_FREQUENCY, Integer.class, 0);
	}
}
