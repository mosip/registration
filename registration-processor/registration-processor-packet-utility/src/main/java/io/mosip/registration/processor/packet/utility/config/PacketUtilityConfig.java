package io.mosip.registration.processor.packet.utility.config;

import io.mosip.registration.processor.packet.utility.utils.RestUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.mosip.registration.processor.packet.utility.service.PacketDecryptor;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;
import io.mosip.registration.processor.packet.utility.service.impl.PacketDecryptorImpl;
import io.mosip.registration.processor.packet.utility.service.impl.PacketReaderServiceImpl;
import io.mosip.registration.processor.packet.utility.utils.IdSchemaUtils;

@Configuration
public class PacketUtilityConfig {

	@Bean
	@Primary
	public PacketDecryptor getPacketDecryptor() {
		return new PacketDecryptorImpl();
	}

	@Bean
	public IdSchemaUtils getIdSchemaUtils() {
		return new IdSchemaUtils();
	}

	@Bean
	public PacketReaderService getPacketReaderService() {
		return new PacketReaderServiceImpl();
	}

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}
}
