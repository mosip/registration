package io.mosip.registration.processor.core.tracing;

import brave.Span;
import brave.Tracer;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the copy from brave instrumentation
 * In this local implementation, we have added Tracer and TraceId into ContextInternal
 * So that its accessible in all required places
 */
final class TracingRoutingContextHandlerLocal implements Handler<RoutingContext> {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(TracingRoutingContextHandlerLocal.class);
    static final Propagation.Getter<HttpServerRequest, String> GETTER = new Propagation.Getter<HttpServerRequest, String>() {
        public String get(HttpServerRequest carrier, String key) {
            return carrier.getHeader(key);
        }

        public String toString() {
            return "HttpServerRequest::getHeader";
        }
    };

    final Tracer tracer;

    final HttpServerHandler<HttpServerRequest, HttpServerResponse> serverHandler;

    final TraceContext.Extractor<HttpServerRequest> extractor;

    TracingRoutingContextHandlerLocal(HttpTracing httpTracing) {
        this.tracer = httpTracing.tracing().tracer();
        this.serverHandler = HttpServerHandler.create(httpTracing, new VertxHttpServerAdapterLocal());
        this.extractor = httpTracing.tracing().propagation().extractor(GETTER);
    }

    public void handle(RoutingContext context) {
        TracingHandler tracingHandler = (TracingHandler) context.get(TracingHandler.class.getName());
        if (tracingHandler != null) {
            if (!context.failed())
                context.addHeadersEndHandler(tracingHandler);
            context.next();
            return;
        }
        Span span = this.serverHandler.handleReceive(this.extractor, context.request());
        TracingHandler handler = new TracingHandler(context, span, this.serverHandler);
        context.put(TracingHandler.class.getName(), handler);
        ContextualData.put(TracingConstant.TRACER, handler);
        ContextualData.put(TracingConstant.TRACE_ID_KEY, handler.span.context().traceIdString());
        logSpanStructured(span, context);
        context.addHeadersEndHandler(handler);
        Tracer.SpanInScope ws = this.tracer.withSpanInScope(span);
        try {
            context.next();
            if (ws != null)
                ws.close();
        } catch (Throwable throwable) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                    LoggerFileConstant.REGISTRATIONID.toString(), "",
                    throwable.getMessage() + ExceptionUtils.getStackTrace(throwable));
            if (ws != null)
                try {
                    ws.close();
                } catch (Throwable throwable1) {
                    regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                            LoggerFileConstant.REGISTRATIONID.toString(), "",
                            throwable1.getMessage() + ExceptionUtils.getStackTrace(throwable1));
                    throwable.addSuppressed(throwable1);
                }
            throw throwable;
        }
    }

    private void logSpanStructured(Span span, RoutingContext context) {
        Map<String, Object> spanMap = new HashMap<>();
        spanMap.put("traceId", span.context().traceIdString());
        spanMap.put("spanId", span.context().spanIdString());
        spanMap.put("parentId", span.context().parentIdString() != null ? span.context().parentIdString() : "-");
        spanMap.put("path", context.normalisedPath());
        spanMap.put("method", context.request().method().name());

        // Optional: include HTTP status if already available
        if (context.response().ended()) {
            spanMap.put("statusCode", context.response().getStatusCode());
        }

        regProcLogger.info("{}", spanMap); // LogstashEncoder will output structured JSON
    }
}
