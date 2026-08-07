package userService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Signing and claim settings for the access tokens minted by userService.
 *
 * <p>{@code privateKey}/{@code publicKey} hold PEM text and are expected to arrive from an
 * environment variable or a Kubernetes Secret. When both are blank the {@code dev} profile falls
 * back to a locally generated key pair (see {@link RsaKeyProvider}).
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        String privateKey,
        String publicKey
) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "userService";
        }
        if (audience == null || audience.isBlank()) {
            audience = "awbd-banking-api";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
    }

    public boolean hasConfiguredKeyPair() {
        return privateKey != null && !privateKey.isBlank()
                && publicKey != null && !publicKey.isBlank();
    }
}
