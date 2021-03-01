package io.mosip.registration.processor.stages.executor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.core.abstractverticle.MosipVerticleAPIManager;
import io.mosip.registration.processor.core.spi.stage.StageInfo;

/**
 * External Stage application
 *
 */
public class ExecuteStagesApplication {

	private static final String PROP_STAGE_INFO_BASE_PACKAGES_DEFAULT_VALUE = "io.mosip.registration.processor.**.stageinfo";
	private static final String PROP_STAGE_INFO_BASE_PACKAGES_KEY = "stage.info.base.packages";

	/**
	 * main method to launch external stage application
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try (AnnotationConfigApplicationContext stageInfoApplicationContext = new AnnotationConfigApplicationContext();) {
			stageInfoApplicationContext.scan(getBaseStageInfoPackages());
			stageInfoApplicationContext.refresh();
			
			Map<String, StageInfo> allStageInfos = stageInfoApplicationContext.getBeansOfType(StageInfo.class);

			List<StageInfo> stageInfos = allStageInfos.values().stream().collect(Collectors.toList());
			try (AnnotationConfigApplicationContext mainApplicationContext = new AnnotationConfigApplicationContext();) {
				// Scan packages
				stageInfos.stream().forEach(stageInfo -> {
					mainApplicationContext.scan(stageInfo.getBasePackages());
				});

				// Refresh the context
				mainApplicationContext.refresh();

				stageInfos.forEach(stageInfo -> {
					MosipVerticleAPIManager stageBean = mainApplicationContext.getBean(stageInfo.getStageBeanClass());
					stageBean.deployVerticle();
				});
			}
		}

	}

	private static String[] getBaseStageInfoPackages() {
		String pkgs = System.getProperty(PROP_STAGE_INFO_BASE_PACKAGES_KEY, PROP_STAGE_INFO_BASE_PACKAGES_DEFAULT_VALUE);
		return Arrays.stream(pkgs.split(",")).filter(str -> !str.isEmpty())
					.toArray(size -> new String[size]);
	}

}
