package userService.demo.adapter.in.web.dto.auth;

import java.util.List;
import java.util.UUID;

/** Identity of the caller behind the current token or remember-me cookie. */
public record CurrentUserResponse(
        UUID userId,
        String username,
        List<String> roles
) {
}
