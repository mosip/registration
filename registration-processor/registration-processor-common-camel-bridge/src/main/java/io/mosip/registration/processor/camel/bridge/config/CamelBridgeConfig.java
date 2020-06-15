package io.mosip.registration.processor.camel.bridge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import io.mosip.registration.processor.camel.bridge.MosipBridgeFactory;
import io.mosip.registration.processor.camel.bridge.processor.TokenGenerationProcessor;
import org.springframework.context.annotation.Primary;

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

}