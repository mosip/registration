package io.mosip.registration.processor.verification;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.verification.stage.VerificationStage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * ManualAdjudicationApplication Main class .
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class VerificationApplication {
    private static Logger regProcLogger = RegProcessorLogger.getLogger(VerificationApplication.class);

    /**
     * Main method to instantiate the spring boot application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
        configApplicationContext.scan("io.mosip.registration.processor.core.config",
                "io.mosip.registration.processor.verification.config",
                "io.mosip.registration.processor.packet.receiver.config",
                "io.mosip.registration.processor.packet.manager.config",
                "io.mosip.kernel.packetmanager.config",
                "io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
                "io.mosip.registration.processor.core.kernel.beans",
                "io.mosip.registration.processor.packet.storage.config",
                "io.mosip.registration.processor.verification.validators");
        configApplicationContext.refresh();
        VerificationStage verificationStage = configApplicationContext
                .getBean(VerificationStage.class);
        verificationStage.deployStage();

    }
}