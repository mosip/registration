package io.mosip.registration.processor.camel.bridge;

import io.mosip.registration.processor.core.tracing.TracingConstant;
import org.apache.camel.Exchange;
import org.apache.camel.impl.MDCUnitOfWork;
import org.slf4j.MDC;

/**
 * In case of kafka eventbus, we need to explicitly add message headers to MDC
 */
public class CustomMDCUnitOfWork extends MDCUnitOfWork {

    public CustomMDCUnitOfWork(Exchange exchange) {
        super(exchange);
        MDC.put(TracingConstant.RID_KEY, new String((byte[]) exchange.getIn().getHeaders().getOrDefault(TracingConstant.RID_KEY, "-")));
        String singleLineB3Header = new String((byte[]) exchange.getIn().getHeaders().getOrDefault(TracingConstant.SINGLE_LINE_B3_HEADER,"-"));
        MDC.put(TracingConstant.TRACE_ID_KEY, singleLineB3Header.split("-")[0]);
    }
}
