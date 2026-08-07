package bankingService.demo.security;

import java.util.List;
import java.util.UUID;

/** The caller's identity as carried by the validated access token. */
public record AuthenticatedUser(UUID id, String username, List<String> roles) {

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
