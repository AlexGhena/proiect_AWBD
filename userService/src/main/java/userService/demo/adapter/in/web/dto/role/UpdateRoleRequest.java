package userService.demo.adapter.in.web.dto.role;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "name must match ROLE_[A-Z0-9_]+") String name,
        @Size(max = 255) String description
) {
}
