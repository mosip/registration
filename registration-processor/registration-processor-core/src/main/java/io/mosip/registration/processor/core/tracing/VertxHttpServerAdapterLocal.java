package io.mosip.registration.processor.core.tracing;

import brave.Span;
import brave.http.HttpServerAdapter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.SocketAddress;

/**
 * This is the copy from brave instrumentation
 */
class VertxHttpServerAdapterLocal extends HttpServerAdapter<HttpServerRequest, HttpServerResponse> {
    public String method(HttpServerRequest request) {
        return request.rawMethod();
    }

    public String path(HttpServerRequest request) {
        return request.path();
    }

    public String url(HttpServerRequest request) {
        return request.absoluteURI();
    }

    public String requestHeader(HttpServerRequest request, String name) {
        return request.headers().get(name);
    }

    public String methodFromResponse(HttpServerResponse ignored) {
        String[] methodAndPath = METHOD_AND_PATH.get();
        return (methodAndPath != null) ? methodAndPath[0] : null;
    }

    public String route(HttpServerResponse ignored) {
        String[] methodAndPath = METHOD_AND_PATH.get();
        String result = (methodAndPath != null) ? methodAndPath[1] : null;
        return (result != null) ? result : "";
    }

    public Integer statusCode(HttpServerResponse response) {
        return Integer.valueOf(statusCodeAsInt(response));
    }

    public int statusCodeAsInt(HttpServerResponse response) {
        return response.getStatusCode();
    }

    public boolean parseClientIpAndPort(HttpServerRequest req, Span span) {
        if (parseClientIpFromXForwardedFor(req, span))
            return true;
        SocketAddress addr = req.remoteAddress();
        return span.remoteIpAndPort(addr.host(), addr.port());
    }

    static final ThreadLocal<String[]> METHOD_AND_PATH = (ThreadLocal)new ThreadLocal<>();

    static void setCurrentMethodAndPath(String method, String path) {
        String[] methodAndPath = METHOD_AND_PATH.get();
        if (methodAndPath == null) {
            methodAndPath = new String[2];
            METHOD_AND_PATH.set(methodAndPath);
        }
        methodAndPath[0] = method;
        methodAndPath[1] = path;
    }
}