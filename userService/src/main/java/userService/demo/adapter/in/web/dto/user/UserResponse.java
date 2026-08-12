package userService.demo.adapter.in.web.dto.user;

import userService.demo.domain.model.ApprovalStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Boolean enabled,
        ApprovalStatus approvalStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
