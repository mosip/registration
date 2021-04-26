package io.mosip.registration.processor.stages.executor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

/**
 * The Class FolderConfigLoadingApplicationContext.
 */
public class PropertySourcesCustomizingApplicationContext extends AnnotationConfigApplicationContext {
	
	/**
	 * Instantiates a new folder config loading application context.
	 *
	 * @param annotatedClasses the annotated classes
	 */
	PropertySourcesCustomizingApplicationContext(Class<?>... annotatedClasses) {
		super(annotatedClasses);
	}

	/**
	 * Creates the environment.
	 *
	 * @return the configurable environment
	 */
	protected ConfigurableEnvironment createEnvironment() {
		ConfigurableEnvironment environment = super.createEnvironment();
		environment.merge(createCustomizedEnvironment());
		return environment;
	}
	
	/**
	 * Creates the customized environment.
	 *
	 * @param configFolderPath the config folder path
	 * @return the property source customizable environment
	 */
	public PropertySourceCustomizableEnvironment createCustomizedEnvironment() {
		PropertySourceCustomizableEnvironment newEnv = new PropertySourceCustomizableEnvironment();
		MutablePropertySources propertySources = getPropertySources();
		if(propertySources != null) {
			newEnv.customizePropertySources(propertySources);
		}
		return newEnv;
	}
	
	public MutablePropertySources getPropertySources() {
		return null;
	}
	

}