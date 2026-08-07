package userService.demo.security;

import java.util.List;
import java.util.UUID;

/** The caller's identity, normalised across the JWT and remember-me authentication paths. */
public record AuthenticatedUser(UUID id, String username, List<String> roles) {

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
