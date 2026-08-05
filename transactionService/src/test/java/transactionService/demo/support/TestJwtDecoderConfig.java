package transactionService.demo.support;

import transactionService.demo.config.ResourceServerProperties;
import transactionService.demo.security.JwtTokenValidators;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Swaps the JWKS-backed decoder for one bound to the test key pair, so the suite never needs a
 * running userService. The validator chain is the production one, so issuer, audience and expiry are
 * still checked exactly as they would be in a real deployment.
 */
@TestConfiguration
public class TestJwtDecoderConfig {

    @Bean
    public TestTokens testTokens() {
        return TestTokens.generate();
    }

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder(TestTokens testTokens, ResourceServerProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(testTokens.publicKey()).build();
        decoder.setJwtValidator(JwtTokenValidators.forIssuerAndAudience(
                properties.issuer(), properties.audience()));
        return decoder;
    }
}
