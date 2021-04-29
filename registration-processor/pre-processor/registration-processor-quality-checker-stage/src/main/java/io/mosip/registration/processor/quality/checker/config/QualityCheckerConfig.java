package io.mosip.registration.processor.quality.checker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;


@PropertySource("classpath:bootstrap.properties")
@Configuration
public class QualityCheckerConfig {


	@Bean
	public CbeffUtil getCbeffUtil() {
		return new CbeffImpl();
	}
}
