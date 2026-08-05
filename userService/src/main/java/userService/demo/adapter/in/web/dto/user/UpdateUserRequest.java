package userService.demo.adapter.in.web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 255) String email,
        @Size(min = 8) String password,
        Boolean enabled
) {
}
