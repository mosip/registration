package io.mosip.registration.processor.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.mosip.commons.khazana.impl.S3Adapter;
import io.mosip.commons.khazana.impl.SwiftAdapter;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.service.impl.RegistrationProcessorRestClientServiceImpl;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.dto.SyncRegistrationDto;
import io.mosip.registration.processor.status.dto.SyncResponseDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;
import io.mosip.registration.processor.status.service.SyncRegistrationService;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.SyncRegistrationServiceImpl;

@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
public class LandingZoneUtilityConfig {
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
	public RegistrationProcessorRestClientService<Object> RegistrationProcessorRestClientService() {
		return new RegistrationProcessorRestClientServiceImpl();
	}
	@Bean
	@Primary
	public SyncRegistrationService<SyncResponseDto, SyncRegistrationDto> SyncRegistrationService() {
		return new SyncRegistrationServiceImpl();
	}
}
