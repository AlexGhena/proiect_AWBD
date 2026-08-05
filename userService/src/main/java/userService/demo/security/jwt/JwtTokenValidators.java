package userService.demo.security.jwt;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.util.List;

/**
 * Builds the validator chain every service applies to an incoming token: expiry, issuer and audience.
 * A token minted for a different audience must not be replayable against this API.
 */
public final class JwtTokenValidators {

    private JwtTokenValidators() {
    }

    public static OAuth2TokenValidator<Jwt> forIssuerAndAudience(String issuer, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(issuer),
                audienceValidator(audience));
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(audience));
    }
}
