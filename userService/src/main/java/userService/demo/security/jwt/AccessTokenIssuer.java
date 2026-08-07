package userService.demo.security.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import userService.demo.config.JwtProperties;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.port.out.UserRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mints the short-lived access tokens that the other microservices verify.
 *
 * <p>The {@code sub} claim carries the stable user UUID rather than the username, so downstream
 * ownership checks compare identifiers that never change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RSAKey signingKey;
    private final UserRepositoryPort userRepositoryPort;

    @Transactional(readOnly = true)
    public AccessToken issue(Authentication authentication) {
        String username = authentication.getName();
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user named " + username));
        return issue(user.getId(), username, rolesOf(authentication));
    }

    /**
     * Keeps only genuine roles. Spring Security also grants factor authorities such as
     * {@code FACTOR_PASSWORD}, which describe how the caller authenticated rather than what they may
     * do, and have no business travelling to the other services in the {@code roles} claim.
     */
    private static List<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }

    public AccessToken issue(UUID userId, String username, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("preferred_username", username)
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKey.getKeyID())
                .build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.debug("Issued access token for user {} valid until {}", userId, expiresAt);
        return new AccessToken(value, expiresAt, userId, username, roles);
    }
}
