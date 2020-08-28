package io.mosip.registration.processor.bio.dedupe.api.config;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.config.CoreConfigBean;
import io.mosip.registration.processor.core.kernel.beans.KernelConfig;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.config.PacketStorageBeanConfig;
import io.mosip.registration.processor.rest.client.config.RestConfigBean;
import io.mosip.registration.processor.status.config.RegistrationStatusBeanConfig;

/**
 * The Class BioDedupeConfig.
 */
@Configuration
@ComponentScan(basePackages = {
		"io.mosip.registration.processor.bio.dedupe.api.controller" },
excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
				RegistrationStatusBeanConfig.class,RestConfigBean.class, PacketStorageBeanConfig.class, KernelConfig.class, CoreConfigBean.class }))
public class BioDedupeConfigTest {

	@MockBean
	public RestTemplate restTemplate;

	@MockBean
	public RegistrationProcessorRestClientService<Object> getRegistrationProcessorRestClientService;

	@MockBean
	public ObjectMapper objectMapper;
}
