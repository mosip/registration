package io.mosip.registration.processor.stages.executor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new CustomConfigApplicationContext(
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

			Class<?>[] entrypointConfigClasses = Stream.concat(Stream.of(StagesConfig.class), stageClasses.stream())
					.toArray(size -> new Class<?>[size]);

			try (AnnotationConfigApplicationContext mainApplicationContext = new CustomConfigApplicationContext(
					entrypointConfigClasses);) {
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
