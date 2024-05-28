package io.mosip.registration.processor.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.registration.processor.core.config.configserverloader.PropertyLoaderConfig;

import io.mosip.registration.processor.rest.client.config.RestConfigBean;
/**
 * Init class for Notification service.
 *
 * @author Urvil Joshi
 * @since 1.0.0
 *
 */
@SpringBootApplication(exclude = { JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class })
@ComponentScan( basePackages = { "io.mosip.registration.processor.notification.*",
		"io.mosip.kernel.websub.api.*","${mosip.auth.adapter.impl.basepackage}", "io.mosip.registration.processor.message.sender.config",
		"io.mosip.registration.processor.rest.client.*", "io.mosip.registration.processor.packet.storage.config",
		"io.mosip.registration.processor.core.config", "io.mosip.registration.processor.packet.storage.dao",
		"io.mosip.registration.processor.status.*",
		"io.mosip.registration.processor.core.kernel.beans" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
				RestConfigBean.class, PropertyLoaderConfig.class }))
@EnableScheduling
public class NotificationApplication {

	/**
	 * Main method to run spring boot application
	 * 
	 * @param args args
	 */
	public static void main(String[] args) {
		SpringApplication.run(NotificationApplication.class, args);
	}

}
