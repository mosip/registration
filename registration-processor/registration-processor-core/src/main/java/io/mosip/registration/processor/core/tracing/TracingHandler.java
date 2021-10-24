package io.mosip.registration.processor.core.tracing;

import brave.Span;
import brave.http.HttpServerHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the copy from brave instrumentation (originally inner class in TracingRoutingContextHandler).
 * But in this local implementation, its separated out to read tracing data stored in vertx context
 */
public class TracingHandler implements Handler<Void>  {

    final RoutingContext context;
    final Span span;
    final AtomicBoolean finished = new AtomicBoolean();
    final HttpServerHandler<HttpServerRequest, HttpServerResponse> serverHandler;

    public TracingHandler(RoutingContext context, Span span, HttpServerHandler<HttpServerRequest, HttpServerResponse> serverHandler) {
        this.context = context;
        this.span = span;
        this.serverHandler = serverHandler;
    }

    public void handle(Void aVoid) {
        if (!this.finished.compareAndSet(false, true))
            return;
        VertxHttpServerAdapterLocal.setCurrentMethodAndPath(this.context
                .request().rawMethod(), this.context
                .currentRoute().getPath());
        try {
            this.serverHandler.handleSend(this.context.response(), this.context.failure(), this.span);
        } finally {
            VertxHttpServerAdapterLocal.setCurrentMethodAndPath(null, null);
        }
    }
}
