package io.mosip.registration.processor.packet.uploader.config;

import java.io.InputStream;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.impl.SwiftAdapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.kernel.virusscanner.clamav.impl.VirusScannerImpl;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.packet.uploader.archiver.util.PacketArchiver;
import io.mosip.registration.processor.packet.uploader.service.PacketUploaderService;
import io.mosip.registration.processor.packet.uploader.service.impl.PacketUploaderServiceImpl;
import io.mosip.registration.processor.packet.uploader.stage.PacketUploaderStage;
import org.springframework.context.annotation.Primary;


/**
 * @author Mukul Puspam
 *
 */
@Configuration
public class PacketUploaderConfig {

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
	 * PacketUploaderStage Bean
	 * @return
	 */
	@Bean
	public PacketUploaderStage getPacketUploaderStage() {
		return new PacketUploaderStage();
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
	 * Virus scanner service.
	 *
	 * @return the virus scanner
	 */
	@Bean
	public VirusScanner<Boolean, InputStream> virusScannerService() {
		return new VirusScannerImpl();
	}


}
