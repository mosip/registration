package io.mosip.registration.processor.workflowmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;

import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.registration.processor.core.config.configserverloader.PropertyLoaderConfig;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionApi;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowActionJob;
import io.mosip.registration.processor.workflowmanager.verticle.WorkflowInternalActionVerticle;

@Configuration
@EnableAutoConfiguration(exclude = { JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class })
@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.registration.processor.core.config",
		"${mosip.auth.adapter.impl.basepackage}",
		"io.mosip.registration.processor.workflowmanager.config",
		"io.mosip.registration.processor.status.config",
		"io.mosip.registration.processor.core.kernel.beans",
		"io.mosip.registration.processor.packet.storage.config",
		"io.mosip.kernel.websub.api.config.publisher",
		"io.mosip.registration.processor.packet.storage.utils.ActiveMqConnectionUtility" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
				HibernateDaoConfig.class, PropertyLoaderConfig.class
}))
public class WorkflowManagerApplication 
{
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(WorkflowManagerApplication.class, args);
		
		
		WorkflowInternalActionVerticle workflowInternalActionVerticle = ctx
				.getBean(WorkflowInternalActionVerticle.class);
		workflowInternalActionVerticle.deployVerticle();
		WorkflowActionApi workflowActionApi = ctx.getBean(WorkflowActionApi.class);
		workflowActionApi.deployVerticle();
		WorkflowActionJob workflowActionJob = ctx.getBean(WorkflowActionJob.class);
		workflowActionJob.deployVerticle();
	}
}
