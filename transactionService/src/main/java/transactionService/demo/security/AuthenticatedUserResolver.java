package transactionService.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the caller's identity straight out of the validated token. This service authenticates only
 * via Bearer tokens, so no user lookup is ever needed - the UUID arrives in the {@code sub} claim.
 */
@Component
public class AuthenticatedUserResolver {

    public Optional<AuthenticatedUser> resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return Optional.of(new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("preferred_username"),
                roles == null ? List.of() : roles));
    }

    /** Convenience for {@code @PreAuthorize} beans, which read more cleanly without an argument. */
    public Optional<AuthenticatedUser> current() {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }
}
