package io.mosip.registration.processor.packet.manager.config;

import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.manager.decryptor.DecryptorImpl;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.manager.idreposervice.impl.IdRepoServiceImpl;
import io.mosip.registration.processor.packet.manager.service.impl.FileManagerImpl;
import io.mosip.registration.processor.packet.manager.service.impl.FileSystemManagerImpl;

/**
 * The Class PacketManagerConfig.
 */
@Configuration
public class PacketManagerConfig {
	
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
    public PacketManager getFileSystemManager() {
        return new FileSystemManagerImpl();

    }
    @Bean
	@Primary
    public Decryptor getDecryptor() {
        return new DecryptorImpl();
    }

}
