package io.mosip.registration.processor.packet.receiver.config;

import java.io.File;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.kernel.idvalidator.rid.impl.RidValidatorImpl;
import io.mosip.kernel.virusscanner.clamav.impl.VirusScannerImpl;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.manager.decryptor.DecryptorImpl;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;
import io.mosip.registration.processor.packet.manager.idreposervice.impl.IdRepoServiceImpl;
import io.mosip.registration.processor.packet.manager.service.impl.FileManagerImpl;
import io.mosip.registration.processor.packet.receiver.builder.PacketReceiverResponseBuilder;
import io.mosip.registration.processor.packet.receiver.exception.handler.PacketReceiverExceptionHandler;
import io.mosip.registration.processor.packet.receiver.service.PacketReceiverService;
import io.mosip.registration.processor.packet.receiver.service.impl.PacketReceiverServiceImpl;
import io.mosip.registration.processor.packet.receiver.stage.PacketReceiverStage;

/**
 * The Class PacketReceiverConfig.
 */
/**
 * @author Mukul Puspam
 *
 */
@Configuration
@EnableAspectJAutoProxy
public class PacketReceiverConfig {

	/**
	 * PacketReceiverService bean.
	 *
	 * @return the packet receiver service
	 */
	@Bean
	public PacketReceiverService<File, MessageDTO> getPacketReceiverService() {
		return new PacketReceiverServiceImpl();
	}

	/**
	 * PacketReceiverStage bean.
	 *
	 * @return the packet receiver stage
	 */
	@Bean
	public PacketReceiverStage getPacketReceiverStage() {
		return new PacketReceiverStage();
	}

	/**
	 * GlobalExceptionHandler bean.
	 *
	 * @return the global exception handler
	 */
	@Bean
	public PacketReceiverExceptionHandler getGlobalExceptionHandler() {
		return new PacketReceiverExceptionHandler();
	}

	/**
	 * Gets the packet receiver response builder.
	 *
	 * @return the packet receiver response builder
	 */
	@Bean
	public PacketReceiverResponseBuilder getPacketReceiverResponseBuilder() {
		return new PacketReceiverResponseBuilder();
	}

	/**
	 * Virus scanner service.
	 *
	 * @return the virus scanner
	 */
	@Bean
	public VirusScanner<Boolean, InputStream> virusScannerService() {
		return new VirusScannerImpl();
	}
	
	@Bean
	@Primary
	public RidValidator<String> getRidValidator() {
		return new RidValidatorImpl();
	}

	@Bean
	@Primary
	public CbeffUtil getCbeffUtil() {
		return new CbeffImpl();
	}
	
	@Bean
	@Primary
	public ObjectMapper getObjectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
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
