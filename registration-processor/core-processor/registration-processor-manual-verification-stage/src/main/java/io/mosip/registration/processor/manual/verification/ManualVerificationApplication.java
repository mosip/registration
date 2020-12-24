package io.mosip.registration.processor.manual.verification;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.manual.verification.stage.ManualVerificationStage;
/**
 * ManualAdjudicationApplication Main class .
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class ManualVerificationApplication {
	private static Logger regProcLogger = RegProcessorLogger.getLogger(ManualVerificationApplication.class);
	/**
	 * Main method to instantiate the spring boot application.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		
		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
		configApplicationContext.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.manual.verification.config",
				"io.mosip.registration.processor.packet.receiver.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.kernel.packetmanager.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.packet.storage.config");
		configApplicationContext.refresh();
		try {
		configApplicationContext.getBean(Listener.class).runQueue();
		}catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),LoggerFileConstant.APPLICATIONID.toString(),e.getMessage(),e.getMessage());
			e.printStackTrace();
		}
		ManualVerificationStage manualVerificationStage = configApplicationContext
				.getBean(ManualVerificationStage.class);
		manualVerificationStage.deployStage();

	}
}