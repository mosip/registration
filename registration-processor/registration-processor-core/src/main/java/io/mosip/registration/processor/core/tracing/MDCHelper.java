package io.mosip.registration.processor.core.tracing;

import brave.Span;
import org.slf4j.MDC;

/**
 * Helper class to add tracing details into MDC (static class to add data threadLocal context)
 * Note: In vertx 4 this will not be required
 */
public class MDCHelper {

    public static void addHeadersToMDC() {
       Object tracer = ContextualData.getOrDefault(TracingConstant.TRACER);

       if(tracer instanceof TracingHandler) {
           TracingHandler tracingHandler = (TracingHandler) tracer;
           MDC.put(TracingConstant.TRACE_ID_KEY, tracingHandler.span.context().traceIdString());
           MDC.put(TracingConstant.SPAN_ID_KEY, tracingHandler.span.context().spanIdString());
       }

       if(tracer instanceof Span) {
           Span span = (Span) tracer;
           MDC.put(TracingConstant.TRACE_ID_KEY, span.context().traceIdString());
           MDC.put(TracingConstant.SPAN_ID_KEY, span.context().spanIdString());
       }

       MDC.put(TracingConstant.RID_KEY, (String) ContextualData.getOrDefault(TracingConstant.RID_KEY));
    }

    /**
     * call this to avoid memory leak, at end of the handler
     */
    public static void clearMDC() {
        MDC.clear();
    }
}
