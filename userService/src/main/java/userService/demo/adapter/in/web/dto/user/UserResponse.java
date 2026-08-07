package userService.demo.adapter.in.web.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
