package io.mosip.registration.processor.stages.executor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MutablePropertySources;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.stages.executor.config.StagesConfig;
import io.mosip.registration.processor.stages.executor.util.StageClassesUtil;

/**
 * The Class MosipStageExecutorApplication.
 */
public class MosipStageExecutorApplication {
	
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
			
			List<String> stageBeansBasePackages = StageClassesUtil.getStageBeansBasePackages(stagesConfig, propertySources);
			
			List<Class<MosipVerticleAPIManager>> stageClasses = StageClassesUtil.getStageBeanClasses(stageBeansBasePackages);

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
							MosipVerticleAPIManager stageBean = StageClassesUtil.getStageBean(mainApplicationContext, stageClass);
							stageBean.deployVerticle();
						} catch (Exception e) {
							regProcLogger.error("Exception occured while loading verticles. Please make sure correct verticle name was passed from deployment script. \n {}",
									ExceptionUtils.getStackTrace(e));
						}
					}));
					executorService.shutdown();
				}
			}
		}

	}
}
