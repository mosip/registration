package io.mosip.registration.processor.stages.executor;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * The Class PropertySourceCustomizableEnvironment.
 */
final class PropertySourceCustomizableEnvironment extends StandardEnvironment{
	
	/** The property sources. */
	private MutablePropertySources propertySources = new MutablePropertySources();
	
	/**
	 * Customize property sources.
	 *
	 * @param propertySources the property sources
	 */
	@Override
	public void customizePropertySources(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
	}
	
	/**
	 * Gets the property sources.
	 *
	 * @return the property sources
	 */
	public MutablePropertySources getPropertySources() {
		return propertySources;
	}

}
