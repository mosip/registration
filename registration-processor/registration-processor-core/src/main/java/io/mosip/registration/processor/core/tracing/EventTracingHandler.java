package io.mosip.registration.processor.core.tracing;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaHeader;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handler to trace events,
 * currently methods are written to handle vertx eventbus and kafka
 * Note: For events, We use single line b3 header
 *      <traceid>-<spanid>-<samplingflag>
 */
public class EventTracingHandler {

    private final Logger logger = LoggerFactory.getLogger(EventTracingHandler.class);

    private final Tracer tracer;
    private TraceContext.Extractor<Message> extractor;
    private TraceContext.Extractor<List<KafkaHeader>> kafkaExtractor;

    public EventTracingHandler(Tracing tracing, String eventBusType) {
        this.tracer = tracing.tracer();
        switch (eventBusType) {
            case "vertx":
                this.extractor = tracing.propagation().extractor(GETTER);
                break;
            case "kafka":
                this.kafkaExtractor = tracing.propagation().extractor(KAFKA_GETTER);
                break;
        }
    }


    /**
     * creates TraceContext based on the provided eventbus Message,
     * creates new traceContext if b3 headers are not found else starts the span with existing span
     * @param carrier
     * @return
     */
    private Span nextSpan(Message carrier) {
        TraceContextOrSamplingFlags extracted = extractor.extract(carrier);
        Span span = extracted.context() != null
                ? tracer.joinSpan(extracted.context())
                : tracer.nextSpan(extracted);
        span.start(System.currentTimeMillis());
        return span;
    }

    /**
     * creates TraceContext based on the provided Kafka Header,
     * creates new traceContext if b3 headers are not found else starts the span with existing span
     * @param carrier
     * @return
     */
    private Span nextSpan(List<KafkaHeader> carrier) {
        TraceContextOrSamplingFlags extracted = kafkaExtractor.extract(carrier);
        Span span = extracted.context() != null
                ? tracer.joinSpan(extracted.context())
                : tracer.nextSpan(extracted);
        span.start(System.currentTimeMillis());
        return span;
    }


    /**
     * b3 header Extractor from eventbus message header
     */
    static final Propagation.Getter<Message, String> GETTER = new Propagation.Getter<Message, String>() {
        public String get(Message carrier, String key) {
            return carrier.headers().get(key);
        }

        public String toString() {
            return "EventBus.Message::getHeader";
        }
    };

    /**
     * b3 header extractor from kafka header
     */
    static final Propagation.Getter<List<KafkaHeader>, String> KAFKA_GETTER = new Propagation.Getter<List<KafkaHeader>, String>() {
        public String get(List<KafkaHeader> headers, String key) {
            String value = null;
            for (KafkaHeader header : headers) {
                if(key.equalsIgnoreCase(header.key()))
                    value= header.value().toString(StandardCharsets.UTF_8);
            }
            return value;
        }

        public String toString() {
            return "Kafka::getHeader";
        }
    };

    public void readHeaderOnConsume(EventBus eventBus) {
        eventBus.addInboundInterceptor(deliveryContext -> {
            Span span = nextSpan(deliveryContext.message());
            JsonObject body = new JsonObject((String) deliveryContext.message().body());
            initializeContextWithTracing(span, body == null ? "-" : (body.getString("rid", "-")));
            MDCHelper.addHeadersToMDC();
            deliveryContext.next();
        });
    }

    public void writeHeaderOnProduce(EventBus eventBus) {
        eventBus.addOutboundInterceptor(deliveryContext -> {
            Object tracer = ContextualData.getOrDefault(TracingConstant.TRACER);
            Span span = (tracer instanceof TracingHandler) ? ((TracingHandler)tracer).span : (Span)tracer;
            if(span == null) {
                span = nextSpan(deliveryContext.message());
                JsonObject body = new JsonObject((String) deliveryContext.message().body());
                initializeContextWithTracing(span, body == null ? "-" : (body.getString("rid", "-")));
                MDCHelper.addHeadersToMDC();
            }

            deliveryContext.message().headers().add(TracingConstant.SINGLE_LINE_B3_HEADER,
                    String.format("%s-%s", span.context().traceIdString(), span.context().spanIdString()));
            deliveryContext.message().headers().add(TracingConstant.RID_KEY,
                    (String) ContextualData.getOrDefault(TracingConstant.RID_KEY, "-"));
            deliveryContext.next();
        });
    }

    public Span readHeaderOnKafkaConsume(KafkaConsumerRecord<String, String> consumerRecord) {
        Span span = nextSpan(consumerRecord.headers());
        initializeContextWithTracing(span, consumerRecord.key());
        MDCHelper.addHeadersToMDC();
        return span;
    }

    public void writeHeaderOnKafkaProduce(KafkaProducerRecord<String, String> producerRecord) {
        Object tracer = ContextualData.getOrDefault(TracingConstant.TRACER);
        Span span = (tracer instanceof TracingHandler) ? ((TracingHandler)tracer).span : (Span)tracer;
        if(span == null) {
            span = nextSpan(producerRecord.headers());
            initializeContextWithTracing(span, producerRecord.key());
            MDCHelper.addHeadersToMDC();
        }
        producerRecord.addHeader(TracingConstant.SINGLE_LINE_B3_HEADER,
                String.format("%s-%s", span.context().traceIdString(),
                span.context().spanIdString()));
        producerRecord.addHeader(TracingConstant.RID_KEY, producerRecord.key());
    }

    public void writeHeaderOnKafkaProduce(KafkaProducerRecord<String, String> producerRecord, Span span) {
       producerRecord.addHeader(TracingConstant.SINGLE_LINE_B3_HEADER,
                String.format("%s-%s", span.context().traceIdString(),
                        span.context().spanIdString()));
       producerRecord.addHeader(TracingConstant.RID_KEY, producerRecord.key());
    }

    public void closeSpan() {
        Object tracer = ContextualData.getOrDefault(TracingConstant.TRACER);
        if(tracer instanceof Span) {
            ((Span)tracer).finish(System.currentTimeMillis());
        }
    }

    public void closeSpan(Span span) {
        span.finish(System.currentTimeMillis());
        MDCHelper.clearMDC();
    }

    private void initializeContextWithTracing(Span span, String rid) {
        ContextualData.put(TracingConstant.TRACER, span);
        ContextualData.put(TracingConstant.TRACE_ID_KEY, span.context().traceIdString());
        ContextualData.put(TracingConstant.RID_KEY, rid);
    }

}
