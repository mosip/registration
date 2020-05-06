package io.mosip.registration.processor.packet.utility.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.registration.processor.packet.utility.service.Decryptor;
import io.mosip.registration.processor.packet.utility.service.impl.DecryptorImpl;
import io.mosip.registration.processor.packet.utility.utils.IdSchemaUtils;

@Configuration
public class PacketUtilityConfig {

	@Bean
	@Qualifier("decrypter")
	public Decryptor getDecryptor() {
		return new DecryptorImpl();
	}

	@Bean
	public IdSchemaUtils getIdSchemaUtils() {
		return new IdSchemaUtils();
	}
}
