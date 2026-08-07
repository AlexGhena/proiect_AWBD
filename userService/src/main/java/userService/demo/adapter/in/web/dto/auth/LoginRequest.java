package userService.demo.adapter.in.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login payload. Deliberately limited to credentials and the remember-me choice; roles and any
 * other privilege-bearing field are never accepted from the client.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password,
        boolean rememberMe
) {
}
