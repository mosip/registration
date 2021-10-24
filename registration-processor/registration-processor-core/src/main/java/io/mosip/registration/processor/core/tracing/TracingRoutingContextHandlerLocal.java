package io.mosip.registration.processor.core.tracing;

import brave.Span;
import brave.Tracer;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * This is the copy from brave instrumentation
 * In this local implementation, we have added Tracer and TraceId into ContextInternal
 * So that its accessible in all required places
 */
final class TracingRoutingContextHandlerLocal implements Handler<RoutingContext> {

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
        TracingHandler tracingHandler = (TracingHandler)context.get(TracingHandler.class.getName());
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
        context.addHeadersEndHandler(handler);
        Tracer.SpanInScope ws = this.tracer.withSpanInScope(span);
        try {
            context.next();
            if (ws != null)
                ws.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            if (ws != null)
                try {
                    ws.close();
                } catch (Throwable throwable1) {
                    throwable1.printStackTrace();
                    throwable.addSuppressed(throwable1);
                }
            throw throwable;
        }
    }
}
