package io.mosip.registration.processor.core.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import org.slf4j.LoggerFactory;
import io.mosip.kernel.logger.logback.factory.Logfactory;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;


/**
 * The Class RegProcessorLogger.
 * @author : Rishabh Keshari
 */
public final class RegProcessorLogger {
	
	public static final String PROP_PREFIX = "logging.level";
	
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
		Map<String,String> map = new HashMap<String, String>();
		Logger logger=  Logfactory.getSlf4jLogger(clazz);
		if(map.isEmpty()) {
			Properties properties=System.getProperties();
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			properties.forEach((key, value) -> {
			      if (key.toString().startsWith(PROP_PREFIX)) {
			        map.put(key.toString().substring(PROP_PREFIX.length() + 1), value.toString());
			      }
			    });
				map.forEach((key, value) -> {
					loggerContext.getLogger(key).setLevel(Level.valueOf(value));
				    });
			}
		return logger;
	}
}
