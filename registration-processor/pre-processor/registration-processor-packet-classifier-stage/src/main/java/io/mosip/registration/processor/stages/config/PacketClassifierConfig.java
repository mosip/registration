package io.mosip.registration.processor.stages.config;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.stages.packetclassifier.PacketClassificationProcessor;
import io.mosip.registration.processor.stages.packetclassifier.PacketClassifierStage;

/**
 * This Class is a configuration class which declares all the beans that is required by this stage
 * except the tag generator implementation classes
 * The beans belonging to other libraries has their own config class to declare the required beans
 */
@Configuration
@RefreshScope
public class PacketClassifierConfig {

	@Bean
	public PacketClassifierStage getPacketClassifierStage() {
		return new PacketClassifierStage();
	}

	@Bean
	public PacketClassificationProcessor getPacketClassificationProcessor() {
		return new PacketClassificationProcessor();
	}

	@Bean
	public RestApiClient getRestApiClient() {
		return new RestApiClient();
	}
}
