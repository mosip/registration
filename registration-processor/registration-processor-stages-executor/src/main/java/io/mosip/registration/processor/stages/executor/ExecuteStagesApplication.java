package io.mosip.registration.processor.stages.executor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.spi.stage.StageInfo;
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
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new AnnotationConfigApplicationContext();) {
			stageInfoApplicationContext.scan(
					"io.mosip.registration.processor.stages.executor.config"
					);
			stageInfoApplicationContext.refresh();

			StagesConfig stagesConfig = stageInfoApplicationContext.getBean(StagesConfig.class);
			List<StageInfo> stageInfos = stagesConfig.getStages();
			try (AnnotationConfigApplicationContext mainApplicationContext = new AnnotationConfigApplicationContext();) {
				// Scan packages
				stageInfos.stream().forEach(stageInfo -> {
					mainApplicationContext.scan(stageInfo.getBasePackages());
				});

				// Refresh the context
				mainApplicationContext.refresh();

				if (!stageInfos.isEmpty()) {
					ExecutorService executorService = Executors.newFixedThreadPool(stageInfos.size());
					stageInfos.forEach(stageInfo -> executorService.execute(() -> {
						try {
							MosipVerticleAPIManager stageBean = getStageBean(mainApplicationContext, stageInfo);
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
			StageInfo stageInfo) throws Exception {
		try {
			Class<?> stageBeanClass = Class.forName(stageInfo.getStageClass());
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
