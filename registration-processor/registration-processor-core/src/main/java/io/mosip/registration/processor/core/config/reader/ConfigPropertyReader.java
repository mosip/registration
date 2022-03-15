package io.mosip.registration.processor.core.config.reader;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import java.util.List;

public class ConfigPropertyReader {


    public static String getConfig(String packageConfig) {
        AnnotationConfigApplicationContext ctx1 = new AnnotationConfigApplicationContext();
        ctx1.scan("io.mosip.registration.processor.core.config.configserverloader");
        ctx1.refresh();

        Environment env = ctx1.getBean(Environment.class);
        String authAdapterBasePackage = env.getProperty(packageConfig);

        ctx1.close();

        return authAdapterBasePackage;
    }
}
