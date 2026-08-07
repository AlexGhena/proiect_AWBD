package userService.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.port.out.UserRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns whatever authenticated the request into a single identity shape.
 *
 * <p>A Bearer token already carries the user UUID in {@code sub}, so that path needs no database
 * round trip. Remember-me authentication only knows the username and is resolved against the
 * repository.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepositoryPort userRepositoryPort;

    public Optional<AuthenticatedUser> resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(fromJwt(jwt));
        }
        return Optional.of(fromUsername(authentication));
    }

    public AuthenticatedUser require(Authentication authentication) {
        return resolve(authentication)
                .orElseThrow(() -> new UsernameNotFoundException("The request is not authenticated"));
    }

    /** Convenience for {@code @PreAuthorize} beans, which are easier to write without an argument. */
    public Optional<AuthenticatedUser> current() {
        return resolve(SecurityContextHolder.getContext().getAuthentication());
    }

    private AuthenticatedUser fromJwt(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("preferred_username"),
                roles == null ? List.of() : roles);
    }

    private AuthenticatedUser fromUsername(Authentication authentication) {
        String username = authentication.getName();
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user named " + username));
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new AuthenticatedUser(user.getId(), user.getUsername(), roles);
    }
}
