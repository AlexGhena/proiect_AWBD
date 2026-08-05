package userService.demo.adapter.in.web.dto.profile;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}
