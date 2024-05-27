package io.mosip.registration.processor.status.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import io.mosip.kernel.core.authmanager.authadapter.spi.VertxAuthenticationProvider;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.registration.processor.core.config.ActivemqConfigBean;
import io.mosip.registration.processor.core.config.CoreConfigBean;
import io.mosip.registration.processor.core.config.configserverloader.PropertyLoaderConfig;
import io.mosip.registration.processor.rest.client.config.RestConfigBean;
import io.mosip.registration.processor.status.config.RegistrationStatusBeanConfig;
import io.mosip.registration.processor.status.config.RegistrationStatusServiceBeanConfig;
import io.mosip.registration.processor.status.dao.RegistrationStatusDao;
import io.mosip.registration.processor.status.dao.SyncRegistrationDao;
import io.mosip.registration.processor.status.dto.AuthorizedRolesDto;
import io.mosip.registration.processor.status.repositary.RegistrationRepositary;
import io.mosip.registration.processor.status.service.impl.AnonymousProfileServiceImpl;
import io.mosip.registration.processor.status.service.impl.PacketExternalStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.RegistrationStatusServiceImpl;
import io.mosip.registration.processor.status.service.impl.TransactionServiceImpl;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Configuration
@ComponentScan(basePackages = {
		"io.mosip.registration.processor.status.api" }, excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
				RegistrationStatusBeanConfig.class, RestConfigBean.class,
				CoreConfigBean.class, AuthorizedRolesDto.class,
				PacketExternalStatusServiceImpl.class, ActivemqConfigBean.class }))
public class RegistrationStatusConfigTest {

	@Bean
	public VertxAuthenticationProvider vertxAuthenticationProvider() {
		return new VertxAuthenticationProvider() {
			@Override
			public void addCorsFilter(HttpServer httpServer, Vertx vertx) {

			}

			@Override
			public void addAuthFilter(Router router, String s, HttpMethod httpMethod, String s1) {

			}

			@Override
			public void addAuthFilter(RoutingContext routingContext, String s) {

			}

			@Override
			public String getContextUser(RoutingContext routingContext) {
				return null;
			}
		};
	}

}

