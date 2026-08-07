package userService.demo.security.jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A freshly minted access token together with the metadata the client needs to use it. */
public record AccessToken(
        String value,
        Instant expiresAt,
        UUID userId,
        String username,
        List<String> roles
) {
}
