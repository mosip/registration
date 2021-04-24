package io.mosip.registration.processor.stages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.service.impl.RegistrationProcessorRestClientServiceImpl;
import io.mosip.registration.processor.stages.demodedupe.DemoDedupe;
import io.mosip.registration.processor.stages.demodedupe.DemodedupeProcessor;

@Configuration
public class DemoDedupeConfig {

	@Bean
	public DemoDedupe getDemoDedupe() {
		return new DemoDedupe();
	}

	@Bean
	public DemodedupeProcessor getDemodedupeProcessor() {
		return new DemodedupeProcessor();
	}

	@Bean
	public RegistrationProcessorRestClientService<Object> getRegistrationProcessorRestClientService() {
		return new RegistrationProcessorRestClientServiceImpl();
	}

}
