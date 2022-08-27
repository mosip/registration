package io.mosip.registration.processor.core.config.configserverloader;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.config.ConfigurationUtil;
import io.mosip.registration.processor.core.config.CoreConfigBean;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@PropertySource("classpath:bootstrap.properties")
@Configuration
public class PropertyLoaderConfig {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(PropertyLoaderConfig.class);

    private enum HttpConstants {
        HTTP("http://"), HTTPS("https://");

        private String url;

        HttpConstants(String url) {
            this.url = url;
        }

        String getUrl() {
            return url;
        }

    }

    @Bean
    public PropertySourcesPlaceholderConfigurer getPropertiesFromConfigServer(Environment environment) throws InterruptedException {
        try {
            Vertx vertx = Vertx.vertx();
            List<ConfigStoreOptions> configStores = new ArrayList<>();
            List<String> configUrls = CoreConfigBean.getUrls(environment);
            configUrls.forEach(url -> {
                if (url.startsWith(PropertyLoaderConfig.HttpConstants.HTTP.getUrl()))
                    configStores.add(new ConfigStoreOptions().setType(ConfigurationUtil.CONFIG_SERVER_TYPE)
                            .setConfig(new JsonObject().put("url", url).put("timeout",
                                    Long.parseLong(ConfigurationUtil.CONFIG_SERVER_TIME_OUT))));
                else
                    configStores.add(new ConfigStoreOptions().setType(ConfigurationUtil.CONFIG_SERVER_TYPE)
                            .setConfig(new JsonObject().put("url", url)
                                    .put("timeout", Long.parseLong(ConfigurationUtil.CONFIG_SERVER_TIME_OUT))
                                    .put("httpClientConfiguration",
                                            new JsonObject().put("trustAll", true).put("ssl", true))));
            });
            ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions();
            configStores.forEach(configRetrieverOptions::addStore);
            ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions.setScanPeriod(0));
            regProcLogger.info(this.getClass().getName(), "", "", "Getting values from config Server");
            CompletableFuture<JsonObject> configLoader = new CompletableFuture<JsonObject>();
            retriever.getConfig(json -> {
                if (json.succeeded()) {
                    JsonObject jsonObject = json.result();
                    if (jsonObject != null) {
                        jsonObject.iterator().forEachRemaining(sourceValue -> System.setProperty(sourceValue.getKey(),
                                sourceValue.getValue().toString()));
                    }
                    configLoader.complete(json.result());
                    json.mapEmpty();
                    retriever.close();
                    vertx.close();
                } else {
                    regProcLogger.info(this.getClass().getName(), "", json.cause().getLocalizedMessage(),
                            json.cause().getMessage());
                    json.otherwiseEmpty();
                    retriever.close();
                    vertx.close();
                }
            });
            configLoader.get();
        } catch (InterruptedException interruptedException) {
            regProcLogger.error(this.getClass().getName(), "", "", ExceptionUtils.getStackTrace(interruptedException));
            throw interruptedException;
        } catch (Exception exception) {
            regProcLogger.error(this.getClass().getName(), "", "", ExceptionUtils.getStackTrace(exception));
        }
        return new PropertySourcesPlaceholderConfigurer();
    }
}
