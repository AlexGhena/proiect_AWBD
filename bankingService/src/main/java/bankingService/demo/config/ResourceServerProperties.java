package bankingService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Token expectations and allowed browser origins. The issuer and audience must match what
 * userService mints, otherwise every request is rejected with a 401.
 *
 * <p>{@code jwkSetUri} points at userService's public key set, so this service only ever holds
 * verification keys and picks up a rotation automatically.
 */
@ConfigurationProperties(prefix = "app.security")
public record ResourceServerProperties(
        String issuer,
        String audience,
        String jwkSetUri,
        List<String> corsAllowedOrigins
) {

    public ResourceServerProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "userService";
        }
        if (audience == null || audience.isBlank()) {
            audience = "awbd-banking-api";
        }
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            jwkSetUri = "http://localhost:8081/api/auth/jwks.json";
        }
        if (corsAllowedOrigins == null || corsAllowedOrigins.isEmpty()) {
            corsAllowedOrigins = List.of("http://localhost:4200");
        }
    }
}
