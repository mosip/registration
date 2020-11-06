package io.mosip.registration.processor.core.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.appender.RollingFileAppender;
import io.mosip.kernel.logger.logback.factory.Logfactory;


/**
 * The Class RegProcessorLogger.
 * @author : Rishabh Keshari
 */
public final class RegProcessorLogger {
	
	/**
	 * Instantiates a new reg processor logger.
	 */
	private RegProcessorLogger() {
	}

	/**
	 * Gets the logger.
	 *
	 * @param clazz the clazz
	 * @return the logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logfactory.getSlf4jLogger(clazz);
	}
}
