package io.mosip.registration.processor.packet.receiver.config;

import java.io.File;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.impl.SwiftAdapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.kernel.idvalidator.rid.impl.RidValidatorImpl;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
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

	private static Logger logger = RegProcessorLogger.getLogger(PacketReceiverConfig.class);

	@Value("${mosip.regproc.virusscanner.provider}")
	private String virusScannerProviderName;
	private static final String s3_Adapter = "S3Adapter";
	private static final String swift_Adapter = "SwiftAdapter";

	@Value("${registration.processor.objectstore.adapter.name}")
	private String adapter;
	
	@Bean
	@Primary
	public ObjectStoreAdapter objectStoreAdapter() {
		if (adapter.equalsIgnoreCase(s3_Adapter))
			return new S3Adapter();
		else if (adapter.equalsIgnoreCase(swift_Adapter))
			return new SwiftAdapter();
		else
			throw new UnsupportedOperationException("No adapter implementation found for configuration: registration.processor.objectstore.adapter.name");
	}
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
	 * Virus scanner service. Load virus scanner during runtime from property mosip.regproc.virusscanner.provider
	 *
	 * @return the virus scanner
	 */
	@Bean
	@Lazy
	public VirusScanner<Boolean, InputStream> virusScannerService() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		logger.info("mosip.regproc.virusscanner.provider is set as ", virusScannerProviderName,
				"Validating if the implementation is present in classpath", "");
		VirusScanner virusScanner = null;
		try {
			virusScanner = (VirusScanner) Class.forName(virusScannerProviderName).newInstance();

		} catch (ClassNotFoundException | ClassCastException e) {
			logger.error("Exception occurred validating - " + virusScannerProviderName +
					". Please make sure implementation is available in classpath", e);
			throw e;
		}
		logger.info("Successfully validated : " + virusScannerProviderName);

		return virusScanner;
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
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new AfterburnerModule()).registerModule(new JavaTimeModule());
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		return objectMapper;
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
