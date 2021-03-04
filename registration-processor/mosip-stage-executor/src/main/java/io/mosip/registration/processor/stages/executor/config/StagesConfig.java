package io.mosip.registration.processor.stages.executor.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:bootstrap.properties")
public class StagesConfig {
	
	
	@Value("${mosip.regproc.stageClasses}")
	private String[] stageClasses;
	
	
	public List<String> getStageClasses() {
		return Arrays.asList(stageClasses);
	}
	
}
