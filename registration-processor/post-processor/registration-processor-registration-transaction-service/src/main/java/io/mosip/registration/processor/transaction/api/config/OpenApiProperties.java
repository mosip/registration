package io.mosip.registration.processor.transaction.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author GOVINDARAJ VELU
 *
 */
@Configuration
@ConfigurationProperties(prefix = "openapi")
@Data
public class OpenApiProperties {
    private InfoProperty info;
    private RegistrationProcessor registrationProcessor;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class InfoProperty {
    private String title;
    private String description;
    private String version;
    private LicenseProperty license;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class LicenseProperty {
    private String name;
    private String url;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class RegistrationProcessor {
    private List<Server> servers;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Server {
    private String description;
    private String url;
}
