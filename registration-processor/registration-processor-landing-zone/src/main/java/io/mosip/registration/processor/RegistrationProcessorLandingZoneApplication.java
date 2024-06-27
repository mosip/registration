package io.mosip.registration.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;

import io.mosip.registration.processor.core.config.configserverloader.PropertyLoaderConfig;
import io.mosip.registration.processor.rest.client.config.RestConfigBean;

@SpringBootApplication(exclude = { JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class })
@ComponentScan( basePackages = { "io.mosip.registration.processor.status.*",
		"io.mosip.registration.processor.rest.client.*", "io.mosip.registration.processor.util",
		"io.mosip.registration.processor.core.config",
		"${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
				RestConfigBean.class, PropertyLoaderConfig.class }))
public class RegistrationProcessorLandingZoneApplication {

	public static void main(String[] args) {
		SpringApplication.run(RegistrationProcessorLandingZoneApplication.class, args);
	}

}
