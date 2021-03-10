package io.mosip.registration.processor.stages.executor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import com.google.common.io.PatternFilenameFilter;

/**
 * The Class FolderConfigLoadingApplicationContext.
 */
class FolderConfigLoadingApplicationContext extends AnnotationConfigApplicationContext {
	
	/** The Constant EMPTY_STRINGS. */
	private static final String[] EMPTY_STRINGS = new String[0];
	
	/** The Constant UNABLE_TO_LOAD_CONFIG. */
	private static final String UNABLE_TO_LOAD_CONFIG = "Unable to load config: ";
	
	/** The Constant PROP_CONFIG_FOLDER. */
	private static final String PROP_CONFIG_FOLDER = "config.folder";
	
	/** The Constant PROPERTIES_FILE_EXTN. */
	private static final String PROPERTIES_FILE_EXTN = ".properties";
	
	/** The Constant IDENTITY_FILENAME_FILTER. */
	private static final FilenameFilter IDENTITY_FILENAME_FILTER = (dir, name) -> true;

	/**
	 * Instantiates a new folder config loading application context.
	 *
	 * @param annotatedClasses the annotated classes
	 */
	FolderConfigLoadingApplicationContext(Class<?>... annotatedClasses) {
		super(annotatedClasses);
	}

	/**
	 * Creates the environment.
	 *
	 * @return the configurable environment
	 */
	protected ConfigurableEnvironment createEnvironment() {
		ConfigurableEnvironment environment = super.createEnvironment();
		String configFolder = environment.getProperty(PROP_CONFIG_FOLDER);
		environment.merge(createCustomizedEnvironment(configFolder));
		return environment;
	}
	
	/**
	 * Creates the customized environment.
	 *
	 * @param configFolderPath the config folder path
	 * @return the property source customizable environment
	 */
	public PropertySourceCustomizableEnvironment createCustomizedEnvironment(String configFolderPath) {
		PropertySourceCustomizableEnvironment newEnv = new PropertySourceCustomizableEnvironment();
		MutablePropertySources propSources = new MutablePropertySources();
		
		Stream.of(new File(configFolderPath).listFiles(getFilenameFilter()))
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
	
	/**
	 * Gets the exclude filter.
	 *
	 * @return the exclude filter
	 */
	private Optional<FilenameFilter> getExcludeFilter() {
		return patternsToFilter(getExcludeFilePatterns()).map(this::negateFilter);
	}

	/**
	 * Creates the filter for pattern.
	 *
	 * @param filterPattern the filter pattern
	 * @return the optional
	 */
	private Optional<FilenameFilter> createFilterForPattern(String filterPattern) {
		return Optional.ofNullable(filterPattern).map(PatternFilenameFilter::new);
	}
	
	/**
	 * Gets the exclude file patterns.
	 *
	 * @return the exclude file patterns
	 */
	protected String[] getExcludeFilePatterns() {
		return EMPTY_STRINGS;
	}
	
	/**
	 * Gets the include filter.
	 *
	 * @return the include filter
	 */
	private Optional<FilenameFilter> getIncludeFilter() {
		return patternsToFilter(getIncludeFilePatterns());
	}

	/**
	 * Patterns to filter.
	 *
	 * @param filenamePatterns the filename patterns
	 * @return the optional
	 */
	private Optional<FilenameFilter> patternsToFilter(String[] filenamePatterns) {
		return filterStreamToAnd(Arrays.stream(filenamePatterns)
						.map(this::createFilterForPattern)
						.flatMap(Optional::stream));
	}
	
	/**
	 * Gets the include file patterns.
	 *
	 * @return the include file patterns
	 */
	protected String[] getIncludeFilePatterns() {
		return EMPTY_STRINGS;
	}
	
	/**
	 * Gets the filename filter.
	 *
	 * @return the filename filter
	 */
	private FilenameFilter getFilenameFilter() {
		return filterStreamToAnd(Stream.concat(getIncludeFilter().stream(), getExcludeFilter().stream()))
				.orElse(IDENTITY_FILENAME_FILTER);
	}

	/**
	 * Filter stream to and.
	 *
	 * @param filterStream the filter stream
	 * @return the optional
	 */
	private Optional<FilenameFilter> filterStreamToAnd(Stream<FilenameFilter> filterStream) {
		return filterStream
					.reduce((filter1, filter2) -> 
									filterByAnd(filter1, filter2));
	}

	/**
	 * Filter by and.
	 *
	 * @param filter1 the filter 1
	 * @param filter2 the filter 2
	 * @return the filename filter
	 */
	private FilenameFilter filterByAnd(FilenameFilter filter1, FilenameFilter filter2) {
		return (dir, name) -> 
				filter1.accept(dir, name) && filter2.accept(dir, name);
	}

	/**
	 * Negate filter.
	 *
	 * @param filenameFilter the filename filter
	 * @return the filename filter
	 */
	private FilenameFilter negateFilter(FilenameFilter filenameFilter) {
		return (dir, name) -> !filenameFilter.accept(dir, name);
	}

}