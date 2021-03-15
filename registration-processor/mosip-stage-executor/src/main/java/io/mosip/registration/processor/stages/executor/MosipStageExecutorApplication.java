package io.mosip.registration.processor.stages.executor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.stages.executor.config.StagesConfig;

/**
 * The Class MosipStageExecutorApplication.
 */
public class MosipStageExecutorApplication {
	
	private static final String PROP_STAGE_GROUPS_STAGE_BEANS_BASE_PACKAGES_PREFIX = "mosip.regproc.stage-groups.stage-beans-base-packages.";

	private static final String DEFAULT_STAGES_BASE_PACKAGES = "io.mosip.registration.processor,io.mosip.registrationprocessor";

	/** The Constant regProcLogger. */
	private static final Logger regProcLogger = LoggerFactory.getLogger(MosipStageExecutorApplication.class);

	/**
	 * main method to launch external stage application.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		regProcLogger.info("Starting mosip-stage-executor>>>>>>>>>>>>>>");
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new AnnotationConfigApplicationContext(
				new Class<?>[] { StagesConfig.class });) {
			StagesConfig stagesConfig = stageInfoApplicationContext.getBean(StagesConfig.class);
			MutablePropertySources propertySources = stagesConfig.getCloudPropertySources();
			
			List<String> stageBeansBasePackages = getStageBeansBasePackages(stagesConfig, propertySources);
			
			List<Class<MosipVerticleAPIManager>> stageClasses = getStageBeanClasses(stageBeansBasePackages);

			Class<?>[] entrypointConfigClasses = Stream.concat(Stream.of(StagesConfig.class), stageClasses.stream())
					.toArray(size -> new Class<?>[size]);

			try (AnnotationConfigApplicationContext mainApplicationContext = new PropertySourcesCustomizingApplicationContext(
					entrypointConfigClasses) {
						@Override
						public MutablePropertySources getPropertySources() {
							return propertySources;
						};
					};) {
				if (!stageClasses.isEmpty()) {
					ExecutorService executorService = Executors.newFixedThreadPool(stageClasses.size());
					stageClasses.forEach(stageClass -> executorService.execute(() -> {
						try {
							regProcLogger.info("Executing Stage: {}", stageClass.getCanonicalName());
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

	@SuppressWarnings("unchecked")
	private static List<Class<MosipVerticleAPIManager>> getStageBeanClasses(List<String> stageBeansBasePackages) {
		Set<String> stageClassNamesSet = getStageBeanClassNames(stageBeansBasePackages);
		
		List<Class<MosipVerticleAPIManager>> stageClasses = stageClassNamesSet.stream()
				.map(classStr -> {
					try {
						return (Class<MosipVerticleAPIManager>) Class.forName(classStr);
					} catch (ClassNotFoundException e1) {
						regProcLogger.error("Unable to load stage class : \n{}", ExceptionUtils.getStackTrace(e1));
						throw new RuntimeException("Invalid config", e1);
					}
				}).filter(Objects::nonNull).collect(Collectors.toList());
		return stageClasses;
	}

	private static Set<String> getStageBeanClassNames(List<String> stageBeansBasePackages) {
		Set<BeanDefinition> stagesBeanDefinitions = getStagesBeanDefinitions(stageBeansBasePackages);
		
		Set<String> stageClassesSet = stagesBeanDefinitions.stream().map(bd -> bd.getBeanClassName())
											.collect(Collectors.toSet());
		return stageClassesSet;
	}

	private static List<String> getStageBeansBasePackages(StagesConfig stagesConfig, MutablePropertySources propertySources) {
		String stageBeansBasePkgsPropertyName= PROP_STAGE_GROUPS_STAGE_BEANS_BASE_PACKAGES_PREFIX + stagesConfig.getStageGroupName();
		PropertySourcesPropertyResolver propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
		String stageBeanBasePkgsStr = propertySourcesPropertyResolver.getProperty(stageBeansBasePkgsPropertyName, DEFAULT_STAGES_BASE_PACKAGES);
		
		List<String> stageBeansBasePackages = Arrays.stream(stageBeanBasePkgsStr.split(","))
													.map(String::trim)
													.filter(str -> !str .isEmpty())
													.collect(Collectors.toList());
		return stageBeansBasePackages;
	}

	private static Set<BeanDefinition> getStagesBeanDefinitions(List<String> stageBeansBasePackages) {
		ClassPathScanningCandidateComponentProvider pro = new ClassPathScanningCandidateComponentProvider(false);
		TypeFilter tf = new AssignableTypeFilter(MosipVerticleAPIManager.class);
		pro.addIncludeFilter(tf);
		Set<BeanDefinition> stagesBeanDefinitions = stageBeansBasePackages.stream()
						.flatMap(pkg -> pro.findCandidateComponents(pkg).stream())
						.collect(Collectors.toSet());
		return stagesBeanDefinitions;
	}

	/**
	 * Gets the stage bean.
	 *
	 * @param mainApplicationContext the main application context
	 * @param stageBeanClass         the stage bean class
	 * @return the stage bean
	 * @throws Exception the exception
	 */
	private static MosipVerticleAPIManager getStageBean(AnnotationConfigApplicationContext mainApplicationContext,
			Class<MosipVerticleAPIManager> stageBeanClass) throws Exception {
		try {
			Object bean = mainApplicationContext.getBean(stageBeanClass);
			MosipVerticleAPIManager stageBean = (MosipVerticleAPIManager) bean;
			regProcLogger.info("Successfully loaded Bean : " + stageBeanClass.getCanonicalName());
			return stageBean;
		} catch (BeansException e) {
			throw e;
		}
	}
}
