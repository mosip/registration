package io.mosip.registration.processor.util;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.impl.SwiftAdapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.registration.processor.core.spi.filesystem.manager.FileManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.manager.dto.DirectoryPathDto;
import io.mosip.registration.processor.packet.manager.service.impl.FileManagerImpl;
import io.mosip.registration.processor.rest.client.service.impl.RegistrationProcessorRestClientServiceImpl;

@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
public class LandingZoneConfig {
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
	
	@Bean
	@Primary
	public FileManager<DirectoryPathDto, InputStream> filemanager() {
		return new FileManagerImpl();
	}
	
	@Bean
	@Primary
	public RegistrationProcessorRestClientService<Object> getRegistrationProcessorRestClientService() {
		return new RegistrationProcessorRestClientServiceImpl();
	}

}
