package io.mosip.registration.processor.stages.executor.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@RefreshScope
//This is added to fix issue with loading beans with @RefreshScope in the stages
@ImportAutoConfiguration({RefreshAutoConfiguration.class})
//This is added to fix issue with loading stage specific properties from bootstrap, expected by the stages config.
@PropertySource("classpath:bootstrap.properties")
public class StagesConfig {
	
	
	@Value("${mosip.regproc.stageClasses}")
	private String[] stageClasses;
	
	public List<String> getStageClasses() {
		return Arrays.asList(stageClasses);
	}
		
}
