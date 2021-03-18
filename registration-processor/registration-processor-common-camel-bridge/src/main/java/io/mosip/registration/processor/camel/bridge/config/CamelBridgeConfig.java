package io.mosip.registration.processor.camel.bridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.registration.processor.camel.bridge.MosipBridgeFactory;
import io.mosip.registration.processor.camel.bridge.intercepter.RouteIntercepter;
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
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
	
	@Bean
	public RouteIntercepter routeIntercepter() {
		return new RouteIntercepter();
	}

}