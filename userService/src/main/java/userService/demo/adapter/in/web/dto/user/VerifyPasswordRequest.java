package userService.demo.adapter.in.web.dto.user;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequest(
        @NotBlank String password
) {
}
