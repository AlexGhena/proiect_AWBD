package bankingService.demo.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mints access tokens for tests, signed with a key pair generated fresh in memory on every run.
 *
 * <p>No development or production key material is involved. Because the issuer, audience, expiry and
 * signing key are all controllable, the same helper produces both the valid tokens the happy-path
 * tests need and the deliberately wrong ones the rejection tests need.
 */
public final class TestTokens {

    public static final String ISSUER = "userService";
    public static final String AUDIENCE = "awbd-banking-api";

    private final RSAKey rsaKey;
    private final JwtEncoder encoder;

    private TestTokens(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    public static TestTokens generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new TestTokens(new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA key generation is unavailable on this JVM", ex);
        }
    }

    public RSAPublicKey publicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not expose the test public key", ex);
        }
    }

    public String validToken(UUID userId, String username, String... roles) {
        Instant now = Instant.now();
        return token(userId, username, ISSUER, AUDIENCE, now, now.plus(Duration.ofMinutes(15)), List.of(roles));
    }

    public String tokenWithIssuer(String issuer, UUID userId, String... roles) {
        Instant now = Instant.now();
        return token(userId, "test.user", issuer, AUDIENCE, now, now.plus(Duration.ofMinutes(15)), List.of(roles));
    }

    public String tokenWithAudience(String audience, UUID userId, String... roles) {
        Instant now = Instant.now();
        return token(userId, "test.user", ISSUER, audience, now, now.plus(Duration.ofMinutes(15)), List.of(roles));
    }

    /** Issued an hour ago and already expired half an hour ago. */
    public String expiredToken(UUID userId, String... roles) {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(1));
        return token(userId, "test.user", ISSUER, AUDIENCE,
                issuedAt, issuedAt.plus(Duration.ofMinutes(30)), List.of(roles));
    }

    private String token(UUID userId, String username, String issuer, String audience,
                         Instant issuedAt, Instant expiresAt, List<String> roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("preferred_username", username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(rsaKey.getKeyID()).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
