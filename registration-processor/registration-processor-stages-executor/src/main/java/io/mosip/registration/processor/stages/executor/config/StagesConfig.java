package io.mosip.registration.processor.stages.executor.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.registration.processor.core.spi.stage.StageInfo;

@Configuration
@EnableConfigurationProperties
@PropertySource("classpath:bootstrap.properties")
@ConfigurationProperties(prefix = "mosip.regproc.stageinfo")
public class StagesConfig {
	
	private List<StageInfo> stages = new ArrayList<>();
	
	public List<StageInfo> getStages() {
		return stages;
	}
	
}
