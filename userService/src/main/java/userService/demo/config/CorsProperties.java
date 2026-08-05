package userService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Browser origins allowed to call this API. Credentialed CORS forbids a wildcard, so the frontend
 * origin is always listed explicitly.
 */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:4200");
        }
    }
}
