
package io.mosip.registration.processor.camel.bridge.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.registration.processor.camel.bridge.MosipBridgeFactory;
import io.mosip.registration.processor.camel.bridge.intercepter.PauseFlowPredicate;
import io.mosip.registration.processor.camel.bridge.intercepter.RouteIntercepter;
import io.mosip.registration.processor.camel.bridge.intercepter.WorkflowCommandPredicate;
import io.mosip.registration.processor.camel.bridge.processor.TokenGenerationProcessor;

@Configuration
@EnableAspectJAutoProxy
public class CamelBridgeConfig {
	
	@Bean
	public MosipBridgeFactory getMosipBridgeFactory() {
		return new MosipBridgeFactory();
	}
	
	@Bean
	public TokenGenerationProcessor tokenGenerationProcessor() {
		return new TokenGenerationProcessor();
	}

	@Bean
	@Primary
	public ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new AfterburnerModule()).registerModule(new JavaTimeModule());
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		return objectMapper;
	}
	
	@Bean
	public PauseFlowPredicate pauseFlowPredicate() {
		return new PauseFlowPredicate();
	}
	
	@Bean
	public RouteIntercepter routeIntercepter() {
		return new RouteIntercepter();
	}

	@Bean
	public WorkflowCommandPredicate workflowCommandPredicate() {
		return new WorkflowCommandPredicate();
	}
}