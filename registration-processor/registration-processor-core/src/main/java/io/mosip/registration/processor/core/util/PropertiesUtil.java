package io.mosip.registration.processor.core.util;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;

/**
 * The Class PropertiesUtil - used to get properties from environment and throw
 * {@link BaseUncheckedException} in case of property is null.
 *
 * @author Loganathan Sekar
 */
public class PropertiesUtil {
	
	/** The logger. */
	private Logger logger = RegProcessorLogger.getLogger(PropertiesUtil.class);

	/** The environment. */
	@Autowired
	private Environment environment;
	
	/**
	 * Gets the string property.
	 *
	 * @param propPrefix the prop prefix
	 * @param propSuffix the prop suffix
	 * @return the string property
	 */
	public String getProperty(String propPrefix, String propSuffix) {
		return getProperty(propPrefix + propSuffix);	
	}

	/**
	 * Gets the property.
	 *
	 * @param propKey the prop key
	 * @return the property
	 */
	public String getProperty(String propKey) {
		return getProperty(propKey, String.class);
	}

	/**
	 * Gets the integer property.
	 *
	 * @param propPrefix the prop prefix
	 * @param propSuffix the prop suffix
	 * @return the integer property
	 */
	public Integer getIntegerProperty(String propPrefix, String propSuffix) {
		return getIntegerProperty(propPrefix + propSuffix);
	}
	
	/**
	 * Gets the integer property.
	 *
	 * @param propKey the prop key
	 * @return the integer property
	 */
	public Integer getIntegerProperty(String propKey) {
		return getProperty(propKey, Integer.class);
	}
	
	/**
	 * Gets the property.
	 *
	 * @param <T> the generic type
	 * @param propKey the prop key
	 * @param clazz the clazz
	 * @return the property
	 */
	public <T> T getProperty(String propKey, Class<T> clazz) {
		T propValue = environment.getProperty(propKey, clazz);
		return handlPropValue(propKey, propValue);
	}

	/**
	 * Gets the property.
	 *
	 * @param <T> the generic type
	 * @param propKey the prop key
	 * @param clazz the clazz
	 * @param defaultVal the default val
	 * @return the property
	 */
	public <T> T getProperty(String propKey, Class<T> clazz, T defaultVal) {
		T propValue = environment.getProperty(propKey, clazz, defaultVal);
		return handlPropValue(propKey, propValue);
	}
	
	/**
	 * Handl prop value.
	 *
	 * @param <T> the generic type
	 * @param propKey the prop key
	 * @param propValue the prop value
	 * @return the t
	 */
	private <T> T handlPropValue(String propKey, T propValue) {
		if(Objects.isNull(propValue)) {
			logger.error("{} - property is missing.", propKey);
			throw new BaseUncheckedException(String.format("%s - property is missing.", propKey));
		} else {
			return propValue;
		}
	}


}
