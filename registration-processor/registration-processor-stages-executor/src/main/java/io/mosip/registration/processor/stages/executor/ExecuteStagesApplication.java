package io.mosip.registration.processor.stages.executor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.stages.executor.config.StagesConfig;

/**
 * External Stage application
 *
 */
public class ExecuteStagesApplication {

	/**
	 * main method to launch external stage application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new AnnotationConfigApplicationContext(StagesConfig.class);) {
			StagesConfig stagesConfig = stageInfoApplicationContext.getBean(StagesConfig.class);
			List<String> stageClasses = stagesConfig.getStageClasses();
			try (AnnotationConfigApplicationContext mainApplicationContext = new AnnotationConfigApplicationContext();) {
				List<String> basePackages = stageClasses.stream()
												.map(classStr -> classStr.substring(0, classStr.lastIndexOf('.')))
												.collect(Collectors.toList());
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}));
					try {
						executorService.awaitTermination(10000, TimeUnit.DAYS);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					executorService.shutdown();
				}
			}
		}

	}

	private static MosipVerticleAPIManager getStageBean(AnnotationConfigApplicationContext mainApplicationContext,
			String stageClass) throws Exception {
		try {
			Class<?> stageBeanClass = Class.forName(stageClass);
			if (MosipVerticleAPIManager.class.isAssignableFrom(stageBeanClass)) {
				Object bean = mainApplicationContext.getBean(stageBeanClass);
				MosipVerticleAPIManager stageBean = (MosipVerticleAPIManager) bean;
				return stageBean;
			} else {
				// TODO throw invalid config exception
				throw new Exception("Invalid config");
			}
		} catch (BeansException | ClassNotFoundException e) {
			throw e;
		}
	}
}
