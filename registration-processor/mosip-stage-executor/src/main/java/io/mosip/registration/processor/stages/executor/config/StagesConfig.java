package io.mosip.registration.processor.stages.executor.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.config.ConfigurationUtil;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Class StagesConfig.
 */
@Configuration
@RefreshScope
//This is added to fix issue with loading beans with @RefreshScope in the stages
@ImportAutoConfiguration({RefreshAutoConfiguration.class})
//This is added to fix issue with loading stage specific properties from bootstrap, expected by the stages config.
@PropertySource("classpath:bootstrap.properties")
public class StagesConfig {
	
	private static final String PROP_STAGE_GROUP_NAME = "stage-group-name";
	public static final String DEFAULT_STAGE_GROUP_NAME = "default";
	private static Logger regProcLogger = RegProcessorLogger.getLogger(StagesConfig.class);

	
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
	
	@Autowired
	private Environment environment;
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getConfigurationProperties() {
		try {
			Vertx vertx = Vertx.vertx();
			List<ConfigStoreOptions> configStores = new ArrayList<>();
			List<String> configUrls = getUrls(environment);
			configUrls.forEach(url -> {
				if (url.startsWith(HttpConstants.HTTP.getUrl()))
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
			regProcLogger.info("Getting values from config Server");
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
					regProcLogger.info("{} \n {}", json.cause().getLocalizedMessage(),
							json.cause().getMessage());
					json.otherwiseEmpty();
					retriever.close();
					vertx.close();
				}
			});
			return (Map<String,Object>)configLoader.get().mapTo(Map.class);
		}
		catch(InterruptedException e) {
			regProcLogger.error(ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
			throw new RuntimeException("Could not load config", e);		}
		catch (Exception e1) {
			regProcLogger.error(ExceptionUtils.getStackTrace(e1));
			throw new RuntimeException("Could not load config", e1);
		}
	}
	
	private static List<String> getUrls(Environment environment) {
		List<String> configUrls = new ArrayList<>();
		List<String> appNames = getAppNames(environment);
		String uri = environment.getProperty(ConfigurationUtil.CLOUD_CONFIG_URI);
		String label = environment.getProperty(ConfigurationUtil.CLOUD_CONFIG_LABEL);
		List<String> profiles = getProfiles(environment);
		if(appNames!=null && profiles !=null) {
		profiles.forEach(profile -> {
			appNames.forEach(app -> {
				String url = uri + "/" + app + "/" + profile + "/" + label;
				configUrls.add(url);
			});
		});
		appNames.forEach(appName -> {
		});
		}
		return configUrls;
	}
	
	private static List<String> getAppNames(Environment env) {
		String names = env.getProperty(ConfigurationUtil.APPLICATION_NAMES);
		return names!=null?Stream.of(names.split(",")).collect(Collectors.toList()):null;
	}

	private static List<String> getProfiles(Environment env) {
		String names = env.getProperty(ConfigurationUtil.ACTIVE_PROFILES);
		return names!=null?Stream.of(names.split(",")).collect(Collectors.toList()):null;
	}
	
	public String getStageGroupName() {
		return environment.getProperty(PROP_STAGE_GROUP_NAME, DEFAULT_STAGE_GROUP_NAME);
	}
	
	public MutablePropertySources getCloudPropertySources() {
		Map<String, Object> cloudConfigMap = getConfigurationProperties();
		org.springframework.core.env.PropertySource<?> propertySource = new MapPropertySource("cloudConfig", cloudConfigMap);
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(propertySource);
		return propertySources;
	}
		
}
