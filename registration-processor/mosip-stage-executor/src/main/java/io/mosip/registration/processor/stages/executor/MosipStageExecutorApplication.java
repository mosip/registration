package io.mosip.registration.processor.stages.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.stages.executor.config.StagesConfig;

/**
 * External Stage application
 *
 */
public class MosipStageExecutorApplication {

	private static final Logger regProcLogger = LoggerFactory.getLogger(MosipStageExecutorApplication.class);

	/**
	 * main method to launch external stage application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		regProcLogger.info("Starting mosip-stage-executor>>>>>>>>>>>>>>");
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new AnnotationConfigApplicationContext(StagesConfig.class);) {
			StagesConfig stagesConfig = stageInfoApplicationContext.getBean(StagesConfig.class);
			List<String> stageClasses = stagesConfig.getStageClasses();
			try (AnnotationConfigApplicationContext mainApplicationContext = new AnnotationConfigApplicationContext();) {
				List<String> basePackages = stageClasses.stream()
												.map(classStr -> getPackageOfClass(classStr))
												.collect(Collectors.toCollection(() -> new LinkedList<>()));
				basePackages.add(0, getPackageOfClass(StagesConfig.class.getCanonicalName()));
				mainApplicationContext.scan(basePackages.toArray(size -> new String[size]));

				// Refresh the context
				mainApplicationContext.refresh();

				if (!stageClasses.isEmpty()) {
					ExecutorService executorService = Executors.newFixedThreadPool(stageClasses.size());
					stageClasses.forEach(stageClass -> executorService.execute(() -> {
						try {
							MosipVerticleAPIManager stageBean = getStageBean(mainApplicationContext, stageClass);
							stageBean.deployVerticle();
						} catch (Exception e) {
							regProcLogger.error("Exception occured while loading verticles. " +
									"Please make sure correct verticle name was passed from deployment script.",
									ExceptionUtils.getStackTrace(e));
						}
					}));
					executorService.shutdown();
				}
			}
		}

	}

	private static String getPackageOfClass(String classStr) {
		return classStr.substring(0, classStr.lastIndexOf('.'));
	}

	private static MosipVerticleAPIManager getStageBean(AnnotationConfigApplicationContext mainApplicationContext,
			String stageClass) throws Exception {
		try {
			Class<?> stageBeanClass = Class.forName(stageClass);
			if (MosipVerticleAPIManager.class.isAssignableFrom(stageBeanClass)) {
				Object bean = mainApplicationContext.getBean(stageBeanClass);
				MosipVerticleAPIManager stageBean = (MosipVerticleAPIManager) bean;
				regProcLogger.info("Successfully loaded Bean : " + stageClass);
				return stageBean;
			} else {
				regProcLogger.error("Unable to load Bean : " + stageClass);
				throw new Exception("Invalid config");
			}
		} catch (BeansException | ClassNotFoundException e) {
			throw e;
		}
	}
}
