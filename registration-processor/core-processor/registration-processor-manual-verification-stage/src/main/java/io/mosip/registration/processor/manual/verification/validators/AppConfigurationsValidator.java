package io.mosip.registration.processor.manual.verification.validators;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * All the configuration validations will be done in this class
 */
@Component
public class AppConfigurationsValidator {

    private static final Logger logger = LoggerFactory.getLogger(AppConfigurationsValidator.class);

    /**
     * This configuration will be used by reprocessor stage to reprocess the events
     */
    @Value("${registration.processor.reprocess.elapse.time}")
    private long reprocessorElapseTime;

    @Value("${registration.processor.queue.manualverification.request.messageTTL}")
	private int mvRequestMessageTTL;

    @Value("${registration.processor.manual.verification.reprocess.buffer.time}")
    private long reprocessBufferTime;

    /**
     * The below method will be called by spring once the context is initialized or refreshed
     * @param event Context refreshed event
     */
    @EventListener
    public void validateConfigurations(ContextRefreshedEvent event) {
        logger.info("ContextRefreshedEvent received");
        validateReprocessElapseTimeConfig();
    }

    private void validateReprocessElapseTimeConfig() {
        long allowedReprocessTime = mvRequestMessageTTL + reprocessBufferTime;
        if(reprocessorElapseTime <= allowedReprocessTime) {
            logger.warn("registration.processor.reprocess.elapse.time config {} is invalid," +
                " it should should be greater than the queue expiry with an" +
                " additional buffer {}", reprocessorElapseTime, allowedReprocessTime);
        }
    }
}
