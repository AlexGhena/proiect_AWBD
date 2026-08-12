package bankingService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Locations of the services this one calls. */
@ConfigurationProperties(prefix = "app.clients")
public record ServiceClientProperties(String userServiceUrl) {

    public ServiceClientProperties {
        if (userServiceUrl == null || userServiceUrl.isBlank()) {
            userServiceUrl = "http://localhost:8081";
        }
    }
}
