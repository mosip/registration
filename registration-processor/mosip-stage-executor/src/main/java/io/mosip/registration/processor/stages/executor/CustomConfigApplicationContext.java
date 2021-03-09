package io.mosip.registration.processor.stages.executor;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

public final class CustomConfigApplicationContext extends AnnotationConfigApplicationContext {
	
	private static final String UNABLE_TO_LOAD_CONFIG = "Unable to load config: ";
	private static final String PROP_CONFIG_FOLDER = "config.folder";
	private static final String PROPERTIES_FILE_EXTN = ".properties";

	CustomConfigApplicationContext(Class<?>... annotatedClasses) {
		super(annotatedClasses);
	}

	protected ConfigurableEnvironment createEnvironment() {
		ConfigurableEnvironment environment = super.createEnvironment();
		String configFolder = environment.getProperty(PROP_CONFIG_FOLDER);
		environment.merge(createCustomEnvironment(configFolder));
		return environment;
	}
	
	public static CustomEnvironment createCustomEnvironment(String configFolderPath) {
		CustomEnvironment newEnv = new CustomEnvironment();
		MutablePropertySources propSources = new MutablePropertySources();
		
		Stream.of(new File(configFolderPath).listFiles())
				.filter(File::isFile)
				.filter(file -> file.getName().toLowerCase().endsWith(PROPERTIES_FILE_EXTN))
				.map(file -> new FileSystemResource(new File(configFolderPath + File.separator + file.getName())))
				.forEach(res -> {
					try {
						propSources.addFirst(new ResourcePropertySource(res));
					} catch (IOException e) {
						throw new RuntimeException(UNABLE_TO_LOAD_CONFIG + res.getPath());
					}
				});
		
		newEnv.customizePropertySources(propSources);
		return newEnv;
	}
}