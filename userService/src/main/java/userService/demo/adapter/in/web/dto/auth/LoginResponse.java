package userService.demo.adapter.in.web.dto.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Login result. Carries the short-lived access token only; no password material ever appears here. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UUID userId,
        String username,
        List<String> roles
) {
}
