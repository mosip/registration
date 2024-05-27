package io.mosip.registration.processor.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.mosip.registration.processor.core.spi.queue.MosipQueueConnectionFactory;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.queue.factory.MosipQueueConnectionFactoryImpl;
import io.mosip.registration.processor.core.queue.impl.MosipActiveMqImpl;

@PropertySource("classpath:bootstrap.properties")
@Configuration
public class ActivemqConfigBean {

	@Bean
	MosipQueueManager<?, ?> getMosipQueueManager() {
		return new MosipActiveMqImpl();
	}

	@Bean
	MosipQueueConnectionFactory<?> getMosipQueueConnectionFactory() {
		return new MosipQueueConnectionFactoryImpl();
	}
}
