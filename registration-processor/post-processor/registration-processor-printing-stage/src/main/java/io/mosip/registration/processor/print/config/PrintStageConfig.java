package io.mosip.registration.processor.print.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.print.stage.PrintingStage;

/**
 * @author M1048399
 *
 */
@Configuration
public class PrintStageConfig {

	@Bean 
	public PrintingStage getPrintStage() {
		return new PrintingStage();
	}
	


}
