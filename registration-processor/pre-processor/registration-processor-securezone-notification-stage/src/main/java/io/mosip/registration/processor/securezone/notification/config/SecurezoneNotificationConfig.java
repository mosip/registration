package io.mosip.registration.processor.securezone.notification.config;

import io.mosip.registration.processor.securezone.notification.stage.SecurezoneNotificationStage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurezoneNotificationConfig {

    @Bean
    public SecurezoneNotificationStage getSecurezoneNotificationStage() {
        return new SecurezoneNotificationStage();
    }
}
