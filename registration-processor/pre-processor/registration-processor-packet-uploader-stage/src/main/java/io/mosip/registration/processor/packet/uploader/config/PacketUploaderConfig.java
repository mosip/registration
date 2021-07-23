package io.mosip.registration.processor.packet.uploader.config;

import java.io.InputStream;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.impl.SwiftAdapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.packet.uploader.archiver.util.PacketArchiver;
import io.mosip.registration.processor.packet.uploader.service.PacketUploaderService;
import io.mosip.registration.processor.packet.uploader.service.impl.PacketUploaderServiceImpl;


/**
 * @author Mukul Puspam
 *
 */
@Configuration
public class PacketUploaderConfig {

	private static final String s3_Adapter = "S3Adapter";
	private static final String swift_Adapter = "SwiftAdapter";

	private static Logger logger = RegProcessorLogger.getLogger(PacketUploaderConfig.class);

	@Value("${registration.processor.objectstore.adapter.name}")
	private String adapter;

	@Value("${mosip.regproc.virusscanner.provider}")
	private String virusScannerProviderName;

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
	 * PacketArchiver Bean
	 * @return
	 */
	@Bean
	public PacketArchiver getPacketArchiver() {
		return new PacketArchiver();
	}
	
	
	@Bean
	public PacketUploaderService<MessageDTO> getPacketUploaderService() {
		return new PacketUploaderServiceImpl();
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


}
