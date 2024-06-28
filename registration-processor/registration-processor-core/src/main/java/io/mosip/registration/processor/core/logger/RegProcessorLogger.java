package io.mosip.registration.processor.core.logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;


/**
 * The Class RegProcessorLogger.
 * @author : Rishabh Keshari
 */
public final class RegProcessorLogger {
	
	public static final String PROP_PREFIX = "logging.level.";

	public static Map<String,String> loggingLevelMap=new HashMap<String,String>();

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
	@SuppressWarnings({ "java:S4792" })
	public static Logger getLogger(Class<?> clazz) {
		if(loggingLevelMap.isEmpty()) {
			loggingLevelMap = System.getProperties().entrySet().stream()
					.filter(entry -> entry.getKey().toString().startsWith(PROP_PREFIX))
					.collect(Collectors.toMap(entry -> (String)entry.getKey(), entry -> (String)entry.getValue()));
		}
		Logger logger=  Logfactory.getSlf4jLogger(clazz);
		String loggerName = clazz.getName();
		if(loggingLevelMap.entrySet().stream().anyMatch( entry -> entry.getKey().equals(PROP_PREFIX+loggerName))) {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			ch.qos.logback.classic.Logger slf4jlogger=loggerContext.getLogger(loggerName);
			if(slf4jlogger!=null) {
			   slf4jlogger.setLevel(Level.valueOf(loggingLevelMap.get(PROP_PREFIX+loggerName)));
			}
		}
		return logger;
	}
}
