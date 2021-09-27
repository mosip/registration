package io.mosip.registration.processor.status.api.config;

import io.mosip.registration.processor.status.dto.AuthorizedRolesDto;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import io.mosip.registration.processor.core.config.CoreConfigBean;
import io.mosip.registration.processor.rest.client.config.RestConfigBean;
import io.mosip.registration.processor.status.config.RegistrationStatusBeanConfig;
import io.mosip.registration.processor.status.config.RegistrationStatusServiceBeanConfig;

@Configuration
@ComponentScan(basePackages = {
		"io.mosip.registration.processor.status.*" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
				RegistrationStatusServiceBeanConfig.class, RegistrationStatusBeanConfig.class, RestConfigBean.class,
				CoreConfigBean.class, AuthorizedRolesDto.class}))
public class RegistrationStatusConfigTest {

}

