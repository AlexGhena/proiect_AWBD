package userService.demo.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import userService.demo.security.jwt.JwtTokenValidators;

import java.security.interfaces.RSAPublicKey;

/**
 * Wires the asymmetric signing setup: userService holds the private key and is therefore the only
 * service able to mint tokens, while every service (including this one) verifies with the public key.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public RSAKey jwtSigningKey(JwtProperties properties) {
        return RsaKeyProvider.resolve(properties);
    }

    /** Public half only; this is what {@code /api/auth/jwks.json} publishes to the other services. */
    @Bean
    public JWKSet jwtPublicJwkSet(RSAKey jwtSigningKey) {
        return new JWKSet(jwtSigningKey.toPublicJWK());
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey jwtSigningKey) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwtSigningKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey jwtSigningKey, JwtProperties properties) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) jwtSigningKey.toPublicKey())
                .build();
        decoder.setJwtValidator(JwtTokenValidators.forIssuerAndAudience(properties.issuer(), properties.audience()));
        return decoder;
    }
}
