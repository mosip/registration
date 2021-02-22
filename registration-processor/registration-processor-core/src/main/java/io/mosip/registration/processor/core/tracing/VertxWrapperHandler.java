package io.mosip.registration.processor.core.tracing;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * This is wrapper on handlers
 * takes care of loading and clearing of MDC with trace information
 */
public abstract class VertxWrapperHandler implements Handler<RoutingContext> {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(VertxWrapperHandler.class);

      private Handler<RoutingContext> wrappedHandler;

    public VertxWrapperHandler(Handler<RoutingContext> handler) {
        this.wrappedHandler = handler;
    }

    @Override
    public void handle(RoutingContext context) {
        JsonObject obj = context.getBodyAsJson();
        ContextualData.put(TracingConstant.RID_KEY, obj.getString("rid"));
        MDCHelper.addHeadersToMDC();
        regProcLogger.debug("VertxWrapperHandler::entry");
        try {
            wrappedHandler.handle(context);
        } finally {
            MDCHelper.clearMDC();
        }
    }

}