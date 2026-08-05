package userService.demo.adapter.in.web.dto.role;

import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description
) {
}
