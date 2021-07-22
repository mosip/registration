package io.mosip.registration.processor.packet.uploader.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.core.env.Environment;


/**
 * @author Mukul Puspam
 *
 */
@Configuration
public class PacketUploaderConfig {

	private static final String s3_Adapter = "S3Adapter";
	private static final String swift_Adapter = "SwiftAdapter";
	public static final String VIRUS_SCANNER_PROVIDER = "mosip.regproc.virusscanner.provider";

	private static Logger logger = RegProcessorLogger.getLogger(PacketUploaderConfig.class);

	@Value("${registration.processor.objectstore.adapter.name}")
	private String adapter;

	@Autowired
	private Environment env;

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
		if (StringUtils.isNotBlank(env.getProperty(VIRUS_SCANNER_PROVIDER))) {
			logger.debug("mosip.regproc.virusscanner.provider is set as ", env.getProperty(VIRUS_SCANNER_PROVIDER),
					"loading VirusScanner", "");
			return (VirusScanner) Class.forName(env.getProperty(VIRUS_SCANNER_PROVIDER)).newInstance();
		} else {
			logger.debug("Property 'mosip.regproc.virusscanner.provider' is not set with correct VirusScanner instance",
					env.getProperty(VIRUS_SCANNER_PROVIDER),
					"loading dummy VirusScanner.", "This will by default fail virus scanning");
			return new VirusScanner<Boolean, InputStream>() {
				@Override
				public Boolean scanFile(String s) {
					return Boolean.FALSE;
				}

				@Override
				public Boolean scanFile(InputStream inputStream) {
					return Boolean.FALSE;
				}

				@Override
				public Boolean scanFolder(String s) {
					return Boolean.FALSE;
				}

				@Override
				public Boolean scanDocument(byte[] bytes) throws IOException {
					return Boolean.FALSE;
				}

				@Override
				public Boolean scanDocument(File file) throws IOException {
					return Boolean.FALSE;
				}
			};
		}
	}


}
