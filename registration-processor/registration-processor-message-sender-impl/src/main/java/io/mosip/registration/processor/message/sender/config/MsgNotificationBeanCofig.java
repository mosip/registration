package io.mosip.registration.processor.message.sender.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import io.mosip.registration.processor.core.notification.template.generator.dto.ResponseDto;
import io.mosip.registration.processor.core.notification.template.generator.dto.SmsResponseDto;
import io.mosip.registration.processor.core.spi.message.sender.MessageNotificationService;
import io.mosip.registration.processor.core.util.LanguageUtility;
import io.mosip.registration.processor.message.sender.service.impl.MessageNotificationServiceImpl;
import io.mosip.registration.processor.message.sender.template.TemplateGenerator;
import io.mosip.registration.processor.packet.manager.decryptor.Decryptor;
import io.mosip.registration.processor.packet.manager.decryptor.DecryptorImpl;

@Configuration
public class MsgNotificationBeanCofig {

	@Bean
	public MessageNotificationService<SmsResponseDto, ResponseDto, MultipartFile[]> getMessageNotificationService() {
		return new MessageNotificationServiceImpl();
	}
	
	@Bean
	public TemplateGenerator getTemplateGenerator() {
		return new TemplateGenerator();
	}
	
	@Bean
	public LanguageUtility getLanguageUtility() {
		return new LanguageUtility();
	}
	
	@Bean
	public Decryptor getDecryptor() {
		return new DecryptorImpl();
	}
}
