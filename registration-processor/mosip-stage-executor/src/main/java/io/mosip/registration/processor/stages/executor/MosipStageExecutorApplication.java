package io.mosip.registration.processor.stages.executor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.stages.executor.config.CustomEnvironment;
import io.mosip.registration.processor.stages.executor.config.StagesConfig;

/**
 * External Stage application
 *
 */
public class MosipStageExecutorApplication {

	private static final String PROPERTIES_FILE_EXTN = ".properties";
	private static final Logger regProcLogger = LoggerFactory.getLogger(MosipStageExecutorApplication.class);

	/**
	 * main method to launch external stage application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		regProcLogger.info("Starting mosip-stage-executor>>>>>>>>>>>>>>");
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new AnnotationConfigApplicationContext(
				StagesConfig.class);) {
			StagesConfig stagesConfig = stageInfoApplicationContext.getBean(StagesConfig.class);
			@SuppressWarnings("unchecked")
			List<Class<MosipVerticleAPIManager>> stageClasses = stagesConfig.getStageClasses().stream()
					.map(classStr -> {
						try {
							return (Class<MosipVerticleAPIManager>) Class.forName(classStr);
						} catch (ClassNotFoundException e1) {
							regProcLogger.error("Unable to load Bean : " + classStr);
							throw new RuntimeException("Invalid config");
						}
					}).filter(Objects::nonNull).collect(Collectors.toList());
			
			Class<?>[] entrypointConfigClasses = Stream
					.concat(Stream.of(StagesConfig.class), stageClasses.stream())
					.toArray(size -> new Class<?>[size]);
			
			final String configFolder = stagesConfig.getConfigFolder();
			// This needs to be anonymous class only. Should not be converted to inner
			// class, because, the configFolder variable needs to be used consumed during
			// the initialization of the superclass constructor itself by the createEnvironment() method.
			try (AnnotationConfigApplicationContext mainApplicationContext = new AnnotationConfigApplicationContext(
					entrypointConfigClasses) {
				
				protected ConfigurableEnvironment createEnvironment() {
					ConfigurableEnvironment environment = super.createEnvironment();
					environment.merge(createCustomEnvironment(configFolder));
					return environment;
				}

				protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
					System.out.println(beanFactory);
				}
				
			};) {
				if (!stageClasses.isEmpty()) {
					ExecutorService executorService = Executors.newFixedThreadPool(stageClasses.size());
					stageClasses.forEach(stageClass -> executorService.execute(() -> {
						try {
							MosipVerticleAPIManager stageBean = getStageBean(mainApplicationContext, stageClass);
							stageBean.deployVerticle();
						} catch (Exception e) {
							regProcLogger.error("Exception occured while loading verticles. "
									+ "Please make sure correct verticle name was passed from deployment script.",
									ExceptionUtils.getStackTrace(e));
						}
					}));
					executorService.shutdown();
				}
			}
		}

	}
	
	public static CustomEnvironment createCustomEnvironment(String configFolderPath) {
		CustomEnvironment newEnv = new CustomEnvironment();
		MutablePropertySources propSources = new MutablePropertySources();
		
		Stream.of(new File(configFolderPath).listFiles())
				.filter(File::isFile)
				.filter(file -> file.getName().toLowerCase().endsWith(PROPERTIES_FILE_EXTN))
				.map(file -> new FileSystemResource(new File(configFolderPath + "/" + file.getName())))
				.forEach(res -> {
					try {
						propSources.addFirst(new ResourcePropertySource(res));
					} catch (IOException e) {
						throw new RuntimeException("Unable to load config: " + res.getPath());
					}
				});
		
		newEnv.customizePropertySources(propSources);
		return newEnv;
	}

	private static MosipVerticleAPIManager getStageBean(AnnotationConfigApplicationContext mainApplicationContext,
			Class<?> stageBeanClass) throws Exception {
		try {
			if (MosipVerticleAPIManager.class.isAssignableFrom(stageBeanClass)) {
				Object bean = mainApplicationContext.getBean(stageBeanClass);
				MosipVerticleAPIManager stageBean = (MosipVerticleAPIManager) bean;
				regProcLogger.info("Successfully loaded Bean : " + stageBeanClass.getCanonicalName());
				return stageBean;
			} else {
				regProcLogger.error("Unable to load Bean : " + stageBeanClass.getCanonicalName());
				throw new Exception("Invalid config");
			}
		} catch (BeansException | ClassNotFoundException e) {
			throw e;
		}
	}
}
