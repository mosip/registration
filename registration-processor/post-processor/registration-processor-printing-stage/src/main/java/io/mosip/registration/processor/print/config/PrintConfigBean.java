package io.mosip.registration.processor.print.config;

import io.mosip.registration.processor.print.util.CredentialPartnerUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrintConfigBean {

	@Bean
	CredentialPartnerUtil getCredentialPartnerUtil() {
		return new CredentialPartnerUtil();
	}

}