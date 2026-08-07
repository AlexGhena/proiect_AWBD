package userService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Remember-me settings. The {@code key} signs the persistent token and must come from an environment
 * variable or a Kubernetes Secret outside local development.
 */
@ConfigurationProperties(prefix = "app.security.remember-me")
public record RememberMeProperties(
        String key,
        Duration validity,
        String cookieName,
        boolean secureCookie
) {

    public RememberMeProperties {
        if (key == null || key.isBlank()) {
            key = "dev-only-remember-me-key";
        }
        if (validity == null) {
            validity = Duration.ofDays(7);
        }
        if (cookieName == null || cookieName.isBlank()) {
            cookieName = "remember-me";
        }
    }

    public int validitySeconds() {
        return Math.toIntExact(validity.toSeconds());
    }
}
