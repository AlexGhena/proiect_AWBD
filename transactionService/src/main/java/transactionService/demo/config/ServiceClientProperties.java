package transactionService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Locations of the services this one calls. */
@ConfigurationProperties(prefix = "app.clients")
public record ServiceClientProperties(String bankingServiceUrl) {

    public ServiceClientProperties {
        if (bankingServiceUrl == null || bankingServiceUrl.isBlank()) {
            bankingServiceUrl = "http://localhost:8082";
        }
    }
}
