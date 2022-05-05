package io.mosip.registration.processor.core.config.reader;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.config.CoreConfigBean;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import java.util.List;

public class ConfigPropertyReader {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(ConfigPropertyReader.class);

    public static String getConfig(String packageConfig) {
        AnnotationConfigApplicationContext ctx1 = new AnnotationConfigApplicationContext();
        ctx1.scan("io.mosip.registration.processor.core.config.configserverloader");
        ctx1.refresh();

        Environment env = ctx1.getBean(Environment.class);
        String authAdapterBasePackage = env.getProperty(packageConfig);

        regProcLogger.info("Loading auth adapter from package : " + authAdapterBasePackage);

        ctx1.close();

        return authAdapterBasePackage;
    }
}
