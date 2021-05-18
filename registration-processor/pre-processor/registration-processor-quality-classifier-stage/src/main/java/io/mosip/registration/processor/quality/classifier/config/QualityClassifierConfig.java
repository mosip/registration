package io.mosip.registration.processor.quality.classifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;


@PropertySource("classpath:bootstrap.properties")
@Configuration
public class QualityClassifierConfig {


	@Bean
	public CbeffUtil getCbeffUtil() {
		return new CbeffImpl();
	}

	@Bean
	public BioAPIFactory getBioAPIFactory() {
		return new BioAPIFactory();
	}

}
