package io.mosip.registartion.processor.abis.middleware.validators;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.List;

import io.mosip.registration.processor.abis.queue.dto.AbisQueueDetails;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

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


    @Value("${registration.processor.bio.dedupe.reprocess.buffer.time}")
    private long reprocessBufferTime;

    @Autowired
    private Utilities utility;

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
        try {
            List<AbisQueueDetails> abisQueueDetails = utility.getAbisQueueDetails();
            long allowedReprocessTime = reprocessBufferTime;
            for(AbisQueueDetails abisQueueDetail : abisQueueDetails) {
                //TTL is multiplied by 2 to compute both Insert and Identify request expiry time
                allowedReprocessTime += (abisQueueDetail.getInboundMessageTTL() * 2);
            }
            if(reprocessorElapseTime <= allowedReprocessTime) {
                logger.warn("registration.processor.reprocess.elapse.time config {} is invalid," +
                    " it should should be greater than all the queue expiry put together with an" +
                    " additional buffer {}", reprocessorElapseTime, allowedReprocessTime);
            }
        } catch (RegistrationProcessorCheckedException e) {
            logger.error("Abis queue details invalid", e);
        }
    }
}
