package io.mosip.registration.processor.packet.manager.config;

import java.io.InputStream;

import io.mosip.registration.processor.packet.manager.idreposervice.IdrepoDraftService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.manager.decryptor.DecryptorImpl;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.manager.idreposervice.impl.IdRepoServiceImpl;
import io.mosip.registration.processor.packet.manager.service.impl.FileManagerImpl;

/**
 * The Class PacketManagerConfig.
 */
@Configuration
public class PacketManagerConfig {

	@Bean
	public IdrepoDraftService idrepoDraftService() {
		return new IdrepoDraftService();
	}
	
	@Bean
	@Primary
	public FileManager<DirectoryPathDto, InputStream> filemanager() {
		return new FileManagerImpl();
	}

	@Bean
	@Primary
	public IdRepoService getIdRepoService() {
		return new IdRepoServiceImpl();
	}

    @Bean
	@Primary
    public Decryptor getDecryptor() {
        return new DecryptorImpl();
    }

}
