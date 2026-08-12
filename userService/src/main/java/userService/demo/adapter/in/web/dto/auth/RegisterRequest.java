package userService.demo.adapter.in.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload. There is no roles field on purpose: the account is created
 * disabled and PENDING with no role at all, and only becomes a usable ROLE_USER client once an
 * administrator approves the request.
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8) String password
) {
}
