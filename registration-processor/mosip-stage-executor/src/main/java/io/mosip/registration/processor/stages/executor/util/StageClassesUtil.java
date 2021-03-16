package io.mosip.registration.processor.stages.executor.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
 * The Class StageClassesUtil.
 */
public class StageClassesUtil {
	
	/** The Constant regProcLogger. */
	private static final Logger regProcLogger = LoggerFactory.getLogger(StageClassesUtil.class);
	
	/** The Constant PROP_STAGE_GROUPS_STAGE_BEANS_BASE_PACKAGES_PREFIX. */
	private static final String PROP_STAGE_GROUPS_STAGE_BEANS_BASE_PACKAGES_PREFIX = "mosip.regproc.stage-groups.stage-beans-base-packages.";

	/** The Constant DEFAULT_STAGES_BASE_PACKAGES. */
	private static final String DEFAULT_STAGES_BASE_PACKAGES = "io.mosip.registration.processor,io.mosip.registrationprocessor";


	/**
	 * Gets the stage bean classes.
	 *
	 * @param stageBeansBasePackages the stage beans base packages
	 * @return the stage bean classes
	 */
	@SuppressWarnings("unchecked")
	public static List<Class<MosipVerticleAPIManager>> getStageBeanClasses(List<String> stageBeansBasePackages) {
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

	/**
	 * Gets the stage bean class names.
	 *
	 * @param stageBeansBasePackages the stage beans base packages
	 * @return the stage bean class names
	 */
	public static Set<String> getStageBeanClassNames(List<String> stageBeansBasePackages) {
		Set<BeanDefinition> stagesBeanDefinitions = getStagesBeanDefinitions(stageBeansBasePackages);
		
		Set<String> stageClassesSet = stagesBeanDefinitions.stream().map(bd -> bd.getBeanClassName())
											.collect(Collectors.toSet());
		return stageClassesSet;
	}

	/**
	 * Gets the stage beans base packages.
	 *
	 * @param stagesConfig the stages config
	 * @param propertySources the property sources
	 * @return the stage beans base packages
	 */
	public static List<String> getStageBeansBasePackages(StagesConfig stagesConfig, MutablePropertySources propertySources) {
		String stageBeansBasePkgsPropertyName= PROP_STAGE_GROUPS_STAGE_BEANS_BASE_PACKAGES_PREFIX + stagesConfig.getStageGroupName();
		PropertySourcesPropertyResolver propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
		String stageBeanBasePkgsStr = propertySourcesPropertyResolver.getProperty(stageBeansBasePkgsPropertyName, DEFAULT_STAGES_BASE_PACKAGES);
		
		List<String> stageBeansBasePackages = Arrays.stream(stageBeanBasePkgsStr.split(","))
													.map(String::trim)
													.filter(str -> !str .isEmpty())
													.collect(Collectors.toList());
		return stageBeansBasePackages;
	}

	/**
	 * Gets the stages bean definitions.
	 *
	 * @param stageBeansBasePackages the stage beans base packages
	 * @return the stages bean definitions
	 */
	public static Set<BeanDefinition> getStagesBeanDefinitions(List<String> stageBeansBasePackages) {
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
	public static MosipVerticleAPIManager getStageBean(AnnotationConfigApplicationContext mainApplicationContext,
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
