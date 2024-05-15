package io.mosip.registration.processor.packet.storage.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PacketReaderWriterConfig {

	@Bean(name = "readerConfiguration")
	@ConfigurationProperties(prefix = "provider.packetreader")
	public Map<String, String> readerConfiguration() {
		return new HashMap<>();
	}

	@Bean(name = "providerConfiguration")
	@ConfigurationProperties(prefix = "packetmanager.provider")
	public Map<String, String> providerConfiguration() {
		return new HashMap<>();
	}

	@Bean(name = "writerConfiguration")
	@ConfigurationProperties(prefix = "provider.packetwriter")
	public Map<String, String> writerConfiguration() {
		return new HashMap<>();
	}
}
