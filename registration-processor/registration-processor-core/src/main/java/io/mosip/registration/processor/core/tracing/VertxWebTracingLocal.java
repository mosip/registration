package io.mosip.registration.processor.core.tracing;

import brave.Tracing;
import brave.http.HttpTracing;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public final class VertxWebTracingLocal {
    final HttpTracing httpTracing;

    public static VertxWebTracingLocal create(Tracing tracing) {
        return new VertxWebTracingLocal(HttpTracing.create(tracing));
    }

    public static VertxWebTracingLocal create(HttpTracing httpTracing) {
        return new VertxWebTracingLocal(httpTracing);
    }

    VertxWebTracingLocal(HttpTracing httpTracing) {
        if (httpTracing == null)
            throw new NullPointerException("httpTracing == null");
        this.httpTracing = httpTracing;
    }

    public Handler<RoutingContext> routingContextHandler() {
        return new TracingRoutingContextHandlerLocal(this.httpTracing);
    }
}
